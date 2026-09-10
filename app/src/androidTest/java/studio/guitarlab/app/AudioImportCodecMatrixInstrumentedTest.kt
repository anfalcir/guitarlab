package studio.guitarlab.app

import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import kotlin.math.abs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import studio.guitarlab.core.codec.AudioImportFormat
import studio.guitarlab.core.codec.AudioImportFormatPolicy
import studio.guitarlab.core.codec.FileSeekableByteSource
import studio.guitarlab.core.codec.WavMetadataReader
import studio.guitarlab.core.codec.WavPcmDecoder
import studio.guitarlab.platform.codec.android.AndroidAudioImportTranscoder

@RunWith(AndroidJUnit4::class)
class AudioImportCodecMatrixInstrumentedTest {
    @Test
    fun allAdvertisedFormatsDecodeToNonSilentStereo48kEditingWav() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        fixtures.forEach { fixture ->
            val input = File(context.cacheDir, "matrix-${System.nanoTime()}-${fixture.fileName}")
            instrumentation.context.assets.open("codec-fixtures/${fixture.fileName}").use { source ->
                input.outputStream().buffered().use(source::copyTo)
            }
            try {
                assertEquals(
                    "Format policy must detect ${fixture.fileName}",
                    fixture.format,
                    AudioImportFormatPolicy.detect(fixture.fileName, fixture.mimeType),
                )
                val prepared = AndroidAudioImportTranscoder.prepare(context, Uri.fromFile(input), fixture.format)
                val output = prepared.wavFile
                try {
                    val metadata = FileSeekableByteSource(output).use(WavMetadataReader()::read)
                    assertEquals("${fixture.fileName} sample rate", SAMPLE_RATE_HZ, metadata.sampleRateHz)
                    assertEquals("${fixture.fileName} channel count", CHANNELS, metadata.channelCount)
                    assertTrue("${fixture.fileName} must contain decoded frames", metadata.totalFrames > 0L)

                    val samples = FloatArray(READ_FRAMES * CHANNELS)
                    val frames = FileSeekableByteSource(output).use { source ->
                        WavPcmDecoder(source).readInterleaved(samples, frameCount = READ_FRAMES)
                    }
                    assertTrue("${fixture.fileName} must yield PCM frames", frames > 0)
                    assertTrue(
                        "${fixture.fileName} decoded PCM must not be silent",
                        samples.take(frames * CHANNELS).sumOf { abs(it).toDouble() } > NON_SILENT_SUM,
                    )
                } finally {
                    prepared.close()
                }
                assertFalse("Prepared ${fixture.fileName} proxy must be disposable", output.exists())
            } finally {
                input.delete()
            }
        }
    }

    private data class Fixture(
        val fileName: String,
        val mimeType: String,
        val format: AudioImportFormat,
    )

    private companion object {
        const val SAMPLE_RATE_HZ = 48_000
        const val CHANNELS = 2
        const val READ_FRAMES = 512
        const val NON_SILENT_SUM = 1.0

        val fixtures = listOf(
            Fixture("tone.wav", "audio/wav", AudioImportFormat.WAV_PCM),
            Fixture("tone.flac", "audio/flac", AudioImportFormat.FLAC),
            Fixture("tone.aiff", "audio/aiff", AudioImportFormat.AIFF),
            Fixture("tone.mp3", "audio/mpeg", AudioImportFormat.MP3),
            Fixture("tone.m4a", "audio/mp4", AudioImportFormat.AAC_M4A),
            Fixture("tone.ogg", "audio/ogg", AudioImportFormat.OGG_VORBIS),
            Fixture("tone.opus", "audio/opus", AudioImportFormat.OPUS),
        )
    }
}
