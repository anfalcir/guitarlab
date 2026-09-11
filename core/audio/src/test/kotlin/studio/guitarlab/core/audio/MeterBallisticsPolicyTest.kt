package studio.guitarlab.core.audio

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MeterBallisticsPolicyTest {
    @Test
    fun attackIsImmediateAndPeakIsHeld() {
        val first = MeterBallisticsPolicy.update(MeterBallisticsState(), rawPeak = 0.8f, rawRms = 0.4f, nowMs = 1_000L)
        assertEquals(0.8f, first.peak)
        assertEquals(0.4f, first.rms)
        assertEquals(0.8f, first.heldPeak)

        val duringHold = MeterBallisticsPolicy.update(first, rawPeak = 0.1f, rawRms = 0.1f, nowMs = 1_500L)
        assertEquals(0.8f, duringHold.heldPeak)
        assertTrue(duringHold.peak < first.peak)
        assertTrue(duringHold.peak >= 0.1f)
    }

    @Test
    fun heldPeakDecaysAfterHoldWindow() {
        val first = MeterBallisticsPolicy.update(MeterBallisticsState(), rawPeak = 1f, rawRms = 0.5f, nowMs = 1_000L)
        val afterHold = MeterBallisticsPolicy.update(first, rawPeak = 0f, rawRms = 0f, nowMs = 2_000L)
        assertTrue(afterHold.heldPeak < 1f)
        assertTrue(afterHold.heldPeak > 0f)
        assertTrue(afterHold.rms < first.rms)
    }

    @Test
    fun resetReturnsSilentState() {
        assertEquals(MeterBallisticsState(), MeterBallisticsPolicy.reset())
    }
}
