package studio.guitarlab.app.backup

import java.io.File
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import studio.guitarlab.core.project.BackupCommitRequest

class DriveV3BackupRemoteStoreTest {
    @Test fun commitIsVerifiedAndIdempotent() = runBlocking {
        val bytes = "deterministic package".toByteArray()
        val request = request(bytes)
        val state = FakeDriveState(rootFolderId = "root")
        val api = FakeDriveApi()
        val store = DriveV3BackupRemoteStore(api, state) { 10_000L }

        val first = store.commit(request)
        val second = store.commit(request)

        assertEquals(first.remoteId, second.remoteId)
        assertEquals(request.sha256, first.sha256)
        assertEquals(1, api.uploadStarts)
        assertEquals(1, api.chunkStarts.size)
        assertEquals(0L, api.chunkStarts.single())
        assertEquals(1, api.patchCalls)
        assertEquals(null, state.session(request, 10_000L))
    }

    @Test fun persistedResumableSessionAlwaysUsesServerConfirmedOffset() = runBlocking {
        val bytes = ByteArray(2 * 1024 * 1024) { (it % 251).toByte() }
        val request = request(bytes)
        val state = FakeDriveState(rootFolderId = "root")
        val api = FakeDriveApi()
        val confirmedOffset = 1024 * 1024L
        val session = "https://upload.test/session-old"
        state.saveSession(request, session, 9_000L)
        api.registerSession(session, request, confirmedOffset)
        val store = DriveV3BackupRemoteStore(api, state) { 10_000L }

        val committed = store.commit(request)

        assertEquals(request.revisionId, committed.revisionId)
        assertEquals(0, api.uploadStarts)
        assertEquals(listOf(confirmedOffset), api.chunkStarts)
    }

    @Test fun completedUploadingRevisionIsReconciledInsteadOfUploadedAgain() = runBlocking {
        val bytes = "already arrived".toByteArray()
        val request = request(bytes)
        val state = FakeDriveState(rootFolderId = "root")
        val api = FakeDriveApi()
        api.putBackup("pending", request, bytes, state = "uploading")
        val store = DriveV3BackupRemoteStore(api, state) { 10_000L }

        val found = store.findCommittedVersion(request.projectId, request.revisionId)

        assertNotNull(found)
        assertEquals("pending", found?.remoteId)
        assertEquals(0, api.uploadStarts)
        assertEquals(1, api.patchCalls)
        assertEquals("committed", api.fileProperties("pending")["guitarlabState"])
    }

    @Test fun checksumMismatchNeverBecomesCommitted() = runBlocking {
        val bytes = "must stay trustworthy".toByteArray()
        val request = request(bytes)
        val state = FakeDriveState(rootFolderId = "root")
        val api = FakeDriveApi().apply { corruptNextUploadChecksum = true }
        val store = DriveV3BackupRemoteStore(api, state) { 10_000L }

        try {
            store.commit(request)
            fail("Expected integrity verification failure")
        } catch (expected: IllegalArgumentException) {
            assertTrue(expected.message.orEmpty().contains("SHA-256"))
        }
        assertEquals(0, api.patchCalls)
        assertTrue(api.allFiles().all { it.properties["guitarlabState"] != "committed" })
    }

    private fun request(bytes: ByteArray): BackupCommitRequest {
        val file = File.createTempFile("drive-store-test-", ".guitarlab").apply {
            deleteOnExit()
            writeBytes(bytes)
        }
        return BackupCommitRequest(
            projectId = "project-1",
            projectName = "Projeto Teste",
            projectUpdatedAtEpochMs = 1L,
            backupCreatedAtEpochMs = 2L,
            sizeBytes = bytes.size.toLong(),
            sha256 = sha256(bytes),
            packageFile = file,
            revisionId = "r_1_aaaaaaaaaaaaaaaaaaaaaaaa",
        )
    }

    private fun sha256(bytes: ByteArray): String = MessageDigest.getInstance("SHA-256")
        .digest(bytes)
        .joinToString("") { "%02x".format(it) }
}

private class FakeDriveState(
    override var rootFolderId: String? = null,
) : DriveBackupState {
    private val sessions = mutableMapOf<String, DriveUploadSession>()

    override fun session(request: BackupCommitRequest, nowEpochMs: Long): DriveUploadSession? =
        sessions[key(request.projectId, request.revisionId)]

    override fun saveSession(request: BackupCommitRequest, url: String, nowEpochMs: Long) {
        sessions[key(request.projectId, request.revisionId)] = DriveUploadSession(url, request.sha256, request.sizeBytes, nowEpochMs)
    }

    override fun clearSession(projectId: String, revisionId: String) {
        sessions.remove(key(projectId, revisionId))
    }

    override fun purgeExpiredSessions(nowEpochMs: Long): Int = 0

    private fun key(projectId: String, revisionId: String) = "$projectId\u0000$revisionId"
}

private class FakeDriveApi : DriveV3Api {
    data class FakeFile(
        val id: String,
        val name: String,
        val bytes: ByteArray,
        val properties: MutableMap<String, String>,
        var checksumOverride: String? = null,
        var trashed: Boolean = false,
    )

    private data class Session(
        val properties: MutableMap<String, String>,
        var confirmedOffset: Long = 0L,
        var completedFileId: String? = null,
    )

    private val json = Json { ignoreUnknownKeys = true }
    private val files = linkedMapOf<String, FakeFile>()
    private val sessions = mutableMapOf<String, Session>()
    var uploadStarts = 0
    var patchCalls = 0
    val chunkStarts = mutableListOf<Long>()
    var corruptNextUploadChecksum = false
    private var nextId = 1

    fun registerSession(url: String, request: BackupCommitRequest, confirmedOffset: Long) {
        sessions[url] = Session(properties(request, "uploading"), confirmedOffset)
    }

    fun putBackup(id: String, request: BackupCommitRequest, bytes: ByteArray, state: String) {
        files[id] = FakeFile(id, "test.guitarlab", bytes.copyOf(), properties(request, state))
    }

    fun fileProperties(id: String): Map<String, String> = files.getValue(id).properties.toMap()
    fun allFiles(): List<FakeFile> = files.values.toList()

    override suspend fun get(url: String): DriveHttpResponse {
        if (url.contains("/files/root?")) {
            return ok(fileJson(FakeFile("root", "GuitarLab Studio Backups", ByteArray(0), mutableMapOf("guitarlabKind" to "backup-root"))))
        }
        if (url.contains("/drive/v3/files?")) {
            val query = decodeQueryParameter(url, "q")
            val matched = files.values.filter { file -> matchesQuery(file, query) }
            val body = buildJsonObject {
                put("files", buildJsonArray { matched.forEach { add(json.parseToJsonElement(fileJson(it))) } })
            }.toString()
            return ok(body)
        }
        val id = url.substringAfter("/files/").substringBefore('?')
        val file = files[id] ?: return DriveHttpResponse(404, "", emptyMap())
        return ok(fileJson(file))
    }

    override suspend fun postJson(url: String, body: String, extraHeaders: Map<String, String>): DriveHttpResponse {
        if (url.contains("/upload/drive/v3/files")) {
            uploadStarts++
            val sessionUrl = "https://upload.test/session-${uploadStarts}"
            sessions[sessionUrl] = Session(parseProperties(body))
            return DriveHttpResponse(200, "", mapOf("Location" to listOf(sessionUrl)))
        }
        error("Unexpected POST $url")
    }

    override suspend fun patchJson(url: String, body: String): DriveHttpResponse {
        patchCalls++
        val id = url.substringAfter("/files/").substringBefore('?')
        val file = files.getValue(id)
        file.properties.putAll(parseProperties(body))
        return ok(fileJson(file))
    }

    override suspend fun delete(url: String): DriveHttpResponse {
        val id = url.substringAfter("/files/").substringBefore('?')
        files.remove(id)
        return DriveHttpResponse(204, "", emptyMap())
    }

    override suspend fun uploadStatus(sessionUrl: String, totalBytes: Long): DriveHttpResponse {
        val session = sessions[sessionUrl] ?: return DriveHttpResponse(404, "", emptyMap())
        session.completedFileId?.let { return ok(fileJson(files.getValue(it))) }
        val headers = if (session.confirmedOffset > 0L) {
            mapOf("Range" to listOf("bytes=0-${session.confirmedOffset - 1}"))
        } else emptyMap()
        return DriveHttpResponse(308, "", headers)
    }

    override suspend fun uploadChunk(
        sessionUrl: String,
        file: File,
        start: Long,
        length: Int,
        totalBytes: Long,
    ): DriveHttpResponse {
        chunkStarts += start
        val session = sessions.getValue(sessionUrl)
        require(start == session.confirmedOffset) { "client replayed an unconfirmed offset" }
        session.confirmedOffset += length
        if (session.confirmedOffset < totalBytes) {
            return DriveHttpResponse(308, "", mapOf("Range" to listOf("bytes=0-${session.confirmedOffset - 1}")))
        }
        val id = "file-${nextId++}"
        val bytes = file.readBytes()
        val remote = FakeFile(id, "test.guitarlab", bytes, session.properties.toMutableMap())
        if (corruptNextUploadChecksum) {
            remote.checksumOverride = "0".repeat(64)
            corruptNextUploadChecksum = false
        }
        files[id] = remote
        session.completedFileId = id
        return ok(fileJson(remote))
    }

    override suspend fun download(url: String, destination: File) {
        val id = url.substringAfter("/files/").substringBefore('?')
        destination.writeBytes(files.getValue(id).bytes)
    }

    private fun matchesQuery(file: FakeFile, query: String): Boolean {
        if (file.trashed) return false
        val requirements = Regex("key='([^']+)' and value='([^']*)'")
            .findAll(query)
            .map { it.groupValues[1] to it.groupValues[2].replace("\\'", "'").replace("\\\\", "\\") }
            .toList()
        return requirements.all { (key, value) -> file.properties[key] == value }
    }

    private fun parseProperties(body: String): MutableMap<String, String> {
        val obj = json.parseToJsonElement(body).jsonObject
        return obj["appProperties"]?.jsonObject?.mapNotNull { (key, value) ->
            value.jsonPrimitive.contentOrNull?.let { key to it }
        }?.toMap()?.toMutableMap() ?: mutableMapOf()
    }

    private fun fileJson(file: FakeFile): String = buildJsonObject {
        put("id", file.id)
        put("name", file.name)
        put("size", file.bytes.size.toString())
        put("sha256Checksum", file.checksumOverride ?: sha256(file.bytes))
        put("trashed", file.trashed)
        put("appProperties", buildJsonObject { file.properties.forEach { (key, value) -> put(key, value) } })
    }.toString()

    private fun properties(request: BackupCommitRequest, state: String): MutableMap<String, String> = linkedMapOf(
        "guitarlabKind" to "project-backup",
        "guitarlabFormat" to "2",
        "guitarlabState" to state,
        "projectId" to request.projectId,
        "projectName" to request.projectName,
        "revisionId" to request.revisionId,
        "projectUpdatedAt" to request.projectUpdatedAtEpochMs.toString(),
        "backupCreatedAt" to request.backupCreatedAtEpochMs.toString(),
        "sizeBytes" to request.sizeBytes.toString(),
        "sha256" to request.sha256,
    )

    private fun decodeQueryParameter(url: String, name: String): String {
        val raw = url.substringAfter("?", "").split('&')
            .firstOrNull { it.substringBefore('=') == name }
            ?.substringAfter('=', "")
            .orEmpty()
        return URLDecoder.decode(raw, StandardCharsets.UTF_8.name())
    }

    private fun ok(body: String) = DriveHttpResponse(200, body, emptyMap())

    private fun sha256(bytes: ByteArray): String = MessageDigest.getInstance("SHA-256")
        .digest(bytes)
        .joinToString("") { "%02x".format(it) }
}
