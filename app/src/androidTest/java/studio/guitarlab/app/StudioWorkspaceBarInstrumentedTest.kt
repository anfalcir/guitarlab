package studio.guitarlab.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import studio.guitarlab.app.ui.PracticeControls
import studio.guitarlab.app.ui.StudioWorkspaceBar
import studio.guitarlab.app.ui.theme.GuitarLabTheme
import studio.guitarlab.core.model.ProjectFactory
import studio.guitarlab.core.model.ProjectTemplate
import studio.guitarlab.core.project.GuitarAuditionMode

@RunWith(AndroidJUnit4::class)
class StudioWorkspaceBarInstrumentedTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun navbarSlotsStayFixedWhileModesMixerAndPanelsChange() {
        val mode = mutableStateOf(GuitarAuditionMode.MIXER)
        val visible = mutableStateOf(true)
        val pinned = mutableStateOf(true)
        val expanded = mutableStateOf(false)
        compose.setContent {
            GuitarLabTheme(darkTheme = true) {
                StudioWorkspaceBar(
                    auditionMode = mode.value, mixerVisible = visible.value,
                    mixerPinned = pinned.value, mixerExpanded = expanded.value,
                    onToggleMixerPin = { pinned.value = !pinned.value },
                    onToggleMixerHeight = { expanded.value = !expanded.value },
                    panelContent = { Text(it.label) },
                    transportContent = { Text("Transport") },
                )
            }
        }
        val tags = listOf("studio-action-comparison", "studio-action-adjustments", "studio-action-timeline", "studio-mixer-pin", "studio-mixer-height")
        fun bounds() = tags.map { tag ->
            compose.onNodeWithTag(tag).performScrollTo().fetchSemanticsNode().boundsInRoot
        }
        val before = bounds()
        compose.runOnIdle {
            mode.value = GuitarAuditionMode.BOTH
            visible.value = false
            pinned.value = false
            expanded.value = true
        }
        assertEquals(before, bounds())
        compose.onNodeWithTag("studio-mixer-pin").assertIsNotEnabled()
        compose.onNodeWithTag("studio-action-comparison").performScrollTo().performClick()
        compose.onNodeWithTag("studio-panel-comparison").assertIsDisplayed()
        assertEquals(before, bounds())
        compose.captureCohesionScreenshot("rc23-navbar-comparison")
    }

    @Test
    fun comparisonRemainsOpenAcrossAuditionChangesAndAdjustmentsUsesExistingAction() {
        val mode = mutableStateOf(GuitarAuditionMode.MIXER)
        var levelClicks = 0
        val project = ProjectFactory().create("Studio menus", ProjectTemplate.BLANK)
        compose.setContent {
            GuitarLabTheme(darkTheme = false) {
                StudioWorkspaceBar(
                    auditionMode = mode.value, mixerVisible = true, mixerPinned = true, mixerExpanded = false,
                    onToggleMixerPin = {}, onToggleMixerHeight = {}, transportContent = { Text("Transport") },
                    panelContent = { panel ->
                        PracticeControls(
                            project = project, auditionMode = mode.value, suggestions = 0,
                            enabled = true, loopEnabled = false,
                            onAuditionMode = { mode.value = it }, onAddMarker = {}, onAddSection = {},
                            onSuggestSections = {}, onAcceptSections = {}, onDiscardSections = {}, onClearSections = {},
                            onLoopSection = {}, onRemoveMarker = {}, onRemoveSection = {},
                            onOpenLevelAnalysis = { levelClicks++ }, panel = panel,
                        )
                    },
                )
            }
        }
        compose.onNodeWithTag("studio-action-comparison").performScrollTo().performClick()
        compose.onNodeWithText("Referência").performScrollTo().performClick()
        compose.onNodeWithText("Minha").performScrollTo().performClick()
        compose.onNodeWithText("Ambas").performScrollTo().performClick()
        assertEquals(GuitarAuditionMode.BOTH, mode.value)
        compose.onNodeWithTag("studio-panel-comparison").assertIsDisplayed()
        // Dismiss through the real platform back action before opening a different anchored menu.
        androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_BACK)
        compose.waitForIdle()
        compose.onNodeWithTag("studio-action-adjustments").performScrollTo().performClick()
        compose.onNodeWithTag("open-all-level-analysis").assertIsDisplayed().performClick()
        assertEquals(1, levelClicks)
    }

    @Test
    fun narrowViewportKeepsEverySlotReachableWithoutWrapping() {
        compose.setContent {
            GuitarLabTheme {
                Box(Modifier.width(320.dp)) {
                    StudioWorkspaceBar(
                        auditionMode = GuitarAuditionMode.REFERENCE, mixerVisible = true,
                        mixerPinned = true, mixerExpanded = false,
                        onToggleMixerPin = {}, onToggleMixerHeight = {},
                        panelContent = { Text(it.label) }, transportContent = { Text("Transport") },
                    )
                }
            }
        }
        val first = compose.onNodeWithTag("studio-action-comparison").performScrollTo().assertIsDisplayed().fetchSemanticsNode().boundsInRoot
        val last = compose.onNodeWithTag("studio-mixer-height").performScrollTo().assertIsDisplayed().fetchSemanticsNode().boundsInRoot
        assertEquals(first.top, last.top, 1f)
        assertEquals(first.height, last.height, 1f)
    }
}
