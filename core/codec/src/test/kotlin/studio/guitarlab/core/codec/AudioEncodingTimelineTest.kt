package studio.guitarlab.core.codec

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class AudioEncodingTimelineTest {
    @Test
    fun timestampsStartAtZeroAndRemainMonotonicThroughEndOfStream() {
        val rate = 48_000
        val frames = listOf(0L, 1_024L, 2_048L, 48_000L)
        val timestamps = frames.map { AudioEncodingTimeline.presentationTimeUs(it, rate) }

        assertEquals(0L, timestamps.first())
        assertEquals(1_000_000L, timestamps.last())
        assertTrue(timestamps.zipWithNext().all { (left, right) -> right > left })
    }

    @Test
    fun timestampUsesStartFrameRatherThanEndFrameOfEncodedChunk() {
        val rate = 48_000
        val chunkFrames = 1_024L

        assertEquals(0L, AudioEncodingTimeline.presentationTimeUs(0L, rate))
        assertEquals(21_333L, AudioEncodingTimeline.presentationTimeUs(chunkFrames, rate))
    }

    @Test
    fun invalidTimelineInputsAreRejected() {
        assertFailsWith<IllegalArgumentException> { AudioEncodingTimeline.presentationTimeUs(-1L, 48_000) }
        assertFailsWith<IllegalArgumentException> { AudioEncodingTimeline.presentationTimeUs(0L, 0) }
    }
}
