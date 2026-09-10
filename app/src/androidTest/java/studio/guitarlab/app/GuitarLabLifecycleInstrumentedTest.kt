package studio.guitarlab.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import studio.guitarlab.core.project.FileProjectRepository

@RunWith(AndroidJUnit4::class)
class GuitarLabLifecycleInstrumentedTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    private val instrumentation by lazy { InstrumentationRegistry.getInstrumentation() }
    private val device by lazy { UiDevice.getInstance(instrumentation) }

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

            // Studio is intentionally dynamic and can keep Compose recompositions pending.
            // Observe it through the Android accessibility tree so the test does not require
            // global Compose idleness merely to prove route and persisted-project recovery.
            assertTrue("Studio must show the newly created project", waitForText(projectName))
            val persisted = repository.list().singleOrNull { it.name == projectName }
            assertNotNull("Created Studio project must be persisted", persisted)
            projectId = persisted!!.id
            assertEquals(projectName, repository.load(projectId)?.name)

            // Recreate from Studio. The saveable route must survive and the new StudioViewModel
            // must reload the same persisted project rather than transient composable state.
            composeRule.activityRule.scenario.recreate()
            assertTrue("Studio route must survive Activity recreation", waitForText(projectName))
            assertEquals(projectName, repository.load(projectId)?.name)
            assertTrue("Studio home action must remain exposed", device.hasObject(By.desc("Início")))

            // Project-scoped Options embeds the project id in the saveable route. Navigate using
            // the accessibility tree, recreate there, then return to the exact Studio project.
            val options = device.wait(Until.findObject(By.desc("Opções")), UI_TIMEOUT_MS)
            assertNotNull("Studio must expose Options after recreation", options)
            options!!.click()
            assertTrue("Project-scoped Options must open", waitForText("Opções"))

            composeRule.activityRule.scenario.recreate()
            assertTrue("Project-scoped Options route must survive Activity recreation", waitForText("Opções"))

            val back = device.wait(Until.findObject(By.desc("Voltar")), UI_TIMEOUT_MS)
            assertNotNull("Options must expose Back after recreation", back)
            back!!.click()
            assertTrue("Back from project-scoped Options must restore the same Studio", waitForText(projectName))
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

    private fun waitForText(text: String): Boolean =
        device.wait(Until.hasObject(By.text(text)), UI_TIMEOUT_MS)

    private companion object {
        const val UI_TIMEOUT_MS = 10_000L
    }
}
