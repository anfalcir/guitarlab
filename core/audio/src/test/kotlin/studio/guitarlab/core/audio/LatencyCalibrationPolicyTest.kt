package studio.guitarlab.core.audio

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LatencyCalibrationPolicyTest {
    @Test fun stableMeasurementsAreAcceptedAndMedianIsUsed() {
        val result = LatencyCalibrationPolicy.evaluate(
            measurementsFrames = listOf(4700, 4704, 4698, 4702, 4701),
            confidences = listOf(.91f, .93f, .92f, .94f, .90f),
            sampleRateHz = 48_000,
            elapsedMs = 8_000,
            measuredAtEpochMs = 1L,
        )
        assertEquals(4701, result.latencyFrames)
        assertEquals(5, result.attempts)
        assertTrue(result.accepted)
    }

    @Test fun lowConfidenceIsRejected() {
        val result = LatencyCalibrationPolicy.evaluate(
            listOf(2000, 2001, 1999), listOf(.2f, .3f, .25f), 48_000, 4_000, 1L,
        )
        assertFalse(result.accepted)
    }

    @Test fun `too few attempts are diagnostic only and never auto-applied`() {
        val result = LatencyCalibrationPolicy.evaluate(
            listOf(2000, 2001), listOf(.95f, .95f), 48_000, 4_000, 1L,
        )
        assertFalse(result.accepted)
        assertEquals(2, result.attempts)
    }

    @Test fun `excessive jitter or drift rejects otherwise confident calibration`() {
        val jittery = LatencyCalibrationPolicy.evaluate(
            listOf(0L, 4_800L, 9_600L), listOf(.95f, .95f, .95f), 48_000, 10_000L, 1L,
        )
        assertFalse(jittery.accepted)
        val drifting = LatencyCalibrationPolicy.evaluate(
            listOf(1_000L, 1_000L, 2_000L), listOf(.95f, .95f, .95f), 48_000, 1_000L, 1L,
        )
        assertFalse(drifting.accepted)
        assertTrue(kotlin.math.abs(drifting.driftPpm) > LatencyCalibrationPolicy.MAX_ABS_DRIFT_PPM)
    }

    @Test fun `negative latency measurement is rejected at policy boundary`() {
        assertFailsWith<IllegalArgumentException> {
            LatencyCalibrationPolicy.evaluate(listOf(1L, -1L, 1L), listOf(.9f, .9f, .9f), 48_000, 1_000L, 1L)
        }
    }

    @Test fun `non finite confidence is rejected at policy boundary`() {
        assertFailsWith<IllegalArgumentException> {
            LatencyCalibrationPolicy.evaluate(listOf(1L, 1L, 1L), listOf(.9f, Float.NaN, .9f), 48_000, 1_000L, 1L)
        }
    }

    @Test fun compensationMovesTakeEarlierAndTrimsWhenTimelineCannotGoNegative() {
        assertEquals(CompensatedTakePlacement(38_000, 0, 48_000), LatencyCompensationPolicy.compensate(40_000, 48_000, 2_000))
        assertEquals(CompensatedTakePlacement(0, 1_000, 47_000), LatencyCompensationPolicy.compensate(1_000, 48_000, 2_000))
        assertEquals(CompensatedTakePlacement(40_000, 0, 48_000), LatencyCompensationPolicy.compensate(40_000, 48_000, 0))
    }

    @Test fun `calibration scope key cannot cross route or sample rate`() {
        val base = LatencyCalibrationScopeKeyPolicy.storageKey("route3:usb:input-a", "route3:usb:output-a", 44_100)
        val otherInput = LatencyCalibrationScopeKeyPolicy.storageKey("route3:usb:input-b", "route3:usb:output-a", 44_100)
        val otherOutput = LatencyCalibrationScopeKeyPolicy.storageKey("route3:usb:input-a", "route3:usb:output-b", 44_100)
        val otherRate = LatencyCalibrationScopeKeyPolicy.storageKey("route3:usb:input-a", "route3:usb:output-a", 48_000)
        assertNotEquals(base, otherInput)
        assertNotEquals(base, otherOutput)
        assertNotEquals(base, otherRate)
        assertNull(LatencyCalibrationScopeKeyPolicy.storageKey(null, "route3:usb:output-a", 44_100))
        assertNull(LatencyCalibrationScopeKeyPolicy.storageKey("route3:usb:input-a", null, 44_100))
        assertNotEquals(
            LatencyCalibrationScopeKeyPolicy.storageKey("ab|c", "d", 48_000),
            LatencyCalibrationScopeKeyPolicy.storageKey("ab", "c|d", 48_000),
        )
    }
}

class LatencyFineAdjustmentPolicyTest {
    @Test fun `fine adjustment round trips every supported session rate`() {
        for (rate in listOf(44_100, 48_000, 88_200, 96_000)) {
            val frames = LatencyFineAdjustmentPolicy.millisecondsToFrames(5.0, rate)
            assertEquals(5.0, LatencyFineAdjustmentPolicy.framesToMilliseconds(frames, rate), 0.03)
        }
    }

    @Test fun `fine adjustment is signed bounded and zero is neutral`() {
        assertEquals(24_000L, LatencyFineAdjustmentPolicy.millisecondsToFrames(999.0, 48_000))
        assertEquals(-24_000L, LatencyFineAdjustmentPolicy.clampFrames(-99_999L, 48_000))
        assertEquals(0L, LatencyFineAdjustmentPolicy.millisecondsToFrames(0.0, 48_000))
        assertTrue(LatencyFineAdjustmentPolicy.millisecondsToFrames(1.0, 48_000) > 0L)
        assertTrue(LatencyFineAdjustmentPolicy.millisecondsToFrames(-1.0, 48_000) < 0L)
    }

    @Test fun `five hundred millisecond residual bound is exact at every supported session rate`() {
        val expected = mapOf(44_100 to 22_050L, 48_000 to 24_000L, 88_200 to 44_100L, 96_000 to 48_000L)
        expected.forEach { (rate, frames) ->
            assertEquals(frames, LatencyFineAdjustmentPolicy.millisecondsToFrames(500.0, rate))
            assertEquals(frames, LatencyFineAdjustmentPolicy.millisecondsToFrames(5_000.0, rate))
            assertEquals(-frames, LatencyFineAdjustmentPolicy.millisecondsToFrames(-5_000.0, rate))
            assertEquals(frames, LatencyFineAdjustmentPolicy.clampFrames(Long.MAX_VALUE, rate))
            assertEquals(-frames, LatencyFineAdjustmentPolicy.clampFrames(Long.MIN_VALUE, rate))
        }
    }

    @Test fun `non finite residual input is rejected`() {
        assertFailsWith<IllegalArgumentException> { LatencyFineAdjustmentPolicy.millisecondsToFrames(Double.NaN, 48_000) }
        assertFailsWith<IllegalArgumentException> { LatencyFineAdjustmentPolicy.millisecondsToFrames(Double.POSITIVE_INFINITY, 48_000) }
    }
}
