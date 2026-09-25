package studio.guitarlab.app.ui

import androidx.lifecycle.SavedStateHandle
import org.junit.Assert.assertEquals
import org.junit.Test
import studio.guitarlab.core.model.GuitarProject
import studio.guitarlab.core.model.ProjectTemplate

class AppNavigationViewModelTest {
    private fun project(id: String = "project / ç ?") = GuitarProject(
        id = id,
        name = "Project",
        template = ProjectTemplate.GUITAR,
        createdAtEpochMs = 1,
        updatedAtEpochMs = 2,
    )

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
    fun `last project workspace survives Home and controller recreation`() {
        val savedState = SavedStateHandle()
        val first = AppNavigationViewModel(savedState)
        val project = project()

        first.navigate(AppScreen.Export(project.id))
        first.navigate(AppScreen.Home)

        val restored = AppNavigationViewModel(savedState)
        assertEquals(AppScreen.Export(project.id), restored.destinationForProject(project))
    }

    @Test
    fun `project settings returns to exact originating workspace through nested diagnostics`() {
        val savedState = SavedStateHandle()
        val navigation = AppNavigationViewModel(savedState)
        val projectId = "opaque:project/id?1"

        navigation.navigate(AppScreen.Prepare(projectId))
        navigation.navigate(AppScreen.Options(projectId))
        navigation.navigate(AppScreen.AudioProbe(projectId))
        navigation.navigate(AppScreen.Options(projectId))

        val restored = AppNavigationViewModel(savedState)
        assertEquals(AppScreen.Prepare(projectId), restored.returnFromSettings(projectId))
    }

    @Test
    fun `navigation entry changes even when destination route is identical`() {
        val navigation = AppNavigationViewModel(SavedStateHandle())
        val destination = AppScreen.Studio("same-project")
        val initial = navigation.navigationEntry.value

        navigation.navigate(destination)
        val first = navigation.navigationEntry.value
        navigation.navigate(destination)
        val second = navigation.navigationEntry.value

        assertEquals(initial + 1L, first)
        assertEquals(first + 1L, second)
        assertEquals(destination, AppRouteCodec.decode(navigation.persistedRoute.value))
    }

    @Test
    fun `new controller starts at Home`() {
        val navigation = AppNavigationViewModel(SavedStateHandle())

        assertEquals(AppScreen.Home, AppRouteCodec.decode(navigation.persistedRoute.value))
    }
}
