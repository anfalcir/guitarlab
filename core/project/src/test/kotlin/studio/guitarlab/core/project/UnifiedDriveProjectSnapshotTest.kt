package studio.guitarlab.core.project

import java.io.File
import java.nio.file.Files
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import org.junit.Test
import studio.guitarlab.core.model.AssetClassification
import studio.guitarlab.core.model.AssetLifecycle
import studio.guitarlab.core.model.AssetRole
import studio.guitarlab.core.model.ManagedAsset
import studio.guitarlab.core.model.ProjectFactory
import studio.guitarlab.core.model.ProjectTemplate

class UnifiedDriveProjectSnapshotTest {
    @Test fun metadataOnlyEditChangesStateObjectButReusesMediaObject() {
        val fixture = fixture()
        val builder = UnifiedDriveProjectSnapshotBuilder(fixture.root, nowEpochMs = { 10L })
        val first = builder.freeze(fixture.project, null)
        val edited = fixture.project.copy(masterGainDb = -3f, updatedAtEpochMs = 2L)
        val second = builder.freeze(edited, first.manifest.revisionId)
        try {
            assertTrue(first.manifest.isCompleteProjectSnapshot)
            assertTrue(second.manifest.isCompleteProjectSnapshot)
            assertNotEquals(first.manifest.projectStateAsset, second.manifest.projectStateAsset)
            assertEquals(
                first.manifest.fileEntries.map { it.asset },
                second.manifest.fileEntries.map { it.asset },
            )
            assertEquals(2, first.manifest.assets.size)
            assertEquals(2, second.manifest.assets.size)
        } finally {
            first.close()
            second.close()
            fixture.root.deleteRecursively()
        }
    }

    @Test fun duplicateMediaContentIsOneRemoteObjectWithTwoPaths() {
        val fixture = fixture(twoPathsSameBytes = true)
        val snapshot = UnifiedDriveProjectSnapshotBuilder(fixture.root, nowEpochMs = { 10L }).freeze(fixture.project, null)
        try {
            assertEquals(2, snapshot.manifest.fileEntries.size)
            assertEquals(1, snapshot.manifest.fileEntries.map { it.asset.sha256 }.distinct().size)
            assertEquals(2, snapshot.manifest.assets.size)
            assertEquals(snapshot.manifest.assets.toSet(), snapshot.localAssets.map { it.identity }.toSet())
        } finally {
            snapshot.close()
            fixture.root.deleteRecursively()
        }
    }

    private fun fixture(twoPathsSameBytes: Boolean = false): Fixture {
        val root = Files.createTempDirectory("u8j-snapshot").toFile()
        val project = ProjectFactory(idGenerator = { "project-snapshot" }, clock = { 1L })
            .create("Snapshot", ProjectTemplate.BLANK)
        val projectDir = File(File(root, "projects"), project.id).apply { mkdirs() }
        val firstPath = "media/source/source.wav"
        val firstFile = File(projectDir, firstPath).apply {
            parentFile.mkdirs()
            writeText("audio-content")
        }
        val first = asset("source", firstPath, firstFile)
        if (!twoPathsSameBytes) return Fixture(root, project.copy(assets = listOf(first)))

        val secondPath = "media/references/reference.wav"
        val secondFile = File(projectDir, secondPath).apply {
            parentFile.mkdirs()
            writeText("audio-content")
        }
        val second = asset("reference", secondPath, secondFile, role = AssetRole.REFERENCE_GUITAR)
        return Fixture(root, project.copy(assets = listOf(first, second)))
    }

    private fun asset(
        id: String,
        relativePath: String,
        file: File,
        role: AssetRole = AssetRole.SOURCE_ORIGINAL,
    ) = ManagedAsset(
        assetId = id,
        role = role,
        relativePath = relativePath,
        sha256 = BackupHashing.sha256(file),
        byteSize = file.length(),
        format = "wav",
        createdAtEpochMs = 1L,
        classification = AssetClassification.AUTHORITATIVE,
        lifecycle = AssetLifecycle.MANAGED,
    )

    private data class Fixture(
        val root: File,
        val project: studio.guitarlab.core.model.GuitarProject,
    )
}
