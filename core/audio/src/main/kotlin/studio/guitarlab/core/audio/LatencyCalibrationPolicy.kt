package studio.guitarlab.core.audio

import kotlin.math.abs
import kotlin.math.roundToLong

data class LatencyCalibration(
    val latencyFrames: Long,
    val jitterFrames: Long,
    val driftPpm: Double,
    val confidence: Float,
    val sampleRateHz: Int,
    val attempts: Int,
    val accepted: Boolean,
    val measuredAtEpochMs: Long,
)

data class CompensatedTakePlacement(
    val timelineStartFrame: Long,
    val sourceStartFrame: Long,
    val lengthFrames: Long,
)

/** Exact persistence/lookup scope for route-specific calibration and residual correction. */
object LatencyCalibrationScopeKeyPolicy {
    fun storageKey(inputSignature: String?, outputSignature: String?, sampleRateHz: Int): String? {
        require(sampleRateHz > 0)
        val input = inputSignature?.trim().orEmpty()
        val output = outputSignature?.trim().orEmpty()
        if (input.isBlank() || output.isBlank()) return null
        // Length-prefix each signature so the key is unambiguous without a lossy 32-bit hash.
        return "v2:${input.length}:$input|${output.length}:$output|$sampleRateHz"
    }
}

object LatencyCalibrationPolicy {
    const val MIN_ATTEMPTS = 3
    const val MIN_CONFIDENCE = 0.55f
    const val MAX_JITTER_MS = 8.0
    const val MAX_ABS_DRIFT_PPM = 2_000.0

    fun evaluate(
        measurementsFrames: List<Long>,
        confidences: List<Float>,
        sampleRateHz: Int,
        elapsedMs: Long,
        measuredAtEpochMs: Long,
    ): LatencyCalibration {
        require(sampleRateHz > 0)
        require(measurementsFrames.isNotEmpty())
        require(measurementsFrames.size == confidences.size)
        require(measurementsFrames.all { it >= 0L }) { "Latency measurements cannot be negative." }
        require(confidences.all { it.isFinite() && it in 0f..1f }) { "Calibration confidence must be finite and within 0..1." }
        val sorted = measurementsFrames.sorted()
        val median = sorted[sorted.size / 2]
        val deviations = measurementsFrames.map { abs(it - median) }.sorted()
        val jitter = deviations[deviations.size / 2]
        val confidence = confidences.average().toFloat()
        val driftPpm = if (measurementsFrames.size >= 2 && elapsedMs > 0L) {
            val deltaFrames = measurementsFrames.last().toDouble() - measurementsFrames.first().toDouble()
            deltaFrames / sampleRateHz.toDouble() / (elapsedMs.toDouble() / 1000.0) * 1_000_000.0
        } else 0.0
        val jitterMs = jitter.toDouble() * 1000.0 / sampleRateHz.toDouble()
        val accepted = measurementsFrames.size >= MIN_ATTEMPTS &&
            confidence >= MIN_CONFIDENCE &&
            jitterMs <= MAX_JITTER_MS &&
            driftPpm.isFinite() && abs(driftPpm) <= MAX_ABS_DRIFT_PPM
        return LatencyCalibration(median, jitter, driftPpm, confidence, sampleRateHz, measurementsFrames.size, accepted, measuredAtEpochMs)
    }
}

object LatencyCompensationPolicy {
    fun compensate(
        requestedTimelineStartFrame: Long,
        capturedFrames: Long,
        roundTripLatencyFrames: Long,
    ): CompensatedTakePlacement {
        require(requestedTimelineStartFrame >= 0L)
        require(capturedFrames > 0L)
        val latency = roundTripLatencyFrames.coerceAtLeast(0L)
        if (latency == 0L) return CompensatedTakePlacement(requestedTimelineStartFrame, 0L, capturedFrames)
        if (requestedTimelineStartFrame >= latency) {
            return CompensatedTakePlacement(requestedTimelineStartFrame - latency, 0L, capturedFrames)
        }
        val sourceTrim = (latency - requestedTimelineStartFrame).coerceAtMost(capturedFrames - 1L)
        return CompensatedTakePlacement(0L, sourceTrim, capturedFrames - sourceTrim)
    }
}

/** Signed, route-specific residual correction used only after automatic timing has done its work. */
object LatencyFineAdjustmentPolicy {
    const val MAX_ABS_MILLISECONDS = 500.0

    fun millisecondsToFrames(milliseconds: Double, sampleRateHz: Int): Long {
        require(sampleRateHz > 0)
        require(milliseconds.isFinite()) { "Fine latency adjustment must be finite." }
        val bounded = milliseconds.coerceIn(-MAX_ABS_MILLISECONDS, MAX_ABS_MILLISECONDS)
        return (bounded * sampleRateHz.toDouble() / 1000.0).roundToLong()
    }

    fun framesToMilliseconds(frames: Long, sampleRateHz: Int): Double {
        require(sampleRateHz > 0)
        return frames.toDouble() * 1000.0 / sampleRateHz.toDouble()
    }

    fun clampFrames(frames: Long, sampleRateHz: Int): Long {
        require(sampleRateHz > 0)
        val limit = (MAX_ABS_MILLISECONDS * sampleRateHz.toDouble() / 1000.0).toLong()
        return frames.coerceIn(-limit, limit)
    }
}
