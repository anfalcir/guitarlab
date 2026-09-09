package studio.guitarlab.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun GuitarLabApp(homeViewModel: HomeViewModel = viewModel()) {
    var persistedRoute by rememberSaveable { mutableStateOf(AppScreen.Home.toPersistedRoute()) }
    val screen = appScreenFromPersistedRoute(persistedRoute)

    fun navigate(destination: AppScreen) {
        persistedRoute = destination.toPersistedRoute()
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

private fun AppScreen.toPersistedRoute(): String = when (this) {
    AppScreen.Home -> "home"
    AppScreen.NewProject -> "new"
    is AppScreen.Studio -> "studio:$projectId"
    is AppScreen.Options -> "options:${projectId.orEmpty()}"
    is AppScreen.AudioProbe -> "audio-probe:${projectId.orEmpty()}"
    is AppScreen.CodecProbe -> "codec-probe:${projectId.orEmpty()}"
}

private fun appScreenFromPersistedRoute(route: String): AppScreen = when {
    route == "home" -> AppScreen.Home
    route == "new" -> AppScreen.NewProject
    route.startsWith("studio:") -> route.substringAfter(':').takeIf { it.isNotBlank() }?.let(AppScreen::Studio) ?: AppScreen.Home
    route.startsWith("options:") -> AppScreen.Options(route.substringAfter(':').takeIf { it.isNotBlank() })
    route.startsWith("audio-probe:") -> AppScreen.AudioProbe(route.substringAfter(':').takeIf { it.isNotBlank() })
    route.startsWith("codec-probe:") -> AppScreen.CodecProbe(route.substringAfter(':').takeIf { it.isNotBlank() })
    else -> AppScreen.Home
}
