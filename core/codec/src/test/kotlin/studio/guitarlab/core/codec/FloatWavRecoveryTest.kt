package studio.guitarlab.core.codec

import java.io.File
import java.io.RandomAccessFile
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FloatWavRecoveryTest {
    @Test
    fun repairsHeaderCapturedBeforeWriterFinishAndPreservesSamples() {
        val source = File.createTempFile("guitarlab-live-writer-", ".wav")
        val interrupted = File.createTempFile("guitarlab-interrupted-", ".recording.part.wav")
        try {
            val expected = floatArrayOf(0.1f, -0.1f, 0.2f, -0.2f)
            val writer = FloatWavFileWriter(source, sampleRateHz = 48_000, channelCount = 2)
            try {
                writer.writeInterleaved(expected, frameCount = 2)
                interrupted.writeBytes(source.readBytes())
            } finally {
                writer.close()
            }

            val before = FileSeekableByteSource(interrupted).use { WavMetadataReader().read(it) }
            assertEquals(0L, before.totalFrames)

            val result = FloatWavRecovery.repairInterrupted(interrupted)
            assertEquals(FloatWavRecoveryStatus.REPAIRED, result.status)
            assertEquals(2L, result.framesRecovered)
            assertEquals(0, result.trailingBytesDiscarded)

            val after = FileSeekableByteSource(interrupted).use { WavMetadataReader().read(it) }
            assertEquals(2L, after.totalFrames)
            assertEquals(2, after.channelCount)
            assertEquals(48_000, after.sampleRateHz)

            FileSeekableByteSource(interrupted).use { input ->
                WavPcmDecoder(input).use { decoder ->
                    val decoded = FloatArray(expected.size)
                    assertEquals(2, decoder.readInterleaved(decoded, frameCount = 2))
                    assertTrue(decoded.indices.all { abs(decoded[it] - expected[it]) < 0.000001f })
                }
            }
        } finally {
            source.delete()
            interrupted.delete()
        }
    }

    @Test
    fun trimsOnlyIncompleteTailFrameBeforeRepairingHeader() {
        val source = File.createTempFile("guitarlab-live-tail-", ".wav")
        val interrupted = File.createTempFile("guitarlab-interrupted-tail-", ".recording.part.wav")
        try {
            val writer = FloatWavFileWriter(source, sampleRateHz = 44_100, channelCount = 1)
            try {
                writer.writeInterleaved(floatArrayOf(0.25f, -0.25f), frameCount = 2)
                interrupted.writeBytes(source.readBytes())
            } finally {
                writer.close()
            }
            RandomAccessFile(interrupted, "rw").use { file ->
                file.seek(file.length())
                file.write(byteArrayOf(1, 2, 3))
            }

            val result = FloatWavRecovery.repairInterrupted(interrupted)
            assertEquals(FloatWavRecoveryStatus.REPAIRED, result.status)
            assertEquals(2L, result.framesRecovered)
            assertEquals(3, result.trailingBytesDiscarded)
            assertEquals(52L, interrupted.length())
            val metadata = FileSeekableByteSource(interrupted).use { WavMetadataReader().read(it) }
            assertEquals(2L, metadata.totalFrames)
        } finally {
            source.delete()
            interrupted.delete()
        }
    }

    @Test
    fun refusesToModifyNonCanonicalFloatWriterHeader() {
        val source = File.createTempFile("guitarlab-foreign-source-", ".wav")
        val foreign = File.createTempFile("guitarlab-foreign-", ".wav")
        try {
            val writer = FloatWavFileWriter(source, sampleRateHz = 48_000, channelCount = 1)
            try {
                writer.writeInterleaved(floatArrayOf(0.5f), frameCount = 1)
                val bytes = source.readBytes()
                bytes[20] = 1
                bytes[21] = 0
                foreign.writeBytes(bytes)
            } finally {
                writer.close()
            }
            val before = foreign.readBytes()

            val result = FloatWavRecovery.repairInterrupted(foreign)

            assertEquals(FloatWavRecoveryStatus.NOT_CANONICAL_GUITARLAB_FLOAT_WAV, result.status)
            assertContentEquals(before, foreign.readBytes())
        } finally {
            source.delete()
            foreign.delete()
        }
    }
}
