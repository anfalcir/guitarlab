package studio.guitarlab.app.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import studio.guitarlab.core.audio.MeterBallisticsState

class RecordingTrackMeterPolicyTest {
    @Test
    fun recordingInputUpdatesOnlyTheTargetTrackAndPreservesOtherMeters() {
        val other = MeterBallisticsState(peak = 0.2f, rms = 0.1f, heldPeak = 0.2f, updatedAtMs = 90L)
        val result = RecordingTrackMeterPolicy.update(
            previous = mapOf("other" to other),
            targetTrackId = "recording",
            rawPeak = 0.8f,
            rawRms = 0.35f,
            nowMs = 100L,
        )

        assertSame(other, result.getValue("other"))
        val recording = result.getValue("recording")
        assertTrue(recording.peak > 0f)
        assertTrue(recording.rms > 0f)
        assertTrue(recording.heldPeak >= recording.peak)
        assertEquals(100L, recording.updatedAtMs)
    }

    @Test
    fun missingTargetDoesNotInventAMeter() {
        val previous = mapOf("track" to MeterBallisticsState(peak = 0.3f))
        val result = RecordingTrackMeterPolicy.update(previous, null, 0.9f, 0.4f, 100L)
        assertSame(previous, result)
    }
}
