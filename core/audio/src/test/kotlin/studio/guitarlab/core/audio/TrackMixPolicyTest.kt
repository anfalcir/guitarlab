package studio.guitarlab.core.audio

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TrackMixPolicyTest {
    @Test fun muteAlwaysWins() {
        assertFalse(TrackMixPolicy.isAudible(muted = true, solo = true, anySolo = true))
        assertFalse(TrackMixPolicy.isAudible(muted = true, solo = false, anySolo = false))
    }

    @Test fun soloSetRestrictsPlaybackToSoloTracks() {
        assertTrue(TrackMixPolicy.isAudible(muted = false, solo = true, anySolo = true))
        assertFalse(TrackMixPolicy.isAudible(muted = false, solo = false, anySolo = true))
        assertTrue(TrackMixPolicy.isAudible(muted = false, solo = false, anySolo = false))
    }

    @Test fun panUsesStableBalanceLaw() {
        val center = TrackMixPolicy.channelGains(0f, 0f)
        assertClose(1f, center.left)
        assertClose(1f, center.right)

        val hardLeft = TrackMixPolicy.channelGains(0f, -1f)
        assertClose(1f, hardLeft.left)
        assertClose(0f, hardLeft.right)

        val hardRight = TrackMixPolicy.channelGains(0f, 1f)
        assertClose(0f, hardRight.left)
        assertClose(1f, hardRight.right)
    }

    @Test fun gainDbConvertsToLinearAmplitude() {
        val minusSix = TrackMixPolicy.channelGains(-6.0206f, 0f)
        assertTrue(abs(minusSix.left - 0.5f) < 0.002f)
        assertEquals(minusSix.left, minusSix.right)
    }

    private fun assertClose(expected: Float, actual: Float) {
        assertTrue(abs(expected - actual) < 0.0001f, "expected=$expected actual=$actual")
    }
}
