package studio.guitarlab.app.backup

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import studio.guitarlab.core.project.BackupVersionDescriptor
import studio.guitarlab.core.project.DriveReconciliation

internal data class BackupCatalogSnapshot(
    val versions: List<BackupVersionDescriptor>,
    val reconciliations: Map<String, DriveReconciliation>,
    val remoteTips: Map<String, List<BackupVersionDescriptor>>,
    val localRevisions: Map<String, String>,
    val refreshedAtEpochMs: Long,
)

internal object BackupCatalogFreshnessPolicy {
    const val DEFAULT_MAX_AGE_MS: Long = 2L * 60L * 1_000L

    fun shouldRefresh(
        snapshot: BackupCatalogSnapshot?,
        nowEpochMs: Long,
        force: Boolean,
        maxAgeMs: Long = DEFAULT_MAX_AGE_MS,
    ): Boolean {
        if (force || snapshot == null) return true
        if (snapshot.refreshedAtEpochMs <= 0L || nowEpochMs < snapshot.refreshedAtEpochMs) return true
        return nowEpochMs - snapshot.refreshedAtEpochMs >= maxAgeMs
    }
}

/**
 * Small durable metadata cache for the Drive backup catalog.
 *
 * It never contains media, OAuth credentials or upload-session state. A cache is accepted only for
 * the same Drive account label and retention settings, and failed refreshes never overwrite it.
 */
internal class BackupCatalogCacheStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)

    fun load(settings: BackupSettingsSnapshot): BackupCatalogSnapshot? {
        if (!settings.driveConnected) return null
        val raw = preferences.getString(KEY_SNAPSHOT, null) ?: return null
        return runCatching {
            val root = JSONObject(raw)
            if (root.optInt("schema", 0) != SCHEMA) return@runCatching null
            if (root.optString("account") != settings.driveAccountLabel.orEmpty()) return@runCatching null
            if (root.optInt("retentionDays", Int.MIN_VALUE) != (settings.retentionDays ?: NO_RETENTION)) return@runCatching null
            if (root.optInt("maximumVersions", Int.MIN_VALUE) != settings.maximumVersions) return@runCatching null

            val versions = root.getJSONArray("versions").toDescriptors()
            val reconciliationsObject = root.optJSONObject("reconciliations") ?: JSONObject()
            val reconciliations = buildMap {
                val keys = reconciliationsObject.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    runCatching { DriveReconciliation.valueOf(reconciliationsObject.getString(key)) }
                        .getOrNull()
                        ?.let { put(key, it) }
                }
            }
            val tipsObject = root.optJSONObject("remoteTips") ?: JSONObject()
            val remoteTips = buildMap {
                val keys = tipsObject.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    put(key, tipsObject.getJSONArray(key).toDescriptors())
                }
            }
            val revisionsObject = root.optJSONObject("localRevisions") ?: JSONObject()
            val localRevisions = buildMap {
                val keys = revisionsObject.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    put(key, revisionsObject.getString(key))
                }
            }
            BackupCatalogSnapshot(
                versions = versions,
                reconciliations = reconciliations,
                remoteTips = remoteTips,
                localRevisions = localRevisions,
                refreshedAtEpochMs = root.getLong("refreshedAtEpochMs"),
            )
        }.getOrNull()
    }

    fun save(settings: BackupSettingsSnapshot, snapshot: BackupCatalogSnapshot) {
        if (!settings.driveConnected) return
        val reconciliations = JSONObject()
        snapshot.reconciliations.forEach { (projectId, state) -> reconciliations.put(projectId, state.name) }
        val tips = JSONObject()
        snapshot.remoteTips.forEach { (projectId, versions) -> tips.put(projectId, versions.toJson()) }
        val localRevisions = JSONObject()
        snapshot.localRevisions.forEach { (projectId, revision) -> localRevisions.put(projectId, revision) }
        val root = JSONObject()
            .put("schema", SCHEMA)
            .put("account", settings.driveAccountLabel.orEmpty())
            .put("retentionDays", settings.retentionDays ?: NO_RETENTION)
            .put("maximumVersions", settings.maximumVersions)
            .put("refreshedAtEpochMs", snapshot.refreshedAtEpochMs)
            .put("versions", snapshot.versions.toJson())
            .put("reconciliations", reconciliations)
            .put("remoteTips", tips)
            .put("localRevisions", localRevisions)
        preferences.edit().putString(KEY_SNAPSHOT, root.toString()).apply()
    }

    fun clear() {
        preferences.edit().remove(KEY_SNAPSHOT).apply()
    }

    private fun List<BackupVersionDescriptor>.toJson(): JSONArray = JSONArray().also { array ->
        forEach { version ->
            array.put(
                JSONObject()
                    .put("remoteId", version.remoteId)
                    .put("projectId", version.projectId)
                    .put("projectName", version.projectName)
                    .put("projectUpdatedAtEpochMs", version.projectUpdatedAtEpochMs)
                    .put("backupCreatedAtEpochMs", version.backupCreatedAtEpochMs)
                    .put("sizeBytes", version.sizeBytes)
                    .put("sha256", version.sha256)
                    .put("revisionId", version.revisionId)
                    .put("formatVersion", version.formatVersion),
            )
        }
    }

    private fun JSONArray.toDescriptors(): List<BackupVersionDescriptor> = buildList {
        for (index in 0 until length()) {
            val item = getJSONObject(index)
            add(
                BackupVersionDescriptor(
                    remoteId = item.getString("remoteId"),
                    projectId = item.getString("projectId"),
                    projectName = item.getString("projectName"),
                    projectUpdatedAtEpochMs = item.getLong("projectUpdatedAtEpochMs"),
                    backupCreatedAtEpochMs = item.getLong("backupCreatedAtEpochMs"),
                    sizeBytes = item.getLong("sizeBytes"),
                    sha256 = item.getString("sha256"),
                    revisionId = item.getString("revisionId"),
                    formatVersion = item.getInt("formatVersion"),
                ),
            )
        }
    }

    private companion object {
        const val PREFERENCES = "guitarlab_backup_catalog_cache"
        const val KEY_SNAPSHOT = "snapshot"
        const val SCHEMA = 2
        const val NO_RETENTION = -1
    }
}
