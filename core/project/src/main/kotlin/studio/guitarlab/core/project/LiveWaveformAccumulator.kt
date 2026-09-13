package studio.guitarlab.core.project

import kotlin.math.max

/** Bounded online envelope. Pairwise max compaction preserves transients on long takes. */
class LiveWaveformAccumulator(private val maxPoints: Int = 512) {
    init { require(maxPoints >= 8) }
    private val peaks = ArrayList<Float>(maxPoints)

    fun append(peak: Float): List<Float> {
        peaks += peak.coerceIn(0f, 1f)
        if (peaks.size > maxPoints) compact()
        return peaks.toList()
    }
    fun clear() = peaks.clear()
    fun snapshot(): List<Float> = peaks.toList()

    private fun compact() {
        var write = 0
        var read = 0
        while (read < peaks.size) {
            peaks[write++] = if (read + 1 < peaks.size) max(peaks[read], peaks[read + 1]) else peaks[read]
            read += 2
        }
        while (peaks.size > write) peaks.removeAt(peaks.lastIndex)
    }
}
