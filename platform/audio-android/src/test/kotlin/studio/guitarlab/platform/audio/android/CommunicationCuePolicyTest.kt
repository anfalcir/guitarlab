package studio.guitarlab.platform.audio.android

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CommunicationCuePolicyTest {
    @Test fun triesNonModeCandidateBeforeModeInCommunication() {
        assertEquals(listOf(false, true), CommunicationCuePolicy.modeCandidates)
    }

    @Test fun cancellationAndSamePhysicalEndpointDoNotEscalate() {
        assertFalse(CommunicationCuePolicy.shouldAttemptAfterMedia(CuePreflightStatus.CANCELLED))
        assertFalse(CommunicationCuePolicy.shouldAttemptAfterMedia(CuePreflightStatus.EXPECTED_PAIR_NOT_DISTINCT))
    }

    @Test fun mediaConvergenceCanEscalateToCommunicationStrategy() {
        assertTrue(CommunicationCuePolicy.shouldAttemptAfterMedia(CuePreflightStatus.CONVERGED_TO_MAIN))
        assertTrue(CommunicationCuePolicy.shouldAttemptAfterMedia(CuePreflightStatus.WRONG_OR_MIRRORED_ROUTE))
    }

    @Test fun communicationProfileRetainsRateAdaptation() {
        val profile = CueOutputProfile(
            44_100, 44_100, 48_000,
            CueOutputStrategy.COMMUNICATION_SPLIT,
            communicationModeRequired = true,
        )
        assertTrue(profile.requiresCueResampling)
        assertTrue(profile.communicationModeRequired)
        assertEquals(CueOutputStrategy.COMMUNICATION_SPLIT, profile.strategy)
    }
}
