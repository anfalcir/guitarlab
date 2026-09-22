package studio.guitarlab.core.audio

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LatencyCalibrationStimulusPolicyTest {
    @Test fun chirpIsDeterministicWindowedAndLowLevelAtSupportedRates() {
        for (rate in listOf(44_100, 48_000, 88_200, 96_000)) {
            for (gain in LatencyCalibrationStimulusPolicy.adaptiveGains) {
                val first = LatencyCalibrationStimulusPolicy.generate(rate, gain)
                val second = LatencyCalibrationStimulusPolicy.generate(rate, gain)
                assertContentEquals(first, second)
                assertTrue(first.size >= (rate * 0.030).toInt())
                assertTrue(first.size <= (rate * 0.034).toInt())
                assertTrue(first.maxOf { abs(it) } <= gain + 1e-6f)
                assertTrue(abs(first.first()) < 1e-6f)
                assertTrue(abs(first.last()) < 1e-6f)
                assertTrue(first.any { abs(it) > gain * 0.25f })
            }
        }
    }

    @Test fun gainSequenceStartsQuietAndNeverApproachesLegacyBurstLevel() {
        assertEquals(listOf(0.03f, 0.06f, 0.12f), LatencyCalibrationStimulusPolicy.adaptiveGains)
        assertTrue(LatencyCalibrationStimulusPolicy.adaptiveGains.last() < 0.20f)
    }
}
