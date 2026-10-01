package studio.guitarlab.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.performClick
import androidx.lifecycle.ViewModelProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import studio.guitarlab.app.ui.AppNavigationViewModel
import studio.guitarlab.app.ui.AppScreen
import studio.guitarlab.app.ui.StudioUiPreferencesStore
import studio.guitarlab.core.model.ProjectFactory
import studio.guitarlab.core.model.ProjectTemplate
import studio.guitarlab.core.project.FileProjectRepository

@RunWith(AndroidJUnit4::class)
class StudioMixerTabletInstrumentedTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test
    fun fiveCompleteChannelsAndMasterFitInPinnedTargetTabletWorkspace() {
        if (InstrumentationRegistry.getArguments().getString("targetGeometry") != "true") return
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val preferences = StudioUiPreferencesStore(context)
        val oldVisible = preferences.mixerVisible()
        val oldPinned = preferences.mixerPinned()
        val oldExpanded = preferences.mixerExpanded()
        val repository = FileProjectRepository(context.filesDir)
        val project = ProjectFactory().create("RC23 canais completos", ProjectTemplate.GUITAR)
        repository.save(project)
        try {
            preferences.setMixerVisible(true)
            preferences.setMixerPinned(true)
            preferences.setMixerExpanded(false)
            compose.activityRule.scenario.onActivity { activity ->
                ViewModelProvider(activity)[AppNavigationViewModel::class.java].navigate(AppScreen.Studio(project.id))
            }
            compose.waitUntil(20_000) {
                runCatching { compose.onNodeWithTag("studio-loaded").assertIsDisplayed() }.isSuccess
            }
            // A restored Studio composition may retain workspace preferences from a prior entry;
            // use the actual controls to normalize this test's explicitly requested dock state.
            if (compose.onAllNodesWithTag("mixer-master-strip").fetchSemanticsNodes().isEmpty()) {
                compose.onNodeWithTag("studio-mixer-toggle").performClick()
            }
            val pin = compose.onNodeWithTag("studio-mixer-pin")
            if (pin.fetchSemanticsNode().config[SemanticsProperties.StateDescription] != "Fixado") pin.performClick()
            val height = compose.onNodeWithTag("studio-mixer-height")
            if (height.fetchSemanticsNode().config[SemanticsProperties.StateDescription] != "Compacto") height.performClick()
            compose.waitForIdle()
            val scroller = compose.onNodeWithTag("mixer-track-scroll").fetchSemanticsNode().boundsInRoot
            project.tracks.forEach { track ->
                val bounds = compose.onNodeWithTag("mixer-track-strip-${track.id}").assertIsDisplayed().fetchSemanticsNode().boundsInRoot
                assertTrue("Every default track must fit fully without scrolling", bounds.left >= scroller.left - 1 && bounds.right <= scroller.right + 1)
            }
            val master = compose.onNodeWithTag("mixer-master-strip").fetchSemanticsNode().boundsInRoot
            assertTrue("Master must own a separate non-overlapping column", master.left >= scroller.right)
            // Compose semantics can precede the actual SurfaceFlinger frame during load/toggle.
            // Wait for accessibility/window quiescence before full-display artifact capture.
            UiDevice.getInstance(InstrumentationRegistry.getInstrumentation()).waitForIdle()
            compose.captureCohesionScreenshot("rc23-studio-five-channels-pinned")
            val navbar = compose.onNodeWithTag("studio-workspace-bar").fetchSemanticsNode().boundsInRoot
            compose.onNodeWithTag("studio-mixer-toggle").performClick()
            val closedNavbar = compose.onNodeWithTag("studio-workspace-bar").fetchSemanticsNode().boundsInRoot
            assertTrue("Closing Mixer must not resize or move navbar", navbar == closedNavbar)
            // Compose semantics can precede the actual SurfaceFlinger frame during load/toggle.
            // Wait for accessibility/window quiescence before full-display artifact capture.
            UiDevice.getInstance(InstrumentationRegistry.getInstrumentation()).waitForIdle()
            compose.captureCohesionScreenshot("rc23-studio-mixer-closed")
        } finally {
            preferences.setMixerVisible(oldVisible)
            preferences.setMixerPinned(oldPinned)
            preferences.setMixerExpanded(oldExpanded)
            repository.delete(project.id)
        }
    }
}
