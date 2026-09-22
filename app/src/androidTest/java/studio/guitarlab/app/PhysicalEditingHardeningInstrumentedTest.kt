package studio.guitarlab.app

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.lifecycle.ViewModelProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import studio.guitarlab.app.ui.AppNavigationViewModel
import studio.guitarlab.app.ui.AppRouteCodec
import studio.guitarlab.app.ui.AppScreen
import studio.guitarlab.app.ui.StudioViewModel
import studio.guitarlab.core.codec.WaveformEnvelope
import studio.guitarlab.core.model.AudioClip
import studio.guitarlab.core.model.AudioTrack
import studio.guitarlab.core.model.ProjectFactory
import studio.guitarlab.core.model.ProjectTemplate
import studio.guitarlab.core.project.FileProjectRepository
import studio.guitarlab.core.project.TimelineControlPolicy
import studio.guitarlab.core.project.WaveformCacheStore

@RunWith(AndroidJUnit4::class)
class PhysicalEditingHardeningInstrumentedTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    private val instrumentation by lazy { InstrumentationRegistry.getInstrumentation() }

    @Test
    fun trimHandlesAreIndependentlyDraggableAndClipDeleteRequiresConfirmation() {
        val root = instrumentation.targetContext.filesDir
        val repository = FileProjectRepository(root)
        val project = ProjectFactory(idGenerator = { "hardening-${System.nanoTime()}" }, clock = { 100L })
            .create("Hardening", ProjectTemplate.BLANK)
            .copy(
                tracks = listOf(AudioTrack(id = "t1", name = "Guitar", order = 0)),
                clips = listOf(
                    AudioClip(
                        id = "c1",
                        trackId = "t1",
                        name = "Take 1",
                        sourceUri = "managed://media/source/take.wav",
                        managedSourcePath = "media/source/take.wav",
                        startFrame = 0L,
                        lengthFrames = 48_000L,
                        sourceTotalFrames = 48_000L,
                        sourceSampleRateHz = 48_000,
                        sourceChannelCount = 1,
                    )
                ),
            )
        repository.save(project)
        WaveformCacheStore(root).write(
            project.id,
            "c1",
            WaveformEnvelope(List(160) { index -> if (index % 17 == 0) 0.9f else 0.25f }),
        )

        try {
            navigation().navigate(AppScreen.Studio(project.id))
            waitForRoute(AppScreen.Studio(project.id))
            waitForStudioProject(project.id)

            composeRule.onNodeWithContentDescription("Ações do áudio").performClick()
            composeRule.onNodeWithText("Cortar").assertIsDisplayed().performClick()
            composeRule.waitUntil(timeoutMillis = UI_TIMEOUT_MS) {
                runCatching { composeRule.onNodeWithTag("trim-start-handle").fetchSemanticsNode() }.isSuccess &&
                    runCatching { composeRule.onNodeWithTag("trim-end-handle").fetchSemanticsNode() }.isSuccess
            }
            composeRule.onNodeWithTag("trim-start-handle").assertIsDisplayed()
            composeRule.onNodeWithTag("trim-end-handle").assertIsDisplayed()
            composeRule.onNodeWithContentDescription("Marcador de início do corte").assertIsDisplayed()
            composeRule.onNodeWithContentDescription("Marcador de fim do corte").assertIsDisplayed()
            composeRule.onNodeWithTag("trim-ruler-start").assertIsDisplayed()
            composeRule.onNodeWithTag("trim-ruler-end").assertIsDisplayed()
            composeRule.onNodeWithTag("timeline-time-ruler").assertIsDisplayed()
            composeRule.onNodeWithTag("timeline-marker-rail").assertIsDisplayed()
            composeRule.onAllNodesWithText("T1 ", substring = true).assertCountEquals(0)
            composeRule.onAllNodesWithText("T2 ", substring = true).assertCountEquals(0)

            val railBounds = composeRule.onNodeWithTag("timeline-marker-rail").fetchSemanticsNode().boundsInRoot
            val rulerBounds = composeRule.onNodeWithTag("timeline-time-ruler").fetchSemanticsNode().boundsInRoot
            assertEquals(
                "The playhead/loop rail must sit directly above the time ruler without a dedicated Trim lane",
                railBounds.bottom,
                rulerBounds.top,
                1.5f,
            )
            assertTrue(
                "The time ruler must remain compact instead of reserving a Trim marker track",
                rulerBounds.height < railBounds.height * 0.5f,
            )

            val initialStart = requireNotNull(studio().state.value.trimControls).startFrame
            composeRule.onNodeWithTag("trim-start-handle").performTouchInput {
                swipe(center, center + Offset(90f, 0f), durationMillis = 350L)
            }
            composeRule.waitUntil(timeoutMillis = UI_TIMEOUT_MS) {
                studio().state.value.trimControls?.startFrame?.let { it > initialStart } == true
            }
            val movedStart = requireNotNull(studio().state.value.trimControls).startFrame
            assertTrue("Start trim handle must move independently", movedStart > initialStart)
            composeRule.waitForIdle()

            val currentProject = requireNotNull(studio().state.value.project)
            val currentRailBounds = composeRule.onNodeWithTag("timeline-marker-rail").fetchSemanticsNode().boundsInRoot
            val currentRulerBounds = composeRule.onNodeWithTag("timeline-time-ruler").fetchSemanticsNode().boundsInRoot
            val trimMarkerBounds = composeRule.onNodeWithTag("trim-ruler-start").fetchSemanticsNode().boundsInRoot
            val expectedTrimX = currentRailBounds.left +
                TimelineControlPolicy.frameToFraction(movedStart, TimelineControlPolicy.projectEndFrame(currentProject)) * currentRailBounds.width
            assertEquals(
                "T1 must use the exact same horizontal timeline geometry as playhead/loop markers",
                expectedTrimX,
                trimMarkerBounds.center.x,
                4f,
            )
            assertTrue(
                "CUT ticks must start inside the time ruler, never in the sections/playhead rail",
                trimMarkerBounds.top >= currentRulerBounds.top - 1.5f,
            )
            assertTrue(
                "CUT ticks must remain completely inside the compact time ruler",
                trimMarkerBounds.bottom <= currentRulerBounds.bottom + 1.5f,
            )
            assertTrue(
                "CUT ticks must no longer extend into the sections/playhead rail",
                trimMarkerBounds.top >= currentRailBounds.bottom - 1.5f,
            )

            val initialEnd = requireNotNull(studio().state.value.trimControls).endFrame
            composeRule.onNodeWithTag("trim-end-handle").performTouchInput {
                swipe(center, center - Offset(90f, 0f), durationMillis = 350L)
            }
            composeRule.waitUntil(timeoutMillis = UI_TIMEOUT_MS) {
                studio().state.value.trimControls?.endFrame?.let { it < initialEnd } == true
            }
            assertTrue("End trim handle must move independently", requireNotNull(studio().state.value.trimControls).endFrame < initialEnd)

            studio().cancelTrim()
            composeRule.waitUntil(timeoutMillis = UI_TIMEOUT_MS) { studio().state.value.trimControls == null }
            composeRule.onNodeWithTag("timeline-clip-c1").assertIsDisplayed()

            composeRule.onNodeWithContentDescription("Ações do áudio").performClick()
            composeRule.onNodeWithText("Excluir clipe").assertIsDisplayed().performClick()
            composeRule.onNodeWithText("Excluir clipe?").assertIsDisplayed()
            composeRule.onNodeWithText("Cancelar").performClick()
            composeRule.waitForIdle()
            assertEquals(listOf("c1"), studio().state.value.project?.clips?.map { it.id })
        } finally {
            runCatching { repository.delete(project.id) }
        }
    }

    @Test
    fun clickingTimelineWaveformAreaSelectsItsTrack() {
        val root = instrumentation.targetContext.filesDir
        val repository = FileProjectRepository(root)
        val project = ProjectFactory(idGenerator = { "select-${System.nanoTime()}" }, clock = { 200L })
            .create("Waveform select", ProjectTemplate.BLANK)
            .copy(
                tracks = listOf(
                    AudioTrack(id = "select-t1", name = "Primeira", order = 0),
                    AudioTrack(id = "select-t2", name = "Segunda", order = 1),
                ),
            )
        repository.save(project)

        try {
            navigation().navigate(AppScreen.Studio(project.id))
            waitForRoute(AppScreen.Studio(project.id))
            waitForStudioProject(project.id)

            composeRule.onNodeWithTag("track-waveform-area-select-t2")
                .assertIsDisplayed()
                .performClick()
                .assertIsSelected()
        } finally {
            runCatching { repository.delete(project.id) }
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

    private fun waitForRoute(expected: AppScreen) {
        composeRule.waitUntil(timeoutMillis = UI_TIMEOUT_MS) {
            AppRouteCodec.decode(navigation().persistedRoute.value) == expected
        }
    }

    private fun waitForStudioProject(projectId: String) {
        composeRule.waitUntil(timeoutMillis = UI_TIMEOUT_MS) {
            studio().state.value.project?.id == projectId && !studio().state.value.loading
        }
    }

    private companion object {
        const val UI_TIMEOUT_MS = 20_000L
    }
}
