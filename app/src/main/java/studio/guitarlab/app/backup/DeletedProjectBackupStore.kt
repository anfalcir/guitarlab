package studio.guitarlab.app.backup

import android.content.Context
import java.util.concurrent.TimeUnit

data class DeletedProjectBackup(
    val projectId: String,
    val projectName: String,
    val deletedAtEpochMs: Long,
)

internal object DeletedProjectRetentionPolicy {
    const val RETENTION_DAYS = 10L
    fun expired(records: Collection<DeletedProjectBackup>, nowEpochMs: Long): List<DeletedProjectBackup> {
        val cutoff = nowEpochMs - TimeUnit.DAYS.toMillis(RETENTION_DAYS)
        return records.filter { it.deletedAtEpochMs <= cutoff }
    }
}

/** Explicit deletion tombstones. Mere absence from this installation is never destructive. */
internal class DeletedProjectBackupStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)

    fun record(projectId: String, projectName: String, deletedAtEpochMs: Long = System.currentTimeMillis()) {
        preferences.edit()
            .putLong("$TIME_PREFIX$projectId", deletedAtEpochMs)
            .putString("$NAME_PREFIX$projectId", projectName)
            .apply()
    }

    fun remove(projectId: String) {
        preferences.edit().remove("$TIME_PREFIX$projectId").remove("$NAME_PREFIX$projectId").apply()
    }

    fun all(): Map<String, DeletedProjectBackup> = preferences.all
        .filterKeys { it.startsWith(TIME_PREFIX) }
        .mapNotNull { (key, value) ->
            val deletedAt = value as? Long ?: return@mapNotNull null
            val id = key.removePrefix(TIME_PREFIX).takeIf(String::isNotBlank) ?: return@mapNotNull null
            id to DeletedProjectBackup(id, preferences.getString("$NAME_PREFIX$id", null).orEmpty(), deletedAt)
        }
        .toMap()

    private companion object {
        const val PREFERENCES = "deleted_project_backups_v1"
        const val TIME_PREFIX = "deleted_at:"
        const val NAME_PREFIX = "name:"
    }
}
