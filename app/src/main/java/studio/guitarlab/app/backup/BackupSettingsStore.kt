package studio.guitarlab.app.backup

import android.content.Context
import android.net.Uri

enum class BackupCadence(val hours: Long, val label: String) {
    SIX_HOURS(6, "A cada 6 horas"),
    TWELVE_HOURS(12, "A cada 12 horas"),
    DAILY(24, "Diariamente"),
    WEEKLY(24 * 7, "Semanalmente"),
}

data class BackupSettingsSnapshot(
    /** Legacy H26-H28 SAF destination retained only for one-time migration of existing history. */
    val treeUri: Uri? = null,
    val folderLabel: String? = null,
    val driveConnected: Boolean = false,
    val driveAccountLabel: String? = null,
    val automaticEnabled: Boolean = false,
    val cadence: BackupCadence = BackupCadence.DAILY,
    val unmeteredOnly: Boolean = true,
    val chargingOnly: Boolean = false,
    val retentionDays: Int? = 90,
    val maximumVersions: Int = 3,
    val lastRunEpochMs: Long? = null,
    val lastSuccessEpochMs: Long? = null,
    val lastRunSummary: String? = null,
    val lastError: String? = null,
)

class BackupSettingsStore(context: Context) {
    private val appContext = context.applicationContext
    private val preferences = appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    /** Authorization/account selection is device-local state and must never be restored onto another device. */
    private val connectionPreferences = appContext.getSharedPreferences(CONNECTION_PREFS, Context.MODE_PRIVATE)

    fun snapshot(): BackupSettingsSnapshot = BackupSettingsSnapshot(
        treeUri = preferences.getString(KEY_TREE_URI, null)?.let(Uri::parse),
        folderLabel = preferences.getString(KEY_FOLDER_LABEL, null),
        driveConnected = connectionPreferences.getBoolean(KEY_DRIVE_CONNECTED, false),
        driveAccountLabel = connectionPreferences.getString(KEY_DRIVE_ACCOUNT_LABEL, null),
        automaticEnabled = preferences.getBoolean(KEY_AUTO, false),
        cadence = preferences.getString(KEY_CADENCE, null)?.let { runCatching { BackupCadence.valueOf(it) }.getOrNull() } ?: BackupCadence.DAILY,
        unmeteredOnly = preferences.getBoolean(KEY_UNMETERED, true),
        chargingOnly = preferences.getBoolean(KEY_CHARGING, false),
        retentionDays = when (val value = preferences.getInt(KEY_RETENTION_DAYS, 90)) { 0 -> null; else -> value },
        maximumVersions = preferences.getInt(
            KEY_MAX_VERSIONS,
            preferences.getInt(KEY_MIN_VERSIONS_LEGACY, 3),
        ).coerceAtLeast(1),
        lastRunEpochMs = preferences.getLong(KEY_LAST_RUN, 0L).takeIf { it > 0L }
            ?: preferences.getLong(KEY_LAST_SUCCESS, 0L).takeIf { it > 0L },
        lastSuccessEpochMs = preferences.getLong(KEY_LAST_SUCCESS, 0L).takeIf { it > 0L },
        lastRunSummary = preferences.getString(KEY_LAST_SUMMARY, null),
        lastError = preferences.getString(KEY_LAST_ERROR, null),
    )

    /** Legacy only: existing SAF users can migrate their committed history to Drive once. */
    fun setFolder(uri: Uri, label: String) {
        preferences.edit().putString(KEY_TREE_URI, uri.toString()).putString(KEY_FOLDER_LABEL, label).remove(KEY_LAST_ERROR).apply()
    }

    fun clearLegacyFolder() {
        preferences.edit().remove(KEY_TREE_URI).remove(KEY_FOLDER_LABEL).apply()
    }

    fun setDriveConnected(label: String) {
        connectionPreferences.edit()
            .putBoolean(KEY_DRIVE_CONNECTED, true)
            .putString(KEY_DRIVE_ACCOUNT_LABEL, label)
            .apply()
        preferences.edit().remove(KEY_LAST_ERROR).apply()
    }

    fun clearDriveConnection() {
        connectionPreferences.edit().clear().apply()
    }

    fun setAutomaticEnabled(value: Boolean) = preferences.edit().putBoolean(KEY_AUTO, value).apply()
    fun setCadence(value: BackupCadence) = preferences.edit().putString(KEY_CADENCE, value.name).apply()
    fun setUnmeteredOnly(value: Boolean) = preferences.edit().putBoolean(KEY_UNMETERED, value).apply()
    fun setChargingOnly(value: Boolean) = preferences.edit().putBoolean(KEY_CHARGING, value).apply()
    fun setRetentionDays(value: Int?) = preferences.edit().putInt(KEY_RETENTION_DAYS, value ?: 0).apply()
    fun setMaximumVersions(value: Int) = preferences.edit()
        .putInt(KEY_MAX_VERSIONS, value.coerceAtLeast(1))
        .remove(KEY_MIN_VERSIONS_LEGACY)
        .apply()

    fun recordSuccess(summary: String, nowEpochMs: Long = System.currentTimeMillis()) {
        recordRun(summary = summary, error = null, completedSuccessfully = true, nowEpochMs = nowEpochMs)
    }

    fun recordPartial(summary: String, error: String, nowEpochMs: Long = System.currentTimeMillis()) {
        recordRun(summary = summary, error = error, completedSuccessfully = false, nowEpochMs = nowEpochMs)
    }

    fun recordError(message: String, nowEpochMs: Long = System.currentTimeMillis()) {
        recordRun(summary = message, error = message, completedSuccessfully = false, nowEpochMs = nowEpochMs)
    }

    private fun recordRun(summary: String, error: String?, completedSuccessfully: Boolean, nowEpochMs: Long) {
        val editor = preferences.edit()
            .putLong(KEY_LAST_RUN, nowEpochMs)
            .putString(KEY_LAST_SUMMARY, summary)
        if (completedSuccessfully) editor.putLong(KEY_LAST_SUCCESS, nowEpochMs)
        if (error == null) editor.remove(KEY_LAST_ERROR) else editor.putString(KEY_LAST_ERROR, error)
        editor.apply()
    }

    private companion object {
        const val PREFS = "guitarlab_backup"
        const val CONNECTION_PREFS = "guitarlab_drive_connection"
        const val KEY_TREE_URI = "tree_uri"
        const val KEY_FOLDER_LABEL = "folder_label"
        const val KEY_DRIVE_CONNECTED = "drive_connected"
        const val KEY_DRIVE_ACCOUNT_LABEL = "drive_account_label"
        const val KEY_AUTO = "automatic_enabled"
        const val KEY_CADENCE = "cadence"
        const val KEY_UNMETERED = "unmetered_only"
        const val KEY_CHARGING = "charging_only"
        const val KEY_RETENTION_DAYS = "retention_days"
        const val KEY_MAX_VERSIONS = "maximum_versions"
        const val KEY_MIN_VERSIONS_LEGACY = "minimum_versions"
        const val KEY_LAST_RUN = "last_run_epoch_ms"
        const val KEY_LAST_SUCCESS = "last_success_epoch_ms"
        const val KEY_LAST_SUMMARY = "last_run_summary"
        const val KEY_LAST_ERROR = "last_error"
    }
}
