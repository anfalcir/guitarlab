package studio.guitarlab.core.project

import java.io.File
import studio.guitarlab.core.model.ProjectFactory
import studio.guitarlab.core.model.ProjectTemplate
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.test.assertContentEquals
import kotlin.test.assertFailsWith
import studio.guitarlab.core.model.AudioClip
import studio.guitarlab.core.model.AudioTrack
import studio.guitarlab.core.model.AssetClassification
import studio.guitarlab.core.model.AssetLifecycle
import studio.guitarlab.core.model.AssetRole
import studio.guitarlab.core.model.ManagedAsset
import studio.guitarlab.core.model.PreparationState
import studio.guitarlab.core.model.PreparationStatus

class FileProjectRepositoryTest {
    @Test
    fun saveLoadListAndDeleteRoundTrip() {
        val root = Files.createTempDirectory("guitarlab-test").toFile()
        try {
            val repository = FileProjectRepository(root)
            var nextId = 0
            val project = ProjectFactory(idGenerator = { "id-${nextId++}" }, clock = { 100L })
                .create("Tone Test", ProjectTemplate.GUITAR)

            repository.save(project)
            assertEquals(1, repository.list().size)
            assertNotNull(repository.load(project.id))
            assertTrue(repository.delete(project.id))
            assertTrue(repository.list().isEmpty())
        } finally {
            root.deleteRecursively()
        }
    }


    @Test
    fun renamePreservesStableProjectIdentityWhileDuplicateGetsFreshIdentity() {
        val root = Files.createTempDirectory("guitarlab-project-identity").toFile()
        try {
            val repository = FileProjectRepository(root)
            val original = ProjectFactory(idGenerator = { "stable-id" }, clock = { 100L })
                .create("Original", ProjectTemplate.BLANK)
            repository.save(original)

            repository.save(original.copy(name = "Renamed", updatedAtEpochMs = 200L))
            val renamed = repository.load("stable-id")
            assertEquals("stable-id", renamed?.id)
            assertEquals("Renamed", renamed?.name)

            val duplicate = repository.duplicate("stable-id", "Renamed - Cópia", "fresh-id", 300L)
            assertEquals("fresh-id", duplicate.id)
            assertEquals("stable-id", repository.load("stable-id")?.id)
            assertEquals(2, repository.list().map { it.id }.toSet().size)
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun duplicateCopiesManagedSourceAndProxyAndRollsBackOnMissingMedia() {
        val root = Files.createTempDirectory("guitarlab-duplicate").toFile()
        try {
            val repository = FileProjectRepository(root)
            val sourceBytes = byteArrayOf(1, 2, 3, 4)
            val proxyBytes = byteArrayOf(5, 6, 7)
            val project = ProjectFactory(idGenerator = { "source" }, clock = { 100L })
                .create("Original", ProjectTemplate.BLANK)
                .copy(
                    tracks = listOf(AudioTrack("t1", "Track", order = 0)),
                    clips = listOf(AudioClip(
                        id = "c1", trackId = "t1", name = "Take", sourceUri = "managed://media/source/take.wav",
                        startFrame = 0, lengthFrames = 10, sourceTotalFrames = 10,
                        managedSourcePath = "media/source/take.wav", managedEditProxyPath = "media/proxy/take.wav",
                    )),
                )
            repository.save(project)
            val sourceDir = File(root, "projects/source")
            File(sourceDir, "media/source").mkdirs()
            File(sourceDir, "media/proxy").mkdirs()
            File(sourceDir, "media/source/take.wav").writeBytes(sourceBytes)
            File(sourceDir, "media/proxy/take.wav").writeBytes(proxyBytes)

            val duplicate = repository.duplicate("source", "Copy", "copy", 200L)
            assertEquals("Copy", duplicate.name)
            assertContentEquals(sourceBytes, File(root, "projects/copy/media/source/take.wav").readBytes())
            assertContentEquals(proxyBytes, File(root, "projects/copy/media/proxy/take.wav").readBytes())

            File(sourceDir, "media/proxy/take.wav").delete()
            assertFailsWith<IllegalArgumentException> { repository.duplicate("source", "Broken", "broken", 300L) }
            assertTrue(!File(root, "projects/broken").exists())
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun duplicateNormalizesTransientPreparationStateWithoutLosingDurableSource() {
        val root = Files.createTempDirectory("guitarlab-duplicate-lifecycle").toFile()
        try {
            val repository = FileProjectRepository(root)
            val source = ManagedAsset(
                assetId = "source-asset", role = AssetRole.SOURCE_ORIGINAL, relativePath = "media/source.wav",
                sha256 = "a".repeat(64), byteSize = 128, format = "wav", sampleRateHz = 44_100, channelCount = 2, frameCount = 32,
                createdAtEpochMs = 1L, classification = AssetClassification.AUTHORITATIVE, lifecycle = AssetLifecycle.MANAGED,
            )
            val original = ProjectFactory({ "source-project" }, { 1L }).create("Original", ProjectTemplate.BLANK).copy(
                assets = listOf(source),
                preparation = PreparationState(status = PreparationStatus.SEPARATING, sourceAssetId = source.assetId),
            )
            repository.save(original)
            val media = File(root, "projects/source-project/media/source.wav")
            media.parentFile!!.mkdirs()
            media.writeBytes(ByteArray(128) { 7 })

            val duplicate = repository.duplicate("source-project", "Copy", "copy-project", 5L)

            assertEquals(PreparationStatus.SOURCE_READY, duplicate.preparation?.status)
            assertEquals(source.assetId, duplicate.preparation?.sourceAssetId)
            assertTrue(File(root, "projects/copy-project/media/source.wav").isFile)
            assertEquals(PreparationStatus.SEPARATING, repository.load("source-project")?.preparation?.status)
        } finally { root.deleteRecursively() }
    }

    @Test
    fun unsafeAndPreviouslyCollidingIdsStayConfinedAndIndependent() {
        val root = Files.createTempDirectory("guitarlab-storage-key").toFile()
        try {
            val repository = FileProjectRepository(root)
            val slash = ProjectFactory(idGenerator = { "a/b" }, clock = { 1L })
                .create("Slash", ProjectTemplate.BLANK)
            val question = ProjectFactory(idGenerator = { "a?b" }, clock = { 2L })
                .create("Question", ProjectTemplate.BLANK)
            val parent = ProjectFactory(idGenerator = { ".." }, clock = { 3L })
                .create("Parent", ProjectTemplate.BLANK)

            repository.save(slash)
            repository.save(question)
            repository.save(parent)

            assertEquals("Slash", repository.load("a/b")?.name)
            assertEquals("Question", repository.load("a?b")?.name)
            assertEquals("Parent", repository.load("..")?.name)
            assertEquals(3, repository.list().size)
            assertTrue(repository.delete(".."))
            assertTrue(root.isDirectory)
            assertTrue(File(root, "projects").isDirectory)
            assertEquals(2, repository.list().size)
        } finally { root.deleteRecursively() }
    }

    @Test
    fun blankProjectIdIsRejectedWithoutWritingAtProjectsRoot() {
        val root = Files.createTempDirectory("guitarlab-blank-id").toFile()
        try {
            val repository = FileProjectRepository(root)
            val project = ProjectFactory(idGenerator = { "" }, clock = { 1L })
                .create("Invalid", ProjectTemplate.BLANK)

            assertFailsWith<IllegalArgumentException> { repository.save(project) }
            assertTrue(!File(root, "projects/project.json").exists())
        } finally { root.deleteRecursively() }
    }
    @Test
    fun recordedSplitMoveDeleteRoundTripsWithoutDanglingTakeLineage() {
        val root = Files.createTempDirectory("guitarlab-split-lineage-roundtrip").toFile()
        try {
            val repository = FileProjectRepository(root)
            val base = ProjectFactory(idGenerator = { "p-lineage" }, clock = { 10L })
                .create("Lineage", ProjectTemplate.BLANK)
                .copy(
                    tracks = listOf(
                        AudioTrack("t1", "Guitar", order = 0),
                        AudioTrack("t2", "Double", order = 1),
                    ),
                    clips = listOf(
                        AudioClip(
                            id = "c1",
                            trackId = "t1",
                            name = "Take",
                            sourceUri = "managed://media/source/take.wav",
                            managedSourcePath = "media/source/take.wav",
                            startFrame = 0,
                            lengthFrames = 48_000,
                            sourceTotalFrames = 48_000,
                            takeId = "take-1",
                        )
                    ),
                    takes = listOf(
                        studio.guitarlab.core.model.RecordingTake(
                            id = "take-1",
                            trackId = "t1",
                            clipId = "c1",
                            name = "Take 1",
                            createdAtEpochMs = 10L,
                            active = true,
                        )
                    ),
                )

            val split = ProjectClipEditor.splitClipAtTimelineFrame(base, "c1", 24_000, "c2", 11L)
            val moved = ProjectClipEditor.moveClipToTrack(split, "c2", "t2", 12L)
            repository.save(moved)
            val reopenedAfterMove = requireNotNull(repository.load(base.id))
            assertTrue(studio.guitarlab.core.model.ProjectValidator.validate(reopenedAfterMove).isEmpty())
            assertEquals("take-1", reopenedAfterMove.clips.first { it.id == "c1" }.takeId)
            assertEquals(null, reopenedAfterMove.clips.first { it.id == "c2" }.takeId)
            assertEquals("c1", reopenedAfterMove.takes.single().clipId)

            val deleted = ProjectClipEditor.removeClip(reopenedAfterMove, "c1", 13L)
            repository.save(deleted)
            val reopenedAfterDelete = requireNotNull(repository.load(base.id))
            assertTrue(studio.guitarlab.core.model.ProjectValidator.validate(reopenedAfterDelete).isEmpty())
            assertEquals(listOf("c2"), reopenedAfterDelete.clips.map { it.id })
            assertTrue(reopenedAfterDelete.takes.isEmpty())
        } finally {
            root.deleteRecursively()
        }
    }

}
