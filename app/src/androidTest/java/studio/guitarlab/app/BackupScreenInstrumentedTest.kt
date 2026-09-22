package studio.guitarlab.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import studio.guitarlab.app.backup.BackupScreenContent
import studio.guitarlab.app.backup.BackupSettingsSnapshot
import studio.guitarlab.app.backup.BackupUiState
import studio.guitarlab.app.ui.theme.GuitarLabTheme
import studio.guitarlab.core.model.ProjectFactory
import studio.guitarlab.core.model.ProjectTemplate
import studio.guitarlab.core.project.BackupVersionDescriptor

class BackupScreenInstrumentedTest {
    @get:Rule val compose = createComposeRule()

    @Test fun manualProjectAndTotalBackupAreDistinctActions() {
        val project = ProjectFactory(idGenerator = { "p1" }, clock = { 10 }).create("Projeto", ProjectTemplate.BLANK)
        var projectCalls = 0
        var allCalls = 0
        compose.setContent {
            GuitarLabTheme {
                BackupScreenContent(
                    state = configuredState(localProjects = listOf(project)),
                    projectId = project.id,
                    onBack = {}, onConnectDrive = {}, onDisconnectDrive = {}, onMigrateLegacySaf = {}, onRefresh = {},
                    onAutomaticEnabled = {}, onCadence = {}, onUnmeteredOnly = {}, onChargingOnly = {},
                    onRetentionDays = {}, onMaximumVersions = {},
                    onBackupAll = { allCalls++ }, onBackupProject = { if (it == project.id) projectCalls++ },
                    onRestoreVersion = {}, onRestoreAll = {},
                )
            }
        }
        scrollToAndClick("backup-current-project")
        scrollToAndClick("backup-all-now")
        assertEquals(1, projectCalls)
        assertEquals(1, allCalls)
    }


    @Test fun backupScreenUsesEndUserCopyAndMaximumVersionSemantics() {
        compose.setContent {
            GuitarLabTheme {
                BackupScreenContent(
                    state = configuredState(versions = listOf(version("remote-copy"))), projectId = null,
                    onBack = {}, onConnectDrive = {}, onDisconnectDrive = {}, onMigrateLegacySaf = {}, onRefresh = {},
                    onAutomaticEnabled = {}, onCadence = {}, onUnmeteredOnly = {}, onChargingOnly = {},
                    onRetentionDays = {}, onMaximumVersions = {}, onBackupAll = {}, onBackupProject = {},
                    onRestoreVersion = {}, onRestoreAll = {},
                )
            }
        }

        compose.onNodeWithText("Storage Access Framework", substring = true).assertDoesNotExist()
        compose.onNodeWithText("COMMITTED", substring = true).assertDoesNotExist()
        compose.onNodeWithText("SHA-256", substring = true).assertDoesNotExist()
        compose.onNode(hasScrollAction()).performScrollToNode(hasText("Máximo de versões por projeto"))
        compose.onNodeWithText("Máximo de versões por projeto").assertIsDisplayed()
    }

    @Test fun singleVersionRestoreRequiresExplicitConfirmation() {
        val version = version("remote-1")
        var restored: BackupVersionDescriptor? = null
        compose.setContent {
            GuitarLabTheme {
                BackupScreenContent(
                    state = configuredState(versions = listOf(version)), projectId = null,
                    onBack = {}, onConnectDrive = {}, onDisconnectDrive = {}, onMigrateLegacySaf = {}, onRefresh = {},
                    onAutomaticEnabled = {}, onCadence = {}, onUnmeteredOnly = {}, onChargingOnly = {},
                    onRetentionDays = {}, onMaximumVersions = {}, onBackupAll = {}, onBackupProject = {},
                    onRestoreVersion = { restored = it }, onRestoreAll = {},
                )
            }
        }
        scrollToAndClick("restore-version-remote-1")
        compose.onNodeWithTag("confirm-restore-version").assertIsDisplayed().performClick()
        assertEquals(version, restored)
    }

    @Test fun restoreAllRequiresExplicitConfirmation() {
        var calls = 0
        compose.setContent {
            GuitarLabTheme {
                BackupScreenContent(
                    state = configuredState(versions = listOf(version("remote-2"))), projectId = null,
                    onBack = {}, onConnectDrive = {}, onDisconnectDrive = {}, onMigrateLegacySaf = {}, onRefresh = {},
                    onAutomaticEnabled = {}, onCadence = {}, onUnmeteredOnly = {}, onChargingOnly = {},
                    onRetentionDays = {}, onMaximumVersions = {}, onBackupAll = {}, onBackupProject = {},
                    onRestoreVersion = {}, onRestoreAll = { calls++ },
                )
            }
        }
        scrollToAndClick("restore-all")
        compose.onNodeWithTag("confirm-restore-all").assertIsDisplayed().performClick()
        assertEquals(1, calls)
    }

    private fun scrollToAndClick(tag: String) {
        compose.onNode(hasScrollAction()).performScrollToNode(hasTestTag(tag))
        compose.onNodeWithTag(tag).assertIsDisplayed().performClick()
    }

    private fun configuredState(
        localProjects: List<studio.guitarlab.core.model.GuitarProject> = emptyList(),
        versions: List<BackupVersionDescriptor> = emptyList(),
    ) = BackupUiState(
        loading = false,
        settings = BackupSettingsSnapshot(driveConnected = true, driveAccountLabel = "conta@example.com"),
        localProjects = localProjects,
        versions = versions,
    )

    private fun version(id: String) = BackupVersionDescriptor(
        remoteId = id,
        projectId = "p",
        projectName = "Projeto",
        projectUpdatedAtEpochMs = 10,
        backupCreatedAtEpochMs = 20,
        sizeBytes = 1024,
        sha256 = "a".repeat(64),
    )
}
