package studio.guitarlab.core.project

import java.io.File
import java.nio.file.Files
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import studio.guitarlab.core.model.ProjectFactory
import studio.guitarlab.core.model.ProjectTemplate
import studio.guitarlab.core.model.AssetClassification
import studio.guitarlab.core.model.AssetLifecycle
import studio.guitarlab.core.model.AssetRole
import studio.guitarlab.core.model.ManagedAsset
import studio.guitarlab.core.model.PreparationState
import studio.guitarlab.core.model.PreparationStatus
import studio.guitarlab.core.model.AudioTrack
import studio.guitarlab.core.model.AudioClip
import studio.guitarlab.core.model.RecordingTake

class SourceAssetPublisherTest {
    @Test fun publishIsIdempotentForSameOperation() {
        val root = Files.createTempDirectory("source-publish").toFile()
        val repo = FileProjectRepository(root)
        val mediaStore = ProjectManagedMediaStore(root) { "media-fixed" }
        val project = ProjectFactory({ "project-1" }, { 1L }).create("Song", ProjectTemplate.BLANK)
        repo.save(project)
        val staged = File(root, "source.wav").apply { writeBytes(ByteArray(128) { it.toByte() }) }
        var assetCounter = 0
        val publisher = SourceAssetPublisher(repo, mediaStore, { 2L }) { "asset-${++assetCounter}" }
        val request = SourcePublicationRequest("project-1", "op-1", null, ValidatedSourceMedia(staged, "source.wav", "wav", 48_000, 2, 32, "local-import"))

        val first = publisher.publish(request)
        val second = publisher.publish(request)

        assertEquals(first.asset.assetId, second.asset.assetId)
        assertTrue(second.alreadyPublished)
        assertEquals(1, repo.load("project-1")!!.assets.size)
    }

    @Test fun renameDuringAcquisitionIsPreserved() {
        val root = Files.createTempDirectory("source-rename").toFile()
        val repo = FileProjectRepository(root)
        val project = ProjectFactory({ "project-1" }, { 1L }).create("Before", ProjectTemplate.BLANK)
        repo.save(project.copy(name = "After", updatedAtEpochMs = 5L))
        val staged = File(root, "source.wav").apply { writeBytes(ByteArray(128) { 7 }) }
        val publisher = SourceAssetPublisher(repo, ProjectManagedMediaStore(root), { 6L }) { "asset-1" }

        publisher.publish(SourcePublicationRequest("project-1", "op-rename", null, ValidatedSourceMedia(staged, "source.wav", "wav", 44_100, 2, 32, "local-import")))

        assertEquals("After", repo.load("project-1")!!.name)
    }

    @Test fun staleOperationCannotReplaceNewerSource() {
        val root = Files.createTempDirectory("source-stale").toFile()
        val repo = FileProjectRepository(root)
        repo.save(ProjectFactory({ "project-1" }, { 1L }).create("Song", ProjectTemplate.BLANK))
        val publisher = SourceAssetPublisher(repo, ProjectManagedMediaStore(root), { 2L }) { java.util.UUID.randomUUID().toString() }
        val a = File(root, "a.wav").apply { writeBytes(ByteArray(128) { 1 }) }
        val b = File(root, "b.wav").apply { writeBytes(ByteArray(128) { 2 }) }
        val first = publisher.publish(SourcePublicationRequest("project-1", "op-new", null, ValidatedSourceMedia(a, "a.wav", "wav", 44_100, 2, 32, "local-import")))
        val firstAsset = first.asset.assetId

        val failure = runCatching {
            publisher.publish(SourcePublicationRequest("project-1", "op-stale", null, ValidatedSourceMedia(b, "b.wav", "wav", 44_100, 2, 32, "local-import")))
        }.exceptionOrNull()

        assertTrue(failure is IllegalArgumentException)
        assertEquals(firstAsset, repo.load("project-1")!!.preparation!!.sourceAssetId)
        assertNotEquals("op-stale", repo.load("project-1")!!.assets.single().provenance!!.parameters["operationId"])
    }
    @Test fun sourceReplacementStartsFreshPrepareGenerationAndPreservesCreativeStateAndOldAssets() {
        val root = Files.createTempDirectory("source-replacement").toFile()
        val repo = FileProjectRepository(root)
        val mediaStore = ProjectManagedMediaStore(root)
        repo.save(ProjectFactory({ "project-1" }, { 1L }).create("Custom name", ProjectTemplate.BLANK))
        val oldFile = File(root, "old.wav").apply { writeBytes(ByteArray(128) { 1 }) }
        var counter = 0
        val publisher = SourceAssetPublisher(repo, mediaStore, { 10L + counter }) { "asset-${++counter}" }
        val oldSource = publisher.publish(
            SourcePublicationRequest("project-1", "op-old", null, ValidatedSourceMedia(oldFile, "old.wav", "wav", 44_100, 2, 32, "local-import")),
        ).asset

        val stemRoles = listOf(
            AssetRole.STEM_DRUMS, AssetRole.STEM_BASS, AssetRole.STEM_OTHER,
            AssetRole.STEM_VOCALS, AssetRole.STEM_GUITAR, AssetRole.STEM_PIANO,
        )
        val stems = stemRoles.mapIndexed { index, role -> testAsset("stem-$index", role) }
        val backing = testAsset("backing-old", AssetRole.REFERENCE_BACKING)
        val guitar = testAsset("guitar-old", AssetRole.REFERENCE_GUITAR)
        val track = AudioTrack("track-1", "Minha guitarra", order = 0)
        val clip = AudioClip("clip-1", track.id, "Take", "managed://take.wav", 0, lengthFrames = 32, sourceTotalFrames = 32, takeId = "take-1")
        val take = RecordingTake("take-1", track.id, clip.id, "Take 1", 5L)
        val prepared = requireNotNull(repo.load("project-1")).copy(
            assets = requireNotNull(repo.load("project-1")).assets + stems + backing + guitar,
            preparation = PreparationState(
                status = PreparationStatus.READY,
                sourceAssetId = oldSource.assetId,
                activeStemAssetIds = stems.associate { it.role to it.assetId },
                activeBackingAssetId = backing.assetId,
                activeGuitarAssetId = guitar.assetId,
                availableReferenceAssetIds = listOf(backing.assetId, guitar.assetId),
            ),
            tracks = listOf(track), clips = listOf(clip), takes = listOf(take),
        )
        repo.save(prepared)

        val replacementFile = File(root, "new.wav").apply { writeBytes(ByteArray(128) { 2 }) }
        val replacement = publisher.publish(
            SourcePublicationRequest("project-1", "op-new", oldSource.assetId, ValidatedSourceMedia(replacementFile, "new.wav", "wav", 48_000, 2, 32, "local-import")),
        ).asset
        val updated = requireNotNull(repo.load("project-1"))

        assertEquals("Custom name", updated.name)
        assertEquals(replacement.assetId, updated.preparation?.sourceAssetId)
        assertEquals(PreparationStatus.SOURCE_READY, updated.preparation?.status)
        assertTrue(updated.preparation?.activeStemAssetIds?.isEmpty() == true)
        assertEquals(null, updated.preparation?.activeBackingAssetId)
        assertEquals(null, updated.preparation?.activeGuitarAssetId)
        assertTrue(updated.preparation?.availableReferenceAssetIds?.isEmpty() == true)
        assertTrue((listOf(oldSource.assetId) + stems.map { it.assetId } + backing.assetId + guitar.assetId).all { id -> updated.assets.any { it.assetId == id } })
        assertEquals(listOf(track), updated.tracks)
        assertEquals(listOf(clip), updated.clips)
        assertEquals(listOf(take), updated.takes)
    }

    private fun testAsset(id: String, role: AssetRole) = ManagedAsset(
        assetId = id, role = role, relativePath = "media/$id.wav", sha256 = "a".repeat(64),
        byteSize = 128, format = "wav", sampleRateHz = 44_100, channelCount = 2, frameCount = 32,
        createdAtEpochMs = 3L, classification = if (role == AssetRole.REFERENCE_BACKING || role == AssetRole.REFERENCE_GUITAR) AssetClassification.DERIVED else AssetClassification.AUTHORITATIVE,
        lifecycle = AssetLifecycle.MANAGED,
    )

}
