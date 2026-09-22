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
