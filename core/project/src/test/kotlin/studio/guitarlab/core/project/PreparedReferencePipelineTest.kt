package studio.guitarlab.core.project

import java.io.File
import java.security.MessageDigest
import java.util.concurrent.atomic.AtomicInteger
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import studio.guitarlab.core.codec.FileSeekableByteSource
import studio.guitarlab.core.codec.FloatWavFileWriter
import studio.guitarlab.core.codec.WavPcmDecoder
import studio.guitarlab.core.model.*

class PreparedReferencePipelineTest {
    @get:Rule val temp = TemporaryFolder()

    @Test fun renderUsesFiveStemBackingAndOneSharedGainWithoutLosingAlignment() {
        val dir = temp.newFolder()
        val stems = linkedMapOf<AssetRole, File>()
        val roles = listOf(AssetRole.STEM_DRUMS, AssetRole.STEM_BASS, AssetRole.STEM_OTHER, AssetRole.STEM_VOCALS, AssetRole.STEM_PIANO, AssetRole.STEM_GUITAR)
        roles.forEachIndexed { index, role -> stems[role] = wav(File(dir, "$role.wav"), 16, (index + 1) * 0.1f) }
        val backing = File(dir, "backing.wav"); val guitar = File(dir, "guitar.wav")
        val result = PreparedReferenceRenderer.render(stems, backing, guitar)
        assertEquals(16, result.frames)
        assertEquals(44_100, result.sampleRateHz)
        assertTrue(result.sharedGainDb < 0.0)
        val backingSamples = decode(backing)
        val guitarSamples = decode(guitar)
        assertEquals(32, backingSamples.size)
        assertEquals(32, guitarSamples.size)
        val gain = guitarSamples[0] / 0.6f
        assertEquals((0.1f + 0.2f + 0.3f + 0.4f + 0.5f) * gain, backingSamples[0], 0.0001f)
        assertTrue(backingSamples.maxOf { kotlin.math.abs(it) } <= 0.8914f)
        assertTrue(guitarSamples.maxOf { kotlin.math.abs(it) } <= 0.8914f)
    }

    @Test fun preparePublishesZeroCopyReferencesAndRetryIsIdempotent() {
        val fixture = fixture()
        val first = fixture.service.prepare(fixture.projectId)
        assertFalse(first.reusedExisting)
        val prepared = first.project
        assertEquals(4, prepared.preparation!!.availableReferenceAssetIds.size)
        assertTrue(prepared.preparation!!.activeBackingAssetId != null)
        assertTrue(prepared.preparation!!.activeGuitarAssetId != null)
        assertEquals(3, prepared.referenceBindings.size)
        assertEquals(3, prepared.clips.size)
        prepared.clips.forEach { clip ->
            assertTrue(clip.managedSourcePath!!.startsWith("media/references/"))
            assertTrue(File(fixture.projectDir, clip.managedSourcePath!!).isFile)
        }
        val second = fixture.service.prepare(fixture.projectId)
        assertTrue(second.reusedExisting)
        assertEquals(prepared.assets.size, second.project.assets.size)
        assertEquals(prepared.referenceBindings, second.project.referenceBindings)
    }

    @Test fun newPreparationDoesNotSilentlyReplaceStudioBindingsAndExplicitUpdatePreservesCreativeState() {
        val fixture = fixture()
        val first = fixture.service.prepare(fixture.projectId).project
        val oldBindings = first.referenceBindings.associateBy { it.kind }
        val recordedTrack = first.tracks.first { it.roleId == BuiltInRoles.RECORDED_GUITAR_L }
        val recording = AudioClip("recording", recordedTrack.id, "Take 1", "managed://recording", 11, lengthFrames = 7, takeId = "take")
        val take = RecordingTake("take", recordedTrack.id, recording.id, "Take 1", 5, active = true)
        val marker = TimelineMarker("m", "Entrada", 9)
        val section = TimelineSection("s", "Refrão", 4, 20)
        val mixedTracks = first.tracks.map { if (it.id == recordedTrack.id) it.copy(gainDb = -4f, muted = true) else it }
        fixture.repo.save(first.copy(clips = first.clips + recording, takes = listOf(take), markers = listOf(marker), sections = listOf(section), tracks = mixedTracks, masterGainDb = -3f))
        fixture.replaceStemSet(scale = 0.03f)
        val second = fixture.service.prepare(fixture.projectId).project
        assertEquals(oldBindings.mapValues { it.value.assetId }, second.referenceBindings.associate { it.kind to it.assetId })
        assertTrue(PreparedReferenceBindingPolicy.updateAvailable(second))
        val updated = PreparedReferenceBindingPolicy.applyUpdate(second, 999) { "bind-${fixture.ids.incrementAndGet()}" }
        assertFalse(PreparedReferenceBindingPolicy.updateAvailable(updated))
        assertNotEquals(oldBindings[ReferenceBindingKind.BACKING]!!.assetId, updated.referenceBindings.first { it.kind == ReferenceBindingKind.BACKING }.assetId)
        assertEquals(take, updated.takes.single())
        assertEquals(marker, updated.markers.single())
        assertEquals(section, updated.sections.single())
        assertEquals(mixedTracks, updated.tracks)
        assertEquals(-3f, updated.masterGainDb)
        assertEquals(recording, updated.clips.single { it.id == "recording" })
    }

    @Test fun preparedReferenceRebindSurvivesSaveReopenAndUndoRedo() {
        val fixture = fixture()
        val first = fixture.service.prepare(fixture.projectId).project
        fixture.replaceStemSet(scale = 0.04f)
        val pending = fixture.service.prepare(fixture.projectId).project
        val updated = PreparedReferenceBindingPolicy.applyUpdate(pending, 999) { "bind-${fixture.ids.incrementAndGet()}" }

        val history = ProjectHistory()
        history.record(pending, updated)
        fixture.repo.save(updated)
        val reopened = fixture.repo.load(fixture.projectId)!!

        assertEquals(updated.referenceBindings, reopened.referenceBindings)
        assertEquals(updated.clips, reopened.clips)
        assertEquals(updated.preparation, reopened.preparation)

        val undone = history.undo(reopened)!!
        assertEquals(first.referenceBindings.map { it.assetId }, undone.referenceBindings.map { it.assetId })
        assertTrue(PreparedReferenceBindingPolicy.updateAvailable(undone))

        val redone = history.redo(undone)!!
        assertEquals(updated.referenceBindings, redone.referenceBindings)
        assertFalse(PreparedReferenceBindingPolicy.updateAvailable(redone))
    }

    @Test fun deletedPreparedClipsAreDetectedAndCanBeReinsertedWithoutNewSeparation() {
        val fixture = fixture()
        val prepared = fixture.service.prepare(fixture.projectId).project
        val withoutReferenceClips = prepared.copy(clips = emptyList())

        assertTrue(PreparedReferenceBindingPolicy.bindingDiffersFromDesired(withoutReferenceClips))
        assertTrue(PreparedReferenceBindingPolicy.updateAvailable(withoutReferenceClips))

        val repaired = PreparedReferenceBindingPolicy.applyUpdate(withoutReferenceClips, 999) {
            "repair-${fixture.ids.incrementAndGet()}"
        }
        assertEquals(3, repaired.clips.size)
        assertEquals(3, repaired.referenceBindings.size)
        assertFalse(PreparedReferenceBindingPolicy.bindingDiffersFromDesired(repaired))
    }

    @Test fun newPreparationAutomaticallyFillsReferenceTracksThatUserCleared() {
        val fixture = fixture()
        val first = fixture.service.prepare(fixture.projectId).project
        fixture.repo.save(first.copy(clips = emptyList()))
        fixture.replaceStemSet(scale = 0.03f)

        val second = fixture.service.prepare(fixture.projectId).project

        assertEquals(3, second.clips.size)
        assertFalse(PreparedReferenceBindingPolicy.bindingDiffersFromDesired(second))
        assertNotEquals(
            first.preparation!!.activeBackingAssetId,
            second.preparation!!.activeBackingAssetId,
        )
    }

    @Test fun keepCurrentAcknowledgesExactPreparedRevisionDurablyWithoutChangingCreativeState() {
        val fixture = fixture()
        val first = fixture.service.prepare(fixture.projectId).project
        fixture.replaceStemSet(scale = 0.05f)
        val pending = fixture.service.prepare(fixture.projectId).project
        assertTrue(PreparedReferenceBindingPolicy.updateAvailable(pending))

        val acknowledged = PreparedReferenceBindingPolicy.acknowledgeCurrent(pending, 1_000)
        assertFalse(PreparedReferenceBindingPolicy.updateAvailable(acknowledged))
        assertTrue(PreparedReferenceBindingPolicy.bindingDiffersFromDesired(acknowledged))
        assertEquals(first.referenceBindings, acknowledged.referenceBindings)
        assertEquals(pending.clips, acknowledged.clips)
        assertEquals(pending.takes, acknowledged.takes)
        assertEquals(pending.sections, acknowledged.sections)
        fixture.repo.save(acknowledged)
        assertFalse(PreparedReferenceBindingPolicy.updateAvailable(fixture.repo.load(fixture.projectId)!!))

        fixture.replaceStemSet(scale = 0.06f)
        val newer = fixture.service.prepare(fixture.projectId).project
        assertTrue(PreparedReferenceBindingPolicy.updateAvailable(newer))
    }

    @Test fun corruptStemFailsClosedBeforeProjectMutation() {
        val fixture = fixture()
        val before = fixture.repo.load(fixture.projectId)!!
        val stemId = before.preparation!!.activeStemAssetIds.getValue(AssetRole.STEM_DRUMS)
        val stem = before.assets.single { it.assetId == stemId }
        File(fixture.projectDir, stem.relativePath).appendBytes(byteArrayOf(1))
        assertThrows(IllegalArgumentException::class.java) { fixture.service.prepare(fixture.projectId) }
        assertEquals(before, fixture.repo.load(fixture.projectId))
        assertTrue(File(fixture.projectDir, "media/references").listFiles().orEmpty().isEmpty())
    }

    private data class Fixture(
        val root: File,
        val projectId: String,
        val repo: FileProjectRepository,
        val media: ProjectManagedMediaStore,
        val service: PreparedReferenceService,
        val ids: AtomicInteger,
    ) {
        val projectDir: File get() = File(File(root, "projects"), ManagedStorageKey.from(projectId))
        fun replaceStemSet(scale: Float) {
            val current = repo.load(projectId)!!
            val old = current.preparation!!
            val generation = ids.incrementAndGet()
            val newAssets = mutableListOf<ManagedAsset>()
            val map = linkedMapOf<AssetRole, String>()
            val roles = listOf(AssetRole.STEM_DRUMS, AssetRole.STEM_BASS, AssetRole.STEM_OTHER, AssetRole.STEM_VOCALS, AssetRole.STEM_GUITAR, AssetRole.STEM_PIANO)
            roles.forEachIndexed { index, role ->
                val file = File(root, "replacement-$generation-$role.wav")
                wav(file, 32, scale * (index + 1))
                val managed = file.inputStream().use { media.ingestStem(projectId, "replacement-$generation-$role.wav", it) }
                val asset = ManagedAsset("replacement-$generation-${role.name}", role, managed.relativePath, sha(managed.file), managed.byteCount, "wav", 44_100, 2, 32, 3, AssetClassification.AUTHORITATIVE, AssetLifecycle.MANAGED, AssetProvenance("REMOTE_SEPARATION", parameters = mapOf("jobId" to "replacement-$generation")))
                newAssets += asset; map[role] = asset.assetId
            }
            repo.save(current.copy(assets = current.assets + newAssets, preparation = old.copy(activeStemAssetIds = map), updatedAtEpochMs = 3))
        }
    }

    private fun fixture(): Fixture {
        val root = temp.newFolder()
        val repo = FileProjectRepository(root)
        val ids = AtomicInteger()
        val media = ProjectManagedMediaStore(root) { "media-${ids.incrementAndGet()}" }
        val projectId = "project-u5"
        val factoryCounter = AtomicInteger()
        val base = ProjectFactory(idGenerator = { if (factoryCounter.getAndIncrement() == 0) projectId else "id-${factoryCounter.get()}" }, clock = { 1 }).create("U5", ProjectTemplate.GUITAR)
        val roles = listOf(AssetRole.STEM_DRUMS, AssetRole.STEM_BASS, AssetRole.STEM_OTHER, AssetRole.STEM_VOCALS, AssetRole.STEM_GUITAR, AssetRole.STEM_PIANO)
        val assets = mutableListOf<ManagedAsset>(); val map = linkedMapOf<AssetRole, String>()
        roles.forEachIndexed { index, role ->
            val raw = wav(File(root, "$role.wav"), 32, (index + 1) * 0.02f)
            val managed = raw.inputStream().use { media.ingestStem(projectId, "$role.wav", it) }
            val asset = ManagedAsset("stem-${role.name}", role, managed.relativePath, sha(managed.file), managed.byteCount, "wav", 44_100, 2, 32, 2, AssetClassification.AUTHORITATIVE, AssetLifecycle.MANAGED, AssetProvenance("REMOTE_SEPARATION", parameters = mapOf("jobId" to "job")))
            assets += asset; map[role] = asset.assetId
        }
        repo.save(base.copy(assets = assets, preparation = PreparationState(PreparationStatus.READY, activeStemAssetIds = map)))
        return Fixture(root, projectId, repo, media, PreparedReferenceService(repo, media, File(root, "tmp"), nowMs = { 10 }, idFactory = { "asset-${ids.incrementAndGet()}" }), ids)
    }

    companion object {
        private fun wav(file: File, frames: Int, value: Float): File {
            FloatWavFileWriter(file, 44_100, 2).use { writer ->
                val samples = FloatArray(frames * 2) { index -> if (index % 2 == 0) value else value * 0.5f }
                writer.writeInterleaved(samples, frames)
            }
            return file
        }
        private fun decode(file: File): FloatArray = FileSeekableByteSource(file).use { source ->
            val decoder = WavPcmDecoder(source); FloatArray(decoder.metadata.totalFrames.toInt() * decoder.metadata.channelCount).also { decoder.readInterleaved(it, 0, decoder.metadata.totalFrames.toInt()) }
        }
        private fun sha(file: File): String = MessageDigest.getInstance("SHA-256").digest(file.readBytes()).joinToString("") { "%02x".format(it) }
    }
}
