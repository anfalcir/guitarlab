package studio.guitarlab.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import studio.guitarlab.app.ui.SettingsScreen
import studio.guitarlab.app.ui.theme.GuitarLabTheme

@RunWith(AndroidJUnit4::class)
class SettingsCalibrationModalInstrumentedTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun calibrationDetailsStayOutOfMainSettingsUntilDedicatedModalIsOpened() {
        composeRule.setContent {
            GuitarLabTheme(darkTheme = true) {
                SettingsScreen(projectId = null, onBack = {}, onAudioDiagnostics = {}, onCodecDiagnostics = {})
            }
        }

        composeRule.onNodeWithTag("settings-open-calibration").assertIsDisplayed()
        composeRule.onNodeWithText("Entrada da calibração").assertDoesNotExist()
        composeRule.onNodeWithTag("settings-open-calibration").performClick()
        composeRule.onNodeWithTag("settings-calibration-dialog").assertIsDisplayed()
        composeRule.onNodeWithText("Entrada da calibração").assertIsDisplayed()
        composeRule.onNodeWithText("REC continua disponível sem ela", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("Ajuste global para novas takes").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("nunca reposiciona takes já gravadas", substring = true).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("−25 ms").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("−1 ms").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("+1 ms").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("+25 ms").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Zerar ajuste").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("settings-run-digital-timing-verification").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Verificação digital silenciosa").assertIsDisplayed()
        composeRule.onNodeWithTag("settings-run-calibration").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Fechar").assertIsDisplayed().performClick()
        composeRule.onNodeWithTag("settings-calibration-dialog").assertDoesNotExist()
    }
}
