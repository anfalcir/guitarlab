package studio.guitarlab.app.ui

import kotlin.test.assertEquals
import org.junit.Test
import studio.guitarlab.core.model.ProjectFactory
import studio.guitarlab.core.model.ProjectTemplate
import studio.guitarlab.core.project.BackupRevisionIdentity
import studio.guitarlab.core.project.UnifiedOperationKind
import studio.guitarlab.core.project.UnifiedOperationRecord
import studio.guitarlab.core.project.UnifiedOperationState

class ProjectSyncLabelTest {
    private val project = ProjectFactory(idGenerator = { "project-sync" }, clock = { 1L })
        .create("Projeto", ProjectTemplate.BLANK)
        .copy(updatedAtEpochMs = 100L)

    @Test fun workerSuccessCannotClaimSynchronizedWithoutConfirmedRevision() {
        val completed = record("backup", project.id, UnifiedOperationState.SUCCEEDED, 10)
        assertEquals("Backup pendente", projectSyncLabel(project, true, "older-revision", listOf(completed)))
        assertEquals(
            "Sincronizado",
            projectSyncLabel(project, true, BackupRevisionIdentity.forProject(project), listOf(completed)),
        )
    }

    @Test fun activeGlobalBackupIsVisibleWithoutInventingConfirmation() {
        val running = record("automatic-backup", null, UnifiedOperationState.RUNNING, 20)
        assertEquals("Sincronizando…", projectSyncLabel(project, true, null, listOf(running)))
    }

    @Test fun disconnectedDriveAlwaysWinsPresentation() {
        val running = record("automatic-backup", null, UnifiedOperationState.RUNNING, 20)
        assertEquals("Nuvem desconectada · somente local", projectSyncLabel(project, false, null, listOf(running)))
    }

    private fun record(id: String, projectId: String?, state: UnifiedOperationState, updated: Long) =
        UnifiedOperationRecord(id, projectId, UnifiedOperationKind.BACKUP, state, null, updated, id)
}
