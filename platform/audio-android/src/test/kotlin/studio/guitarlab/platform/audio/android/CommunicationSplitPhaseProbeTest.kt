package studio.guitarlab.platform.audio.android

import kotlin.math.abs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CommunicationSplitPhaseProbeTest {
    @Test
    fun generatedToneIsStereoBoundedAndContinuousAcrossChunks() {
        val first = CommunicationSplitProbeSignal.stereoTone(48_000, 440.0, 0, 256)
        val second = CommunicationSplitProbeSignal.stereoTone(48_000, 440.0, 256, 256)
        assertEquals(512, first.size)
        assertEquals(512, second.size)
        for (index in first.indices step 2) assertEquals(first[index], first[index + 1], 0f)
        for (value in first + second) assertTrue(abs(value) <= 0.060001f)
        assertTrue(abs(first[first.lastIndex] - second[0]) < 0.08f)
    }

    @Test
    fun phaseOrderMatchesDiagnosticContract() {
        assertEquals(
            listOf(
                CommunicationSplitProbePhase.A_MAIN_ONLY,
                CommunicationSplitProbePhase.B_COMMUNICATION_DEVICE_SELECTED,
                CommunicationSplitProbePhase.C_CUE_TRACK_SILENT,
                CommunicationSplitProbePhase.D_CUE_TONE_ACTIVE,
            ),
            CommunicationSplitProbePhase.entries,
        )
    }
}
