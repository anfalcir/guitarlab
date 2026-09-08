package studio.guitarlab.core.codec

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class WavPcmDecoderTest {
    @Test
    fun decodesStereoWithoutChannelSwapOrGainMutation() {
        val wav = GoldenWavFactory.pcm16(
            sampleRate = 48_000,
            channels = 2,
            frames = listOf(
                shortArrayOf(16_384, -8_192),
                shortArrayOf(8_192, -16_384),
            ),
        )
        val decoder = WavPcmDecoder(ByteArraySeekableSource(wav))
        val output = FloatArray(4)

        assertEquals(2, decoder.readInterleaved(output, frameCount = 2))
        assertNear(0.5f, output[0])
        assertNear(-0.25f, output[1])
        assertNear(0.25f, output[2])
        assertNear(-0.5f, output[3])
    }

    @Test
    fun seekIsFrameAccurate() {
        val wav = GoldenWavFactory.pcm16(
            sampleRate = 44_100,
            channels = 1,
            frames = listOf(
                shortArrayOf(1_000),
                shortArrayOf(2_000),
                shortArrayOf(3_000),
                shortArrayOf(4_000),
            ),
        )
        val decoder = WavPcmDecoder(ByteArraySeekableSource(wav))
        decoder.seekToFrame(2)
        val output = FloatArray(2)

        assertEquals(2, decoder.readInterleaved(output, frameCount = 2))
        assertNear(3_000 / 32768f, output[0])
        assertNear(4_000 / 32768f, output[1])
        assertEquals(4L, decoder.positionFrames)
        assertEquals(0, decoder.readInterleaved(output, frameCount = 1))
    }

    @Test
    fun decodesPcm24ExtremesAndFloat32() {
        val pcm24 = WavPcmDecoder(
            ByteArraySeekableSource(
                GoldenWavFactory.pcm24(48_000, 1, intArrayOf(-8_388_608, 0, 8_388_607))
            )
        )
        val pcmOut = FloatArray(3)
        assertEquals(3, pcm24.readInterleaved(pcmOut, frameCount = 3))
        assertNear(-1f, pcmOut[0])
        assertNear(0f, pcmOut[1])
        assertTrue(pcmOut[2] > 0.9999f)

        val floatDecoder = WavPcmDecoder(
            ByteArraySeekableSource(
                GoldenWavFactory.float32(48_000, 1, floatArrayOf(-0.75f, 0.25f, 1.25f))
            )
        )
        val floatOut = FloatArray(3)
        assertEquals(3, floatDecoder.readInterleaved(floatOut, frameCount = 3))
        assertNear(-0.75f, floatOut[0])
        assertNear(0.25f, floatOut[1])
        assertNear(1f, floatOut[2])
    }

    private fun assertNear(expected: Float, actual: Float, tolerance: Float = 0.00001f) {
        assertTrue(abs(expected - actual) <= tolerance, "expected=$expected actual=$actual")
    }
}
