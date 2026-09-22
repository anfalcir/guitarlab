package studio.guitarlab.app.backup

import android.content.Context
import java.security.MessageDigest
import studio.guitarlab.core.project.BackupCommitRequest

internal data class DriveUploadSession(
    val url: String,
    val sha256: String,
    val sizeBytes: Long,
    val createdAtEpochMs: Long,
)

internal interface DriveBackupState {
    var rootFolderId: String?
    fun session(request: BackupCommitRequest, nowEpochMs: Long = System.currentTimeMillis()): DriveUploadSession?
    fun saveSession(request: BackupCommitRequest, url: String, nowEpochMs: Long = System.currentTimeMillis())
    fun clearSession(projectId: String, revisionId: String)
    fun purgeExpiredSessions(nowEpochMs: Long = System.currentTimeMillis()): Int
}

/** Local recovery state only. Drive remains the source of truth for committed backups. */
internal class DriveBackupStateStore(context: Context) : DriveBackupState {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    override var rootFolderId: String?
        get() = prefs.getString(KEY_ROOT_ID, null)
        set(value) {
            prefs.edit().apply { if (value == null) remove(KEY_ROOT_ID) else putString(KEY_ROOT_ID, value) }.apply()
        }

    override fun session(request: BackupCommitRequest, nowEpochMs: Long): DriveUploadSession? {
        val prefix = sessionPrefix(request.projectId, request.revisionId)
        val url = prefs.getString("$prefix.url", null) ?: return null
        val hash = prefs.getString("$prefix.sha", null) ?: return null
        val size = prefs.getLong("$prefix.size", -1L)
        val created = prefs.getLong("$prefix.created", 0L)
        val valid = hash.equals(request.sha256, true) && size == request.sizeBytes &&
            created > 0L && nowEpochMs - created < SESSION_MAX_AGE_MS
        if (!valid) {
            clearSession(request.projectId, request.revisionId)
            return null
        }
        return DriveUploadSession(url, hash, size, created)
    }

    override fun saveSession(request: BackupCommitRequest, url: String, nowEpochMs: Long) {
        val prefix = sessionPrefix(request.projectId, request.revisionId)
        prefs.edit()
            .putString("$prefix.url", url)
            .putString("$prefix.sha", request.sha256.lowercase())
            .putLong("$prefix.size", request.sizeBytes)
            .putLong("$prefix.created", nowEpochMs)
            .apply()
    }

    override fun clearSession(projectId: String, revisionId: String) {
        val prefix = sessionPrefix(projectId, revisionId)
        prefs.edit()
            .remove("$prefix.url")
            .remove("$prefix.sha")
            .remove("$prefix.size")
            .remove("$prefix.created")
            .apply()
    }

    fun clearAll() {
        prefs.edit().clear().apply()
    }

    override fun purgeExpiredSessions(nowEpochMs: Long): Int {
        val prefixes = prefs.all.keys
            .filter { it.startsWith(SESSION_PREFIX) && it.endsWith(".created") }
            .map { it.removeSuffix(".created") }
        var removed = 0
        val editor = prefs.edit()
        prefixes.forEach { prefix ->
            val created = prefs.getLong("$prefix.created", 0L)
            if (created <= 0L || nowEpochMs - created >= SESSION_MAX_AGE_MS) {
                listOf("url", "sha", "size", "created").forEach { editor.remove("$prefix.$it") }
                removed++
            }
        }
        editor.apply()
        return removed
    }

    private fun sessionPrefix(projectId: String, revisionId: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest("$projectId\u0000$revisionId".toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
            .take(32)
        return "$SESSION_PREFIX$digest"
    }

    private companion object {
        const val PREFS = "guitarlab_drive_backup_state"
        const val KEY_ROOT_ID = "root_folder_id"
        const val SESSION_PREFIX = "session."
        const val SESSION_MAX_AGE_MS = 6L * 24L * 60L * 60L * 1000L // Drive sessions expire after one week.
    }
}
