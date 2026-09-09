package studio.guitarlab.core.codec

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FloatWavFileWriterTest {
    @Test
    fun writesReadableMonoFloatWavAndFinalizesHeader() {
        val file = File.createTempFile("guitarlab-recording-", ".wav")
        try {
            FloatWavFileWriter(file, sampleRateHz = 48_000, channelCount = 1).use { writer ->
                writer.writeInterleaved(floatArrayOf(-1f, -0.5f, 0f, 0.5f, 1f), frameCount = 5)
                assertEquals(5L, writer.totalFrames)
            }

            val metadata = FileSeekableByteSource(file).use { WavMetadataReader().read(it) }
            assertEquals(AudioFileFormat.WAV, metadata.fileFormat)
            assertEquals(AudioSampleEncoding.FLOAT32_LE, metadata.sampleEncoding)
            assertEquals(48_000, metadata.sampleRateHz)
            assertEquals(1, metadata.channelCount)
            assertEquals(32, metadata.bitsPerSample)
            assertEquals(5L, metadata.totalFrames)

            FileSeekableByteSource(file).use { source ->
                WavPcmDecoder(source).use { decoder ->
                    val decoded = FloatArray(5)
                    assertEquals(5, decoder.readInterleaved(decoded, frameCount = 5))
                    assertTrue(decoded.zip(floatArrayOf(-1f, -0.5f, 0f, 0.5f, 1f).asIterable()).all { (a, b) -> kotlin.math.abs(a - b) < 0.000001f })
                }
            }
        } finally {
            file.delete()
        }
    }

    @Test
    fun writesStereoFramesWithoutConfusingSamplesAndFrames() {
        val file = File.createTempFile("guitarlab-recording-stereo-", ".wav")
        try {
            FloatWavFileWriter(file, sampleRateHz = 44_100, channelCount = 2).use { writer ->
                writer.writeInterleaved(floatArrayOf(0.1f, -0.1f, 0.2f, -0.2f), frameCount = 2)
            }
            val metadata = FileSeekableByteSource(file).use { WavMetadataReader().read(it) }
            assertEquals(2L, metadata.totalFrames)
            assertEquals(2, metadata.channelCount)
        } finally {
            file.delete()
        }
    }
}
