package studio.guitarlab.core.audio

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AudioProbePolicyTest {
    @Test
    fun usbDeviceAdvertisingOnly44100KeepsNativeRate() {
        assertEquals(listOf(44_100), AudioProbePolicy.sampleRateCandidates(listOf(44_100)))
    }

    @Test
    fun genericUnknownDeviceUsesStableRateOrder() {
        assertEquals(
            listOf(48_000, 44_100, 96_000, 88_200),
            AudioProbePolicy.sampleRateCandidates(emptyList())
        )
    }

    @Test
    fun inputPrefersMonoAndOutputPrefersStereo() {
        assertEquals(listOf(1, 2), AudioProbePolicy.channelCandidates(listOf(1, 2), AudioDirection.INPUT))
        assertEquals(listOf(2, 1), AudioProbePolicy.channelCandidates(listOf(1, 2), AudioDirection.OUTPUT))
    }

    @Test
    fun floatIsPreferredWhenAdvertised() {
        assertEquals(
            listOf(PcmEncoding.FLOAT_32, PcmEncoding.PCM_16),
            AudioProbePolicy.encodingCandidates(listOf(PcmEncoding.PCM_16, PcmEncoding.FLOAT_32))
        )
    }

    @Test
    fun digitalZeroCaptureNeverPassesInputGate() {
        assertFalse(AudioProbePolicy.hasUsableInputSignal(AudioSignalStats(frames = 24_000, peak = 0f, rms = 0f)))
        assertTrue(AudioProbePolicy.hasUsableInputSignal(AudioSignalStats(frames = 24_000, peak = 0.001f, rms = 0.0002f)))
    }

    @Test
    fun duplexPrimeTargetsFiftyMillisecondsWithoutFillingBuffer() {
        assertEquals(2_400, AudioProbePolicy.duplexPrimeFrames(sampleRateHz = 48_000, bufferFrames = 12_288))
        assertEquals(1_280, AudioProbePolicy.duplexPrimeFrames(sampleRateHz = 48_000, bufferFrames = 3_840))
        assertEquals(0, AudioProbePolicy.duplexPrimeFrames(sampleRateHz = 0, bufferFrames = 3_840))
    }

    @Test
    fun underrunDeltaNeverReportsPreexistingOrNegativeCounts() {
        assertEquals(0, AudioProbePolicy.underrunDelta(baseline = 1, finalCount = 1))
        assertEquals(2, AudioProbePolicy.underrunDelta(baseline = 1, finalCount = 3))
        assertEquals(0, AudioProbePolicy.underrunDelta(baseline = 3, finalCount = 1))
    }
}
