package studio.guitarlab.platform.separation

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import studio.guitarlab.core.model.PreparationStatus

class RemoteRecoveryPolicyTest {
    @Test fun orphanedSeparationIsRecoveredAfterProcessOrDeviceRestart() {
        assertTrue(RemoteRecoveryPolicy.shouldRestoreSourceReady(PreparationStatus.SEPARATING, hasActiveJob = false))
    }

    @Test fun durableActiveJobIsNeverMistakenForAnOrphan() {
        assertFalse(RemoteRecoveryPolicy.shouldRestoreSourceReady(PreparationStatus.SEPARATING, hasActiveJob = true))
        assertFalse(RemoteRecoveryPolicy.shouldRestoreSourceReady(PreparationStatus.READY, hasActiveJob = false))
    }
}
