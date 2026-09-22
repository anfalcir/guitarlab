package studio.guitarlab.core.project

import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import studio.guitarlab.core.codec.FloatWavFileWriter
import studio.guitarlab.core.model.AudioTrack
import studio.guitarlab.core.model.ChannelLayout
import studio.guitarlab.core.model.GuitarProject
import studio.guitarlab.core.model.ProjectTemplate

class RecordingRecoveryPolicyTest {
    @Test
    fun interruptedCanonicalWavIsRepairableAndRecoverable() {
        val root = createTempDirectory("guitarlab-h30-recovery").toFile()
        try {
            val store = ProjectRecordingMediaStore(root, idFactory = { "take-r" }, nowEpochMs = { 100L })
            val tx = store.begin("project-r", "Take.wav", targetTrackId = "track-r", requestedTimelineStartFrame = 240L)
            writeCanonicalTake(tx.temporaryFile, 48_000, 1, 1_000)
            // Simulate abrupt process death before FloatWavFileWriter.finish(): zero RIFF/data sizes.
            zeroDeclaredSizes(tx.temporaryFile)

            val candidate = store.recoveryCandidates("project-r").single()
            assertTrue(candidate.safePlayable)
            assertEquals(1_000L, candidate.frames)
            assertEquals(240L, RecordingRecoveryPolicy.timelineStart(candidate))
            assertEquals(
                RecordingRecoveryPublicationDecision.RECOVERABLE,
                RecordingRecoveryPolicy.publicationDecision(project(), candidate),
            )
            val final = store.promoteRecovery(candidate)
            assertTrue(final.isFile)
            assertFalse(tx.temporaryFile.exists())
            assertTrue(tx.markerFile.exists())
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun finalizedUnpublishedMarkerMakesPublicationIdempotent() {
        val root = createTempDirectory("guitarlab-h30-post-rename").toFile()
        try {
            val store = ProjectRecordingMediaStore(root, idFactory = { "take-post" }, nowEpochMs = { 200L })
            val tx = store.begin("project-r", "Take.wav", targetTrackId = "track-r", requestedTimelineStartFrame = 12L)
            writeCanonicalTake(tx.temporaryFile, 48_000, 1, 480)
            store.preparePublication(tx, "track-r", 10L, 2L, 478L, 48_000, 1, 480L)
            store.commit(tx)

            val candidate = store.recoveryCandidates("project-r").single()
            assertEquals(RecordingRecoveryMediaState.FINALIZED_UNPUBLISHED, candidate.state)
            assertEquals(10L, RecordingRecoveryPolicy.timelineStart(candidate))
            assertEquals(2L, RecordingRecoveryPolicy.sourceStart(candidate))
            assertEquals(478L, RecordingRecoveryPolicy.lengthFrames(candidate))

            val published = project().copy(
                clips = listOf(
                    studio.guitarlab.core.model.AudioClip(
                        id = "take-post", trackId = "track-r", name = "Recovered", sourceUri = "managed://${tx.relativePath}",
                        managedSourcePath = tx.relativePath, startFrame = 10L, sourceStartFrame = 2L, lengthFrames = 478L,
                        sourceSampleRateHz = 48_000, sourceChannelCount = 1, takeId = "take-post",
                    )
                ),
                takes = listOf(studio.guitarlab.core.model.RecordingTake("take-post", "track-r", "take-post", "Recovered", 1L, true)),
            )
            assertEquals(
                RecordingRecoveryPublicationDecision.ALREADY_PUBLISHED,
                RecordingRecoveryPolicy.publicationDecision(published, candidate),
            )
            store.markPublished("project-r", "take-post")
            assertTrue(store.recoveryCandidates("project-r").isEmpty())
            assertTrue(tx.finalFile.isFile)
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun renamedProjectKeepsStableProjectIdentityForRecovery() {
        val candidate = RecordingRecoveryCandidate(
            projectId = "project-r", transactionId = "take-r", mediaFile = File("take.wav"), finalFile = File("take.wav"),
            relativePath = "media/source/take.wav",
            marker = RecordingRecoveryMarker(projectId="project-r", transactionId="take-r", suggestedFinalName="take.wav", targetTrackId="track-r", requestedTimelineStartFrame=0L, createdAtEpochMs=1L),
            state = RecordingRecoveryMediaState.FINALIZED_UNPUBLISHED, sampleRateHz=48_000, channelCount=1, frames=100L, safePlayable=true,
        )
        assertEquals(
            RecordingRecoveryPublicationDecision.RECOVERABLE,
            RecordingRecoveryPolicy.publicationDecision(project().copy(name = "Renamed project"), candidate),
        )
    }

    @Test
    fun malformedPayloadIsPreservedAndNeverGuessed() {
        val root = createTempDirectory("guitarlab-h30-unsafe").toFile()
        try {
            val store = ProjectRecordingMediaStore(root, idFactory = { "unsafe" })
            val tx = store.begin("project-r", targetTrackId = "track-r", requestedTimelineStartFrame = 0L)
            tx.temporaryFile.parentFile?.mkdirs()
            tx.temporaryFile.writeBytes(ByteArray(256) { 0x55.toByte() })
            val candidate = store.recoveryCandidates("project-r").single()
            assertFalse(candidate.safePlayable)
            assertTrue(tx.temporaryFile.isFile)
            assertTrue(tx.markerFile.isFile)
            assertEquals(
                RecordingRecoveryPublicationDecision.UNSAFE_PAYLOAD,
                RecordingRecoveryPolicy.publicationDecision(project(), candidate),
            )
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun collisionOnlyConvergesWhenBytesAreIdentical() {
        val root = createTempDirectory("guitarlab-h30-collision").toFile()
        try {
            val store = ProjectRecordingMediaStore(root, idFactory = { "collision" })
            val tx = store.begin("project-r", targetTrackId = "track-r", requestedTimelineStartFrame = 0L)
            writeCanonicalTake(tx.temporaryFile, 48_000, 1, 64)
            tx.finalFile.parentFile?.mkdirs()
            tx.temporaryFile.copyTo(tx.finalFile)
            store.commit(tx)
            assertFalse(tx.temporaryFile.exists())

            val second = ProjectRecordingMediaStore(root, idFactory = { "different" })
                .begin("project-r", targetTrackId = "track-r", requestedTimelineStartFrame = 0L)
            writeCanonicalTake(second.temporaryFile, 48_000, 1, 64)
            second.finalFile.parentFile?.mkdirs()
            second.finalFile.writeBytes(ByteArray(second.temporaryFile.length().toInt()) { 0x33.toByte() })
            assertFailsWith<IllegalArgumentException> { second.let(store::commit) }
            assertTrue(second.temporaryFile.exists())
            assertTrue(second.finalFile.exists())
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun explicitDiscardRemovesOnlySelectedRecoveryPayload() {
        val root = createTempDirectory("guitarlab-h30-discard").toFile()
        try {
            val store = ProjectRecordingMediaStore(root, idFactory = { "discard" })
            val tx = store.begin("project-r", targetTrackId = "track-r", requestedTimelineStartFrame = 0L)
            writeCanonicalTake(tx.temporaryFile, 48_000, 1, 64)
            val candidate = assertNotNull(store.recoveryCandidates("project-r").singleOrNull())
            store.discardRecovery(candidate)
            assertFalse(tx.temporaryFile.exists())
            assertFalse(tx.markerFile.exists())
        } finally {
            root.deleteRecursively()
        }
    }

    private fun project() = GuitarProject(
        id = "project-r",
        name = "Project",
        template = ProjectTemplate.GUITAR,
        createdAtEpochMs = 1L,
        updatedAtEpochMs = 1L,
        tracks = listOf(AudioTrack(id="track-r", name="Guitar", channelLayout=ChannelLayout.MONO, order=0)),
    )

    private fun writeCanonicalTake(file: File, rate: Int, channels: Int, frames: Int) {
        file.parentFile?.mkdirs()
        FloatWavFileWriter(file, rate, channels).use { writer ->
            writer.writeInterleaved(FloatArray(frames * channels) { index -> if (index % 2 == 0) .25f else -.25f }, frames)
        }
    }

    private fun zeroDeclaredSizes(file: File) {
        java.io.RandomAccessFile(file, "rw").use { out ->
            out.seek(4); repeat(4) { out.write(0) }
            out.seek(40); repeat(4) { out.write(0) }
        }
    }
}
