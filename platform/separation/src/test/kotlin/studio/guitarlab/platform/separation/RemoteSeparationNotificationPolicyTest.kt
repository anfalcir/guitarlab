package studio.guitarlab.platform.separation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import studio.guitarlab.core.separation.RemoteJobState

class RemoteSeparationNotificationPolicyTest {
    @Test fun everyBackgroundActiveStateProducesAnOngoingNotification() {
        val active = listOf(
            RemoteJobState.UPLOADING,
            RemoteJobState.READY,
            RemoteJobState.QUEUED,
            RemoteJobState.RUNNING,
            RemoteJobState.COMPLETED,
            RemoteJobState.IMPORTING,
            RemoteJobState.CANCEL_REQUESTED,
        )
        active.forEach { state ->
            assertTrue("Expected $state to remain ongoing", RemoteSeparationNotificationPolicy.copy(state).ongoing)
        }
    }

    @Test fun everyTerminalOrAttentionStateProducesADismissibleNotification() {
        RemoteSeparationNotificationPolicy.terminalStates.forEach { state ->
            assertFalse("Expected $state to be dismissible", RemoteSeparationNotificationPolicy.copy(state).ongoing)
        }
    }

    @Test fun transientRetryRemainsOngoingAndExplainsNextAttempt() {
        val copy = RemoteSeparationNotificationPolicy.copy(
            RemoteJobState.IMPORTING,
            "RETRY:DOWNLOADING_RESULTS:NETWORK_IO:ATTEMPT_2",
        )
        assertTrue(copy.ongoing)
        assertEquals("Falha temporária · nova tentativa agendada", copy.text)
    }

    @Test fun importFailureIsDismissibleButExplicitlyRecoverable() {
        val copy = RemoteSeparationNotificationPolicy.copy(RemoteJobState.IMPORT_FAILED)
        assertFalse(copy.ongoing)
        assertTrue(copy.text.contains("retomada"))
    }
}
