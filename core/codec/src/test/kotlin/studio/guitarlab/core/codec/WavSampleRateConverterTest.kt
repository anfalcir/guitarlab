package studio.guitarlab.core.codec

import java.io.File
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin
import kotlin.test.Test
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
}
