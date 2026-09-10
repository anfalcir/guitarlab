package studio.guitarlab.app

import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
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
                    pinned = true,
                    mixControlsEnabled = true,
                    structuralControlsEnabled = true,
                    masterGainDb = 0f,
                    masterMeter = MeterBallisticsState(peak = 0.99f, rms = 0.5f, heldPeak = 1f),
                    trackMeters = mapOf(track.id to MeterBallisticsState(peak = 0.99f, rms = 0.5f, heldPeak = 1f)),
                    masterClipLatched = true,
                    trackClipLatched = setOf(track.id),
                    onSelectTrack = {},
                    onPin = {},
                    onClose = {},
                    onGainPreview = { _, _ -> },
                    onGainCommit = { _, _ -> },
                    onPanPreview = { _, _ -> },
                    onPanCommit = { _, _ -> },
                    onToggleMute = { muteClicks.incrementAndGet() },
                    onToggleSolo = { soloClicks.incrementAndGet() },
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
        val arm = composeRule.onNodeWithContentDescription("Gravação da pista Teste")

        val buttonRole = SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button)
        mute.assert(buttonRole).assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Desativado")).assertIsEnabled().assert(hasClickAction())
        solo.assert(buttonRole).assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Ativado")).assertIsEnabled().assert(hasClickAction())
        arm.assert(buttonRole).assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Desarmada")).assertIsEnabled().assert(hasClickAction())

        mute.performClick()
        solo.performClick()
        arm.performClick()
        composeRule.onNodeWithContentDescription("Limpar clipping da pista Teste").assert(buttonRole).performClick()
        composeRule.onNodeWithContentDescription("Limpar clipping do Master").assert(buttonRole).performClick()

        assertEquals(1, muteClicks.get())
        assertEquals(1, soloClicks.get())
        assertEquals(1, armClicks.get())
        assertEquals(1, trackClipClicks.get())
        assertEquals(1, masterClipClicks.get())

        val muteCenter = mute.fetchSemanticsNode().boundsInRoot.center
        val soloCenter = solo.fetchSemanticsNode().boundsInRoot.center
        val armCenter = arm.fetchSemanticsNode().boundsInRoot.center
        val density = InstrumentationRegistry.getInstrumentation().targetContext.resources.displayMetrics.density
        val minimumCenterDistancePx = 48f * density - 1f

        assertTrue("Mute/Solo expanded touch targets must not overlap", abs(soloCenter.x - muteCenter.x) >= minimumCenterDistancePx)
        assertTrue("Solo/Arm expanded touch targets must not overlap", abs(armCenter.x - soloCenter.x) >= minimumCenterDistancePx)
    }
}
