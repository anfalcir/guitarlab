package studio.guitarlab.core.project

import java.io.File
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import studio.guitarlab.core.codec.WaveformEnvelope

class WaveformCacheStoreTest {
    @Test
    fun cacheRoundTripsAndCanBeRegeneratedIndependently() {
        val root = Files.createTempDirectory("guitarlab-waveform-cache").toFile()
        try {
            val store = WaveformCacheStore(root)
            val envelope = WaveformEnvelope(listOf(0f, 0.25f, 1f, 0.5f))
            store.write("project-1", "clip-1", envelope)
            assertEquals(envelope, store.read("project-1", "clip-1"))
            store.remove("project-1", "clip-1")
            assertNull(store.read("project-1", "clip-1"))
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun pruneRemovesOnlyUnreferencedRegenerableCaches() {
        val root = Files.createTempDirectory("guitarlab-waveform-prune").toFile()
        try {
            val store = WaveformCacheStore(root)
            val envelope = WaveformEnvelope(listOf(0.1f, 0.9f))
            store.write("project", "keep", envelope)
            store.write("project", "orphan", envelope)
            val unrelated = File(root, "projects/project/media/derived/waveform/notes.txt").apply { writeText("keep") }

            assertEquals(1, store.prune("project", setOf("keep")))
            assertEquals(envelope, store.read("project", "keep"))
            assertNull(store.read("project", "orphan"))
            assertTrue(unrelated.isFile)
        } finally { root.deleteRecursively() }
    }
}
