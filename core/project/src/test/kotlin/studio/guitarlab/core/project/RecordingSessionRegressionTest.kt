package studio.guitarlab.core.project

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import studio.guitarlab.core.model.*

class RecordingSessionRegressionTest {
    private fun project(armed: Int) = GuitarProject(id = "p", name = "P", template = ProjectTemplate.BLANK,
        createdAtEpochMs = 1, updatedAtEpochMs = 1,
        tracks = List(3) { AudioTrack("t$it", "T$it", armed = it < armed, order = it) })

    @Test fun completeStateMachineIsDeterministicAndResetIsSafeFromEveryPhase() {
        var state = RecordingSessionPolicy.begin(project(1), 12_345)
        assertEquals(RecordingSessionPhase.COUNTDOWN, state.phase)
        assertEquals("t0", state.targetTrackId)
        assertEquals(12_345, state.timelineStartFrame)
        while (!state.readyToOpenCapture) state = RecordingSessionPolicy.tickCountdown(state)
        state = RecordingSessionPolicy.markCaptureStarted(state)
        state = RecordingSessionPolicy.updateCapturedFrames(state, 1)
        state = RecordingSessionPolicy.updateCapturedFrames(state, 48_000)
        assertFailsWith<IllegalArgumentException> { RecordingSessionPolicy.updateCapturedFrames(state, 47_999) }
        state = RecordingSessionPolicy.beginFinalizing(state)
        assertTrue(state.active)
        assertFalse(RecordingSessionPolicy.reset().active)
    }

    @Test fun invalidStartsAndInvalidTransitionsAreRejectedWithoutPartialState() {
        assertTrue(RecordingSessionPolicy.validateStart(project(0), 0) is RecordingStartDecision.Rejected)
        assertTrue(RecordingSessionPolicy.validateStart(project(2), 0) is RecordingStartDecision.Rejected)
        assertTrue(RecordingSessionPolicy.validateStart(project(1), -1) is RecordingStartDecision.Rejected)
        assertFailsWith<IllegalStateException> { RecordingSessionPolicy.begin(project(0), 0) }
        assertFailsWith<IllegalArgumentException> { RecordingSessionPolicy.markCaptureStarted(RecordingSessionState()) }
        assertFailsWith<IllegalArgumentException> { RecordingSessionPolicy.beginFinalizing(RecordingSessionState()) }
    }
}
