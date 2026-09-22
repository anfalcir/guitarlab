package studio.guitarlab.core.audio

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RecordingDurationTimingStressTest {
    @Test
    fun placementArithmeticIsSafeForOneThreeFiveAndTenMinuteTakesAtAllSupportedRates() {
        val rates = listOf(44_100, 48_000, 88_200, 96_000)
        val minutes = listOf(1, 3, 5, 10)
        for (rate in rates) for (durationMinutes in minutes) {
            val captured = Math.multiplyExact(rate.toLong(), durationMinutes.toLong() * 60L)
            val placement = RecordingTimingCompensationPolicy.compensate(
                requestedTimelineStartFrame = 17_000L,
                capturedFrames = captured,
                startupOffsetFrames = -(rate / 5L),
                roundTripLatencyFrames = rate / 20L,
                fineAdjustmentFrames = -(rate / 200L),
            )
            assertTrue(placement.timelineStartFrame >= 0L)
            assertTrue(placement.sourceStartFrame >= 0L)
            assertTrue(placement.lengthFrames > 0L)
            assertEquals(captured, placement.sourceStartFrame + placement.lengthFrames)
        }
    }

    @Test
    fun extremeMetadataSaturatesInsteadOfOverflowingOrCreatingNegativeState() {
        val placement = RecordingTimingCompensationPolicy.compensate(
            requestedTimelineStartFrame = Long.MAX_VALUE - 4,
            capturedFrames = 57_600_000L,
            startupOffsetFrames = Long.MAX_VALUE,
            roundTripLatencyFrames = Long.MAX_VALUE,
            fineAdjustmentFrames = Long.MIN_VALUE,
        )
        assertTrue(placement.timelineStartFrame >= 0L)
        assertTrue(placement.sourceStartFrame >= 0L)
        assertTrue(placement.lengthFrames in 1L..57_600_000L)
    }
}
