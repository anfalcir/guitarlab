package studio.guitarlab.core.codec

import kotlin.math.abs

data class WaveformEnvelope(
    val peaks: List<Float>,
    val channelPeaks: List<List<Float>> = emptyList(),
)

object WaveformEnvelopeBuilder {
    fun build(decoder: AudioFrameDecoder, targetPoints: Int = 320): WaveformEnvelope {
        require(targetPoints > 0) { "targetPoints must be positive" }
        val totalFrames = decoder.metadata.totalFrames
        val channels = decoder.metadata.channelCount
        if (totalFrames == 0L) return WaveformEnvelope(emptyList(), List(channels) { emptyList() })

        val pointCount = minOf(targetPoints.toLong(), totalFrames).toInt()
        val framesPerPoint = (totalFrames + pointCount - 1L) / pointCount
        val peaks = ArrayList<Float>(pointCount)
        val perChannel = List(channels) { ArrayList<Float>(pointCount) }
        val bufferFrames = minOf(4096L, framesPerPoint).toInt().coerceAtLeast(1)
        val buffer = FloatArray(bufferFrames * channels)

        decoder.seekToFrame(0L)
        var consumed = 0L
        repeat(pointCount) {
            var remaining = minOf(framesPerPoint, totalFrames - consumed)
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
                consumed += framesRead
                remaining -= framesRead
            }
            peaks += aggregatePeak.coerceIn(0f, 1f)
            repeat(channels) { channel -> perChannel[channel] += channelPeak[channel].coerceIn(0f, 1f) }
        }
        return WaveformEnvelope(peaks, perChannel.map { it.toList() })
    }
}
