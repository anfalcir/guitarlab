package studio.guitarlab.core.project

import java.io.File
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import studio.guitarlab.core.codec.WaveformEnvelope

class WaveformCacheStoreTest {
    @Test
    fun cacheRoundTripsOnlyForTheExactMediaWindowIdentity() {
        val root = Files.createTempDirectory("guitarlab-waveform-cache").toFile()
        try {
            val store = WaveformCacheStore(root)
            val envelope = WaveformEnvelope(listOf(0f, 0.25f, 1f, 0.5f))
            val identity = identity("media/a.wav", start = 0, length = 48_000)
            store.write("project-1", "clip-1", identity, envelope)

            assertEquals(envelope, store.read("project-1", "clip-1", identity))
            assertNull(store.read("project-1", "clip-1", identity.copy(mediaKey = "media/b.wav")))
            assertNull(store.read("project-1", "clip-1", identity.copy(sourceStartFrame = 1)))
            assertNull(store.read("project-1", "clip-1", identity.copy(lengthFrames = 24_000)))
            assertNull(store.read("project-1", "clip-1", identity.copy(targetPoints = 320)))

            store.remove("project-1", "clip-1")
            assertNull(store.read("project-1", "clip-1", identity))
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun cacheFingerprintChangesForMediaWindowAndAlgorithmInputs() {
        val base = identity("media/reference.wav", start = 0, length = 96_000)
        assertNotEquals(base.fingerprint, base.copy(mediaKey = "media/reference-v2.wav").fingerprint)
        assertNotEquals(base.fingerprint, base.copy(sourceStartFrame = 12_000).fingerprint)
        assertNotEquals(base.fingerprint, base.copy(lengthFrames = 48_000).fingerprint)
        assertNotEquals(base.fingerprint, base.copy(sampleRateHz = 44_100).fingerprint)
        assertNotEquals(base.fingerprint, base.copy(targetPoints = 320).fingerprint)
        assertNotEquals(base.fingerprint, base.copy(algorithmVersion = 99).fingerprint)
    }

    @Test
    fun pruneRemovesOnlyUnreferencedRegenerableCaches() {
        val root = Files.createTempDirectory("guitarlab-waveform-prune").toFile()
        try {
            val store = WaveformCacheStore(root)
            val envelope = WaveformEnvelope(listOf(0.1f, 0.9f))
            val keepIdentity = identity("media/keep.wav", 0, 100)
            val orphanIdentity = identity("media/orphan.wav", 0, 100)
            store.write("project", "keep", keepIdentity, envelope)
            store.write("project", "orphan", orphanIdentity, envelope)
            val unrelated = File(root, "projects/project/media/derived/waveform/notes.txt").apply { writeText("keep") }

            assertEquals(1, store.prune("project", setOf("keep")))
            assertEquals(envelope, store.read("project", "keep", keepIdentity))
            assertNull(store.read("project", "orphan", orphanIdentity))
            assertTrue(unrelated.isFile)
        } finally { root.deleteRecursively() }
    }

    @Test
    fun distinctUnsafeClipIdsNeverShareACacheFile() {
        val root = Files.createTempDirectory("guitarlab-waveform-key").toFile()
        try {
            val store = WaveformCacheStore(root)
            val first = WaveformEnvelope(listOf(0.1f))
            val second = WaveformEnvelope(listOf(0.9f))
            val firstIdentity = identity("media/first.wav", 0, 10)
            val secondIdentity = identity("media/second.wav", 0, 10)
            store.write("project", "a/b", firstIdentity, first)
            store.write("project", "a?b", secondIdentity, second)

            assertEquals(first, store.read("project", "a/b", firstIdentity))
            assertEquals(second, store.read("project", "a?b", secondIdentity))
        } finally { root.deleteRecursively() }
    }

    private fun identity(media: String, start: Long, length: Long) = WaveformCacheIdentity(
        mediaKey = media,
        sourceStartFrame = start,
        lengthFrames = length,
        sampleRateHz = 48_000,
        totalFrames = 192_000,
        channelCount = 2,
        targetPoints = 4096,
    )
}
