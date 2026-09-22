package studio.guitarlab.core.audio

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AudioRouteSessionPolicyTest {
    @Test
    fun routeLossInvalidatesRouteSpecificCompensationExactlyOnce() {
        val starting = AudioRouteSessionPolicy.starting("usb-in:mk300", "usb-out:mk300")
        val active = AudioRouteSessionPolicy.confirmed(starting, "usb-in:mk300", "usb-out:mk300")
        val firstLoss = AudioRouteSessionPolicy.lost(active, "disconnect")
        val repeatedLoss = AudioRouteSessionPolicy.lost(firstLoss, "duplicate callback")
        assertTrue(firstLoss.compensationInvalidated)
        assertEquals(1, firstLoss.invalidationCount)
        assertEquals(1, repeatedLoss.invalidationCount)
    }

    @Test
    fun compatibleReconnectNeverAutoResumesRecording() {
        val lost = AudioRouteSessionPolicy.lost(
            AudioRouteSessionPolicy.confirmed(
                AudioRouteSessionPolicy.starting("usb-in:mk300", "usb-out:mk300"),
                "usb-in:mk300",
                "usb-out:mk300",
            ),
            "disconnect",
        )
        val reappeared = AudioRouteSessionPolicy.compatibleReappeared(lost)
        assertEquals(AudioRouteSessionPhase.COMPATIBLE_REAPPEARED, reappeared.phase)
        assertTrue(reappeared.canStart)
        assertFalse(reappeared.phase == AudioRouteSessionPhase.ACTIVE_CONFIRMED)
        assertEquals(1, reappeared.invalidationCount)
    }
}
