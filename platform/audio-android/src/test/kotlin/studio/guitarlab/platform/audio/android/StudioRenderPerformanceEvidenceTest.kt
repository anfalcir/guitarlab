package studio.guitarlab.platform.audio.android

import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.system.measureNanoTime
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.Test
import studio.guitarlab.core.codec.FileSeekableByteSource
import studio.guitarlab.core.codec.FloatWavFileWriter
import studio.guitarlab.core.codec.WavPcmDecoder

/** JVM/CI render metrics only; not a claim about realtime Samsung/Pocket Amp performance. */
class StudioRenderPerformanceEvidenceTest {
    @Test
    fun smallMediumLargeOneSecondRenderMetricsAreReproducibleAndValid() {
        val dir = createTempDirectory("guitarlab-render-perf-").toFile()
        try {
            val source = File(dir, "source.wav")
            val frames = 48_000
            val mono = FloatArray(frames) { index -> ((index % 101) - 50) / 100f }
            FloatWavFileWriter(source, 48_000, 1).use { it.writeInterleaved(mono, frames) }

            listOf(
                Triple("Small", 5, 10),
                Triple("Medium", 12, 50),
                Triple("Large", 24, 120),
            ).forEach { (label, trackCount, clipCount) ->
                val output = File(dir, "${label.lowercase()}-master.wav")
                val request = StudioMasterRenderRequest(
                    sampleRateHz = 48_000,
                    projectEndFrame = frames.toLong(),
                    clips = List(clipCount) { index ->
                        StudioMasterRenderClip(
                            file = source,
                            trackId = "t${index % trackCount}",
                            timelineStartFrame = 0,
                            sourceStartFrame = 0,
                            lengthFrames = frames.toLong(),
                            gainDb = -24f,
                            fadeInFrames = 64,
                            fadeOutFrames = 64,
                        )
                    },
                    trackMixes = List(trackCount) { index ->
                        StudioMasterRenderTrack(
                            trackId = "t$index",
                            gainDb = -6f,
                            pan = when (index % 3) { 0 -> -0.5f; 1 -> 0f; else -> 0.5f },
                        )
                    },
                    masterGainDb = -12f,
                )

                val beforeUsed = usedHeapBytes()
                val renderNs = measureNanoTime { StudioMasterRenderer.renderFloatWav(request, output) }
                val heapDelta = (usedHeapBytes() - beforeUsed).coerceAtLeast(0L)

                FileSeekableByteSource(output).use { byteSource ->
                    WavPcmDecoder(byteSource).use { decoder ->
                        assertEquals(48_000, decoder.metadata.sampleRateHz)
                        assertEquals(2, decoder.metadata.channelCount)
                        assertEquals(frames.toLong(), decoder.metadata.totalFrames)
                        val probe = FloatArray(2_048)
                        assertTrue(decoder.readInterleaved(probe, frameCount = 1_024) > 0)
                        assertTrue(probe.all(Float::isFinite))
                    }
                }

                println(
                    "GUITARLAB_RENDER_PERF|size=$label|tracks=$trackCount|clips=$clipCount" +
                        "|frames=$frames|render_ms=${renderNs / 1_000_000.0}" +
                        "|output_bytes=${output.length()}|heap_delta_bytes=$heapDelta"
                )
            }
        } finally {
            dir.deleteRecursively()
        }
    }

    private fun usedHeapBytes(): Long {
        val runtime = Runtime.getRuntime()
        return runtime.totalMemory() - runtime.freeMemory()
    }
}
