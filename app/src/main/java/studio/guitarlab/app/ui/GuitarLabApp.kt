package studio.guitarlab.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import studio.guitarlab.app.backup.BackupScreen
import studio.guitarlab.app.activity.ActivityScreen
import studio.guitarlab.app.activity.UnifiedActivityViewModel
import studio.guitarlab.core.model.GuitarProject
import studio.guitarlab.core.model.ProjectTemplate
import studio.guitarlab.platform.separation.RemoteCloudAuthClient

@Composable
fun GuitarLabApp(
    homeViewModel: HomeViewModel = viewModel(),
    navigationViewModel: AppNavigationViewModel = viewModel(),
    deepLinkRoute: String? = null,
    onDeepLinkConsumed: () -> Unit = {},
) {
    val persistedRoute by navigationViewModel.persistedRoute.collectAsState()
    val homeState by homeViewModel.state.collectAsState()
    val context = LocalContext.current
    val remoteCloudAuth = remember(context) { RemoteCloudAuthClient(context) }
    val screen = AppRouteCodec.decode(persistedRoute)
    val workspaceStateHolder = rememberSaveableStateHolder()

    fun navigate(destination: AppScreen) {
        navigationViewModel.navigate(destination)
    }

    LaunchedEffect(deepLinkRoute) {
        deepLinkRoute?.takeIf { it.isNotBlank() }?.let { route ->
            navigate(AppRouteCodec.decode(route))
            onDeepLinkConsumed()
        }
    }

    when (val current = screen) {
        AppScreen.Home -> HomeScreen(
            viewModel = homeViewModel,
            onNewProject = { navigate(AppScreen.NewProject) },
            onOpenProject = { projectId ->
                val project = homeState.projectsById[projectId]
                navigate(project?.let(navigationViewModel::destinationForProject) ?: AppScreen.Studio(projectId))
            },
            onPrepareProject = { navigate(AppScreen.Prepare(it)) },
            onExportWorkspace = { navigate(ExportEntryPointPolicy.destination(it)) },
            onSettings = { navigate(AppScreen.Options()) },
            onActivity = { navigate(AppScreen.Activity()) },
            onBackupProject = { navigate(AppScreen.Backup(it, returnToHome = true)) },
        )
        is AppScreen.Activity -> ActivityScreen(
            onBack = { navigate(AppScreen.Home) },
            viewModel = viewModel<UnifiedActivityViewModel>(),
            focusOperationId = current.operationId,
        )
        AppScreen.NewProject -> NewProjectScreen(
            onBack = { navigate(AppScreen.Home) },
            onCreate = { name, template ->
                homeViewModel.createProject(name, template) { projectId -> navigate(AppScreen.Studio(projectId)) }
            },
            onCreateForPrepare = { name, _ ->
                homeViewModel.createProject(name, ProjectTemplate.GUITAR) { projectId -> navigate(AppScreen.Prepare(projectId)) }
            },
        )
        is AppScreen.Prepare -> {
            DisposableEffect(current.projectId) {
                homeViewModel.startPrepareObservation(current.projectId)
                onDispose { homeViewModel.stopPrepareObservation(current.projectId) }
            }
            ProjectRouteGate(homeState, current.projectId, homeViewModel::refresh, { navigate(AppScreen.Home) }) { project ->
                workspaceStateHolder.SaveableStateProvider("prepare:${current.projectId}") {
                    UnifiedPrepareScreen(
                    project = project,
                    projectId = current.projectId,
                    onBack = { navigate(AppScreen.Home) },
                    onStudio = { navigate(AppScreen.Studio(current.projectId)) },
                    onExport = { navigate(ExportEntryPointPolicy.destination(current.projectId)) },
                    onSettings = { navigate(AppScreen.Options(current.projectId)) },
                    candidates = homeState.sourceCandidatesByProject[current.projectId].orEmpty(),
                    warnings = homeState.sourceWarningsByProject[current.projectId].orEmpty(),
                    searchBusy = current.projectId in homeState.sourceSearchBusyProjects,
                    operation = homeState.sourceOperationsByProject[current.projectId],
                    separationJob = homeState.separationJobsByProject[current.projectId],
                    referenceBusy = current.projectId in homeState.preparedReferenceBusyProjects,
                    sourceReplacementActive = current.projectId in homeState.sourceReplacementProjects,
                    onBeginSourceReplacement = { homeViewModel.beginSourceReplacement(current.projectId) },
                    onCancelSourceReplacement = { homeViewModel.cancelSourceReplacement(current.projectId) },
                    onSearch = { artist, song -> homeViewModel.searchSources(current.projectId, artist, song) },
                    onImport = { uri -> homeViewModel.importSource(current.projectId, uri) },
                    onAcquire = { candidate -> homeViewModel.acquireSource(current.projectId, candidate) },
                    onCancel = { homeViewModel.cancelSourceAcquisition(current.projectId) },
                    onStartSeparation = { homeViewModel.startSeparation(current.projectId) },
                    onCancelSeparation = { homeViewModel.cancelSeparation(current.projectId) },
                    onPrepareReferences = { homeViewModel.prepareReferences(current.projectId) },
                    initialCloudSession = remoteCloudAuth.currentSession(),
                )
                }
            }
        }
        is AppScreen.Studio -> ProjectRouteGate(homeState, current.projectId, homeViewModel::refresh, { navigate(AppScreen.Home) }) { project ->
            workspaceStateHolder.SaveableStateProvider("studio:${current.projectId}") {
                StudioShellScreen(
                    projectId = current.projectId,
                    shellProject = project,
                    onBack = {
                        homeViewModel.refresh()
                        navigate(AppScreen.Home)
                    },
                    onPrepare = { navigate(AppScreen.Prepare(current.projectId)) },
                    onOptions = { navigate(AppScreen.Options(current.projectId)) },
                    onExport = { navigate(ExportEntryPointPolicy.destination(current.projectId)) },
                )
            }
        }
        is AppScreen.Export -> ProjectRouteGate(homeState, current.projectId, homeViewModel::refresh, { navigate(AppScreen.Home) }) { project ->
            workspaceStateHolder.SaveableStateProvider("export:${current.projectId}") {
                UnifiedExportScreen(
                    project = project,
                    projectId = current.projectId,
                    viewModel = homeViewModel,
                    onBack = { navigate(AppScreen.Home) },
                    onPrepare = { navigate(AppScreen.Prepare(current.projectId)) },
                    onStudio = { navigate(AppScreen.Studio(current.projectId)) },
                    onSettings = { navigate(AppScreen.Options(current.projectId)) },
                )
            }
        }
        is AppScreen.Options -> SettingsScreen(
            projectId = current.projectId,
            onBack = { navigate(navigationViewModel.returnFromSettings(current.projectId)) },
            onAudioDiagnostics = { navigate(AppScreen.AudioProbe(current.projectId)) },
            onCodecDiagnostics = { navigate(AppScreen.CodecProbe(current.projectId)) },
            onBackupSettings = { navigate(AppScreen.Backup(current.projectId)) },
            onActivity = { navigate(AppScreen.Activity()) },
        )
        is AppScreen.AudioProbe -> AudioProbeScreen(onBack = { navigate(AppScreen.Options(current.projectId)) })
        is AppScreen.CodecProbe -> CodecProbeScreen(onBack = { navigate(AppScreen.Options(current.projectId)) })
        is AppScreen.Backup -> BackupScreen(
            projectId = current.projectId,
            onBack = { if (current.returnToHome) navigate(AppScreen.Home) else navigate(AppScreen.Options(current.projectId)) },
            onProjectsChanged = homeViewModel::refresh,
        )
    }
}

@Composable
private fun ProjectRouteGate(
    homeState: HomeUiState,
    projectId: String,
    onRefresh: () -> Unit,
    onProjects: () -> Unit,
    content: @Composable (GuitarProject) -> Unit,
) {
    val project = homeState.projectsById[projectId]
    LaunchedEffect(projectId) {
        if (project == null) onRefresh()
    }
    when {
        project != null -> content(project)
        homeState.loading -> Box(Modifier.fillMaxSize().testTag("project-route-loading"), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        else -> Box(Modifier.fillMaxSize().padding(24.dp).testTag("project-route-missing"), contentAlignment = Alignment.Center) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("Projeto indisponível", style = MaterialTheme.typography.headlineSmall)
                Text(
                    homeState.error ?: "O projeto não existe mais ou não pôde ser carregado. Volte à biblioteca para escolher outro projeto.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Button(onClick = onProjects) { Text("Voltar aos projetos") }
            }
        }
    }
}
