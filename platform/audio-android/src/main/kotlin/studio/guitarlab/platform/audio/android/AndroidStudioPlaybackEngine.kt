package studio.guitarlab.platform.audio.android

import android.media.AudioAttributes
import android.media.AudioDeviceInfo
import android.media.AudioFormat
import android.media.AudioTrack
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt
import studio.guitarlab.core.audio.PlaybackClockPolicy
import studio.guitarlab.core.project.GuitarAuditionMode
import studio.guitarlab.core.project.GuitarAuditionPolicy

data class StudioPlaybackClip(
    val file: File,
    val trackId: String,
    val timelineStartFrame: Long,
    val sourceStartFrame: Long,
    val lengthFrames: Long,
    val gainDb: Float = 0f,
    val pan: Float = 0f,
    val muted: Boolean = false,
    val fadeInFrames: Long = 0,
    val fadeOutFrames: Long = 0,
)

data class StudioPlaybackTrackMix(
    val trackId: String,
    val roleId: String? = null,
    val gainDb: Float = 0f,
    val pan: Float = 0f,
    val muted: Boolean = false,
    val solo: Boolean = false,
)

data class StudioPlaybackRequest(
    val sampleRateHz: Int,
    val startFrame: Long,
    val projectEndFrame: Long,
    val loopEnabled: Boolean,
    val loopStartFrame: Long,
    val loopEndFrame: Long,
    val clips: List<StudioPlaybackClip>,
    val trackMixes: List<StudioPlaybackTrackMix> = emptyList(),
    val preferredOutputDevice: AudioDeviceInfo? = null,
    val preferredOutputRequested: Boolean = false,
    val masterGainDb: Float = 0f,
    val auditionMode: GuitarAuditionMode = GuitarAuditionMode.MIXER,
    val repeatLoop: Boolean = true,
)

data class StudioPlaybackRoutingStatus(
    val requestedPreferredOutput: Boolean,
    val usingPreferredOutput: Boolean,
    val fellBackToAuto: Boolean,
    val deviceLabel: String? = null,
)

data class StudioPlaybackMeter(
    val peak: Float,
    val rms: Float,
)

data class StudioPlaybackTrackMeter(
    val trackId: String,
    val meter: StudioPlaybackMeter,
)

interface StudioPlaybackListener {
    fun onPosition(frame: Long)
    fun onStopped(frame: Long)
    fun onError(message: String)
    fun onRouting(status: StudioPlaybackRoutingStatus) {}
    fun onMasterMeter(meter: StudioPlaybackMeter) {}
    fun onTrackMeters(meters: List<StudioPlaybackTrackMeter>) {}
}

class AndroidStudioPlaybackEngine : AutoCloseable {
    @Volatile private var running = false
    @Volatile private var worker: Thread? = null
    @Volatile private var pendingSeekFrame: Long? = null
    @Volatile private var runtimeMasterGainDb: Float = 0f
    @Volatile private var runtimeAuditionMode: GuitarAuditionMode = GuitarAuditionMode.MIXER
    private val runtimeTrackMixes = ConcurrentHashMap<String, StudioPlaybackTrackMix>()

    @Synchronized
    fun start(request: StudioPlaybackRequest, listener: StudioPlaybackListener) {
        check(!running && worker?.isAlive != true) { "A reprodução anterior ainda está encerrando." }
        require(request.sampleRateHz > 0) { "A taxa de amostragem deve ser positiva." }
        require(request.projectEndFrame > 0) { "O projeto precisa conter áudio reproduzível." }
        require(request.startFrame in 0..request.projectEndFrame) { "O início da reprodução está fora da linha do tempo." }
        require(request.masterGainDb in -60f..12f) { "O ganho Master deve ficar entre -60 dB e +12 dB." }
        request.trackMixes.forEach {
            require(it.gainDb in -60f..12f) { "O ganho da pista deve ficar entre -60 dB e +12 dB." }
            require(it.pan in -1f..1f) { "O pan da pista deve ficar entre -1 e +1." }
        }
        if (request.loopEnabled) {
            require(request.loopStartFrame >= 0 && request.loopEndFrame > request.loopStartFrame) { "A região de loop é inválida." }
            require(request.loopEndFrame <= request.projectEndFrame) { "A região de loop ultrapassa o fim do projeto." }
        }

        runtimeTrackMixes.clear()
        request.trackMixes.forEach { runtimeTrackMixes[it.trackId] = it }
        runtimeMasterGainDb = request.masterGainDb
        runtimeAuditionMode = request.auditionMode
        pendingSeekFrame = null
        running = true
        worker = Thread({ runPlayback(request, listener) }, "GuitarLab-StudioPlayback").also { it.start() }
    }

    /**
     * Requests a low-latency seek without tearing down the playback session. Multiple drag events
     * may coalesce; the audio thread always consumes the most recent target at the next render
     * boundary. The ViewModel remains responsible for validating loop/recording policy.
     */
    fun seekTo(frame: Long) {
        if (!running) return
        pendingSeekFrame = frame.coerceAtLeast(0L)
    }

    fun setTrackMix(trackId: String, gainDb: Float, pan: Float) {
        val current = runtimeTrackMixes[trackId] ?: StudioPlaybackTrackMix(trackId)
        runtimeTrackMixes[trackId] = current.copy(
            gainDb = gainDb.coerceIn(-60f, 12f),
            pan = pan.coerceIn(-1f, 1f),
        )
    }

    fun setTrackAudibility(trackId: String, muted: Boolean, solo: Boolean) {
        val current = runtimeTrackMixes[trackId] ?: StudioPlaybackTrackMix(trackId)
        runtimeTrackMixes[trackId] = current.copy(muted = muted, solo = solo)
    }

    fun setMasterGainDb(gainDb: Float) {
        runtimeMasterGainDb = gainDb.coerceIn(-60f, 12f)
    }

    fun setAuditionMode(mode: GuitarAuditionMode) { runtimeAuditionMode = mode }

    fun stop() {
        val thread = synchronized(this) {
            running = false
            pendingSeekFrame = null
            worker
        }
        thread?.interrupt()
        if (thread != null && thread !== Thread.currentThread()) runCatching { thread.join(STOP_JOIN_TIMEOUT_MS) }
        synchronized(this) { if (worker === thread && thread?.isAlive != true) worker = null }
    }

    override fun close() {
        stop()
        runtimeTrackMixes.clear()
    }

    private fun runPlayback(request: StudioPlaybackRequest, listener: StudioPlaybackListener) {
        var lastTimelineFrame = request.startFrame
        val readers = mutableListOf<StudioPcmClipReader>()
        var track: AudioTrack? = null
        try {
            request.clips.filterNot { it.muted }.forEach { clip -> readers += StudioPcmClipReader(StudioPcmClip(
                clip.file, clip.trackId, clip.timelineStartFrame, clip.sourceStartFrame, clip.lengthFrames,
                clip.gainDb, clip.pan, clip.fadeInFrames, clip.fadeOutFrames,
            ), request.sampleRateHz) }
            require(readers.isNotEmpty()) { "Não há clipes WAV gerenciados disponíveis para reprodução." }

            val channelMask = AudioFormat.CHANNEL_OUT_STEREO
            val minBytes = AudioTrack.getMinBufferSize(request.sampleRateHz, channelMask, AudioFormat.ENCODING_PCM_FLOAT)
            require(minBytes > 0) { "O Android não conseguiu reservar o buffer de reprodução para ${request.sampleRateHz} Hz." }
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
            require(track.state == AudioTrack.STATE_INITIALIZED) { "A saída de áudio do Android não foi inicializada." }

            applyOutputRouting(track, request, listener)

            val repeatLoop = request.loopEnabled && request.repeatLoop
            val playbackBoundary = if (request.loopEnabled) request.loopEndFrame else request.projectEndFrame
            var renderFrame = normalizedStart(request)
            var clockStartFrame = renderFrame
            var clockHeadBase = playbackHead(track)
            var writtenFramesSinceClockBase = 0L
            val mix = FloatArray(CHUNK_FRAMES * 2)
            val trackBuffers = linkedMapOf<String, FloatArray>()
            readers.forEach { reader -> trackBuffers.getOrPut(reader.trackId) { FloatArray(CHUNK_FRAMES * 2) } }
            track.play()
            clockHeadBase = playbackHead(track)

            while (running) {
                val requestedSeek = pendingSeekFrame
                if (requestedSeek != null) {
                    pendingSeekFrame = null
                    renderFrame = normalizedSeek(request, requestedSeek)
                    lastTimelineFrame = renderFrame
                    track.pause()
                    track.flush()
                    track.play()
                    clockStartFrame = renderFrame
                    clockHeadBase = playbackHead(track)
                    writtenFramesSinceClockBase = 0L
                    listener.onPosition(renderFrame)
                }

                if (renderFrame >= playbackBoundary) {
                    if (repeatLoop) {
                        renderFrame = request.loopStartFrame
                        continue
                    }
                    break
                }

                val framesToRender = min(CHUNK_FRAMES.toLong(), playbackBoundary - renderFrame).toInt()
                val sampleCount = framesToRender * 2
                java.util.Arrays.fill(mix, 0, sampleCount, 0f)
                trackBuffers.values.forEach { java.util.Arrays.fill(it, 0, sampleCount, 0f) }
                readers.forEach { reader -> reader.mixInto(renderFrame, framesToRender, trackBuffers.getValue(reader.trackId)) }

                val trackMeters = ArrayList<StudioPlaybackTrackMeter>(trackBuffers.size)
                trackBuffers.forEach { (trackId, buffer) ->
                    applyRuntimeTrackMix(trackId, buffer, sampleCount)
                    trackMeters += StudioPlaybackTrackMeter(trackId, meterFor(buffer, sampleCount))
                    for (index in 0 until sampleCount) mix[index] += buffer[index]
                }
                listener.onTrackMeters(trackMeters)

                var peak = 0f
                var sumSquares = 0.0
                val masterLinear = 10.0.pow(runtimeMasterGainDb / 20.0).toFloat()
                for (index in 0 until sampleCount) {
                    val mastered = mix[index] * masterLinear
                    peak = max(peak, abs(mastered))
                    sumSquares += mastered.toDouble() * mastered.toDouble()
                }
                val rms = if (sampleCount > 0) sqrt(sumSquares / sampleCount).toFloat() else 0f
                listener.onMasterMeter(StudioPlaybackMeter(peak = peak, rms = rms))
                StudioPcmMixKernel.applyMaster(mix, sampleCount, runtimeMasterGainDb)

                val samplesWritten = track.write(mix, 0, sampleCount, AudioTrack.WRITE_BLOCKING)
                if (samplesWritten < 0) error("Falha ao enviar áudio para a saída Android: código $samplesWritten.")
                val writtenFrames = samplesWritten / 2
                if (writtenFrames <= 0) error("A saída de áudio não avançou durante a reprodução.")
                writtenFramesSinceClockBase += writtenFrames
                renderFrame += writtenFrames
                if (repeatLoop && renderFrame >= request.loopEndFrame) renderFrame = request.loopStartFrame

                val presentedFrames = playbackHeadDelta(playbackHead(track), clockHeadBase)
                lastTimelineFrame = PlaybackClockPolicy.timelineFrame(
                    startFrame = clockStartFrame,
                    presentedFrames = presentedFrames,
                    projectEndFrame = if (repeatLoop) request.projectEndFrame else playbackBoundary,
                    loopEnabled = repeatLoop,
                    loopStartFrame = request.loopStartFrame,
                    loopEndFrame = request.loopEndFrame,
                )
                listener.onPosition(lastTimelineFrame)
            }

            if (running && !repeatLoop) {
                val deadline = System.nanoTime() + DRAIN_TIMEOUT_NS
                while (running && playbackHeadDelta(playbackHead(track), clockHeadBase) < writtenFramesSinceClockBase && System.nanoTime() < deadline) {
                    Thread.sleep(4)
                    val presentedFrames = playbackHeadDelta(playbackHead(track), clockHeadBase)
                    lastTimelineFrame = PlaybackClockPolicy.timelineFrame(
                        clockStartFrame,
                        presentedFrames,
                        playbackBoundary,
                        false,
                        0,
                        playbackBoundary,
                    )
                    listener.onPosition(lastTimelineFrame)
                }
                lastTimelineFrame = playbackBoundary
                listener.onPosition(lastTimelineFrame)
            }
        } catch (_: InterruptedException) {
        } catch (error: Throwable) {
            listener.onError(error.message ?: "A reprodução do Studio falhou.")
        } finally {
            running = false
            pendingSeekFrame = null
            runCatching { track?.pause() }
            runCatching { track?.flush() }
            runCatching { track?.release() }
            readers.forEach { runCatching { it.close() } }
            listener.onTrackMeters(emptyList())
            listener.onMasterMeter(StudioPlaybackMeter(peak = 0f, rms = 0f))
            listener.onStopped(lastTimelineFrame)
            synchronized(this) { worker = null }
        }
    }

    private fun applyRuntimeTrackMix(trackId: String, samples: FloatArray, sampleCount: Int) {
        val runtime = runtimeTrackMixes[trackId] ?: return
        val anySolo = runtimeTrackMixes.values.any { it.solo }
        if (runtime.muted || (anySolo && !runtime.solo) || !GuitarAuditionPolicy.roleAudible(runtime.roleId, runtimeAuditionMode)) {
            java.util.Arrays.fill(samples, 0, sampleCount, 0f)
            return
        }
        StudioPcmMixKernel.applyTrack(samples, sampleCount, runtime.gainDb, runtime.pan, audible = true)
    }

    private fun meterFor(samples: FloatArray, sampleCount: Int): StudioPlaybackMeter {
        var peak = 0f
        var sumSquares = 0.0
        for (index in 0 until sampleCount) {
            val sample = samples[index]
            peak = max(peak, abs(sample))
            sumSquares += sample.toDouble() * sample.toDouble()
        }
        val rms = if (sampleCount > 0) sqrt(sumSquares / sampleCount).toFloat() else 0f
        return StudioPlaybackMeter(peak = peak, rms = rms)
    }

    private fun applyOutputRouting(
        track: AudioTrack,
        request: StudioPlaybackRequest,
        listener: StudioPlaybackListener,
    ) {
        if (!request.preferredOutputRequested) {
            listener.onRouting(
                StudioPlaybackRoutingStatus(
                    requestedPreferredOutput = false,
                    usingPreferredOutput = false,
                    fellBackToAuto = false,
                )
            )
            return
        }

        val preferred = request.preferredOutputDevice
        if (preferred == null) {
            listener.onRouting(
                StudioPlaybackRoutingStatus(
                    requestedPreferredOutput = true,
                    usingPreferredOutput = false,
                    fellBackToAuto = true,
                )
            )
            return
        }

        val accepted = runCatching { track.setPreferredDevice(preferred) }.getOrDefault(false)
        listener.onRouting(
            StudioPlaybackRoutingStatus(
                requestedPreferredOutput = true,
                usingPreferredOutput = accepted,
                fellBackToAuto = !accepted,
                deviceLabel = preferred.productName?.toString(),
            )
        )
    }

    private fun normalizedStart(request: StudioPlaybackRequest): Long =
        if (request.loopEnabled && request.startFrame >= request.loopEndFrame) request.loopStartFrame else request.startFrame

    private fun normalizedSeek(request: StudioPlaybackRequest, requestedFrame: Long): Long {
        val target = requestedFrame.coerceIn(0L, request.projectEndFrame)
        if (!request.loopEnabled) return target
        val lastPlayableFrame = (request.loopEndFrame - 1L).coerceAtLeast(request.loopStartFrame)
        return target.coerceIn(request.loopStartFrame, lastPlayableFrame)
    }

    private fun playbackHead(track: AudioTrack): Long = track.playbackHeadPosition.toLong() and 0xffffffffL

    private fun playbackHeadDelta(current: Long, base: Long): Long = (current - base) and 0xffffffffL

    private companion object {
        const val CHUNK_FRAMES = 1024
        const val DRAIN_TIMEOUT_NS = 2_000_000_000L
        const val STOP_JOIN_TIMEOUT_MS = 1_500L
    }
}
