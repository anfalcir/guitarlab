package studio.guitarlab.core.project

import java.io.ByteArrayInputStream
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class ProjectManagedMediaStoreTest {
    @Test
    fun ingestCopiesIntoProjectControlledSourceDirectory() {
        val root = Files.createTempDirectory("guitarlab-media-test").toFile()
        try {
            val store = ProjectManagedMediaStore(root) { "asset-1" }
            val bytes = byteArrayOf(1, 2, 3, 4)
            val asset = store.ingest("project-1", "Take 1.wav", ByteArrayInputStream(bytes))

            assertEquals("media/source/asset-1-Take 1.wav", asset.relativePath)
            assertEquals(4L, asset.byteCount)
            assertContentEquals(bytes, store.resolve("project-1", asset.relativePath).readBytes())
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun ingestRejectsEmptyInputAndPathTraversal() {
        val root = Files.createTempDirectory("guitarlab-media-test").toFile()
        try {
            val store = ProjectManagedMediaStore(root) { "asset-2" }
            assertFailsWith<IllegalArgumentException> {
                store.ingest("project-1", "empty.wav", ByteArrayInputStream(byteArrayOf()))
            }
            assertFailsWith<IllegalArgumentException> {
                store.resolve("project-1", "media/source/../project.json")
            }
            assertTrue(File(root, "projects/project-1/media/source").listFiles().orEmpty().none { it.isFile })
        } finally {
            root.deleteRecursively()
        }
    }
}
