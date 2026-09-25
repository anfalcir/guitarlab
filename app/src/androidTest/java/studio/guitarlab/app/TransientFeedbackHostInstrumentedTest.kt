package studio.guitarlab.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import java.util.concurrent.atomic.AtomicInteger
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import studio.guitarlab.app.ui.AppTransientFeedbackHost
import studio.guitarlab.app.ui.TransientFeedbackKind
import studio.guitarlab.app.ui.theme.GuitarLabTheme

class TransientFeedbackHostInstrumentedTest {
    @get:Rule val compose = createComposeRule()

    @Test fun asyncCompletionIsConsumedImmediatelyAndShownOnce() {
        val consumed = AtomicInteger(0)
        compose.setContent {
            GuitarLabTheme {
                AppTransientFeedbackHost(
                    message = "Projeto GuitarLab salvo com sucesso.",
                    kind = TransientFeedbackKind.ASYNC_COMPLETION,
                    onConsumed = { consumed.incrementAndGet() },
                )
            }
        }

        compose.waitUntil(timeoutMillis = 1_000L) { consumed.get() == 1 }
        assertEquals(1, consumed.get())
        compose.onNodeWithText("Projeto GuitarLab salvo com sucesso.").assertIsDisplayed()
    }

    @Test fun operationalStatusIsConsumedWithoutSnackbarNoise() {
        val consumed = AtomicInteger(0)
        compose.setContent {
            GuitarLabTheme {
                AppTransientFeedbackHost(
                    message = "Aquisição iniciada em segundo plano.",
                    kind = TransientFeedbackKind.OPERATIONAL_STATUS,
                    onConsumed = { consumed.incrementAndGet() },
                )
            }
        }

        compose.waitUntil(timeoutMillis = 1_000L) { consumed.get() == 1 }
        assertEquals(1, consumed.get())
        compose.onNodeWithText("Aquisição iniciada em segundo plano.").assertDoesNotExist()
    }
}
