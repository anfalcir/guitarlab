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
import studio.guitarlab.core.audio.TrackMixPolicy
import studio.guitarlab.core.codec.FileSeekableByteSource
import studio.guitarlab.core.codec.WavPcmDecoder

data class StudioPlaybackClip(
    val file: File,
    val trackId: String,
    val timelineStartFrame: Long,
    val sourceStartFrame: Long,
    val lengthFrames: Long,
    val gainDb: Float = 0f,
    val pan: Float = 0f,
    val muted: Boolean = false,
)

data class StudioPlaybackTrackMix(
    val trackId: String,
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
    @Volatile private var runtimeMasterGainDb: Float = 0f
    private val runtimeTrackMixes = ConcurrentHashMap<String, StudioPlaybackTrackMix>()

    @Synchronized
    fun start(request: StudioPlaybackRequest, listener: StudioPlaybackListener) {
        check(!running) { "A reprodução já está em execução." }
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
        running = true
        worker = Thread({ runPlayback(request, listener) }, "GuitarLab-StudioPlayback").also { it.start() }
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

    @Synchronized
    fun stop() {
        running = false
        worker?.interrupt()
    }

    override fun close() {
        stop()
        runtimeTrackMixes.clear()
    }

    private fun runPlayback(request: StudioPlaybackRequest, listener: StudioPlaybackListener) {
        var lastTimelineFrame = request.startFrame
        val readers = mutableListOf<ClipReader>()
        var track: AudioTrack? = null
        try {
            request.clips.filterNot { it.muted }.forEach { readers += ClipReader(it, request.sampleRateHz) }
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

            var renderFrame = normalizedStart(request)
            var totalWrittenFrames = 0L
            val mix = FloatArray(CHUNK_FRAMES * 2)
            val trackBuffers = linkedMapOf<String, FloatArray>()
            readers.forEach { reader -> trackBuffers.getOrPut(reader.trackId) { FloatArray(CHUNK_FRAMES * 2) } }
            track.play()

            while (running) {
                if (!request.loopEnabled && renderFrame >= request.projectEndFrame) break
                val boundary = if (request.loopEnabled) request.loopEndFrame else request.projectEndFrame
                if (renderFrame >= boundary) {
                    renderFrame = request.loopStartFrame
                    continue
                }

                val framesToRender = min(CHUNK_FRAMES.toLong(), boundary - renderFrame).toInt()
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
                    mix[index] = mastered.coerceIn(-1f, 1f)
                }
                val rms = if (sampleCount > 0) sqrt(sumSquares / sampleCount).toFloat() else 0f
                listener.onMasterMeter(StudioPlaybackMeter(peak = peak, rms = rms))

                val samplesWritten = track.write(mix, 0, sampleCount, AudioTrack.WRITE_BLOCKING)
                if (samplesWritten < 0) error("Falha ao enviar áudio para a saída Android: código $samplesWritten.")
                val writtenFrames = samplesWritten / 2
                if (writtenFrames <= 0) error("A saída de áudio não avançou durante a reprodução.")
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
        } catch (error: Throwable) {
            listener.onError(error.message ?: "A reprodução do Studio falhou.")
        } finally {
            running = false
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
        if (runtime.muted || (anySolo && !runtime.solo)) {
            java.util.Arrays.fill(samples, 0, sampleCount, 0f)
            return
        }
        val gain = TrackMixPolicy.channelGains(runtime.gainDb, runtime.pan)
        var index = 0
        while (index + 1 < sampleCount) {
            samples[index] *= gain.left
            samples[index + 1] *= gain.right
            index += 2
        }
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

    private class ClipReader(
        private val clip: StudioPlaybackClip,
        expectedSampleRateHz: Int,
    ) : AutoCloseable {
        val trackId: String = clip.trackId
        private val source = FileSeekableByteSource(clip.file)
        private val decoder = WavPcmDecoder(source)
        private val channels = decoder.metadata.channelCount
        private val stereoGain = TrackMixPolicy.channelGains(clip.gainDb, clip.pan)

        init {
            require(decoder.metadata.sampleRateHz == expectedSampleRateHz) {
                "A reprodução exige que a fonte e o projeto tenham a mesma taxa de amostragem enquanto o resampler não estiver ativo."
            }
            require(channels in 1..2) { "A reprodução atual suporta fontes WAV mono ou estéreo." }
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
                    val sample = temp[frame]
                    destinationStereo[dst] += sample * stereoGain.left
                    destinationStereo[dst + 1] += sample * stereoGain.right
                } else {
                    val src = frame * 2
                    destinationStereo[dst] += temp[src] * stereoGain.left
                    destinationStereo[dst + 1] += temp[src + 1] * stereoGain.right
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
