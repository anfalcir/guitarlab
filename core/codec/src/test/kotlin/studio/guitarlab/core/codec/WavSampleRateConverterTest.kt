package studio.guitarlab.core.codec

import java.io.File
import java.security.MessageDigest
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.roundToLong
import kotlin.math.sin
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.io.path.createTempDirectory

class WavSampleRateConverterTest {
    @Test fun resamples44100To48000WithStableDurationAndTone() {
        val dir = createTempDirectory("guitarlab-resample-").toFile()
        val input = File(dir, "in.wav")
        val output = File(dir, "out.wav")
        val rate = 44_100
        val frames = rate
        FloatWavFileWriter(input, rate, 1).use { writer ->
            val samples = FloatArray(frames) { i -> (0.6 * sin(2.0 * PI * 1000.0 * i / rate)).toFloat() }
            writer.writeInterleaved(samples, frames)
        }
        val result = WavSampleRateConverter.convert(input, output, 48_000)
        assertEquals(48_000, result.targetRateHz)
        assertTrue(abs(result.targetFrames - 48_000L) <= 1L)
        FileSeekableByteSource(output).use { source ->
            val decoder = WavPcmDecoder(source)
            assertEquals(48_000, decoder.metadata.sampleRateHz)
            assertTrue(abs(decoder.metadata.totalFrames - 48_000L) <= 1L)
            val probe = FloatArray(4_800)
            decoder.readInterleaved(probe, frameCount = probe.size)
            val rms = kotlin.math.sqrt(probe.map { it * it }.average()).toFloat()
            assertTrue(rms in 0.35f..0.5f)
        }
        dir.deleteRecursively()
    }

    @Test fun downsamplingKeepsExpectedDuration() {
        val dir = createTempDirectory("guitarlab-resample-down-").toFile()
        val input = File(dir, "in.wav")
        val output = File(dir, "out.wav")
        FloatWavFileWriter(input, 48_000, 2).use { writer ->
            val samples = FloatArray(48_000 * 2) { i -> if (i % 2 == 0) 0.25f else -0.25f }
            writer.writeInterleaved(samples, 48_000)
        }
        val result = WavSampleRateConverter.convert(input, output, 44_100)
        assertTrue(abs(result.targetFrames - 44_100L) <= 1L)
        assertEquals(2, result.channelCount)
        dir.deleteRecursively()
    }

    @Test fun conversionMatrixPreservesDurationPitchChannelsAmplitudeAndOriginalBytes() {
        val rates = listOf(44_100 to 48_000, 48_000 to 44_100, 88_200 to 48_000, 96_000 to 48_000, 44_100 to 96_000)
        rates.forEach { (sourceRate, targetRate) ->
            val dir = createTempDirectory("guitarlab-src-matrix-").toFile()
            try {
                val input = File(dir, "in.wav")
                val output = File(dir, "out.wav")
                val frames = sourceRate / 10
                FloatWavFileWriter(input, sourceRate, 2).use { writer ->
                    val samples = FloatArray(frames * 2)
                    repeat(frames) { frame ->
                        samples[frame * 2] = (0.50 * sin(2.0 * PI * 440.0 * frame / sourceRate)).toFloat()
                        samples[frame * 2 + 1] = (0.25 * sin(2.0 * PI * 880.0 * frame / sourceRate)).toFloat()
                    }
                    writer.writeInterleaved(samples, frames)
                }
                val before = input.readBytes()
                val beforeDigest = MessageDigest.getInstance("SHA-256").digest(before)
                val result = WavSampleRateConverter.convert(input, output, targetRate)
                assertContentEquals(beforeDigest, MessageDigest.getInstance("SHA-256").digest(input.readBytes()))
                assertEquals((frames.toDouble() * targetRate / sourceRate).roundToLong(), result.targetFrames)
                FileSeekableByteSource(output).use { source ->
                    WavPcmDecoder(source).use { decoder ->
                        assertEquals(targetRate, decoder.metadata.sampleRateHz)
                        assertEquals(2, decoder.metadata.channelCount)
                        val decoded = FloatArray(result.targetFrames.toInt() * 2)
                        assertEquals(result.targetFrames.toInt(), decoder.readInterleaved(decoded, frameCount = result.targetFrames.toInt()))
                        val left = FloatArray(result.targetFrames.toInt()) { decoded[it * 2] }
                        val right = FloatArray(result.targetFrames.toInt()) { decoded[it * 2 + 1] }
                        assertTrue(rms(left) in 0.33f..0.38f, "$sourceRate->$targetRate left RMS=${rms(left)}")
                        assertTrue(rms(right) in 0.16f..0.20f, "$sourceRate->$targetRate right RMS=${rms(right)}")
                        assertTrue(estimateFrequency(left, targetRate) in 430.0..450.0)
                        assertTrue(estimateFrequency(right, targetRate) in 860.0..900.0)
                    }
                }
            } finally { dir.deleteRecursively() }
        }
    }

    @Test fun sameRateIsByteExactCopyAndDoesNotAliasOutputToInput() {
        val dir = createTempDirectory("guitarlab-src-same-").toFile()
        try {
            val input = File(dir, "in.wav")
            val output = File(dir, "out.wav")
            FloatWavFileWriter(input, 48_000, 1).use { it.writeInterleaved(floatArrayOf(-1f, 0f, 1f), 3) }
            val expected = input.readBytes()
            val result = WavSampleRateConverter.convert(input, output, 48_000)
            assertEquals(3, result.targetFrames)
            assertContentEquals(expected, output.readBytes())
            output.writeBytes(byteArrayOf(9))
            assertContentEquals(expected, input.readBytes())
        } finally { dir.deleteRecursively() }
    }

    private fun rms(samples: FloatArray): Float = kotlin.math.sqrt(samples.map { it * it }.average()).toFloat()

    private fun estimateFrequency(samples: FloatArray, sampleRate: Int): Double {
        var crossings = 0
        for (i in 1 until samples.size) if (samples[i - 1] <= 0f && samples[i] > 0f) crossings++
        return crossings * sampleRate.toDouble() / samples.size
    }
}
