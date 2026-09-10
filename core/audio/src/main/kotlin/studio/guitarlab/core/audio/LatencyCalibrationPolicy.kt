package studio.guitarlab.core.audio

import kotlin.math.abs

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

object LatencyCalibrationPolicy {
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
        val sorted = measurementsFrames.sorted()
        val median = sorted[sorted.size / 2].coerceAtLeast(0L)
        val deviations = measurementsFrames.map { abs(it - median) }.sorted()
        val jitter = deviations[deviations.size / 2]
        val confidence = confidences.average().toFloat()
        val driftPpm = if (measurementsFrames.size >= 2 && elapsedMs > 0L) {
            val deltaFrames = measurementsFrames.last() - measurementsFrames.first()
            deltaFrames.toDouble() / sampleRateHz.toDouble() / (elapsedMs / 1000.0) * 1_000_000.0
        } else 0.0
        val jitterMs = jitter * 1000.0 / sampleRateHz
        val accepted = confidence >= MIN_CONFIDENCE && jitterMs <= MAX_JITTER_MS && abs(driftPpm) <= MAX_ABS_DRIFT_PPM
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
