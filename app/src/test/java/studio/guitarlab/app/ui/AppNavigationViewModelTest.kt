package studio.guitarlab.app.ui

import androidx.lifecycle.SavedStateHandle
import org.junit.Assert.assertEquals
import org.junit.Test

class AppNavigationViewModelTest {
    @Test
    fun `navigation persists project-scoped route in saved state`() {
        val savedState = SavedStateHandle()
        val first = AppNavigationViewModel(savedState)

        first.navigate(AppScreen.Studio("project / ç ?"))

        val restored = AppNavigationViewModel(savedState)
        assertEquals(
            AppScreen.Studio("project / ç ?"),
            AppRouteCodec.decode(restored.persistedRoute.value),
        )
    }

    @Test
    fun `new controller starts at Home`() {
        val navigation = AppNavigationViewModel(SavedStateHandle())

        assertEquals(AppScreen.Home, AppRouteCodec.decode(navigation.persistedRoute.value))
    }
}
