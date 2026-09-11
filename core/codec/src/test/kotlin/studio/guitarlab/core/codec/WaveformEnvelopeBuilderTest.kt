package studio.guitarlab.core.codec

import kotlin.test.Test
import kotlin.test.assertEquals

class WaveformEnvelopeBuilderTest {
    @Test
    fun buildsPeakEnvelopeWithoutChangingAudio() {
        val decoder = FakeDecoder(
            samples = floatArrayOf(0f, 0.25f, -0.5f, 0.1f, 0.75f, -1f, 0.2f, 0.3f),
            channels = 1,
        )
        val envelope = WaveformEnvelopeBuilder.build(decoder, targetPoints = 4)
        assertEquals(listOf(0.25f, 0.5f, 1f, 0.3f), envelope.peaks)
    }

    private class FakeDecoder(
        private val samples: FloatArray,
        private val channels: Int,
    ) : AudioFrameDecoder {
        override val metadata = AudioMetadata(
            fileFormat = AudioFileFormat.WAV,
            sampleRateHz = 48_000,
            channelCount = channels,
            sampleEncoding = AudioSampleEncoding.FLOAT32_LE,
            bitsPerSample = 32,
            totalFrames = samples.size.toLong() / channels,
            durationUs = samples.size.toLong() / channels * 1_000_000L / 48_000L,
        )
        override var positionFrames: Long = 0L
            private set

        override fun seekToFrame(frame: Long) {
            positionFrames = frame.coerceIn(0L, metadata.totalFrames)
        }

        override fun readInterleaved(destination: FloatArray, destinationFrameOffset: Int, frameCount: Int): Int {
            val readable = minOf(frameCount.toLong(), metadata.totalFrames - positionFrames).toInt()
            val sourceOffset = (positionFrames * channels).toInt()
            val sampleCount = readable * channels
            samples.copyInto(destination, destinationFrameOffset * channels, sourceOffset, sourceOffset + sampleCount)
            positionFrames += readable
            return readable
        }
    }
}
