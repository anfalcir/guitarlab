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
    fun returnToStartWorksDuringPlaybackButNotRecording() {
        val stopped = TransportPolicy.toggleLoop(TransportState())
        assertTrue(stopped.loopEnabled)
        assertEquals(0L, TransportPolicy.returnToStartFrame(stopped, 12_000L))

        val playing = stopped.copy(mode = TransportMode.PLAYING)
        assertEquals(playing, TransportPolicy.toggleLoop(playing))
        assertEquals(0L, TransportPolicy.returnToStartFrame(playing, 12_000L))
        assertEquals(12_000L, TransportPolicy.returnToStartFrame(playing.copy(mode = TransportMode.RECORDING), 12_000L))
        assertTrue(TransportPolicy.playStopEnabled(playing, false))
        assertFalse(TransportPolicy.playStopEnabled(playing.copy(mode = TransportMode.RECORDING), true))
    }

    @Test
    fun playWithLoopNormalizesAnyOutsidePlayheadToLoopStart() {
        val looped = TransportState(loopEnabled = true)
        assertEquals(10_000L, TransportPolicy.playbackStartFrame(looped, 4_000L, 40_000L, 10_000L, 20_000L))
        assertEquals(10_000L, TransportPolicy.playbackStartFrame(looped, 20_000L, 40_000L, 10_000L, 20_000L))
        assertEquals(10_000L, TransportPolicy.playbackStartFrame(looped, 30_000L, 40_000L, 10_000L, 20_000L))
        assertEquals(14_000L, TransportPolicy.playbackStartFrame(looped, 14_000L, 40_000L, 10_000L, 20_000L))
    }

    @Test
    fun loopPlaybackPositionNeverEscapesLoopButRecordingPositionIsUntouched() {
        val playing = TransportState(mode = TransportMode.PLAYING, loopEnabled = true)
        assertEquals(10_000L, TransportPolicy.playbackPositionFrame(playing, 9_999L, 40_000L, 10_000L, 20_000L))
        assertEquals(15_000L, TransportPolicy.playbackPositionFrame(playing, 15_000L, 40_000L, 10_000L, 20_000L))
        assertEquals(10_000L, TransportPolicy.playbackPositionFrame(playing, 20_000L, 40_000L, 10_000L, 20_000L))

        val recording = playing.copy(mode = TransportMode.RECORDING)
        assertEquals(7_000L, TransportPolicy.playbackPositionFrame(recording, 7_000L, 40_000L, 10_000L, 20_000L))
    }

    @Test
    fun nonLoopPlaybackRetainsPreviousEndOfProjectRestartRule() {
        val stopped = TransportState(loopEnabled = false)
        assertEquals(12_000L, TransportPolicy.playbackStartFrame(stopped, 12_000L, 40_000L, 10_000L, 20_000L))
        assertEquals(0L, TransportPolicy.playbackStartFrame(stopped, 40_000L, 40_000L, 10_000L, 20_000L))
    }
}
