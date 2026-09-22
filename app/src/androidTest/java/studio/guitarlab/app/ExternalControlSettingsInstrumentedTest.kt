package studio.guitarlab.app

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import studio.guitarlab.app.ui.SettingsScreen
import studio.guitarlab.app.ui.theme.GuitarLabTheme
import studio.guitarlab.core.project.ExternalControlAction

@RunWith(AndroidJUnit4::class)
class ExternalControlSettingsInstrumentedTest {
    @get:Rule val composeRule = createComposeRule()

    @Before
    fun resetExternalControlPreferences() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        context.getSharedPreferences("external-control-v1", Context.MODE_PRIVATE).edit().clear().commit()
    }

    @Test
    fun externalControlIsOptInAndLearnControlsHaveVisibleSemantics() {
        composeRule.setContent {
            GuitarLabTheme(darkTheme = true) {
                SettingsScreen(projectId = null, onBack = {}, onAudioDiagnostics = {}, onCodecDiagnostics = {})
            }
        }

        composeRule.onNode(hasScrollAction()).performScrollToNode(hasTestTag("settings-external-control-toggle"))
        composeRule.onNodeWithText("Desativado por padrão", substring = true).assertIsDisplayed()
        composeRule.onNodeWithTag("settings-external-control-toggle").assertIsDisplayed().performClick()
        composeRule.onNode(hasScrollAction()).performScrollToNode(hasTestTag("settings-external-hid-toggle"))
        composeRule.onNodeWithTag("settings-external-hid-toggle").assertIsDisplayed()
        ExternalControlAction.entries.forEach { action ->
            val tag = "settings-external-learn-${action.name}"
            composeRule.onNode(hasScrollAction()).performScrollToNode(hasTestTag(tag))
            composeRule.onNodeWithTag(tag).assertIsDisplayed()
        }
    }
}
