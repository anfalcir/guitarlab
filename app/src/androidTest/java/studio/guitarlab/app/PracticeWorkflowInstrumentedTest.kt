package studio.guitarlab.app

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.ViewModelProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import studio.guitarlab.app.ui.AppNavigationViewModel
import studio.guitarlab.app.ui.AppRouteCodec
import studio.guitarlab.app.ui.AppScreen
import studio.guitarlab.app.ui.StudioViewModel
import studio.guitarlab.core.model.ProjectFactory
import studio.guitarlab.core.model.ProjectTemplate
import studio.guitarlab.core.project.FileProjectRepository
import studio.guitarlab.core.project.RecordingSessionPhase

@RunWith(AndroidJUnit4::class)
class PracticeWorkflowInstrumentedTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    private val instrumentation by lazy { InstrumentationRegistry.getInstrumentation() }

    @Test
    fun loopRecShowsTransientChoiceAndCancelPreservesLoop() {
        val repository = FileProjectRepository(instrumentation.targetContext.filesDir)
        val project = ProjectFactory().create("Practice-${System.nanoTime()}", ProjectTemplate.BLANK)
        repository.save(project)

        try {
            // This regression owns the Loop -> REC contract. Project creation/lifecycle have their
            // own instrumentation coverage, so enter Studio directly and avoid coupling this test
            // to the Android IME opened by the New Project name field.
            navigation().navigate(AppScreen.Studio(project.id))
            waitForRoute(AppScreen.Studio(project.id))
            waitForStudioProject(project.id)
            composeRule.waitForIdle()

            assertEquals(RecordingSessionPhase.IDLE, studio().state.value.recordingSession.phase)
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

            // AlertDialog is hosted in a separate Android window. Validate its semantic contract;
            // target-tablet geometry is covered independently by TargetTabletGeometryInstrumentedTest.
            waitUntilExists("Gravar com o loop ativo")
            waitUntilEnabled("Somente o loop")
            waitUntilEnabled("Desde o início")
            waitUntilEnabled("Cancelar")
            composeRule.onNodeWithText("Cancelar").performClick()

            composeRule.waitForIdle()
            assertTrue("Cancel must preserve the active loop", studio().state.value.transport.loopEnabled)
            assertEquals(RecordingSessionPhase.IDLE, studio().state.value.recordingSession.phase)
            composeRule.onNodeWithTag("transport-record").assertIsEnabled()
        } finally {
            runCatching { repository.delete(project.id) }
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

    private companion object {
        const val UI_TIMEOUT_MS = 20_000L
    }
}
