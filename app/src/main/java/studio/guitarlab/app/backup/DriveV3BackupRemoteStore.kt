package studio.guitarlab.app.backup

import android.content.Context
import java.io.File
import java.time.Instant
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import studio.guitarlab.core.project.BackupCommitRequest
import studio.guitarlab.core.project.BackupVersionDescriptor
import studio.guitarlab.core.project.ProjectBackupRemoteStore

/**
 * Native Google Drive API v3 backend for GuitarLab backups.
 *
 * H28 identities remain authoritative. Each logical backup revision is a single .guitarlab Drive
 * file whose appProperties carry immutable project/revision/integrity metadata. A revision becomes
 * visible to restore only after Drive's own size + SHA-256 metadata match the locally generated
 * package and the state is atomically patched to "committed".
 */
internal class DriveV3BackupRemoteStore internal constructor(
    private val http: DriveV3Api,
    private val state: DriveBackupState,
    private val nowEpochMs: () -> Long,
) : ProjectBackupRemoteStore {
    constructor(
        context: Context,
        tokenProvider: DriveAccessTokenProvider = GoogleDriveAccessTokenProvider(context),
        nowEpochMs: () -> Long = System::currentTimeMillis,
    ) : this(
        http = DriveV3HttpClient(tokenProvider),
        state = DriveBackupStateStore(context.applicationContext),
        nowEpochMs = nowEpochMs,
    )


    override suspend fun listCommittedVersions(projectId: String?): List<BackupVersionDescriptor> = withContext(Dispatchers.IO) {
        ensureRootFolder()
        listFiles(
            query = buildBackupQuery(stateValue = STATE_COMMITTED, projectId = projectId),
        ).mapNotNull(::descriptorOrNull)
            .sortedWith(compareByDescending<BackupVersionDescriptor> { it.projectUpdatedAtEpochMs }.thenByDescending { it.remoteId })
    }

    override suspend fun findCommittedVersion(projectId: String, revisionId: String): BackupVersionDescriptor? = withContext(Dispatchers.IO) {
        ensureRootFolder()
        val committed = listFiles(buildRevisionQuery(projectId, revisionId, STATE_COMMITTED))
            .mapNotNull(::descriptorOrNull)
            .firstOrNull()
        if (committed != null) return@withContext committed

        // If upload content completed but the app died before final metadata patch, reconcile it
        // into a committed revision rather than sending a duplicate package.
        val pending = listFiles(buildRevisionQuery(projectId, revisionId, STATE_UPLOADING))
        for (candidate in pending) {
            val recovered = finalizeIfVerified(candidate)
            if (recovered != null) return@withContext recovered
        }
        null
    }

    override suspend fun commit(request: BackupCommitRequest): BackupVersionDescriptor = withContext(Dispatchers.IO) {
        require(request.packageFile.isFile && request.packageFile.length() == request.sizeBytes) {
            "Pacote local de backup divergente antes do upload."
        }
        val rootId = ensureRootFolder()

        listFiles(buildRevisionQuery(request.projectId, request.revisionId, STATE_COMMITTED))
            .mapNotNull(::descriptorOrNull)
            .firstOrNull()
            ?.let { existing ->
                require(existing.sizeBytes == request.sizeBytes && existing.sha256.equals(request.sha256, true)) {
                    "A mesma revisão já existe no Drive com bytes diferentes."
                }
                state.clearSession(request.projectId, request.revisionId)
                return@withContext existing
            }

        // Reconcile a completed-but-not-committed file from a previous interrupted run.
        listFiles(buildRevisionQuery(request.projectId, request.revisionId, STATE_UPLOADING)).forEach { pending ->
            val verified = getFile(pending.id)
            if (verified.size == request.sizeBytes && verified.sha256Checksum.equals(request.sha256, true)) {
                state.clearSession(request.projectId, request.revisionId)
                return@withContext markCommitted(verified, request)
            }
        }

        var sessionUrl = state.session(request)?.url
        var startOffset = 0L
        if (sessionUrl != null) {
            when (val status = queryUploadStatus(sessionUrl!!, request.sizeBytes)) {
                null -> {
                    // A persisted session has an unknown server offset. Never replay from byte zero.
                    throw java.io.IOException("Não foi possível confirmar o ponto de retomada do upload no Google Drive.")
                }
                else -> when (status.code) {
                    200, 201 -> {
                        val uploaded = DriveV3Json.parseFile(status.body)
                        state.clearSession(request.projectId, request.revisionId)
                        return@withContext verifyAndCommit(uploaded.id, request)
                    }
                    308 -> startOffset = nextOffset(status.header("Range"))
                    404 -> {
                        state.clearSession(request.projectId, request.revisionId)
                        sessionUrl = null
                    }
                    else -> throw DriveApiException(status.code, status.body)
                }
            }
        }

        if (sessionUrl == null) {
            sessionUrl = initiateResumableUpload(rootId, request)
            state.saveSession(request, sessionUrl!!, nowEpochMs())
            startOffset = 0L
        }

        val uploaded = uploadResumably(sessionUrl!!, request, startOffset)
        state.clearSession(request.projectId, request.revisionId)
        verifyAndCommit(uploaded.id, request)
    }

    override suspend fun copyPackage(version: BackupVersionDescriptor, destination: File): Unit = withContext(Dispatchers.IO) {
        http.download("$DRIVE_FILES/${version.remoteId}?alt=media", destination)
    }

    override suspend fun deleteVersion(version: BackupVersionDescriptor): Boolean = withContext(Dispatchers.IO) {
        val response = http.delete("$DRIVE_FILES/${version.remoteId}")
        when (response.code) {
            204 -> true
            404 -> true // already absent is idempotent success
            else -> {
                requireSuccessful(response, "Falha ao remover backup antigo do Drive.")
                true
            }
        }
    }

    override suspend fun cleanupIncomplete(olderThanEpochMs: Long): Int = withContext(Dispatchers.IO) {
        state.purgeExpiredSessions(nowEpochMs())
        ensureRootFolder()
        var deleted = 0
        listFiles(buildBackupQuery(STATE_UPLOADING, null)).forEach { file ->
            val created = file.appProperties[PROP_BACKUP_CREATED]?.toLongOrNull()
                ?: file.createdTime?.let { runCatching { Instant.parse(it).toEpochMilli() }.getOrNull() }
                ?: return@forEach
            if (created < olderThanEpochMs) {
                val response = http.delete("$DRIVE_FILES/${file.id}")
                if (response.code == 204 || response.code == 404) deleted++
            }
        }
        deleted
    }

    /** Verifies OAuth + Drive read/create/get/delete and returns a user-facing account label. */
    suspend fun probeReadWriteDelete(): String = withContext(Dispatchers.IO) {
        val rootId = ensureRootFolder()
        val probeName = ".guitarlab-probe-${UUID.randomUUID()}"
        val create = requireSuccessful(
            http.postJson(
                "$DRIVE_FILES?fields=id,name",
                DriveV3Json.metadata(
                    name = probeName,
                    mimeType = MIME_BINARY,
                    parents = listOf(rootId),
                    appProperties = mapOf(PROP_KIND to KIND_PROBE),
                ),
            ),
            "Não foi possível criar o arquivo de teste no Google Drive.",
        )
        val probe = DriveV3Json.parseFile(create.body)
        var readFailure: Exception? = null
        try {
            requireSuccessful(
                http.get("$DRIVE_FILES/${probe.id}?fields=id,name"),
                "Não foi possível reler o arquivo de teste no Google Drive.",
            )
        } catch (failure: Exception) {
            readFailure = failure
        }

        val deleteFailure = try {
            withContext(NonCancellable) {
                requireSuccessful(
                    http.delete("$DRIVE_FILES/${probe.id}"),
                    "Não foi possível remover o arquivo de teste do Google Drive.",
                )
            }
            null
        } catch (failure: Exception) {
            failure
        }

        readFailure?.let { failure ->
            deleteFailure?.let(failure::addSuppressed)
            throw failure
        }
        deleteFailure?.let { throw it }
        accountLabel()
    }

    private suspend fun accountLabel(): String {
        val response = requireSuccessful(
            http.get("$DRIVE_ABOUT?fields=user(displayName,emailAddress)"),
            "Não foi possível identificar a conta do Google Drive.",
        )
        val raw = response.body
        val email = Regex("\\\"emailAddress\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"").find(raw)?.groupValues?.getOrNull(1)
        val name = Regex("\\\"displayName\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"").find(raw)?.groupValues?.getOrNull(1)
        return email ?: name ?: "Google Drive conectado"
    }

    private suspend fun ensureRootFolder(): String {
        state.rootFolderId?.let { cached ->
            val check = http.get("$DRIVE_FILES/$cached?fields=id,trashed,appProperties")
            if (check.code == 200) {
                val folder = DriveV3Json.parseFile(check.body)
                if (folder.trashed != true && folder.appProperties[PROP_KIND] == KIND_ROOT) return cached
                state.rootFolderId = null
            } else if (check.code == 404) {
                state.rootFolderId = null
            } else {
                requireSuccessful(check, "Falha ao validar a pasta do GuitarLab no Drive.")
            }
        }

        val query = "trashed = false and mimeType = '$MIME_FOLDER' and appProperties has { key='$PROP_KIND' and value='$KIND_ROOT' }"
        listFiles(query).firstOrNull()?.id?.let { found ->
            state.rootFolderId = found
            return found
        }

        val created = requireSuccessful(
            http.postJson(
                "$DRIVE_FILES?fields=id,name,appProperties",
                DriveV3Json.metadata(
                    name = ROOT_FOLDER_NAME,
                    mimeType = MIME_FOLDER,
                    appProperties = mapOf(PROP_KIND to KIND_ROOT, PROP_FORMAT to FORMAT_VALUE),
                ),
            ),
            "Não foi possível criar a pasta do GuitarLab no Google Drive.",
        )
        return DriveV3Json.parseFile(created.body).id.also { state.rootFolderId = it }
    }

    private suspend fun initiateResumableUpload(rootId: String, request: BackupCommitRequest): String {
        val response = requireSuccessful(
            http.postJson(
                "$DRIVE_UPLOAD_FILES?uploadType=resumable&fields=$FILE_FIELDS_ENCODED",
                DriveV3Json.metadata(
                    name = safeFileName(request.projectName, request.revisionId),
                    mimeType = MIME_PACKAGE,
                    parents = listOf(rootId),
                    appProperties = properties(request, STATE_UPLOADING),
                ),
                extraHeaders = mapOf(
                    "X-Upload-Content-Type" to MIME_PACKAGE,
                    "X-Upload-Content-Length" to request.sizeBytes.toString(),
                ),
            ),
            "Não foi possível iniciar o upload retomável no Google Drive.",
        )
        return response.header("Location")
            ?: error("O Google Drive não retornou a sessão retomável do upload.")
    }

    private suspend fun uploadResumably(
        sessionUrl: String,
        request: BackupCommitRequest,
        initialOffset: Long,
    ): DriveFileResource {
        var offset = initialOffset.coerceIn(0L, request.sizeBytes)
        var recoveries = 0
        while (offset < request.sizeBytes) {
            val length = minOf(CHUNK_BYTES.toLong(), request.sizeBytes - offset).toInt()
            val response = try {
                http.uploadChunk(sessionUrl, request.packageFile, offset, length, request.sizeBytes)
            } catch (error: Throwable) {
                if (error is CancellationException) throw error
                if (recoveries >= DriveRetryPolicy.MAX_ATTEMPTS) throw error
                delay(DriveRetryPolicy.delayMs(recoveries++))
                val status = queryUploadStatus(sessionUrl, request.sizeBytes)
                    ?: throw java.io.IOException("Não foi possível confirmar o estado do upload após a interrupção.")
                when (status.code) {
                    200, 201 -> return DriveV3Json.parseFile(status.body)
                    308 -> {
                        offset = nextOffset(status.header("Range"))
                        continue
                    }
                    404 -> error("A sessão retomável do Google Drive expirou; o próximo backup reiniciará o upload com segurança.")
                    else -> throw DriveApiException(status.code, status.body)
                }
            }

            when (response.code) {
                200, 201 -> return DriveV3Json.parseFile(response.body)
                308 -> {
                    offset = nextOffset(response.header("Range"))
                    recoveries = 0
                }
                404 -> error("A sessão retomável do Google Drive expirou; o próximo backup reiniciará o upload com segurança.")
                else -> {
                    if ((response.code == 401 || DriveRetryPolicy.retryableStatus(response.code, response.body)) && recoveries < DriveRetryPolicy.MAX_ATTEMPTS) {
                        delay(DriveRetryPolicy.delayMs(recoveries++))
                        val status = queryUploadStatus(sessionUrl, request.sizeBytes)
                            ?: throw java.io.IOException("Não foi possível confirmar o estado do upload após a falha temporária.")
                        when (status.code) {
                            200, 201 -> return DriveV3Json.parseFile(status.body)
                            308 -> offset = nextOffset(status.header("Range"))
                            404 -> error("A sessão retomável do Google Drive expirou; o próximo backup reiniciará o upload com segurança.")
                            else -> throw DriveApiException(status.code, status.body)
                        }
                    } else throw DriveApiException(response.code, response.body)
                }
            }
        }
        error("O Google Drive não confirmou a conclusão do upload.")
    }

    private suspend fun queryUploadStatus(sessionUrl: String, totalBytes: Long): DriveHttpResponse? {
        var attempt = 0
        while (attempt < DriveRetryPolicy.MAX_ATTEMPTS) {
            val response = try {
                http.uploadStatus(sessionUrl, totalBytes)
            } catch (error: Throwable) {
                if (error is CancellationException) throw error
                if (attempt >= DriveRetryPolicy.MAX_ATTEMPTS - 1) return null
                delay(DriveRetryPolicy.delayMs(attempt++))
                continue
            }
            if (response.code in listOf(200, 201, 308, 404)) return response
            if (!DriveRetryPolicy.retryableStatus(response.code, response.body)) return response
            if (attempt >= DriveRetryPolicy.MAX_ATTEMPTS - 1) return response
            delay(DriveRetryPolicy.delayMs(attempt++))
        }
        return null
    }

    private suspend fun verifyAndCommit(fileId: String, request: BackupCommitRequest): BackupVersionDescriptor {
        val verified = getFile(fileId)
        require(verified.size == request.sizeBytes) { "Upload incompleto: tamanho remoto divergente." }
        require(verified.sha256Checksum.equals(request.sha256, true)) {
            "O arquivo enviado ao Drive não passou na verificação SHA-256."
        }
        return markCommitted(verified, request)
    }

    private suspend fun markCommitted(file: DriveFileResource, request: BackupCommitRequest): BackupVersionDescriptor {
        val response = requireSuccessful(
            http.patchJson(
                "$DRIVE_FILES/${file.id}?fields=$FILE_FIELDS_ENCODED",
                DriveV3Json.appProperties(properties(request, STATE_COMMITTED)),
            ),
            "O arquivo chegou ao Drive, mas não foi possível confirmar o commit do backup.",
        )
        val committedFile = DriveV3Json.parseFile(response.body)
        val descriptor = descriptorOrNull(committedFile)
            ?: error("O Drive confirmou o arquivo, mas os metadados de commit são inválidos.")
        require(descriptor.revisionId == request.revisionId && descriptor.sha256.equals(request.sha256, true)) {
            "A confirmação do Drive não corresponde à revisão enviada."
        }
        return descriptor
    }

    private suspend fun finalizeIfVerified(file: DriveFileResource): BackupVersionDescriptor? {
        val full = getFile(file.id)
        val props = full.appProperties
        val expectedSize = props[PROP_SIZE]?.toLongOrNull() ?: return null
        val expectedHash = props[PROP_SHA256] ?: return null
        if (full.size != expectedSize || !full.sha256Checksum.equals(expectedHash, true)) return null
        val committedProps = props.toMutableMap().apply { put(PROP_STATE, STATE_COMMITTED) }
        val response = requireSuccessful(
            http.patchJson("$DRIVE_FILES/${file.id}?fields=$FILE_FIELDS_ENCODED", DriveV3Json.appProperties(committedProps)),
            "Falha ao reconciliar backup concluído no Drive.",
        )
        return descriptorOrNull(DriveV3Json.parseFile(response.body))
    }

    private suspend fun getFile(fileId: String): DriveFileResource {
        val response = requireSuccessful(
            http.get("$DRIVE_FILES/$fileId?fields=$FILE_FIELDS_ENCODED"),
            "Não foi possível consultar o arquivo de backup no Drive.",
        )
        return DriveV3Json.parseFile(response.body)
    }

    private suspend fun listFiles(query: String): List<DriveFileResource> {
        val result = mutableListOf<DriveFileResource>()
        var pageToken: String? = null
        do {
            val url = buildString {
                append(DRIVE_FILES)
                append("?spaces=drive&pageSize=1000&fields=")
                append(LIST_FIELDS_ENCODED)
                append("&q=")
                append(encodeQuery(query))
                pageToken?.let { append("&pageToken=").append(encodeQuery(it)) }
            }
            val response = requireSuccessful(http.get(url), "Não foi possível listar os backups no Google Drive.")
            val page = DriveV3Json.parsePage(response.body)
            result += page.files
            pageToken = page.nextPageToken
        } while (!pageToken.isNullOrBlank())
        return result
    }

    private fun descriptorOrNull(file: DriveFileResource): BackupVersionDescriptor? {
        val p = file.appProperties
        if (p[PROP_KIND] != KIND_BACKUP || p[PROP_STATE] != STATE_COMMITTED) return null
        val projectId = p[PROP_PROJECT_ID]?.takeIf { it.isNotBlank() } ?: return null
        val projectName = p[PROP_PROJECT_NAME]?.takeIf { it.isNotBlank() } ?: return null
        val revisionId = p[PROP_REVISION_ID]
            ?.takeIf { studio.guitarlab.core.project.BackupRevisionIdentity.isValid(it) }
            ?: return null
        val updated = p[PROP_PROJECT_UPDATED]?.toLongOrNull() ?: return null
        val created = p[PROP_BACKUP_CREATED]?.toLongOrNull() ?: return null
        val expectedSize = p[PROP_SIZE]?.toLongOrNull() ?: return null
        val expectedHash = p[PROP_SHA256]?.lowercase()?.takeIf { it.length == 64 } ?: return null
        if (file.size != expectedSize || !file.sha256Checksum.equals(expectedHash, true)) return null
        return BackupVersionDescriptor(
            remoteId = file.id,
            projectId = projectId,
            projectName = projectName,
            projectUpdatedAtEpochMs = updated,
            backupCreatedAtEpochMs = created,
            sizeBytes = expectedSize,
            sha256 = expectedHash,
            revisionId = revisionId,
            formatVersion = 2,
        )
    }

    private fun properties(request: BackupCommitRequest, stateValue: String): Map<String, String> = linkedMapOf(
        PROP_KIND to KIND_BACKUP,
        PROP_FORMAT to FORMAT_VALUE,
        PROP_STATE to stateValue,
        PROP_PROJECT_ID to request.projectId,
        PROP_PROJECT_NAME to request.projectName,
        PROP_REVISION_ID to request.revisionId,
        PROP_PROJECT_UPDATED to request.projectUpdatedAtEpochMs.toString(),
        PROP_BACKUP_CREATED to request.backupCreatedAtEpochMs.toString(),
        PROP_SIZE to request.sizeBytes.toString(),
        PROP_SHA256 to request.sha256.lowercase(),
    )

    private fun buildBackupQuery(stateValue: String, projectId: String?): String = buildString {
        append("trashed = false")
        append(" and appProperties has { key='$PROP_KIND' and value='$KIND_BACKUP' }")
        append(" and appProperties has { key='$PROP_STATE' and value='$stateValue' }")
        projectId?.let { append(" and appProperties has { key='$PROP_PROJECT_ID' and value='").append(escapeQuery(it)).append("' }") }
    }

    private fun buildRevisionQuery(projectId: String, revisionId: String, stateValue: String): String =
        buildBackupQuery(stateValue, projectId) +
            " and appProperties has { key='$PROP_REVISION_ID' and value='${escapeQuery(revisionId)}' }"

    private fun nextOffset(range: String?): Long {
        if (range.isNullOrBlank()) return 0L
        val match = Regex("bytes=0-([0-9]+)").matchEntire(range.trim())
            ?: throw java.io.IOException("O Google Drive retornou um intervalo de retomada inválido.")
        return match.groupValues[1].toLong() + 1L
    }

    private fun safeFileName(projectName: String, revisionId: String): String {
        val clean = projectName.replace(Regex("[\\\\/:*?\"<>|]"), "_").trim().take(60).ifBlank { "Projeto" }
        return "$clean — $revisionId.guitarlab"
    }

    private fun escapeQuery(value: String): String = value.replace("\\", "\\\\").replace("'", "\\'")

    private companion object {
        const val DRIVE_FILES = "https://www.googleapis.com/drive/v3/files"
        const val DRIVE_UPLOAD_FILES = "https://www.googleapis.com/upload/drive/v3/files"
        const val DRIVE_ABOUT = "https://www.googleapis.com/drive/v3/about"
        const val ROOT_FOLDER_NAME = "GuitarLab Studio Backups"
        const val MIME_FOLDER = "application/vnd.google-apps.folder"
        const val MIME_PACKAGE = "application/vnd.guitarlab.project"
        const val MIME_BINARY = "application/octet-stream"
        const val KIND_ROOT = "backup-root"
        const val KIND_BACKUP = "project-backup"
        const val KIND_PROBE = "probe"
        const val STATE_UPLOADING = "uploading"
        const val STATE_COMMITTED = "committed"
        const val FORMAT_VALUE = "2"
        const val PROP_KIND = "guitarlabKind"
        const val PROP_FORMAT = "guitarlabFormat"
        const val PROP_STATE = "guitarlabState"
        const val PROP_PROJECT_ID = "projectId"
        const val PROP_PROJECT_NAME = "projectName"
        const val PROP_REVISION_ID = "revisionId"
        const val PROP_PROJECT_UPDATED = "projectUpdatedAt"
        const val PROP_BACKUP_CREATED = "backupCreatedAt"
        const val PROP_SIZE = "sizeBytes"
        const val PROP_SHA256 = "sha256"
        const val CHUNK_BYTES = 8 * 1024 * 1024
        const val FILE_FIELDS = "id,name,size,sha256Checksum,createdTime,modifiedTime,trashed,appProperties"
        val FILE_FIELDS_ENCODED = encodeQuery(FILE_FIELDS)
        val LIST_FIELDS_ENCODED = encodeQuery("nextPageToken,files($FILE_FIELDS)")
    }
}
