package studio.guitarlab.platform.audio.android

internal data class CueWriteDrainResult(
    val writtenSamples: Int,
    val pendingSamples: Int,
    val fatalError: Boolean,
)

/**
 * Bounded non-blocking queue for the secondary sink.
 *
 * Partial/zero AudioTrack writes are normal scheduling/backpressure signals. They must not stall
 * MAIN and must not immediately kill CUE. Samples stay ordered until accepted, while a hard
 * bounded backlog still fails closed before latency can grow without limit.
 */
internal class CueNonBlockingWriteQueue(
    private val maxPendingSamples: Int,
    private val maxWritesPerDrain: Int = 4,
) {
    private data class Chunk(val samples: FloatArray, var offset: Int = 0)
    private val chunks = ArrayDeque<Chunk>()
    var pendingSamples: Int = 0
        private set

    init {
        require(maxPendingSamples > 0)
        require(maxWritesPerDrain > 0)
    }

    fun clear() {
        chunks.clear()
        pendingSamples = 0
    }

    fun enqueue(samples: FloatArray, sampleCount: Int): Boolean {
        require(sampleCount >= 0 && sampleCount <= samples.size)
        if (sampleCount == 0) return true
        if (pendingSamples + sampleCount > maxPendingSamples) return false
        chunks.addLast(Chunk(samples.copyOf(sampleCount)))
        pendingSamples += sampleCount
        return true
    }

    fun drain(write: (FloatArray, Int, Int) -> Int): CueWriteDrainResult {
        var writtenTotal = 0
        repeat(maxWritesPerDrain) {
            val chunk = chunks.firstOrNull() ?: return CueWriteDrainResult(
                writtenSamples = writtenTotal,
                pendingSamples = pendingSamples,
                fatalError = false,
            )
            val remaining = chunk.samples.size - chunk.offset
            val result = write(chunk.samples, chunk.offset, remaining)
            if (result < 0 || result > remaining) {
                return CueWriteDrainResult(writtenTotal, pendingSamples, fatalError = true)
            }
            if (result == 0) {
                return CueWriteDrainResult(writtenTotal, pendingSamples, fatalError = false)
            }
            chunk.offset += result
            pendingSamples -= result
            writtenTotal += result
            if (chunk.offset == chunk.samples.size) chunks.removeFirst()
        }
        return CueWriteDrainResult(writtenTotal, pendingSamples, fatalError = false)
    }
}
