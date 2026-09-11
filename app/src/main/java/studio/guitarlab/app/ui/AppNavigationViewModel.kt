package studio.guitarlab.app.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.StateFlow

class AppNavigationViewModel(private val savedStateHandle: SavedStateHandle) : ViewModel() {
    val persistedRoute: StateFlow<String> = savedStateHandle.getStateFlow(
        ROUTE_KEY,
        AppRouteCodec.encode(AppScreen.Home),
    )

    fun navigate(destination: AppScreen) {
        savedStateHandle[ROUTE_KEY] = AppRouteCodec.encode(destination)
    }

    private companion object {
        const val ROUTE_KEY = "app-route"
    }
}
