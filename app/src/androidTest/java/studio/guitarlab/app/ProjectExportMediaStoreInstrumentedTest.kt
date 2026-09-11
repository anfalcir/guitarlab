package studio.guitarlab.app

import android.content.ContentValues
import android.provider.MediaStore
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.util.zip.ZipInputStream
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import studio.guitarlab.app.ui.ProjectExportService
import studio.guitarlab.core.model.ProjectFactory
import studio.guitarlab.core.model.ProjectTemplate
import studio.guitarlab.core.project.FileProjectRepository
import studio.guitarlab.core.project.ProjectBundleReader

@RunWith(AndroidJUnit4::class)
class ProjectExportMediaStoreInstrumentedTest {
    @Test
    fun projectExportTruncatesExistingMediaStoreDocumentAndRoundTrips() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val resolver = context.contentResolver
        val repository = FileProjectRepository(context.filesDir)
        val project = ProjectFactory().create("MediaStore-${System.nanoTime()}", ProjectTemplate.BLANK)
        repository.save(project)
        var restoredId: String? = null
        val uri = resolver.insert(
            MediaStore.Downloads.EXTERNAL_CONTENT_URI,
            ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, "guitarlab-test-${System.nanoTime()}.guitarlab")
                put(MediaStore.MediaColumns.MIME_TYPE, "application/zip")
                put(MediaStore.MediaColumns.RELATIVE_PATH, "Download/GuitarLabTests")
            },
        )
        assertNotNull("MediaStore.Downloads must create a writable test destination", uri)

        try {
            resolver.openOutputStream(uri!!, "w")!!.use { output ->
                output.write(ByteArray(1024 * 1024) { 0x5A.toByte() })
            }

            ProjectExportService(context).saveProject(project.id, uri)

            val bytes = resolver.openInputStream(uri)!!.use { it.readBytes() }
            assertTrue("Exported package must replace the 1 MiB prefill", bytes.size < 1024 * 1024)
            assertTrue("Exported package must contain a complete EOCD record at EOF", hasEocdExactlyAtEof(bytes))
            val entries = ZipInputStream(bytes.inputStream()).use { zip ->
                buildSet {
                    var entry = zip.nextEntry
                    while (entry != null) {
                        add(entry.name)
                        zip.closeEntry()
                        entry = zip.nextEntry
                    }
                }
            }
            assertTrue("Portable package must contain project.json", "project.json" in entries)
            assertTrue("Portable package must contain its manifest", "manifest.properties" in entries)

            val restored = resolver.openInputStream(uri)!!.use { ProjectBundleReader(context.filesDir).read(it) }
            restoredId = restored.id
            assertEquals(project.name, restored.name)
            assertEquals(project.template, restored.template)
            assertEquals(project.sampleRate, restored.sampleRate)
            assertEquals(project.groups, restored.groups)
            assertEquals(project.tracks, restored.tracks)
            assertEquals(project.clips, restored.clips)
        } finally {
            resolver.delete(uri!!, null, null)
            restoredId?.let(repository::delete)
            repository.delete(project.id)
        }
    }

    private fun hasEocdExactlyAtEof(bytes: ByteArray): Boolean {
        if (bytes.size < EOCD_MIN_BYTES) return false
        val offset = bytes.size - EOCD_MIN_BYTES
        return bytes[offset] == 0x50.toByte() &&
            bytes[offset + 1] == 0x4B.toByte() &&
            bytes[offset + 2] == 0x05.toByte() &&
            bytes[offset + 3] == 0x06.toByte() &&
            bytes[offset + 20] == 0.toByte() &&
            bytes[offset + 21] == 0.toByte()
    }

    private companion object {
        const val EOCD_MIN_BYTES = 22
    }
}
