package studio.guitarlab.app

import android.util.Log
import androidx.compose.ui.geometry.Rect
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
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
        val minimal = mutableStateOf(false)
        compose.setContent {
            GuitarLabTheme(darkTheme = true) {
                StudioWorkspaceBar(
                    auditionMode = mode.value, mixerVisible = visible.value,
                    mixerMinimal = minimal.value,
                    onToggleMixerMode = { minimal.value = !minimal.value },
                    panelContent = { Text(it.label) },
                    transportContent = { Text("Transport", Modifier.testTag("test-transport")) },
                )
            }
        }
        val tags = listOf("studio-action-comparison", "studio-action-timeline", "studio-mixer-mode")
        // Compare in content coordinates: physical scroll position and clipped viewport bounds
        // are not slot positions. Narrow screens intentionally scroll this fixed-width row.
        fun contentBounds(tag: String): Rect {
            val row = compose.onNodeWithTag("studio-workspace-row").fetchSemanticsNode()
            val node = compose.onNodeWithTag(tag).fetchSemanticsNode()
            val x = node.positionInRoot.x - row.positionInRoot.x
            val y = node.positionInRoot.y - row.positionInRoot.y
            return Rect(x, y, x + node.size.width, y + node.size.height)
        }
        fun bounds() = tags.map(::contentBounds)
        val before = bounds()
        val row = compose.onNodeWithTag("studio-workspace-row").fetchSemanticsNode()
        val transport = contentBounds("test-transport")
        val viewport = compose.onNodeWithTag("studio-workspace-viewport").fetchSemanticsNode()
        Log.i("WorkspaceGeometryTest", "viewport=${viewport.size} content=${row.size} transport=$transport")
        org.junit.Assert.assertTrue("Content row must cover the viewport", row.size.width >= viewport.size.width)
        assertEquals("Transport must be centered in the fixed content row", row.size.width / 2f, transport.center.x, 1f)
        compose.onNodeWithTag("studio-mixer-mode").performScrollTo().performClick()
        assertEquals(before, bounds())
        compose.runOnIdle {
            mode.value = GuitarAuditionMode.BOTH
            visible.value = false
            minimal.value = true
        }
        assertEquals(before, bounds())
        compose.onNodeWithTag("studio-mixer-mode").assertIsNotEnabled()
        compose.onNodeWithTag("studio-action-comparison").performScrollTo().performClick()
        compose.onNodeWithTag("studio-panel-comparison").assertIsDisplayed()
        assertEquals(before, bounds())
        assertEquals("Transport slot stays fixed when modes and panels change", transport, contentBounds("test-transport"))
        compose.captureCohesionScreenshot("rc24-navbar-comparison")
    }

    @Test
    fun comparisonRemainsOpenAndTimelineActionsFitWithoutHorizontalScrolling() {
        val mode = mutableStateOf(GuitarAuditionMode.MIXER)
        val project = ProjectFactory().create("Studio menus", ProjectTemplate.BLANK)
        compose.setContent {
            GuitarLabTheme(darkTheme = false) {
                StudioWorkspaceBar(
                    auditionMode = mode.value, mixerVisible = true, mixerMinimal = false,
                    onToggleMixerMode = {}, transportContent = { Text("Transport", Modifier.testTag("test-transport")) },
                    panelContent = { panel ->
                        PracticeControls(
                            project = project, auditionMode = mode.value, suggestions = 0,
                            enabled = true, loopEnabled = false,
                            onAuditionMode = { mode.value = it }, onAddMarker = {}, onAddSection = {},
                            onSuggestSections = {}, onAcceptSections = {}, onDiscardSections = {}, onClearSections = {},
                            onLoopSection = {}, onRemoveMarker = {}, onRemoveSection = {},
                            onOpenLevelAnalysis = {}, panel = panel,
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
        compose.onNodeWithTag("studio-action-timeline").performScrollTo().performClick()
        val menu = compose.onNodeWithTag("studio-panel-timeline").assertIsDisplayed().fetchSemanticsNode().boundsInRoot
        listOf("+ Marcador", "Criar seção do loop", "Auto seções", "Limpar seções").forEach { label ->
            val action = compose.onNodeWithText(label).assertIsDisplayed().fetchSemanticsNode().boundsInRoot
            org.junit.Assert.assertTrue("$label must fit fully in the menu", action.left >= menu.left && action.right <= menu.right)
        }
        compose.onNodeWithText("Criar seção do loop").assertIsNotEnabled()
        compose.onNodeWithText("Defina e ative um loop primeiro").assertIsDisplayed()
        compose.captureCohesionScreenshot("rc24-navbar-timeline")

    }

    @Test
    fun narrowViewportKeepsEverySlotReachableWithoutWrapping() {
        compose.setContent {
            GuitarLabTheme {
                Box(Modifier.width(320.dp)) {
                    StudioWorkspaceBar(
                        auditionMode = GuitarAuditionMode.REFERENCE, mixerVisible = true,
                        mixerMinimal = false,
                        onToggleMixerMode = {},
                        panelContent = { Text(it.label) }, transportContent = { Text("Transport", Modifier.testTag("test-transport")) },
                    )
                }
            }
        }
        val first = compose.onNodeWithTag("studio-action-comparison").performScrollTo().assertIsDisplayed().fetchSemanticsNode().boundsInRoot
        val last = compose.onNodeWithTag("studio-mixer-mode").performScrollTo().assertIsDisplayed().fetchSemanticsNode().boundsInRoot
        assertEquals(first.top, last.top, 1f)
        assertEquals(first.height, last.height, 1f)
    }
}
