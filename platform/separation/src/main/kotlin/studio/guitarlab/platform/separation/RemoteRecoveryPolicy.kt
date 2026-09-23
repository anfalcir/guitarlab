package studio.guitarlab.platform.separation

import studio.guitarlab.core.model.PreparationStatus
import studio.guitarlab.core.separation.DurableRemoteJob
import studio.guitarlab.core.separation.RemoteFailurePolicy
import studio.guitarlab.core.separation.RemoteJobState

enum class CancellationFailureAction { RETRY, TERMINAL_FAILURE }

object RemoteRecoveryPolicy {
    val terminalStates = setOf(
        RemoteJobState.IMPORT_FAILED,
        RemoteJobState.IMPORTED,
        RemoteJobState.CANCELLED,
        RemoteJobState.FAILED,
        RemoteJobState.EXPIRED,
    )

    fun isUnresolved(state: RemoteJobState): Boolean =
        state !in terminalStates || state == RemoteJobState.IMPORT_FAILED

    fun shouldRestoreSourceReady(status: PreparationStatus?, hasActiveJob: Boolean): Boolean =
        status == PreparationStatus.SEPARATING && !hasActiveJob

    fun shouldRestoreAfterTerminalJob(
        status: PreparationStatus?,
        currentSourceAssetId: String?,
        recoverySourceAssetId: String,
        hasOtherActiveJob: Boolean,
    ): Boolean =
        status == PreparationStatus.SEPARATING &&
            currentSourceAssetId == recoverySourceAssetId &&
            !hasOtherActiveJob

    fun cancellationFailureAction(code: String, attempt: Int, maxAttempts: Int = 8): CancellationFailureAction =
        if (RemoteFailurePolicy.shouldRetry(code, attempt, maxAttempts)) {
            CancellationFailureAction.RETRY
        } else {
            CancellationFailureAction.TERMINAL_FAILURE
        }

    fun legacyImportRecoveryCandidate(jobs: List<DurableRemoteJob>, projectId: String): DurableRemoteJob? {
        val latest = jobs
            .asSequence()
            .filter { it.identity.projectId == projectId }
            .maxByOrNull { it.updatedAtMs }
            ?: return null
        return latest.takeIf {
            it.state == RemoteJobState.EXPIRED &&
                !it.resultManifestSha256.isNullOrBlank()
        }
    }

    fun selectLatestForProject(jobs: List<DurableRemoteJob>, projectId: String): DurableRemoteJob? =
        jobs
            .asSequence()
            .filter { it.identity.projectId == projectId }
            .sortedWith(
                compareByDescending<DurableRemoteJob> { isUnresolved(it.state) }
                    .thenByDescending { it.updatedAtMs },
            )
            .firstOrNull()
}
