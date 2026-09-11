package studio.guitarlab.core.audio

import kotlin.test.Test
import kotlin.test.assertEquals

class PlaybackClockPolicyTest {
    @Test
    fun nonLoopPlaybackClampsAtProjectEnd() {
        assertEquals(15_000, PlaybackClockPolicy.timelineFrame(10_000, 5_000, 48_000, false, 0, 48_000))
        assertEquals(48_000, PlaybackClockPolicy.timelineFrame(40_000, 20_000, 48_000, false, 0, 48_000))
    }

    @Test
    fun loopPlaybackWrapsInsideLoopRange() {
        assertEquals(35_000, PlaybackClockPolicy.timelineFrame(30_000, 5_000, 48_000, true, 12_000, 36_000))
        assertEquals(13_000, PlaybackClockPolicy.timelineFrame(30_000, 7_000, 48_000, true, 12_000, 36_000))
        assertEquals(12_000, PlaybackClockPolicy.timelineFrame(36_000, 0, 48_000, true, 12_000, 36_000))
    }

    @Test
    fun playbackBeforeLoopRegionRunsForwardThenWraps() {
        assertEquals(8_000, PlaybackClockPolicy.timelineFrame(2_000, 6_000, 48_000, true, 12_000, 24_000))
        assertEquals(13_000, PlaybackClockPolicy.timelineFrame(2_000, 23_000, 48_000, true, 12_000, 24_000))
    }
}
