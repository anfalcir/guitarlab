package studio.guitarlab.core.codec

import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class StereoWavChannelSplitterTest {
    @Test
    fun `split creates independent synchronized mono channels`() {
        val dir = Files.createTempDirectory("guitarlab-stereo-split-").toFile()
        try {
            val input = dir.resolve("stereo.wav")
            FloatWavFileWriter(input, 48_000, 2).use { writer ->
                writer.writeInterleaved(floatArrayOf(0.1f, 0.9f, 0.2f, 0.8f, 0.3f, 0.7f), 3)
            }
            val left = dir.resolve("left.wav")
            val right = dir.resolve("right.wav")
            val result = StereoWavChannelSplitter.split(input, left, right)
            assertEquals(3L, result.totalFrames)
            assertEquals(48_000, result.sampleRateHz)
            val leftDecoder = FileSeekableByteSource(left).use { source ->
                val decoder = WavPcmDecoder(source)
                val values = FloatArray(3)
                assertEquals(3, decoder.readInterleaved(values, 0, 3))
                values.toList()
            }
            val rightDecoder = FileSeekableByteSource(right).use { source ->
                val decoder = WavPcmDecoder(source)
                val values = FloatArray(3)
                assertEquals(3, decoder.readInterleaved(values, 0, 3))
                values.toList()
            }
            assertTrue(kotlin.math.abs(leftDecoder[0] - 0.1f) < 0.0001f)
            assertTrue(kotlin.math.abs(leftDecoder[2] - 0.3f) < 0.0001f)
            assertTrue(kotlin.math.abs(rightDecoder[0] - 0.9f) < 0.0001f)
            assertTrue(kotlin.math.abs(rightDecoder[2] - 0.7f) < 0.0001f)
        } finally {
            dir.deleteRecursively()
        }
    }
}
