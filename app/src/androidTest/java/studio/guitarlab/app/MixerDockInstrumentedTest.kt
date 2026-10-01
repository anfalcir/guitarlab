package studio.guitarlab.app

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.abs
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
    fun mixerStateControlsExposeRoleStateCallbacksAndNonOverlapping48dpCenters() {
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

        mute.performClick()
        solo.performClick()
        cue.performClick()
        arm.performClick()
        composeRule.onNodeWithContentDescription("Limpar clipping da pista Teste").assert(buttonRole).performClick()
        composeRule.onNodeWithContentDescription("Limpar clipping do master").assert(buttonRole).performClick()

        assertEquals(1, muteClicks.get())
        assertEquals(1, soloClicks.get())
        assertEquals(1, cueClicks.get())
        assertEquals(1, armClicks.get())
        assertEquals(1, trackClipClicks.get())
        assertEquals(1, masterClipClicks.get())

        val muteCenter = mute.fetchSemanticsNode().boundsInRoot.center
        val soloCenter = solo.fetchSemanticsNode().boundsInRoot.center
        val cueCenter = cue.fetchSemanticsNode().boundsInRoot.center
        val armCenter = arm.fetchSemanticsNode().boundsInRoot.center
        val density = InstrumentationRegistry.getInstrumentation().targetContext.resources.displayMetrics.density
        val minimumCenterDistancePx = 48f * density - 1f

        assertTrue("Mute/Solo expanded touch targets must not overlap", abs(soloCenter.x - muteCenter.x) >= minimumCenterDistancePx)
        assertTrue("Solo/CUE expanded touch targets must not overlap", abs(cueCenter.x - soloCenter.x) >= minimumCenterDistancePx)
        assertTrue("CUE/Arm expanded touch targets must not overlap", abs(armCenter.x - cueCenter.x) >= minimumCenterDistancePx)
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

}
