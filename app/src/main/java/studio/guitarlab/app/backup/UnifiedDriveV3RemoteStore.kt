package studio.guitarlab.app.backup

import java.io.File
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.time.Instant
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import studio.guitarlab.core.project.DriveAssetObject
import studio.guitarlab.core.project.DriveCurrentDescriptor
import studio.guitarlab.core.project.DriveGcCandidate
import studio.guitarlab.core.project.DriveLocalAsset
import studio.guitarlab.core.project.DriveProjectRevisionManifest
import studio.guitarlab.core.project.DrivePublishedHead
import studio.guitarlab.core.project.BackupHashing
import studio.guitarlab.core.project.DriveRemoteObjectReceipt
import studio.guitarlab.core.project.UnifiedDriveGarbageCollectionStore
import studio.guitarlab.core.project.UnifiedDriveRemoteStore
import studio.guitarlab.core.project.UnifiedDriveRestoreSource

/** Drive v3 adapter for U8 immutable objects, manifests and append-only heads. */
internal class UnifiedDriveV3RemoteStore(
    private val http: DriveV3Api,
    private val rootFolderId: suspend () -> String,
    private val uploadState: UnifiedDriveUploadState = NoopUnifiedDriveUploadState,
    private val retryDelay: suspend (Long) -> Unit = { delay(it) },
) : UnifiedDriveRemoteStore, UnifiedDriveRestoreSource, UnifiedDriveGarbageCollectionStore {
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
        listFiles(query(KIND_HEAD, PROP_PROJECT_ID to projectId))
            .map(::publishedHead)

    suspend fun listAllHeads(): List<DrivePublishedHead> =
        listFiles(queryKind(KIND_HEAD)).map(::publishedHead)

    /** Deletes one project's heads/manifests and only content assets not referenced elsewhere. */
    suspend fun deleteProject(projectId: String): Int {
        require(projectId.isNotBlank()) { "Project id is required for Drive cleanup." }
        val headFiles = listFiles(query(KIND_HEAD, PROP_PROJECT_ID to projectId))
        val manifestFiles = listFiles(query(KIND_MANIFEST, PROP_PROJECT_ID to projectId))
        val manifests = manifestFiles.map { loadManifestResource(it, projectId) }
        val ownedHashes = manifests.flatMapTo(mutableSetOf()) { it.assets.map(DriveAssetObject::sha256) }
        val protectedHashes = listAllHeads()
            .filter { it.descriptor.projectId != projectId }
            .distinctBy { it.descriptor.manifestSha256 }
            .flatMapTo(mutableSetOf()) { loadManifest(it.descriptor).assets.map(DriveAssetObject::sha256) }

        var deleted = 0
        headFiles.forEach { deleteFile(it.id); deleted++ }
        manifestFiles.forEach { deleteFile(it.id); deleted++ }
        (ownedHashes - protectedHashes).forEach { hash ->
            listFiles(query(KIND_ASSET, PROP_SHA256 to hash)).forEach { file ->
                require(file.verifiedReceipt()?.sha256 == hash) { "Drive cleanup found ambiguous asset metadata." }
                deleteFile(file.id)
                deleted++
            }
        }
        require(listFiles(query(KIND_HEAD, PROP_PROJECT_ID to projectId)).isEmpty())
        require(listFiles(query(KIND_MANIFEST, PROP_PROJECT_ID to projectId)).isEmpty())
        return deleted
    }

    override suspend fun listCommittedManifests(): List<DriveProjectRevisionManifest> =
        listAllHeads()
            .distinctBy { it.descriptor.manifestSha256 }
            .map { loadManifest(it.descriptor) }

    override suspend fun listAssets(): List<DriveGcCandidate> =
        listFiles(queryKind(KIND_ASSET))
            .mapNotNull { file ->
                val receipt = file.verifiedReceipt() ?: return@mapNotNull null
                val createdAt = file.createdTime
                    ?.let { runCatching { Instant.parse(it).toEpochMilli() }.getOrNull() }
                    ?: return@mapNotNull null
                DriveGcCandidate(
                    DriveAssetObject(receipt.sha256, receipt.sizeBytes),
                    createdAt,
                )
            }
            .groupBy { it.asset.sha256 }
            .values
            .map { duplicates -> duplicates.maxBy { it.uploadedAtEpochMs } }

    override suspend fun deleteAsset(asset: DriveAssetObject): Boolean {
        val matches = listFiles(query(KIND_ASSET, PROP_SHA256 to asset.sha256))
        if (matches.isEmpty()) return true
        matches.forEach { file ->
            require(file.verifiedReceipt()?.matches(asset) == true) {
                "Drive object selected for garbage collection has incompatible metadata."
            }
        }
        matches.forEach { file ->
            val response = http.delete("$FILES/${file.id}")
            if (response.code != 204 && response.code != 404) {
                requireSuccess(response, "Drive asset deletion failed.")
            }
        }
        return true
    }


    /**
     * U8m-only cleanup. It can only target the random acceptance namespace and protects any
     * content object still reachable from another project head.
     */
    internal suspend fun cleanupAcceptanceProject(
        projectId: String,
        extraOwnedAssetHashes: Set<String>,
    ): U8mRemoteCleanupResult {
        require(U8mRealDriveAcceptanceContract.isCampaignProjectId(projectId)) {
            "Refusing Drive cleanup outside a U8m campaign namespace."
        }
        require(extraOwnedAssetHashes.all(SHA256::matches)) {
            "U8m cleanup received an invalid owned asset hash."
        }

        val headFiles = listFiles(query(KIND_HEAD, PROP_PROJECT_ID to projectId))
        val heads = headFiles.map(::publishedHead)
        require(heads.all { it.descriptor.projectId == projectId }) {
            "U8m cleanup encountered a foreign Drive head."
        }

        val manifestFiles = listFiles(query(KIND_MANIFEST, PROP_PROJECT_ID to projectId))
        val manifests = manifestFiles.map { loadManifestResource(it, projectId) }
        val ownedHashes = manifests
            .flatMapTo(mutableSetOf()) { manifest -> manifest.assets.map { it.sha256 } }
            .apply { addAll(extraOwnedAssetHashes) }

        val protectedOutsideCampaign = listAllHeads()
            .filter { it.descriptor.projectId != projectId }
            .distinctBy { it.descriptor.manifestSha256 }
            .flatMapTo(mutableSetOf()) { head ->
                loadManifest(head.descriptor).assets.map { it.sha256 }
            }
        val deletableHashes = ownedHashes - protectedOutsideCampaign

        var deletedHeads = 0
        var deletedManifests = 0
        var deletedAssets = 0
        for (file in headFiles) {
            deleteFile(file.id)
            deletedHeads++
        }
        for (file in manifestFiles) {
            deleteFile(file.id)
            deletedManifests++
        }
        for (hash in deletableHashes) {
            val matches = listFiles(query(KIND_ASSET, PROP_SHA256 to hash))
            matches.forEach { file ->
                require(file.verifiedReceipt()?.sha256 == hash) {
                    "U8m cleanup found ambiguous asset metadata."
                }
                deleteFile(file.id)
                deletedAssets++
            }
        }

        require(listFiles(query(KIND_HEAD, PROP_PROJECT_ID to projectId)).isEmpty()) {
            "U8m cleanup left campaign heads behind."
        }
        require(listFiles(query(KIND_MANIFEST, PROP_PROJECT_ID to projectId)).isEmpty()) {
            "U8m cleanup left campaign manifests behind."
        }
        return U8mRemoteCleanupResult(
            deletedHeads = deletedHeads,
            deletedManifests = deletedManifests,
            deletedAssets = deletedAssets,
            protectedSharedAssets = ownedHashes.size - deletableHashes.size,
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
        require(file.isFile && file.length() > 0L)
        val totalBytes = file.length()
        val properties =
            mapOf(PROP_SCHEMA to SCHEMA, PROP_KIND to kind, PROP_SHA256 to sha256) + extra
        var session = uploadState.session(sha256, totalBytes)?.url
        var offset = 0L
        var sessionRestarts = 0

        while (true) {
            if (session != null) {
                when (val status = queryUploadStatus(session, totalBytes)) {
                    null -> throw java.io.IOException(
                        "Drive resumable upload state could not be verified.",
                    )
                    else -> when (status.code) {
                        200, 201 -> {
                            uploadState.clear(sha256)
                            return verifyUploaded(
                                DriveV3Json.parseFile(status.body),
                                sha256,
                                totalBytes,
                            )
                        }
                        308 -> offset = nextOffset(status.header("Range"))
                        404 -> {
                            uploadState.clear(sha256)
                            session = null
                            offset = 0L
                        }
                        else -> throw DriveApiException(status.code, status.body)
                    }
                }
            }

            if (session == null) {
                val initiated = requireSuccess(
                    http.postJson(
                        "$UPLOAD_FILES?uploadType=resumable&fields=$FIELDS_ENCODED",
                        DriveV3Json.metadata(
                            name,
                            MIME_BINARY,
                            listOf(rootFolderId()),
                            properties,
                        ),
                        mapOf(
                            "X-Upload-Content-Type" to MIME_BINARY,
                            "X-Upload-Content-Length" to totalBytes.toString(),
                        ),
                    ),
                    "Drive did not create a resumable upload session.",
                )
                session = initiated.header("Location")
                    ?: error("Drive response omitted the resumable session URL.")
                uploadState.save(sha256, totalBytes, session)
                offset = 0L
            }

            while (offset < totalBytes) {
                val activeSession = requireNotNull(session) {
                    "Drive resumable session disappeared during upload."
                }
                val length = minOf(CHUNK_BYTES.toLong(), totalBytes - offset).toInt()
                val response = try {
                    http.uploadChunk(activeSession, file, offset, length, totalBytes)
                } catch (error: Throwable) {
                    if (
                        error is CancellationException ||
                            error is DriveAuthorizationRequiredException
                    ) {
                        throw error
                    }
                    queryUploadStatus(activeSession, totalBytes) ?: throw error
                }

                when (response.code) {
                    200, 201 -> {
                        uploadState.clear(sha256)
                        return verifyUploaded(
                            DriveV3Json.parseFile(response.body),
                            sha256,
                            totalBytes,
                        )
                    }
                    308 -> offset = nextOffset(response.header("Range"))
                    404 -> {
                        uploadState.clear(sha256)
                        session = null
                        offset = 0L
                        sessionRestarts++
                        require(sessionRestarts <= MAX_SESSION_RESTARTS) {
                            "Drive resumable session repeatedly expired."
                        }
                        break
                    }
                    401 -> {
                        val recovered = queryUploadStatus(activeSession, totalBytes)
                            ?: throw DriveApiException(response.code, response.body)
                        when (recovered.code) {
                            200, 201 -> {
                                uploadState.clear(sha256)
                                return verifyUploaded(
                                    DriveV3Json.parseFile(recovered.body),
                                    sha256,
                                    totalBytes,
                                )
                            }
                            308 -> offset = nextOffset(recovered.header("Range"))
                            404 -> {
                                uploadState.clear(sha256)
                                session = null
                                offset = 0L
                                sessionRestarts++
                                require(sessionRestarts <= MAX_SESSION_RESTARTS) {
                                    "Drive resumable session repeatedly expired."
                                }
                                break
                            }
                            else -> throw DriveApiException(recovered.code, recovered.body)
                        }
                    }
                    else -> {
                        if (!DriveRetryPolicy.retryableStatus(response.code, response.body)) {
                            throw DriveApiException(response.code, response.body)
                        }
                        val recovered = queryUploadStatus(activeSession, totalBytes)
                            ?: throw DriveApiException(response.code, response.body)
                        when (recovered.code) {
                            200, 201 -> {
                                uploadState.clear(sha256)
                                return verifyUploaded(
                                    DriveV3Json.parseFile(recovered.body),
                                    sha256,
                                    totalBytes,
                                )
                            }
                            308 -> offset = nextOffset(recovered.header("Range"))
                            404 -> {
                                uploadState.clear(sha256)
                                session = null
                                offset = 0L
                                sessionRestarts++
                                require(sessionRestarts <= MAX_SESSION_RESTARTS) {
                                    "Drive resumable session repeatedly expired."
                                }
                                break
                            }
                            else -> throw DriveApiException(recovered.code, recovered.body)
                        }
                    }
                }
            }
        }
    }

    private suspend fun queryUploadStatus(
        session: String,
        totalBytes: Long,
    ): DriveHttpResponse? {
        var attempt = 0
        while (attempt < DriveRetryPolicy.MAX_ATTEMPTS) {
            val response = try {
                http.uploadStatus(session, totalBytes)
            } catch (error: Throwable) {
                if (
                    error is CancellationException ||
                        error is DriveAuthorizationRequiredException
                ) {
                    throw error
                }
                if (attempt >= DriveRetryPolicy.MAX_ATTEMPTS - 1) return null
                retryDelay(DriveRetryPolicy.delayMs(attempt++))
                continue
            }
            if (response.code in setOf(200, 201, 308, 404)) return response
            if (!DriveRetryPolicy.retryableStatus(response.code, response.body)) return response
            if (attempt >= DriveRetryPolicy.MAX_ATTEMPTS - 1) return response
            retryDelay(DriveRetryPolicy.delayMs(attempt++))
        }
        return null
    }

    private suspend fun verifyUploaded(file: DriveFileResource, sha256: String, size: Long): DriveRemoteObjectReceipt {
        val complete = if (file.size != null && file.sha256Checksum != null) file else getFile(file.id)
        require(complete.size == size && complete.sha256Checksum.equals(sha256, true)) {
            "Drive object size or SHA-256 did not match the local object."
        }
        return DriveRemoteObjectReceipt(sha256, size)
    }

    private suspend fun findOne(
        kind: String,
        property: Pair<String, String>,
    ): DriveFileResource? {
        val matches = listFiles(query(kind, property))
        if (matches.isEmpty()) return null
        if (matches.size == 1) return matches.single()
        val receipts = matches.map { it.verifiedReceipt() }
        require(receipts.all { it != null } && receipts.distinct().size == 1) {
            "Drive contains ambiguous objects for the same content identity."
        }
        return matches.first()
    }

    private fun publishedHead(file: DriveFileResource): DrivePublishedHead {
        val projectId = requireNotNull(
            file.appProperties[PROP_PROJECT_ID]?.takeIf(String::isNotBlank),
        ) { "Drive head is missing project identity." }
        val revision = requireNotNull(
            file.appProperties[PROP_REVISION_ID]?.takeIf(String::isNotBlank),
        ) { "Drive head is missing revision identity." }
        val manifestHash = requireNotNull(
            file.appProperties[PROP_MANIFEST_SHA256]?.takeIf { SHA256.matches(it) },
        ) { "Drive head is missing a valid manifest SHA-256." }
        return DrivePublishedHead(
            descriptor = DriveCurrentDescriptor(projectId, revision, manifestHash),
            baseRevisionId = file.appProperties[PROP_BASE_REVISION]?.takeIf(String::isNotBlank),
        )
    }

    private suspend fun getFile(id: String): DriveFileResource = DriveV3Json.parseFile(
        requireSuccess(http.get("$FILES/$id?fields=$FIELDS_ENCODED"), "Drive object verification failed.").body,
    )


    private suspend fun loadManifestResource(
        resource: DriveFileResource,
        expectedProjectId: String,
    ): DriveProjectRevisionManifest {
        val hash = resource.appProperties[PROP_SHA256]
            ?.lowercase()
            ?.takeIf(SHA256::matches)
            ?: error("U8m manifest is missing its content hash.")
        val temporary = kotlin.io.path.createTempFile("guitarlab-u8m-cleanup-", ".manifest").toFile()
        return try {
            http.download("$FILES/${resource.id}?alt=media", temporary)
            require(BackupHashing.sha256(temporary) == hash) {
                "U8m manifest failed SHA-256 verification before cleanup."
            }
            DriveProjectRevisionManifest.parseCanonical(temporary.readBytes()).also { manifest ->
                require(manifest.projectId == expectedProjectId) {
                    "U8m cleanup encountered a manifest owned by another project."
                }
                require(manifest.manifestSha256 == hash) {
                    "U8m cleanup manifest identity mismatch."
                }
            }
        } finally {
            temporary.delete()
        }
    }

    private suspend fun deleteFile(fileId: String) {
        val response = http.delete("$FILES/$fileId")
        if (response.code != 204 && response.code != 404) {
            requireSuccess(response, "U8m Drive cleanup failed.")
        }
    }

    private suspend fun listFiles(query: String): List<DriveFileResource> {
        val output = mutableListOf<DriveFileResource>()
        val seenPageTokens = mutableSetOf<String>()
        var page: String? = null
        do {
            val url = buildString {
                append(FILES)
                    .append("?q=")
                    .append(encode(query))
                    .append("&fields=")
                    .append(PAGE_FIELDS_ENCODED)
                page?.let { append("&pageToken=").append(encode(it)) }
            }
            val parsed = DriveV3Json.parsePage(
                requireSuccess(http.get(url), "Drive object listing failed.").body,
            )
            output += parsed.files
            parsed.nextPageToken?.let { token ->
                require(seenPageTokens.add(token)) {
                    "Drive pagination repeated the same page token."
                }
            }
            page = parsed.nextPageToken
        } while (page != null)
        return output
    }

    private suspend fun query(kind: String, property: Pair<String, String>): String =
        queryKind(kind) +
            " and appProperties has { key='${property.first}' and value='${property.second}' }"

    private suspend fun queryKind(kind: String): String =
        "trashed = false and '${rootFolderId()}' in parents and " +
            "appProperties has { key='$PROP_SCHEMA' and value='$SCHEMA' } and " +
            "appProperties has { key='$PROP_KIND' and value='$kind' }"

    private fun DriveFileResource.verifiedReceipt(): DriveRemoteObjectReceipt? {
        val hash = sha256Checksum?.lowercase()?.takeIf { SHA256.matches(it) } ?: return null
        val bytes = size?.takeIf { it > 0L } ?: return null
        return DriveRemoteObjectReceipt(hash, bytes)
    }

    private fun requireSuccess(response: DriveHttpResponse, message: String): DriveHttpResponse {
        if (!response.successful) throw DriveApiException(response.code, response.body)
        return response.also { require(response.successful) { message } }
    }

    private fun nextOffset(range: String?): Long {
        if (range.isNullOrBlank()) return 0L
        val match = Regex("bytes=0-([0-9]+)").matchEntire(range.trim())
            ?: throw java.io.IOException("Drive returned an invalid resumable upload range.")
        return match.groupValues[1].toLong() + 1L
    }
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
        const val MAX_SESSION_RESTARTS = 2
        val SHA256 = Regex("[0-9a-f]{64}")
        val FIELDS_ENCODED =
            encodeStatic("id,name,size,sha256Checksum,createdTime,appProperties,trashed")
        val PAGE_FIELDS_ENCODED =
            encodeStatic("nextPageToken,files(id,name,size,sha256Checksum,createdTime,appProperties,trashed)")
        fun encodeStatic(value: String) = URLEncoder.encode(value, StandardCharsets.UTF_8.toString())
    }
}
