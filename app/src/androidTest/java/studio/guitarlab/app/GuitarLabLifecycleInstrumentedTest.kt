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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
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

            // Creation is asynchronous. First prove that the real repository contains the
            // project; this is a stronger and less UI-fragile readiness signal than the
            // project title text exported by Compose to the accessibility hierarchy.
            val persisted = waitForPersistedProject(repository, projectName)
            assertNotNull("Created Studio project must be persisted", persisted)
            projectId = persisted!!.id
            assertEquals(projectName, repository.load(projectId)?.name)

            // Studio intentionally has continuously changing UI (meters/clock), so Compose/Espresso
            // may never become globally idle. Observe the exported test tag through UiAutomator,
            // which validates the real accessibility tree without requiring Compose idleness.
            assertTrue("Studio must load the persisted project", waitForResource("studio-loaded"))
            assertFalse("Rename must stay out of Studio", device.hasObject(By.desc("Renomear projeto")))

            // Recreate the Activity from Studio. The saveable route must survive Android
            // configuration recreation and still render the persisted project state.
            composeRule.activityRule.scenario.recreate()
            assertTrue("Studio route must survive Activity recreation", waitForResource("studio-loaded"))
            assertEquals(projectName, repository.load(projectId)?.name)

            // Project-scoped Options embeds the project id in the saveable route. Recreate there,
            // then return to Studio. AppRouteCodec JVM tests separately prove exact project-id
            // encode/decode; this path proves Android route save/restore + repository persistence.
            val options = device.wait(Until.findObject(By.res("studio-options")), UI_TIMEOUT_MS)
            assertNotNull("Studio must expose Options after recreation", options)
            options!!.click()
            composeRule.onNodeWithText("Opções").assertIsDisplayed()

            composeRule.activityRule.scenario.recreate()
            composeRule.waitForIdle()
            composeRule.onNodeWithText("Opções").assertIsDisplayed()
            composeRule.onNodeWithContentDescription("Voltar").performClick()
            assertTrue("Back from project-scoped Options must restore Studio", waitForResource("studio-loaded"))
            assertEquals(projectName, repository.load(projectId)?.name)

            // Renaming intentionally lives only on Home. Prove the Studio pencil/control is absent
            // while the Home overflow action remains available and opens the shared dialog.
            val home = device.wait(Until.findObject(By.desc("Início")), UI_TIMEOUT_MS)
            assertNotNull("Studio must expose Home navigation", home)
            home!!.click()
            composeRule.onNodeWithText(projectName).assertIsDisplayed()
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

    private fun waitForPersistedProject(repository: FileProjectRepository, name: String): GuitarProject? {
        val deadlineNanos = System.nanoTime() + UI_TIMEOUT_MS * 1_000_000L
        do {
            repository.list().singleOrNull { it.name == name }?.let { return it }
            Thread.sleep(POLL_INTERVAL_MS)
        } while (System.nanoTime() < deadlineNanos)
        return null
    }

    private fun waitForResource(resourceName: String): Boolean =
        device.wait(Until.hasObject(By.res(resourceName)), UI_TIMEOUT_MS)

    private companion object {
        const val UI_TIMEOUT_MS = 10_000L
        const val POLL_INTERVAL_MS = 100L
    }
}
