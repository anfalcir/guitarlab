package studio.guitarlab.core.project

import java.nio.file.Files
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import org.junit.Test
import studio.guitarlab.core.model.ProjectFactory
import studio.guitarlab.core.model.ProjectTemplate

class BackupDomainTest {
    @Test fun retentionCapsHistoryEvenWhenAgeLimitIsDisabled() {
        val versions = (1..5).map { index -> version("p", created = index.toLong(), remoteId = "v$index") }
        val deleted = BackupRetentionPlanner.deletions(
            versions,
            BackupRetentionPolicy(maxAgeDays = null, maximumVersionsPerProject = 3),
            nowEpochMs = 10_000,
        )
        assertEquals(setOf("v1", "v2"), deleted.mapTo(mutableSetOf()) { it.remoteId })
    }

    @Test fun retentionCollapsesDuplicateCopiesOfTheSameProjectRevision() {
        val versions = listOf(
            version("p", created = 500, remoteId = "current", updated = 300),
            version("p", created = 490, remoteId = "duplicate-current", updated = 300),
            version("p", created = 400, remoteId = "previous", updated = 200),
            version("p", created = 300, remoteId = "older", updated = 100),
        )
        val deleted = BackupRetentionPlanner.deletions(
            versions,
            BackupRetentionPolicy(maxAgeDays = null, maximumVersionsPerProject = 3),
            nowEpochMs = 1_000,
        )
        assertEquals(listOf("duplicate-current"), deleted.map { it.remoteId })
    }


    @Test fun retentionDoesNotCollapseDifferentV2ContentThatSharesTheSameEditTimestamp() {
        val first = version("p", created = 20, remoteId = "first", updated = 300).copy(
            sha256 = "1".repeat(64),
            revisionId = "r_300_${"a".repeat(24)}",
            formatVersion = 2,
        )
        val second = version("p", created = 10, remoteId = "second", updated = 300).copy(
            sha256 = "2".repeat(64),
            revisionId = "r_300_${"b".repeat(24)}",
            formatVersion = 2,
        )
        val deleted = BackupRetentionPlanner.deletions(
            listOf(first, second),
            BackupRetentionPolicy(maxAgeDays = null, maximumVersionsPerProject = 3),
            nowEpochMs = 1_000,
        )
        assertTrue(deleted.isEmpty())
    }

    @Test fun retentionAlwaysKeepsNewestVersionEvenWhenEverythingIsExpired() {
        val day = 86_400_000L
        val now = 1_000L * day
        val versions = listOf(
            version("p", created = now - 100 * day, remoteId = "newest"),
            version("p", created = now - 101 * day, remoteId = "older-1"),
            version("p", created = now - 102 * day, remoteId = "older-2"),
        )
        val deleted = BackupRetentionPlanner.deletions(
            versions,
            BackupRetentionPolicy(maxAgeDays = 90, maximumVersionsPerProject = 10),
            now,
        )
        assertEquals(setOf("older-1", "older-2"), deleted.mapTo(mutableSetOf()) { it.remoteId })
        assertTrue(deleted.none { it.remoteId == "newest" })
    }

    @Test fun retentionIsIndependentPerProject() {
        val versions = listOf(
            version("a", 4, "a4"), version("a", 3, "a3"), version("a", 2, "a2"),
            version("b", 6, "b6"), version("b", 5, "b5"), version("b", 1, "b1"),
        )
        val deleted = BackupRetentionPlanner.deletions(
            versions,
            BackupRetentionPolicy(maxAgeDays = null, maximumVersionsPerProject = 2),
            nowEpochMs = 10_000,
        )
        assertEquals(setOf("a2", "b1"), deleted.mapTo(mutableSetOf()) { it.remoteId })
    }

    @Test fun incrementalSkipsOnlyExactPersistedRevision() {
        val project = ProjectFactory(clock = { 100 }).create("Teste", ProjectTemplate.BLANK).copy(updatedAtEpochMs = 222)
        assertFalse(BackupIncrementalPolicy.needsBackup(project, listOf(version(project.id, 1, "same", updated = 222))))
        assertTrue(BackupIncrementalPolicy.needsBackup(project, listOf(version(project.id, 1, "old", updated = 221))))
    }


    @Test fun revisionIdentityIsStableForTheSameProjectStateAndChangesForSameMillisecondEdits() {
        val base = ProjectFactory(idGenerator = { "stable-project" }, clock = { 1234L })
            .create("Nome A", ProjectTemplate.BLANK)
            .copy(updatedAtEpochMs = 9_876_543_210L)
        val same = base.copy()
        val renamedSameMillisecond = base.copy(name = "Nome B")

        val first = BackupRevisionIdentity.forProject(base)
        assertEquals(first, BackupRevisionIdentity.forProject(same))
        assertTrue(first.startsWith("r_${base.updatedAtEpochMs}_"))
        assertNotEquals(first, BackupRevisionIdentity.forProject(renamedSameMillisecond))
        assertEquals(base.id, renamedSameMillisecond.id)
    }

    @Test fun incrementalIdentityIgnoresDisplayNameButNotAV2RevisionChange() {
        val base = ProjectFactory(idGenerator = { "stable-project" }, clock = { 100L })
            .create("Antes", ProjectTemplate.BLANK)
            .copy(updatedAtEpochMs = 222L)
        val renamedSameMillisecond = base.copy(name = "Depois")
        val descriptor = version(base.id, created = 1L, remoteId = "v2", updated = base.updatedAtEpochMs).copy(
            revisionId = BackupRevisionIdentity.forProject(base),
            formatVersion = 2,
        )

        assertFalse(BackupIncrementalPolicy.needsBackup(base, listOf(descriptor)))
        assertTrue(BackupIncrementalPolicy.needsBackup(renamedSameMillisecond, listOf(descriptor)))
        assertEquals(base.id, renamedSameMillisecond.id)
    }

    @Test fun v1CommitMetadataRemainsReadableAfterRevisionIdentityUpgrade() {
        val temp = Files.createTempFile("backup-contract-v1", ".bin").toFile()
        try {
            temp.writeText("legacy-payload")
            val request = BackupCommitRequest(
                projectId = "legacy-project",
                projectName = "Legacy",
                projectUpdatedAtEpochMs = 123,
                backupCreatedAtEpochMs = 456,
                sizeBytes = temp.length(),
                sha256 = BackupHashing.sha256(temp),
                packageFile = temp,
            )
            val metadata = BackupCommitContract.metadata(request).apply {
                setProperty("version", "1")
                remove("revisionId")
            }
            val commit = BackupCommitContract.commitMarker(request).apply {
                setProperty("version", "1")
                remove("revisionId")
            }

            val parsed = BackupCommitContract.parseCommittedOrNull(
                remoteId = "legacy-remote",
                metadata = metadata,
                commit = commit,
                observedPackageSize = temp.length(),
            )
            assertEquals("legacy-project", parsed?.projectId)
            assertEquals(1, parsed?.formatVersion)
            assertEquals(BackupRevisionIdentity.legacy("legacy-project", 123), parsed?.revisionId)
        } finally { temp.delete() }
    }

    @Test fun sha256IsStable() {
        val file = Files.createTempFile("backup-hash", ".bin").toFile()
        try {
            file.writeText("GuitarLab")
            assertEquals("8bdcc3042c8d8ce06ea1613240a60fb33247b7c600238e05c5371dc805e4065d", BackupHashing.sha256(file))
        } finally { file.delete() }
    }

    @Test fun committedMarkerMustMatchMetadataExactly() {
        val temp = Files.createTempFile("backup-contract", ".bin").toFile()
        try {
            temp.writeText("payload")
            val request = BackupCommitRequest(
                projectId = "p1", projectName = "Projeto", projectUpdatedAtEpochMs = 10,
                backupCreatedAtEpochMs = 20, sizeBytes = temp.length(), sha256 = BackupHashing.sha256(temp), packageFile = temp,
            )
            val metadata = BackupCommitContract.metadata(request)
            val commit = BackupCommitContract.commitMarker(request)
            val valid = BackupCommitContract.parseCommittedOrNull("remote", metadata, commit, observedPackageSize = temp.length())
            assertEquals("p1", valid?.projectId)

            val wrongHash = java.util.Properties().apply { putAll(commit); setProperty("sha256", "0".repeat(64)) }
            assertEquals(null, BackupCommitContract.parseCommittedOrNull("remote", metadata, wrongHash, observedPackageSize = temp.length()))

            val partial = java.util.Properties().apply { setProperty("format", BackupCommitContract.COMMIT_FORMAT) }
            assertEquals(null, BackupCommitContract.parseCommittedOrNull("remote", metadata, partial, observedPackageSize = temp.length()))

            assertEquals(null, BackupCommitContract.parseCommittedOrNull("remote", metadata, commit, observedPackageSize = temp.length() + 1))
            assertEquals(null, BackupCommitContract.parseCommittedOrNull("remote", metadata, commit, expectedProjectId = "different"))
        } finally { temp.delete() }
    }

    private fun version(project: String, created: Long, remoteId: String, updated: Long = created) = BackupVersionDescriptor(
        remoteId = remoteId,
        projectId = project,
        projectName = project,
        projectUpdatedAtEpochMs = updated,
        backupCreatedAtEpochMs = created,
        sizeBytes = 1,
        sha256 = "00",
    )
}
