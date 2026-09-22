package studio.guitarlab.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollToNode
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.ext.junit.runners.AndroidJUnit4
import studio.guitarlab.app.ui.SettingsScreen
import studio.guitarlab.app.ui.theme.GuitarLabTheme

@RunWith(AndroidJUnit4::class)
class SettingsVisualHierarchyInstrumentedTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun mainSettingsKeepsPrimaryActionsCompactAndDiscoverable() {
        composeRule.setContent {
            GuitarLabTheme(darkTheme = true) {
                SettingsScreen(projectId = null, onBack = {}, onAudioDiagnostics = {}, onCodecDiagnostics = {})
            }
        }

        composeRule.onNodeWithTag("settings-content").assertIsDisplayed()
        composeRule.onNodeWithTag("settings-open-calibration").assertIsDisplayed()
        composeRule.onNodeWithTag("settings-refresh-audio-devices").assertIsDisplayed()

        composeRule.onNode(hasScrollAction()).performScrollToNode(hasTestTag("settings-external-control-toggle"))
        composeRule.onNodeWithTag("settings-external-control-toggle").assertIsDisplayed()
        composeRule.onNode(hasScrollAction()).performScrollToNode(hasText("Diagnóstico"))
        composeRule.onNodeWithText("Diagnóstico").assertIsDisplayed()
    }
}
