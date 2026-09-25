package studio.guitarlab.core.codec

import kotlin.math.abs

data class WaveformEnvelope(
    val peaks: List<Float>,
    val channelPeaks: List<List<Float>> = emptyList(),
)

object WaveformEnvelopeBuilder {
    fun build(
        decoder: AudioFrameDecoder,
        targetPoints: Int = 320,
        startFrame: Long = 0L,
        frameCount: Long? = null,
    ): WaveformEnvelope {
        require(targetPoints > 0) { "targetPoints must be positive" }
        val totalFrames = decoder.metadata.totalFrames
        val channels = decoder.metadata.channelCount
        require(startFrame in 0L..totalFrames) { "startFrame must be inside the decoded media" }
        val availableFrames = (totalFrames - startFrame).coerceAtLeast(0L)
        val requestedFrames = frameCount?.also { require(it >= 0L) { "frameCount must be non-negative" } }
            ?.coerceAtMost(availableFrames)
            ?: availableFrames
        if (requestedFrames == 0L) return WaveformEnvelope(emptyList(), List(channels) { emptyList() })

        val pointCount = minOf(targetPoints.toLong(), requestedFrames).toInt()
        val peaks = ArrayList<Float>(pointCount)
        val perChannel = List(channels) { ArrayList<Float>(pointCount) }
        val maxBucketFrames = ((requestedFrames + pointCount - 1L) / pointCount).coerceAtLeast(1L)
        val bufferFrames = minOf(4096L, maxBucketFrames).toInt()
        val buffer = FloatArray(bufferFrames * channels)

        decoder.seekToFrame(startFrame)
        repeat(pointCount) { point ->
            val bucketStart = point.toLong() * requestedFrames / pointCount
            val bucketEnd = (point.toLong() + 1L) * requestedFrames / pointCount
            var remaining = (bucketEnd - bucketStart).coerceAtLeast(1L)
            var aggregatePeak = 0f
            val channelPeak = FloatArray(channels)
            while (remaining > 0L) {
                val requested = minOf(bufferFrames.toLong(), remaining).toInt()
                val framesRead = decoder.readInterleaved(buffer, 0, requested)
                if (framesRead <= 0) break
                repeat(framesRead) { frame ->
                    repeat(channels) { channel ->
                        val value = abs(buffer[frame * channels + channel])
                        aggregatePeak = maxOf(aggregatePeak, value)
                        channelPeak[channel] = maxOf(channelPeak[channel], value)
                    }
                }
                remaining -= framesRead
            }
            peaks += aggregatePeak.coerceIn(0f, 1f)
            repeat(channels) { channel -> perChannel[channel] += channelPeak[channel].coerceIn(0f, 1f) }
        }
        return WaveformEnvelope(peaks, perChannel.map { it.toList() })
    }
}
