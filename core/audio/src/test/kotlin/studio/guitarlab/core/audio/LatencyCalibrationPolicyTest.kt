package studio.guitarlab.core.audio

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LatencyCalibrationPolicyTest {
    @Test
    fun stableMeasurementsAreAcceptedAndMedianIsUsed() {
        val result = LatencyCalibrationPolicy.evaluate(
            measurementsFrames = listOf(4700, 4704, 4698, 4702, 4701),
            confidences = listOf(.91f, .93f, .92f, .94f, .90f),
            sampleRateHz = 48_000,
            elapsedMs = 8_000,
            measuredAtEpochMs = 1L,
        )
        assertEquals(4701, result.latencyFrames)
        assertTrue(result.accepted)
    }

    @Test
    fun lowConfidenceIsRejected() {
        val result = LatencyCalibrationPolicy.evaluate(
            listOf(2000, 2001, 1999), listOf(.2f, .3f, .25f), 48_000, 4_000, 1L,
        )
        assertFalse(result.accepted)
    }

    @Test
    fun compensationMovesTakeEarlierAndTrimsWhenTimelineCannotGoNegative() {
        assertEquals(CompensatedTakePlacement(38_000, 0, 48_000), LatencyCompensationPolicy.compensate(40_000, 48_000, 2_000))
        assertEquals(CompensatedTakePlacement(0, 1_000, 47_000), LatencyCompensationPolicy.compensate(1_000, 48_000, 2_000))
    }
}
