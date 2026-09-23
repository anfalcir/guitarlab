package studio.guitarlab.platform.separation

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import studio.guitarlab.core.separation.DurableRemoteJob
import studio.guitarlab.core.separation.RemoteJobIdentity
import studio.guitarlab.core.separation.RemoteJobState

internal data class RemoteNotificationCopy(
    val text: String,
    val ongoing: Boolean,
)

internal object RemoteSeparationNotificationPolicy {
    fun copy(state: RemoteJobState, errorCode: String? = null): RemoteNotificationCopy {
        if (state !in terminalStates && errorCode?.startsWith("RETRY:") == true) {
            return RemoteNotificationCopy("Falha temporária · nova tentativa agendada", true)
        }
        return when (state) {
            RemoteJobState.UPLOADING -> RemoteNotificationCopy("Enviando a fonte para a nuvem", true)
            RemoteJobState.READY -> RemoteNotificationCopy("Fonte enviada · preparando processamento", true)
            RemoteJobState.QUEUED -> RemoteNotificationCopy("Aguardando início do processamento", true)
            RemoteJobState.RUNNING -> RemoteNotificationCopy("Separação em andamento no Cloud Run", true)
            RemoteJobState.COMPLETED -> RemoteNotificationCopy("Processamento concluído · preparando importação", true)
            RemoteJobState.IMPORTING -> RemoteNotificationCopy("Baixando, validando e importando as seis faixas", true)
            RemoteJobState.CANCEL_REQUESTED -> RemoteNotificationCopy("Cancelamento solicitado", true)
            RemoteJobState.IMPORT_FAILED -> RemoteNotificationCopy("Processamento concluído · importação precisa ser retomada", false)
            RemoteJobState.IMPORTED -> RemoteNotificationCopy("Separação concluída e importada", false)
            RemoteJobState.CANCELLED -> RemoteNotificationCopy("Separação cancelada", false)
            RemoteJobState.FAILED -> RemoteNotificationCopy("Separação interrompida por falha", false)
            RemoteJobState.EXPIRED -> RemoteNotificationCopy("Resultado remoto não está mais disponível", false)
        }
    }

    val terminalStates: Set<RemoteJobState> = setOf(
        RemoteJobState.IMPORT_FAILED,
        RemoteJobState.IMPORTED,
        RemoteJobState.CANCELLED,
        RemoteJobState.FAILED,
        RemoteJobState.EXPIRED,
    )
}

internal class RemoteSeparationNotifier(context: Context) {
    private val appContext = context.applicationContext
    private val manager = appContext.getSystemService(NotificationManager::class.java)

    init {
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                "Separação em nuvem",
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = "Acompanha processos de separação em nuvem do GuitarLab"
                setShowBadge(true)
            },
        )
    }

    fun show(
        identity: RemoteJobIdentity,
        projectName: String,
        state: RemoteJobState,
        errorCode: String? = null,
    ) {
        val copy = RemoteSeparationNotificationPolicy.copy(state, errorCode)
        manager.notify(
            notificationId(identity.jobId),
            Notification.Builder(appContext, CHANNEL_ID)
                .setSmallIcon(
                    when (state) {
                        RemoteJobState.IMPORTED -> android.R.drawable.stat_sys_download_done
                        RemoteJobState.IMPORT_FAILED, RemoteJobState.FAILED, RemoteJobState.EXPIRED ->
                            android.R.drawable.stat_notify_error
                        else -> android.R.drawable.stat_sys_upload
                    },
                )
                .setContentTitle(projectName.ifBlank { "GuitarLab" })
                .setContentText(copy.text)
                .setSubText("GuitarLab · Separação")
                .setContentIntent(launchIntent(identity.jobId))
                .setCategory(Notification.CATEGORY_PROGRESS)
                .setOngoing(copy.ongoing)
                .setAutoCancel(!copy.ongoing)
                .setOnlyAlertOnce(copy.ongoing)
                .setShowWhen(true)
                .build(),
        )
    }

    fun showRetry(identity: RemoteJobIdentity, projectName: String, errorCode: String?) =
        show(identity, projectName, RemoteJobState.RUNNING, errorCode ?: "RETRY:UNKNOWN")

    fun cancel(identity: RemoteJobIdentity) {
        manager.cancel(notificationId(identity.jobId))
    }

    fun reconcileExisting(
        jobs: Collection<DurableRemoteJob>,
        projectName: (String) -> String,
    ) {
        val visibleIds = manager.activeNotifications
            .map { it.id }
            .filter { (it and NOTIFICATION_FAMILY_MASK) == NOTIFICATION_BASE }
            .toSet()
        jobs
            .filter { notificationId(it.identity.jobId) in visibleIds }
            .forEach { job ->
                show(
                    job.identity,
                    projectName(job.identity.projectId),
                    job.state,
                    job.errorCode,
                )
            }
    }

    private fun launchIntent(jobId: String): PendingIntent? {
        val launch = appContext.packageManager.getLaunchIntentForPackage(appContext.packageName)
            ?: return null
        launch.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        return PendingIntent.getActivity(
            appContext,
            notificationId(jobId),
            launch,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun notificationId(jobId: String): Int =
        NOTIFICATION_BASE or (jobId.hashCode() and NOTIFICATION_MASK)

    private companion object {
        const val CHANNEL_ID = "guitarlab_separation"
        const val NOTIFICATION_BASE = 0x52000000
        const val NOTIFICATION_FAMILY_MASK = 0xFF000000.toInt()
        const val NOTIFICATION_MASK = 0x00FFFFFF
    }
}
