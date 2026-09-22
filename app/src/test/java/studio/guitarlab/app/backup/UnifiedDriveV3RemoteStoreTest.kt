package studio.guitarlab.app.backup

import java.io.File
import java.nio.file.Files
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking
import org.junit.Test
import studio.guitarlab.core.project.BackupHashing
import studio.guitarlab.core.project.DriveAssetObject
import studio.guitarlab.core.project.DriveCurrentDescriptor
import studio.guitarlab.core.project.DriveLocalAsset
import studio.guitarlab.core.project.DriveProjectFileEntry
import studio.guitarlab.core.project.DriveProjectRevisionManifest
import studio.guitarlab.core.project.DrivePublishedHead

class UnifiedDriveV3RemoteStoreTest {
    @Test fun canonicalManifestCanBeDownloadedAsRestoreSource() = runBlocking {
        val state = DriveAssetObject("a".repeat(64), 5)
        val media = DriveAssetObject("b".repeat(64), 5)
        val manifest = DriveProjectRevisionManifest(
            "project", "revision", null, 1, "c".repeat(64),
            listOf(state, media),
            projectStateAsset = state,
            fileEntries = listOf(DriveProjectFileEntry("media/source.wav", media)),
        )
        val api = FakeApi()
        val store = UnifiedDriveV3RemoteStore(api) { "root" }
        api.getResponses += response(
            200,
            page(file("manifest-id", manifest.canonicalBytes().size.toLong(), manifest.manifestSha256)),
        )
        api.downloadBytes["manifest-id"] = manifest.canonicalBytes()
        val restored = store.loadManifest(DriveCurrentDescriptor("project", "revision", manifest.manifestSha256))
        assertEquals(manifest, restored)
    }

    @Test fun verifiedAssetDownloadUsesExactContentIdentity() = runBlocking {
        val payload = "audio".toByteArray()
        val hash = java.security.MessageDigest.getInstance("SHA-256")
            .digest(payload).joinToString("") { "%02x".format(it) }
        val asset = DriveAssetObject(hash, payload.size.toLong())
        val api = FakeApi()
        val store = UnifiedDriveV3RemoteStore(api) { "root" }
        api.getResponses += response(200, page(file("asset-id", payload.size.toLong(), hash)))
        api.downloadBytes["asset-id"] = payload
        val destination = Files.createTempFile("u8c-download", ".bin").toFile()
        store.downloadAsset(asset, destination)
        assertEquals("audio", destination.readText())
    }

    @Test fun listsAndPublishesAppendOnlyHeads() = runBlocking {
        val api = FakeApi()
        val store = UnifiedDriveV3RemoteStore(api) { "root" }
        api.getResponses += response(200, page())
        api.postResponse = response(200, file("head-1", props = headProps("p", "r1", "")))
        store.publishHead(DrivePublishedHead(DriveCurrentDescriptor("p", "r1", "a".repeat(64)), null))
        assertTrue(api.lastPostBody.orEmpty().contains("guitarlab-unified-drive-v3"))
        api.getResponses += response(200, page(file("head-1", props = headProps("p", "r1", ""))))
        assertEquals("r1", store.listHeads("p").single().descriptor.revisionId)
    }

    @Test fun existingAssetIsReturnedWithoutUpload() = runBlocking {
        val hash = "b".repeat(64)
        val api = FakeApi().apply { getResponses += response(200, page(file("asset", 7, hash))) }
        val receipt = UnifiedDriveV3RemoteStore(api) { "root" }.findAsset(hash)
        assertEquals(hash, receipt?.sha256)
        assertEquals(7, receipt?.sizeBytes)
    }

    @Test fun missingAssetUsesResumableUploadAndVerifiesServerIdentity() = runBlocking {
        val payload = kotlin.io.path.createTempFile("u8c", ".bin").toFile().apply { writeText("payload") }
        val hash = BackupHashing.sha256(payload)
        val api = FakeApi().apply {
            getResponses += response(200, page())
            postResponse = DriveHttpResponse(200, "", mapOf("Location" to listOf("session")))
            uploadResponse = response(201, file("asset", payload.length(), hash))
        }
        val receipt = UnifiedDriveV3RemoteStore(api) { "root" }
            .uploadAsset(DriveLocalAsset(DriveAssetObject(hash, payload.length()), payload))
        assertEquals(hash, receipt.sha256)
        assertEquals(payload.length(), receipt.sizeBytes)
        assertEquals(1, api.uploadCalls)
    }

    private fun headProps(project: String, revision: String, base: String) = mapOf(
        "glSchema" to "guitarlab-unified-drive-v3", "glKind" to "head",
        "glProjectId" to project, "glRevisionId" to revision, "glBaseRevisionId" to base,
        "glManifestSha256" to "a".repeat(64),
    )

    private fun file(id: String, size: Long? = null, hash: String? = null, props: Map<String, String> = emptyMap()): String =
        buildString {
            append("{\"id\":\"").append(id).append("\"")
            size?.let { append(",\"size\":\"").append(it).append("\"") }
            hash?.let { append(",\"sha256Checksum\":\"").append(it).append("\"") }
            if (props.isNotEmpty()) append(",\"appProperties\":{").append(props.entries.joinToString(",") { "\"${it.key}\":\"${it.value}\"" }).append('}')
            append('}')
        }

    private fun page(vararg files: String) = "{\"files\":[${files.joinToString(",")}] }"
    private fun response(code: Int, body: String) = DriveHttpResponse(code, body, emptyMap())

    private class FakeApi : DriveV3Api {
        val getResponses = ArrayDeque<DriveHttpResponse>()
        var postResponse = DriveHttpResponse(500, "unset", emptyMap())
        var uploadResponse = DriveHttpResponse(500, "unset", emptyMap())
        var uploadCalls = 0
        val downloadBytes = mutableMapOf<String, ByteArray>()
        var lastPostBody: String? = null
        override suspend fun get(url: String) = getResponses.removeFirst()
        override suspend fun postJson(url: String, body: String, extraHeaders: Map<String, String>): DriveHttpResponse {
            lastPostBody = body
            return postResponse
        }
        override suspend fun patchJson(url: String, body: String) = error("unused")
        override suspend fun delete(url: String) = error("unused")
        override suspend fun uploadStatus(sessionUrl: String, totalBytes: Long) = error("unused")
        override suspend fun uploadChunk(sessionUrl: String, file: File, start: Long, length: Int, totalBytes: Long): DriveHttpResponse {
            uploadCalls++
            return uploadResponse
        }
        override suspend fun download(url: String, destination: File) {
            val id = url.substringAfter("/files/").substringBefore('?')
            destination.writeBytes(downloadBytes[id] ?: error("No fake download for $id"))
        }
    }
}
