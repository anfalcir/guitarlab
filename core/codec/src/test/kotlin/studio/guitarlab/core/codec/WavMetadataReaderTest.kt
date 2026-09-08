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
    fun rejectsTruncatedOrNonWaveInput() {
        assertFailsWith<AudioCodecException> {
            WavMetadataReader().read(ByteArraySeekableSource(byteArrayOf(1, 2, 3)))
        }
        assertFailsWith<AudioCodecException> {
            WavMetadataReader().read(ByteArraySeekableSource("not-a-wave-file".toByteArray()))
        }
    }
}
