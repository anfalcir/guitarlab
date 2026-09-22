package studio.guitarlab.app.backup

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object BackupScheduler {
    private const val PERIODIC_WORK = "guitarlab-auto-backup-periodic"
    private const val COALESCED_WORK = "guitarlab-auto-backup-coalesced"
    private const val EDIT_DEBOUNCE_MINUTES = 2L

    fun sync(context: Context) {
        val appContext = context.applicationContext
        val settings = BackupSettingsStore(appContext).snapshot()
        val manager = WorkManager.getInstance(appContext)
        if (!settings.automaticEnabled || !settings.driveConnected) {
            manager.cancelUniqueWork(PERIODIC_WORK)
            manager.cancelUniqueWork(COALESCED_WORK)
            return
        }
        val constraints = constraints(settings)
        val request = PeriodicWorkRequest.Builder(AutomaticBackupWorker::class.java, settings.cadence.hours, TimeUnit.HOURS)
            .setConstraints(constraints)
            .addTag(PERIODIC_WORK)
            .build()
        manager.enqueueUniquePeriodicWork(PERIODIC_WORK, ExistingPeriodicWorkPolicy.UPDATE, request)
    }

    /** Debounces repeated project saves: repeated saves share one pending/running incremental job. */
    fun enqueueCoalesced(context: Context) {
        val appContext = context.applicationContext
        val settings = BackupSettingsStore(appContext).snapshot()
        if (!settings.automaticEnabled || !settings.driveConnected) return
        val request = OneTimeWorkRequest.Builder(AutomaticBackupWorker::class.java)
            .setInitialDelay(EDIT_DEBOUNCE_MINUTES, TimeUnit.MINUTES)
            .setConstraints(constraints(settings))
            .addTag(COALESCED_WORK)
            .build()
        WorkManager.getInstance(appContext).enqueueUniqueWork(COALESCED_WORK, ExistingWorkPolicy.KEEP, request)
    }

    private fun constraints(settings: BackupSettingsSnapshot): Constraints = Constraints.Builder()
        .setRequiredNetworkType(if (settings.unmeteredOnly) NetworkType.UNMETERED else NetworkType.CONNECTED)
        .setRequiresCharging(settings.chargingOnly)
        .setRequiresBatteryNotLow(true)
        .setRequiresStorageNotLow(true)
        .build()
}
