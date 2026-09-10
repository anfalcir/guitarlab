package studio.guitarlab.app

import android.media.AudioFormat
import android.media.MediaCodecList
import android.media.MediaExtractor
import android.media.MediaFormat
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.nio.ByteBuffer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import studio.guitarlab.core.codec.AudioCodecException
import studio.guitarlab.core.codec.FloatWavFileWriter
import studio.guitarlab.platform.codec.android.AndroidMasterAudioEncoder
import studio.guitarlab.platform.codec.android.MasterExportFormat

@RunWith(AndroidJUnit4::class)
class AndroidMasterAudioEncoderInstrumentedTest {
    @Test
    fun flacEncoderProducesExtractableStereo48kFile() {
        withScratchDir { dir ->
            val source = writeStereoFloatWav(File(dir, "source.wav"))
            val encoded = File(dir, "master.flac")

            AndroidMasterAudioEncoder.encode(source, encoded, MasterExportFormat.FLAC)

            assertTrue("FLAC output must be non-empty", encoded.isFile && encoded.length() > 0L)
            assertFlacStreamMarker(encoded)

            // Android's native FLACExtractor decodes FLAC and intentionally exposes
            // its track to MediaExtractor as audio/raw. The container identity is
            // therefore proven by the fLaC marker plus successful extraction/decoding,
            // not by expecting audio/flac from getTrackFormat().
            assertExtractableAudio(
                file = encoded,
                expectedMime = MediaFormat.MIMETYPE_AUDIO_RAW,
                expectedSampleRateHz = SAMPLE_RATE_HZ,
                expectedChannels = CHANNELS,
            )
        }
    }

    @Test
    fun mp3EncoderEitherProducesExtractableAudioOrFailsWithControlledCapabilityError() {
        withScratchDir { dir ->
            val source = writeStereoFloatWav(File(dir, "source.wav"))
            val encoded = File(dir, "master.mp3")
            val advertisedEncoder = findMp3EncoderName()

            if (advertisedEncoder == null) {
                val error = assertThrows(AudioCodecException::class.java) {
                    AndroidMasterAudioEncoder.encode(source, encoded, MasterExportFormat.MP3)
                }
                assertTrue(
                    "Missing MP3 encoder must surface a controlled codec/capability message",
                    error.message?.contains("MP3", ignoreCase = true) == true,
                )
                assertFalse("Failed encode must not leave a destination file", encoded.exists())
            } else {
                AndroidMasterAudioEncoder.encode(source, encoded, MasterExportFormat.MP3)
                assertTrue("MP3 output must be non-empty", encoded.isFile && encoded.length() > 0L)
                assertExtractableAudio(
                    file = encoded,
                    expectedMime = MediaFormat.MIMETYPE_AUDIO_MPEG,
                    expectedSampleRateHz = SAMPLE_RATE_HZ,
                    expectedChannels = CHANNELS,
                )
            }
        }
    }

    private fun findMp3EncoderName(): String? {
        val format = MediaFormat.createAudioFormat(
            MediaFormat.MIMETYPE_AUDIO_MPEG,
            SAMPLE_RATE_HZ,
            CHANNELS,
        ).apply {
            setInteger(MediaFormat.KEY_PCM_ENCODING, AudioFormat.ENCODING_PCM_16BIT)
            setInteger(MediaFormat.KEY_BIT_RATE, 320_000)
        }
        return runCatching {
            MediaCodecList(MediaCodecList.REGULAR_CODECS).findEncoderForFormat(format)
        }.getOrNull()
    }

    private fun assertFlacStreamMarker(file: File) {
        val marker = ByteArray(4)
        file.inputStream().use { input ->
            assertEquals("FLAC output must contain a complete stream marker", marker.size, input.read(marker))
        }
        assertEquals(
            "FLAC output must start with the native fLaC stream marker",
            "fLaC",
            marker.toString(Charsets.US_ASCII),
        )
    }

    private fun assertExtractableAudio(
        file: File,
        expectedMime: String,
        expectedSampleRateHz: Int,
        expectedChannels: Int,
    ) {
        val extractor = MediaExtractor()
        try {
            extractor.setDataSource(file.absolutePath)
            assertTrue("Encoded output must expose at least one media track", extractor.trackCount > 0)

            val trackIndex = (0 until extractor.trackCount).firstOrNull { index ->
                extractor.getTrackFormat(index).getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true
            }
            assertNotNull("Encoded output must expose an audio track", trackIndex)

            val index = trackIndex!!
            val format = extractor.getTrackFormat(index)
            assertEquals(expectedMime, format.getString(MediaFormat.KEY_MIME))
            assertEquals(expectedSampleRateHz, format.getInteger(MediaFormat.KEY_SAMPLE_RATE))
            assertEquals(expectedChannels, format.getInteger(MediaFormat.KEY_CHANNEL_COUNT))

            extractor.selectTrack(index)
            val firstPacket = ByteBuffer.allocate(64 * 1024)
            assertTrue(
                "Extractor must read audio payload, not only recognize a header",
                extractor.readSampleData(firstPacket, 0) > 0,
            )
        } finally {
            extractor.release()
        }
    }

    private fun writeStereoFloatWav(file: File): File {
        val frames = SAMPLE_RATE_HZ
        val samples = FloatArray(frames * CHANNELS)
        for (frame in 0 until frames) {
            val phase = (frame % 480) / 480f
            val sample = (phase * 2f - 1f) * 0.25f
            samples[frame * CHANNELS] = sample
            samples[frame * CHANNELS + 1] = -sample
        }
        FloatWavFileWriter(file, SAMPLE_RATE_HZ, CHANNELS).use { writer ->
            writer.writeInterleaved(samples, frames)
        }
        return file
    }

    private inline fun withScratchDir(block: (File) -> Unit) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val dir = File(context.cacheDir, "codec-instrumented-${System.nanoTime()}").apply { mkdirs() }
        try {
            block(dir)
        } finally {
            dir.deleteRecursively()
        }
    }

    private companion object {
        const val SAMPLE_RATE_HZ = 48_000
        const val CHANNELS = 2
    }
}
