package studio.guitarlab.core.audio

import kotlin.math.PI
import kotlin.math.sin

/**
 * Deterministic low-level, band-limited calibration stimulus.
 *
 * A short windowed chirp is substantially less harsh than the previous full-band pseudo-random
 * +/-0.62 burst. Gain is intentionally adaptive and capped at 0.12 (~-18.4 dBFS peak).
 */
object LatencyCalibrationStimulusPolicy {
    const val DURATION_MS = 32.0
    const val START_FREQUENCY_HZ = 700.0
    const val MAX_END_FREQUENCY_HZ = 6_500.0
    const val CORRELATION_THRESHOLD = 0.30f
    val adaptiveGains: List<Float> = listOf(0.03f, 0.06f, 0.12f)

    fun generate(sampleRateHz: Int, gain: Float): FloatArray {
        require(sampleRateHz in 8_000..192_000)
        require(gain.isFinite() && gain > 0f && gain <= adaptiveGains.last())
        val size = (sampleRateHz * DURATION_MS / 1000.0).toInt().coerceAtLeast(128)
        val durationSeconds = size.toDouble() / sampleRateHz.toDouble()
        val endFrequency = MAX_END_FREQUENCY_HZ.coerceAtMost(sampleRateHz * 0.20)
        val sweep = endFrequency - START_FREQUENCY_HZ
        return FloatArray(size) { index ->
            val normalized = if (size <= 1) 0.0 else index.toDouble() / (size - 1).toDouble()
            val seconds = normalized * durationSeconds
            val phase = 2.0 * PI * (
                START_FREQUENCY_HZ * seconds +
                    0.5 * sweep * seconds * seconds / durationSeconds
                )
            val window = sin(PI * normalized).let { it * it }
            (sin(phase) * window * gain).toFloat()
        }
    }
}
