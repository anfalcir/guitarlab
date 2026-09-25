package studio.guitarlab.app

import android.content.pm.ApplicationInfo
import androidx.compose.material3.Text
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.runner.RunWith
import studio.guitarlab.app.activity.ActivityScreen
import studio.guitarlab.app.activity.UnifiedActivityStore
import studio.guitarlab.app.activity.UnifiedActivityViewModel
import studio.guitarlab.app.ui.GuitarLabUserGuideDialog
import studio.guitarlab.app.ui.HomeUiState
import studio.guitarlab.app.ui.HomeViewModel
import studio.guitarlab.app.ui.SourceSearchOutcome
import studio.guitarlab.app.ui.SourceSearchTerminalState
import studio.guitarlab.app.ui.StudioUserGuideDialog
import studio.guitarlab.app.ui.NewProjectScreen
import studio.guitarlab.app.ui.NewProjectSourceIntent
import studio.guitarlab.app.ui.ProjectShellScaffold
import studio.guitarlab.app.ui.ProjectMediaDialog
import studio.guitarlab.app.ui.ProjectWorkspace
import studio.guitarlab.app.ui.UnifiedPrepareScreen
import studio.guitarlab.app.ui.UnifiedExportScreen
import studio.guitarlab.app.ui.theme.GuitarLabTheme
import studio.guitarlab.core.model.AssetClassification
import studio.guitarlab.core.model.AssetLifecycle
import studio.guitarlab.core.model.AssetRole
import studio.guitarlab.core.model.ManagedAsset
import studio.guitarlab.core.model.PreparationState
import studio.guitarlab.core.model.PreparationStatus
import studio.guitarlab.core.model.GuitarProject
import studio.guitarlab.core.model.ProjectTemplate
import studio.guitarlab.core.project.UnifiedOperationKind
import studio.guitarlab.core.project.UnifiedOperationState
import studio.guitarlab.core.source.RankedSourceCandidate
import studio.guitarlab.core.source.SourceProvider
import studio.guitarlab.core.separation.DurableRemoteJob
import studio.guitarlab.core.separation.RemoteJobIdentity
import studio.guitarlab.core.separation.RemoteJobState
import studio.guitarlab.platform.separation.RemoteCloudAuthSession
import studio.guitarlab.platform.source.android.SourceOperationSnapshot
import studio.guitarlab.platform.source.android.SourceOperationState

@RunWith(AndroidJUnit4::class)
class UnifiedProjectShellInstrumentedTest {
    @get:Rule val composeRule = createComposeRule()

    @Test fun packagedSourceRuntimeExtractsNativeLibraries() {
        val appInfo = InstrumentationRegistry.getInstrumentation().targetContext.applicationInfo
        check(appInfo.flags and ApplicationInfo.FLAG_EXTRACT_NATIVE_LIBS != 0) {
            "The packaged yt-dlp runtime requires extracted native libraries."
        }
    }

    @Test fun newProjectExposesThreeExplicitIntentsAndHidesTemplatesFromStudyCreation() {
        var created: Pair<String, ProjectTemplate>? = null
        var prepare: Pair<String, NewProjectSourceIntent>? = null
        composeRule.setContent {
            GuitarLabTheme(darkTheme = true) {
                NewProjectScreen(
                    onBack = {},
                    onCreate = { name, template -> created = name to template },
                    onCreateForPrepare = { name, intent -> prepare = name to intent },
                )
            }
        }

        composeRule.captureCohesionScreenshot("new-project-phone-dark")
        composeRule.onNodeWithTag("new-project-search").assertIsDisplayed().performClick()
        composeRule.onAllNodesWithText("Projeto vazio").assertCountEquals(0)
        composeRule.onNodeWithTag("new-project-name").performTextInput("Projeto C3")
        composeRule.onNodeWithTag("new-project-create").performScrollTo().performClick()
        composeRule.runOnIdle { check(prepare == ("Projeto C3" to NewProjectSourceIntent.SEARCH)) }

        composeRule.runOnIdle { prepare = null }
        composeRule.onNodeWithTag("new-project-import").performScrollTo().performClick()
        composeRule.onAllNodesWithText("Projeto vazio").assertCountEquals(0)
        composeRule.onNodeWithTag("new-project-create").performScrollTo().performClick()
        composeRule.runOnIdle { check(prepare == ("Projeto C3" to NewProjectSourceIntent.IMPORT)) }

        composeRule.onNodeWithTag("new-project-studio").performScrollTo().performClick()
        composeRule.onNodeWithTag("new-project-studio-template-options").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Projeto vazio").performScrollTo().performClick()
        composeRule.onNodeWithTag("new-project-create").performScrollTo().performClick()
        composeRule.runOnIdle { check(created == ("Projeto C3" to ProjectTemplate.BLANK)) }
    }

    @Test fun prepareWorkspaceIsSafeWhenProjectIsStillLoading() {
        composeRule.setContent {
            GuitarLabTheme(darkTheme = true) {
                UnifiedPrepareScreen(project = null, projectId = "legacy", onBack = {}, onStudio = {}, onExport = {})
            }
        }
        composeRule.onNodeWithTag("prepare-safe-state-legacy").assertIsDisplayed()
        composeRule.onNodeWithTag("prepare-import-source").performScrollTo().assertIsDisplayed().assertIsNotEnabled()
        composeRule.onNodeWithTag("prepare-search-action").performScrollTo().assertIsDisplayed().assertIsNotEnabled()
    }

    @Test fun preparedProjectSearchDispatchesAndExposesVisibleProgress() {
        val project = GuitarProject(
            id = "search-project",
            name = "Pesquisa",
            template = ProjectTemplate.GUITAR,
            createdAtEpochMs = 1,
            updatedAtEpochMs = 2,
        )
        var request: Pair<String, String>? = null
        val searchBusy = androidx.compose.runtime.mutableStateOf(false)
        composeRule.setContent {
            GuitarLabTheme(darkTheme = true) {
                UnifiedPrepareScreen(
                    project = project,
                    projectId = project.id,
                    onBack = {},
                    onStudio = {},
                    onExport = {},
                    searchBusy = searchBusy.value,
                    onSearch = { artist, song -> request = artist to song },
                )
            }
        }
        composeRule.onNodeWithTag("prepare-search-artist").performScrollTo().performTextInput("Banda")
        composeRule.onNodeWithTag("prepare-search-song").performTextInput("Música")
        composeRule.onNodeWithTag("prepare-search-action").performClick()
        composeRule.runOnIdle {
            check(request == ("Banda" to "Música"))
            searchBusy.value = true
        }

        composeRule.onNodeWithTag("prepare-search-progress").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("prepare-search-action").assertIsNotEnabled()
    }

    @Test fun preparedReferencesAreVisibleAndStudioHandoffRemainsAvailable() {
        val stemRoles = listOf(
            AssetRole.STEM_DRUMS, AssetRole.STEM_BASS, AssetRole.STEM_OTHER,
            AssetRole.STEM_VOCALS, AssetRole.STEM_GUITAR, AssetRole.STEM_PIANO,
        )
        val stemIds = stemRoles.associateWith { "asset-${it.name}" }
        val assets = stemRoles.map { role -> asset(stemIds.getValue(role), role) } + listOf(
            asset("source", AssetRole.SOURCE_ORIGINAL),
            asset("backing", AssetRole.REFERENCE_BACKING),
            asset("guitar", AssetRole.REFERENCE_GUITAR),
        )
        val project = GuitarProject(
            id = "prepared", name = "Song", template = ProjectTemplate.GUITAR,
            createdAtEpochMs = 1, updatedAtEpochMs = 2, assets = assets,
            preparation = PreparationState(
                status = PreparationStatus.READY,
                sourceAssetId = "source",
                activeStemAssetIds = stemIds,
                activeBackingAssetId = "backing",
                activeGuitarAssetId = "guitar",
                availableReferenceAssetIds = listOf("backing", "guitar"),
            ),
        )
        var openedStudio = false
        var restored: Pair<Boolean, Boolean>? = null
        composeRule.setContent {
            GuitarLabTheme(darkTheme = true) {
                UnifiedPrepareScreen(
                    project = project, projectId = project.id, onBack = {},
                    onStudio = { openedStudio = true }, onExport = {},
                    onRestoreReferences = { backing, guitar -> restored = backing to guitar },
                )
            }
        }

        composeRule.onNodeWithTag("prepare-references-ready").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("prepare-restore-references").assertIsDisplayed().performClick()
        composeRule.onNodeWithTag("restore-reference-dialog").assertIsDisplayed()
        composeRule.onNodeWithTag("restore-reference-backing").assertIsDisplayed()
        composeRule.onNodeWithTag("restore-reference-guitar").assertIsDisplayed().performClick()
        composeRule.onNodeWithTag("restore-reference-confirm").performClick()
        composeRule.runOnIdle { check(restored == (true to false)) }
        composeRule.captureCohesionScreenshot("prepare-ready-dark")
        composeRule.onNodeWithText("Studio").performClick()
        composeRule.runOnIdle { check(openedStudio) }
    }

    @Test fun projectMediaSurfaceExplainsManagedAssetsAndOffersExplicitReferenceChoice() {
        val backing = asset("backing", AssetRole.REFERENCE_BACKING)
        val project = GuitarProject(
            id = "media", name = "Song", template = ProjectTemplate.GUITAR,
            createdAtEpochMs = 1, updatedAtEpochMs = 2,
            assets = listOf(backing),
            preparation = PreparationState(
                status = PreparationStatus.READY,
                activeBackingAssetId = backing.assetId,
                availableReferenceAssetIds = listOf(backing.assetId),
            ),
        )
        var kept = false
        var updated = false
        composeRule.setContent {
            GuitarLabTheme(darkTheme = true) {
                ProjectMediaDialog(
                    project = project,
                    referenceBindingDiffers = true,
                    referenceDecisionPending = true,
                    onKeepCurrent = { kept = true },
                    onApplyUpdate = { updated = true },
                    onDismiss = {},
                )
            }
        }

        composeRule.onNodeWithTag("project-media-dialog").assertIsDisplayed()
        composeRule.onNodeWithText("Referências · 1").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Manter atual").performClick()
        composeRule.runOnIdle { check(kept && !updated) }
    }

    @Test fun prepareWorkspaceShowsRankedCandidateAndPersistentActivity() {
        val project = GuitarProject(id = "p", name = "Song", template = ProjectTemplate.BLANK, createdAtEpochMs = 1, updatedAtEpochMs = 1)
        val candidate = RankedSourceCandidate(
            provider = SourceProvider.YOUTUBE,
            title = "Hero (Official Audio)",
            uploader = "Skillet - Topic",
            url = "https://example.test/hero",
            quality = "stream M4A",
            formatId = "140",
            qualityBonus = 0,
            durationSeconds = 190.0,
            previewOnly = false,
            durationWarning = false,
            official = true,
            score = 88,
            reason = "canal oficial, título compatível",
            automaticDownloadSupported = true,
        )
        var selected = false
        composeRule.setContent {
            GuitarLabTheme(darkTheme = true) {
                UnifiedPrepareScreen(
                    project = project,
                    projectId = project.id,
                    onBack = {}, onStudio = {}, onExport = {},
                    candidates = listOf(candidate),
                    operation = SourceOperationSnapshot(project.id, "op", SourceOperationState.RUNNING, 42, "Download 42%", 1),
                    onAcquire = { selected = true },
                )
            }
        }
        composeRule.onNodeWithTag("prepare-candidate-0").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("prepare-use-candidate-0").assertIsEnabled().performClick()
        composeRule.runOnIdle { check(selected) }
        composeRule.onNodeWithTag("prepare-activity").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("prepare-cancel-source").assertIsDisplayed().assertIsEnabled()
        composeRule.captureCohesionScreenshot("prepare-search-running-dark")
    }
    @Test fun existingSourceRequiresExplicitReplacementBeforeSourcePickersReturn() {
        val source = asset("source", AssetRole.SOURCE_ORIGINAL)
        val project = GuitarProject(
            id = "replace", name = "Song", template = ProjectTemplate.GUITAR, createdAtEpochMs = 1, updatedAtEpochMs = 2,
            assets = listOf(source),
            preparation = PreparationState(status = PreparationStatus.SOURCE_READY, sourceAssetId = source.assetId),
        )
        var replacementRequested = false
        composeRule.setContent {
            GuitarLabTheme(darkTheme = true) {
                UnifiedPrepareScreen(
                    project = project, projectId = project.id, onBack = {}, onStudio = {}, onExport = {},
                    onBeginSourceReplacement = { replacementRequested = true },
                )
            }
        }
        composeRule.onNodeWithTag("prepare-replace-source").performScrollTo().assertIsDisplayed().performClick()
        composeRule.runOnIdle { check(replacementRequested) }
        composeRule.onAllNodesWithText("Importar áudio").assertCountEquals(0)
    }

    @Test fun sourceReplacementModeReturnsPickersWithoutDroppingProtectedSource() {
        val source = asset("source", AssetRole.SOURCE_ORIGINAL)
        val project = GuitarProject(
            id = "replace-active", name = "Song", template = ProjectTemplate.GUITAR, createdAtEpochMs = 1, updatedAtEpochMs = 2,
            assets = listOf(source),
            preparation = PreparationState(status = PreparationStatus.SOURCE_READY, sourceAssetId = source.assetId),
        )
        composeRule.setContent {
            GuitarLabTheme(darkTheme = true) {
                UnifiedPrepareScreen(
                    project = project, projectId = project.id, onBack = {}, onStudio = {}, onExport = {},
                    sourceReplacementActive = true,
                )
            }
        }
        composeRule.onNodeWithTag("prepare-source-replacement-note").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("prepare-import-source").performScrollTo().assertIsDisplayed().assertIsEnabled()
        composeRule.onNodeWithTag("prepare-search-action").performScrollTo().assertIsDisplayed().assertIsNotEnabled()
        composeRule.onNodeWithTag("prepare-cancel-source-replacement").performScrollTo().assertIsDisplayed()
    }

    @Test fun sixValidatedStemsEnterAutomaticReferencePreparationWithoutManualConfirmation() {
        val stemRoles = listOf(
            AssetRole.STEM_DRUMS, AssetRole.STEM_BASS, AssetRole.STEM_OTHER,
            AssetRole.STEM_VOCALS, AssetRole.STEM_GUITAR, AssetRole.STEM_PIANO,
        )
        val stemIds = stemRoles.associateWith { "asset-${it.name}" }
        val project = GuitarProject(
            id = "auto-references", name = "Song", template = ProjectTemplate.GUITAR,
            createdAtEpochMs = 1, updatedAtEpochMs = 2,
            assets = stemRoles.map { role -> asset(stemIds.getValue(role), role) },
            preparation = PreparationState(
                status = PreparationStatus.READY,
                sourceAssetId = "source",
                activeStemAssetIds = stemIds,
            ),
        )
        composeRule.setContent {
            GuitarLabTheme(darkTheme = true) {
                UnifiedPrepareScreen(project = project, projectId = project.id, onBack = {}, onStudio = {}, onExport = {})
            }
        }

        composeRule.onNodeWithTag("prepare-reference-progress").performScrollTo().assertIsDisplayed()
        composeRule.onAllNodesWithText("Criar base e referência").assertCountEquals(0)
        composeRule.onAllNodesWithText("Tentar novamente").assertCountEquals(0)
        composeRule.onNodeWithText("O GuitarLab cria automaticamente a base sem guitarra e a guitarra de referência. Nenhuma confirmação extra é necessária.")
            .performScrollTo().assertIsDisplayed()
    }

    @Test fun referencePreparationFailureOffersRetryWithoutLosingStemProgress() {
        val stemRoles = listOf(
            AssetRole.STEM_DRUMS, AssetRole.STEM_BASS, AssetRole.STEM_OTHER,
            AssetRole.STEM_VOCALS, AssetRole.STEM_GUITAR, AssetRole.STEM_PIANO,
        )
        val stemIds = stemRoles.associateWith { "asset-${it.name}" }
        val project = GuitarProject(
            id = "reference-retry", name = "Song", template = ProjectTemplate.GUITAR,
            createdAtEpochMs = 1, updatedAtEpochMs = 2,
            assets = stemRoles.map { role -> asset(stemIds.getValue(role), role) },
            preparation = PreparationState(
                status = PreparationStatus.ERROR,
                sourceAssetId = "source",
                activeStemAssetIds = stemIds,
            ),
        )
        var retried = false
        composeRule.setContent {
            GuitarLabTheme(darkTheme = true) {
                UnifiedPrepareScreen(
                    project = project, projectId = project.id, onBack = {}, onStudio = {}, onExport = {},
                    onPrepareReferences = { retried = true },
                )
            }
        }

        composeRule.onNodeWithTag("prepare-step-separation").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("prepare-retry-references").performScrollTo().assertIsDisplayed().performClick()
        composeRule.runOnIdle { check(retried) }
    }

    @Test fun sourceSearchTerminalSuggestionRemainsVisibleAndRetriesExplicitly() {
        val project = GuitarProject(
            id = "search-terminal",
            name = "MMF - Misery",
            template = ProjectTemplate.GUITAR,
            createdAtEpochMs = 1,
            updatedAtEpochMs = 2,
        )
        var retriedArtist: String? = null
        var retriedSong: String? = null
        composeRule.setContent {
            GuitarLabTheme(darkTheme = true) {
                UnifiedPrepareScreen(
                    project = project,
                    projectId = project.id,
                    onBack = {},
                    onStudio = {},
                    onExport = {},
                    searchOutcome = SourceSearchOutcome(
                        operationId = "search-1",
                        artist = "memphys may fire",
                        song = "misery",
                        terminalState = SourceSearchTerminalState.DID_YOU_MEAN,
                        message = "Nenhuma correspondência exata foi encontrada.",
                        suggestedArtist = "Memphis May Fire",
                    ),
                    onSearch = { artist, song ->
                        retriedArtist = artist
                        retriedSong = song
                    },
                )
            }
        }

        composeRule.onNodeWithTag("prepare-search-terminal-did_you_mean").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("prepare-search-suggestion").assertTextEquals("Você quis dizer “Memphis May Fire”?")
        composeRule.onNodeWithTag("prepare-search-use-suggestion").performClick()
        composeRule.runOnIdle {
            check(retriedArtist == "Memphis May Fire")
            check(retriedSong == "misery")
        }
    }

    @Test fun sourceSearchTimeoutNeverFallsBackToSilentIdle() {
        val project = GuitarProject(
            id = "search-timeout",
            name = "Song",
            template = ProjectTemplate.GUITAR,
            createdAtEpochMs = 1,
            updatedAtEpochMs = 2,
        )
        composeRule.setContent {
            GuitarLabTheme(darkTheme = true) {
                UnifiedPrepareScreen(
                    project = project,
                    projectId = project.id,
                    onBack = {},
                    onStudio = {},
                    onExport = {},
                    searchOutcome = SourceSearchOutcome(
                        operationId = "search-timeout-1",
                        artist = "Artist",
                        song = "Song",
                        terminalState = SourceSearchTerminalState.TIMEOUT,
                        message = "A pesquisa demorou mais que o esperado. Verifique a conexão e tente novamente.",
                    ),
                )
            }
        }

        composeRule.onNodeWithTag("prepare-search-terminal-timeout").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("prepare-search-retry").assertIsDisplayed().assertIsEnabled()
    }

    @Test fun rankingBadgeIsTextualAndTechnicalDetailsStayCollapsed() {
        val candidate = RankedSourceCandidate(
            provider = SourceProvider.YOUTUBE,
            title = "Hero (Official Audio)", uploader = "Skillet - Topic", url = "https://example.test/hero",
            quality = "stream M4A", formatId = "140", qualityBonus = 0, durationSeconds = 190.0,
            previewOnly = false, durationWarning = false, official = true, score = 88,
            reason = "canal oficial, título compatível", automaticDownloadSupported = true,
        )
        composeRule.setContent {
            GuitarLabTheme(darkTheme = true) {
                UnifiedPrepareScreen(
                    project = GuitarProject(id = "score", name = "Song", template = ProjectTemplate.BLANK, createdAtEpochMs = 1, updatedAtEpochMs = 1),
                    projectId = "score", onBack = {}, onStudio = {}, onExport = {}, candidates = listOf(candidate),
                )
            }
        }
        composeRule.onNodeWithText("Excelente · 88/100").performScrollTo().assertIsDisplayed()
    }

    @Test fun technicalAssetDetailsStayCollapsedUntilExplicitlyExpanded() {
        val source = asset("source-details", AssetRole.SOURCE_ORIGINAL)
        val sourceProject = GuitarProject(
            id = "technical-details", name = "Song", template = ProjectTemplate.GUITAR,
            createdAtEpochMs = 1, updatedAtEpochMs = 2, assets = listOf(source),
            preparation = PreparationState(status = PreparationStatus.SOURCE_READY, sourceAssetId = source.assetId),
        )
        composeRule.setContent {
            GuitarLabTheme(darkTheme = true) {
                UnifiedPrepareScreen(project = sourceProject, projectId = sourceProject.id, onBack = {}, onStudio = {}, onExport = {})
            }
        }
        composeRule.onAllNodesWithTag("prepare-asset-source_original").assertCountEquals(0)
        composeRule.onNodeWithTag("prepare-details-toggle").performScrollTo().performClick()
        composeRule.onNodeWithTag("prepare-asset-source_original").performScrollTo().assertIsDisplayed()
    }

    @Test fun separationRunningUsesUnifiedProgressLanguageAndRetainsScreenshotEvidence() {
        val source = asset("source-running", AssetRole.SOURCE_ORIGINAL)
        val project = GuitarProject(
            id = "separation-running", name = "Song", template = ProjectTemplate.GUITAR,
            createdAtEpochMs = 1, updatedAtEpochMs = 2, assets = listOf(source),
            preparation = PreparationState(status = PreparationStatus.SOURCE_READY, sourceAssetId = source.assetId),
        )
        val job = DurableRemoteJob(
            identity = RemoteJobIdentity(
                "00000000-0000-4000-8000-000000000002", project.id, source.assetId, source.sha256,
            ),
            state = RemoteJobState.RUNNING,
            updatedAtMs = 2,
        )
        composeRule.setContent {
            GuitarLabTheme(darkTheme = true) {
                UnifiedPrepareScreen(
                    project = project, projectId = project.id, onBack = {}, onStudio = {}, onExport = {}, separationJob = job,
                )
            }
        }

        composeRule.onNodeWithTag("prepare-safe-state-separation-running").assertIsDisplayed()
        composeRule.captureCohesionScreenshot("prepare-separating-dark")
    }

    @Test fun prepareSeparationOffersCloudLoginInlineWhenSessionIsMissing() {
        val source = asset("source-login", AssetRole.SOURCE_ORIGINAL)
        val project = GuitarProject(
            id = "separation-login",
            name = "Song",
            template = ProjectTemplate.GUITAR,
            createdAtEpochMs = 1,
            updatedAtEpochMs = 2,
            assets = listOf(source),
            preparation = PreparationState(status = PreparationStatus.SOURCE_READY, sourceAssetId = source.assetId),
        )
        composeRule.setContent {
            GuitarLabTheme(darkTheme = true) {
                UnifiedPrepareScreen(
                    project = project,
                    projectId = project.id,
                    onBack = {},
                    onStudio = {},
                    onExport = {},
                )
            }
        }

        composeRule.onNodeWithTag("prepare-cloud-auth-required").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("prepare-cloud-auth-action").assertIsDisplayed().assertIsEnabled().performClick()
        composeRule.onNodeWithTag("prepare-cloud-auth-dialog").assertIsDisplayed()
        composeRule.onNodeWithTag("prepare-cloud-auth-email").assertIsDisplayed()
        composeRule.onNodeWithTag("prepare-cloud-auth-password").assertIsDisplayed()
        composeRule.onNodeWithTag("prepare-cloud-auth-submit").assertIsDisplayed()
    }

    @Test fun completedRemoteResultCanResumeImportWithoutStartingDemucsAgain() {
        val source = asset("source-import-resume", AssetRole.SOURCE_ORIGINAL)
        val project = GuitarProject(
            id = "import-resume",
            name = "Song",
            template = ProjectTemplate.GUITAR,
            createdAtEpochMs = 1,
            updatedAtEpochMs = 2,
            assets = listOf(source),
            preparation = PreparationState(status = PreparationStatus.SEPARATING, sourceAssetId = source.assetId),
        )
        val job = DurableRemoteJob(
            identity = RemoteJobIdentity(
                "00000000-0000-4000-8000-000000000004",
                project.id,
                source.assetId,
                source.sha256,
            ),
            state = RemoteJobState.IMPORT_FAILED,
            updatedAtMs = 4,
            resultManifestSha256 = "b".repeat(64),
            errorCode = "WORKER_RETRY_EXHAUSTED_RETRY:DOWNLOADING_RESULTS:NETWORK_IO:ATTEMPT_6",
        )
        var resumed = false
        var restarted = false
        composeRule.setContent {
            GuitarLabTheme(darkTheme = true) {
                UnifiedPrepareScreen(
                    project = project,
                    projectId = project.id,
                    onBack = {},
                    onStudio = {},
                    onExport = {},
                    separationJob = job,
                    onStartSeparation = { restarted = true },
                    onResumeSeparationImport = { resumed = true },
                    initialCloudSession = RemoteCloudAuthSession("test-uid", "test@example.com"),
                    requestNotificationPermission = false,
                )
            }
        }

        composeRule.onNodeWithTag("prepare-resume-import").performScrollTo().assertIsDisplayed().assertIsEnabled().performClick()
        composeRule.onNodeWithTag("prepare-resume-import-note").assertIsDisplayed()
        composeRule.runOnIdle {
            check(resumed)
            check(!restarted)
        }
    }

    @Test fun recoveredOrphanExposesRetryAndPreservesAcceptedSource() {
        val source = asset("source-orphan", AssetRole.SOURCE_ORIGINAL)
        val project = GuitarProject(
            id = "separation-orphan", name = "Song", template = ProjectTemplate.GUITAR,
            createdAtEpochMs = 1, updatedAtEpochMs = 2, assets = listOf(source),
            preparation = PreparationState(status = PreparationStatus.SOURCE_READY, sourceAssetId = source.assetId),
        )
        val job = DurableRemoteJob(
            identity = RemoteJobIdentity(
                "00000000-0000-4000-8000-000000000003", project.id, source.assetId, source.sha256,
            ),
            state = RemoteJobState.CANCELLED,
            updatedAtMs = 3,
            errorCode = "REMOTE_JOB_NOT_FOUND",
        )
        var retried = false
        composeRule.setContent {
            GuitarLabTheme(darkTheme = true) {
                UnifiedPrepareScreen(
                    project = project,
                    projectId = project.id,
                    onBack = {},
                    onStudio = {},
                    onExport = {},
                    separationJob = job,
                    onStartSeparation = { retried = true },
                    initialCloudSession = RemoteCloudAuthSession("test-uid", "test@example.com"),
                    requestNotificationPermission = false,
                )
            }
        }

        composeRule.onNodeWithTag("prepare-source-ready").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("prepare-start-separation").performScrollTo().assertIsDisplayed().assertIsEnabled().performClick()
        composeRule.runOnIdle { check(retried) }
        composeRule.onAllNodesWithText("REMOTE_JOB_NOT_FOUND").assertCountEquals(0)
    }

    @Test fun separationFailureUsesUserSafeCopyInsteadOfRawJobStateOrBackendCode() {
        val source = asset("source", AssetRole.SOURCE_ORIGINAL)
        val project = GuitarProject(
            id = "separation-failed", name = "Song", template = ProjectTemplate.GUITAR,
            createdAtEpochMs = 1, updatedAtEpochMs = 2, assets = listOf(source),
            preparation = PreparationState(status = PreparationStatus.SOURCE_READY, sourceAssetId = source.assetId),
        )
        val job = DurableRemoteJob(
            identity = RemoteJobIdentity(
                "00000000-0000-4000-8000-000000000001", project.id, source.assetId, source.sha256,
            ),
            state = RemoteJobState.FAILED,
            updatedAtMs = 2,
            errorCode = "REMOTE_JOB_NOT_FOUND",
        )
        composeRule.setContent {
            GuitarLabTheme(darkTheme = true) {
                UnifiedPrepareScreen(
                    project = project, projectId = project.id, onBack = {}, onStudio = {}, onExport = {}, separationJob = job,
                )
            }
        }

        composeRule.onNodeWithTag("prepare-safe-state-separation-failed")
            .performScrollTo()
            .assertTextContains("Não foi possível concluir a separação. As mídias válidas do projeto foram preservadas.")
            .assertIsDisplayed()
        composeRule.onAllNodesWithText("FAILED").assertCountEquals(0)
        composeRule.onAllNodesWithText("REMOTE_JOB_NOT_FOUND").assertCountEquals(0)
        composeRule.captureCohesionScreenshot("prepare-failed-dark")
    }

    @Test fun exportWorkspaceUsesCanonicalPortugueseSectionsAndKeepsConversionDetailSecondary() {
        val project = GuitarProject(
            id = "export", name = "Song", template = ProjectTemplate.GUITAR, createdAtEpochMs = 1, updatedAtEpochMs = 2,
            assets = listOf(asset("backing", AssetRole.REFERENCE_BACKING), asset("guitar", AssetRole.REFERENCE_GUITAR)),
            preparation = PreparationState(
                status = PreparationStatus.READY, activeBackingAssetId = "backing", activeGuitarAssetId = "guitar",
                availableReferenceAssetIds = listOf("backing", "guitar"),
            ),
        )
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val viewModel = HomeViewModel(context.applicationContext as android.app.Application)
        composeRule.setContent {
            GuitarLabTheme(darkTheme = true) {
                UnifiedExportScreen(project, project.id, viewModel, onBack = {}, onPrepare = {}, onStudio = {})
            }
        }
        composeRule.onNodeWithText("Projeto portátil").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Arquivos para estudo").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("export-backing-wav").performScrollTo().assertIsDisplayed().assertIsEnabled()
        composeRule.onNodeWithTag("export-guitar-wav").performScrollTo().assertIsDisplayed().assertIsEnabled()
        composeRule.onNodeWithTag("export-backing-wav").assertTextEquals("WAV")
        composeRule.onNodeWithTag("export-guitar-wav").assertTextEquals("WAV")
        composeRule.onAllNodesWithText("WAV • sem conversão").assertCountEquals(0)
        composeRule.onNodeWithText("As duas referências finais — base sem guitarra e guitarra — ficam em WAV sem perdas dentro do projeto; os seis stems intermediários não são armazenados no fluxo v2. WAV é publicado sem conversão; FLAC/MP3 só são gerados quando você pedir.").assertIsDisplayed()
        composeRule.onNodeWithText("Base sem guitarra").assertIsDisplayed()
        composeRule.onNodeWithText("Mix final do Studio").performScrollTo().assertIsDisplayed()
        composeRule.captureCohesionScreenshot("export-ready-dark")
    }

    @Test fun exportRunningStateIsVisibleInLightThemeAndRetainedAsEvidence() {
        val project = GuitarProject(
            id = "export-running", name = "Song", template = ProjectTemplate.GUITAR, createdAtEpochMs = 1, updatedAtEpochMs = 2,
            assets = listOf(asset("backing-running", AssetRole.REFERENCE_BACKING), asset("guitar-running", AssetRole.REFERENCE_GUITAR)),
            preparation = PreparationState(
                status = PreparationStatus.READY, activeBackingAssetId = "backing-running", activeGuitarAssetId = "guitar-running",
                availableReferenceAssetIds = listOf("backing-running", "guitar-running"),
            ),
        )
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val viewModel = HomeViewModel(context.applicationContext as android.app.Application)
        composeRule.setContent {
            GuitarLabTheme(darkTheme = false) {
                UnifiedExportScreen(project, project.id, viewModel, onBack = {}, onPrepare = {}, onStudio = {})
            }
        }
        composeRule.runOnIdle {
            setHomeState(
                viewModel,
                viewModel.state.value.copy(
                    loading = false,
                    exportBusy = true,
                    exportOperationLabel = "Exportando mix final…",
                ),
            )
        }
        composeRule.onNodeWithTag("export-progress").assertIsDisplayed()
        composeRule.onNodeWithTag("export-cancel").assertIsDisplayed().assertIsEnabled()
        composeRule.captureCohesionScreenshot("export-running-light")
    }


    @Test fun sharedProjectShellUsesOneNavigationGrammar() {
        val project = GuitarProject(id = "shell", name = "Shell project", template = ProjectTemplate.GUITAR, createdAtEpochMs = 1, updatedAtEpochMs = 2)
        var destination: String? = null
        composeRule.setContent {
            GuitarLabTheme(darkTheme = true) {
                ProjectShellScaffold(
                    project = project,
                    currentWorkspace = ProjectWorkspace.PREPARE,
                    onProjects = { destination = "projects" },
                    onPrepare = { destination = "prepare" },
                    onStudio = { destination = "studio" },
                    onExport = { destination = "export" },
                    onSettings = { destination = "settings" },
                ) { Text("Conteúdo") }
            }
        }

        composeRule.onNodeWithTag("project-shell-prepare").assertIsDisplayed()
        composeRule.onNodeWithTag("project-shell-project-name").assertTextEquals("Shell project")
        composeRule.onNodeWithTag("project-shell-nav-prepare").assertIsDisplayed()
        composeRule.onNodeWithTag("project-shell-nav-studio").assertIsDisplayed().performClick()
        composeRule.runOnIdle { check(destination == "studio") }
        composeRule.onNodeWithTag("project-shell-nav-export").assertIsDisplayed().performClick()
        composeRule.runOnIdle { check(destination == "export") }
        composeRule.onNodeWithTag("project-shell-settings").assertIsDisplayed().performClick()
        composeRule.runOnIdle { check(destination == "settings") }
        composeRule.onNodeWithTag("project-shell-projects").assertIsDisplayed().performClick()
        composeRule.runOnIdle { check(destination == "projects") }
        composeRule.onNodeWithContentDescription("Início").assertIsDisplayed()
        composeRule.onAllNodesWithText("Projetos").assertCountEquals(0)
        val shellBounds = composeRule.onNodeWithTag("project-shell-prepare").getUnclippedBoundsInRoot()
        val navigationBounds = composeRule.onNodeWithTag("project-shell-navigation").getUnclippedBoundsInRoot()
        val shellCenter = (shellBounds.left + shellBounds.right) / 2
        val navigationCenter = (navigationBounds.left + navigationBounds.right) / 2
        assertEquals(shellCenter.value, navigationCenter.value, 1f)
    }


    @Test fun productWideHelpCoversWholeWorkflowAndStudioHelpRemainsContextual() {
        composeRule.setContent {
            GuitarLabTheme(darkTheme = true) {
                GuitarLabUserGuideDialog(onDismiss = {})
            }
        }

        composeRule.onNodeWithTag("guitarlab-user-guide").assertIsDisplayed()
        composeRule.onNodeWithText("Guia do GuitarLab").assertIsDisplayed()
        listOf(
            "Biblioteca e novo projeto",
            "Preparar",
            "Studio · transporte e edição",
            "Exportar",
            "Atividade, nuvem e backup",
            "Opções e diagnósticos",
        ).forEach { heading ->
            composeRule.onNodeWithText(heading).performScrollTo().assertIsDisplayed()
        }
        composeRule.onAllNodesWithText("GBW", substring = true).assertCountEquals(0)
    }

    @Test fun studioHelpRemainsContextual() {
        composeRule.setContent {
            GuitarLabTheme(darkTheme = true) {
                StudioUserGuideDialog(onDismiss = {})
            }
        }

        composeRule.onNodeWithTag("studio-user-guide").assertIsDisplayed()
        composeRule.onNodeWithText("Ajuda do Studio").assertIsDisplayed()
        composeRule.onNodeWithText("Studio · transporte e edição").assertIsDisplayed()
        composeRule.onAllNodesWithText("Biblioteca e novo projeto").assertCountEquals(0)
        composeRule.onAllNodesWithText("Atividade, nuvem e backup").assertCountEquals(0)
    }

    @Suppress("UNCHECKED_CAST")
    private fun setHomeState(viewModel: HomeViewModel, state: HomeUiState) {
        val field = HomeViewModel::class.java.getDeclaredField("_state").apply { isAccessible = true }
        (field.get(viewModel) as MutableStateFlow<HomeUiState>).value = state
    }

    private fun asset(id: String, role: AssetRole) = ManagedAsset(
        assetId = id, role = role, relativePath = "media/$id.wav",
        sha256 = "a".repeat(64), byteSize = 128, format = "wav",
        sampleRateHz = 48_000, channelCount = 2, frameCount = 4_800,
        createdAtEpochMs = 1, classification = if (role.name.startsWith("STEM_")) AssetClassification.AUTHORITATIVE else AssetClassification.DERIVED,
        lifecycle = AssetLifecycle.MANAGED,
    )


    @Test fun activityCancelTerminatesOrphanedActiveRecord() {
        val application = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as android.app.Application
        val store = UnifiedActivityStore(application)
        val operationId = "test-activity-cancel-orphan"
        store.remove(operationId)
        try {
            store.record(
                operationId,
                null,
                UnifiedOperationKind.EXPORT,
                UnifiedOperationState.RUNNING,
                null,
                "Exportação órfã em andamento",
            )
            val viewModel = UnifiedActivityViewModel(application)
            composeRule.setContent {
                GuitarLabTheme(darkTheme = true) {
                    ActivityScreen(onBack = {}, viewModel = viewModel)
                }
            }

            composeRule.onNodeWithTag("activity-cancel-$operationId").assertIsDisplayed().performClick()
            composeRule.waitUntil(5_000) {
                store.snapshot().firstOrNull { it.operationId == operationId }?.state == UnifiedOperationState.CANCELLED
            }
            composeRule.onNodeWithTag("activity-record-$operationId").assertIsDisplayed()
            composeRule.onNodeWithText("Cancelada").assertIsDisplayed()
            composeRule.onAllNodesWithTag("activity-cancel-$operationId").assertCountEquals(0)
        } finally {
            store.remove(operationId)
        }
    }

    @Test fun activityClearHistoryRemovesAllTerminalRecordsButKeepsActiveWork() {
        val application = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as android.app.Application
        val store = UnifiedActivityStore(application)
        val activeId = "test-history-active"
        val failedId = "test-history-failed"
        val successId = "test-history-success"
        val cancelledId = "test-history-cancelled"
        listOf(activeId, failedId, successId, cancelledId).forEach(store::remove)
        try {
            store.record(activeId, null, UnifiedOperationKind.BACKUP, UnifiedOperationState.RUNNING, null, "Operação ativa")
            store.record(failedId, null, UnifiedOperationKind.BACKUP, UnifiedOperationState.FAILED, null, "Operação falhou")
            store.record(successId, null, UnifiedOperationKind.BACKUP, UnifiedOperationState.SUCCEEDED, 100, "Operação concluída")
            store.record(cancelledId, null, UnifiedOperationKind.BACKUP, UnifiedOperationState.CANCELLED, null, "Operação cancelada")

            val viewModel = UnifiedActivityViewModel(application)
            composeRule.setContent {
                GuitarLabTheme(darkTheme = true) {
                    ActivityScreen(onBack = {}, viewModel = viewModel)
                }
            }

            composeRule.onNodeWithTag("activity-record-$activeId").assertIsDisplayed()
            composeRule.onNodeWithTag("activity-record-$failedId").assertIsDisplayed()
            composeRule.onNodeWithTag("activity-clear-history").assertIsDisplayed().performClick()
            composeRule.waitForIdle()

            composeRule.onNodeWithTag("activity-record-$activeId").assertIsDisplayed()
            composeRule.onAllNodesWithTag("activity-record-$failedId").assertCountEquals(0)
            composeRule.onAllNodesWithTag("activity-record-$successId").assertCountEquals(0)
            composeRule.onAllNodesWithTag("activity-record-$cancelledId").assertCountEquals(0)
        } finally {
            listOf(activeId, failedId, successId, cancelledId).forEach(store::remove)
        }
    }

}
