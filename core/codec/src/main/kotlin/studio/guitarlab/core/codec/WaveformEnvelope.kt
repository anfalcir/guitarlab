package studio.guitarlab.core.codec

import kotlin.math.abs

data class WaveformEnvelope(val peaks: List<Float>)

object WaveformEnvelopeBuilder {
    fun build(decoder: AudioFrameDecoder, targetPoints: Int = 320): WaveformEnvelope {
        require(targetPoints > 0) { "targetPoints must be positive" }
        val totalFrames = decoder.metadata.totalFrames
        if (totalFrames == 0L) return WaveformEnvelope(emptyList())

        val pointCount = minOf(targetPoints.toLong(), totalFrames).toInt()
        val framesPerPoint = (totalFrames + pointCount - 1L) / pointCount
        val channels = decoder.metadata.channelCount
        val peaks = ArrayList<Float>(pointCount)
        val bufferFrames = minOf(4096L, framesPerPoint).toInt().coerceAtLeast(1)
        val buffer = FloatArray(bufferFrames * channels)

        decoder.seekToFrame(0L)
        var consumed = 0L
        repeat(pointCount) {
            var remaining = minOf(framesPerPoint, totalFrames - consumed)
            var peak = 0f
            while (remaining > 0L) {
                val requested = minOf(bufferFrames.toLong(), remaining).toInt()
                val read = decoder.readInterleaved(buffer, 0, requested)
                if (read <= 0) break
                val sampleCount = read * channels
                for (index in 0 until sampleCount) peak = maxOf(peak, abs(buffer[index]))
                consumed += read
                remaining -= read
            }
            peaks += peak.coerceIn(0f, 1f)
        }
        return WaveformEnvelope(peaks)
    }
}
