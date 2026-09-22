package studio.guitarlab.core.audio

/** Evidence basis used to align capture and backing clocks for one recording session. */
enum class RecordingTimingEvidenceBasis {
    TIMESTAMP_ANCHORED,
    COMMAND_TIME_FALLBACK,
    MIXED_CLOCK_REJECTED,
    NO_BACKING_REFERENCE,
    UNAVAILABLE,
}

enum class RecordingSessionHealthClass {
    OK,
    DEGRADED,
    INVALID_FOR_ROUTE_SPECIFIC_COMPENSATION,
}

/**
 * Sanitized, bounded diagnostic evidence for one recording session.
 *
 * This record is deliberately observational: it never feeds compensation back into project state.
 * It contains no audio samples, private file paths or Android transient numeric device ids.
 */
data class RecordingSessionHealthRecord(
    val completedAtEpochMs: Long,
    val selectedInputIdentity: String?,
    val effectiveInputIdentity: String?,
    val selectedOutputIdentity: String?,
    val effectiveOutputIdentity: String?,
    val sampleRateHz: Int,
    val timingEvidenceBasis: RecordingTimingEvidenceBasis,
    val captureAnchorJitterNs: Long?,
    val backingAnchorJitterNs: Long?,
    val captureAnchorObservations: Int?,
    val backingAnchorObservations: Int?,
    val sessionDeltaFrames: Long,
    val acceptedRouteLatencyFrames: Long,
    val residualFineAdjustmentFrames: Long,
    val outputFallback: Boolean,
    val routeChanged: Boolean,
    val capturedFrames: Long,
    val finalTimelineStartFrame: Long?,
    val finalSourceStartFrame: Long?,
    val finalLengthFrames: Long?,
    val inputZeroReadEvents: Int = 0,
    val outputUnderrunCount: Int? = null,
    val completed: Boolean,
    val failureReason: String? = null,
) {
    val healthClass: RecordingSessionHealthClass
        get() = RecordingSessionHealthPolicy.classify(this)
}

data class RecordingStartupOffsetEvidence(
    val offsetFrames: Long,
    val basis: RecordingTimingEvidenceBasis,
)

object RecordingSessionHealthPolicy {
    const val MAX_HISTORY = 12

    fun classify(record: RecordingSessionHealthRecord): RecordingSessionHealthClass {
        if (record.routeChanged || record.outputFallback) {
            return RecordingSessionHealthClass.INVALID_FOR_ROUTE_SPECIFIC_COMPENSATION
        }
        if (!record.completed || !record.failureReason.isNullOrBlank()) return RecordingSessionHealthClass.DEGRADED
        if (record.inputZeroReadEvents > 0 || (record.outputUnderrunCount ?: 0) > 0) {
            return RecordingSessionHealthClass.DEGRADED
        }
        return when (record.timingEvidenceBasis) {
            RecordingTimingEvidenceBasis.TIMESTAMP_ANCHORED -> {
                val captureJitter = record.captureAnchorJitterNs ?: Long.MAX_VALUE
                val backingJitter = record.backingAnchorJitterNs ?: Long.MAX_VALUE
                if (captureJitter <= AudioClockAnchorPolicy.DEFAULT_MAX_JITTER_NS &&
                    backingJitter <= AudioClockAnchorPolicy.DEFAULT_MAX_JITTER_NS &&
                    (record.captureAnchorObservations ?: 0) >= 2 &&
                    (record.backingAnchorObservations ?: 0) >= 2
                ) RecordingSessionHealthClass.OK else RecordingSessionHealthClass.DEGRADED
            }
            RecordingTimingEvidenceBasis.COMMAND_TIME_FALLBACK,
            RecordingTimingEvidenceBasis.MIXED_CLOCK_REJECTED,
            RecordingTimingEvidenceBasis.NO_BACKING_REFERENCE,
            RecordingTimingEvidenceBasis.UNAVAILABLE -> RecordingSessionHealthClass.DEGRADED
        }
    }

    /** Keeps newest records only; order is deterministic oldest -> newest. */
    fun bounded(history: List<RecordingSessionHealthRecord>, next: RecordingSessionHealthRecord): List<RecordingSessionHealthRecord> =
        (history + next).takeLast(MAX_HISTORY)
}
