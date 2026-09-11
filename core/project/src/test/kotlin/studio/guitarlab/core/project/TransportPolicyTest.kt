package studio.guitarlab.core.project

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TransportPolicyTest {
    @Test
    fun stoppedStateAllowsTimelineEditing() {
        val state = TransportState()
        assertTrue(TransportPolicy.timelineEditingEnabled(state))
        assertFalse(TransportPolicy.isActive(state))
    }

    @Test
    fun playAndRecordLockTimelineEditingUntilStop() {
        val playing = TransportPolicy.togglePlayStop(TransportState())
        assertEquals(TransportMode.PLAYING, playing.mode)
        assertFalse(TransportPolicy.timelineEditingEnabled(playing))
        val stopped = TransportPolicy.togglePlayStop(playing)
        assertEquals(TransportMode.STOPPED, stopped.mode)
        val recording = TransportPolicy.startRecording(stopped)
        assertEquals(TransportMode.RECORDING, recording.mode)
        assertFalse(TransportPolicy.timelineEditingEnabled(recording))
    }

    @Test
    fun loopAndReturnToStartCannotBeEditedDuringActiveTransport() {
        val stopped = TransportPolicy.toggleLoop(TransportState())
        assertTrue(stopped.loopEnabled)
        assertEquals(0L, TransportPolicy.returnToStartFrame(stopped, 12_000L))

        val playing = stopped.copy(mode = TransportMode.PLAYING)
        assertEquals(playing, TransportPolicy.toggleLoop(playing))
        assertEquals(12_000L, TransportPolicy.returnToStartFrame(playing, 12_000L))
    }
}
