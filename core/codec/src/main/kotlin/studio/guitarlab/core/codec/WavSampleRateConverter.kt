package studio.guitarlab.core.codec

import java.io.File
import kotlin.math.PI
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.min
import kotlin.math.roundToLong
import kotlin.math.sin

/**
 * Offline, bounded-memory windowed-sinc sample-rate converter for project editing proxies.
 * Immutable imported sources are never rewritten; callers store this output as a derived proxy.
 */
object WavSampleRateConverter {
    data class Result(
        val sourceRateHz: Int,
        val targetRateHz: Int,
        val sourceFrames: Long,
        val targetFrames: Long,
        val channelCount: Int,
    )

    fun convert(input: File, output: File, targetRateHz: Int): Result {
        FileSeekableByteSource(input).use { source ->
            val decoder = WavPcmDecoder(source)
            decoder.use {
                val meta = decoder.metadata
                require(meta.channelCount in 1..2) { "Resampling supports mono/stereo WAV editing media." }
                require(targetRateHz in 8_000..192_000) { "Unsupported target sample rate: $targetRateHz" }
                if (meta.sampleRateHz == targetRateHz) {
                    input.inputStream().use { i -> output.outputStream().use { o -> i.copyTo(o) } }
                    return Result(meta.sampleRateHz, targetRateHz, meta.totalFrames, meta.totalFrames, meta.channelCount)
                }
                val targetFrames = (meta.totalFrames.toDouble() * targetRateHz / meta.sampleRateHz).roundToLong().coerceAtLeast(1L)
                FloatWavFileWriter(output, targetRateHz, meta.channelCount).use { writer ->
                    var outStart = 0L
                    while (outStart < targetFrames) {
                        val outCount = min(OUTPUT_CHUNK.toLong(), targetFrames - outStart).toInt()
                        val firstPos = outStart.toDouble() * meta.sampleRateHz / targetRateHz
                        val lastPos = (outStart + outCount - 1L).toDouble() * meta.sampleRateHz / targetRateHz
                        val sourceStart = (floor(firstPos).toLong() - RADIUS).coerceAtLeast(0L)
                        val sourceEnd = (ceil(lastPos).toLong() + RADIUS + 1L).coerceAtMost(meta.totalFrames)
                        val sourceCount = (sourceEnd - sourceStart).toInt()
                        val sourceSamples = FloatArray(sourceCount * meta.channelCount)
                        decoder.seekToFrame(sourceStart)
                        val actual = decoder.readInterleaved(sourceSamples, frameCount = sourceCount)
                        val out = FloatArray(outCount * meta.channelCount)
                        val cutoff = min(1.0, targetRateHz.toDouble() / meta.sampleRateHz.toDouble())
                        for (outIndex in 0 until outCount) {
                            val sourcePos = (outStart + outIndex).toDouble() * meta.sampleRateHz / targetRateHz
                            val center = floor(sourcePos).toLong()
                            for (channel in 0 until meta.channelCount) {
                                var sum = 0.0
                                var weights = 0.0
                                for (tap in -RADIUS..RADIUS) {
                                    val absolute = center + tap
                                    if (absolute < sourceStart || absolute >= sourceStart + actual) continue
                                    val distance = sourcePos - absolute.toDouble()
                                    val x = distance * cutoff
                                    val sinc = if (kotlin.math.abs(x) < 1e-12) 1.0 else sin(PI * x) / (PI * x)
                                    val normalizedDistance = kotlin.math.abs(distance) / (RADIUS + 1.0)
                                    if (normalizedDistance >= 1.0) continue
                                    val window = 0.5 * (1.0 + cos(PI * normalizedDistance))
                                    val weight = sinc * window * cutoff
                                    val local = (absolute - sourceStart).toInt()
                                    sum += sourceSamples[local * meta.channelCount + channel] * weight
                                    weights += weight
                                }
                                out[outIndex * meta.channelCount + channel] =
                                    if (kotlin.math.abs(weights) > 1e-12) (sum / weights).toFloat().coerceIn(-1f, 1f) else 0f
                            }
                        }
                        writer.writeInterleaved(out, outCount)
                        outStart += outCount
                    }
                }
                return Result(meta.sampleRateHz, targetRateHz, meta.totalFrames, targetFrames, meta.channelCount)
            }
        }
    }

    private const val RADIUS = 16
    private const val OUTPUT_CHUNK = 4096
}
