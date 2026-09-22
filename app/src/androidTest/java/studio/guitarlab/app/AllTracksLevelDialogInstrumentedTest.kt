package studio.guitarlab.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.util.concurrent.atomic.AtomicInteger
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import studio.guitarlab.app.ui.AllTracksLevelDialog
import studio.guitarlab.app.ui.theme.GuitarLabTheme
import studio.guitarlab.core.model.AudioClip
import studio.guitarlab.core.model.AudioTrack
import studio.guitarlab.core.model.ProjectFactory
import studio.guitarlab.core.model.ProjectTemplate
import studio.guitarlab.core.project.LevelAnalysis

@RunWith(AndroidJUnit4::class)
class AllTracksLevelDialogInstrumentedTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun modalSupportsGlobalAndPerTrackAnalysisWithoutHidingTrackState() {
        val analyzeAll = AtomicInteger(0)
        val applyAll = AtomicInteger(0)
        val analyzeTrack = AtomicInteger(0)
        val applyTrack = AtomicInteger(0)
        val project = ProjectFactory(idGenerator = { "levels-${System.nanoTime()}" }, clock = { 1L })
            .create("Levels", ProjectTemplate.BLANK)
            .copy(
                tracks = listOf(
                    AudioTrack(id = "l1", name = "Base", gainDb = -3f, order = 0),
                    AudioTrack(id = "l2", name = "Guitar", gainDb = 1f, order = 1),
                ),
                clips = listOf(
                    AudioClip(id = "c1", trackId = "l1", name = "Base", sourceUri = "managed://base.wav", startFrame = 0, lengthFrames = 48_000),
                    AudioClip(id = "c2", trackId = "l2", name = "Guitar", sourceUri = "managed://guitar.wav", startFrame = 0, lengthFrames = 48_000),
                ),
            )
        val analyses = mapOf(
            "l1" to LevelAnalysis(peakDbfs = -6f, rmsDbfs = -24f, recommendedGainDb = 6f, clipped = false, silent = false),
            "l2" to LevelAnalysis(peakDbfs = -4f, rmsDbfs = -18f, recommendedGainDb = 0f, clipped = false, silent = false),
        )

        composeRule.setContent {
            GuitarLabTheme(darkTheme = true) {
                AllTracksLevelDialog(
                    project = project,
                    analyses = analyses,
                    busyTrackIds = emptySet(),
                    onAnalyzeTrack = { analyzeTrack.incrementAndGet() },
                    onAnalyzeAll = { analyzeAll.incrementAndGet() },
                    onApplyTrack = { applyTrack.incrementAndGet() },
                    onApplyAll = { applyAll.incrementAndGet() },
                    onDismiss = {},
                )
            }
        }

        composeRule.onNodeWithTag("all-levels-dialog").assertIsDisplayed()
        composeRule.onNodeWithTag("analyze-all-levels").assertIsEnabled().performClick()
        composeRule.onNodeWithTag("apply-all-levels").assertIsEnabled().performClick()
        composeRule.onNodeWithTag("all-level-row-l1").assertIsDisplayed()
        composeRule.onNodeWithText("Aplicar").assertIsDisplayed().performClick()
        composeRule.onNodeWithText("Dentro do alvo").assertIsDisplayed()

        assertEquals(1, analyzeAll.get())
        assertEquals(1, applyAll.get())
        assertEquals(1, applyTrack.get())
        assertEquals(0, analyzeTrack.get())
    }
}
