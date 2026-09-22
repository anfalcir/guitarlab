package studio.guitarlab.app

import android.content.ContentValues
import android.media.MediaExtractor
import android.media.MediaFormat
import android.provider.MediaStore
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.security.MessageDigest
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import studio.guitarlab.app.ui.ProjectExportService
import studio.guitarlab.app.ui.StudyExportKind
import studio.guitarlab.app.ui.StudyExportStrategy
import studio.guitarlab.core.codec.FloatWavFileWriter
import studio.guitarlab.core.model.AssetClassification
import studio.guitarlab.core.model.AssetLifecycle
import studio.guitarlab.core.model.AssetRole
import studio.guitarlab.core.model.GuitarProject
import studio.guitarlab.core.model.ManagedAsset
import studio.guitarlab.core.model.PreparationState
import studio.guitarlab.core.model.PreparationStatus
import studio.guitarlab.core.model.ProjectTemplate
import studio.guitarlab.core.project.FileProjectRepository
import studio.guitarlab.core.project.ProjectManagedMediaStore
import studio.guitarlab.platform.codec.android.AndroidMasterAudioEncoder
import studio.guitarlab.platform.codec.android.MasterExportFormat

@RunWith(AndroidJUnit4::class)
class StudyExportInstrumentedTest {
    @Test fun backingWavIsByteExactDirectPublicationWithoutProjectMutation() = runBlocking {
        val fixture = fixture()
        val before = fixture.repository.load(fixture.project.id)!!
        val uri = destination("guitarlab-u6-${System.nanoTime()}.wav", "audio/wav")
        try {
            val result = fixture.service.exportStudyReference(fixture.project.id, StudyExportKind.BACKING, uri, MasterExportFormat.WAV_FLOAT32)
            val exported = fixture.context.contentResolver.openInputStream(uri)!!.use { it.readBytes() }
            assertTrue(fixture.backing.readBytes().contentEquals(exported))
            assertEquals(StudyExportStrategy.DIRECT_CANONICAL_WAV, result.strategy)
            assertEquals(sha(fixture.backing), result.sourceSha256)
            assertEquals(before, fixture.repository.load(fixture.project.id))
        } finally {
            fixture.context.contentResolver.delete(uri, null, null)
            fixture.repository.delete(fixture.project.id)
        }
    }

    @Test fun explicitFlacIsSingleEncodeFromCanonicalReferenceAndDoesNotRebindStudio() = runBlocking {
        val fixture = fixture()
        assumeTrue(AndroidMasterAudioEncoder.isSupported(MasterExportFormat.FLAC, RATE, 2))
        val before = fixture.repository.load(fixture.project.id)!!
        val uri = destination("guitarlab-u6-${System.nanoTime()}.flac", "audio/flac")
        try {
            val result = fixture.service.exportStudyReference(fixture.project.id, StudyExportKind.GUITAR, uri, MasterExportFormat.FLAC)
            val staged = File(fixture.context.cacheDir, "u6-check-${System.nanoTime()}.flac")
            fixture.context.contentResolver.openInputStream(uri)!!.use { input -> staged.outputStream().use { input.copyTo(it) } }
            try {
                assertTrue(staged.length() > 0)
                assertEquals(StudyExportStrategy.SINGLE_ENCODE, result.strategy)
                val extractor = MediaExtractor()
                try {
                    extractor.setDataSource(staged.absolutePath)
                    assertTrue(extractor.trackCount > 0)
                    val format = extractor.getTrackFormat(0)
                    assertEquals(RATE, format.getInteger(MediaFormat.KEY_SAMPLE_RATE))
                    assertEquals(2, format.getInteger(MediaFormat.KEY_CHANNEL_COUNT))
                } finally { extractor.release() }
                assertEquals(before, fixture.repository.load(fixture.project.id))
            } finally { staged.delete() }
        } finally {
            fixture.context.contentResolver.delete(uri, null, null)
            fixture.repository.delete(fixture.project.id)
        }
    }

    private data class Fixture(
        val context: android.content.Context, val repository: FileProjectRepository, val service: ProjectExportService,
        val project: GuitarProject, val backing: File, val guitar: File,
    )

    private fun fixture(): Fixture {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val repository = FileProjectRepository(context.filesDir)
        val media = ProjectManagedMediaStore(context.filesDir)
        val projectId = "u6-${System.nanoTime()}"
        val backingSource = writeWav(File(context.cacheDir, "$projectId-backing.wav"), 0.20f)
        val guitarSource = writeWav(File(context.cacheDir, "$projectId-guitar.wav"), 0.10f)
        val backingManaged = backingSource.inputStream().use { media.ingestReference(projectId, "backing.wav", it) }
        val guitarManaged = guitarSource.inputStream().use { media.ingestReference(projectId, "guitar.wav", it) }
        backingSource.delete(); guitarSource.delete()
        val backing = asset("backing", AssetRole.REFERENCE_BACKING, backingManaged.file)
        val guitar = asset("guitar", AssetRole.REFERENCE_GUITAR, guitarManaged.file)
        val project = GuitarProject(
            id = projectId, name = "Unsafe / Rename: Música?", template = ProjectTemplate.GUITAR, createdAtEpochMs = 1, updatedAtEpochMs = 2,
            assets = listOf(backing, guitar),
            preparation = PreparationState(status = PreparationStatus.READY, activeBackingAssetId = backing.assetId, activeGuitarAssetId = guitar.assetId, availableReferenceAssetIds = listOf(backing.assetId, guitar.assetId)),
        )
        repository.save(project)
        return Fixture(context, repository, ProjectExportService(context), project, backingManaged.file, guitarManaged.file)
    }

    private fun asset(id: String, role: AssetRole, file: File) = ManagedAsset(
        assetId = id, role = role, relativePath = "media/references/${file.name}", sha256 = sha(file), byteSize = file.length(), format = "wav",
        sampleRateHz = RATE, channelCount = 2, frameCount = FRAMES.toLong(), createdAtEpochMs = 1, classification = AssetClassification.DERIVED, lifecycle = AssetLifecycle.MANAGED,
    )

    private fun writeWav(file: File, level: Float): File {
        val data = FloatArray(FRAMES * 2) { index -> if (index % 2 == 0) level else -level }
        FloatWavFileWriter(file, RATE, 2).use { it.writeInterleaved(data, FRAMES) }
        return file
    }

    private fun destination(name: String, mime: String) = InstrumentationRegistry.getInstrumentation().targetContext.contentResolver.insert(
        MediaStore.Downloads.EXTERNAL_CONTENT_URI,
        ContentValues().apply { put(MediaStore.MediaColumns.DISPLAY_NAME, name); put(MediaStore.MediaColumns.MIME_TYPE, mime); put(MediaStore.MediaColumns.RELATIVE_PATH, "Download/GuitarLabTests") },
    ) ?: error("MediaStore destination unavailable")

    private fun sha(file: File): String = MessageDigest.getInstance("SHA-256").digest(file.readBytes()).joinToString("") { "%02x".format(it) }

    private companion object { const val RATE = 48_000; const val FRAMES = 4_800 }
}
