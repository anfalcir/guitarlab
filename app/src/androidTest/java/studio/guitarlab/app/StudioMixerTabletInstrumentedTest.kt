package studio.guitarlab.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.lifecycle.ViewModelProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
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
                runCatching { compose.onNodeWithTag("mixer-master-strip").assertIsDisplayed() }.isSuccess
            }
            val scroller = compose.onNodeWithTag("mixer-track-scroll").fetchSemanticsNode().boundsInRoot
            project.tracks.forEach { track ->
                val bounds = compose.onNodeWithTag("mixer-track-strip-${track.id}").assertIsDisplayed().fetchSemanticsNode().boundsInRoot
                assertTrue("Every default track must fit fully without scrolling", bounds.left >= scroller.left - 1 && bounds.right <= scroller.right + 1)
            }
            val master = compose.onNodeWithTag("mixer-master-strip").fetchSemanticsNode().boundsInRoot
            assertTrue("Master must own a separate non-overlapping column", master.left >= scroller.right)
            captureCohesionScreenshot("rc23-studio-five-channels-pinned")
            val navbar = compose.onNodeWithTag("studio-workspace-bar").fetchSemanticsNode().boundsInRoot
            compose.onNodeWithTag("studio-mixer-toggle").performClick()
            val closedNavbar = compose.onNodeWithTag("studio-workspace-bar").fetchSemanticsNode().boundsInRoot
            assertTrue("Closing Mixer must not resize or move navbar", navbar == closedNavbar)
            captureCohesionScreenshot("rc23-studio-mixer-closed")
        } finally {
            preferences.setMixerVisible(oldVisible)
            preferences.setMixerPinned(oldPinned)
            preferences.setMixerExpanded(oldExpanded)
            repository.delete(project.id)
        }
    }
}
