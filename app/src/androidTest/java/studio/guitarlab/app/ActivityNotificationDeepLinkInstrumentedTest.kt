package studio.guitarlab.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import studio.guitarlab.app.activity.AppNotificationDeepLink
import studio.guitarlab.app.activity.UnifiedActivityStore
import studio.guitarlab.app.backup.ConfirmedRevisionStore
import studio.guitarlab.app.ui.AppScreen
import studio.guitarlab.core.project.BackupRunReport
import studio.guitarlab.core.project.BackupVersionDescriptor
import studio.guitarlab.core.project.ProjectBackupAttempt
import studio.guitarlab.core.project.UnifiedOperationKind
import studio.guitarlab.core.project.UnifiedOperationState

@RunWith(AndroidJUnit4::class)
class ActivityNotificationDeepLinkInstrumentedTest {
    @get:Rule val composeRule = createEmptyComposeRule()

    @Test fun notificationPendingIntentOpensOwningActivityItem() {
        val operationId = "notification-deep-link-test"
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val store = UnifiedActivityStore(context)
        store.record(
            operationId = operationId,
            projectId = "project-notification",
            kind = UnifiedOperationKind.BACKUP,
            state = UnifiedOperationState.SUCCEEDED,
            progressPercent = 100,
            summary = "Backup concluído",
        )
        try {
            val intent = AppNotificationDeepLink.intent(context, AppScreen.Activity(operationId))
            ActivityScenario.launch<MainActivity>(intent).use {
                composeRule.onNodeWithTag("activity-record-$operationId").assertIsDisplayed()
                composeRule.onNodeWithText("Concluída").assertIsDisplayed()
                composeRule.onNodeWithText("project-notification", substring = true).assertDoesNotExist()
                composeRule.captureCohesionScreenshot("activity-completed")
            }
        } finally {
            store.remove(operationId)
        }
    }

    @Test fun confirmedRevisionStoreIgnoresUnconfirmedFailure() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val store = ConfirmedRevisionStore(context)
        val projectId = "confirmed-store-test"
        store.clearProject(projectId)
        val confirmed = descriptor(projectId, "r_1_" + "a".repeat(24))
        store.record(report(ProjectBackupAttempt.Status.COMMITTED, confirmed))
        assertEquals(confirmed.revisionId, store.confirmedRevision(projectId))

        val failed = descriptor(projectId, "r_2_" + "b".repeat(24))
        store.record(report(ProjectBackupAttempt.Status.FAILED, failed))
        assertEquals(confirmed.revisionId, store.confirmedRevision(projectId))
        store.clearProject(projectId)
    }

    private fun descriptor(projectId: String, revisionId: String) = BackupVersionDescriptor(
        remoteId = revisionId,
        projectId = projectId,
        projectName = "Projeto",
        projectUpdatedAtEpochMs = 1L,
        backupCreatedAtEpochMs = 2L,
        sizeBytes = 1L,
        sha256 = "a".repeat(64),
        revisionId = revisionId,
        formatVersion = 2,
    )

    private fun report(status: ProjectBackupAttempt.Status, version: BackupVersionDescriptor) = BackupRunReport(
        attempts = listOf(ProjectBackupAttempt(version.projectId, version.projectName, status, version = version)),
        deletedVersions = 0,
        retentionDeleteFailures = 0,
        cleanedIncompleteVersions = 0,
        retentionSuppressed = false,
    )
}
