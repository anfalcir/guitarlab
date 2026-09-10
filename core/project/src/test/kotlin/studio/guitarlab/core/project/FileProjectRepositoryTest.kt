package studio.guitarlab.core.project

import studio.guitarlab.core.model.ProjectFactory
import studio.guitarlab.core.model.ProjectTemplate
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.test.assertContentEquals
import kotlin.test.assertFailsWith
import studio.guitarlab.core.model.AudioClip
import studio.guitarlab.core.model.AudioTrack

class FileProjectRepositoryTest {
    @Test
    fun saveLoadListAndDeleteRoundTrip() {
        val root = Files.createTempDirectory("guitarlab-test").toFile()
        try {
            val repository = FileProjectRepository(root)
            var nextId = 0
            val project = ProjectFactory(idGenerator = { "id-${nextId++}" }, clock = { 100L })
                .create("Tone Test", ProjectTemplate.GUITAR)

            repository.save(project)
            assertEquals(1, repository.list().size)
            assertNotNull(repository.load(project.id))
            assertTrue(repository.delete(project.id))
            assertTrue(repository.list().isEmpty())
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun duplicateCopiesManagedSourceAndProxyAndRollsBackOnMissingMedia() {
        val root = Files.createTempDirectory("guitarlab-duplicate").toFile()
        try {
            val repository = FileProjectRepository(root)
            val sourceBytes = byteArrayOf(1, 2, 3, 4)
            val proxyBytes = byteArrayOf(5, 6, 7)
            val project = ProjectFactory(idGenerator = { "source" }, clock = { 100L })
                .create("Original", ProjectTemplate.BLANK)
                .copy(
                    tracks = listOf(AudioTrack("t1", "Track", order = 0)),
                    clips = listOf(AudioClip(
                        id = "c1", trackId = "t1", name = "Take", sourceUri = "managed://media/source/take.wav",
                        startFrame = 0, lengthFrames = 10, sourceTotalFrames = 10,
                        managedSourcePath = "media/source/take.wav", managedEditProxyPath = "media/proxy/take.wav",
                    )),
                )
            repository.save(project)
            val sourceDir = File(root, "projects/source")
            File(sourceDir, "media/source").mkdirs()
            File(sourceDir, "media/proxy").mkdirs()
            File(sourceDir, "media/source/take.wav").writeBytes(sourceBytes)
            File(sourceDir, "media/proxy/take.wav").writeBytes(proxyBytes)

            val duplicate = repository.duplicate("source", "Copy", "copy", 200L)
            assertEquals("Copy", duplicate.name)
            assertContentEquals(sourceBytes, File(root, "projects/copy/media/source/take.wav").readBytes())
            assertContentEquals(proxyBytes, File(root, "projects/copy/media/proxy/take.wav").readBytes())

            File(sourceDir, "media/proxy/take.wav").delete()
            assertFailsWith<IllegalArgumentException> { repository.duplicate("source", "Broken", "broken", 300L) }
            assertTrue(!File(root, "projects/broken").exists())
        } finally {
            root.deleteRecursively()
        }
    }
}
