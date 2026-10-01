package studio.guitarlab.app

import android.util.Log
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.geometry.Offset
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.click
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
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

        mute.performScrollTo().assertIsDisplayed().performTouchInput { click() }
        solo.performScrollTo().assertIsDisplayed().performTouchInput { click() }
        cue.performScrollTo().assertIsDisplayed().performTouchInput { click() }
        arm.performScrollTo().assertIsDisplayed().performTouchInput { click() }
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
                assertTrue("Inline state targets must not overlap", !first.overlaps(second))
            }
        }
        rectangles.zipWithNext().forEach { (first, second) ->
            assertEquals("All four actions share one row", first.top, second.top, 1f)
            assertEquals("No wasted gap between touch regions", first.right, second.left, 1f)
        }
        listOf("Mute", "Solo", "Saída CUE", "Gravação").forEach { action ->
            val face = composeRule.onNodeWithTag("mixer-button-face-$action da pista Teste", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
            assertEquals("Visible button nearly fills its touch region", 46f * density, face.width, 1f)
            assertEquals(46f * density, face.height, 1f)
        }
        val strip = composeRule.onNodeWithTag("mixer-track-strip-${track.id}").fetchSemanticsNode().boundsInRoot
        assertEquals("Narrow channel width", 200f * density, strip.width, 1f)
        val volume = composeRule.onNodeWithContentDescription("Volume da pista Teste").performScrollTo().assertIsDisplayed().fetchSemanticsNode().boundsInRoot
        val pan = composeRule.onNodeWithContentDescription("Pan da pista Teste").performScrollTo().assertIsDisplayed().fetchSemanticsNode().boundsInRoot
        assertTrue("Volume and pan targets must not overlap", !volume.overlaps(pan))
        assertTrue("Pan must stay fully inside the dock", pan.bottom <= composeRule.onNodeWithTag("mixer-dock").fetchSemanticsNode().boundsInRoot.bottom)
        composeRule.captureCohesionScreenshot("rc24-mixer-narrow-clipping")

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
        composeRule.onNodeWithTag("open-all-level-analysis").assertIsNotEnabled()
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
        val gainPreviews = AtomicInteger(0)
        val gainCommits = AtomicInteger(0)
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
                        onSelectTrack = {}, onGainPreview = { _, _ -> gainPreviews.incrementAndGet() }, onGainCommit = { _, _ -> gainCommits.incrementAndGet() },
                        onPanPreview = { _, _ -> }, onPanCommit = { _, _ -> },
                        onToggleMute = {}, onToggleSolo = {}, onToggleCue = {}, onToggleArm = {},
                        onMasterGainPreview = {}, onMasterGainCommit = {},
                        onClearTrackClip = {}, onClearMasterClip = {},
                    )
                }
            }
        }
        Log.i("MixerGeometryTest", "large-font: initial geometry")
        val strip = composeRule.onNodeWithTag("mixer-track-strip-${track.id}")
        val before = unclippedBounds(strip)
        Log.i("MixerGeometryTest", "large-font: set scale 1.5")
        composeRule.runOnIdle { fontScale.value = 1.5f }
        val after = unclippedBounds(strip)
        Log.i("MixerGeometryTest", "large-font: geometry $before -> $after")
        assertTrue("Larger fonts must increase channel width", after.width > before.width)
        assertTrue("Larger fonts must increase channel height", after.height > before.height)
        listOf("Mute", "Solo", "Saída CUE", "Gravação", "Volume", "Pan").forEach { action ->
            val control = composeRule.onNodeWithContentDescription("$action da pista ${track.name}")
            scrollMixerControlIntoView(control, action)
            val node = unclippedBounds(control.assertIsDisplayed())
            val dock = unclippedBounds(composeRule.onNodeWithTag("mixer-dock"))
            assertTrue("$action must be vertically inside the dock: $node / $dock",
                node.top >= dock.top - 1f && node.bottom <= dock.bottom + 1f)
            val density = InstrumentationRegistry.getInstrumentation().targetContext.resources.displayMetrics.density
            val touch = control.fetchSemanticsNode().touchBoundsInRoot
            Log.i("MixerGeometryTest", "$action fullSemantics=$node effectiveTouch=$touch dock=$dock")
            assertTrue("$action retains 48dp touch width: $touch", touch.width >= 48f * density - 1f)
            assertTrue("$action retains 48dp touch height: $touch", touch.height >= 48f * density - 1f)
            assertTrue("$action touch target stays inside dock: $touch / $dock",
                touch.top >= dock.top - 1f && touch.bottom <= dock.bottom + 1f)
        }
        val volume = composeRule.onNodeWithContentDescription("Volume da pista ${track.name}")
        scrollMixerControlIntoView(volume, "Volume touch test")
        // Touch above the 20dp visual thumb, inside the actual 48dp interactive height.
        volume.performTouchInput { click(Offset(center.x, height * 0.1f)) }
        composeRule.waitForIdle()
        assertTrue("Touch above the drawn thumb must update volume", gainPreviews.get() > 0)
        assertTrue("Touch above the drawn thumb must commit volume", gainCommits.get() > 0)
        composeRule.captureCohesionScreenshot("rc24-mixer-large-font")
    }

    /** Full layout bounds: clipped semantics bounds would hide the very defect this test checks. */
    private fun unclippedBounds(node: SemanticsNodeInteraction): Rect {
        val semantics = node.fetchSemanticsNode()
        val origin = semantics.positionInRoot
        // Semantics coordinates include the target modifiers; layoutInfo.coordinates is the
        // inner drawing layout (20dp for the custom thumb), not the interactive node bounds.
        return Rect(origin.x, origin.y, origin.x + semantics.size.width, origin.y + semantics.size.height)
    }

    /** Finite ScrollBy calls, with subpixel tolerance and explicit no-progress failure. */
    private fun scrollMixerControlIntoView(control: SemanticsNodeInteraction, label: String) {
        val scroller = composeRule.onNodeWithTag("mixer-track-scroll")
        repeat(12) { attempt ->
            val viewport = scroller.fetchSemanticsNode().boundsInRoot
            val before = unclippedBounds(control)
            Log.i("MixerGeometryTest", "$label attempt=$attempt target=$before viewport=$viewport")
            assertTrue("$label target exceeds horizontal viewport: $before / $viewport", before.width <= viewport.width + 1f)
            val delta = when {
                before.left < viewport.left - 1f -> before.left - viewport.left
                before.right > viewport.right + 1f -> before.right - viewport.right
                else -> return
            }
            scroller.performSemanticsAction(SemanticsActions.ScrollBy) { scroll -> scroll(delta, 0f) }
            composeRule.waitForIdle()
            val after = unclippedBounds(control)
            val aligned = after.left >= viewport.left - 1f && after.right <= viewport.right + 1f
            if (aligned) return
            assertTrue("$label scrolling made no progress: delta=$delta before=$before after=$after viewport=$viewport",
                kotlin.math.abs(after.left - before.left) > 0.5f)
        }
        throw AssertionError("$label did not become fully visible after 12 bounded scroll attempts")
    }

    @Test
    fun minimalRetainsFourActionsAndGainWhileDetailsExposePanAndClipping() {
        val minimal = mutableStateOf(false)
        val track = mutableStateOf(AudioTrack(id = "modes", name = "Minha guitarra", pan = -1f, gainDb = -6f, order = 0))
        var panCommits = 0
        var gainCommits = 0
        var clipResets = 0
        var levelClicks = 0
        composeRule.setContent {
            GuitarLabTheme(darkTheme = true) {
                MixerDock(
                    tracks = listOf(track.value), selectedTrackId = track.value.id,
                    minimal = minimal.value, onOpenLevelAnalysis = { levelClicks++ },
                    mixControlsEnabled = true, structuralControlsEnabled = true,
                    masterGainDb = 0f, masterMeter = MeterBallisticsState(), trackMeters = emptyMap(),
                    masterClipLatched = false, trackClipLatched = setOf(track.value.id),
                    onSelectTrack = {}, onGainPreview = { _, _ -> },
                    onGainCommit = { _, value -> track.value = track.value.copy(gainDb = value); gainCommits++ },
                    onPanPreview = { _, _ -> },
                    onPanCommit = { _, value -> track.value = track.value.copy(pan = value); panCommits++ },
                    onToggleMute = {}, onToggleSolo = {}, onToggleCue = {}, onToggleArm = {},
                    onMasterGainPreview = {}, onMasterGainCommit = {},
                    onClearTrackClip = { clipResets++ }, onClearMasterClip = {},
                )
            }
        }
        val full = composeRule.onNodeWithTag("mixer-dock").fetchSemanticsNode().boundsInRoot
        composeRule.onNodeWithContentDescription("Volume da pista Minha guitarra").performTouchInput {
            swipe(Offset(width * 0.2f, height / 2f), Offset(width * 0.9f, height / 2f), 250L)
        }
        assertTrue("Thin slider must commit gain", gainCommits > 0 && track.value.gainDb > -6f)
        val committedGain = track.value.gainDb
        composeRule.runOnIdle { minimal.value = true }
        val compact = composeRule.onNodeWithTag("mixer-dock").fetchSemanticsNode().boundsInRoot
        assertTrue("Minimum must recover substantial timeline space", compact.height < full.height * 0.72f)
        listOf("Mute", "Solo", "Saída CUE", "Gravação", "Volume").forEach { action ->
            composeRule.onNodeWithContentDescription("$action da pista Minha guitarra").assertIsDisplayed()
        }
        composeRule.onNodeWithTag("open-all-level-analysis").performClick()
        assertEquals(1, levelClicks)
        composeRule.onNodeWithContentDescription("Detalhes da pista Minha guitarra").performClick()
        composeRule.onNodeWithTag("mixer-track-details").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Pan da pista Minha guitarra").performTouchInput {
            swipe(Offset(width * 0.15f, height / 2f), Offset(width * 0.85f, height / 2f), 250L)
        }
        assertTrue("Pan must preview and commit from minimum details", panCommits > 0 && track.value.pan > -1f)
        composeRule.onNodeWithContentDescription("Limpar clipping da pista Minha guitarra").performClick()
        assertEquals(1, clipResets)
        composeRule.onNodeWithText("Fechar").performClick()
        composeRule.runOnIdle { minimal.value = false }
        composeRule.onNodeWithContentDescription("Pan da pista Minha guitarra").assertIsDisplayed()
        assertEquals("Changing presentation must preserve committed gain", committedGain, track.value.gainDb, 0f)
        composeRule.captureCohesionScreenshot("rc24-mixer-complete-after-minimum")
    }

}
