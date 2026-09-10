package studio.guitarlab.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun GuitarLabApp(homeViewModel: HomeViewModel = viewModel()) {
    var persistedRoute by rememberSaveable { mutableStateOf(AppRouteCodec.encode(AppScreen.Home)) }
    val screen = AppRouteCodec.decode(persistedRoute)

    fun navigate(destination: AppScreen) {
        persistedRoute = AppRouteCodec.encode(destination)
    }

    fun returnTo(projectId: String?) {
        navigate(projectId?.let { AppScreen.Studio(it) } ?: AppScreen.Home)
    }

    when (val current = screen) {
        AppScreen.Home -> HomeScreen(
            viewModel = homeViewModel,
            onNewProject = { navigate(AppScreen.NewProject) },
            onOpenProject = { navigate(AppScreen.Studio(it)) },
            onSettings = { navigate(AppScreen.Options()) },
        )
        AppScreen.NewProject -> NewProjectScreen(
            onBack = { navigate(AppScreen.Home) },
            onCreate = { name, template ->
                homeViewModel.createProject(name, template) { projectId ->
                    navigate(AppScreen.Studio(projectId))
                }
            },
        )
        is AppScreen.Studio -> StudioShellScreen(
            projectId = current.projectId,
            onBack = {
                homeViewModel.refresh()
                navigate(AppScreen.Home)
            },
            onOptions = { navigate(AppScreen.Options(current.projectId)) },
        )
        is AppScreen.Options -> SettingsScreen(
            projectId = current.projectId,
            onBack = { returnTo(current.projectId) },
            onAudioDiagnostics = { navigate(AppScreen.AudioProbe(current.projectId)) },
            onCodecDiagnostics = { navigate(AppScreen.CodecProbe(current.projectId)) },
        )
        is AppScreen.AudioProbe -> AudioProbeScreen(onBack = { navigate(AppScreen.Options(current.projectId)) })
        is AppScreen.CodecProbe -> CodecProbeScreen(onBack = { navigate(AppScreen.Options(current.projectId)) })
    }
}
