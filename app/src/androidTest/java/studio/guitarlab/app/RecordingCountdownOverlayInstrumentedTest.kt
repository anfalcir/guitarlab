package studio.guitarlab.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import studio.guitarlab.app.ui.RecordingCountdownOverlay
import studio.guitarlab.app.ui.theme.GuitarLabTheme

@RunWith(AndroidJUnit4::class)
class RecordingCountdownOverlayInstrumentedTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun countdownIsCenteredOverlayAndLargeWithoutParticipatingInWorkspaceLayout() {
        composeRule.setContent {
            GuitarLabTheme(darkTheme = true) {
                RecordingCountdownOverlay(seconds = 3)
            }
        }

        composeRule.onNodeWithTag("recording-countdown-overlay").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Contagem para gravação: 3").assertIsDisplayed()
        val disc = composeRule.onNodeWithTag("recording-countdown-disc")
            .assertIsDisplayed()
            .fetchSemanticsNode()
            .boundsInRoot
        assertTrue("Countdown deve ter presença visual grande", disc.width >= 200f && disc.height >= 200f)
    }
}
