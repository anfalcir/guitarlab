package studio.guitarlab.app

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import studio.guitarlab.app.ui.ProjectLibraryControls
import studio.guitarlab.app.ui.HomeViewModel
import studio.guitarlab.app.ui.theme.GuitarLabTheme
import studio.guitarlab.core.project.ProjectContentFilter
import studio.guitarlab.core.project.ProjectLibraryQuery
import studio.guitarlab.core.project.ProjectSampleRateFilter
import studio.guitarlab.core.project.ProjectSortOrder
import studio.guitarlab.core.project.ProjectTemplateFilter

@RunWith(AndroidJUnit4::class)
class HomeProjectLibraryInstrumentedTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun searchClearAndFilterControlsRemainExplicitAndAccessible() {
        var query by mutableStateOf(ProjectLibraryQuery())
        composeRule.setContent {
            GuitarLabTheme(darkTheme = true) {
                ProjectLibraryControls(
                    query = query,
                    enabled = true,
                    onSearchChange = { query = query.copy(searchText = it) },
                    onTemplateFilter = { query = query.copy(template = it) },
                    onContentFilter = { query = query.copy(content = it) },
                    onSampleRateFilter = { query = query.copy(sampleRate = it) },
                    onSortOrder = { query = query.copy(sortOrder = it) },
                    onClearFilters = { query = query.clearFilters() },
                )
            }
        }

        composeRule.onNodeWithTag("home-project-search").assertIsDisplayed().performTextInput("Hero")
        composeRule.onNodeWithTag("home-project-search").assertTextContains("Hero")
        composeRule.onNodeWithContentDescription("Limpar pesquisa").assertIsDisplayed().performClick()
        composeRule.onNodeWithContentDescription("Limpar pesquisa").assertDoesNotExist()

        composeRule.onNodeWithTag("home-project-filter").assertIsDisplayed().performClick()
        composeRule.onNodeWithText("Guitarra").assertIsDisplayed().performClick()
        composeRule.onNodeWithTag("home-active-filters").assertIsDisplayed()
        composeRule.onNodeWithText("Filtros: Guitarra").assertIsDisplayed()
        composeRule.onNodeWithTag("home-clear-filters").performClick()
        composeRule.onNodeWithTag("home-active-filters").assertDoesNotExist()
    }

    @Test
    fun sortMenuPublishesChosenOrderInButtonSemantics() {
        var query by mutableStateOf(ProjectLibraryQuery())
        composeRule.setContent {
            GuitarLabTheme(darkTheme = true) {
                ProjectLibraryControls(
                    query = query,
                    enabled = true,
                    onSearchChange = { query = query.copy(searchText = it) },
                    onTemplateFilter = { query = query.copy(template = it) },
                    onContentFilter = { query = query.copy(content = it) },
                    onSampleRateFilter = { query = query.copy(sampleRate = it) },
                    onSortOrder = { query = query.copy(sortOrder = it) },
                    onClearFilters = { query = query.clearFilters() },
                )
            }
        }

        composeRule.onNodeWithTag("home-project-sort").assertIsDisplayed().performClick()
        composeRule.onNodeWithText("Nome A–Z").assertIsDisplayed().performClick()
        composeRule.onNodeWithContentDescription("Ordenar projetos: Nome A–Z").assertIsDisplayed()
    }

    @Test
    fun selectedSortOrderPersistsAcrossHomeViewModelRecreation() {
        val application = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as android.app.Application
        val preferences = application.getSharedPreferences("guitarlab_home_preferences", android.content.Context.MODE_PRIVATE)
        preferences.edit().clear().commit()
        try {
            val first = HomeViewModel(application)
            first.setProjectSortOrder(ProjectSortOrder.NAME_DESC)
            assertEquals(ProjectSortOrder.NAME_DESC, first.state.value.libraryQuery.sortOrder)

            val recreated = HomeViewModel(application)
            assertEquals(ProjectSortOrder.NAME_DESC, recreated.state.value.libraryQuery.sortOrder)
        } finally {
            preferences.edit().clear().commit()
        }
    }

}
