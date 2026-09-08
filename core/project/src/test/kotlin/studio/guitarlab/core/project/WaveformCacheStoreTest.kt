package studio.guitarlab.core.project

import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
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
}
