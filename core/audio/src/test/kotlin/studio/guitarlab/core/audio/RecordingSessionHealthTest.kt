package studio.guitarlab.core.audio

import kotlin.test.Test
import kotlin.test.assertEquals

class RecordingSessionHealthTest {
    @Test
    fun timestampEvidenceWithStableAnchorsIsOk() {
        val record = base().copy(
            timingEvidenceBasis = RecordingTimingEvidenceBasis.TIMESTAMP_ANCHORED,
            captureAnchorJitterNs = 1_000_000,
            backingAnchorJitterNs = 2_000_000,
            captureAnchorObservations = 4,
            backingAnchorObservations = 4,
        )
        assertEquals(RecordingSessionHealthClass.OK, record.healthClass)
    }

    @Test
    fun mixedClockEvidenceIsDegradedButDoesNotInventCompensation() {
        val evidence = RecordingTimingCompensationPolicy.startupOffsetEvidence(
            captureStartMonotonicNs = 2_000_000_000L,
            captureTimestampBased = true,
            backingPresentationStartMonotonicNs = 1_900_000_000L,
            backingTimestampBased = false,
            sampleRateHz = 48_000,
        )
        assertEquals(0L, evidence.offsetFrames)
        assertEquals(RecordingTimingEvidenceBasis.MIXED_CLOCK_REJECTED, evidence.basis)
        assertEquals(
            RecordingSessionHealthClass.DEGRADED,
            base().copy(timingEvidenceBasis = evidence.basis).healthClass,
        )
    }

    @Test
    fun fallbackInvalidatesRouteSpecificCompensationHealth() {
        assertEquals(
            RecordingSessionHealthClass.INVALID_FOR_ROUTE_SPECIFIC_COMPENSATION,
            base().copy(outputFallback = true, acceptedRouteLatencyFrames = 240).healthClass,
        )
    }

    @Test
    fun historyIsBoundedAndDeterministic() {
        var history = emptyList<RecordingSessionHealthRecord>()
        repeat(RecordingSessionHealthPolicy.MAX_HISTORY + 7) { index ->
            history = RecordingSessionHealthPolicy.bounded(history, base().copy(completedAtEpochMs = index.toLong()))
        }
        assertEquals(RecordingSessionHealthPolicy.MAX_HISTORY, history.size)
        assertEquals(7L, history.first().completedAtEpochMs)
        assertEquals(18L, history.last().completedAtEpochMs)
    }

    @Test
    fun routeSpecificTermsRemainExactlyThoseAlreadyAccepted() {
        val placement = RecordingTimingCompensationPolicy.compensate(
            requestedTimelineStartFrame = 10_000,
            capturedFrames = 48_000,
            startupOffsetFrames = 120,
            roundTripLatencyFrames = 240,
            fineAdjustmentFrames = 24,
        )
        assertEquals(9_856L, placement.timelineStartFrame)
        assertEquals(120L, placement.startupOffsetFrames)
        assertEquals(240L, placement.routeLatencyFrames)
        assertEquals(24L, placement.fineAdjustmentFrames)
    }

    private fun base() = RecordingSessionHealthRecord(
        completedAtEpochMs = 1L,
        selectedInputIdentity = "usb-in",
        effectiveInputIdentity = "usb-in",
        selectedOutputIdentity = "usb-out",
        effectiveOutputIdentity = "usb-out",
        sampleRateHz = 48_000,
        timingEvidenceBasis = RecordingTimingEvidenceBasis.COMMAND_TIME_FALLBACK,
        captureAnchorJitterNs = null,
        backingAnchorJitterNs = null,
        captureAnchorObservations = null,
        backingAnchorObservations = null,
        sessionDeltaFrames = 0L,
        acceptedRouteLatencyFrames = 0L,
        residualFineAdjustmentFrames = 0L,
        outputFallback = false,
        routeChanged = false,
        capturedFrames = 48_000,
        finalTimelineStartFrame = 0L,
        finalSourceStartFrame = 0L,
        finalLengthFrames = 48_000L,
        completed = true,
    )
}
