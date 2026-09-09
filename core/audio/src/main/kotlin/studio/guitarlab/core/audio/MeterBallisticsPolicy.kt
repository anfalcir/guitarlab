package studio.guitarlab.core.audio

import kotlin.math.max
import kotlin.math.pow

data class MeterBallisticsState(
    val peak: Float = 0f,
    val rms: Float = 0f,
    val heldPeak: Float = 0f,
    val holdUntilMs: Long = 0L,
    val updatedAtMs: Long = 0L,
)

object MeterBallisticsPolicy {
    const val PEAK_HOLD_MS: Long = 750L
    private const val PEAK_DECAY_DB_PER_SECOND = 18f
    private const val RMS_DECAY_DB_PER_SECOND = 24f
    private const val HELD_PEAK_DECAY_DB_PER_SECOND = 12f

    fun update(
        previous: MeterBallisticsState,
        rawPeak: Float,
        rawRms: Float,
        nowMs: Long,
    ): MeterBallisticsState {
        val safePeak = rawPeak.coerceAtLeast(0f)
        val safeRms = rawRms.coerceAtLeast(0f)
        val elapsedMs = if (previous.updatedAtMs <= 0L) 0L else (nowMs - previous.updatedAtMs).coerceAtLeast(0L)

        val peak = max(safePeak, decay(previous.peak, elapsedMs, PEAK_DECAY_DB_PER_SECOND))
        val rms = max(safeRms, decay(previous.rms, elapsedMs, RMS_DECAY_DB_PER_SECOND))

        val heldPeak: Float
        val holdUntilMs: Long
        if (safePeak >= previous.heldPeak) {
            heldPeak = safePeak
            holdUntilMs = nowMs + PEAK_HOLD_MS
        } else if (nowMs <= previous.holdUntilMs) {
            heldPeak = previous.heldPeak
            holdUntilMs = previous.holdUntilMs
        } else {
            heldPeak = max(safePeak, decay(previous.heldPeak, elapsedMs, HELD_PEAK_DECAY_DB_PER_SECOND))
            holdUntilMs = previous.holdUntilMs
        }

        return MeterBallisticsState(
            peak = peak,
            rms = rms,
            heldPeak = heldPeak,
            holdUntilMs = holdUntilMs,
            updatedAtMs = nowMs,
        )
    }

    fun reset(): MeterBallisticsState = MeterBallisticsState()

    private fun decay(value: Float, elapsedMs: Long, dbPerSecond: Float): Float {
        if (value <= 0f || elapsedMs <= 0L) return value.coerceAtLeast(0f)
        val decayDb = dbPerSecond * (elapsedMs / 1000f)
        return value * 10.0.pow(-decayDb / 20.0).toFloat()
    }
}
