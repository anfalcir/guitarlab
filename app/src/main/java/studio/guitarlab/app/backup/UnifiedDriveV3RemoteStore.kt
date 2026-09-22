package studio.guitarlab.app.backup

import java.io.File
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import kotlinx.coroutines.CancellationException
import studio.guitarlab.core.project.DriveAssetObject
import studio.guitarlab.core.project.DriveCurrentDescriptor
import studio.guitarlab.core.project.DriveLocalAsset
import studio.guitarlab.core.project.DriveProjectRevisionManifest
import studio.guitarlab.core.project.DrivePublishedHead
import studio.guitarlab.core.project.BackupHashing
import studio.guitarlab.core.project.DriveRemoteObjectReceipt
import studio.guitarlab.core.project.UnifiedDriveRemoteStore
import studio.guitarlab.core.project.UnifiedDriveRestoreSource

/** Drive v3 adapter for U8 immutable objects, manifests and append-only heads. */
internal class UnifiedDriveV3RemoteStore(
    private val http: DriveV3Api,
    private val rootFolderId: suspend () -> String,
) : UnifiedDriveRemoteStore, UnifiedDriveRestoreSource {
    override suspend fun findAsset(sha256: String): DriveRemoteObjectReceipt? =
        findOne(KIND_ASSET, PROP_SHA256 to sha256)?.verifiedReceipt()

    override suspend fun uploadAsset(asset: DriveLocalAsset): DriveRemoteObjectReceipt {
        findAsset(asset.identity.sha256)?.let { existing ->
            require(existing.matches(asset.identity)) { "Drive contains an incompatible object for this SHA-256." }
            return existing
        }
        return uploadImmutable(
            file = asset.file,
            name = asset.identity.sha256,
            kind = KIND_ASSET,
            sha256 = asset.identity.sha256,
            extra = emptyMap(),
        ).also { require(it.matches(asset.identity)) { "Drive asset verification failed." } }
    }

    override suspend fun findManifest(manifestSha256: String): DriveRemoteObjectReceipt? =
        findOne(KIND_MANIFEST, PROP_SHA256 to manifestSha256)?.verifiedReceipt()

    override suspend fun uploadManifest(manifest: DriveProjectRevisionManifest): DriveRemoteObjectReceipt {
        val bytes = manifest.canonicalBytes()
        val identity = DriveAssetObject(manifest.manifestSha256, bytes.size.toLong())
        findManifest(identity.sha256)?.let { existing ->
            require(existing.matches(identity)) { "Drive contains an incompatible manifest object." }
            return existing
        }
        val temporary = kotlin.io.path.createTempFile("guitarlab-u8-manifest-", ".txt").toFile()
        return try {
            temporary.writeBytes(bytes)
            uploadImmutable(
                file = temporary,
                name = "${manifest.projectId}-${manifest.revisionId}.manifest",
                kind = KIND_MANIFEST,
                sha256 = identity.sha256,
                extra = mapOf(PROP_PROJECT_ID to manifest.projectId, PROP_REVISION_ID to manifest.revisionId),
            ).also { require(it.matches(identity)) { "Drive manifest verification failed." } }
        } finally {
            temporary.delete()
        }
    }

    override suspend fun listHeads(projectId: String): List<DrivePublishedHead> =
        listFiles(query(KIND_HEAD, PROP_PROJECT_ID to projectId)).mapNotNull { file ->
            val revision = file.appProperties[PROP_REVISION_ID]?.takeIf(String::isNotBlank) ?: return@mapNotNull null
            val manifestHash = file.appProperties[PROP_MANIFEST_SHA256]
                ?.takeIf { SHA256.matches(it) } ?: return@mapNotNull null
            DrivePublishedHead(
                descriptor = DriveCurrentDescriptor(projectId, revision, manifestHash),
                baseRevisionId = file.appProperties[PROP_BASE_REVISION]?.takeIf(String::isNotBlank),
            )
        }

    override suspend fun loadManifest(descriptor: DriveCurrentDescriptor): DriveProjectRevisionManifest {
        val resource = findOne(KIND_MANIFEST, PROP_SHA256 to descriptor.manifestSha256)
            ?: error("Drive manifest is missing.")
        val temporary = kotlin.io.path.createTempFile("guitarlab-u8-manifest-download-", ".txt").toFile()
        return try {
            http.download("$FILES/${resource.id}?alt=media", temporary)
            require(BackupHashing.sha256(temporary) == descriptor.manifestSha256) {
                "Downloaded Drive manifest failed SHA-256 validation."
            }
            DriveProjectRevisionManifest.parseCanonical(temporary.readBytes()).also { manifest ->
                require(
                    manifest.projectId == descriptor.projectId &&
                        manifest.revisionId == descriptor.revisionId &&
                        manifest.manifestSha256 == descriptor.manifestSha256
                ) { "Downloaded Drive manifest does not match the requested revision." }
            }
        } finally {
            temporary.delete()
        }
    }

    override suspend fun downloadAsset(asset: DriveAssetObject, destination: File) {
        val resource = findOne(KIND_ASSET, PROP_SHA256 to asset.sha256)
            ?: error("Drive asset is missing.")
        require(resource.verifiedReceipt()?.matches(asset) == true) { "Drive asset metadata is incompatible." }
        http.download("$FILES/${resource.id}?alt=media", destination)
        require(destination.isFile && destination.length() == asset.sizeBytes) { "Downloaded Drive asset size mismatch." }
        require(BackupHashing.sha256(destination) == asset.sha256) { "Downloaded Drive asset failed SHA-256 validation." }
    }

    override suspend fun publishHead(head: DrivePublishedHead) {
        val existing = listHeads(head.descriptor.projectId).filter {
            it.descriptor.revisionId == head.descriptor.revisionId
        }
        if (existing.isNotEmpty()) {
            require(existing.size == 1 && existing.single() == head) { "Drive revision head is ambiguous." }
            return
        }
        val properties = mapOf(
            PROP_SCHEMA to SCHEMA,
            PROP_KIND to KIND_HEAD,
            PROP_PROJECT_ID to head.descriptor.projectId,
            PROP_REVISION_ID to head.descriptor.revisionId,
            PROP_BASE_REVISION to head.baseRevisionId.orEmpty(),
            PROP_MANIFEST_SHA256 to head.descriptor.manifestSha256,
        )
        val response = http.postJson(
            "$FILES?fields=$FIELDS_ENCODED",
            DriveV3Json.metadata(
                name = "${head.descriptor.projectId}-${head.descriptor.revisionId}.head",
                mimeType = MIME_HEAD,
                parents = listOf(rootFolderId()),
                appProperties = properties,
            ),
        )
        requireSuccess(response, "Drive did not publish the project head.")
        val created = DriveV3Json.parseFile(response.body)
        require(created.appProperties == properties) { "Drive returned incompatible head metadata." }
    }

    private suspend fun uploadImmutable(
        file: File,
        name: String,
        kind: String,
        sha256: String,
        extra: Map<String, String>,
    ): DriveRemoteObjectReceipt {
        val properties = mapOf(PROP_SCHEMA to SCHEMA, PROP_KIND to kind, PROP_SHA256 to sha256) + extra
        val initiated = requireSuccess(
            http.postJson(
                "$UPLOAD_FILES?uploadType=resumable&fields=$FIELDS_ENCODED",
                DriveV3Json.metadata(name, MIME_BINARY, listOf(rootFolderId()), properties),
                mapOf("X-Upload-Content-Type" to MIME_BINARY, "X-Upload-Content-Length" to file.length().toString()),
            ),
            "Drive did not create a resumable upload session.",
        )
        val session = initiated.header("Location") ?: error("Drive response omitted the resumable session URL.")
        var offset = 0L
        while (offset < file.length()) {
            val length = minOf(CHUNK_BYTES.toLong(), file.length() - offset).toInt()
            val response = try {
                http.uploadChunk(session, file, offset, length, file.length())
            } catch (error: Throwable) {
                if (error is CancellationException) throw error
                http.uploadStatus(session, file.length())
            }
            when (response.code) {
                200, 201 -> return verifyUploaded(DriveV3Json.parseFile(response.body), sha256, file.length())
                308 -> offset = nextOffset(response.header("Range"))
                404 -> error("Drive resumable session expired before object commit.")
                else -> throw DriveApiException(response.code, response.body)
            }
        }
        error("Drive did not confirm immutable object upload.")
    }

    private suspend fun verifyUploaded(file: DriveFileResource, sha256: String, size: Long): DriveRemoteObjectReceipt {
        val complete = if (file.size != null && file.sha256Checksum != null) file else getFile(file.id)
        require(complete.size == size && complete.sha256Checksum.equals(sha256, true)) {
            "Drive object size or SHA-256 did not match the local object."
        }
        return DriveRemoteObjectReceipt(sha256, size)
    }

    private suspend fun findOne(kind: String, property: Pair<String, String>): DriveFileResource? =
        listFiles(query(kind, property)).singleOrNull()

    private suspend fun getFile(id: String): DriveFileResource = DriveV3Json.parseFile(
        requireSuccess(http.get("$FILES/$id?fields=$FIELDS_ENCODED"), "Drive object verification failed.").body,
    )

    private suspend fun listFiles(query: String): List<DriveFileResource> {
        val output = mutableListOf<DriveFileResource>()
        var page: String? = null
        do {
            val url = buildString {
                append(FILES).append("?q=").append(encode(query)).append("&fields=").append(PAGE_FIELDS_ENCODED)
                page?.let { append("&pageToken=").append(encode(it)) }
            }
            val parsed = DriveV3Json.parsePage(requireSuccess(http.get(url), "Drive object listing failed.").body)
            output += parsed.files
            page = parsed.nextPageToken
        } while (page != null)
        return output
    }

    private suspend fun query(kind: String, property: Pair<String, String>): String =
        "trashed = false and '${rootFolderId()}' in parents and " +
            "appProperties has { key='$PROP_SCHEMA' and value='$SCHEMA' } and " +
            "appProperties has { key='$PROP_KIND' and value='$kind' } and " +
            "appProperties has { key='${property.first}' and value='${property.second}' }"

    private fun DriveFileResource.verifiedReceipt(): DriveRemoteObjectReceipt? {
        val hash = sha256Checksum?.lowercase()?.takeIf { SHA256.matches(it) } ?: return null
        val bytes = size?.takeIf { it > 0L } ?: return null
        return DriveRemoteObjectReceipt(hash, bytes)
    }

    private fun requireSuccess(response: DriveHttpResponse, message: String): DriveHttpResponse {
        if (!response.successful) throw DriveApiException(response.code, response.body)
        return response.also { require(response.successful) { message } }
    }

    private fun nextOffset(range: String?): Long = range?.substringAfterLast('-')?.toLongOrNull()?.plus(1L) ?: 0L
    private fun encode(value: String) = URLEncoder.encode(value, StandardCharsets.UTF_8.toString())

    private companion object {
        const val FILES = "https://www.googleapis.com/drive/v3/files"
        const val UPLOAD_FILES = "https://www.googleapis.com/upload/drive/v3/files"
        const val MIME_BINARY = "application/octet-stream"
        const val MIME_HEAD = "application/vnd.studio.guitarlab.drive-head+json"
        const val SCHEMA = "guitarlab-unified-drive-v3"
        const val KIND_ASSET = "asset"
        const val KIND_MANIFEST = "manifest"
        const val KIND_HEAD = "head"
        const val PROP_SCHEMA = "glSchema"
        const val PROP_KIND = "glKind"
        const val PROP_SHA256 = "glSha256"
        const val PROP_PROJECT_ID = "glProjectId"
        const val PROP_REVISION_ID = "glRevisionId"
        const val PROP_BASE_REVISION = "glBaseRevisionId"
        const val PROP_MANIFEST_SHA256 = "glManifestSha256"
        const val CHUNK_BYTES = 8 * 1024 * 1024
        val SHA256 = Regex("[0-9a-f]{64}")
        val FIELDS_ENCODED = encodeStatic("id,name,size,sha256Checksum,appProperties,trashed")
        val PAGE_FIELDS_ENCODED = encodeStatic("nextPageToken,files(id,name,size,sha256Checksum,appProperties,trashed)")
        fun encodeStatic(value: String) = URLEncoder.encode(value, StandardCharsets.UTF_8.toString())
    }
}
