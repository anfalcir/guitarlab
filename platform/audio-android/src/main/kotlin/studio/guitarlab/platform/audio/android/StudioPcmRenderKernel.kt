package studio.guitarlab.platform.audio.android

import java.io.File
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import studio.guitarlab.core.audio.ClipFadePolicy
import studio.guitarlab.core.audio.TrackMixPolicy
import studio.guitarlab.core.codec.FileSeekableByteSource
import studio.guitarlab.core.codec.WavPcmDecoder

/** Shared deterministic PCM path used by realtime playback and offline master rendering. */
internal data class StudioPcmClip(
    val file: File,
    val trackId: String,
    val timelineStartFrame: Long,
    val sourceStartFrame: Long,
    val lengthFrames: Long,
    val gainDb: Float = 0f,
    val pan: Float = 0f,
    val fadeInFrames: Long = 0,
    val fadeOutFrames: Long = 0,
)

internal class StudioPcmClipReader(
    private val clip: StudioPcmClip,
    expectedSampleRateHz: Int,
) : AutoCloseable {
    val trackId: String = clip.trackId
    private val source = FileSeekableByteSource(clip.file)
    private val decoder = WavPcmDecoder(source)
    private val channels = decoder.metadata.channelCount
    private val stereoGain = TrackMixPolicy.channelGains(clip.gainDb, clip.pan)
    private var scratch = FloatArray(0)

    init {
        require(decoder.metadata.sampleRateHz == expectedSampleRateHz) { "PCM media and project sample rates must match." }
        require(channels in 1..2) { "PCM render supports mono/stereo editing media." }
    }

    fun mixInto(renderStartFrame: Long, frameCount: Int, destinationStereo: FloatArray) {
        require(frameCount >= 0 && destinationStereo.size >= frameCount * 2)
        val renderEndFrame = renderStartFrame + frameCount
        val clipEndFrame = clip.timelineStartFrame + clip.lengthFrames
        val overlapStart = max(renderStartFrame, clip.timelineStartFrame)
        val overlapEnd = min(renderEndFrame, clipEndFrame)
        if (overlapStart >= overlapEnd) return
        val requested = (overlapEnd - overlapStart).toInt()
        decoder.seekToFrame(clip.sourceStartFrame + overlapStart - clip.timelineStartFrame)
        val requiredSamples = requested * channels
        if (scratch.size < requiredSamples) scratch = FloatArray(requiredSamples)
        val decoded = decoder.readInterleaved(scratch, frameCount = requested)
        val destinationFrameOffset = (overlapStart - renderStartFrame).toInt()
        for (frame in 0 until decoded) {
            val dst = (destinationFrameOffset + frame) * 2
            val localFrame = overlapStart - clip.timelineStartFrame + frame
            val envelope = ClipFadePolicy.gain(localFrame, clip.lengthFrames, clip.fadeInFrames, clip.fadeOutFrames)
            if (channels == 1) {
                val sample = scratch[frame] * envelope
                destinationStereo[dst] += sample * stereoGain.left
                destinationStereo[dst + 1] += sample * stereoGain.right
            } else {
                val src = frame * 2
                destinationStereo[dst] += scratch[src] * stereoGain.left * envelope
                destinationStereo[dst + 1] += scratch[src + 1] * stereoGain.right * envelope
            }
        }
    }

    override fun close() { decoder.close(); source.close() }
}

internal object StudioPcmMixKernel {
    fun applyTrack(samples: FloatArray, sampleCount: Int, gainDb: Float, pan: Float, audible: Boolean) {
        if (!audible) {
            java.util.Arrays.fill(samples, 0, sampleCount, 0f)
            return
        }
        val gain = TrackMixPolicy.channelGains(gainDb, pan)
        var index = 0
        while (index + 1 < sampleCount) {
            samples[index] *= gain.left
            samples[index + 1] *= gain.right
            index += 2
        }
    }

    fun applyMaster(samples: FloatArray, sampleCount: Int, gainDb: Float) {
        val linear = 10.0.pow(gainDb.coerceIn(-60f, 12f).toDouble() / 20.0).toFloat()
        for (index in 0 until sampleCount) samples[index] = (samples[index] * linear).coerceIn(-1f, 1f)
    }
}
