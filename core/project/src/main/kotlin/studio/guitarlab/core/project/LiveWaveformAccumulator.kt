package studio.guitarlab.core.project

import kotlin.math.max
import kotlin.math.min

/** One bounded envelope segment with explicit recording-frame coverage. */
data class LiveWaveformPoint(
    val startFrame: Long,
    val endFrameExclusive: Long,
    val peak: Float,
) {
    init {
        require(startFrame >= 0L) { "Waveform start frame must be non-negative." }
        require(endFrameExclusive > startFrame) { "Waveform point must cover at least one frame." }
        require(peak in 0f..1f) { "Waveform peak must be normalized." }
    }
}

/**
 * Bounded online envelope whose timebase is recording frames, never callback count.
 *
 * The active bucket width is global for the whole recording. When capacity is exceeded we double
 * that width and re-bin *all* existing points, then keep using the same width for future samples.
 * This avoids the old mixed-resolution shape where historical audio became sparse while newly
 * captured audio stayed dense. Every snapshot therefore covers [0, framesCaptured) contiguously
 * with one uniform temporal resolution (apart from the final partial bucket).
 */
class LiveWaveformAccumulator(private val maxPoints: Int = 512) {
    init { require(maxPoints >= 8) }

    private val points = ArrayList<LiveWaveformPoint>(maxPoints)
    private var lastFrameExclusive = 0L
    private var bucketFrames = 0L

    @Synchronized
    fun append(framesCaptured: Long, peak: Float) {
        require(framesCaptured >= 0L) { "Captured frames must be non-negative." }
        if (framesCaptured <= lastFrameExclusive) return

        if (bucketFrames == 0L) {
            bucketFrames = (framesCaptured - lastFrameExclusive).coerceAtLeast(1L)
        }
        appendSpan(lastFrameExclusive, framesCaptured, peak.coerceIn(0f, 1f))
        lastFrameExclusive = framesCaptured

        while (points.size > maxPoints) {
            val nextWidth = (bucketFrames * 2L).coerceAtLeast(bucketFrames + 1L)
            rebin(nextWidth)
        }
    }

    @Synchronized
    fun clear() {
        points.clear()
        lastFrameExclusive = 0L
        bucketFrames = 0L
    }

    @Synchronized
    fun snapshot(): List<LiveWaveformPoint> = points.toList()

    @Synchronized
    fun coveredFrames(): Long = lastFrameExclusive

    private fun appendSpan(startFrame: Long, endFrameExclusive: Long, peak: Float) {
        var cursor = startFrame
        while (cursor < endFrameExclusive) {
            val bucketStart = (cursor / bucketFrames) * bucketFrames
            val bucketEnd = min(endFrameExclusive, bucketStart + bucketFrames)
            val last = points.lastOrNull()
            if (last != null && last.startFrame == bucketStart) {
                points[points.lastIndex] = last.copy(
                    endFrameExclusive = max(last.endFrameExclusive, bucketEnd),
                    peak = max(last.peak, peak),
                )
            } else {
                val actualStart = last?.endFrameExclusive ?: cursor
                points += LiveWaveformPoint(
                    startFrame = actualStart,
                    endFrameExclusive = bucketEnd,
                    peak = peak,
                )
            }
            cursor = bucketEnd
        }
    }

    private fun rebin(newBucketFrames: Long) {
        val previous = points.toList()
        points.clear()
        bucketFrames = newBucketFrames
        previous.forEach { point ->
            appendSpan(point.startFrame, point.endFrameExclusive, point.peak)
        }
    }
}
