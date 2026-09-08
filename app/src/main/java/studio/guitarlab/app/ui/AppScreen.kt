package studio.guitarlab.app.ui

sealed interface AppScreen {
    data object Home : AppScreen
    data object NewProject : AppScreen
    data class Studio(val projectId: String) : AppScreen
    data object Settings : AppScreen
    data object AudioProbe : AppScreen
}
