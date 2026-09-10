package studio.guitarlab.app.ui

sealed interface AppScreen {
    data object Home : AppScreen
    data object NewProject : AppScreen
    data class Studio(val projectId: String) : AppScreen
    data class Options(val projectId: String? = null) : AppScreen
    data class AudioProbe(val projectId: String? = null) : AppScreen
    data class CodecProbe(val projectId: String? = null) : AppScreen
}

/** Stable string codec used by rememberSaveable so Activity/process recreation can restore navigation. */
object AppRouteCodec {
    fun encode(screen: AppScreen): String = when (screen) {
        AppScreen.Home -> "home"
        AppScreen.NewProject -> "new"
        is AppScreen.Studio -> "studio:${screen.projectId}"
        is AppScreen.Options -> "options:${screen.projectId.orEmpty()}"
        is AppScreen.AudioProbe -> "audio-probe:${screen.projectId.orEmpty()}"
        is AppScreen.CodecProbe -> "codec-probe:${screen.projectId.orEmpty()}"
    }

    fun decode(route: String): AppScreen = when {
        route == "home" -> AppScreen.Home
        route == "new" -> AppScreen.NewProject
        route.startsWith("studio:") -> route.substringAfter(':').takeIf { it.isNotBlank() }?.let(AppScreen::Studio) ?: AppScreen.Home
        route.startsWith("options:") -> AppScreen.Options(route.substringAfter(':').takeIf { it.isNotBlank() })
        route.startsWith("audio-probe:") -> AppScreen.AudioProbe(route.substringAfter(':').takeIf { it.isNotBlank() })
        route.startsWith("codec-probe:") -> AppScreen.CodecProbe(route.substringAfter(':').takeIf { it.isNotBlank() })
        else -> AppScreen.Home
    }
}
