package studio.guitarlab.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun GuitarLabApp(homeViewModel: HomeViewModel = viewModel()) {
    var screen: AppScreen by remember { mutableStateOf(AppScreen.Home) }

    fun returnTo(projectId: String?) {
        screen = projectId?.let { AppScreen.Studio(it) } ?: AppScreen.Home
    }

    when (val current = screen) {
        AppScreen.Home -> HomeScreen(
            viewModel = homeViewModel,
            onNewProject = { screen = AppScreen.NewProject },
            onOpenProject = { screen = AppScreen.Studio(it) },
            onSettings = { screen = AppScreen.Options() },
        )
        AppScreen.NewProject -> NewProjectScreen(
            onBack = { screen = AppScreen.Home },
            onCreate = { name, template ->
                homeViewModel.createProject(name, template) { projectId ->
                    screen = AppScreen.Studio(projectId)
                }
            },
        )
        is AppScreen.Studio -> StudioShellScreen(
            projectId = current.projectId,
            onBack = {
                homeViewModel.refresh()
                screen = AppScreen.Home
            },
            onOptions = { screen = AppScreen.Options(current.projectId) },
        )
        is AppScreen.Options -> SettingsScreen(
            projectId = current.projectId,
            onBack = { returnTo(current.projectId) },
            onAudioDiagnostics = { screen = AppScreen.AudioProbe(current.projectId) },
            onCodecDiagnostics = { screen = AppScreen.CodecProbe(current.projectId) },
        )
        is AppScreen.AudioProbe -> AudioProbeScreen(onBack = { screen = AppScreen.Options(current.projectId) })
        is AppScreen.CodecProbe -> CodecProbeScreen(onBack = { screen = AppScreen.Options(current.projectId) })
    }
}
