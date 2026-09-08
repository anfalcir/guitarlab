package studio.guitarlab.platform.audio.android

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import java.io.File
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import studio.guitarlab.core.audio.PlaybackClockPolicy
import studio.guitarlab.core.codec.FileSeekableByteSource
import studio.guitarlab.core.codec.WavPcmDecoder

data class StudioPlaybackClip(
    val file: File,
    val timelineStartFrame: Long,
    val sourceStartFrame: Long,
    val lengthFrames: Long,
    val gainDb: Float = 0f,
    val muted: Boolean = false,
)

data class StudioPlaybackRequest(
    val sampleRateHz: Int,
    val startFrame: Long,
    val projectEndFrame: Long,
    val loopEnabled: Boolean,
    val loopStartFrame: Long,
    val loopEndFrame: Long,
    val clips: List<StudioPlaybackClip>,
)

interface StudioPlaybackListener {
    fun onPosition(frame: Long)
    fun onStopped(frame: Long)
    fun onError(message: String)
}

class AndroidStudioPlaybackEngine : AutoCloseable {
    @Volatile private var running = false
    @Volatile private var worker: Thread? = null

    @Synchronized
    fun start(request: StudioPlaybackRequest, listener: StudioPlaybackListener) {
        check(!running) { "Playback engine is already running." }
        require(request.sampleRateHz > 0) { "Playback sample rate must be positive." }
        require(request.projectEndFrame > 0) { "Project must contain playable timeline frames." }
        require(request.startFrame in 0..request.projectEndFrame) { "Playback start is outside the project timeline." }
        if (request.loopEnabled) {
            require(request.loopStartFrame >= 0 && request.loopEndFrame > request.loopStartFrame) { "Invalid loop range." }
            require(request.loopEndFrame <= request.projectEndFrame) { "Loop range exceeds project end." }
        }

        running = true
        worker = Thread({ runPlayback(request, listener) }, "GuitarLab-StudioPlayback").also { it.start() }
    }

    @Synchronized
    fun stop() {
        running = false
        worker?.interrupt()
    }

    override fun close() {
        stop()
    }

    private fun runPlayback(request: StudioPlaybackRequest, listener: StudioPlaybackListener) {
        var lastTimelineFrame = request.startFrame
        val readers = mutableListOf<ClipReader>()
        var track: AudioTrack? = null
        try {
            request.clips.filterNot { it.muted }.forEach { readers += ClipReader(it, request.sampleRateHz) }
            require(readers.isNotEmpty()) { "No playable managed WAV clips are available." }

            val channelMask = AudioFormat.CHANNEL_OUT_STEREO
            val minBytes = AudioTrack.getMinBufferSize(request.sampleRateHz, channelMask, AudioFormat.ENCODING_PCM_FLOAT)
            require(minBytes > 0) { "Android could not allocate a playback buffer for ${request.sampleRateHz} Hz." }
            val bufferBytes = max(minBytes, CHUNK_FRAMES * 2 * Float.SIZE_BYTES * 4)
            track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_FLOAT)
                        .setSampleRate(request.sampleRateHz)
                        .setChannelMask(channelMask)
                        .build()
                )
                .setTransferMode(AudioTrack.MODE_STREAM)
                .setBufferSizeInBytes(bufferBytes)
                .setPerformanceMode(AudioTrack.PERFORMANCE_MODE_LOW_LATENCY)
                .build()
            require(track.state == AudioTrack.STATE_INITIALIZED) { "Android AudioTrack failed to initialize." }

            var renderFrame = normalizedStart(request)
            var totalWrittenFrames = 0L
            val mix = FloatArray(CHUNK_FRAMES * 2)
            track.play()

            while (running) {
                if (!request.loopEnabled && renderFrame >= request.projectEndFrame) break
                val boundary = if (request.loopEnabled) request.loopEndFrame else request.projectEndFrame
                if (renderFrame >= boundary) {
                    renderFrame = request.loopStartFrame
                    continue
                }

                val framesToRender = min(CHUNK_FRAMES.toLong(), boundary - renderFrame).toInt()
                java.util.Arrays.fill(mix, 0, framesToRender * 2, 0f)
                readers.forEach { it.mixInto(renderFrame, framesToRender, mix) }
                for (index in 0 until framesToRender * 2) mix[index] = mix[index].coerceIn(-1f, 1f)

                val samplesWritten = track.write(mix, 0, framesToRender * 2, AudioTrack.WRITE_BLOCKING)
                if (samplesWritten < 0) error("AudioTrack write failed with code $samplesWritten.")
                val writtenFrames = samplesWritten / 2
                if (writtenFrames <= 0) error("AudioTrack made no playback progress.")
                totalWrittenFrames += writtenFrames
                renderFrame += writtenFrames
                if (request.loopEnabled && renderFrame >= request.loopEndFrame) renderFrame = request.loopStartFrame

                val presentedFrames = track.playbackHeadPosition.toLong() and 0xffffffffL
                lastTimelineFrame = PlaybackClockPolicy.timelineFrame(
                    startFrame = normalizedStart(request),
                    presentedFrames = presentedFrames,
                    projectEndFrame = request.projectEndFrame,
                    loopEnabled = request.loopEnabled,
                    loopStartFrame = request.loopStartFrame,
                    loopEndFrame = request.loopEndFrame,
                )
                listener.onPosition(lastTimelineFrame)
            }

            if (running && !request.loopEnabled) {
                val deadline = System.nanoTime() + DRAIN_TIMEOUT_NS
                while (running && (track.playbackHeadPosition.toLong() and 0xffffffffL) < totalWrittenFrames && System.nanoTime() < deadline) {
                    Thread.sleep(4)
                    val presentedFrames = track.playbackHeadPosition.toLong() and 0xffffffffL
                    lastTimelineFrame = PlaybackClockPolicy.timelineFrame(
                        normalizedStart(request), presentedFrames, request.projectEndFrame, false, 0, request.projectEndFrame
                    )
                    listener.onPosition(lastTimelineFrame)
                }
                lastTimelineFrame = request.projectEndFrame
                listener.onPosition(lastTimelineFrame)
            }
        } catch (_: InterruptedException) {
            // Expected when Stop interrupts the playback worker.
        } catch (error: Throwable) {
            listener.onError(error.message ?: "Studio playback failed.")
        } finally {
            running = false
            runCatching { track?.pause() }
            runCatching { track?.flush() }
            runCatching { track?.release() }
            readers.forEach { runCatching { it.close() } }
            listener.onStopped(lastTimelineFrame)
            synchronized(this) { worker = null }
        }
    }

    private fun normalizedStart(request: StudioPlaybackRequest): Long =
        if (request.loopEnabled && request.startFrame >= request.loopEndFrame) request.loopStartFrame else request.startFrame

    private class ClipReader(
        private val clip: StudioPlaybackClip,
        expectedSampleRateHz: Int,
    ) : AutoCloseable {
        private val source = FileSeekableByteSource(clip.file)
        private val decoder = WavPcmDecoder(source)
        private val channels = decoder.metadata.channelCount
        private val gain = 10.0.pow(clip.gainDb.toDouble() / 20.0).toFloat()

        init {
            require(decoder.metadata.sampleRateHz == expectedSampleRateHz) {
                "Playback requires source/project sample-rate parity until the resampler gate is implemented."
            }
            require(channels in 1..2) { "Studio playback currently supports mono/stereo WAV sources." }
        }

        fun mixInto(renderStartFrame: Long, frameCount: Int, destinationStereo: FloatArray) {
            val renderEndFrame = renderStartFrame + frameCount
            val clipEndFrame = clip.timelineStartFrame + clip.lengthFrames
            val overlapStart = max(renderStartFrame, clip.timelineStartFrame)
            val overlapEnd = min(renderEndFrame, clipEndFrame)
            if (overlapStart >= overlapEnd) return

            val requested = (overlapEnd - overlapStart).toInt()
            val sourceFrame = clip.sourceStartFrame + (overlapStart - clip.timelineStartFrame)
            decoder.seekToFrame(sourceFrame)
            val temp = FloatArray(requested * channels)
            val decoded = decoder.readInterleaved(temp, frameCount = requested)
            val destinationFrameOffset = (overlapStart - renderStartFrame).toInt()
            for (frame in 0 until decoded) {
                val dst = (destinationFrameOffset + frame) * 2
                if (channels == 1) {
                    val sample = temp[frame] * gain
                    destinationStereo[dst] += sample
                    destinationStereo[dst + 1] += sample
                } else {
                    val src = frame * 2
                    destinationStereo[dst] += temp[src] * gain
                    destinationStereo[dst + 1] += temp[src + 1] * gain
                }
            }
        }

        override fun close() {
            decoder.close()
            source.close()
        }
    }

    private companion object {
        const val CHUNK_FRAMES = 1024
        const val DRAIN_TIMEOUT_NS = 2_000_000_000L
    }
}
