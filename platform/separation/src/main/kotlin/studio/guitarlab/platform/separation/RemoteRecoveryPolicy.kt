package studio.guitarlab.platform.separation

import studio.guitarlab.core.model.PreparationStatus

object RemoteRecoveryPolicy {
    fun shouldRestoreSourceReady(status: PreparationStatus?, hasActiveJob: Boolean): Boolean =
        status == PreparationStatus.SEPARATING && !hasActiveJob
}
