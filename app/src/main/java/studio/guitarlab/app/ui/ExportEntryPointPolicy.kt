package studio.guitarlab.app.ui

/** Canonical destination for every external-export entry point. */
object ExportEntryPointPolicy {
    fun destination(projectId: String): AppScreen = AppScreen.Export(projectId)
}
