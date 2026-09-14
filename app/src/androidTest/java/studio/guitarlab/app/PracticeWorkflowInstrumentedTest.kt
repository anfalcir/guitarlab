package studio.guitarlab.app

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
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

            composeRule.onNodeWithText("Auto Seções").assertIsEnabled()
            composeRule.onNodeWithText("Criar seção do loop").assertIsNotEnabled()

            // Activate loop through the same ViewModel command used by the toolbar so the test is
            // deterministic even when a blank project has no playable timeline content.
            studio().toggleLoop()
            composeRule.waitUntil(timeoutMillis = UI_TIMEOUT_MS) {
                studio().state.value.transport.loopEnabled
            }
            assertTrue(studio().state.value.transport.loopEnabled)
            composeRule.onNodeWithText("Criar seção do loop").assertIsEnabled()
            waitUntilTagEnabled("transport-record")

            composeRule.onNodeWithTag("transport-record").performClick()

            // AlertDialog is hosted in a separate Android window. Avoid viewport-based
            // assertions here; validate the dialog contract through semantic presence and
            // enabled actions. Target-tablet geometry is covered separately.
            waitUntilExists("Gravar com o loop ativo")
            waitUntilEnabled("Somente o loop")
            waitUntilEnabled("Desde o início")
            waitUntilEnabled("Cancelar")
            composeRule.onNodeWithText("Cancelar").performClick()

            composeRule.waitForIdle()
            assertTrue("Cancel must preserve the active loop", studio().state.value.transport.loopEnabled)
            composeRule.onNodeWithTag("transport-record").assertIsEnabled()
        } finally {
            projectId?.let { runCatching { repository.delete(it) } }
        }
    }

    private fun waitUntilEnabled(text: String) {
        composeRule.waitUntil(timeoutMillis = UI_TIMEOUT_MS) {
            runCatching { composeRule.onNodeWithText(text).assertIsEnabled() }.isSuccess
        }
    }

    private fun waitUntilTagEnabled(tag: String) {
        composeRule.waitUntil(timeoutMillis = UI_TIMEOUT_MS) {
            runCatching { composeRule.onNodeWithTag(tag).assertIsEnabled() }.isSuccess
        }
    }

    private fun waitUntilExists(text: String) {
        composeRule.waitUntil(timeoutMillis = UI_TIMEOUT_MS) {
            runCatching { composeRule.onNodeWithText(text).fetchSemanticsNode() }.isSuccess
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
            repository.list().singleOrNull { it.name == name }?.also { persisted = it } != null
        }
        return persisted
    }

    private companion object {
        const val UI_TIMEOUT_MS = 20_000L
    }
}
