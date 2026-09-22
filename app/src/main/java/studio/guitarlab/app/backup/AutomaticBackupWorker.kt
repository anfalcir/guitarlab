package studio.guitarlab.app.backup

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import java.io.IOException
import kotlinx.coroutines.CancellationException
import studio.guitarlab.app.activity.AppNotificationDeepLink
import studio.guitarlab.app.activity.UnifiedActivityStore
import studio.guitarlab.app.ui.AppScreen
import studio.guitarlab.core.project.BackupRetentionPolicy
import studio.guitarlab.core.project.ProjectBackupCoordinator
import studio.guitarlab.core.project.UnifiedOperationKind
import studio.guitarlab.core.project.UnifiedOperationState

class AutomaticBackupWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val settingsStore = BackupSettingsStore(applicationContext)
        val settings = settingsStore.snapshot()
        if (!settings.driveConnected || !settings.automaticEnabled) return Result.success()
        val operationId = "automatic-backup"
        val activity = UnifiedActivityStore(applicationContext)
        val confirmedRevisions = ConfirmedRevisionStore(applicationContext)
        activity.record(operationId, null, UnifiedOperationKind.BACKUP, UnifiedOperationState.RUNNING, null, "Backup automático em andamento")

        setForeground(createForegroundInfo(operationId))
        return BackupOperationLock.withLock {
            val remote = DriveV3BackupRemoteStore(applicationContext)
            val report = try {
                ProjectBackupCoordinator(applicationContext.filesDir, remote).backupAll(
                    force = false,
                    retentionPolicy = BackupRetentionPolicy(settings.retentionDays, settings.maximumVersions),
                )
            } catch (error: Throwable) {
                if (error is CancellationException) throw error
                val message = error.message ?: "Falha inesperada no backup automático."
                settingsStore.recordError(message)
                activity.record(operationId, null, UnifiedOperationKind.BACKUP, UnifiedOperationState.FAILED, null, "Backup automático interrompido", message)
                return@withLock when {
                    error is DriveAuthorizationRequiredException -> Result.failure()
                    error is IOException && runAttemptCount < 4 -> Result.retry()
                    else -> Result.failure()
                }
            }
            confirmedRevisions.record(report)
            val summary = report.userSummary("Backup automático")
            val failure = report.userFailureDetail()
            if (failure == null) {
                settingsStore.recordSuccess(summary)
                activity.record(operationId, null, UnifiedOperationKind.BACKUP, UnifiedOperationState.SUCCEEDED, 100, summary)
                Result.success()
            } else {
                settingsStore.recordPartial(summary, failure)
                activity.record(operationId, null, UnifiedOperationKind.BACKUP, UnifiedOperationState.FAILED, null, "Backup automático parcial", failure)
                val authorizationBlocked = report.attempts.any { attempt ->
                    attempt.error?.contains(DriveAuthorizationRequiredException.MESSAGE, ignoreCase = true) == true
                }
                when {
                    authorizationBlocked -> Result.failure()
                    report.committedCount == 0 && runAttemptCount < 4 -> Result.retry()
                    else -> Result.success()
                }
            }
        }
    }

    private fun createForegroundInfo(operationId: String): ForegroundInfo {
        val manager = applicationContext.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Backup do GuitarLab", NotificationManager.IMPORTANCE_LOW).apply {
                description = "Backup e restauração de projetos no Google Drive"
            },
        )
        val notification = Notification.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_upload)
            .setContentTitle("GuitarLab")
            .setContentText("Protegendo projetos no Google Drive…")
            .setContentIntent(AppNotificationDeepLink.pendingIntent(applicationContext, AppScreen.Activity(operationId)))
            .setOngoing(true)
            .setCategory(Notification.CATEGORY_SERVICE)
            .build()
        return ForegroundInfo(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
    }

    private companion object {
        const val CHANNEL_ID = "guitarlab_backup"
        const val NOTIFICATION_ID = 2601
    }
}
