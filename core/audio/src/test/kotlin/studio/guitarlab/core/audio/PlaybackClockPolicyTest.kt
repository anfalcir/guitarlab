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

    @Test
    fun presentationClockDoesNotAdvanceBeforeAudioOrigin() {
        val origin = 5_000_000_000L
        assertEquals(0L, PlaybackClockPolicy.presentedFramesAt(origin - 1L, origin, 48_000, 8_000L))
        assertEquals(0L, PlaybackClockPolicy.presentedFramesAt(origin, origin, 48_000, 8_000L))
    }

    @Test
    fun presentationClockMapsElapsedTimeAndNeverOutrunsWrittenAudio() {
        val origin = 5_000_000_000L
        val presented = PlaybackClockPolicy.presentedFramesAt(
            nowMonotonicNs = origin + 100_000_000L,
            presentationOriginMonotonicNs = origin,
            sampleRateHz = 48_000,
            maxWrittenFrames = 8_000L,
        )
        assertEquals(4_800L, presented)
        assertEquals(
            244_800L,
            PlaybackClockPolicy.timelineFrame(240_000L, presented, 480_000L, false, 0L, 480_000L),
        )
        assertEquals(
            2_000L,
            PlaybackClockPolicy.presentedFramesAt(origin + 1_000_000_000L, origin, 48_000, 2_000L),
        )
    }

}
