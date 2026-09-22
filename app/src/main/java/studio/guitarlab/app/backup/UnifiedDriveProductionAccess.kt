package studio.guitarlab.app.backup

import android.content.Context
import java.util.UUID

internal data class UnifiedDriveUploadSession(
    val url: String,
    val sha256: String,
    val sizeBytes: Long,
    val createdAtEpochMs: Long,
)

internal interface UnifiedDriveUploadState {
    fun session(
        sha256: String,
        sizeBytes: Long,
        nowEpochMs: Long = System.currentTimeMillis(),
    ): UnifiedDriveUploadSession?

    fun save(
        sha256: String,
        sizeBytes: Long,
        url: String,
        nowEpochMs: Long = System.currentTimeMillis(),
    )

    fun clear(sha256: String)
    fun clearAll()
}

internal object NoopUnifiedDriveUploadState : UnifiedDriveUploadState {
    override fun session(
        sha256: String,
        sizeBytes: Long,
        nowEpochMs: Long,
    ): UnifiedDriveUploadSession? = null

    override fun save(
        sha256: String,
        sizeBytes: Long,
        url: String,
        nowEpochMs: Long,
    ) = Unit

    override fun clear(sha256: String) = Unit
    override fun clearAll() = Unit
}

internal class SharedPreferencesUnifiedDriveUploadState(
    context: Context,
) : UnifiedDriveUploadState {
    private val preferences =
        context.applicationContext.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)

    override fun session(
        sha256: String,
        sizeBytes: Long,
        nowEpochMs: Long,
    ): UnifiedDriveUploadSession? {
        val prefix = prefix(sha256)
        val url = preferences.getString("$prefix.url", null) ?: return null
        val storedSha = preferences.getString("$prefix.sha", null) ?: return null
        val storedSize = preferences.getLong("$prefix.size", -1L)
        val created = preferences.getLong("$prefix.created", 0L)
        val valid =
            storedSha == sha256 &&
                storedSize == sizeBytes &&
                created > 0L &&
                nowEpochMs - created < SESSION_MAX_AGE_MS
        if (!valid) {
            clear(sha256)
            return null
        }
        return UnifiedDriveUploadSession(url, storedSha, storedSize, created)
    }

    override fun save(
        sha256: String,
        sizeBytes: Long,
        url: String,
        nowEpochMs: Long,
    ) {
        val prefix = prefix(sha256)
        preferences.edit()
            .putString("$prefix.url", url)
            .putString("$prefix.sha", sha256)
            .putLong("$prefix.size", sizeBytes)
            .putLong("$prefix.created", nowEpochMs)
            .apply()
    }

    override fun clear(sha256: String) {
        val prefix = prefix(sha256)
        preferences.edit()
            .remove("$prefix.url")
            .remove("$prefix.sha")
            .remove("$prefix.size")
            .remove("$prefix.created")
            .apply()
    }

    override fun clearAll() {
        preferences.edit().clear().apply()
    }

    private fun prefix(sha256: String): String {
        require(Regex("[0-9a-f]{64}").matches(sha256))
        return "session.$sha256"
    }

    private companion object {
        const val PREFERENCES = "guitarlab_unified_drive_uploads"
        const val SESSION_MAX_AGE_MS = 6L * 24L * 60L * 60L * 1000L
    }
}

internal class UnifiedDriveRootAccess(
    context: Context,
    private val http: DriveV3Api,
    private val state: DriveBackupStateStore = DriveBackupStateStore(context),
) {
    suspend fun rootFolderId(): String {
        state.rootFolderId?.let { cached ->
            val response = http.get("$DRIVE_FILES/$cached?fields=id,trashed,appProperties")
            when (response.code) {
                200 -> {
                    val folder = DriveV3Json.parseFile(response.body)
                    if (folder.trashed != true && folder.appProperties[PROP_KIND] == KIND_ROOT) {
                        return cached
                    }
                    state.rootFolderId = null
                }
                404 -> state.rootFolderId = null
                else -> requireSuccessful(response, "Falha ao validar a pasta do GuitarLab no Drive.")
            }
        }

        val query =
            "trashed = false and mimeType = '$MIME_FOLDER' and " +
                "appProperties has { key='$PROP_KIND' and value='$KIND_ROOT' }"
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
                    appProperties = mapOf(
                        PROP_KIND to KIND_ROOT,
                        PROP_FORMAT to FORMAT_VALUE,
                    ),
                ),
            ),
            "Não foi possível criar a pasta do GuitarLab no Google Drive.",
        )
        return DriveV3Json.parseFile(created.body).id.also { state.rootFolderId = it }
    }

    suspend fun probeReadWriteDelete(): String {
        val rootId = rootFolderId()
        val probeName = ".guitarlab-vnext-probe-${UUID.randomUUID()}"
        val create = requireSuccessful(
            http.postJson(
                "$DRIVE_FILES?fields=id,name,appProperties",
                DriveV3Json.metadata(
                    name = probeName,
                    mimeType = MIME_BINARY,
                    parents = listOf(rootId),
                    appProperties = mapOf(
                        "glSchema" to "guitarlab-unified-drive-v3",
                        "glKind" to "probe",
                    ),
                ),
            ),
            "Não foi possível criar o arquivo de teste no Google Drive.",
        )
        val probe = DriveV3Json.parseFile(create.body)
        var readFailure: Throwable? = null
        try {
            requireSuccessful(
                http.get("$DRIVE_FILES/${probe.id}?fields=id,name"),
                "Não foi possível reler o arquivo de teste no Google Drive.",
            )
        } catch (error: Throwable) {
            readFailure = error
        }

        val deleteFailure = try {
            val response = http.delete("$DRIVE_FILES/${probe.id}")
            if (response.code != 404) {
                requireSuccessful(response, "Não foi possível remover o arquivo de teste do Google Drive.")
            }
            null
        } catch (error: Throwable) {
            error
        }
        readFailure?.let { error ->
            deleteFailure?.let(error::addSuppressed)
            throw error
        }
        deleteFailure?.let { throw it }
        return accountLabel()
    }

    fun clearLocalState() {
        state.clearAll()
    }

    private suspend fun accountLabel(): String {
        val response = requireSuccessful(
            http.get("$DRIVE_ABOUT?fields=user(displayName,emailAddress)"),
            "Não foi possível identificar a conta do Google Drive.",
        )
        val email = Regex("\\\"emailAddress\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"")
            .find(response.body)?.groupValues?.getOrNull(1)
        val name = Regex("\\\"displayName\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"")
            .find(response.body)?.groupValues?.getOrNull(1)
        return email ?: name ?: "Google Drive conectado"
    }

    private suspend fun listFiles(query: String): List<DriveFileResource> {
        val output = mutableListOf<DriveFileResource>()
        var pageToken: String? = null
        do {
            val url = buildString {
                append(DRIVE_FILES)
                append("?spaces=drive&pageSize=1000&fields=")
                append(encodeQuery(LIST_FIELDS))
                append("&q=")
                append(encodeQuery(query))
                pageToken?.let { append("&pageToken=").append(encodeQuery(it)) }
            }
            val page = DriveV3Json.parsePage(
                requireSuccessful(
                    http.get(url),
                    "Não foi possível localizar a pasta do GuitarLab no Google Drive.",
                ).body,
            )
            output += page.files
            pageToken = page.nextPageToken
        } while (!pageToken.isNullOrBlank())
        return output
    }

    private companion object {
        const val DRIVE_FILES = "https://www.googleapis.com/drive/v3/files"
        const val DRIVE_ABOUT = "https://www.googleapis.com/drive/v3/about"
        const val ROOT_FOLDER_NAME = "GuitarLab Studio Backups"
        const val MIME_FOLDER = "application/vnd.google-apps.folder"
        const val MIME_BINARY = "application/octet-stream"
        const val KIND_ROOT = "backup-root"
        const val FORMAT_VALUE = "2"
        const val PROP_KIND = "guitarlabKind"
        const val PROP_FORMAT = "guitarlabFormat"
        const val LIST_FIELDS =
            "nextPageToken,files(id,name,size,sha256Checksum,createdTime,modifiedTime,trashed,appProperties)"
    }
}
