package studio.guitarlab.app

import androidx.compose.ui.test.assertIsDisplayed
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
import studio.guitarlab.app.ui.ProjectDeleteConfirmationDialog
import studio.guitarlab.app.ui.theme.GuitarLabTheme

@RunWith(AndroidJUnit4::class)
class ProjectDeleteConfirmationInstrumentedTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun cancelDoesNotInvokeDestructiveAction() {
        val confirmed = AtomicInteger(0)
        val dismissed = AtomicInteger(0)
        composeRule.setContent {
            GuitarLabTheme(darkTheme = true) {
                ProjectDeleteConfirmationDialog("Projeto Teste", { dismissed.incrementAndGet() }, { confirmed.incrementAndGet() })
            }
        }
        composeRule.onNodeWithTag("home-delete-confirmation").assertIsDisplayed()
        composeRule.onNodeWithText("Excluir projeto?").assertIsDisplayed()
        composeRule.onNodeWithText("Projeto Teste", substring = true).assertIsDisplayed()
        composeRule.onNodeWithTag("home-cancel-delete").performClick()
        assertEquals(0, confirmed.get())
        assertEquals(1, dismissed.get())
    }

    @Test
    fun explicitConfirmationInvokesDestructiveAction() {
        val confirmed = AtomicInteger(0)
        composeRule.setContent {
            GuitarLabTheme(darkTheme = true) {
                ProjectDeleteConfirmationDialog("Projeto Teste", {}, { confirmed.incrementAndGet() })
            }
        }
        assertEquals(0, confirmed.get())
        composeRule.onNodeWithTag("home-confirm-delete").assertIsDisplayed().performClick()
        assertEquals(1, confirmed.get())
    }
    @Test
    fun activeOperationDeletionExplainsCancellationBeforeDelete() {
        composeRule.setContent {
            GuitarLabTheme(darkTheme = true) {
                ProjectDeleteConfirmationDialog(
                    projectName = "Projeto Ativo",
                    onDismiss = {},
                    onConfirm = {},
                    hasActiveOperations = true,
                )
            }
        }
        composeRule.onNodeWithText("operações em andamento serão canceladas", substring = true).assertIsDisplayed()
    }

}
