package studio.guitarlab.app

import androidx.compose.ui.geometry.Offset
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.util.concurrent.atomic.AtomicInteger
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import studio.guitarlab.app.ui.MixerDock
import studio.guitarlab.app.ui.theme.GuitarLabTheme
import studio.guitarlab.core.audio.MeterBallisticsState
import studio.guitarlab.core.model.AudioTrack

@RunWith(AndroidJUnit4::class)
class MixerDockInstrumentedTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun mixerStateControlsExposeRoleStateCallbacksAndNonOverlapping48dpTargets() {
        val muteClicks = AtomicInteger(0)
        val soloClicks = AtomicInteger(0)
        val cueClicks = AtomicInteger(0)
        val armClicks = AtomicInteger(0)
        val trackClipClicks = AtomicInteger(0)
        val masterClipClicks = AtomicInteger(0)
        val track = AudioTrack(
            id = "track-1",
            name = "Teste",
            muted = false,
            solo = true,
            armed = false,
            order = 0,
        )

        composeRule.setContent {
            GuitarLabTheme(darkTheme = false) {
                MixerDock(
                    tracks = listOf(track),
                    selectedTrackId = track.id,
                    mixControlsEnabled = true,
                    structuralControlsEnabled = true,
                    masterGainDb = 0f,
                    masterMeter = MeterBallisticsState(peak = 0.99f, rms = 0.5f, heldPeak = 1f),
                    trackMeters = mapOf(track.id to MeterBallisticsState(peak = 0.99f, rms = 0.5f, heldPeak = 1f)),
                    masterClipLatched = true,
                    trackClipLatched = setOf(track.id),
                    onSelectTrack = {},
                    onGainPreview = { _, _ -> },
                    onGainCommit = { _, _ -> },
                    onPanPreview = { _, _ -> },
                    onPanCommit = { _, _ -> },
                    onToggleMute = { muteClicks.incrementAndGet() },
                    onToggleSolo = { soloClicks.incrementAndGet() },
                    onToggleCue = { cueClicks.incrementAndGet() },
                    onToggleArm = { armClicks.incrementAndGet() },
                    onMasterGainPreview = {},
                    onMasterGainCommit = {},
                    onClearTrackClip = { trackClipClicks.incrementAndGet() },
                    onClearMasterClip = { masterClipClicks.incrementAndGet() },
                )
            }
        }

        val mute = composeRule.onNodeWithContentDescription("Mute da pista Teste")
        val solo = composeRule.onNodeWithContentDescription("Solo da pista Teste")
        val cue = composeRule.onNodeWithContentDescription("Saída CUE da pista Teste")
        val arm = composeRule.onNodeWithContentDescription("Gravação da pista Teste")

        val buttonRole = SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button)
        mute.assert(buttonRole).assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Desativado")).assertIsEnabled().assert(hasClickAction())
        solo.assert(buttonRole).assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Ativado")).assertIsEnabled().assert(hasClickAction())
        cue.assert(buttonRole).assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Desativado")).assertIsEnabled().assert(hasClickAction())
        arm.assert(buttonRole).assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Desarmada")).assertIsEnabled().assert(hasClickAction())

        mute.performScrollTo().assertIsDisplayed().performClick()
        solo.performScrollTo().assertIsDisplayed().performClick()
        cue.performScrollTo().assertIsDisplayed().performClick()
        arm.performScrollTo().assertIsDisplayed().performClick()
        composeRule.onNodeWithContentDescription("Limpar clipping da pista Teste")
            .performScrollTo()
            .assertIsDisplayed()
            .assert(buttonRole)
            .performClick()
        composeRule.onNodeWithContentDescription("Limpar clipping do master").assert(buttonRole).performClick()

        assertEquals(1, muteClicks.get())
        assertEquals(1, soloClicks.get())
        assertEquals(1, cueClicks.get())
        assertEquals(1, armClicks.get())
        assertEquals(1, trackClipClicks.get())
        assertEquals(1, masterClipClicks.get())

        val density = InstrumentationRegistry.getInstrumentation().targetContext.resources.displayMetrics.density
        val controls = listOf(mute, solo, cue, arm)
        val rectangles = controls.map { control ->
            control.performScrollTo().assertIsDisplayed()
            control.fetchSemanticsNode().boundsInRoot
        }
        rectangles.forEach { bounds ->
            assertTrue("Control must retain 48dp width", bounds.width >= 48f * density - 1f)
            assertTrue("Control must retain 48dp height", bounds.height >= 48f * density - 1f)
        }
        rectangles.forEachIndexed { i, first ->
            rectangles.drop(i + 1).forEach { second ->
                assertTrue("2x2 state targets must not overlap", !first.overlaps(second))
            }
        }
        val strip = composeRule.onNodeWithTag("mixer-track-strip-${track.id}").fetchSemanticsNode().boundsInRoot
        assertEquals("Narrow channel width", 168f * density, strip.width, 1f)
        val volume = composeRule.onNodeWithContentDescription("Volume da pista Teste").performScrollTo().assertIsDisplayed().fetchSemanticsNode().boundsInRoot
        val pan = composeRule.onNodeWithContentDescription("Pan da pista Teste").performScrollTo().assertIsDisplayed().fetchSemanticsNode().boundsInRoot
        assertTrue("Volume and pan targets must not overlap", !volume.overlaps(pan))
        assertTrue("Pan must stay fully inside the dock", pan.bottom <= composeRule.onNodeWithTag("mixer-dock").fetchSemanticsNode().boundsInRoot.bottom)
        composeRule.captureCohesionScreenshot("rc23-mixer-narrow-clipping")

    }
    @Test
    fun cueRoutingIsDisabledWhenStructuralTransportEditsAreLocked() {
        val track = AudioTrack(id = "route-locked", name = "Route locked", order = 0)
        composeRule.setContent {
            GuitarLabTheme(darkTheme = false) {
                MixerDock(
                    tracks = listOf(track),
                    selectedTrackId = track.id,
                    mixControlsEnabled = true,
                    structuralControlsEnabled = false,
                    masterGainDb = 0f,
                    masterMeter = MeterBallisticsState(),
                    trackMeters = emptyMap(),
                    masterClipLatched = false,
                    trackClipLatched = emptySet(),
                    onSelectTrack = {},
                    onGainPreview = { _, _ -> },
                    onGainCommit = { _, _ -> },
                    onPanPreview = { _, _ -> },
                    onPanCommit = { _, _ -> },
                    onToggleMute = {},
                    onToggleSolo = {},
                    onToggleCue = {},
                    onToggleArm = {},
                    onMasterGainPreview = {},
                    onMasterGainCommit = {},
                    onClearTrackClip = {},
                    onClearMasterClip = {},
                )
            }
        }

        composeRule.onNodeWithContentDescription("Mute da pista Route locked").assertIsEnabled()
        composeRule.onNodeWithContentDescription("Solo da pista Route locked").assertIsEnabled()
        composeRule.onNodeWithContentDescription("Saída CUE da pista Route locked").assertIsNotEnabled()
        composeRule.onNodeWithContentDescription("Gravação da pista Route locked").assertIsNotEnabled()
    }

    @Test
    fun overflowingTracksSwipeHorizontallyWhileMasterRemainsAnchored() {
        val tracks = List(10) { index -> AudioTrack(id = "overflow-$index", name = "Track $index", order = index) }
        composeRule.setContent {
            GuitarLabTheme(darkTheme = true) {
                MixerDock(
                    tracks = tracks,
                    selectedTrackId = tracks.first().id,
                    mixControlsEnabled = true,
                    structuralControlsEnabled = true,
                    masterGainDb = 0f,
                    masterMeter = MeterBallisticsState(),
                    trackMeters = emptyMap(),
                    masterClipLatched = false,
                    trackClipLatched = emptySet(),
                    onSelectTrack = {},
                    onGainPreview = { _, _ -> },
                    onGainCommit = { _, _ -> },
                    onPanPreview = { _, _ -> },
                    onPanCommit = { _, _ -> },
                    onToggleMute = {},
                    onToggleSolo = {},
                    onToggleCue = {},
                    onToggleArm = {},
                    onMasterGainPreview = {},
                    onMasterGainCommit = {},
                    onClearTrackClip = {},
                    onClearMasterClip = {},
                )
            }
        }

        val master = composeRule.onNodeWithTag("mixer-master-strip").assertIsDisplayed()
        val masterBefore = master.fetchSemanticsNode().boundsInRoot
        val scroller = composeRule.onNodeWithTag("mixer-track-scroll").assertIsDisplayed()
        val scrollerBounds = scroller.fetchSemanticsNode().boundsInRoot
        val swipeStart = Offset(scrollerBounds.width * 0.90f, scrollerBounds.height * 0.08f)
        val swipeEnd = Offset(scrollerBounds.width * 0.10f, scrollerBounds.height * 0.08f)
        var lastTrackVisible = false
        repeat(20) {
            if (!lastTrackVisible) {
                // Swipe through the strip header band instead of centerY. The center crosses the
                // volume/pan sliders, which legitimately consume horizontal gestures. A user
                // scrolls the Mixer from non-slider chrome such as the track header.
                scroller.performTouchInput { swipe(swipeStart, swipeEnd, durationMillis = 220L) }
                composeRule.waitForIdle()
                val candidates = composeRule.onAllNodesWithTag("mixer-track-strip-overflow-9")
                    .fetchSemanticsNodes()
                lastTrackVisible = candidates.any { node ->
                    val bounds = node.boundsInRoot
                    bounds.right > scrollerBounds.left && bounds.left < scrollerBounds.right
                }
            }
        }
        assertTrue(
            "Last mixer track must be reachable by repeated physical horizontal swipe regardless of viewport width",
            lastTrackVisible,
        )
        val masterAfter = composeRule.onNodeWithTag("mixer-master-strip").fetchSemanticsNode().boundsInRoot
        assertEquals(masterBefore.left, masterAfter.left, 1f)
        assertEquals(masterBefore.right, masterAfter.right, 1f)
    }

    @Test
    fun largeFontsGrowChannelsWithoutClippingPanOrShrinkingTargets() {
        val fontScale = mutableStateOf(1f)
        val track = AudioTrack(id = "large-font", name = "Guitarra de referência E", order = 0)
        composeRule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale.value)) {
                GuitarLabTheme(darkTheme = true) {
                    MixerDock(
                        tracks = listOf(track), selectedTrackId = track.id,
                        mixControlsEnabled = true, structuralControlsEnabled = true,
                        masterGainDb = 0f, masterMeter = MeterBallisticsState(), trackMeters = emptyMap(),
                        masterClipLatched = true, trackClipLatched = setOf(track.id),
                        onSelectTrack = {}, onGainPreview = { _, _ -> }, onGainCommit = { _, _ -> },
                        onPanPreview = { _, _ -> }, onPanCommit = { _, _ -> },
                        onToggleMute = {}, onToggleSolo = {}, onToggleCue = {}, onToggleArm = {},
                        onMasterGainPreview = {}, onMasterGainCommit = {},
                        onClearTrackClip = {}, onClearMasterClip = {},
                    )
                }
            }
        }
        val before = composeRule.onNodeWithTag("mixer-track-strip-${track.id}").fetchSemanticsNode().boundsInRoot
        composeRule.runOnIdle { fontScale.value = 1.5f }
        val after = composeRule.onNodeWithTag("mixer-track-strip-${track.id}").fetchSemanticsNode().boundsInRoot
        assertTrue("Larger fonts must increase channel width", after.width > before.width)
        assertTrue("Larger fonts must increase channel height", after.height > before.height)
        listOf("Mute", "Solo", "Saída CUE", "Gravação", "Volume", "Pan").forEach { action ->
            val node = composeRule.onNodeWithContentDescription("$action da pista ${track.name}")
                .performScrollTo().assertIsDisplayed().fetchSemanticsNode().boundsInRoot
            assertTrue("$action must be inside the dock", node.bottom <= after.bottom + 1f)
        }
        composeRule.captureCohesionScreenshot("rc23-mixer-large-font")
    }

}
