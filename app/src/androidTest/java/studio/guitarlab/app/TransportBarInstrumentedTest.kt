package studio.guitarlab.app

import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.util.concurrent.atomic.AtomicInteger
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import studio.guitarlab.app.ui.TransportBar
import studio.guitarlab.app.ui.theme.GuitarLabTheme
import studio.guitarlab.core.project.RecordingSessionPhase
import studio.guitarlab.core.project.TransportState

@RunWith(AndroidJUnit4::class)
class TransportBarInstrumentedTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun recordButtonExposesClickActionAndInvokesCallbackExactlyOnce() {
        val recordClicks = AtomicInteger(0)

        composeRule.setContent {
            GuitarLabTheme(darkTheme = false) {
                TransportBar(
                    state = TransportState(loopEnabled = true),
                    engineReady = false,
                    recordEnabled = true,
                    recordingPhase = RecordingSessionPhase.IDLE,
                    canUndo = false,
                    canRedo = false,
                    onReturnToStart = {},
                    onPlayStop = {},
                    onRecord = { recordClicks.incrementAndGet() },
                    onToggleLoop = {},
                    onUndo = {},
                    onRedo = {},
                )
            }
        }

        val record = composeRule.onNodeWithTag("transport-record")
        record.assertIsEnabled().assert(hasClickAction())
        record.performClick()
        composeRule.waitForIdle()

        assertEquals("REC must invoke its callback exactly once", 1, recordClicks.get())
    }
}
