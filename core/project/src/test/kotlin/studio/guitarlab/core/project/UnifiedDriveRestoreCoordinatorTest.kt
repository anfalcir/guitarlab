package studio.guitarlab.core.project

import java.io.File
import java.nio.file.Files
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlinx.coroutines.runBlocking
import org.junit.Test

class UnifiedDriveRestoreCoordinatorTest {
    @Test fun validatesEveryByteBeforeAtomicPublication() = runBlocking {
        val source = FakeSource(mapOf("media/take.wav" to "audio"))
        var published = false
        val result = coordinator(source) { staging, _ ->
            assertEquals("audio", staging.resolve("media/take.wav").readText())
            published = true
        }.restore(source.descriptor)
        assertEquals(1, result.downloadedAssets)
        assertEquals(5, result.downloadedBytes)
        assertEquals(true, published)
    }

    @Test fun corruptedDownloadNeverReachesPublisherAndStagingIsRemoved() = runBlocking {
        val root = Files.createTempDirectory("u8e-corrupt").toFile()
        val source = FakeSource(mapOf("media/take.wav" to "audio"), corrupt = true)
        var published = false
        assertFailsWith<IllegalArgumentException> {
            coordinator(source, root) { _, _ -> published = true }.restore(source.descriptor)
        }
        assertFalse(published)
        assertEquals(emptyList(), root.listFiles()?.toList().orEmpty())
        Unit
    }

    @Test fun validatorFailurePreservesExistingProject() = runBlocking {
        val source = FakeSource(mapOf("project.json" to "state"))
        var published = false
        val coordinator = coordinator(source, validator = DriveRestoreValidator { _, _ -> error("invalid project") }) {
            _, _ -> published = true
        }
        assertFailsWith<IllegalStateException> { coordinator.restore(source.descriptor) }
        assertFalse(published)
        Unit
    }

    @Test fun traversalTargetsAreRejected() {
        val asset = DriveAssetObject("a".repeat(64), 1)
        assertFailsWith<IllegalArgumentException> { DriveRestoreAssetTarget(asset, "../outside") }
        assertFailsWith<IllegalArgumentException> { DriveRestoreAssetTarget(asset, "..\\outside") }
        assertFailsWith<IllegalArgumentException> { DriveRestoreAssetTarget(asset, "C:\\outside") }
    }

    @Test fun conflictActionsAreExplicit() {
        assertEquals(DriveConflictAction.entries.toSet(), DriveConflictActionPolicy.allowed(DriveReconciliation.CONFLICT))
        assertEquals(setOf(DriveConflictAction.KEEP_LOCAL), DriveConflictActionPolicy.allowed(DriveReconciliation.UPLOAD_LOCAL))
        assertEquals(emptySet(), DriveConflictActionPolicy.allowed(DriveReconciliation.NO_OP))
    }

    private fun coordinator(
        source: FakeSource,
        root: File = Files.createTempDirectory("u8e-restore").toFile(),
        validator: DriveRestoreValidator = DriveRestoreValidator { _, _ -> },
        publish: (File, DriveRestorePlan) -> Unit,
    ) = UnifiedDriveRestoreCoordinator(
        source,
        DriveRestoreLayout { manifest -> source.files.map { (path, bytes) -> DriveRestoreAssetTarget(DriveAssetObject(BackupHashing.sha256(bytes), bytes.length()), path) } },
        validator,
        DriveRestorePublisher(publish),
        root,
    )

    private class FakeSource(
        contents: Map<String, String>,
        private val corrupt: Boolean = false,
    ) : UnifiedDriveRestoreSource {
        val files = contents.mapValues { (_, text) ->
            Files.createTempFile("u8e-source", ".bin").toFile().apply { writeText(text) }
        }
        private val manifest = DriveProjectRevisionManifest(
            "project", "revision", null, 1, "c".repeat(64),
            files.values.map { DriveAssetObject(BackupHashing.sha256(it), it.length()) },
        )
        val descriptor = DriveCurrentDescriptor("project", "revision", manifest.manifestSha256)
        override suspend fun loadManifest(descriptor: DriveCurrentDescriptor) = manifest
        override suspend fun downloadAsset(asset: DriveAssetObject, destination: File) {
            val source = files.values.single { BackupHashing.sha256(it) == asset.sha256 }
            destination.writeBytes(source.readBytes() + if (corrupt) byteArrayOf(0) else byteArrayOf())
        }
    }
}
