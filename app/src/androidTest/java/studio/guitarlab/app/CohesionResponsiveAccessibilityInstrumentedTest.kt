package studio.guitarlab.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasStateDescription
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNode
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import studio.guitarlab.app.backup.BackupScreenContent
import studio.guitarlab.app.backup.BackupSettingsSnapshot
import studio.guitarlab.app.backup.BackupUiState
import studio.guitarlab.app.ui.NewProjectScreen
import studio.guitarlab.app.ui.ProductStatusChip
import studio.guitarlab.app.ui.ProductStatusTone
import studio.guitarlab.app.ui.ProjectShellScaffold
import studio.guitarlab.app.ui.ProjectWorkspace
import studio.guitarlab.app.ui.SettingsScreen
import studio.guitarlab.app.ui.UnifiedPrepareScreen
import studio.guitarlab.app.ui.theme.GuitarLabTheme
import studio.guitarlab.core.model.GuitarProject
import studio.guitarlab.core.model.ProjectTemplate

@RunWith(AndroidJUnit4::class)
class CohesionResponsiveAccessibilityInstrumentedTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun compactLargeFontNewProjectKeepsAllCreationIntentsAndPrimaryActionDiscoverable() {
        compactContent(darkTheme = true) {
            NewProjectScreen(onBack = {}, onCreate = { _, _ -> }, onCreateForPrepare = { _, _ -> })
        }

        compose.onNodeWithTag("new-project-name").assertIsDisplayed()
        compose.onNodeWithTag("new-project-search").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("new-project-import").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("new-project-studio").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("new-project-create").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun compactLargeFontSettingsKeepsPrimaryAndDeepControlsReachableInLightTheme() {
        compactContent(darkTheme = false) {
            SettingsScreen(
                projectId = null,
                onBack = {},
                onAudioDiagnostics = {},
                onCodecDiagnostics = {},
            )
        }

        compose.onNodeWithTag("settings-content").assertIsDisplayed()
        compose.onNodeWithTag("settings-open-calibration").assertIsDisplayed()
        compose.onNode(hasScrollAction())
            .performScrollToNode(hasTestTag("settings-external-control-toggle"))
        compose.onNodeWithTag("settings-external-control-toggle").assertIsDisplayed()
        compose.onNode(hasScrollAction()).performScrollToNode(
            androidx.compose.ui.test.hasText("Codecs e arquivos"),
        )
        compose.onNodeWithText("Codecs e arquivos").assertIsDisplayed()
    }

    @Test
    fun compactLargeFontBackupKeepsBlockedCatalogMessageReachable() {
        compactContent(darkTheme = true) {
            BackupScreenContent(
                state = BackupUiState(
                    loading = false,
                    settings = BackupSettingsSnapshot(driveConnected = false),
                ),
                projectId = null,
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
                onKeepLocal = { _, _ -> },
                onUseCloud = {},
                onRestoreVersion = {},
                onRestoreAll = {},
            )
        }

        compose.onNodeWithText("Backup e restauração").assertIsDisplayed()
        compose.onNode(hasScrollAction())
            .performScrollToNode(hasTestTag("backup-catalog-disconnected"))
        compose.onNodeWithTag("backup-catalog-disconnected").assertIsDisplayed()
        compose.onNodeWithText("Backup indisponível").assertIsDisplayed()
    }

    @Test
    fun compactLargeFontPreparePreservesProjectNavigationAndJourneyState() {
        val project = GuitarProject(
            id = "c7-prepare",
            name = "Projeto responsivo",
            template = ProjectTemplate.GUITAR,
            createdAtEpochMs = 1,
            updatedAtEpochMs = 2,
        )
        compactContent(darkTheme = false) {
            UnifiedPrepareScreen(
                project = project,
                projectId = project.id,
                onBack = {},
                onStudio = {},
                onExport = {},
            )
        }

        compose.onNodeWithTag("project-shell-navigation").assertIsDisplayed()
        compose.onNodeWithTag("project-shell-nav-prepare").assertIsDisplayed()
        compose.onNodeWithTag("project-shell-nav-studio").assertIsDisplayed()
        compose.onNodeWithTag("project-shell-nav-export").assertIsDisplayed()
        compose.onNodeWithTag("prepare-progress-summary").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("prepare-safe-state-${project.id}").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun wideProjectShellKeepsSingleNavigationGrammarInDarkTheme() {
        val project = GuitarProject(
            id = "c7-wide-shell",
            name = "Projeto amplo",
            template = ProjectTemplate.BLANK,
            createdAtEpochMs = 1,
            updatedAtEpochMs = 2,
        )

        compose.setContent {
            CompositionLocalProvider(LocalDensity provides Density(density = 0.45f, fontScale = 1f)) {
                GuitarLabTheme(darkTheme = true) {
                    ProjectShellScaffold(
                        project = project,
                        currentWorkspace = ProjectWorkspace.STUDIO,
                        onProjects = {},
                        onPrepare = {},
                        onStudio = {},
                        onExport = {},
                        onSettings = {},
                    ) {
                        Box(Modifier.fillMaxSize())
                    }
                }
            }
        }

        compose.onNodeWithTag("project-shell-navigation").assertIsDisplayed()
        compose.onNodeWithTag("project-shell-nav-prepare").assertIsDisplayed()
        compose.onNodeWithTag("project-shell-nav-studio").assertIsDisplayed()
        compose.onNodeWithTag("project-shell-nav-export").assertIsDisplayed()
        compose.onNodeWithTag("project-shell-settings").assertIsDisplayed()
        compose.onNodeWithTag("project-shell-projects").assertIsDisplayed()
    }

    @Test
    fun statusChipPublishesTextAndStateSemanticsWithoutDependingOnColor() {
        compose.setContent {
            GuitarLabTheme(darkTheme = true) {
                ProductStatusChip(label = "Falhou", tone = ProductStatusTone.ERROR)
            }
        }

        compose.onNodeWithText("Falhou").assertIsDisplayed()
        compose.onNode(hasStateDescription("Falhou")).assertIsDisplayed()
    }

    private fun compactContent(
        darkTheme: Boolean,
        content: @androidx.compose.runtime.Composable () -> Unit,
    ) {
        compose.setContent {
            CompositionLocalProvider(LocalDensity provides Density(density = 3f, fontScale = 1.3f)) {
                GuitarLabTheme(darkTheme = darkTheme) {
                    Box(
                        modifier = Modifier
                            .requiredWidth(360.dp)
                            .requiredHeight(720.dp),
                    ) {
                        content()
                    }
                }
            }
        }
    }
}
