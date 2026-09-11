package studio.guitarlab.core.project

import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.io.OutputStream
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class StagedFilePublisherTest {
    @Test
    fun publishesCompleteStagedFileExactly() = runBlocking {
        val root = createTempDirectory("guitarlab-publish-").toFile()
        try {
            val source = File(root, "source.bin").apply { writeBytes(ByteArray(150_000) { (it % 251).toByte() }) }
            val target = ByteArrayOutputStream()
            var resets = 0

            StagedFilePublisher.publish(
                source = source,
                openDestination = { target },
                resetDestination = { resets++ },
                bufferSize = 4096,
            )

            assertTrue(source.readBytes().contentEquals(target.toByteArray()))
            assertEquals(0, resets)
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun outputFailureTriggersBestEffortResetAndPreservesOriginalError() = runBlocking {
        val root = createTempDirectory("guitarlab-publish-error-").toFile()
        try {
            val source = File(root, "source.bin").apply { writeBytes(ByteArray(100_000) { 7 }) }
            var resets = 0
            val failing = object : OutputStream() {
                private var written = 0
                override fun write(value: Int) = Unit
                override fun write(bytes: ByteArray, offset: Int, length: Int) {
                    written += length
                    if (written > 20_000) throw IOException("provider write failure")
                }
            }

            val error = assertFailsWith<IOException> {
                StagedFilePublisher.publish(source, { failing }, { resets++ }, bufferSize = 8192)
            }

            assertEquals("provider write failure", error.message)
            assertEquals(1, resets)
            assertTrue(source.isFile)
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun coroutineCancellationAfterDestinationOpenTriggersReset() = runBlocking {
        val root = createTempDirectory("guitarlab-publish-cancel-").toFile()
        try {
            val source = File(root, "source.bin").apply { writeBytes(ByteArray(200_000) { 3 }) }
            var resets = 0
            lateinit var publication: Job
            val target = object : OutputStream() {
                private var writes = 0
                override fun write(value: Int) = Unit
                override fun write(bytes: ByteArray, offset: Int, length: Int) {
                    writes++
                    if (writes == 1) publication.cancel()
                }
            }

            publication = launch(start = CoroutineStart.LAZY) {
                StagedFilePublisher.publish(source, { target }, { resets++ }, bufferSize = 4096)
            }
            publication.start()
            publication.join()

            assertTrue(publication.isCancelled)
            assertEquals(1, resets)
            assertTrue(source.isFile)
        } finally {
            root.deleteRecursively()
        }
    }
}
