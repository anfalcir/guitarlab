package studio.guitarlab.app.ui

sealed interface AppScreen {
    data object Home : AppScreen
    data object NewProject : AppScreen
    data class Studio(val projectId: String) : AppScreen
    data class Options(val projectId: String? = null) : AppScreen
    data class AudioProbe(val projectId: String? = null) : AppScreen
    data class CodecProbe(val projectId: String? = null) : AppScreen
}
