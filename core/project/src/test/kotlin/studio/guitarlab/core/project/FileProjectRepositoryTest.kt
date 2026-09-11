package studio.guitarlab.core.project

import java.io.File
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

    @Test
    fun unsafeAndPreviouslyCollidingIdsStayConfinedAndIndependent() {
        val root = Files.createTempDirectory("guitarlab-storage-key").toFile()
        try {
            val repository = FileProjectRepository(root)
            val slash = ProjectFactory(idGenerator = { "a/b" }, clock = { 1L })
                .create("Slash", ProjectTemplate.BLANK)
            val question = ProjectFactory(idGenerator = { "a?b" }, clock = { 2L })
                .create("Question", ProjectTemplate.BLANK)
            val parent = ProjectFactory(idGenerator = { ".." }, clock = { 3L })
                .create("Parent", ProjectTemplate.BLANK)

            repository.save(slash)
            repository.save(question)
            repository.save(parent)

            assertEquals("Slash", repository.load("a/b")?.name)
            assertEquals("Question", repository.load("a?b")?.name)
            assertEquals("Parent", repository.load("..")?.name)
            assertEquals(3, repository.list().size)
            assertTrue(repository.delete(".."))
            assertTrue(root.isDirectory)
            assertTrue(File(root, "projects").isDirectory)
            assertEquals(2, repository.list().size)
        } finally { root.deleteRecursively() }
    }

    @Test
    fun blankProjectIdIsRejectedWithoutWritingAtProjectsRoot() {
        val root = Files.createTempDirectory("guitarlab-blank-id").toFile()
        try {
            val repository = FileProjectRepository(root)
            val project = ProjectFactory(idGenerator = { "" }, clock = { 1L })
                .create("Invalid", ProjectTemplate.BLANK)

            assertFailsWith<IllegalArgumentException> { repository.save(project) }
            assertTrue(!File(root, "projects/project.json").exists())
        } finally { root.deleteRecursively() }
    }
}
