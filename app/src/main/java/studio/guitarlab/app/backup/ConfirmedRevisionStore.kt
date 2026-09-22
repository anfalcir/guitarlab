package studio.guitarlab.app.backup

import android.content.Context
import studio.guitarlab.core.project.BackupRunReport
import studio.guitarlab.core.project.DriveCurrentDescriptor
import studio.guitarlab.core.project.ProjectBackupAttempt

/**
 * Device-local mirror of the last project revision that the remote store actually confirmed.
 *
 * Worker/UI success is not sufficient: only an exact descriptor returned by the coordinator can
 * advance this boundary. The store is provider-neutral so the U8 vNext cutover can reuse it.
 */
class ConfirmedRevisionStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)

    fun confirmedRevision(projectId: String): String? =
        preferences.getString(key(projectId), null)?.takeIf { it.isNotBlank() }

    fun record(descriptor: DriveCurrentDescriptor) {
        preferences.edit()
            .putString(key(descriptor.projectId), descriptor.revisionId)
            .apply()
    }

    fun record(report: BackupRunReport) {
        val editor = preferences.edit()
        report.attempts.forEach { attempt ->
            if (attempt.status == ProjectBackupAttempt.Status.FAILED) return@forEach
            val version = attempt.version ?: return@forEach
            if (version.projectId == attempt.projectId && version.revisionId.isNotBlank()) {
                editor.putString(key(attempt.projectId), version.revisionId)
            }
        }
        editor.apply()
    }

    fun clearProject(projectId: String) {
        preferences.edit().remove(key(projectId)).apply()
    }

    fun clearAll() {
        preferences.edit().clear().apply()
    }

    private fun key(projectId: String): String {
        require(projectId.isNotBlank())
        return "revision:$projectId"
    }

    private companion object {
        const val PREFERENCES = "guitarlab_confirmed_revisions"
    }
}
