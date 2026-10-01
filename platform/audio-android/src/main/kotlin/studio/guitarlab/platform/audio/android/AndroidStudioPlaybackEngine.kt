package studio.guitarlab.platform.audio.android

import android.media.AudioAttributes
import android.media.AudioDeviceInfo
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.AudioTimestamp
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt
import studio.guitarlab.core.audio.AudioClockAnchor
import studio.guitarlab.core.audio.AudioClockAnchorPolicy
import studio.guitarlab.core.audio.AudioClockObservation
import studio.guitarlab.core.audio.CueRouteBlockReason
import studio.guitarlab.core.audio.CueRouteSafetyPolicy
import studio.guitarlab.core.audio.PlaybackClockPolicy
import studio.guitarlab.core.model.TrackOutputRoute
import studio.guitarlab.core.model.TrackOutputRoutingPolicy
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
    val outputRoute: TrackOutputRoute = TrackOutputRoute.MAIN,
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
    val preferredCueOutputDevice: AudioDeviceInfo? = null,
    val preferredCueOutputRequested: Boolean = false,
    val masterGainDb: Float = 0f,
    val auditionMode: GuitarAuditionMode = GuitarAuditionMode.MIXER,
    val repeatLoop: Boolean = true,
)

data class StudioPlaybackRoutingStatus(
    val requestedPreferredOutput: Boolean,
    val usingPreferredOutput: Boolean,
    val fellBackToAuto: Boolean,
    val deviceLabel: String? = null,
    val requestedPreferredCueOutput: Boolean = false,
    val usingPreferredCueOutput: Boolean = false,
    val cueSuppressed: Boolean = false,
    val cueDeviceLabel: String? = null,
    val cueFailureReason: String? = null,
)

data class StudioPlaybackStartInfo(
    val presentationStartMonotonicNs: Long,
    val timestampBased: Boolean,
    val routedOutputLabel: String? = null,
    val routedOutputDeviceId: Int? = null,
    val cueRoutedOutputLabel: String? = null,
    val cueRoutedOutputDeviceId: Int? = null,
    val playbackAnchorJitterNs: Long? = null,
    val playbackAnchorObservations: Int? = null,
    val outputUnderrunCount: Int? = null,
    val cueOutputUnderrunCount: Int? = null,
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
    fun onStarted(info: StudioPlaybackStartInfo) {}
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
    private val pendingSeekFrame = AtomicLong(NO_PENDING_SEEK)
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
        pendingSeekFrame.set(NO_PENDING_SEEK)
        running = true
        worker = Thread({ runPlayback(request, listener) }, "GuitarLab-StudioPlayback").also { it.start() }
    }

    /**
     * Requests a low-latency seek without tearing down the playback session. Multiple drag events
     * intentionally coalesce to the most recent frame at the next audio render boundary.
     */
    fun seekTo(frame: Long) {
        if (!running) return
        pendingSeekFrame.set(frame.coerceAtLeast(0L))
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

    fun setTrackOutputRoute(trackId: String, outputRoute: TrackOutputRoute) {
        val current = runtimeTrackMixes[trackId] ?: StudioPlaybackTrackMix(trackId)
        runtimeTrackMixes[trackId] = current.copy(outputRoute = outputRoute)
    }

    fun setMasterGainDb(gainDb: Float) {
        runtimeMasterGainDb = gainDb.coerceIn(-60f, 12f)
    }

    fun setAuditionMode(mode: GuitarAuditionMode) { runtimeAuditionMode = mode }

    fun stop() {
        val thread = synchronized(this) {
            running = false
            pendingSeekFrame.set(NO_PENDING_SEEK)
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
        var cueTrack: AudioTrack? = null
        try {
            request.clips.filterNot { it.muted }.forEach { clip ->
                readers += StudioPcmClipReader(
                    StudioPcmClip(
                        clip.file,
                        clip.trackId,
                        clip.timelineStartFrame,
                        clip.sourceStartFrame,
                        clip.lengthFrames,
                        clip.gainDb,
                        clip.pan,
                        clip.fadeInFrames,
                        clip.fadeOutFrames,
                    ),
                    request.sampleRateHz,
                )
            }
            require(readers.isNotEmpty()) { "Não há clipes WAV gerenciados disponíveis para reprodução." }

            val channelMask = AudioFormat.CHANNEL_OUT_STEREO
            val minBytes = AudioTrack.getMinBufferSize(request.sampleRateHz, channelMask, AudioFormat.ENCODING_PCM_FLOAT)
            require(minBytes > 0) { "O Android não conseguiu reservar o buffer de reprodução para ${request.sampleRateHz} Hz." }
            val bufferBytes = max(minBytes, CHUNK_FRAMES * 2 * Float.SIZE_BYTES * 4)
            val audioTrack = AudioTrack.Builder()
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
            track = audioTrack
            require(audioTrack.state == AudioTrack.STATE_INITIALIZED) { "A saída de áudio do Android não foi inicializada." }

            val mainPreferredAccepted = applyOutputRouting(audioTrack, request, listener)
            cueTrack = prepareCueOutput(
                mainTrack = audioTrack,
                request = request,
                bufferBytes = bufferBytes,
                mainPreferredAccepted = mainPreferredAccepted,
                listener = listener,
            )

            val repeatLoop = request.loopEnabled && request.repeatLoop
            val playbackBoundary = if (request.loopEnabled) request.loopEndFrame else request.projectEndFrame
            var renderFrame = normalizedStart(request)
            var clockStartFrame = renderFrame
            var clockHeadBase = playbackHead(audioTrack)
            var writtenFramesSinceClockBase = 0L
            var playbackClockAnchor: AudioClockAnchor? = null
            var clockAnchorAttempted = false
            val mainMix = FloatArray(CHUNK_FRAMES * 2)
            val cueMix = FloatArray(CHUNK_FRAMES * 2)
            val trackBuffers = linkedMapOf<String, FloatArray>()
            readers.forEach { reader -> trackBuffers.getOrPut(reader.trackId) { FloatArray(CHUNK_FRAMES * 2) } }
            val playCommandNs = System.nanoTime()
            audioTrack.play()
            cueTrack?.play()
            clockHeadBase = playbackHead(audioTrack)
            var cueClockHeadBase = cueTrack?.let(::playbackHead) ?: 0L
            var cueDriftViolationCount = 0
            var playbackStartReported = false

            while (running) {
                val requestedSeek = pendingSeekFrame.getAndSet(NO_PENDING_SEEK)
                if (requestedSeek != NO_PENDING_SEEK) {
                    renderFrame = normalizedSeek(request, requestedSeek)
                    lastTimelineFrame = renderFrame
                    audioTrack.pause()
                    audioTrack.flush()
                    cueTrack?.pause()
                    cueTrack?.flush()
                    audioTrack.play()
                    cueTrack?.play()
                    clockStartFrame = renderFrame
                    clockHeadBase = playbackHead(audioTrack)
                    cueClockHeadBase = cueTrack?.let(::playbackHead) ?: 0L
                    cueDriftViolationCount = 0
                    writtenFramesSinceClockBase = 0L
                    playbackClockAnchor = null
                    clockAnchorAttempted = false
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
                java.util.Arrays.fill(mainMix, 0, sampleCount, 0f)
                java.util.Arrays.fill(cueMix, 0, sampleCount, 0f)
                trackBuffers.values.forEach { java.util.Arrays.fill(it, 0, sampleCount, 0f) }
                readers.forEach { reader -> reader.mixInto(renderFrame, framesToRender, trackBuffers.getValue(reader.trackId)) }

                val trackMeters = ArrayList<StudioPlaybackTrackMeter>(trackBuffers.size)
                trackBuffers.forEach { (trackId, buffer) ->
                    applyRuntimeTrackMix(trackId, buffer, sampleCount)
                    trackMeters += StudioPlaybackTrackMeter(trackId, meterFor(buffer, sampleCount))
                    val route = runtimeTrackMixes[trackId]?.outputRoute ?: TrackOutputRoute.MAIN
                    if (TrackOutputRoutingPolicy.sendsToMain(route)) {
                        for (index in 0 until sampleCount) mainMix[index] += buffer[index]
                    }
                    if (TrackOutputRoutingPolicy.sendsToCue(route) && cueTrack != null) {
                        for (index in 0 until sampleCount) cueMix[index] += buffer[index]
                    }
                }
                listener.onTrackMeters(trackMeters)

                var peak = 0f
                var sumSquares = 0.0
                val masterLinear = 10.0.pow(runtimeMasterGainDb / 20.0).toFloat()
                for (index in 0 until sampleCount) {
                    val mastered = mainMix[index] * masterLinear
                    peak = max(peak, abs(mastered))
                    sumSquares += mastered.toDouble() * mastered.toDouble()
                }
                val rms = if (sampleCount > 0) sqrt(sumSquares / sampleCount).toFloat() else 0f
                listener.onMasterMeter(StudioPlaybackMeter(peak = peak, rms = rms))
                StudioPcmMixKernel.applyMaster(mainMix, sampleCount, runtimeMasterGainDb)
                StudioPcmMixKernel.applyMaster(cueMix, sampleCount, runtimeMasterGainDb)

                val samplesWritten = audioTrack.write(mainMix, 0, sampleCount, AudioTrack.WRITE_BLOCKING)
                if (samplesWritten < 0) error("Falha ao enviar áudio para a saída Android: código $samplesWritten.")
                val writtenFrames = samplesWritten / 2
                if (writtenFrames <= 0) error("A saída de áudio não avançou durante a reprodução.")
                writtenFramesSinceClockBase += writtenFrames

                cueTrack?.let { activeCue ->
                    // CUE is deliberately non-blocking: a slow/blocked secondary sink may be
                    // silenced, but it may never stall the render loop feeding MAIN.
                    val cueWrite = runCatching {
                        activeCue.write(cueMix, 0, sampleCount, AudioTrack.WRITE_NON_BLOCKING)
                    }.getOrDefault(AudioTrack.ERROR_INVALID_OPERATION)
                    val writeComplete = CueRouteSafetyPolicy.secondaryWriteComplete(sampleCount, cueWrite)
                    val routeSafe = cueRouteStillSafe(audioTrack, activeCue, request)
                    val mainPresented = playbackHeadDelta(playbackHead(audioTrack), clockHeadBase)
                    val cuePresented = playbackHeadDelta(playbackHead(activeCue), cueClockHeadBase)
                    val driftUnsafe = CueRouteSafetyPolicy.driftExceeded(
                        mainPresentedFrames = mainPresented,
                        cuePresentedFrames = cuePresented,
                        sampleRateHz = request.sampleRateHz,
                    )
                    cueDriftViolationCount = if (driftUnsafe) cueDriftViolationCount + 1 else 0
                    if (!writeComplete || !routeSafe || cueDriftViolationCount >= CUE_DRIFT_CONSECUTIVE_LIMIT) {
                        val reason = when {
                            !writeComplete ->
                                "A saída CUE não acompanhou o fluxo em tempo real; foi silenciada para não bloquear a saída principal."
                            !routeSafe ->
                                "A rota CUE mudou, convergiu para a saída principal ou deixou de ser confirmada."
                            else ->
                                "A saída CUE excedeu o limite de deriva segura em relação à saída principal."
                        }
                        runCatching { activeCue.pause() }
                        runCatching { activeCue.flush() }
                        runCatching { activeCue.release() }
                        cueTrack = null
                        listener.onRouting(
                            StudioPlaybackRoutingStatus(
                                requestedPreferredOutput = request.preferredOutputRequested,
                                usingPreferredOutput = request.preferredOutputRequested && request.preferredOutputDevice != null,
                                fellBackToAuto = false,
                                deviceLabel = request.preferredOutputDevice?.productName?.toString(),
                                requestedPreferredCueOutput = true,
                                usingPreferredCueOutput = false,
                                cueSuppressed = true,
                                cueDeviceLabel = request.preferredCueOutputDevice?.productName?.toString(),
                                cueFailureReason = reason,
                            )
                        )
                    }
                }

                if (!clockAnchorAttempted) {
                    playbackClockAnchor = stablePlaybackClockAnchor(audioTrack, request.sampleRateHz)
                    clockAnchorAttempted = true
                }
                if (!playbackStartReported) {
                    val hasTimestamp = playbackClockAnchor != null
                    val presentationStartNs = playbackClockAnchor?.streamOriginMonotonicNs ?: playCommandNs
                    val routed = audioTrack.routedDevice
                    val cueRouted = cueTrack?.routedDevice
                    listener.onStarted(
                        StudioPlaybackStartInfo(
                            presentationStartMonotonicNs = presentationStartNs,
                            timestampBased = hasTimestamp,
                            routedOutputLabel = routed?.productName?.toString(),
                            routedOutputDeviceId = routed?.id,
                            cueRoutedOutputLabel = cueRouted?.productName?.toString(),
                            cueRoutedOutputDeviceId = cueRouted?.id,
                            playbackAnchorJitterNs = playbackClockAnchor?.jitterNs,
                            playbackAnchorObservations = playbackClockAnchor?.observations,
                            outputUnderrunCount = audioTrack.underrunCount,
                            cueOutputUnderrunCount = cueTrack?.underrunCount,
                        )
                    )
                    playbackStartReported = true
                }
                renderFrame += writtenFrames
                if (repeatLoop && renderFrame >= request.loopEndFrame) renderFrame = request.loopStartFrame

                val presentedFrames = presentedFrames(
                    track = audioTrack,
                    clockHeadBase = clockHeadBase,
                    playbackClockAnchor = playbackClockAnchor,
                    sampleRateHz = request.sampleRateHz,
                    writtenFramesSinceClockBase = writtenFramesSinceClockBase,
                )
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
                while (
                    running &&
                    presentedFrames(
                        track = audioTrack,
                        clockHeadBase = clockHeadBase,
                        playbackClockAnchor = playbackClockAnchor,
                        sampleRateHz = request.sampleRateHz,
                        writtenFramesSinceClockBase = writtenFramesSinceClockBase,
                    ) < writtenFramesSinceClockBase &&
                    System.nanoTime() < deadline
                ) {
                    Thread.sleep(4)
                    val presentedFrames = presentedFrames(
                        track = audioTrack,
                        clockHeadBase = clockHeadBase,
                        playbackClockAnchor = playbackClockAnchor,
                        sampleRateHz = request.sampleRateHz,
                        writtenFramesSinceClockBase = writtenFramesSinceClockBase,
                    )
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
            pendingSeekFrame.set(NO_PENDING_SEEK)
            runCatching { cueTrack?.pause() }
            runCatching { cueTrack?.flush() }
            runCatching { cueTrack?.release() }
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

    private fun stablePlaybackClockAnchor(
        track: AudioTrack,
        sampleRateHz: Int,
        maxJitterNs: Long = AudioClockAnchorPolicy.DEFAULT_MAX_JITTER_NS,
    ) =
        AudioClockAnchorPolicy.estimate(
            observations = buildList {
                repeat(CLOCK_ANCHOR_SAMPLES) { index ->
                    val timestamp = AudioTimestamp()
                    if (track.getTimestamp(timestamp)) {
                        add(AudioClockObservation(timestamp.framePosition, timestamp.nanoTime))
                    }
                    if (index + 1 < CLOCK_ANCHOR_SAMPLES) {
                        try { Thread.sleep(CLOCK_ANCHOR_POLL_MS) } catch (_: InterruptedException) { return@repeat }
                    }
                }
            },
            sampleRateHz = sampleRateHz,
            maxJitterNs = maxJitterNs,
        )

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
    ): Boolean {
        if (!request.preferredOutputRequested) {
            listener.onRouting(
                StudioPlaybackRoutingStatus(
                    requestedPreferredOutput = false,
                    usingPreferredOutput = false,
                    fellBackToAuto = false,
                )
            )
            return false
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
            return false
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
        return accepted
    }

    private fun prepareCueOutput(
        mainTrack: AudioTrack,
        request: StudioPlaybackRequest,
        bufferBytes: Int,
        mainPreferredAccepted: Boolean,
        listener: StudioPlaybackListener,
    ): AudioTrack? {
        if (!request.preferredCueOutputRequested) return null

        val main = request.preferredOutputDevice
        val cue = request.preferredCueOutputDevice
        val admission = CueRouteSafetyPolicy.admit(
            cueRequested = true,
            mainRequested = request.preferredOutputRequested,
            mainAccepted = mainPreferredAccepted,
            mainDeviceId = main?.id,
            cueDeviceId = cue?.id,
        )
        if (!admission.allowed) {
            val failure = when (admission.blockReason) {
                CueRouteBlockReason.MAIN_NOT_EXPLICIT ->
                    "CUE exige uma saída principal explícita para impedir convergência silenciosa de rotas."
                CueRouteBlockReason.MAIN_NOT_ACCEPTED ->
                    "A saída principal explícita não foi aceita pelo Android; CUE foi bloqueado."
                CueRouteBlockReason.CUE_UNAVAILABLE ->
                    "Nenhuma saída CUE explícita e disponível pôde ser resolvida."
                CueRouteBlockReason.SAME_ENDPOINT ->
                    "MAIN e CUE resolveram para o mesmo endpoint físico."
                CueRouteBlockReason.NOT_REQUESTED, null ->
                    "A saída CUE não foi solicitada."
            }
            reportCueSuppressed(request, listener, failure)
            return null
        }
        val explicitMain = requireNotNull(main)
        val explicitCue = requireNotNull(cue)

        val cueTrack = runCatching {
            AudioTrack.Builder()
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
                        .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                        .build()
                )
                .setTransferMode(AudioTrack.MODE_STREAM)
                .setBufferSizeInBytes(bufferBytes)
                .setPerformanceMode(AudioTrack.PERFORMANCE_MODE_LOW_LATENCY)
                .build()
        }.getOrNull()

        if (cueTrack == null || cueTrack.state != AudioTrack.STATE_INITIALIZED) {
            runCatching { cueTrack?.release() }
            reportCueSuppressed(request, listener, "O Android não conseguiu abrir a saída CUE.")
            return null
        }
        if (!runCatching { cueTrack.setPreferredDevice(explicitCue) }.getOrDefault(false)) {
            runCatching { cueTrack.release() }
            reportCueSuppressed(request, listener, "O Android recusou a rota CUE solicitada.")
            return null
        }

        val dualRouteFailure = primeAndVerifyDualRoutes(
            mainTrack = mainTrack,
            cueTrack = cueTrack,
            expectedMain = explicitMain,
            expectedCue = explicitCue,
            sampleRateHz = request.sampleRateHz,
        )
        if (dualRouteFailure != null) {
            runCatching { cueTrack.release() }
            reportCueSuppressed(request, listener, dualRouteFailure)
            return null
        }

        listener.onRouting(
            StudioPlaybackRoutingStatus(
                requestedPreferredOutput = true,
                usingPreferredOutput = true,
                fellBackToAuto = false,
                deviceLabel = explicitMain.productName?.toString(),
                requestedPreferredCueOutput = true,
                usingPreferredCueOutput = true,
                cueSuppressed = false,
                cueDeviceLabel = explicitCue.productName?.toString(),
            )
        )
        return cueTrack
    }

    private fun primeAndVerifyDualRoutes(
        mainTrack: AudioTrack, cueTrack: AudioTrack, expectedMain: AudioDeviceInfo,
        expectedCue: AudioDeviceInfo, sampleRateHz: Int,
    ): String? = AndroidCueRouteVerifier.verifyTracks(
        mainTrack, cueTrack, expectedMain, expectedCue, sampleRateHz,
        keepRunning = { running },
    )

    private fun cueRouteStillSafe(
        mainTrack: AudioTrack,
        cueTrack: AudioTrack,
        request: StudioPlaybackRequest,
    ): Boolean {
        val expectedMain = request.preferredOutputDevice ?: return false
        val expectedCue = request.preferredCueOutputDevice ?: return false
        val mainRouted = mainTrack.routedDevice ?: return false
        val cueRouted = cueTrack.routedDevice ?: return false
        return CueRouteSafetyPolicy.routedPairMatches(
            expectedMainDeviceId = expectedMain.id,
            expectedCueDeviceId = expectedCue.id,
            actualMainDeviceId = mainRouted.id,
            actualCueDeviceId = cueRouted.id,
        )
    }

    private fun reportCueSuppressed(
        request: StudioPlaybackRequest,
        listener: StudioPlaybackListener,
        reason: String,
    ) {
        listener.onRouting(
            StudioPlaybackRoutingStatus(
                requestedPreferredOutput = request.preferredOutputRequested,
                usingPreferredOutput = request.preferredOutputRequested && request.preferredOutputDevice != null,
                fellBackToAuto = request.preferredOutputRequested && request.preferredOutputDevice == null,
                deviceLabel = request.preferredOutputDevice?.productName?.toString(),
                requestedPreferredCueOutput = true,
                usingPreferredCueOutput = false,
                cueSuppressed = true,
                cueDeviceLabel = request.preferredCueOutputDevice?.productName?.toString(),
                cueFailureReason = reason,
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

    private fun presentedFrames(
        track: AudioTrack,
        clockHeadBase: Long,
        playbackClockAnchor: AudioClockAnchor?,
        sampleRateHz: Int,
        writtenFramesSinceClockBase: Long,
    ): Long = playbackClockAnchor?.let { anchor ->
        PlaybackClockPolicy.presentedFramesAt(
            nowMonotonicNs = System.nanoTime(),
            presentationOriginMonotonicNs = anchor.streamOriginMonotonicNs,
            sampleRateHz = sampleRateHz,
            maxWrittenFrames = writtenFramesSinceClockBase,
        )
    } ?: playbackHeadDelta(playbackHead(track), clockHeadBase).coerceAtMost(writtenFramesSinceClockBase)

    private fun playbackHead(track: AudioTrack): Long = track.playbackHeadPosition.toLong() and 0xffffffffL

    private fun playbackHeadDelta(current: Long, base: Long): Long = (current - base) and 0xffffffffL

    private companion object {
        const val CHUNK_FRAMES = 1024
        const val DRAIN_TIMEOUT_NS = 2_000_000_000L
        const val STOP_JOIN_TIMEOUT_MS = 1_500L
        const val NO_PENDING_SEEK = Long.MIN_VALUE
        const val CLOCK_ANCHOR_SAMPLES = 6
        const val CLOCK_ANCHOR_POLL_MS = 3L
        const val CUE_DRIFT_CONSECUTIVE_LIMIT = 3
    }
}
