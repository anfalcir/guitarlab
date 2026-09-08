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

    when (val current = screen) {
        AppScreen.Home -> HomeScreen(
            viewModel = homeViewModel,
            onNewProject = { screen = AppScreen.NewProject },
            onOpenProject = { screen = AppScreen.Studio(it) },
            onSettings = { screen = AppScreen.Settings }
        )
        AppScreen.NewProject -> NewProjectScreen(
            onBack = { screen = AppScreen.Home },
            onCreate = { name, template ->
                homeViewModel.createProject(name, template) { projectId ->
                    screen = AppScreen.Studio(projectId)
                }
            }
        )
        is AppScreen.Studio -> StudioPlaceholderScreen(
            projectId = current.projectId,
            onBack = {
                homeViewModel.refresh()
                screen = AppScreen.Home
            }
        )
        AppScreen.Settings -> SettingsScreen(
            onBack = { screen = AppScreen.Home },
            onAudioDiagnostics = { screen = AppScreen.AudioProbe },
            onCodecDiagnostics = { screen = AppScreen.CodecProbe },
        )
        AppScreen.AudioProbe -> AudioProbeScreen(onBack = { screen = AppScreen.Settings })
        AppScreen.CodecProbe -> CodecProbeScreen(onBack = { screen = AppScreen.Settings })
    }
}
