package studio.guitarlab.core.audio

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RecordingTimingCompensationPolicyTest {
    @Test fun `capture starting half second before backing becomes source pre-roll trim`() {
        val captureNs = 10_000_000_000L
        val backingNs = captureNs + 500_000_000L
        val offset = RecordingTimingCompensationPolicy.startupOffsetFrames(captureNs, true, backingNs, true, 48_000)
        assertEquals(-24_000L, offset)
        assertEquals(
            RecordingTimingPlacement(0L, 24_000L, 24_000L, -24_000L, 0L, 0L),
            RecordingTimingCompensationPolicy.compensate(0L, 48_000L, offset, 0L),
        )
    }

    @Test fun `capture starting after backing shifts take later instead of inventing source trim`() {
        val backingNs = 10_000_000_000L
        val captureNs = backingNs + 25_000_000L
        val offset = RecordingTimingCompensationPolicy.startupOffsetFrames(captureNs, true, backingNs, true, 48_000)
        assertEquals(1_200L, offset)
        assertEquals(
            RecordingTimingPlacement(11_200L, 0L, 48_000L, 1_200L, 0L, 0L),
            RecordingTimingCompensationPolicy.compensate(10_000L, 48_000L, offset, 0L),
        )
    }

    @Test fun `simultaneous capture and backing have zero session delta`() {
        val origin = 8_000_000_000L
        assertEquals(0L, RecordingTimingCompensationPolicy.startupOffsetFrames(origin, true, origin, true, 44_100))
        assertEquals(RecordingTimingPlacement(3_000L, 0L, 2_000L, 0L, 0L, 0L), RecordingTimingCompensationPolicy.compensate(3_000L, 2_000L, 0L, 0L))
    }

    @Test fun `route and fine compensation are applied exactly once on the common timeline`() {
        assertEquals(
            RecordingTimingPlacement(7_100L, 0L, 48_000L, 0L, 2_400L, 500L),
            RecordingTimingCompensationPolicy.compensate(10_000L, 48_000L, 0L, 2_400L, 500L),
        )
        assertEquals(
            RecordingTimingPlacement(0L, 1_900L, 46_100L, -500L, 2_400L, 0L),
            RecordingTimingCompensationPolicy.compensate(1_000L, 48_000L, -500L, 2_400L),
        )
    }

    @Test fun `positive measured route latency advances the take and zero latency is neutral`() {
        assertEquals(8_000L, RecordingTimingCompensationPolicy.compensate(10_000L, 20_000L, 0L, 2_000L).timelineStartFrame)
        assertEquals(10_000L, RecordingTimingCompensationPolicy.compensate(10_000L, 20_000L, 0L, 0L).timelineStartFrame)
    }

    @Test fun `residual fine adjustment has explicit sign convention`() {
        val positive = RecordingTimingCompensationPolicy.compensate(10_000L, 20_000L, 0L, 0L, 500L)
        val negative = RecordingTimingCompensationPolicy.compensate(10_000L, 20_000L, 0L, 0L, -500L)
        assertEquals(9_500L, positive.timelineStartFrame) // positive advances
        assertEquals(10_500L, negative.timelineStartFrame) // negative delays
    }

    @Test fun `mixed timestamp bases fail closed instead of creating synthetic skew`() {
        assertEquals(0L, RecordingTimingCompensationPolicy.startupOffsetFrames(5_000_000L, true, 4_000_000L, false, 48_000))
        assertEquals(0L, RecordingTimingCompensationPolicy.startupOffsetFrames(5_000_000L, false, 4_000_000L, true, 48_000))
    }

    @Test fun `command fallback can align against command fallback and missing backing stays neutral`() {
        assertEquals(48L, RecordingTimingCompensationPolicy.startupOffsetFrames(2_001_000_000L, false, 2_000_000_000L, false, 48_000))
        assertEquals(0L, RecordingTimingCompensationPolicy.startupOffsetFrames(2_001_000_000L, false, null, false, 48_000))
    }

    @Test fun `signed startup mapping is sample rate exact at every supported session rate`() {
        val expectedAt10Ms = mapOf(44_100 to 441L, 48_000 to 480L, 88_200 to 882L, 96_000 to 960L)
        expectedAt10Ms.forEach { (rate, expected) ->
            val backingNs = 5_000_000_000L
            val captureNs = backingNs + 10_000_000L
            assertEquals(expected, RecordingTimingCompensationPolicy.startupOffsetFrames(captureNs, true, backingNs, true, rate))
        }
    }

    @Test fun `nanoseconds to frames uses deterministic nearest-frame rounding`() {
        val backingNs = 5_000_000_000L
        assertEquals(44L, RecordingTimingCompensationPolicy.startupOffsetFrames(backingNs + 1_000_000L, true, backingNs, true, 44_100))
        assertEquals(88L, RecordingTimingCompensationPolicy.startupOffsetFrames(backingNs + 1_000_000L, true, backingNs, true, 88_200))
        assertEquals(-44L, RecordingTimingCompensationPolicy.startupOffsetFrames(backingNs - 1_000_000L, true, backingNs, true, 44_100))
    }

    @Test fun `timeline bounds trim source rather than creating a negative frame`() {
        val placement = RecordingTimingCompensationPolicy.compensate(100L, 1_000L, -500L, 200L, 0L)
        assertEquals(0L, placement.timelineStartFrame)
        assertEquals(600L, placement.sourceStartFrame)
        assertEquals(400L, placement.lengthFrames)
    }

    @Test fun `extreme arithmetic saturates and never wraps into invalid placement`() {
        val cases = listOf(
            RecordingTimingCompensationPolicy.compensate(Long.MAX_VALUE, 64L, Long.MAX_VALUE, 0L, Long.MIN_VALUE),
            RecordingTimingCompensationPolicy.compensate(0L, 64L, Long.MIN_VALUE, Long.MAX_VALUE, Long.MAX_VALUE),
            RecordingTimingCompensationPolicy.compensate(Long.MAX_VALUE, 64L, Long.MIN_VALUE, Long.MAX_VALUE, Long.MIN_VALUE),
        )
        cases.forEach { placement ->
            assertTrue(placement.timelineStartFrame >= 0L)
            assertTrue(placement.sourceStartFrame in 0L until 64L)
            assertTrue(placement.lengthFrames in 1L..64L)
        }
        val maxOrigin = Long.MAX_VALUE - 1L
        val minPositiveOrigin = 1L
        assertTrue(RecordingTimingCompensationPolicy.startupOffsetFrames(maxOrigin, true, minPositiveOrigin, true, 96_000) in -480_000L..480_000L)
    }

    @Test fun `consecutive takes are stateless and cannot inherit prior compensation`() {
        val first = RecordingTimingCompensationPolicy.compensate(10_000L, 20_000L, 900L, 2_000L, 300L)
        val second = RecordingTimingCompensationPolicy.compensate(10_000L, 20_000L, 0L, 0L, 0L)
        assertEquals(8_600L, first.timelineStartFrame)
        assertEquals(10_000L, second.timelineStartFrame)
        assertEquals(0L, second.startupOffsetFrames)
        assertEquals(0L, second.routeLatencyFrames)
        assertEquals(0L, second.fineAdjustmentFrames)
    }

    @Test fun `placement invariants and determinism hold across one hundred thousand randomized combinations`() {
        val random = Random(23)
        repeat(100_000) {
            val captured = random.nextLong(2L, 500_000L)
            val requested = random.nextLong(0L, 2_000_000L)
            val startup = random.nextLong(-100_000L, 100_000L)
            val route = random.nextLong(0L, 20_000L)
            val fine = random.nextLong(-10_000L, 10_000L)
            val placement = RecordingTimingCompensationPolicy.compensate(requested, captured, startup, route, fine)
            val repeated = RecordingTimingCompensationPolicy.compensate(requested, captured, startup, route, fine)
            assertEquals(placement, repeated)
            assertTrue(placement.timelineStartFrame >= 0L)
            assertTrue(placement.sourceStartFrame in 0 until captured)
            assertEquals(captured - placement.sourceStartFrame, placement.lengthFrames)
            assertTrue(placement.lengthFrames > 0L)
        }
    }
}
