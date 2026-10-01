package studio.guitarlab.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.performClick
import androidx.lifecycle.ViewModelProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import studio.guitarlab.app.ui.AppNavigationViewModel
import studio.guitarlab.app.ui.AppScreen
import studio.guitarlab.app.ui.StudioViewModel
import studio.guitarlab.app.ui.StudioUiPreferencesStore
import java.io.File
import kotlin.math.sin
import studio.guitarlab.core.codec.FloatWavFileWriter
import studio.guitarlab.core.model.AudioClip
import studio.guitarlab.core.project.ProjectManagedMediaStore
import studio.guitarlab.core.project.WaveformCacheStore
import studio.guitarlab.core.project.WaveformCacheIdentity
import studio.guitarlab.core.codec.WaveformEnvelope
import studio.guitarlab.core.model.ProjectFactory
import studio.guitarlab.core.model.ProjectTemplate
import studio.guitarlab.core.project.FileProjectRepository

@RunWith(AndroidJUnit4::class)
class StudioMixerTabletInstrumentedTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test
    fun fiveAudioChannelsFitAndMixerModesRecoverTimelineSpaceWithoutMovingNavbar() {
        if (InstrumentationRegistry.getArguments().getString("targetGeometry") != "true") return
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val preferences = StudioUiPreferencesStore(context)
        val oldVisible = preferences.mixerVisible()
        val oldMinimal = preferences.mixerMinimal()
        val repository = FileProjectRepository(context.filesDir)
        val base = ProjectFactory().create("RC24 cinco pistas com áudio", ProjectTemplate.GUITAR)
        val media = ProjectManagedMediaStore(context.filesDir)
        val temp = File(context.cacheDir, "${base.id}-layout.wav")
        val frames = 48_000
        FloatWavFileWriter(temp, 48_000, 2).use { writer ->
            writer.writeInterleaved(FloatArray(frames * 2) { index ->
                (sin(index / 2.0 * 2.0 * Math.PI * 220 / 48_000) * 0.4).toFloat()
            }, frames)
        }
        val managed = try { temp.inputStream().use { media.ingest(base.id, "layout.wav", it) } } finally { temp.delete() }
        val project = base.copy(clips = base.tracks.mapIndexed { index, track ->
            AudioClip(id = "layout-$index", trackId = track.id, name = track.name,
                sourceUri = "managed://${managed.relativePath}", managedSourcePath = managed.relativePath,
                startFrame = 0, lengthFrames = frames.toLong(), sourceTotalFrames = frames.toLong(),
                sourceSampleRateHz = 48_000, sourceChannelCount = 2)
        })
        repository.save(project)
        project.clips.forEach { clip ->
            WaveformCacheStore(context.filesDir).write(project.id, clip.id,
                WaveformCacheIdentity.forClip(clip, targetPoints = 4096),
                WaveformEnvelope(List(4096) { index -> if (index % 17 == 0) 0.55f else 0.28f }))
        }
        try {
            preferences.setMixerVisible(true)
            preferences.setMixerMinimal(false)
            lateinit var studio: StudioViewModel
            compose.activityRule.scenario.onActivity { activity ->
                studio = ViewModelProvider(activity)[StudioViewModel::class.java]
                ViewModelProvider(activity)[AppNavigationViewModel::class.java].navigate(AppScreen.Studio(project.id))
            }
            compose.waitUntil(20_000) {
                studio.state.value.project?.id == project.id && project.clips.all {
                    studio.state.value.waveforms[it.id]?.isNotEmpty() == true
                } && runCatching { compose.onNodeWithTag("studio-loaded").assertIsDisplayed() }.isSuccess
            }
            // A restored Studio composition may retain workspace preferences from a prior entry;
            // use the actual controls to normalize this test's explicitly requested dock state.
            if (compose.onAllNodesWithTag("mixer-master-strip").fetchSemanticsNodes().isEmpty()) {
                compose.onNodeWithTag("studio-mixer-toggle").performClick()
            }
            val mode = compose.onNodeWithTag("studio-mixer-mode")
            if (mode.fetchSemanticsNode().config[SemanticsProperties.StateDescription] != "Completo") mode.performClick()
            compose.waitForIdle()
            val scroller = compose.onNodeWithTag("mixer-track-scroll").fetchSemanticsNode().boundsInRoot
            project.tracks.forEach { track ->
                val bounds = compose.onNodeWithTag("mixer-track-strip-${track.id}").assertIsDisplayed().fetchSemanticsNode().boundsInRoot
                assertTrue("Every default track must fit fully without scrolling", bounds.left >= scroller.left - 1 && bounds.right <= scroller.right + 1)
            }
            val master = compose.onNodeWithTag("mixer-master-strip").fetchSemanticsNode().boundsInRoot
            assertTrue("Master must own a separate non-overlapping column", master.left >= scroller.right)
            // Compose semantics can precede the actual SurfaceFlinger frame during load/toggle.
            // Wait for accessibility/window quiescence before full-display artifact capture.
            UiDevice.getInstance(InstrumentationRegistry.getInstrumentation()).waitForIdle()
            compose.captureCohesionScreenshot("rc24-studio-five-audio-channels-complete")
            val navbar = compose.onNodeWithTag("studio-workspace-bar").fetchSemanticsNode().boundsInRoot
            val transport = compose.onNodeWithTag("studio-navigation").assertIsDisplayed().fetchSemanticsNode().boundsInRoot
            assertEquals("Tablet transport must be centered on the viewport", navbar.center.x, transport.center.x, 1f)
            val fullDock = compose.onNodeWithTag("mixer-dock").fetchSemanticsNode().boundsInRoot
            val density = context.resources.displayMetrics.density
            assertTrue("Complete dock must be at least 25% shorter than RC23", fullDock.height <= 264f * density + 1f)
            // Three complete waveform lanes must now be visible, rather than fewer than two in the owner video.
            val third = compose.onNodeWithTag("track-waveform-area-${project.tracks[2].id}").assertIsDisplayed().fetchSemanticsNode().boundsInRoot
            assertTrue("Third lane must fit above the dock", third.bottom <= fullDock.top + 1f)
            mode.performClick()
            val minimumDock = compose.onNodeWithTag("mixer-dock").fetchSemanticsNode().boundsInRoot
            assertTrue("Minimum recovers at least 72dp", minimumDock.height <= fullDock.height - 72f * density)
            assertTrue("Mode switch leaves navbar fixed", navbar == compose.onNodeWithTag("studio-workspace-bar").fetchSemanticsNode().boundsInRoot)
            assertEquals("Mixer mode leaves tablet transport fixed", transport, compose.onNodeWithTag("studio-navigation").fetchSemanticsNode().boundsInRoot)
            compose.onNodeWithTag("open-all-level-analysis").assertIsDisplayed()
            UiDevice.getInstance(InstrumentationRegistry.getInstrumentation()).waitForIdle()
            compose.captureCohesionScreenshot("rc24-studio-five-audio-channels-minimum")
            compose.onNodeWithTag("open-all-level-analysis").performClick()
            compose.onNodeWithTag("all-levels-dialog").assertIsDisplayed()
            InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_BACK)
            compose.waitForIdle()
            compose.onNodeWithTag("studio-mixer-toggle").performClick()
            compose.onNodeWithTag("studio-mixer-toggle").performClick()
            assertTrue("Reopening restores minimum", compose.onNodeWithTag("studio-mixer-mode").fetchSemanticsNode().config[SemanticsProperties.StateDescription] == "Mínimo")
            compose.onNodeWithTag("studio-mixer-toggle").performClick()
            val closedNavbar = compose.onNodeWithTag("studio-workspace-bar").fetchSemanticsNode().boundsInRoot
            assertTrue("Closing Mixer must not resize or move navbar", navbar == closedNavbar)
            // Compose semantics can precede the actual SurfaceFlinger frame during load/toggle.
            // Wait for accessibility/window quiescence before full-display artifact capture.
            UiDevice.getInstance(InstrumentationRegistry.getInstrumentation()).waitForIdle()
            compose.captureCohesionScreenshot("rc24-studio-mixer-closed")
        } finally {
            preferences.setMixerVisible(oldVisible)
            preferences.setMixerMinimal(oldMinimal)
            repository.delete(project.id)
        }
    }
}
