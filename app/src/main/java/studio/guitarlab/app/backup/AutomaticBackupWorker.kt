package studio.guitarlab.app.backup

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import kotlinx.coroutines.CancellationException
import studio.guitarlab.app.activity.AppNotificationDeepLink
import studio.guitarlab.app.R
import studio.guitarlab.app.activity.UnifiedActivityStore
import studio.guitarlab.app.ui.AppScreen
import studio.guitarlab.core.project.BackupRetentionPolicy
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
        val unifiedDrive = UnifiedDriveProductionService(applicationContext)
        activity.record(operationId, null, UnifiedOperationKind.BACKUP, UnifiedOperationState.RUNNING, null, "Backup automático em andamento")

        setForeground(createForegroundInfo(operationId))
        return try {
            BackupOperationLock.withLock {
                val report = try {
                    val deletedStore = DeletedProjectBackupStore(applicationContext)
                    DeletedProjectRetentionPolicy.expired(deletedStore.all().values, System.currentTimeMillis()).forEach { tombstone ->
                        unifiedDrive.deleteProjectBackups(tombstone.projectId)
                        deletedStore.remove(tombstone.projectId)
                    }
                    unifiedDrive.backupAll(
                        retentionPolicy = BackupRetentionPolicy(
                            settings.retentionDays,
                            settings.maximumVersions,
                        ),
                    )
                } catch (error: Throwable) {
                    if (error is CancellationException) throw error
                    val message = error.message ?: "Falha inesperada no backup automático."
                    settingsStore.recordError(message)
                    val retry =
                        isTransientDriveFailure(error) &&
                            runAttemptCount < 4
                    activity.record(
                        operationId,
                        null,
                        UnifiedOperationKind.BACKUP,
                        if (retry) UnifiedOperationState.RETRYING else UnifiedOperationState.FAILED,
                        null,
                        if (retry) "Backup automático aguardando nova tentativa" else "Backup automático interrompido",
                        message,
                    )
                    return@withLock when {
                        error is DriveAuthorizationRequiredException -> Result.failure()
                        retry -> Result.retry()
                        else -> Result.failure()
                    }
                }
                val summary = report.userSummary("Backup automático")
                val failure = report.userFailureDetail()
                if (failure == null) {
                    settingsStore.recordSuccess(summary)
                    activity.record(
                        operationId,
                        null,
                        UnifiedOperationKind.BACKUP,
                        UnifiedOperationState.SUCCEEDED,
                        100,
                        summary,
                    )
                    Result.success()
                } else {
                    settingsStore.recordPartial(summary, failure)
                    val authorizationBlocked = report.attempts.any { attempt ->
                        attempt.error?.contains(
                            DriveAuthorizationRequiredException.MESSAGE,
                            ignoreCase = true,
                        ) == true
                    }
                    val retry =
                        !authorizationBlocked &&
                            report.committedCount == 0 &&
                            runAttemptCount < 4
                    activity.record(
                        operationId,
                        null,
                        UnifiedOperationKind.BACKUP,
                        if (retry) UnifiedOperationState.RETRYING else UnifiedOperationState.FAILED,
                        null,
                        if (retry) "Backup automático aguardando nova tentativa" else "Backup automático parcial",
                        failure,
                    )
                    when {
                        authorizationBlocked -> Result.failure()
                        retry -> Result.retry()
                        else -> Result.success()
                    }
                }
            }
        } catch (error: CancellationException) {
            activity.record(
                operationId,
                null,
                UnifiedOperationKind.BACKUP,
                UnifiedOperationState.CANCELLED,
                null,
                "Backup automático cancelado",
            )
            throw error
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
            .setSmallIcon(R.drawable.ic_notification_guitarlab)
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
