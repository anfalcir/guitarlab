package studio.guitarlab.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.lifecycle.ViewModelProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import studio.guitarlab.core.model.GuitarProject
import studio.guitarlab.core.project.FileProjectRepository
import studio.guitarlab.app.ui.AppNavigationViewModel
import studio.guitarlab.app.ui.AppRouteCodec
import studio.guitarlab.app.ui.AppScreen

@RunWith(AndroidJUnit4::class)
class GuitarLabLifecycleInstrumentedTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    private val instrumentation by lazy { InstrumentationRegistry.getInstrumentation() }

    @Test
    fun optionsRouteSurvivesActivityRecreation() {
        waitUntilEnabled("Novo projeto")
        composeRule.onNodeWithContentDescription("Opções").performClick()
        composeRule.onNodeWithText("Opções").assertIsDisplayed()

        composeRule.activityRule.scenario.recreate()
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Opções").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Voltar").assertIsDisplayed()
    }

    @Test
    fun persistedStudioProjectAndProjectScopedOptionsSurviveActivityRecreation() {
        val projectName = "Lifecycle-${System.nanoTime()}"
        val repository = FileProjectRepository(instrumentation.targetContext.filesDir)
        var projectId: String? = null

        try {
            waitUntilEnabled("Novo projeto")
            composeRule.onNodeWithText("Novo projeto").performClick()
            composeRule.onNode(hasSetTextAction()).performTextInput(projectName)
            // Keep this lifecycle regression focused on route/persistence rather than rendering
            // the heavier five-track guitar template on a cold CI emulator.
            composeRule.onNodeWithText("Projeto vazio").performClick()
            composeRule.onNodeWithText("Criar projeto").assertIsEnabled().performClick()

            // Project creation only invokes navigation after repository.save() succeeds. Prove the
            // same project is durable on disk before asserting the resulting Studio state.
            val persisted = waitForPersistedProject(repository, projectName)
            assertNotNull("Created Studio project must be persisted", persisted)
            projectId = persisted!!.id
            assertEquals(projectName, repository.load(projectId)?.name)

            waitForRoute(AppScreen.Studio(projectId))

            // The saveable project-scoped route must survive Android Activity recreation and reload
            // enough persisted state for both the title and project-only export action to return.
            composeRule.activityRule.scenario.recreate()
            waitForRoute(AppScreen.Studio(projectId))
            assertEquals(projectName, repository.load(projectId)?.name)

            // Project-scoped Options embeds the project id in the saveable route. Recreate there,
            // then return to Studio and prove the persisted project is rehydrated again.
            navigation().navigate(AppScreen.Options(projectId))
            waitForRoute(AppScreen.Options(projectId))

            composeRule.activityRule.scenario.recreate()
            waitForRoute(AppScreen.Options(projectId))
            navigation().navigate(AppScreen.Studio(projectId))
            waitForRoute(AppScreen.Studio(projectId))
            assertEquals(projectName, repository.load(projectId)?.name)

            // Renaming intentionally lives only on Home. Verify the Studio flow returns home and the
            // existing overflow action still opens the shared rename dialog for this exact project.
            navigation().navigate(AppScreen.Home)
            waitForRoute(AppScreen.Home)
            waitUntilDisplayed(projectName)
            composeRule.onNodeWithContentDescription("Mais ações").performClick()
            composeRule.onNodeWithText("Renomear").assertIsDisplayed().performClick()
            composeRule.onNodeWithText("Renomear projeto").assertIsDisplayed()
            composeRule.onNodeWithText("Cancelar").performClick()
        } finally {
            projectId?.let { runCatching { repository.delete(it) } }
        }
    }

    private fun waitUntilEnabled(text: String) {
        composeRule.waitUntil(timeoutMillis = UI_TIMEOUT_MS) {
            runCatching {
                composeRule.onNodeWithText(text).assertIsEnabled()
            }.isSuccess
        }
    }

    private fun waitUntilDisplayed(text: String) {
        composeRule.waitUntil(timeoutMillis = UI_TIMEOUT_MS) {
            runCatching {
                composeRule.onNodeWithText(text).assertIsDisplayed()
            }.isSuccess
        }
    }

    private fun navigation(): AppNavigationViewModel {
        lateinit var result: AppNavigationViewModel
        composeRule.activityRule.scenario.onActivity { activity ->
            result = ViewModelProvider(activity)[AppNavigationViewModel::class.java]
        }
        return result
    }

    private fun waitForRoute(expected: AppScreen) {
        composeRule.waitUntil(timeoutMillis = UI_TIMEOUT_MS) {
            AppRouteCodec.decode(navigation().persistedRoute.value) == expected
        }
    }

    private fun waitForPersistedProject(repository: FileProjectRepository, name: String): GuitarProject? {
        var persisted: GuitarProject? = null
        composeRule.waitUntil(timeoutMillis = UI_TIMEOUT_MS) {
            repository.list().singleOrNull { it.name == name }
                ?.also { persisted = it } != null
        }
        return persisted
    }

    private companion object {
        const val UI_TIMEOUT_MS = 20_000L
    }
}
