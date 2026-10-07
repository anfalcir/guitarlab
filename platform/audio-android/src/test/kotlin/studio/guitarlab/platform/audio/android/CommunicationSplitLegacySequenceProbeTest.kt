package studio.guitarlab.platform.audio.android

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CommunicationSplitLegacySequenceProbeTest {
    @Test
    fun gChangesOnlyOrderingWithoutMediaPrecondition() {
        assertFalse(
            CommunicationSplitLegacySequencePolicy.requiresMediaPrecondition(
                CommunicationSplitLegacyScenario.G_COMMUNICATION_BEFORE_OPEN,
            ),
        )
    }

    @Test
    fun hReconstructsRc31MediaFirstPrecondition() {
        assertTrue(
            CommunicationSplitLegacySequencePolicy.requiresMediaPrecondition(
                CommunicationSplitLegacyScenario.H_MEDIA_PRECONDITION_THEN_COMMUNICATION,
            ),
        )
    }

    @Test
    fun windowsStayBoundedAndSignalRemainsLowLevel() {
        assertTrue(CommunicationSplitLegacySequencePolicy.PRECONDITION_MS in 500L..1_500L)
        assertTrue(CommunicationSplitLegacySequencePolicy.MUTED_COMMUNICATION_WARMUP_MS in 300L..1_200L)
        assertTrue(CommunicationSplitLegacySequencePolicy.AUDIBLE_WINDOW_MS in 1_500L..3_500L)
        assertTrue(CommunicationSplitLegacySequencePolicy.AMPLITUDE in 0.01f..0.10f)
    }
}
