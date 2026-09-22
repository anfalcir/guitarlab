package studio.guitarlab.core.audio

import kotlin.math.roundToLong

/** One monotonic audio timestamp sample taken from an Android capture/playback stream. */
data class AudioClockObservation(
    val framePosition: Long,
    val nanoTime: Long,
)

/** Stable estimate of the monotonic time at which stream frame zero crossed the audio boundary. */
data class AudioClockAnchor(
    val streamOriginMonotonicNs: Long,
    val jitterNs: Long,
    val observations: Int,
)

/**
 * Converts repeated frame/timestamp observations into a conservative stream-origin estimate.
 * A single Android AudioTimestamp is deliberately not trusted for recording placement.
 *
 * Android drivers can temporarily repeat a frame position/timestamp while a stream is starting.
 * Repeated/non-advancing samples do not count as independent evidence; backwards clock movement
 * invalidates the anchor instead of being averaged into a plausible-looking result.
 */
object AudioClockAnchorPolicy {
    const val DEFAULT_MAX_JITTER_NS = 20_000_000L

    fun estimate(
        observations: List<AudioClockObservation>,
        sampleRateHz: Int,
        maxJitterNs: Long = DEFAULT_MAX_JITTER_NS,
    ): AudioClockAnchor? {
        require(sampleRateHz > 0) { "Sample rate must be positive." }
        require(maxJitterNs >= 0L)

        val valid = observations.filter { it.framePosition >= 0L && it.nanoTime > 0L }
        if (valid.size < 2) return null

        val progressing = ArrayList<AudioClockObservation>(valid.size)
        valid.forEach { observation ->
            val previous = progressing.lastOrNull()
            if (previous == null) {
                progressing += observation
            } else {
                // A backwards frame/time clock is not a stable monotonic timestamp stream.
                if (observation.framePosition < previous.framePosition || observation.nanoTime < previous.nanoTime) {
                    return null
                }
                // Same frame or same timestamp is stale/non-independent evidence. Ignore it and
                // keep polling; a later observation may advance both dimensions.
                if (observation.framePosition == previous.framePosition || observation.nanoTime == previous.nanoTime) {
                    return@forEach
                }
                progressing += observation
            }
        }
        if (progressing.size < 2) return null

        val origins = progressing.map { observation ->
            saturatedSubtract(observation.nanoTime, framesToNanos(observation.framePosition, sampleRateHz))
        }.sorted()
        val median = origins[origins.size / 2]
        val deviations = origins.map { absoluteDistance(it, median) }.sorted()
        val jitter = deviations[deviations.size / 2]
        val fullSpread = absoluteDistance(origins.last(), origins.first())
        val maxFullSpread = saturatedMultiplyByTwo(maxJitterNs)
        if (jitter > maxJitterNs || fullSpread > maxFullSpread) return null
        return AudioClockAnchor(median, jitter, progressing.size)
    }

    private fun framesToNanos(frames: Long, sampleRateHz: Int): Long {
        val nanos = frames.toDouble() * 1_000_000_000.0 / sampleRateHz.toDouble()
        return when {
            nanos >= Long.MAX_VALUE.toDouble() -> Long.MAX_VALUE
            nanos <= Long.MIN_VALUE.toDouble() -> Long.MIN_VALUE
            else -> nanos.roundToLong()
        }
    }

    private fun saturatedSubtract(left: Long, right: Long): Long = try {
        Math.subtractExact(left, right)
    } catch (_: ArithmeticException) {
        if (right >= 0L) Long.MIN_VALUE else Long.MAX_VALUE
    }

    private fun absoluteDistance(left: Long, right: Long): Long {
        val delta = saturatedSubtract(left, right)
        return when {
            delta == Long.MIN_VALUE -> Long.MAX_VALUE
            delta < 0L -> -delta
            else -> delta
        }
    }

    private fun saturatedMultiplyByTwo(value: Long): Long =
        if (value > Long.MAX_VALUE / 2L) Long.MAX_VALUE else value * 2L
}
