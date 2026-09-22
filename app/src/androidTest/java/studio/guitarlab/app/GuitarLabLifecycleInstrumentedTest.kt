package studio.guitarlab.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.lifecycle.ViewModelProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import studio.guitarlab.core.model.GuitarProject
import studio.guitarlab.core.project.FileProjectRepository
import studio.guitarlab.app.ui.AppNavigationViewModel
import studio.guitarlab.app.ui.AppRouteCodec
import studio.guitarlab.app.ui.AppScreen
import studio.guitarlab.app.ui.HomeViewModel
import studio.guitarlab.app.ui.StudioViewModel
import studio.guitarlab.core.model.AudioTrack
import studio.guitarlab.core.model.ProjectFactory
import studio.guitarlab.core.model.ProjectTemplate

@RunWith(AndroidJUnit4::class)
class GuitarLabLifecycleInstrumentedTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    private val instrumentation by lazy { InstrumentationRegistry.getInstrumentation() }

    @Test
    fun homeHelpUsesTheSharedGuitarLabGuide() {
        waitUntilEnabled("Novo projeto")
        composeRule.onNodeWithContentDescription("Ajuda").assertIsDisplayed().performClick()
        composeRule.onNodeWithText("Guia do GuitarLab").assertIsDisplayed()
        composeRule.onNodeWithText("Preparar").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Atividade, nuvem e backup").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Fechar").performClick()
    }

    @Test
    fun optionsRouteSurvivesActivityRecreation() {
        waitUntilEnabled("Novo projeto")
        composeRule.onNodeWithContentDescription("Opções").performClick()
        composeRule.onNodeWithText("Opções").assertIsDisplayed()

        composeRule.activityRule.scenario.recreate()
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Opções").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Voltar").assertIsDisplayed()
    }

    @Test
    fun newProjectIntentAndNameSurviveActivityRecreation() {
        val projectName = "NewProjectState-${System.nanoTime()}"

        waitUntilEnabled("Novo projeto")
        composeRule.onNodeWithText("Novo projeto").performClick()
        composeRule.onNodeWithTag("new-project-name").performTextInput(projectName)
        composeRule.onNodeWithTag("new-project-import").performClick()
        composeRule.onNodeWithText("Criar e importar").assertIsDisplayed()

        composeRule.activityRule.scenario.recreate()
        composeRule.waitForIdle()

        waitForRoute(AppScreen.NewProject)
        composeRule.onNodeWithTag("new-project-name").assertTextContains(projectName)
        composeRule.onNodeWithText("Criar e importar").assertIsDisplayed()
        composeRule.onNodeWithTag("new-project-create").assertIsEnabled()
    }

    @Test
    fun persistedStudioProjectAndProjectScopedOptionsSurviveActivityRecreation() {
        val projectName = "Lifecycle-${System.nanoTime()}"
        val repository = FileProjectRepository(instrumentation.targetContext.filesDir)
        var projectId: String? = null

        try {
            waitUntilEnabled("Novo projeto")
            composeRule.onNodeWithText("Novo projeto").performClick()
            composeRule.onNode(hasSetTextAction()).performTextInput(projectName)
            // C3 makes the user intent explicit before exposing Studio-only templates. Keep this
            // lifecycle regression focused on route/persistence by choosing Studio + blank project.
            composeRule.onNodeWithTag("new-project-studio").performClick()
            composeRule.onNodeWithText("Projeto vazio").performClick()
            composeRule.onNodeWithTag("new-project-create").assertIsEnabled().performClick()

            // Project creation only invokes navigation after repository.save() succeeds. Prove the
            // same project is durable on disk before asserting the resulting Studio state.
            val persisted = waitForPersistedProject(repository, projectName)
            assertNotNull("Created Studio project must be persisted", persisted)
            projectId = persisted!!.id
            assertEquals(projectName, repository.load(projectId)?.name)

            waitForRoute(AppScreen.Studio(projectId))

            // The saveable project-scoped route must survive Android Activity recreation and reload
            // enough persisted state for both the title and project-only export action to return.
            composeRule.activityRule.scenario.recreate()
            waitForRoute(AppScreen.Studio(projectId))
            assertEquals(projectName, repository.load(projectId)?.name)

            // Project-scoped Options embeds the project id in the saveable route. Recreate there,
            // then return to Studio and prove the persisted project is rehydrated again.
            navigation().navigate(AppScreen.Options(projectId))
            waitForRoute(AppScreen.Options(projectId))

            composeRule.activityRule.scenario.recreate()
            waitForRoute(AppScreen.Options(projectId))
            navigation().navigate(AppScreen.Studio(projectId))
            waitForRoute(AppScreen.Studio(projectId))
            assertEquals(projectName, repository.load(projectId)?.name)

            // Return to Home and prove the persisted project list is rehydrated after the
            // lifecycle round trip. Rename behavior was already physically approved in M6.
            navigation().navigate(AppScreen.Home)
            waitForRoute(AppScreen.Home)
            home().refresh()
            composeRule.waitUntil(timeoutMillis = UI_TIMEOUT_MS) {
                home().state.value.projects.any { it.id == projectId }
            }
        } finally {
            projectId?.let { runCatching { repository.delete(it) } }
        }
    }

    @Test
    fun studioExportActionNavigatesToCanonicalExportWorkspace() {
        val repository = FileProjectRepository(instrumentation.targetContext.filesDir)
        val project = ProjectFactory(idGenerator = { "export-route-${System.nanoTime()}" }, clock = { 700L })
            .create("Export route", ProjectTemplate.BLANK)
        repository.save(project)

        try {
            navigation().navigate(AppScreen.Studio(project.id))
            waitForRoute(AppScreen.Studio(project.id))
            composeRule.waitUntil(timeoutMillis = UI_TIMEOUT_MS) { studio().state.value.project?.id == project.id }

            composeRule.onNodeWithTag("project-shell-nav-export").assertIsDisplayed().performClick()
            waitForRoute(AppScreen.Export(project.id))
            composeRule.onNodeWithText("Exportar").assertIsDisplayed()
            composeRule.onNodeWithText("Projeto portátil").assertIsDisplayed()
            composeRule.onNodeWithText("Salvar e exportar").assertDoesNotExist()
        } finally {
            runCatching { repository.delete(project.id) }
        }
    }

    @Test
    fun homeExportActionNavigatesToCanonicalExportWorkspace() {
        val repository = FileProjectRepository(instrumentation.targetContext.filesDir)
        val project = ProjectFactory(idGenerator = { "home-export-route-${System.nanoTime()}" }, clock = { 710L })
            .create("Home export route", ProjectTemplate.BLANK)
        repository.save(project)

        try {
            navigation().navigate(AppScreen.Home)
            waitForRoute(AppScreen.Home)
            home().clearProjectSearchAndFilters()
            home().refresh()
            composeRule.waitUntil(timeoutMillis = UI_TIMEOUT_MS) {
                home().state.value.projects.any { it.id == project.id }
            }

            val menuDescription = "Mais ações de ${project.name}"
            home().updateProjectSearch(project.name)
            composeRule.waitUntil(timeoutMillis = UI_TIMEOUT_MS) {
                home().state.value.projects.map { it.id } == listOf(project.id)
            }
            composeRule.waitUntil(timeoutMillis = UI_TIMEOUT_MS) {
                runCatching { composeRule.onNodeWithContentDescription(menuDescription).assertIsDisplayed() }.isSuccess
            }
            composeRule.onNodeWithContentDescription(menuDescription)
                .assertIsDisplayed()
                .performClick()

            val exportTag = "project-export-${project.id}"
            composeRule.waitUntil(timeoutMillis = UI_TIMEOUT_MS) {
                runCatching { composeRule.onNodeWithTag(exportTag).assertIsDisplayed() }.isSuccess
            }
            composeRule.onNodeWithTag(exportTag).performClick()
            waitForRoute(AppScreen.Export(project.id))
            composeRule.onNodeWithText("Projeto portátil").assertIsDisplayed()
            composeRule.onNodeWithText("Salvar e exportar").assertDoesNotExist()
        } finally {
            home().clearProjectSearchAndFilters()
            runCatching { repository.delete(project.id) }
        }
    }


    @Test
    fun projectSettingsReturnsToExactOriginWorkspaceAfterRecreation() {
        val repository = FileProjectRepository(instrumentation.targetContext.filesDir)
        val project = ProjectFactory(idGenerator = { "settings-origin-${System.nanoTime()}" }, clock = { 720L })
            .create("Settings origin", ProjectTemplate.BLANK)
        repository.save(project)

        try {
            home().refresh()
            composeRule.waitUntil(timeoutMillis = UI_TIMEOUT_MS) {
                home().state.value.projectsById.containsKey(project.id)
            }
            navigation().navigate(AppScreen.Prepare(project.id))
            waitForRoute(AppScreen.Prepare(project.id))
            composeRule.onNodeWithTag("project-shell-settings").assertIsDisplayed().performClick()
            waitForRoute(AppScreen.Options(project.id))

            composeRule.activityRule.scenario.recreate()
            waitForRoute(AppScreen.Options(project.id))
            composeRule.onNodeWithContentDescription("Voltar").assertIsDisplayed().performClick()
            waitForRoute(AppScreen.Prepare(project.id))
            composeRule.onNodeWithTag("project-shell-prepare").assertIsDisplayed()
        } finally {
            runCatching { repository.delete(project.id) }
        }
    }

    @Test
    fun missingProjectRouteShowsDeterministicLibraryFallback() {
        val missingId = "missing-${System.nanoTime()}"
        navigation().navigate(AppScreen.Export(missingId))
        waitForRoute(AppScreen.Export(missingId))
        composeRule.waitUntil(timeoutMillis = UI_TIMEOUT_MS) {
            runCatching { composeRule.onNodeWithTag("project-route-missing").assertIsDisplayed() }.isSuccess
        }
        composeRule.onNodeWithText("Voltar aos projetos").assertIsDisplayed().performClick()
        waitForRoute(AppScreen.Home)
    }

    @Test
    fun returningToSameStudioProjectKeepsResidentStateAndUndoHistory() {
        val repository = FileProjectRepository(instrumentation.targetContext.filesDir)
        val project = ProjectFactory(idGenerator = { "resident-${System.nanoTime()}" }, clock = { 500L })
            .create("Resident Studio", ProjectTemplate.BLANK)
            .copy(tracks = listOf(AudioTrack(id = "resident-track", name = "Resident", order = 0)))
        repository.save(project)

        try {
            navigation().navigate(AppScreen.Studio(project.id))
            waitForRoute(AppScreen.Studio(project.id))
            composeRule.waitUntil(timeoutMillis = UI_TIMEOUT_MS) {
                studio().state.value.project?.id == project.id && !studio().state.value.loading
            }

            studio().toggleTrackMuted("resident-track")
            composeRule.waitUntil(timeoutMillis = UI_TIMEOUT_MS) { studio().state.value.canUndo }
            val residentProject = studio().state.value.project

            navigation().navigate(AppScreen.Options(project.id))
            waitForRoute(AppScreen.Options(project.id))
            navigation().navigate(AppScreen.Studio(project.id))
            waitForRoute(AppScreen.Studio(project.id))
            composeRule.waitUntil(timeoutMillis = UI_TIMEOUT_MS) {
                studio().state.value.project?.id == project.id && !studio().state.value.loading
            }

            assertTrue("Returning to the same resident Studio project must preserve Undo history", studio().state.value.canUndo)
            assertTrue("Resident project state must not be replaced by a reload", studio().state.value.project === residentProject)
        } finally {
            runCatching { repository.delete(project.id) }
        }
    }

    private fun waitUntilEnabled(text: String) {
        composeRule.waitUntil(timeoutMillis = UI_TIMEOUT_MS) {
            runCatching {
                composeRule.onNodeWithText(text).assertIsEnabled()
            }.isSuccess
        }
    }

    private fun navigation(): AppNavigationViewModel {
        lateinit var result: AppNavigationViewModel
        composeRule.activityRule.scenario.onActivity { activity ->
            result = ViewModelProvider(activity)[AppNavigationViewModel::class.java]
        }
        return result
    }

    private fun studio(): StudioViewModel {
        lateinit var result: StudioViewModel
        composeRule.activityRule.scenario.onActivity { activity ->
            result = ViewModelProvider(activity)[StudioViewModel::class.java]
        }
        return result
    }

    private fun home(): HomeViewModel {
        lateinit var result: HomeViewModel
        composeRule.activityRule.scenario.onActivity { activity ->
            result = ViewModelProvider(activity)[HomeViewModel::class.java]
        }
        return result
    }

    private fun waitForRoute(expected: AppScreen) {
        composeRule.waitUntil(timeoutMillis = UI_TIMEOUT_MS) {
            AppRouteCodec.decode(navigation().persistedRoute.value) == expected
        }
    }

    private fun waitForPersistedProject(repository: FileProjectRepository, name: String): GuitarProject? {
        var persisted: GuitarProject? = null
        composeRule.waitUntil(timeoutMillis = UI_TIMEOUT_MS) {
            repository.list().singleOrNull { it.name == name }
                ?.also { persisted = it } != null
        }
        return persisted
    }

    private companion object {
        const val UI_TIMEOUT_MS = 20_000L
    }
}
