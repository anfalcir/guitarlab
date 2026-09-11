package studio.guitarlab.core.project

import java.io.ByteArrayOutputStream
import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.system.measureNanoTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import studio.guitarlab.core.model.AudioClip
import studio.guitarlab.core.model.AudioTrack
import studio.guitarlab.core.model.GuitarProject
import studio.guitarlab.core.model.ProjectTemplate
import studio.guitarlab.core.model.SampleRateConfig
import studio.guitarlab.core.model.SampleRateMode

/** Reproducible JVM/CI evidence only; these timings are not claims about tablet realtime performance. */
class ProjectPerformanceEvidenceTest {
    @Test
    fun smallMediumLargePersistenceAndBundleMetricsRemainStructurallyStable() {
        listOf(
            Triple("Small", 5, 10),
            Triple("Medium", 12, 50),
            Triple("Large", 24, 120),
        ).forEach { (label, trackCount, clipCount) ->
            val root = createTempDirectory("guitarlab-perf-root-").toFile()
            val mediaRoot = createTempDirectory("guitarlab-perf-media-").toFile()
            try {
                val project = project(label.lowercase(), trackCount, clipCount)
                val repository = FileProjectRepository(root)
                val beforeUsed = usedHeapBytes()
                lateinit var loaded: GuitarProject
                val saveLoadNs = measureNanoTime {
                    repository.save(project)
                    loaded = requireNotNull(repository.load(project.id))
                }
                assertEquals(project, loaded)

                val packageBytes = ByteArrayOutputStream()
                val bundleNs = measureNanoTime {
                    ProjectBundleWriter().write(project, mediaRoot, packageBytes)
                }
                lateinit var restored: GuitarProject
                val reopenNs = measureNanoTime {
                    restored = ProjectBundleReader(root).read(packageBytes.toByteArray().inputStream(), nowEpochMs = 99L)
                }
                assertEquals(trackCount, restored.tracks.size)
                assertEquals(clipCount, restored.clips.size)
                val heapDelta = (usedHeapBytes() - beforeUsed).coerceAtLeast(0L)
                val jsonBytes = ProjectCodec().encode(project).toByteArray().size

                println(
                    "GUITARLAB_PERF|size=$label|tracks=$trackCount|clips=$clipCount" +
                        "|save_load_ms=${saveLoadNs / 1_000_000.0}" +
                        "|bundle_ms=${bundleNs / 1_000_000.0}" +
                        "|reopen_ms=${reopenNs / 1_000_000.0}" +
                        "|json_bytes=$jsonBytes|bundle_bytes=${packageBytes.size()}|heap_delta_bytes=$heapDelta"
                )
                assertTrue(packageBytes.size() > 0)
            } finally {
                root.deleteRecursively()
                mediaRoot.deleteRecursively()
            }
        }
    }

    private fun project(id: String, trackCount: Int, clipCount: Int): GuitarProject {
        val tracks = List(trackCount) { index -> AudioTrack(id = "t$index", name = "Track $index", order = index) }
        val clips = List(clipCount) { index ->
            AudioClip(
                id = "c$index",
                trackId = "t${index % trackCount}",
                name = "Clip $index",
                sourceUri = "external://fixture-$index.wav",
                startFrame = index * 2_000L,
                sourceStartFrame = index % 100L,
                lengthFrames = 48_000L,
                sourceTotalFrames = 96_000L,
                sourceSampleRateHz = 48_000,
                sourceChannelCount = if (index % 2 == 0) 1 else 2,
                editingSampleRateHz = 48_000,
                editingTotalFrames = 96_000L,
            )
        }
        return GuitarProject(
            id = id,
            name = id,
            template = ProjectTemplate.BLANK,
            createdAtEpochMs = 1L,
            updatedAtEpochMs = 1L,
            sampleRate = SampleRateConfig(SampleRateMode.FIXED, 48_000),
            tracks = tracks,
            clips = clips,
        )
    }

    private fun usedHeapBytes(): Long {
        val runtime = Runtime.getRuntime()
        return runtime.totalMemory() - runtime.freeMemory()
    }
}
