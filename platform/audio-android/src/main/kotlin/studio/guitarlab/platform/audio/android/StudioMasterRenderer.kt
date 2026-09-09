package studio.guitarlab.platform.audio.android

import java.io.File
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import studio.guitarlab.core.audio.TrackMixPolicy
import studio.guitarlab.core.codec.FileSeekableByteSource
import studio.guitarlab.core.codec.FloatWavFileWriter
import studio.guitarlab.core.codec.WavPcmDecoder

data class StudioMasterRenderClip(
    val file: File,
    val trackId: String,
    val timelineStartFrame: Long,
    val sourceStartFrame: Long,
    val lengthFrames: Long,
    val gainDb: Float = 0f,
)

data class StudioMasterRenderTrack(
    val trackId: String,
    val gainDb: Float = 0f,
    val pan: Float = 0f,
)

data class StudioMasterRenderRequest(
    val sampleRateHz: Int,
    val projectEndFrame: Long,
    val clips: List<StudioMasterRenderClip>,
    val trackMixes: List<StudioMasterRenderTrack>,
    val masterGainDb: Float = 0f,
)

/** Deterministic offline renderer. It shares the same gain/pan law as real-time playback. */
object StudioMasterRenderer {
    fun renderFloatWav(request: StudioMasterRenderRequest, output: File) {
        require(request.sampleRateHz > 0)
        require(request.projectEndFrame > 0)
        val trackMixById = request.trackMixes.associateBy { it.trackId }
        val readers = request.clips.map { ClipReader(it, request.sampleRateHz) }
        try {
            FloatWavFileWriter(output, request.sampleRateHz, 2).use { writer ->
                var renderFrame = 0L
                val mix = FloatArray(CHUNK_FRAMES * 2)
                val trackBuffers = request.trackMixes.associate { it.trackId to FloatArray(CHUNK_FRAMES * 2) }
                val masterLinear = 10.0.pow(request.masterGainDb / 20.0).toFloat()
                while (renderFrame < request.projectEndFrame) {
                    val frames = min(CHUNK_FRAMES.toLong(), request.projectEndFrame - renderFrame).toInt()
                    val samples = frames * 2
                    java.util.Arrays.fill(mix, 0, samples, 0f)
                    trackBuffers.values.forEach { java.util.Arrays.fill(it, 0, samples, 0f) }
                    readers.forEach { reader ->
                        val target = trackBuffers[reader.trackId] ?: return@forEach
                        reader.mixInto(renderFrame, frames, target)
                    }
                    trackBuffers.forEach { (trackId, buffer) ->
                        val track = trackMixById[trackId] ?: return@forEach
                        val gains = TrackMixPolicy.channelGains(track.gainDb, track.pan)
                        var i = 0
                        while (i < samples) {
                            mix[i] += buffer[i] * gains.left
                            mix[i + 1] += buffer[i + 1] * gains.right
                            i += 2
                        }
                    }
                    for (i in 0 until samples) mix[i] = (mix[i] * masterLinear).coerceIn(-1f, 1f)
                    writer.writeInterleaved(mix, frames)
                    renderFrame += frames
                }
            }
        } finally {
            readers.forEach { runCatching { it.close() } }
        }
    }

    private class ClipReader(private val clip: StudioMasterRenderClip, expectedSampleRateHz: Int) : AutoCloseable {
        val trackId: String = clip.trackId
        private val source = FileSeekableByteSource(clip.file)
        private val decoder = WavPcmDecoder(source)
        private val clipGain = TrackMixPolicy.channelGains(clip.gainDb, 0f)

        init {
            require(decoder.metadata.sampleRateHz == expectedSampleRateHz) { "Master export requires media at the project sample rate." }
            require(decoder.metadata.channelCount in 1..2) { "Master export supports mono/stereo editing media." }
        }

        fun mixInto(renderStartFrame: Long, frameCount: Int, destination: FloatArray) {
            val overlapStart = max(renderStartFrame, clip.timelineStartFrame)
            val overlapEnd = min(renderStartFrame + frameCount, clip.timelineStartFrame + clip.lengthFrames)
            if (overlapStart >= overlapEnd) return
            val requested = (overlapEnd - overlapStart).toInt()
            decoder.seekToFrame(clip.sourceStartFrame + overlapStart - clip.timelineStartFrame)
            val channels = decoder.metadata.channelCount
            val temp = FloatArray(requested * channels)
            val decoded = decoder.readInterleaved(temp, frameCount = requested)
            val dstOffset = (overlapStart - renderStartFrame).toInt()
            for (frame in 0 until decoded) {
                val dst = (dstOffset + frame) * 2
                if (channels == 1) {
                    val sample = temp[frame]
                    destination[dst] += sample * clipGain.left
                    destination[dst + 1] += sample * clipGain.right
                } else {
                    destination[dst] += temp[frame * 2] * clipGain.left
                    destination[dst + 1] += temp[frame * 2 + 1] * clipGain.right
                }
            }
        }

        override fun close() {
            decoder.close()
            source.close()
        }
    }

    private const val CHUNK_FRAMES = 1024
}
