package studio.guitarlab.core.project

import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RecordingMediaLosslessDiscardTest {
    @Test
    fun legacyDiscardCallCannotDeleteCapturedAudioPayload() {
        val root = createTempDirectory("guitarlab-recording-discard-").toFile()
        try {
            val store = ProjectRecordingMediaStore(root, idFactory = { "take" })
            val transaction = store.begin("project")
            transaction.temporaryFile.parentFile?.mkdirs()
            transaction.temporaryFile.writeBytes(ByteArray(60) { 7 })

            store.discard(transaction)

            assertTrue(transaction.temporaryFile.isFile)
            assertFalse(transaction.finalFile.exists())
        } finally {
            root.deleteRecursively()
        }
    }
}
