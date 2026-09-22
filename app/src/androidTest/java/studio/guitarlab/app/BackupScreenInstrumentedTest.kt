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
import studio.guitarlab.core.project.DriveReconciliation

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
                    onBack = {}, onConnectDrive = {}, onDisconnectDrive = {}, onRefresh = {},
                    onAutomaticEnabled = {}, onCadence = {}, onUnmeteredOnly = {}, onChargingOnly = {},
                    onRetentionDays = {}, onMaximumVersions = {},
                    onBackupAll = { allCalls++ }, onBackupProject = { if (it == project.id) projectCalls++ },
                    onKeepLocal = { _, _ -> }, onUseCloud = {}, onRestoreVersion = {}, onRestoreAll = {},
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
                    onBack = {}, onConnectDrive = {}, onDisconnectDrive = {}, onRefresh = {},
                    onAutomaticEnabled = {}, onCadence = {}, onUnmeteredOnly = {}, onChargingOnly = {},
                    onRetentionDays = {}, onMaximumVersions = {}, onBackupAll = {}, onBackupProject = {},
                    onKeepLocal = { _, _ -> }, onUseCloud = {}, onRestoreVersion = {}, onRestoreAll = {},
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
                    onBack = {}, onConnectDrive = {}, onDisconnectDrive = {}, onRefresh = {},
                    onAutomaticEnabled = {}, onCadence = {}, onUnmeteredOnly = {}, onChargingOnly = {},
                    onRetentionDays = {}, onMaximumVersions = {}, onBackupAll = {}, onBackupProject = {},
                    onKeepLocal = { _, _ -> }, onUseCloud = {},
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
                    onBack = {}, onConnectDrive = {}, onDisconnectDrive = {}, onRefresh = {},
                    onAutomaticEnabled = {}, onCadence = {}, onUnmeteredOnly = {}, onChargingOnly = {},
                    onRetentionDays = {}, onMaximumVersions = {}, onBackupAll = {}, onBackupProject = {},
                    onKeepLocal = { _, _ -> }, onUseCloud = {},
                    onRestoreVersion = {}, onRestoreAll = { calls++ },
                )
            }
        }
        scrollToAndClick("restore-all")
        compose.onNodeWithTag("confirm-restore-all").assertIsDisplayed().performClick()
        assertEquals(1, calls)
    }


    @Test fun conflictOffersExplicitSafeResolutionActions() {
        val project = ProjectFactory(
            idGenerator = { "p" },
            clock = { 10 },
        ).create("Projeto", ProjectTemplate.BLANK)
        val remote = version("remote-conflict").copy(projectId = project.id)
        var keptLocal: BackupVersionDescriptor? = null
        var usedCloud: BackupVersionDescriptor? = null
        var importedCopy: BackupVersionDescriptor? = null

        compose.setContent {
            GuitarLabTheme {
                BackupScreenContent(
                    state = configuredState(
                        localProjects = listOf(project),
                        versions = listOf(remote),
                        reconciliations = mapOf(project.id to DriveReconciliation.CONFLICT),
                        remoteTips = mapOf(project.id to listOf(remote)),
                    ),
                    projectId = project.id,
                    onBack = {},
                    onConnectDrive = {},
                    onDisconnectDrive = {},
                    onRefresh = {},
                    onAutomaticEnabled = {},
                    onCadence = {},
                    onUnmeteredOnly = {},
                    onChargingOnly = {},
                    onRetentionDays = {},
                    onMaximumVersions = {},
                    onBackupAll = {},
                    onBackupProject = {},
                    onKeepLocal = { id, version ->
                        if (id == project.id) keptLocal = version
                    },
                    onUseCloud = { usedCloud = it },
                    onRestoreVersion = { importedCopy = it },
                    onRestoreAll = {},
                )
            }
        }

        scrollToAndClick("conflict-keep-local")
        compose.onNodeWithTag("confirm-keep-local").assertIsDisplayed().performClick()
        assertEquals(remote, keptLocal)

        scrollToAndClick("conflict-use-cloud-remote-conflict")
        compose.onNodeWithTag("confirm-use-cloud").assertIsDisplayed().performClick()
        assertEquals(remote, usedCloud)

        scrollToAndClick("conflict-import-copy-remote-conflict")
        compose.onNodeWithTag("confirm-restore-version").assertIsDisplayed().performClick()
        assertEquals(remote, importedCopy)
    }

    @Test fun ambiguousRemoteHeadsCannotSilentlyReplaceLocalProject() {
        val project = ProjectFactory(
            idGenerator = { "p" },
            clock = { 10 },
        ).create("Projeto", ProjectTemplate.BLANK)
        val first = version("remote-a").copy(projectId = project.id)
        val second = version("remote-b").copy(projectId = project.id)

        compose.setContent {
            GuitarLabTheme {
                BackupScreenContent(
                    state = configuredState(
                        localProjects = listOf(project),
                        versions = listOf(first, second),
                        reconciliations = mapOf(project.id to DriveReconciliation.CONFLICT),
                        remoteTips = mapOf(project.id to listOf(first, second)),
                    ),
                    projectId = project.id,
                    onBack = {},
                    onConnectDrive = {},
                    onDisconnectDrive = {},
                    onRefresh = {},
                    onAutomaticEnabled = {},
                    onCadence = {},
                    onUnmeteredOnly = {},
                    onChargingOnly = {},
                    onRetentionDays = {},
                    onMaximumVersions = {},
                    onBackupAll = {},
                    onBackupProject = {},
                    onKeepLocal = { _, _ -> error("must stay unavailable") },
                    onUseCloud = { error("must stay unavailable") },
                    onRestoreVersion = {},
                    onRestoreAll = {},
                )
            }
        }

        compose.onNode(hasScrollAction()).performScrollToNode(
            hasTestTag("conflict-import-copy-remote-a"),
        )
        compose.onNodeWithTag("conflict-import-copy-remote-a").assertIsDisplayed()
        compose.onNodeWithTag("conflict-import-copy-remote-b").assertIsDisplayed()
        compose.onNodeWithTag("conflict-keep-local").assertDoesNotExist()
        compose.onNodeWithTag("conflict-use-cloud-remote-a").assertDoesNotExist()
        compose.onNodeWithTag("conflict-use-cloud-remote-b").assertDoesNotExist()
    }

    private fun scrollToAndClick(tag: String) {
        compose.onNode(hasScrollAction()).performScrollToNode(hasTestTag(tag))
        compose.onNodeWithTag(tag).assertIsDisplayed().performClick()
    }

    private fun configuredState(
        localProjects: List<studio.guitarlab.core.model.GuitarProject> = emptyList(),
        versions: List<BackupVersionDescriptor> = emptyList(),
        reconciliations: Map<String, DriveReconciliation> = emptyMap(),
        remoteTips: Map<String, List<BackupVersionDescriptor>> = emptyMap(),
    ) = BackupUiState(
        loading = false,
        settings = BackupSettingsSnapshot(driveConnected = true, driveAccountLabel = "conta@example.com"),
        localProjects = localProjects,
        versions = versions,
        reconciliations = reconciliations,
        remoteTips = remoteTips,
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
