package studio.guitarlab.core.project

import java.io.File
import java.nio.file.Files
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import org.junit.Test
import studio.guitarlab.core.model.GuitarProject
import studio.guitarlab.core.model.ProjectFactory
import studio.guitarlab.core.model.ProjectTemplate

class ProjectBackupCoordinatorTest {
    @Test fun repeatedBackupOfSameRevisionDoesNotCreateDuplicateVersion() = withFixture { root, repository, remote ->
        val project = project("P1", "Projeto", updated = 100)
        repository.save(project)
        val coordinator = coordinator(root, remote, repository, clock = SequenceClock(1000, 2000, 3000))

        val first = runSuspend { coordinator.backupAll(force = false, retentionPolicy = BackupRetentionPolicy(null, 3)) }
        assertEquals(1, first.committedCount)
        val second = runSuspend { coordinator.backupAll(force = false, retentionPolicy = BackupRetentionPolicy(null, 3)) }
        assertEquals(1, second.skippedCount)
        assertEquals(BackupRevisionIdentity.forProject(project), second.attempts.single().version?.revisionId)
        val third = runSuspend { coordinator.backupProject(project.id, force = false, retentionPolicy = BackupRetentionPolicy(null, 3)) }
        assertEquals(1, third.skippedCount)
        assertEquals(BackupRevisionIdentity.forProject(project), third.attempts.single().version?.revisionId)
        assertEquals(1, remote.list(project.id).size)
    }


    @Test fun eventuallyConsistentCatalogUsesTargetedRevisionLookupInsteadOfDuplicating() = withFixture { root, repository, remote ->
        val project = project("P1", "Projeto", updated = 100)
        repository.save(project)
        val payload = validBundle(root, project)
        val hidden = version("P1", 900, "already-remote", 100).copy(
            projectName = project.name,
            sizeBytes = payload.size.toLong(),
            sha256 = sha(payload),
            revisionId = BackupRevisionIdentity.forProject(project),
            formatVersion = 2,
        )
        remote.seed(hidden, payload)
        remote.hiddenFromCatalog += hidden.remoteId

        val report = runSuspend {
            coordinator(root, remote, repository, clock = SequenceClock(1000, 2000)).backupAll(
                force = false,
                retentionPolicy = BackupRetentionPolicy(null, 3),
            )
        }

        assertEquals(1, report.skippedCount)
        assertEquals(0, report.committedCount)
        assertEquals(hidden.remoteId, report.attempts.single().version?.remoteId)
        assertEquals(0, remote.commitCalls)
        assertTrue(remote.targetedLookupCalls > 0)
        assertEquals(1, remote.list(project.id).size)
    }

    @Test fun renameKeepsStableProjectIdentityAndCreatesOnlyANewRevision() = withFixture { root, repository, remote ->
        val original = project("stable-project-id", "Nome antigo", updated = 100)
        repository.save(original)
        val coordinator = coordinator(root, remote, repository, clock = SequenceClock(1000, 2000, 3000, 4000))

        val first = runSuspend { coordinator.backupAll(false, BackupRetentionPolicy(null, 3)) }
        assertEquals(1, first.committedCount)

        val renamed = original.copy(name = "Nome novo", updatedAtEpochMs = 200)
        repository.save(renamed)
        val second = runSuspend { coordinator.backupAll(false, BackupRetentionPolicy(null, 3)) }
        assertEquals(1, second.committedCount)

        val history = remote.list(original.id)
        assertEquals(2, history.size)
        assertTrue(history.all { it.projectId == original.id })
        assertEquals(2, history.map { it.revisionId }.toSet().size)
        assertTrue(history.any { it.projectName == "Nome novo" })
        assertEquals(original.id, repository.load(original.id)?.id)
    }

    @Test fun successfulRunCleansDuplicateCopiesOfSamePersistedRevision() = withFixture { root, repository, remote ->
        val project = project("P1", "Projeto", updated = 500)
        repository.save(project)
        remote.seedFromBundle(root, version("P1", 10, "duplicate-old", 500), project)
        remote.seedFromBundle(root, version("P1", 20, "duplicate-new", 500), project)

        val report = runSuspend {
            coordinator(root, remote, repository, clock = SequenceClock(30)).backupAll(
                force = false,
                retentionPolicy = BackupRetentionPolicy(maxAgeDays = null, maximumVersionsPerProject = 3),
            )
        }

        assertEquals(1, report.skippedCount)
        assertEquals(0, report.committedCount)
        assertEquals(1, report.deletedVersions)
        assertEquals(listOf("duplicate-old"), remote.deletedIds)
        assertEquals(1, remote.list("P1").size)
    }

    @Test fun failedRunNeverDeletesOldSafetyCopies() = withFixture { root, repository, remote ->
        val project = project("P1", "Projeto", updated = 999)
        repository.save(project)
        remote.seed(version("P1", created = 1, id = "old-1", updated = 1), validBundle(root, project.copy(updatedAtEpochMs = 1)))
        remote.seed(version("P1", created = 2, id = "old-2", updated = 2), validBundle(root, project.copy(updatedAtEpochMs = 2)))
        remote.failCommitsFor += "P1"
        val report = runSuspend {
            coordinator(root, remote, repository, clock = SequenceClock(20L * DAY)).backupAll(
                force = true,
                retentionPolicy = BackupRetentionPolicy(maxAgeDays = 7, maximumVersionsPerProject = 1),
            )
        }
        assertEquals(1, report.failedCount)
        assertTrue(report.retentionSuppressed)
        assertEquals(0, remote.deletedIds.size)
        assertEquals(2, remote.list("P1").size)
    }

    @Test fun partialFailureProtectsFailedProjectButCleansSuccessfulProject() = withFixture { root, repository, remote ->
        val a = project("A", "A", 900)
        val b = project("B", "B", 900)
        repository.save(a); repository.save(b)
        remote.seed(version("A", 1, "a-old1", 1), validBundle(root, a.copy(updatedAtEpochMs = 1)))
        remote.seed(version("A", 2, "a-old2", 2), validBundle(root, a.copy(updatedAtEpochMs = 2)))
        remote.seed(version("B", 1, "b-old1", 1), validBundle(root, b.copy(updatedAtEpochMs = 1)))
        remote.seed(version("B", 2, "b-old2", 2), validBundle(root, b.copy(updatedAtEpochMs = 2)))
        remote.failCommitsFor += "B"

        val report = runSuspend {
            coordinator(root, remote, repository, clock = SequenceClock(20L * DAY, 20L * DAY + 1)).backupAll(
                force = true,
                retentionPolicy = BackupRetentionPolicy(7, 1),
            )
        }
        assertEquals(1, report.committedCount)
        assertEquals(1, report.failedCount)
        assertFalse(report.retentionSuppressed)
        assertTrue(remote.deletedIds.any { it.startsWith("a-old") })
        assertFalse(remote.deletedIds.any { it.startsWith("b-old") })
    }


    @Test fun unchangedAutomaticRunStillAppliesRetention() = withFixture { root, repository, remote ->
        val project = project("P1", "Projeto", 500)
        repository.save(project)
        remote.seedFromBundle(root, version("P1", 1, "very-old", 1), project.copy(updatedAtEpochMs = 1))
        remote.seedFromBundle(root, version("P1", 20L * DAY, "current", 500), project)
        val report = runSuspend {
            coordinator(root, remote, repository, clock = SequenceClock(20L * DAY + DAY)).backupAll(
                force = false,
                retentionPolicy = BackupRetentionPolicy(7, 1),
            )
        }
        assertEquals(1, report.skippedCount)
        assertEquals(0, report.committedCount)
        assertFalse(report.retentionSuppressed)
        assertTrue("very-old" in remote.deletedIds)
        assertTrue("current" !in remote.deletedIds)
    }

    @Test fun manualSingleProjectCleanupNeverTouchesOtherProjects() = withFixture { root, repository, remote ->
        val a = project("A", "A", 100)
        val b = project("B", "B", 100)
        repository.save(a); repository.save(b)
        remote.seedFromBundle(root, version("A", 1, "a-old", 1), a.copy(updatedAtEpochMs = 1))
        remote.seedFromBundle(root, version("B", 1, "b-old", 1), b.copy(updatedAtEpochMs = 1))
        val report = runSuspend {
            coordinator(root, remote, repository, clock = SequenceClock(20L * DAY, 20L * DAY + 1)).backupProject(
                projectId = "A", force = false, retentionPolicy = BackupRetentionPolicy(7, 1),
            )
        }
        assertEquals(1, report.committedCount)
        assertTrue("a-old" in remote.deletedIds)
        assertTrue("b-old" !in remote.deletedIds)
    }

    @Test fun incompatibleRemoteCommitIsTreatedAsFailureAndSuppressesRetention() = withFixture { root, repository, remote ->
        val project = project("P1", "Projeto", 100)
        repository.save(project)
        remote.seedFromBundle(root, version("P1", 1, "old", 1), project.copy(updatedAtEpochMs = 1))
        remote.mismatchCommitFor += "P1"
        val report = runSuspend {
            coordinator(root, remote, repository, clock = SequenceClock(20L * DAY)).backupAll(true, BackupRetentionPolicy(7, 1))
        }
        assertEquals(1, report.failedCount)
        assertTrue(report.retentionSuppressed)
        assertTrue("old" !in remote.deletedIds)
    }

    @Test fun retentionDeleteFailureIsReportedWithoutInvalidatingNewBackup() = withFixture { root, repository, remote ->
        val project = project("P1", "Projeto", 100)
        repository.save(project)
        remote.seedFromBundle(root, version("P1", 1, "old", 1), project.copy(updatedAtEpochMs = 1))
        remote.failDeleteIds += "old"
        val report = runSuspend {
            coordinator(root, remote, repository, clock = SequenceClock(20L * DAY, 20L * DAY + 1)).backupAll(true, BackupRetentionPolicy(7, 1))
        }
        assertEquals(1, report.committedCount)
        assertEquals(1, report.retentionDeleteFailures)
        assertTrue(remote.list("P1").any { it.projectUpdatedAtEpochMs == 100L })
    }


    @Test fun cancellationPropagatesAndNeverFallsThroughToRetention() = withFixture { root, repository, remote ->
        val project = project("P1", "Projeto", 100)
        repository.save(project)
        remote.cancelCommitsFor += "P1"
        assertFailsWith<kotlinx.coroutines.CancellationException> {
            runSuspend {
                coordinator(root, remote, repository, clock = SequenceClock(20L * DAY)).backupAll(true, BackupRetentionPolicy(7, 1))
            }
        }
        assertTrue(remote.deletedIds.isEmpty())
    }

    @Test fun restoreRejectsCorruptRemoteBytesAndDoesNotPublishProject() = withFixture { root, repository, remote ->
        val source = project("SRC", "Fonte", 50)
        val bytes = validBundle(root, source)
        val descriptor = version("SRC", 100, "v1", 50).copy(sizeBytes = bytes.size.toLong(), sha256 = sha(bytes))
        remote.seed(descriptor, bytes)
        remote.corruptCopies += descriptor.remoteId
        val before = repository.list().size
        val attempt = runSuspend { coordinator(root, remote, repository, clock = SequenceClock(200)).restoreVersion(descriptor) }
        assertFalse(attempt.succeeded)
        assertTrue(attempt.error.orEmpty().contains("integridade") || attempt.error.orEmpty().contains("tamanho"))
        assertEquals(before, repository.list().size)
    }

    @Test fun restoreLatestAllUsesLatestCommittedVersionAndContinuesAfterOneFailure() = withFixture { root, repository, remote ->
        val aOld = project("A", "A antigo", 10)
        val aNew = project("A", "A novo", 20)
        val b = project("B", "B", 30)
        remote.seedFromBundle(root, version("A", 100, "a1", 10), aOld)
        remote.seedFromBundle(root, version("A", 200, "a2", 20), aNew)
        val badB = remote.seedFromBundle(root, version("B", 300, "b1", 30), b)
        remote.corruptCopies += badB.remoteId

        val attempts = runSuspend { coordinator(root, remote, repository, clock = SequenceClock(500, 501)).restoreLatestAll() }
        assertEquals(2, attempts.size)
        val restoredA = attempts.first { it.version.projectId == "A" }
        assertTrue(restoredA.succeeded)
        assertEquals("A novo", restoredA.restoredProject?.name)
        assertFalse(attempts.first { it.version.projectId == "B" }.succeeded)
        assertEquals(1, repository.list().size)
    }

    private fun coordinator(root: File, remote: FakeRemoteStore, repository: FileProjectRepository, clock: SequenceClock) =
        ProjectBackupCoordinator(root, remote, repository, nowEpochMs = clock::next)

    private fun project(id: String, name: String, updated: Long): GuitarProject = ProjectFactory(idGenerator = { id }, clock = { 1 }).create(name, ProjectTemplate.BLANK)
        .copy(id = id, updatedAtEpochMs = updated)

    private fun version(projectId: String, created: Long, id: String, updated: Long) = BackupVersionDescriptor(
        remoteId = id,
        projectId = projectId,
        projectName = projectId,
        projectUpdatedAtEpochMs = updated,
        backupCreatedAtEpochMs = created,
        sizeBytes = 0,
        sha256 = "",
    )

    private fun validBundle(root: File, project: GuitarProject): ByteArray = validBundleIsolated(project)

    private fun sha(bytes: ByteArray): String {
        val file = File.createTempFile("sha", ".bin")
        return try { file.writeBytes(bytes); BackupHashing.sha256(file) } finally { file.delete() }
    }

    private inline fun withFixture(block: (File, FileProjectRepository, FakeRemoteStore) -> Unit) {
        val root = Files.createTempDirectory("backup-coordinator").toFile()
        try { block(root, FileProjectRepository(root), FakeRemoteStore()) } finally { root.deleteRecursively() }
    }

    private class SequenceClock(vararg values: Long) {
        private val queue = ArrayDeque(values.toList())
        private var last = values.lastOrNull() ?: 1L
        fun next(): Long = if (queue.isEmpty()) ++last else queue.removeFirst().also { last = it }
    }

    private class FakeRemoteStore : ProjectBackupRemoteStore {
        private val versions = linkedMapOf<String, BackupVersionDescriptor>()
        private val bytes = linkedMapOf<String, ByteArray>()
        val failCommitsFor = mutableSetOf<String>()
        val cancelCommitsFor = mutableSetOf<String>()
        val mismatchCommitFor = mutableSetOf<String>()
        val corruptCopies = mutableSetOf<String>()
        val failDeleteIds = mutableSetOf<String>()
        val hiddenFromCatalog = mutableSetOf<String>()
        val deletedIds = mutableListOf<String>()
        var commitCalls = 0
        var targetedLookupCalls = 0
        private var sequence = 0

        override suspend fun listCommittedVersions(projectId: String?): List<BackupVersionDescriptor> =
            list(projectId).filterNot { it.remoteId in hiddenFromCatalog }

        override suspend fun findCommittedVersion(projectId: String, revisionId: String): BackupVersionDescriptor? {
            targetedLookupCalls++
            return list(projectId).firstOrNull { it.revisionId == revisionId }
        }

        fun list(projectId: String?): List<BackupVersionDescriptor> = versions.values.filter { projectId == null || it.projectId == projectId }

        fun seed(descriptor: BackupVersionDescriptor, payload: ByteArray) {
            val fixed = descriptor.copy(sizeBytes = payload.size.toLong(), sha256 = hash(payload))
            versions[fixed.remoteId] = fixed
            bytes[fixed.remoteId] = payload
        }

        fun seedFromBundle(root: File, descriptor: BackupVersionDescriptor, project: GuitarProject): BackupVersionDescriptor {
            val payload = validBundleStatic(root, project)
            val fixed = descriptor.copy(projectName = project.name, sizeBytes = payload.size.toLong(), sha256 = hash(payload))
            seed(fixed, payload)
            return versions.getValue(fixed.remoteId)
        }

        override suspend fun commit(request: BackupCommitRequest): BackupVersionDescriptor {
            commitCalls++
            if (request.projectId in cancelCommitsFor) throw kotlinx.coroutines.CancellationException("simulated cancellation")
            if (request.projectId in failCommitsFor) error("simulated commit failure")
            val payload = request.packageFile.readBytes()
            assertEquals(request.sizeBytes, payload.size.toLong())
            assertEquals(request.sha256, hash(payload))
            val descriptor = BackupVersionDescriptor(
                remoteId = "commit-${++sequence}", projectId = request.projectId, projectName = request.projectName,
                projectUpdatedAtEpochMs = request.projectUpdatedAtEpochMs, backupCreatedAtEpochMs = request.backupCreatedAtEpochMs,
                sizeBytes = request.sizeBytes, sha256 = request.sha256, revisionId = request.revisionId, formatVersion = 2,
            )
            versions[descriptor.remoteId] = descriptor
            bytes[descriptor.remoteId] = payload
            return if (request.projectId in mismatchCommitFor) descriptor.copy(sizeBytes = descriptor.sizeBytes + 1) else descriptor
        }

        override suspend fun copyPackage(version: BackupVersionDescriptor, destination: File) {
            val payload = bytes.getValue(version.remoteId).copyOf()
            if (version.remoteId in corruptCopies && payload.isNotEmpty()) payload[0] = (payload[0].toInt() xor 0x7f).toByte()
            destination.writeBytes(payload)
        }

        override suspend fun deleteVersion(version: BackupVersionDescriptor): Boolean {
            if (version.remoteId in failDeleteIds) return false
            deletedIds += version.remoteId
            bytes.remove(version.remoteId)
            return versions.remove(version.remoteId) != null
        }

        override suspend fun cleanupIncomplete(olderThanEpochMs: Long): Int = 0

        private fun hash(payload: ByteArray): String {
            val temp = File.createTempFile("fake-hash", ".bin")
            return try { temp.writeBytes(payload); BackupHashing.sha256(temp) } finally { temp.delete() }
        }

        companion object {
            private fun validBundleStatic(root: File, project: GuitarProject): ByteArray = validBundleIsolated(project)
        }
    }

    private companion object { const val DAY = 86_400_000L }
}

private fun validBundleIsolated(project: GuitarProject): ByteArray {
    val sourceRoot = Files.createTempDirectory("bundle-source").toFile()
    val file = File.createTempFile("bundle", ".guitarlab")
    return try {
        FileProjectRepository(sourceRoot).save(project)
        file.outputStream().use { ProjectBundleWriter().write(project, ProjectManagedMediaStore(sourceRoot).projectDirectoryForExport(project.id), it) }
        file.readBytes()
    } finally {
        file.delete()
        sourceRoot.deleteRecursively()
    }
}

private fun <T> runSuspend(block: suspend () -> T): T = kotlinx.coroutines.runBlocking { block() }
