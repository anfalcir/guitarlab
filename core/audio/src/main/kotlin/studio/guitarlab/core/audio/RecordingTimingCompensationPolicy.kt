package studio.guitarlab.core.audio

import kotlin.math.roundToLong

/**
 * Final mapping from recorded source frames into the project timeline.
 *
 * startupOffsetFrames is signed: positive means capture frame zero happened after the backing
 * presentation origin; negative means capture started earlier and therefore contains pre-roll.
 * routeLatencyFrames is measured round-trip latency and is always non-negative.
 * fineAdjustmentFrames is a user-owned residual correction: positive advances the take, negative
 * delays it. It is intentionally independent from measured calibration.
 */
data class RecordingTimingPlacement(
    val timelineStartFrame: Long,
    val sourceStartFrame: Long,
    val lengthFrames: Long,
    val startupOffsetFrames: Long,
    val routeLatencyFrames: Long,
    val fineAdjustmentFrames: Long = 0L,
)

object RecordingTimingCompensationPolicy {
    private const val NANOS_PER_SECOND = 1_000_000_000.0
    private const val MAX_TRUSTED_STARTUP_OFFSET_SECONDS = 5L
    private const val MAX_TRUSTED_STARTUP_OFFSET_NS = MAX_TRUSTED_STARTUP_OFFSET_SECONDS * 1_000_000_000L

    /**
     * Returns capture-origin minus backing-origin in frames.
     *
     * Hardware timestamp time and command time are each internally useful, but mixing one basis
     * with the other creates a systematic fake offset. In that case we deliberately return zero.
     * The delta is signed and saturated before conversion so pathological clock values cannot wrap.
     */
    fun startupOffsetFrames(
        captureStartMonotonicNs: Long,
        captureTimestampBased: Boolean,
        backingPresentationStartMonotonicNs: Long?,
        backingTimestampBased: Boolean,
        sampleRateHz: Int,
    ): Long = startupOffsetEvidence(
        captureStartMonotonicNs = captureStartMonotonicNs,
        captureTimestampBased = captureTimestampBased,
        backingPresentationStartMonotonicNs = backingPresentationStartMonotonicNs,
        backingTimestampBased = backingTimestampBased,
        sampleRateHz = sampleRateHz,
    ).offsetFrames

    fun startupOffsetEvidence(
        captureStartMonotonicNs: Long,
        captureTimestampBased: Boolean,
        backingPresentationStartMonotonicNs: Long?,
        backingTimestampBased: Boolean,
        sampleRateHz: Int,
    ): RecordingStartupOffsetEvidence {
        require(sampleRateHz > 0) { "Sample rate must be positive." }
        val backingStart = backingPresentationStartMonotonicNs
            ?: return RecordingStartupOffsetEvidence(0L, RecordingTimingEvidenceBasis.NO_BACKING_REFERENCE)
        if (captureStartMonotonicNs <= 0L || backingStart <= 0L) {
            return RecordingStartupOffsetEvidence(0L, RecordingTimingEvidenceBasis.UNAVAILABLE)
        }
        if (captureTimestampBased != backingTimestampBased) {
            return RecordingStartupOffsetEvidence(0L, RecordingTimingEvidenceBasis.MIXED_CLOCK_REJECTED)
        }
        val deltaNs = saturatedSubtract(captureStartMonotonicNs, backingStart)
            .coerceIn(-MAX_TRUSTED_STARTUP_OFFSET_NS, MAX_TRUSTED_STARTUP_OFFSET_NS)
        val measured = (deltaNs.toDouble() * sampleRateHz.toDouble() / NANOS_PER_SECOND).roundToLong()
        val maxFrames = sampleRateHz.toLong() * MAX_TRUSTED_STARTUP_OFFSET_SECONDS
        return RecordingStartupOffsetEvidence(
            offsetFrames = measured.coerceIn(-maxFrames, maxFrames),
            basis = if (captureTimestampBased) RecordingTimingEvidenceBasis.TIMESTAMP_ANCHORED
            else RecordingTimingEvidenceBasis.COMMAND_TIME_FALLBACK,
        )
    }

    fun compensate(
        requestedTimelineStartFrame: Long,
        capturedFrames: Long,
        startupOffsetFrames: Long,
        roundTripLatencyFrames: Long,
        fineAdjustmentFrames: Long = 0L,
    ): RecordingTimingPlacement {
        require(requestedTimelineStartFrame >= 0L)
        require(capturedFrames > 0L)
        val routeLatency = roundTripLatencyFrames.coerceAtLeast(0L)
        // Source frame 0 maps to requested + capture/backing clock delta - route latency - residual.
        // Arithmetic saturates rather than wrapping so even corrupt/extreme metadata cannot create
        // a negative/overflowed timeline position.
        val mappedStart = saturatedSubtract(
            saturatedSubtract(
                saturatedAdd(requestedTimelineStartFrame, startupOffsetFrames),
                routeLatency,
            ),
            fineAdjustmentFrames,
        )
        val sourceTrim = if (mappedStart < 0L) {
            saturatedNegate(mappedStart).coerceAtMost(capturedFrames - 1L)
        } else 0L
        return RecordingTimingPlacement(
            timelineStartFrame = mappedStart.coerceAtLeast(0L),
            sourceStartFrame = sourceTrim,
            lengthFrames = capturedFrames - sourceTrim,
            startupOffsetFrames = startupOffsetFrames,
            routeLatencyFrames = routeLatency,
            fineAdjustmentFrames = fineAdjustmentFrames,
        )
    }

    private fun saturatedAdd(left: Long, right: Long): Long = try {
        Math.addExact(left, right)
    } catch (_: ArithmeticException) {
        if (right >= 0L) Long.MAX_VALUE else Long.MIN_VALUE
    }

    private fun saturatedSubtract(left: Long, right: Long): Long = try {
        Math.subtractExact(left, right)
    } catch (_: ArithmeticException) {
        if (right >= 0L) Long.MIN_VALUE else Long.MAX_VALUE
    }

    private fun saturatedNegate(value: Long): Long =
        if (value == Long.MIN_VALUE) Long.MAX_VALUE else -value
}
