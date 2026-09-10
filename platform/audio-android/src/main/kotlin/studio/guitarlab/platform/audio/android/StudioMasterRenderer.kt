package studio.guitarlab.platform.audio.android

import java.io.File
import kotlin.math.min
import studio.guitarlab.core.codec.FloatWavFileWriter

data class StudioMasterRenderClip(
    val file: File,
    val trackId: String,
    val timelineStartFrame: Long,
    val sourceStartFrame: Long,
    val lengthFrames: Long,
    val gainDb: Float = 0f,
    val fadeInFrames: Long = 0,
    val fadeOutFrames: Long = 0,
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
        val readers = request.clips.map { clip -> StudioPcmClipReader(StudioPcmClip(
            clip.file, clip.trackId, clip.timelineStartFrame, clip.sourceStartFrame, clip.lengthFrames,
            clip.gainDb, 0f, clip.fadeInFrames, clip.fadeOutFrames,
        ), request.sampleRateHz) }
        try {
            FloatWavFileWriter(output, request.sampleRateHz, 2).use { writer ->
                var renderFrame = 0L
                val mix = FloatArray(CHUNK_FRAMES * 2)
                val trackBuffers = request.trackMixes.associate { it.trackId to FloatArray(CHUNK_FRAMES * 2) }
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
                        StudioPcmMixKernel.applyTrack(buffer, samples, track.gainDb, track.pan, audible = true)
                        for (i in 0 until samples) mix[i] += buffer[i]
                    }
                    StudioPcmMixKernel.applyMaster(mix, samples, request.masterGainDb)
                    writer.writeInterleaved(mix, frames)
                    renderFrame += frames
                }
            }
        } finally {
            readers.forEach { runCatching { it.close() } }
        }
    }

    private const val CHUNK_FRAMES = 1024
}
