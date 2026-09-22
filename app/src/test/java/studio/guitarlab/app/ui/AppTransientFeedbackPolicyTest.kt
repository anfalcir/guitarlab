package studio.guitarlab.app.ui

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AppTransientFeedbackPolicyTest {
    @Test fun `play cut record and other routine operational state never own a snackbar`() {
        // Play, Stop, Pause, CUT/Trim entry, normal REC/countdown, mute/solo/arm, selection and seek
        // all map to OPERATIONAL_STATUS if a caller tries to classify them as transient feedback.
        repeat(8) {
            assertFalse(AppTransientFeedbackPolicy.shouldShowSnackbar(TransientFeedbackKind.OPERATIONAL_STATUS))
        }
    }

    @Test fun `real error warning and non obvious async completion may own a snackbar`() {
        assertTrue(AppTransientFeedbackPolicy.shouldShowSnackbar(TransientFeedbackKind.ERROR))
        assertTrue(AppTransientFeedbackPolicy.shouldShowSnackbar(TransientFeedbackKind.WARNING))
        assertTrue(AppTransientFeedbackPolicy.shouldShowSnackbar(TransientFeedbackKind.ASYNC_COMPLETION))
    }

    @Test fun `low level Android route identifiers never leak into transient feedback`() {
        val rawMessages = listOf(
            "SM-X230 • bottom",
            "remote-submix • hsp:1",
            "route3:usb:deadbeef",
            "route2:usb:legacy",
            "deviceId=44",
            "device #44",
            "address=card=1;device=0",
            "endpoint=7",
            "AudioDeviceInfo{id=3}",
            "productName=SM-X230",
            "output endpoint index 2",
            "back",
        )
        rawMessages.forEach { raw ->
            assertEquals("Rota de áudio alterada.", AppTransientFeedbackPolicy.userSafe(raw, "Rota de áudio alterada."), raw)
        }
    }

    @Test fun `semantic physical route names remain user visible`() {
        for (friendly in listOf("Microfone do tablet", "Alto-falante do tablet", "MK-300")) {
            assertEquals(friendly, AppTransientFeedbackPolicy.userSafe(friendly))
        }
    }
}
