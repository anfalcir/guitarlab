package studio.guitarlab.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.util.concurrent.atomic.AtomicInteger
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import studio.guitarlab.app.ui.AutoSectionsSlot
import studio.guitarlab.app.ui.PracticeControls
import studio.guitarlab.app.ui.theme.GuitarLabTheme
import studio.guitarlab.core.model.ProjectFactory
import studio.guitarlab.core.model.ProjectTemplate
import studio.guitarlab.core.project.GuitarAuditionMode

@RunWith(AndroidJUnit4::class)
class AutoSectionsSlotInstrumentedTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun idleStateUsesTheFixedAutoSectionsSlot() {
        val detectClicks = AtomicInteger(0)
        composeRule.setContent {
            GuitarLabTheme(darkTheme = true) {
                AutoSectionsSlot(
                    previewVisible = false,
                    enabled = true,
                    onDetect = { detectClicks.incrementAndGet() },
                    onApply = {},
                    onDiscard = {},
                )
            }
        }

        composeRule.onNodeWithTag("auto-sections-slot").assertIsDisplayed()
        composeRule.onNodeWithTag("auto-sections-detect").assertIsDisplayed().performClick()
        assertTrue(composeRule.onAllNodesWithTag("auto-sections-apply").fetchSemanticsNodes().isEmpty())
        assertTrue(composeRule.onAllNodesWithTag("auto-sections-discard").fetchSemanticsNodes().isEmpty())
        assertEquals(1, detectClicks.get())
    }

    @Test
    fun previewReplacesAutoSectionsInTheExactSameSlotWithApplyAndRedXAction() {
        val preview = mutableStateOf(false)
        val applyClicks = AtomicInteger(0)
        val discardClicks = AtomicInteger(0)
        composeRule.setContent {
            GuitarLabTheme(darkTheme = true) {
                AutoSectionsSlot(
                    previewVisible = preview.value,
                    enabled = true,
                    onDetect = {},
                    onApply = { applyClicks.incrementAndGet() },
                    onDiscard = { discardClicks.incrementAndGet() },
                )
            }
        }

        val idleWidth = composeRule.onNodeWithTag("auto-sections-slot")
            .assertIsDisplayed()
            .fetchSemanticsNode()
            .boundsInRoot.width

        composeRule.runOnIdle { preview.value = true }
        composeRule.waitForIdle()

        val previewWidth = composeRule.onNodeWithTag("auto-sections-slot")
            .assertIsDisplayed()
            .fetchSemanticsNode()
            .boundsInRoot.width
        assertTrue(composeRule.onAllNodesWithTag("auto-sections-detect").fetchSemanticsNodes().isEmpty())
        composeRule.onNodeWithTag("auto-sections-apply").assertIsDisplayed()
        composeRule.onNodeWithText("Aplicar").performClick()
        composeRule.onNodeWithContentDescription("Descartar prévia de seções").assertIsDisplayed().performClick()

        assertEquals(idleWidth, previewWidth, 0.5f)
        assertEquals(1, applyClicks.get())
        assertEquals(1, discardClicks.get())
    }
    @Test
    fun dockedPracticeControlsKeepAllSemanticGroupsVisibleOnNarrowViewport() {
        val project = ProjectFactory(idGenerator = { "practice-narrow" }, clock = { 1L })
            .create("Practice narrow", ProjectTemplate.BLANK)
        val levelClicks = AtomicInteger(0)
        composeRule.setContent {
            GuitarLabTheme(darkTheme = true) {
                PracticeControls(
                    project = project,
                    auditionMode = GuitarAuditionMode.MIXER,
                    suggestions = 0,
                    enabled = true,
                    loopEnabled = false,
                    onAuditionMode = {},
                    onAddMarker = {},
                    onAddSection = {},
                    onSuggestSections = {},
                    onAcceptSections = {},
                    onDiscardSections = {},
                    onClearSections = {},
                    onLoopSection = {},
                    onRemoveMarker = {},
                    onRemoveSection = {},
                    onOpenLevelAnalysis = { levelClicks.incrementAndGet() },
                    docked = true,
                )
            }
        }

        composeRule.onNodeWithTag("mixer-practice-segmented-bar").assertIsDisplayed()
        composeRule.onNodeWithText("Desativado").assertIsDisplayed()
        composeRule.onNodeWithText("Ajustes").assertIsDisplayed()
        composeRule.onNodeWithText("Timeline").assertIsDisplayed()
        composeRule.onNodeWithTag("open-all-level-analysis").assertIsDisplayed().performClick()

        val comparisonBounds = composeRule.onNodeWithTag("practice-comparison-segment")
            .assertIsDisplayed().fetchSemanticsNode().boundsInRoot
        val adjustmentsBounds = composeRule.onNodeWithTag("practice-adjustments-segment")
            .assertIsDisplayed().fetchSemanticsNode().boundsInRoot
        val timelineBounds = composeRule.onNodeWithTag("practice-timeline-segment")
            .assertIsDisplayed().fetchSemanticsNode().boundsInRoot
        val levelButtonBounds = composeRule.onNodeWithTag("open-all-level-analysis")
            .assertIsDisplayed().fetchSemanticsNode().boundsInRoot

        assertTrue("Ajustes should stack below Comparação on narrow docked widths", adjustmentsBounds.top >= comparisonBounds.bottom - 2f)
        assertTrue("Timeline should stack below Ajustes on narrow docked widths", timelineBounds.top >= adjustmentsBounds.bottom - 2f)
        assertTrue("Níveis must stay fully inside Ajustes", levelButtonBounds.left >= adjustmentsBounds.left && levelButtonBounds.right <= adjustmentsBounds.right)
        assertEquals(1, levelClicks.get())
    }

    @Test
    fun dockedPracticeControlsContainComparisonAndAdjustmentsAtTargetTabletLogicalWidth() {
        val project = ProjectFactory(idGenerator = { "practice-wide" }, clock = { 1L })
            .create("Practice wide", ProjectTemplate.BLANK)
        val levelClicks = AtomicInteger(0)
        composeRule.setContent {
            // The default CI device is phone-shaped. Override only Compose density so the same
            // physical root exercises the ~1280dp landscape width used by the 1920x1200/240dpi
            // target-tablet gate without depending on emulator window reconfiguration here.
            CompositionLocalProvider(LocalDensity provides Density(density = 0.84f, fontScale = 1f)) {
                GuitarLabTheme(darkTheme = true) {
                    PracticeControls(
                        project = project,
                        auditionMode = GuitarAuditionMode.MIXER,
                        suggestions = 0,
                        enabled = true,
                        loopEnabled = false,
                        onAuditionMode = {},
                        onAddMarker = {},
                        onAddSection = {},
                        onSuggestSections = {},
                        onAcceptSections = {},
                        onDiscardSections = {},
                        onClearSections = {},
                        onLoopSection = {},
                        onRemoveMarker = {},
                        onRemoveSection = {},
                        onOpenLevelAnalysis = { levelClicks.incrementAndGet() },
                        docked = true,
                    )
                }
            }
        }

        val comparisonBounds = composeRule.onNodeWithTag("practice-comparison-segment")
            .assertIsDisplayed().fetchSemanticsNode().boundsInRoot
        val adjustmentsBounds = composeRule.onNodeWithTag("practice-adjustments-segment")
            .assertIsDisplayed().fetchSemanticsNode().boundsInRoot
        val timelineBounds = composeRule.onNodeWithTag("practice-timeline-segment")
            .assertIsDisplayed().fetchSemanticsNode().boundsInRoot
        val comparisonTitleBounds = composeRule.onNodeWithText("Comparação")
            .assertIsDisplayed().fetchSemanticsNode().boundsInRoot
        val adjustmentsTitleBounds = composeRule.onNodeWithText("Ajustes")
            .assertIsDisplayed().fetchSemanticsNode().boundsInRoot
        val timelineTitleBounds = composeRule.onNodeWithText("Timeline")
            .assertIsDisplayed().fetchSemanticsNode().boundsInRoot
        val disabledButtonBounds = composeRule.onNodeWithText("Desativado")
            .assertIsDisplayed().fetchSemanticsNode().boundsInRoot
        val levelButtonBounds = composeRule.onNodeWithTag("open-all-level-analysis")
            .assertIsDisplayed().fetchSemanticsNode().boundsInRoot
        val markerButtonBounds = composeRule.onNodeWithText("+ Marcador")
            .assertIsDisplayed().fetchSemanticsNode().boundsInRoot

        assertTrue("Comparação chassis must end before Ajustes begins", comparisonBounds.right <= adjustmentsBounds.left + 2f)
        assertTrue("Ajustes chassis must end before Timeline begins", adjustmentsBounds.right <= timelineBounds.left + 2f)
        assertTrue("Comparação title must read as a header before its first action", comparisonTitleBounds.right < disabledButtonBounds.left)
        assertTrue("Ajustes title must read as a header before Níveis", adjustmentsTitleBounds.right < levelButtonBounds.left)
        assertTrue("Timeline title must read as a header before its first action", timelineTitleBounds.right < markerButtonBounds.left)
        assertTrue("Níveis must stay fully inside Ajustes", levelButtonBounds.left >= adjustmentsBounds.left && levelButtonBounds.right <= adjustmentsBounds.right)
        listOf("Desativado", "Referência", "Minha", "Ambas").forEach { label ->
            val buttonBounds = composeRule.onNodeWithText(label).assertIsDisplayed().fetchSemanticsNode().boundsInRoot
            assertTrue("$label must start inside Comparação", buttonBounds.left >= comparisonBounds.left - 1f)
            assertTrue("$label must be fully contained by Comparação", buttonBounds.right <= comparisonBounds.right + 1f)
        }
        val contentLeft = minOf(adjustmentsTitleBounds.left, levelButtonBounds.left)
        val contentRight = maxOf(adjustmentsTitleBounds.right, levelButtonBounds.right)
        val leftInset = contentLeft - adjustmentsBounds.left
        val rightInset = adjustmentsBounds.right - contentRight
        assertEquals(
            "Ajustes + Níveis must remain visually centered in the wide docked segment",
            leftInset,
            rightInset,
            6f,
        )
        assertEquals(0, levelClicks.get())
    }

}
