package studio.guitarlab.core.project

import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import studio.guitarlab.core.model.AudioClip
import studio.guitarlab.core.model.AudioTrack
import studio.guitarlab.core.model.GuitarProject
import studio.guitarlab.core.model.ProjectTemplate

class ProjectMediaIntegrityAuditorTest {
    @Test
    fun auditSeparatesMissingReferencesRecoverableTakesAndSafeCleanupCandidates() {
        val root = createTempDirectory("guitarlab-media-audit").toFile()
        try {
            val project = project()
            val projectRoot = File(root, "projects/${ManagedStorageKey.from(project.id)}")
            val source = File(projectRoot, "media/source").apply { mkdirs() }
            val proxy = File(projectRoot, "media/proxy").apply { mkdirs() }
            val recording = File(projectRoot, "media/recording").apply { mkdirs() }
            val waveform = File(projectRoot, "media/derived/waveform").apply { mkdirs() }

            File(source, "referenced.wav").writeBytes(ByteArray(60))
            File(source, "orphan-source.wav").writeBytes(ByteArray(60))
            File(source, ".copy.wav.part").writeBytes(ByteArray(20))
            File(proxy, "orphan-proxy.wav").writeBytes(ByteArray(60))
            File(proxy, ".unused.part").writeBytes(ByteArray(20))
            File(recording, "partial.recording.part.wav").writeBytes(ByteArray(60))
            File(recording, "empty.recording.part.wav").writeBytes(ByteArray(44))
            File(waveform, "orphan.glwf").writeBytes(ByteArray(8))

            val auditor = ProjectMediaIntegrityAuditor(root)
            val report = auditor.audit(project)

            assertEquals(listOf("media/proxy/missing-proxy.wav"), report.missingReferencedMedia)
            assertEquals(listOf("media/source/orphan-source.wav"), report.unreferencedSourcesRetained)
            assertEquals(listOf("media/proxy/orphan-proxy.wav"), report.unreferencedProxies)
            assertEquals(listOf("media/recording/partial.recording.part.wav"), report.recoverableInterruptedRecordings)
            assertEquals(
                listOf("media/proxy/.unused.part", "media/recording/empty.recording.part.wav", "media/source/.copy.wav.part"),
                report.removableTemporaries,
            )
            assertEquals(listOf("media/derived/waveform/orphan.glwf"), report.orphanWaveforms)
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun safeCleanupNeverDeletesImmutableSourceProxyOrRecoverableRecordingPayload() {
        val root = createTempDirectory("guitarlab-media-cleanup").toFile()
        try {
            val project = project(withMissingProxy = false)
            val projectRoot = File(root, "projects/${ManagedStorageKey.from(project.id)}")
            val source = File(projectRoot, "media/source").apply { mkdirs() }
            val proxy = File(projectRoot, "media/proxy").apply { mkdirs() }
            val recording = File(projectRoot, "media/recording").apply { mkdirs() }
            val waveform = File(projectRoot, "media/derived/waveform").apply { mkdirs() }

            val referencedSource = File(source, "referenced.wav").apply { writeBytes(ByteArray(60)) }
            val orphanSource = File(source, "orphan-source.wav").apply { writeBytes(ByteArray(60)) }
            val orphanProxy = File(proxy, "orphan-proxy.wav").apply { writeBytes(ByteArray(60)) }
            val ingestPart = File(proxy, ".unused.part").apply { writeBytes(ByteArray(20)) }
            val recoverable = File(recording, "partial.recording.part.wav").apply { writeBytes(ByteArray(60)) }
            val empty = File(recording, "empty.recording.part.wav").apply { writeBytes(ByteArray(44)) }
            val orphanWaveform = File(waveform, "orphan.glwf").apply { writeBytes(ByteArray(8)) }

            val result = ProjectMediaIntegrityAuditor(root).cleanupSafeDerived(project)

            assertEquals(2, result.temporaryFilesRemoved)
            assertEquals(1, result.waveformFilesRemoved)
            assertTrue(referencedSource.isFile)
            assertTrue(orphanSource.isFile)
            assertTrue(orphanProxy.isFile)
            assertTrue(recoverable.isFile)
            assertFalse(ingestPart.exists())
            assertFalse(empty.exists())
            assertFalse(orphanWaveform.exists())
        } finally {
            root.deleteRecursively()
        }
    }

    private fun project(withMissingProxy: Boolean = true): GuitarProject = GuitarProject(
        id = "project-audit",
        name = "Audit",
        template = ProjectTemplate.BLANK,
        createdAtEpochMs = 1,
        updatedAtEpochMs = 1,
        tracks = listOf(AudioTrack(id = "track", name = "Track", order = 0)),
        clips = listOf(
            AudioClip(
                id = "clip",
                trackId = "track",
                name = "Clip",
                sourceUri = "managed://media/source/referenced.wav",
                managedSourcePath = "media/source/referenced.wav",
                managedEditProxyPath = if (withMissingProxy) "media/proxy/missing-proxy.wav" else null,
                startFrame = 0,
                sourceStartFrame = 0,
                lengthFrames = 10,
                sourceTotalFrames = 10,
                sourceSampleRateHz = 48_000,
                sourceChannelCount = 1,
                editingSampleRateHz = 48_000,
                editingTotalFrames = 10,
            )
        ),
    )
}
