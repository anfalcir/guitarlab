package studio.guitarlab.app.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import studio.guitarlab.core.model.GuitarProject

class AppNavigationViewModel(private val savedStateHandle: SavedStateHandle) : ViewModel() {
    val persistedRoute: StateFlow<String> = savedStateHandle.getStateFlow(
        ROUTE_KEY,
        AppRouteCodec.encode(AppScreen.Home),
    )

    private val _navigationEntry = MutableStateFlow(0L)
    val navigationEntry: StateFlow<Long> = _navigationEntry.asStateFlow()

    fun navigate(destination: AppScreen) {
        val current = AppRouteCodec.decode(persistedRoute.value)
        val destinationWorkspace = destination.projectWorkspaceContextOrNull()

        if (destination is AppScreen.Options && destination.projectId != null) {
            val currentWorkspace = current.projectWorkspaceContextOrNull()
            if (currentWorkspace?.projectId == destination.projectId) {
                savedStateHandle[settingsOriginKey(destination.projectId)] = currentWorkspace.workspace.code
            }
        }

        destinationWorkspace?.let { context ->
            savedStateHandle[lastWorkspaceKey(context.projectId)] = context.workspace.code
        }
        // Bump before publishing the destination so a newly composed workspace sees the
        // navigation-entry token that belongs to this exact transition. This also makes an
        // explicit re-entry into the same encoded route observable.
        _navigationEntry.value = _navigationEntry.value + 1L
        savedStateHandle[ROUTE_KEY] = AppRouteCodec.encode(destination)
    }

    fun destinationForProject(project: GuitarProject): AppScreen {
        val remembered = ProjectWorkspace.fromCode(savedStateHandle[lastWorkspaceKey(project.id)])
        return (remembered ?: UnifiedProjectShellPolicy.initialWorkspace(project)).destination(project.id)
    }

    fun returnFromSettings(projectId: String?): AppScreen {
        if (projectId == null) return AppScreen.Home
        val origin = ProjectWorkspace.fromCode(savedStateHandle[settingsOriginKey(projectId)])
        val remembered = ProjectWorkspace.fromCode(savedStateHandle[lastWorkspaceKey(projectId)])
        return (origin ?: remembered ?: ProjectWorkspace.STUDIO).destination(projectId)
    }

    private fun lastWorkspaceKey(projectId: String) = "project-workspace:$projectId"
    private fun settingsOriginKey(projectId: String) = "settings-origin:$projectId"

    private companion object {
        const val ROUTE_KEY = "app-route"
    }
}
