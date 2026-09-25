package studio.guitarlab.core.project

import java.io.File
import java.nio.file.Files
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import org.junit.Test
import studio.guitarlab.core.model.AssetClassification
import studio.guitarlab.core.model.AssetLifecycle
import studio.guitarlab.core.model.AssetRole
import studio.guitarlab.core.model.AudioClip
import studio.guitarlab.core.model.AudioTrack
import studio.guitarlab.core.model.BuiltInRoles
import studio.guitarlab.core.model.ChannelLayout
import studio.guitarlab.core.model.ManagedAsset
import studio.guitarlab.core.model.ProjectFactory
import studio.guitarlab.core.model.ProjectTemplate

class UnifiedDriveProductionStorageTest {
    @Test fun durableSnapshotSurvivesTransientFreezeCleanup() {
        val fixture = fixture()
        val store = FileUnifiedDriveSnapshotStore(File(fixture.root, "drive-vnext/snapshots"))
        val frozen = UnifiedDriveProjectSnapshotBuilder(
            fixture.root,
            nowEpochMs = { 10L },
        ).freeze(fixture.project, null)

        val expectedManifest = frozen.manifest
        store.persist(frozen)
        frozen.close()

        val restored = requireNotNull(store.load(fixture.project.id))
        assertEquals(expectedManifest, restored.manifest)
        assertEquals(
            expectedManifest.assets.toSet(),
            restored.localAssets.map { it.identity }.toSet(),
        )
        restored.localAssets.forEach { local ->
            assertTrue(local.file.isFile)
            assertEquals(local.identity.sha256, BackupHashing.sha256(local.file))
        }
    }

    @Test fun corruptedDurableSnapshotFailsClosed() {
        val fixture = fixture()
        val store = FileUnifiedDriveSnapshotStore(File(fixture.root, "drive-vnext/snapshots"))
        UnifiedDriveProjectSnapshotBuilder(fixture.root, nowEpochMs = { 10L })
            .freeze(fixture.project, null)
            .use(store::persist)

        val persisted = requireNotNull(store.load(fixture.project.id))
        persisted.localAssets.first().file.appendText("corruption")

        assertFailsWith<IllegalArgumentException> {
            store.load(fixture.project.id)
        }
    }

    @Test fun legacyRecordingRestoreVerifiesPersistedDigestBeforeTakeRecovery() {
        val root = Files.createTempDirectory("rc21-legacy-drive-restore").toFile()
        val base = ProjectFactory(idGenerator = { "legacy-drive" }, clock = { 1L })
            .create("Legacy", ProjectTemplate.GUITAR)
        val track = AudioTrack(
            id = "guitar-left",
            name = "Minha Guitarra E",
            roleId = BuiltInRoles.RECORDED_GUITAR_L,
            channelLayout = ChannelLayout.MONO,
            order = 0,
        )
        val relativePath = "media/source/legacy-clip-take-1790203123119.wav"
        val project = base.copy(
            tracks = listOf(track),
            clips = listOf(
                AudioClip(
                    id = "legacy-clip",
                    trackId = track.id,
                    name = "Take 1",
                    sourceUri = "managed://$relativePath",
                    managedSourcePath = relativePath,
                    startFrame = 0L,
                    sourceStartFrame = 10L,
                    lengthFrames = 90L,
                    sourceFormat = "WAV",
                    sourceSampleRateHz = 44_100,
                    sourceChannelCount = 2,
                    sourceBitsPerSample = 32,
                    sourceEncoding = "FLOAT32_LE",
                    sourceTotalFrames = 100L,
                ),
            ),
        )
        val projectDirectory = File(File(root, "projects"), project.id).apply { mkdirs() }
        File(projectDirectory, relativePath).apply {
            parentFile.mkdirs()
            writeText("legacy-recording-bytes")
        }

        val frozen = UnifiedDriveProjectSnapshotBuilder(root, nowEpochMs = { 10L })
            .freeze(project, null)
        try {
            val staging = materializeRestoreStaging(frozen)
            val plan = DriveRestorePlan(
                DriveCurrentDescriptor(
                    frozen.manifest.projectId,
                    frozen.manifest.revisionId,
                    frozen.manifest.manifestSha256,
                ),
                frozen.manifest,
                UnifiedDriveProjectRestoreLayout.targets(frozen.manifest),
            )

            UnifiedDriveLocalRestoreValidator().validate(staging, plan)

            val serialized = File(staging, "project.json").readText()
            val codec = ProjectCodec()
            assertTrue(codec.decodePersistedState(serialized).takes.isEmpty())
            assertEquals("legacy-clip", codec.decode(serialized).takes.single().id)
        } finally {
            frozen.close()
            root.deleteRecursively()
        }
    }

    @Test fun validatedRestoreCanPublishAsIndependentCopyWithoutChangingMediaBytes() {
        val fixture = fixture()
        val frozen = UnifiedDriveProjectSnapshotBuilder(
            fixture.root,
            nowEpochMs = { 10L },
        ).freeze(fixture.project, null)
        try {
            val staging = materializeRestoreStaging(frozen)
            val descriptor = DriveCurrentDescriptor(
                frozen.manifest.projectId,
                frozen.manifest.revisionId,
                frozen.manifest.manifestSha256,
            )
            val plan = DriveRestorePlan(
                descriptor,
                frozen.manifest,
                UnifiedDriveProjectRestoreLayout.targets(frozen.manifest),
            )
            UnifiedDriveLocalRestoreValidator().validate(staging, plan)
            val publication = UnifiedDriveLocalProjectPublisher(
                rootDirectory = fixture.root,
                idGenerator = { "copy-project" },
                nowEpochMs = { 20L },
            ).publish(staging, plan, DriveConflictAction.IMPORT_AS_COPY)

            assertEquals("copy-project", publication.project.id)
            assertNotEquals(fixture.project.id, publication.project.id)
            assertTrue(FileProjectRepository(fixture.root).load(fixture.project.id) != null)
            val copied = requireNotNull(FileProjectRepository(fixture.root).load("copy-project"))
            assertEquals(publication.project, copied)
            val copiedMedia = File(
                File(File(fixture.root, "projects"), "copy-project"),
                fixture.mediaRelativePath,
            )
            assertEquals("audio-content", copiedMedia.readText())
        } finally {
            frozen.close()
        }
    }

    @Test fun useDriveReplacesExistingProjectOnlyAfterFullValidation() {
        val fixture = fixture()
        val remoteProject = fixture.project
        val frozen = UnifiedDriveProjectSnapshotBuilder(
            fixture.root,
            nowEpochMs = { 10L },
        ).freeze(remoteProject, null)
        try {
            val repository = FileProjectRepository(fixture.root)
            repository.save(
                remoteProject.copy(
                    masterGainDb = -8f,
                    updatedAtEpochMs = 99L,
                ),
            )
            val staging = materializeRestoreStaging(frozen)
            val descriptor = DriveCurrentDescriptor(
                frozen.manifest.projectId,
                frozen.manifest.revisionId,
                frozen.manifest.manifestSha256,
            )
            val plan = DriveRestorePlan(
                descriptor,
                frozen.manifest,
                UnifiedDriveProjectRestoreLayout.targets(frozen.manifest),
            )
            UnifiedDriveLocalRestoreValidator().validate(staging, plan)
            UnifiedDriveLocalProjectPublisher(fixture.root)
                .publish(staging, plan, DriveConflictAction.USE_DRIVE)

            assertEquals(remoteProject, repository.load(remoteProject.id))
        } finally {
            frozen.close()
        }
    }

    @Test fun validatorRejectsCorruptedReferencedMediaBeforePublication() {
        val fixture = fixture()
        val frozen = UnifiedDriveProjectSnapshotBuilder(
            fixture.root,
            nowEpochMs = { 10L },
        ).freeze(fixture.project, null)
        try {
            val staging = materializeRestoreStaging(frozen)
            File(staging, fixture.mediaRelativePath).writeText("wrong")
            val descriptor = DriveCurrentDescriptor(
                frozen.manifest.projectId,
                frozen.manifest.revisionId,
                frozen.manifest.manifestSha256,
            )
            val plan = DriveRestorePlan(
                descriptor,
                frozen.manifest,
                UnifiedDriveProjectRestoreLayout.targets(frozen.manifest),
            )
            assertFailsWith<IllegalArgumentException> {
                UnifiedDriveLocalRestoreValidator().validate(staging, plan)
            }
        } finally {
            frozen.close()
        }
    }

    private fun materializeRestoreStaging(
        snapshot: FrozenUnifiedDriveProjectSnapshot,
    ): File {
        val staging = Files.createTempDirectory("u8k-restore-staging").toFile()
        val byHash = snapshot.localAssets.associateBy { it.identity.sha256 }
        UnifiedDriveProjectRestoreLayout.targets(snapshot.manifest).forEach { target ->
            val source = requireNotNull(byHash[target.identity.sha256]).file
            val destination = File(staging, target.relativePath)
            destination.parentFile?.mkdirs()
            source.copyTo(destination)
        }
        return staging
    }

    private fun fixture(): Fixture {
        val root = Files.createTempDirectory("u8k-production-storage").toFile()
        val base = ProjectFactory(
            idGenerator = { "project" },
            clock = { 1L },
        ).create("Projeto", ProjectTemplate.BLANK)
        val projectDirectory = File(File(root, "projects"), base.id).apply { mkdirs() }
        val mediaRelativePath = "media/source/source.wav"
        val media = File(projectDirectory, mediaRelativePath).apply {
            parentFile.mkdirs()
            writeText("audio-content")
        }
        val asset = ManagedAsset(
            assetId = "source",
            role = AssetRole.SOURCE_ORIGINAL,
            relativePath = mediaRelativePath,
            sha256 = BackupHashing.sha256(media),
            byteSize = media.length(),
            format = "wav",
            createdAtEpochMs = 1L,
            classification = AssetClassification.AUTHORITATIVE,
            lifecycle = AssetLifecycle.MANAGED,
        )
        val project = base.copy(assets = listOf(asset))
        FileProjectRepository(root).save(project)
        return Fixture(root, project, mediaRelativePath)
    }

    private data class Fixture(
        val root: File,
        val project: studio.guitarlab.core.model.GuitarProject,
        val mediaRelativePath: String,
    )
}
