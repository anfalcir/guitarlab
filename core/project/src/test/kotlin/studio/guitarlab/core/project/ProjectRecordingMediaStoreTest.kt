package studio.guitarlab.core.project

import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import studio.guitarlab.core.codec.FileSeekableByteSource
import studio.guitarlab.core.codec.FloatWavFileWriter
import studio.guitarlab.core.codec.WavMetadataReader

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
    fun cleanupInterruptedRemovesOnlyFilesWithoutAudioPayload() {
        val root = createTempDirectory("guitarlab-recording-cleanup").toFile()
        try {
            var nextId = 0
            val store = ProjectRecordingMediaStore(root, idFactory = { "take-${++nextId}" })
            val empty = store.begin("project-2")
            empty.temporaryFile.parentFile?.mkdirs()
            empty.temporaryFile.writeBytes(ByteArray(44))
            val recoverable = store.begin("project-2")
            recoverable.temporaryFile.writeBytes(ByteArray(60))
            val unrelated = File(empty.temporaryFile.parentFile, "keep.txt").apply { writeText("keep") }

            assertEquals(1, store.cleanupInterrupted("project-2"))
            assertFalse(empty.temporaryFile.exists())
            assertTrue(recoverable.temporaryFile.exists())
            assertEquals(listOf(recoverable.temporaryFile.canonicalFile), store.recoverableInterrupted("project-2").map { it.canonicalFile })
            assertTrue(unrelated.exists())
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun repairInterruptedMakesProcessKilledFloatTakeReadable() {
        val root = createTempDirectory("guitarlab-recording-repair").toFile()
        val scratch = File.createTempFile("guitarlab-live-recording-", ".wav")
        try {
            val store = ProjectRecordingMediaStore(root, idFactory = { "take-repair" })
            val tx = store.begin("project-repair")
            tx.temporaryFile.parentFile?.mkdirs()
            val writer = FloatWavFileWriter(scratch, sampleRateHz = 48_000, channelCount = 1)
            try {
                writer.writeInterleaved(floatArrayOf(0.25f, -0.25f), frameCount = 2)
                tx.temporaryFile.writeBytes(scratch.readBytes())
            } finally {
                writer.close()
            }

            val before = FileSeekableByteSource(tx.temporaryFile).use { WavMetadataReader().read(it) }
            assertEquals(0L, before.totalFrames)

            assertEquals(1, store.repairInterrupted("project-repair"))

            val after = FileSeekableByteSource(tx.temporaryFile).use { WavMetadataReader().read(it) }
            assertEquals(2L, after.totalFrames)
            assertEquals(48_000, after.sampleRateHz)
            assertTrue(tx.temporaryFile.isFile)
        } finally {
            scratch.delete()
            root.deleteRecursively()
        }
    }

    @Test
    fun abandonPreservesPartialPayloadInsteadOfSilentlyDeletingIt() {
        val root = createTempDirectory("guitarlab-recording-abandon-partial").toFile()
        try {
            val store = ProjectRecordingMediaStore(root, idFactory = { "take-partial" })
            val tx = store.begin("project-partial")
            tx.temporaryFile.parentFile?.mkdirs()
            tx.temporaryFile.writeBytes(ByteArray(60) { 1 })

            assertEquals(RecordingAbandonResult.RECOVERABLE_PARTIAL_PRESERVED, store.abandon(tx))
            assertTrue(tx.temporaryFile.isFile)
            assertFalse(tx.finalFile.exists())
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun abandonPreservesFinalizedButUnpublishedTake() {
        val root = createTempDirectory("guitarlab-recording-abandon-final").toFile()
        try {
            val store = ProjectRecordingMediaStore(root, idFactory = { "take-final" })
            val tx = store.begin("project-final")
            tx.temporaryFile.parentFile?.mkdirs()
            tx.temporaryFile.writeBytes(ByteArray(60) { 2 })
            store.commit(tx)

            assertEquals(RecordingAbandonResult.FINALIZED_UNPUBLISHED_PRESERVED, store.abandon(tx))
            assertFalse(tx.temporaryFile.exists())
            assertTrue(tx.finalFile.isFile)
        } finally {
            root.deleteRecursively()
        }
    }
}
