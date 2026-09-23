package studio.guitarlab.core.project

import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import org.junit.Test

class UnifiedOperationDomainTest {
    @Test fun activityPrioritizesActiveWorkThenRecency() {
        val records = listOf(
            record("old", UnifiedOperationState.SUCCEEDED, 30),
            record("running", UnifiedOperationState.RUNNING, 10),
            record("new", UnifiedOperationState.FAILED, 40),
            record("queued", UnifiedOperationState.QUEUED, 20),
            record("running", UnifiedOperationState.QUEUED, 5),
        )
        assertEquals(listOf("queued", "running", "new", "old"), UnifiedActivityPolicy.ordered(records).map { it.operationId })
        assertEquals("queued", UnifiedActivityPolicy.compactActive(records)?.operationId)
    }

    @Test fun clearHistoryRemovesEveryTerminalOperationAndPreservesAllActiveWork() {
        val records = listOf(
            record("queued", UnifiedOperationState.QUEUED, 1),
            record("running", UnifiedOperationState.RUNNING, 2),
            record("retrying", UnifiedOperationState.RETRYING, 3),
            record("failed", UnifiedOperationState.FAILED, 4),
            record("done", UnifiedOperationState.SUCCEEDED, 5),
            record("cancelled", UnifiedOperationState.CANCELLED, 6),
        )
        assertEquals(
            listOf("queued", "running", "retrying"),
            UnifiedActivityPolicy.clearHistory(records).map { it.operationId },
        )
    }

    @Test fun missingProjectActiveHistoryIsTerminalizedButFailureEvidenceIsPreserved() {
        val records = listOf(
            record("orphan", UnifiedOperationState.RUNNING, 1),
            record("failure", UnifiedOperationState.FAILED, 2),
            UnifiedOperationRecord("global", null, UnifiedOperationKind.BACKUP, UnifiedOperationState.RUNNING, null, 3, "global"),
        )
        val reconciled = UnifiedActivityPolicy.terminalizeMissingProjects(records, emptySet(), 10)
        assertEquals(UnifiedOperationState.CANCELLED, reconciled.single { it.operationId == "orphan" }.state)
        assertEquals("PROJECT_REMOVED", reconciled.single { it.operationId == "orphan" }.technicalDetail)
        assertEquals(UnifiedOperationState.FAILED, reconciled.single { it.operationId == "failure" }.state)
        assertEquals(UnifiedOperationState.RUNNING, reconciled.single { it.operationId == "global" }.state)
    }

    @Test fun deletingProjectTerminalizesOnlyItsActiveOperations() {
        val records = listOf(
            record("target-running", UnifiedOperationState.RETRYING, 1),
            UnifiedOperationRecord("other", "other-project", UnifiedOperationKind.SEPARATION, UnifiedOperationState.RUNNING, null, 2, "other"),
        )
        val result = UnifiedActivityPolicy.terminalizeProject(records, "project", 20)
        assertEquals(UnifiedOperationState.CANCELLED, result.single { it.operationId == "target-running" }.state)
        assertEquals(UnifiedOperationState.RUNNING, result.single { it.operationId == "other" }.state)
    }

    @Test fun invalidProgressFailsClosed() {
        assertFailsWith<IllegalArgumentException> { record("bad", UnifiedOperationState.RUNNING, 1, 101) }
    }

    @Test fun syncStateNeverClaimsSuccessFromWorkerSuccessAlone() {
        val completed = record("backup", UnifiedOperationState.SUCCEEDED, 1)
        assertEquals(ProjectSyncState.PENDING, ProjectSyncStatePolicy.derive(true, "local", "older", backupOperation = completed))
        assertEquals(ProjectSyncState.SYNCED, ProjectSyncStatePolicy.derive(true, "local", "local", backupOperation = completed))
    }

    @Test fun conflictAndActiveBackupHaveExplicitPriority() {
        val active = record("backup", UnifiedOperationState.RUNNING, 1)
        assertEquals(ProjectSyncState.NOT_CONNECTED, ProjectSyncStatePolicy.derive(false, "local", null, DriveReconciliation.CONFLICT, active))
        assertEquals(ProjectSyncState.CONFLICT, ProjectSyncStatePolicy.derive(true, "local", "base", DriveReconciliation.CONFLICT, active))
        assertEquals(ProjectSyncState.SYNCING, ProjectSyncStatePolicy.derive(true, "local", "base", backupOperation = active))
    }

    private fun record(
        id: String,
        state: UnifiedOperationState,
        updated: Long,
        progress: Int? = null,
    ) = UnifiedOperationRecord(id, "project", UnifiedOperationKind.BACKUP, state, progress, updated, id)
}
