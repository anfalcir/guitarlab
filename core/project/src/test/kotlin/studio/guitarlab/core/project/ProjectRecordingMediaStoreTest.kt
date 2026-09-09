package studio.guitarlab.core.project

import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ProjectRecordingMediaStoreTest {
    @Test
    fun commitMovesTemporaryTakeIntoImmutableSourceArea() {
        val root = createTempDirectory("guitarlab-recording-store").toFile()
        try {
            val store = ProjectRecordingMediaStore(root, idFactory = { "take-1" })
            val tx = store.begin("project-1", "Take 1.wav")
            tx.temporaryFile.parentFile?.mkdirs()
            tx.temporaryFile.writeBytes(ByteArray(48) { it.toByte() })

            val committed = store.commit(tx)

            assertFalse(tx.temporaryFile.exists())
            assertTrue(committed.isFile)
            assertEquals("media/source/take-1-Take 1.wav", tx.relativePath)
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun cleanupInterruptedRemovesOnlyRecordingPartFiles() {
        val root = createTempDirectory("guitarlab-recording-cleanup").toFile()
        try {
            val store = ProjectRecordingMediaStore(root, idFactory = { "take-2" })
            val tx = store.begin("project-2")
            tx.temporaryFile.parentFile?.mkdirs()
            tx.temporaryFile.writeBytes(ByteArray(60))
            val unrelated = File(tx.temporaryFile.parentFile, "keep.txt").apply { writeText("keep") }

            assertEquals(1, store.cleanupInterrupted("project-2"))
            assertFalse(tx.temporaryFile.exists())
            assertTrue(unrelated.exists())
        } finally {
            root.deleteRecursively()
        }
    }
}
