package studio.guitarlab.platform.audio.android

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CueOutputNegotiationTest {
    @Test
    fun prefersAdvertised48kWhenSession44kIsNotAdvertisedByCue() {
        assertEquals(
            listOf(48_000),
            CueOutputNegotiationPolicy.candidateCueSampleRates(
                sessionSampleRateHz = 44_100,
                cueAdvertisedSampleRates = listOf(48_000),
            ),
        )
    }

    @Test
    fun preservesSessionRateFirstWhenCueAdvertisesIt() {
        assertEquals(
            listOf(44_100, 48_000),
            CueOutputNegotiationPolicy.candidateCueSampleRates(
                sessionSampleRateHz = 44_100,
                cueAdvertisedSampleRates = listOf(48_000, 44_100),
            ),
        )
    }

    @Test
    fun fallsBackToSessionRateWhenCapabilitiesAreUnknown() {
        assertEquals(
            listOf(44_100),
            CueOutputNegotiationPolicy.candidateCueSampleRates(44_100, emptyList()),
        )
    }

    @Test
    fun streamingResamplerKeepsCumulativeDurationAcrossChunks() {
        val resampler = StereoLinearResampler(44_100, 48_000)
        var outputFrames = 0L
        repeat(44) {
            val frames = 1_000
            val input = FloatArray(frames * 2) { index -> ((index % 37) - 18) / 18f }
            outputFrames += resampler.process(input, frames).size / 2L
        }
        val tailFrames = 100
        outputFrames += resampler.process(FloatArray(tailFrames * 2), tailFrames).size / 2L

        val expected = 48_000L
        assertTrue("expected approximately $expected frames, got $outputFrames", kotlin.math.abs(outputFrames - expected) <= 2L)
    }

    @Test
    fun resetRestartsRatePhaseForSeek() {
        val resampler = StereoLinearResampler(44_100, 48_000)
        val input = FloatArray(1_024 * 2)
        val first = resampler.process(input, 1_024).size
        resampler.reset()
        val afterReset = resampler.process(input, 1_024).size
        assertEquals(first, afterReset)
    }
}
