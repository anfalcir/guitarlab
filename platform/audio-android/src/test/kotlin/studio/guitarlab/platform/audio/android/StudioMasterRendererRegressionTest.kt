package studio.guitarlab.platform.audio.android

import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.math.abs
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import studio.guitarlab.core.codec.FileSeekableByteSource
import studio.guitarlab.core.codec.FloatWavFileWriter
import studio.guitarlab.core.codec.WavPcmDecoder

class StudioMasterRendererRegressionTest {
    @Test fun rendersKnownMonoPcmWithGainPanFadeTimelineAndMasterClipping() {
        val dir = createTempDirectory("guitarlab-master-").toFile()
        try {
            val source = File(dir, "source.wav")
            val output = File(dir, "master.wav")
            FloatWavFileWriter(source, 48_000, 1).use { it.writeInterleaved(FloatArray(8) { 0.5f }, 8) }
            StudioMasterRenderer.renderFloatWav(
                StudioMasterRenderRequest(
                    sampleRateHz = 48_000, projectEndFrame = 12,
                    clips = listOf(StudioMasterRenderClip(source, "t", 2, 0, 8, gainDb = 0f, fadeInFrames = 2, fadeOutFrames = 2)),
                    trackMixes = listOf(StudioMasterRenderTrack("t", gainDb = 6.0206f, pan = -1f)),
                    masterGainDb = 6.0206f,
                ), output,
            )
            FileSeekableByteSource(output).use { byteSource ->
                WavPcmDecoder(byteSource).use { decoder ->
                    assertEquals(12, decoder.metadata.totalFrames)
                    assertEquals(2, decoder.metadata.channelCount)
                    val pcm = FloatArray(24)
                    assertEquals(12, decoder.readInterleaved(pcm, frameCount = 12))
                    repeat(12) { frame -> assertTrue(abs(pcm[frame * 2 + 1]) < 0.00001f) }
                    assertTrue(abs(pcm[0]) < 0.00001f && abs(pcm[2]) < 0.00001f)
                    assertTrue(abs(pcm[4]) < 0.00001f)
                    assertTrue(pcm[6] in 0.99f..1.0f)
                    assertTrue(pcm[16] in 0.99f..1.0f)
                    assertTrue(abs(pcm[18]) < 0.00001f)
                    assertTrue(abs(pcm[20]) < 0.00001f && abs(pcm[22]) < 0.00001f)
                }
            }
        } finally { dir.deleteRecursively() }
    }

    @Test fun overlappingClipsCrossfadeAndStereoChannelsStayIndependent() {
        val dir = createTempDirectory("guitarlab-crossfade-").toFile()
        try {
            val left = File(dir, "left.wav")
            val right = File(dir, "right.wav")
            val output = File(dir, "out.wav")
            FloatWavFileWriter(left, 48_000, 2).use { it.writeInterleaved(FloatArray(16) { index -> if (index % 2 == 0) 1f else 0f }, 8) }
            FloatWavFileWriter(right, 48_000, 2).use { it.writeInterleaved(FloatArray(16) { index -> if (index % 2 == 0) 0f else 1f }, 8) }
            StudioMasterRenderer.renderFloatWav(
                StudioMasterRenderRequest(
                    48_000, 12,
                    listOf(
                        StudioMasterRenderClip(left, "t", 0, 0, 8, fadeOutFrames = 4),
                        StudioMasterRenderClip(right, "t", 4, 0, 8, fadeInFrames = 4),
                    ),
                    listOf(StudioMasterRenderTrack("t")),
                ), output,
            )
            FileSeekableByteSource(output).use { byteSource -> WavPcmDecoder(byteSource).use { decoder ->
                val pcm = FloatArray(24)
                decoder.readInterleaved(pcm, frameCount = 12)
                for (frame in 4..7) {
                    val l = pcm[frame * 2]
                    val r = pcm[frame * 2 + 1]
                    assertTrue(l in 0f..1f && r in 0f..1f)
                    assertTrue(abs((l + r) - 0.75f) < 0.0001f, "frame=$frame left=$l right=$r")
                }
            } }
        } finally { dir.deleteRecursively() }
    }
}
