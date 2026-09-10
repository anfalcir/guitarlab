package studio.guitarlab.app

import androidx.compose.ui.test.assertDoesNotExist
import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import studio.guitarlab.core.model.GuitarProject
import studio.guitarlab.core.project.FileProjectRepository

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
            composeRule.onNodeWithText("Criar projeto").assertIsEnabled().performClick()

            // Creation is asynchronous. First prove that the real repository contains the
            // project; this is a stronger and less UI-fragile readiness signal than the
            // project title text exported by Compose to the accessibility hierarchy.
            val persisted = waitForPersistedProject(repository, projectName)
            assertNotNull("Created Studio project must be persisted", persisted)
            projectId = persisted!!.id
            assertEquals(projectName, repository.load(projectId)?.name)

            // Repository persistence completes on IO before the navigation/recomposition callback
            // necessarily becomes observable. Poll through Compose Test itself so the Compose test
            // scheduler can advance while we wait for the loaded Studio semantics node.
            waitForTag("studio-loaded")
            composeRule.onNodeWithContentDescription("Renomear projeto").assertDoesNotExist()

            // Recreate the Activity from Studio. The saveable route must survive Android
            // configuration recreation and still render the persisted project state.
            composeRule.activityRule.scenario.recreate()
            waitForTag("studio-loaded")
            assertEquals(projectName, repository.load(projectId)?.name)

            // Project-scoped Options embeds the project id in the saveable route. Recreate there,
            // then return to Studio. AppRouteCodec JVM tests separately prove exact project-id
            // encode/decode; this path proves Android route save/restore + repository persistence.
            composeRule.onNodeWithTag("studio-options", useUnmergedTree = true)
                .assertIsDisplayed()
                .performClick()
            composeRule.onNodeWithText("Opções").assertIsDisplayed()

            composeRule.activityRule.scenario.recreate()
            composeRule.waitForIdle()
            composeRule.onNodeWithText("Opções").assertIsDisplayed()
            composeRule.onNodeWithContentDescription("Voltar").performClick()
            waitForTag("studio-loaded")
            assertEquals(projectName, repository.load(projectId)?.name)
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

    private fun waitForPersistedProject(repository: FileProjectRepository, name: String): GuitarProject? {
        val deadlineNanos = System.nanoTime() + UI_TIMEOUT_MS * 1_000_000L
        do {
            repository.list().singleOrNull { it.name == name }?.let { return it }
            Thread.sleep(POLL_INTERVAL_MS)
        } while (System.nanoTime() < deadlineNanos)
        return null
    }

    private fun waitForTag(tag: String) {
        composeRule.waitUntil(timeoutMillis = UI_TIMEOUT_MS) {
            runCatching {
                composeRule.onNodeWithTag(tag, useUnmergedTree = true).assertExists()
            }.isSuccess
        }
    }

    private companion object {
        const val UI_TIMEOUT_MS = 10_000L
        const val POLL_INTERVAL_MS = 100L
    }
}
