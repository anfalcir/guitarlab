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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import studio.guitarlab.app.ui.AppNavigationViewModel
import studio.guitarlab.app.ui.AppRouteCodec
import studio.guitarlab.app.ui.AppScreen
import studio.guitarlab.app.ui.StudioViewModel
import studio.guitarlab.core.model.GuitarProject
import studio.guitarlab.core.project.FileProjectRepository

@RunWith(AndroidJUnit4::class)
class PracticeWorkflowInstrumentedTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    private val instrumentation by lazy { InstrumentationRegistry.getInstrumentation() }

    @Test
    fun loopRecShowsTransientChoiceAndCancelPreservesLoop() {
        val projectName = "Practice-${System.nanoTime()}"
        val repository = FileProjectRepository(instrumentation.targetContext.filesDir)
        var projectId: String? = null

        try {
            waitUntilEnabled("Novo projeto")
            composeRule.onNodeWithText("Novo projeto").performClick()
            composeRule.onNode(hasSetTextAction()).performTextInput(projectName)
            composeRule.onNodeWithText("Projeto vazio").performClick()
            composeRule.onNodeWithText("Criar projeto").assertIsEnabled().performClick()

            val persisted = waitForPersistedProject(repository, projectName)
            assertNotNull("Practice test project must be persisted", persisted)
            projectId = persisted!!.id
            waitForRoute(AppScreen.Studio(projectId))
            waitForStudioProject(projectId)

            // The modal behavior is the subject of this test. Activate loop through the same
            // ViewModel command used by the toolbar so the test is not coupled to top-bar
            // rendering/timing on a zero-length blank project.
            studio().toggleLoop()
            composeRule.waitUntil(timeoutMillis = UI_TIMEOUT_MS) {
                studio().state.value.transport.loopEnabled
            }
            assertTrue(studio().state.value.transport.loopEnabled)
            waitUntilContentDescriptionEnabled("Gravar")

            composeRule.onNodeWithContentDescription("Gravar").performClick()

            composeRule.onNodeWithText("Gravar com o loop ativo").assertIsDisplayed()
            composeRule.onNodeWithText("Somente o loop").assertIsDisplayed()
            composeRule.onNodeWithText("Desde o início").assertIsDisplayed()
            composeRule.onNodeWithText("Cancelar").assertIsDisplayed().performClick()

            composeRule.waitForIdle()
            assertTrue("Cancel must preserve the active loop", studio().state.value.transport.loopEnabled)
            composeRule.onNodeWithContentDescription("Gravar").assertIsEnabled()
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

    private fun waitUntilContentDescriptionEnabled(description: String) {
        composeRule.waitUntil(timeoutMillis = UI_TIMEOUT_MS) {
            runCatching {
                composeRule.onNodeWithContentDescription(description).assertIsEnabled()
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

    private fun studio(): StudioViewModel {
        lateinit var result: StudioViewModel
        composeRule.activityRule.scenario.onActivity { activity ->
            result = ViewModelProvider(activity)[StudioViewModel::class.java]
        }
        return result
    }

    private fun waitForRoute(expected: AppScreen) {
        composeRule.waitUntil(timeoutMillis = UI_TIMEOUT_MS) {
            AppRouteCodec.decode(navigation().persistedRoute.value) == expected
        }
    }

    private fun waitForStudioProject(projectId: String) {
        composeRule.waitUntil(timeoutMillis = UI_TIMEOUT_MS) {
            studio().state.value.project?.id == projectId && !studio().state.value.loading
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
