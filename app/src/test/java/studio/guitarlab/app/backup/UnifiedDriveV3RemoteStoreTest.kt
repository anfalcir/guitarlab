package studio.guitarlab.app.backup

import java.io.File
import java.io.IOException
import java.nio.file.Files
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
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
        val store = UnifiedDriveV3RemoteStore(api, rootFolderId = { "root" })
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
        val store = UnifiedDriveV3RemoteStore(api, rootFolderId = { "root" })
        api.getResponses += response(200, page(file("asset-id", payload.size.toLong(), hash)))
        api.downloadBytes["asset-id"] = payload
        val destination = Files.createTempFile("u8c-download", ".bin").toFile()
        store.downloadAsset(asset, destination)
        assertEquals("audio", destination.readText())
    }

    @Test fun listsAndPublishesAppendOnlyHeads() = runBlocking {
        val api = FakeApi()
        val store = UnifiedDriveV3RemoteStore(api, rootFolderId = { "root" })
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
        val receipt = UnifiedDriveV3RemoteStore(api, rootFolderId = { "root" }).findAsset(hash)
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
        val receipt = UnifiedDriveV3RemoteStore(api, rootFolderId = { "root" })
            .uploadAsset(DriveLocalAsset(DriveAssetObject(hash, payload.length()), payload))
        assertEquals(hash, receipt.sha256)
        assertEquals(payload.length(), receipt.sizeBytes)
        assertEquals(1, api.uploadCalls)
    }


    @Test fun persistedResumableSessionContinuesFromServerRangeAfterProcessRestart() = runBlocking {
        val payload = kotlin.io.path.createTempFile("u8k-resume", ".bin").toFile().apply {
            writeText("payload")
        }
        val hash = BackupHashing.sha256(payload)
        val state = MemoryUploadState().apply {
            save(hash, payload.length(), "persisted-session", 1L)
        }
        val api = FakeApi().apply {
            getResponses += response(200, page())
            statusResponses += DriveHttpResponse(
                308,
                "",
                mapOf("Range" to listOf("bytes=0-2")),
            )
            uploadResponse = response(
                201,
                file("asset", payload.length(), hash),
            )
        }

        val receipt = UnifiedDriveV3RemoteStore(api, rootFolderId = { "root" }, uploadState = state)
            .uploadAsset(
                DriveLocalAsset(
                    DriveAssetObject(hash, payload.length()),
                    payload,
                ),
            )

        assertEquals(hash, receipt.sha256)
        assertEquals(listOf(3L), api.uploadStarts)
        assertEquals(0, api.postCalls)
        assertEquals(null, state.session(hash, payload.length(), 2L))
    }

    @Test fun expiredResumableSessionIsReplacedWithoutLosingTheFrozenObject() = runBlocking {
        val payload = kotlin.io.path.createTempFile("u8k-expired", ".bin").toFile().apply {
            writeText("payload")
        }
        val hash = BackupHashing.sha256(payload)
        val state = MemoryUploadState().apply {
            save(hash, payload.length(), "expired-session", 1L)
        }
        val api = FakeApi().apply {
            getResponses += response(200, page())
            statusResponses += response(404, "")
            postResponse = DriveHttpResponse(
                200,
                "",
                mapOf("Location" to listOf("new-session")),
            )
            uploadResponse = response(
                201,
                file("asset", payload.length(), hash),
            )
        }

        UnifiedDriveV3RemoteStore(api, rootFolderId = { "root" }, uploadState = state)
            .uploadAsset(
                DriveLocalAsset(
                    DriveAssetObject(hash, payload.length()),
                    payload,
                ),
            )

        assertEquals(1, api.postCalls)
        assertEquals(listOf(0L), api.uploadStarts)
        assertEquals(null, state.session(hash, payload.length(), 2L))
    }

    @Test fun garbageCollectionCatalogUsesVerifiedAssetIdentityAndNewestDuplicateAge() = runBlocking {
        val hash = "d".repeat(64)
        val props = mapOf(
            "glSchema" to "guitarlab-unified-drive-v3",
            "glKind" to "asset",
            "glSha256" to hash,
        )
        val api = FakeApi().apply {
            getResponses += response(
                200,
                page(
                    file(
                        "old",
                        7,
                        hash,
                        props,
                        "2026-09-01T00:00:00Z",
                    ),
                    file(
                        "new",
                        7,
                        hash,
                        props,
                        "2026-09-20T00:00:00Z",
                    ),
                ),
            )
        }

        val candidates = UnifiedDriveV3RemoteStore(api, rootFolderId = { "root" }).listAssets()

        assertEquals(1, candidates.size)
        assertEquals(hash, candidates.single().asset.sha256)
        assertEquals(
            java.time.Instant.parse("2026-09-20T00:00:00Z").toEpochMilli(),
            candidates.single().uploadedAtEpochMs,
        )
    }


    @Test fun lostChunkResponseQueriesServerRangeBeforeRetryingBytes() = runBlocking {
        val payload = kotlin.io.path.createTempFile("u8l-lost-chunk", ".bin").toFile().apply {
            writeText("payload")
        }
        val hash = BackupHashing.sha256(payload)
        val api = FakeApi().apply {
            getResponses += response(200, page())
            postResponse = DriveHttpResponse(
                200,
                "",
                mapOf("Location" to listOf("session")),
            )
            uploadFailuresRemaining = 1
            statusResponses += DriveHttpResponse(
                308,
                "",
                mapOf("Range" to listOf("bytes=0-2")),
            )
            uploadResponse = response(
                201,
                file("asset", payload.length(), hash),
            )
        }

        val receipt = UnifiedDriveV3RemoteStore(
            api,
            rootFolderId = { "root" },
            retryDelay = {},
        ).uploadAsset(
            DriveLocalAsset(
                DriveAssetObject(hash, payload.length()),
                payload,
            ),
        )

        assertEquals(hash, receipt.sha256)
        assertEquals(listOf(0L, 3L), api.uploadStarts)
        assertEquals(2, api.uploadCalls)
    }

    @Test fun lostFinalChunkResponseUsesCommittedStatusWithoutBlindReplay() = runBlocking {
        val payload = kotlin.io.path.createTempFile("u8l-lost-final", ".bin").toFile().apply {
            writeText("payload")
        }
        val hash = BackupHashing.sha256(payload)
        val api = FakeApi().apply {
            getResponses += response(200, page())
            postResponse = DriveHttpResponse(
                200,
                "",
                mapOf("Location" to listOf("session")),
            )
            uploadFailuresRemaining = 1
            statusResponses += response(
                201,
                file("asset", payload.length(), hash),
            )
        }

        val receipt = UnifiedDriveV3RemoteStore(
            api,
            rootFolderId = { "root" },
            retryDelay = {},
        ).uploadAsset(
            DriveLocalAsset(
                DriveAssetObject(hash, payload.length()),
                payload,
            ),
        )

        assertEquals(hash, receipt.sha256)
        assertEquals(1, api.uploadCalls)
        assertEquals(listOf(0L), api.uploadStarts)
    }

    @Test fun transientStatusProbeRetriesBeforeResumingUpload() = runBlocking {
        val payload = kotlin.io.path.createTempFile("u8l-status-retry", ".bin").toFile().apply {
            writeText("payload")
        }
        val hash = BackupHashing.sha256(payload)
        val delays = mutableListOf<Long>()
        val api = FakeApi().apply {
            getResponses += response(200, page())
            postResponse = DriveHttpResponse(
                200,
                "",
                mapOf("Location" to listOf("session")),
            )
            uploadFailuresRemaining = 1
            statusResponses += response(503, "backend unavailable")
            statusResponses += DriveHttpResponse(
                308,
                "",
                mapOf("Range" to listOf("bytes=0-2")),
            )
            uploadResponse = response(
                201,
                file("asset", payload.length(), hash),
            )
        }

        UnifiedDriveV3RemoteStore(
            api,
            rootFolderId = { "root" },
            retryDelay = { delays += it },
        ).uploadAsset(
            DriveLocalAsset(
                DriveAssetObject(hash, payload.length()),
                payload,
            ),
        )

        assertEquals(1, delays.size)
        assertEquals(listOf(0L, 3L), api.uploadStarts)
    }

    @Test fun malformedResumableRangeFailsClosed() = runBlocking {
        val payload = kotlin.io.path.createTempFile("u8l-range", ".bin").toFile().apply {
            writeText("payload")
        }
        val hash = BackupHashing.sha256(payload)
        val api = FakeApi().apply {
            getResponses += response(200, page())
            postResponse = DriveHttpResponse(
                200,
                "",
                mapOf("Location" to listOf("session")),
            )
            uploadResponse = DriveHttpResponse(
                308,
                "",
                mapOf("Range" to listOf("bytes=garbage")),
            )
        }

        assertFailsWith<IOException> {
            UnifiedDriveV3RemoteStore(
                api,
                rootFolderId = { "root" },
                retryDelay = {},
            ).uploadAsset(
                DriveLocalAsset(
                    DriveAssetObject(hash, payload.length()),
                    payload,
                ),
            )
        }
    }

    @Test fun paginationConsumesUniqueTokensAndRejectsCycles() = runBlocking {
        val first = headProps("p", "r1", "")
        val second = headProps("p", "r2", "r1")
        val goodApi = FakeApi().apply {
            getResponses += response(
                200,
                pageWithToken("n2", file("head-1", props = first)),
            )
            getResponses += response(
                200,
                page(file("head-2", props = second)),
            )
        }

        val heads = UnifiedDriveV3RemoteStore(
            goodApi,
            rootFolderId = { "root" },
        ).listHeads("p")

        assertEquals(listOf("r1", "r2"), heads.map { it.descriptor.revisionId })
        assertEquals(2, goodApi.getUrls.size)
        assertTrue(goodApi.getUrls.last().contains("pageToken=n2"))

        val cyclicApi = FakeApi().apply {
            getResponses += response(
                200,
                pageWithToken("same", file("head-1", props = first)),
            )
            getResponses += response(
                200,
                pageWithToken("same", file("head-2", props = second)),
            )
        }

        assertFailsWith<IllegalArgumentException> {
            UnifiedDriveV3RemoteStore(
                cyclicApi,
                rootFolderId = { "root" },
            ).listHeads("p")
        }
    }

    private class MemoryUploadState : UnifiedDriveUploadState {
        private val sessions = mutableMapOf<String, UnifiedDriveUploadSession>()

        override fun session(
            sha256: String,
            sizeBytes: Long,
            nowEpochMs: Long,
        ): UnifiedDriveUploadSession? =
            sessions[sha256]?.takeIf { it.sizeBytes == sizeBytes }

        override fun save(
            sha256: String,
            sizeBytes: Long,
            url: String,
            nowEpochMs: Long,
        ) {
            sessions[sha256] =
                UnifiedDriveUploadSession(url, sha256, sizeBytes, nowEpochMs)
        }

        override fun clear(sha256: String) {
            sessions.remove(sha256)
        }

        override fun clearAll() {
            sessions.clear()
        }
    }

    private fun headProps(project: String, revision: String, base: String) = mapOf(
        "glSchema" to "guitarlab-unified-drive-v3", "glKind" to "head",
        "glProjectId" to project, "glRevisionId" to revision, "glBaseRevisionId" to base,
        "glManifestSha256" to "a".repeat(64),
    )

    private fun file(
        id: String,
        size: Long? = null,
        hash: String? = null,
        props: Map<String, String> = emptyMap(),
        createdTime: String? = null,
    ): String =
        buildString {
            append("{\"id\":\"").append(id).append("\"")
            size?.let { append(",\"size\":\"").append(it).append("\"") }
            hash?.let { append(",\"sha256Checksum\":\"").append(it).append("\"") }
            createdTime?.let { append(",\"createdTime\":\"").append(it).append("\"") }
            if (props.isNotEmpty()) append(",\"appProperties\":{").append(props.entries.joinToString(",") { "\"${it.key}\":\"${it.value}\"" }).append('}')
            append('}')
        }

    private fun page(vararg files: String) =
        "{\"files\":[${files.joinToString(",")}] }"

    private fun pageWithToken(token: String, vararg files: String) =
        "{\"nextPageToken\":\"$token\",\"files\":[${files.joinToString(",")}] }"
    private fun response(code: Int, body: String) = DriveHttpResponse(code, body, emptyMap())

    private class FakeApi : DriveV3Api {
        val getResponses = ArrayDeque<DriveHttpResponse>()
        val getUrls = mutableListOf<String>()
        var postResponse = DriveHttpResponse(500, "unset", emptyMap())
        var uploadResponse = DriveHttpResponse(500, "unset", emptyMap())
        val statusResponses = ArrayDeque<DriveHttpResponse>()
        var uploadCalls = 0
        var uploadFailuresRemaining = 0
        var postCalls = 0
        val uploadStarts = mutableListOf<Long>()
        val downloadBytes = mutableMapOf<String, ByteArray>()
        var lastPostBody: String? = null
        override suspend fun get(url: String): DriveHttpResponse {
            getUrls += url
            return getResponses.removeFirst()
        }
        override suspend fun postJson(url: String, body: String, extraHeaders: Map<String, String>): DriveHttpResponse {
            postCalls++
            lastPostBody = body
            return postResponse
        }
        override suspend fun patchJson(url: String, body: String) = error("unused")
        override suspend fun delete(url: String) = error("unused")
        override suspend fun uploadStatus(
            sessionUrl: String,
            totalBytes: Long,
        ): DriveHttpResponse = statusResponses.removeFirst()

        override suspend fun uploadChunk(
            sessionUrl: String,
            file: File,
            start: Long,
            length: Int,
            totalBytes: Long,
        ): DriveHttpResponse {
            uploadCalls++
            uploadStarts += start
            if (uploadFailuresRemaining > 0) {
                uploadFailuresRemaining--
                throw IOException("lost upload response")
            }
            return uploadResponse
        }
        override suspend fun download(url: String, destination: File) {
            val id = url.substringAfter("/files/").substringBefore('?')
            destination.writeBytes(downloadBytes[id] ?: error("No fake download for $id"))
        }
    }
}
