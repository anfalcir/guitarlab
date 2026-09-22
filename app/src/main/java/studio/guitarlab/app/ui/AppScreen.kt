package studio.guitarlab.app.ui

sealed interface AppScreen {
    data object Home : AppScreen
    data object Activity : AppScreen
    data object NewProject : AppScreen
    data class Prepare(val projectId: String) : AppScreen
    data class Studio(val projectId: String) : AppScreen
    data class Export(val projectId: String) : AppScreen
    data class Options(val projectId: String? = null) : AppScreen
    data class AudioProbe(val projectId: String? = null) : AppScreen
    data class CodecProbe(val projectId: String? = null) : AppScreen
    data class Backup(val projectId: String? = null, val returnToHome: Boolean = false) : AppScreen
}

/** Stable string codec used by rememberSaveable so Activity/process recreation can restore navigation. */
object AppRouteCodec {
    fun encode(screen: AppScreen): String = when (screen) {
        AppScreen.Home -> "home"
        AppScreen.Activity -> "activity"
        AppScreen.NewProject -> "new"
        is AppScreen.Prepare -> "prepare:${screen.projectId}"
        is AppScreen.Studio -> "studio:${screen.projectId}"
        is AppScreen.Export -> "export:${screen.projectId}"
        is AppScreen.Options -> "options:${screen.projectId.orEmpty()}"
        is AppScreen.AudioProbe -> "audio-probe:${screen.projectId.orEmpty()}"
        is AppScreen.CodecProbe -> "codec-probe:${screen.projectId.orEmpty()}"
        is AppScreen.Backup -> "${if (screen.returnToHome) "backup-home" else "backup"}:${screen.projectId.orEmpty()}"
    }

    fun decode(route: String): AppScreen = when {
        route == "home" -> AppScreen.Home
        route == "activity" -> AppScreen.Activity
        route == "new" -> AppScreen.NewProject
        route.startsWith("prepare:") -> route.substringAfter(':').takeIf { it.isNotBlank() }?.let(AppScreen::Prepare) ?: AppScreen.Home
        route.startsWith("studio:") -> route.substringAfter(':').takeIf { it.isNotBlank() }?.let(AppScreen::Studio) ?: AppScreen.Home
        route.startsWith("export:") -> route.substringAfter(':').takeIf { it.isNotBlank() }?.let(AppScreen::Export) ?: AppScreen.Home
        route.startsWith("options:") -> AppScreen.Options(route.substringAfter(':').takeIf { it.isNotBlank() })
        route.startsWith("audio-probe:") -> AppScreen.AudioProbe(route.substringAfter(':').takeIf { it.isNotBlank() })
        route.startsWith("codec-probe:") -> AppScreen.CodecProbe(route.substringAfter(':').takeIf { it.isNotBlank() })
        route.startsWith("backup-home:") -> AppScreen.Backup(route.substringAfter(':').takeIf { it.isNotBlank() }, returnToHome = true)
        route.startsWith("backup:") -> AppScreen.Backup(route.substringAfter(':').takeIf { it.isNotBlank() })
        else -> AppScreen.Home
    }
}
