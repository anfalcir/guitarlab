package studio.guitarlab.core.codec

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class WavMetadataReaderTest {
    @Test
    fun readsStereoPcm16MetadataExactly() {
        val bytes = GoldenWavFactory.pcm16(
            sampleRate = 48_000,
            channels = 2,
            frames = listOf(
                shortArrayOf(1_000, -1_000),
                shortArrayOf(2_000, -2_000),
                shortArrayOf(3_000, -3_000),
            ),
        )

        val metadata = WavMetadataReader().read(ByteArraySeekableSource(bytes))

        assertEquals(AudioFileFormat.WAV, metadata.fileFormat)
        assertEquals(48_000, metadata.sampleRateHz)
        assertEquals(2, metadata.channelCount)
        assertEquals(AudioSampleEncoding.PCM_S16_LE, metadata.sampleEncoding)
        assertEquals(16, metadata.bitsPerSample)
        assertEquals(3, metadata.totalFrames)
        assertEquals(62L, metadata.durationUs)
        assertEquals(44L, metadata.dataOffsetBytes)
        assertEquals(12L, metadata.dataSizeBytes)
    }

    @Test
    fun recognizesPcm24AndFloat32WithoutMislabeling() {
        val pcm24 = WavMetadataReader().read(
            ByteArraySeekableSource(
                GoldenWavFactory.pcm24(44_100, 1, intArrayOf(-8_388_608, 0, 8_388_607))
            )
        )
        val float32 = WavMetadataReader().read(
            ByteArraySeekableSource(
                GoldenWavFactory.float32(96_000, 1, floatArrayOf(-1f, 0f, 1f))
            )
        )

        assertEquals(AudioSampleEncoding.PCM_S24_LE, pcm24.sampleEncoding)
        assertEquals(24, pcm24.bitsPerSample)
        assertEquals(AudioSampleEncoding.FLOAT32_LE, float32.sampleEncoding)
        assertEquals(32, float32.bitsPerSample)
    }

    @Test
    fun skipsUnknownOddSizedChunkWithRiffPadding() {
        val original = GoldenWavFactory.pcm16(
            sampleRate = 48_000,
            channels = 1,
            frames = listOf(shortArrayOf(100), shortArrayOf(200)),
        )
        val withJunk = insertChunkAfterWaveHeader(original, "JUNK", byteArrayOf(1, 2, 3))

        val metadata = WavMetadataReader().read(ByteArraySeekableSource(withJunk))

        assertEquals(2, metadata.totalFrames)
        assertEquals(48_000, metadata.sampleRateHz)
        assertEquals(1, metadata.channelCount)
    }

    @Test
    fun rejectsRiffSizeThatClaimsBytesBeyondFile() {
        val bytes = GoldenWavFactory.pcm16(48_000, 1, listOf(shortArrayOf(1))).copyOf()
        writeU32le(bytes, 4, bytes.size + 1024)

        assertFailsWith<AudioCodecException> {
            WavMetadataReader().read(ByteArraySeekableSource(bytes))
        }
    }

    @Test
    fun rejectsMisalignedDataChunk() {
        val bytes = GoldenWavFactory.pcm16(48_000, 2, listOf(shortArrayOf(1, 2))).copyOf()
        val dataSizeOffset = 40
        writeU32le(bytes, dataSizeOffset, 3)
        writeU32le(bytes, 4, bytes.size - 8)

        assertFailsWith<AudioCodecException> {
            WavMetadataReader().read(ByteArraySeekableSource(bytes))
        }
    }

    @Test
    fun rejectsTruncatedOrNonWaveInput() {
        assertFailsWith<AudioCodecException> {
            WavMetadataReader().read(ByteArraySeekableSource(byteArrayOf(1, 2, 3)))
        }
        assertFailsWith<AudioCodecException> {
            WavMetadataReader().read(ByteArraySeekableSource("not-a-wave-file".toByteArray()))
        }
    }

    private fun insertChunkAfterWaveHeader(original: ByteArray, id: String, payload: ByteArray): ByteArray {
        require(id.length == 4)
        val paddedPayloadSize = payload.size + (payload.size and 1)
        val chunk = ByteArray(8 + paddedPayloadSize)
        id.toByteArray(Charsets.US_ASCII).copyInto(chunk, 0)
        writeU32le(chunk, 4, payload.size)
        payload.copyInto(chunk, 8)

        val result = ByteArray(original.size + chunk.size)
        original.copyInto(result, 0, 0, 12)
        chunk.copyInto(result, 12)
        original.copyInto(result, 12 + chunk.size, 12)
        writeU32le(result, 4, result.size - 8)
        return result
    }

    private fun writeU32le(bytes: ByteArray, offset: Int, value: Int) {
        repeat(4) { shift -> bytes[offset + shift] = ((value ushr (shift * 8)) and 0xFF).toByte() }
    }
}
