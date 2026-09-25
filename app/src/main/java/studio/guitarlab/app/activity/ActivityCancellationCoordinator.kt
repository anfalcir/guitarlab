package studio.guitarlab.app.activity

import android.content.Context
import studio.guitarlab.app.backup.BackupScheduler
import studio.guitarlab.core.project.UnifiedOperationKind
import studio.guitarlab.core.project.UnifiedOperationRecord
import studio.guitarlab.platform.separation.RemoteSeparationClient
import studio.guitarlab.platform.source.android.SourceAcquisitionClient

data class ActivityCancellationResult(
    val operationId: String,
    val executorCancelled: Boolean,
    val durableCancellationRequested: Boolean,
    val message: String,
)

class ActivityCancellationCoordinator(context: Context) {
    private val appContext = context.applicationContext
    private val store = UnifiedActivityStore(appContext)
    private val source = SourceAcquisitionClient(appContext)
    private val separation = RemoteSeparationClient(appContext)

    fun cancel(record: UnifiedOperationRecord): ActivityCancellationResult {
        require(record.state.isActive) { "A atividade já terminou." }

        val executorCancelled = ActivityCancellationRegistry.cancel(record.operationId)
        var durableRequested = false
        var durableDetail = "no-durable-owner"

        when (record.kind) {
            UnifiedOperationKind.SOURCE_ACQUISITION -> {
                val projectId = record.projectId
                if (projectId != null && source.cancel(projectId, record.operationId)) {
                    durableRequested = true
                    durableDetail = "source-workmanager+ytdlp"
                } else {
                    durableDetail = "source-local-or-orphan"
                }
            }

            UnifiedOperationKind.SEPARATION -> {
                val projectId = record.projectId
                if (projectId != null && separation.cancelFromActivity(projectId, record.operationId)) {
                    durableRequested = true
                    durableDetail = "firebase-cloud-cancel-requested"
                } else {
                    durableDetail = "remote-cancel-not-requested"
                }
            }

            UnifiedOperationKind.BACKUP -> {
                if (record.operationId == AUTOMATIC_BACKUP_OPERATION_ID) {
                    BackupScheduler.cancelAutomaticExecution(appContext)
                    durableRequested = true
                    durableDetail = "automatic-backup-workmanager"
                } else {
                    durableDetail = "manual-backup-local-or-orphan"
                }
            }

            UnifiedOperationKind.RESTORE -> {
                durableDetail = "restore-local-or-orphan"
            }

            UnifiedOperationKind.REFERENCE_PREPARATION -> {
                durableDetail = "reference-local-or-orphan"
            }

            UnifiedOperationKind.EXPORT -> {
                durableDetail = "export-local-or-orphan"
            }
        }

        val summary = when (record.kind) {
            UnifiedOperationKind.SEPARATION ->
                if (durableRequested) "Cancelamento da separação solicitado" else "Atividade de separação órfã encerrada"
            else -> "Operação cancelada pelo usuário"
        }
        store.cancelActive(
            operationId = record.operationId,
            summary = summary,
            technicalDetail = buildString {
                append("USER_CANCELLED_FROM_ACTIVITY")
                append("; executorCancelled=").append(executorCancelled)
                append("; durable=").append(durableDetail)
            },
        )

        return ActivityCancellationResult(
            operationId = record.operationId,
            executorCancelled = executorCancelled,
            durableCancellationRequested = durableRequested,
            message = when {
                record.kind == UnifiedOperationKind.SEPARATION && durableRequested ->
                    "Cancelamento solicitado ao backend da separação."
                durableRequested || executorCancelled ->
                    "Cancelamento solicitado."
                else ->
                    "A atividade órfã foi encerrada localmente."
            },
        )
    }

    private companion object {
        const val AUTOMATIC_BACKUP_OPERATION_ID = "automatic-backup"
    }
}
