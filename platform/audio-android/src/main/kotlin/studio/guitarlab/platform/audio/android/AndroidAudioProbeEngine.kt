package studio.guitarlab.platform.audio.android

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.hardware.SensorPrivacyManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import studio.guitarlab.core.audio.AudioDeviceDescriptor
import studio.guitarlab.core.audio.AudioDirection
import studio.guitarlab.core.audio.AudioProbeEngine
import studio.guitarlab.core.audio.AudioProbeOperation
import studio.guitarlab.core.audio.AudioProbePolicy
import studio.guitarlab.core.audio.AudioProbeResult
import studio.guitarlab.core.audio.AudioSignalStats
import studio.guitarlab.core.audio.AudioStreamConfig
import studio.guitarlab.core.audio.PcmEncoding
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.PI
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * M2 diagnostic engine.
 *
 * This intentionally uses public Android AudioRecord/AudioTrack APIs so the first
 * hardware gate can validate routing, channel mapping and duplex stability before
 * the final realtime Oboe engine is introduced. Product audio code depends only on
 * [AudioProbeEngine], so this implementation is replaceable.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AndroidAudioProbeEngine(context: Context) : AudioProbeEngine {
    private val appContext = context.applicationContext
    private val audioManager = appContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val stopRequested = AtomicBoolean(false)

    override val deviceUpdates: Flow<List<AudioDeviceDescriptor>> = callbackFlow {
        fun emitSnapshot() {
            trySend(scanDevices())
        }

        val callback = object : AudioDeviceCallback() {
            override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>) = emitSnapshot()
            override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>) = emitSnapshot()
        }

        audioManager.registerAudioDeviceCallback(callback, Handler(Looper.getMainLooper()))
        emitSnapshot()
        awaitClose { runCatching { audioManager.unregisterAudioDeviceCallback(callback) } }
    }

    override suspend fun refreshDevices(): List<AudioDeviceDescriptor> = withContext(Dispatchers.Default) {
        scanDevices()
    }

    override suspend fun runPlaybackTest(
        outputDeviceKey: String?,
        durationMs: Long
    ): AudioProbeResult = withContext(Dispatchers.IO) {
        stopRequested.set(false)
        val startedNs = System.nanoTime()
        val device = resolveDevice(outputDeviceKey, AudioDirection.OUTPUT)
        if (outputDeviceKey != null && device == null) {
            return@withContext failedResult(
                AudioProbeOperation.PLAYBACK,
                startedNs,
                "Selected output device is no longer available."
            )
        }
        val opened = openOutput(device)
            ?: return@withContext failedResult(
                AudioProbeOperation.PLAYBACK,
                startedNs,
                "Could not open a compatible output stream for the selected device."
            )

        val track = opened.track
        var framesWritten = 0L
        var zeroWrites = 0
        val warnings = mutableListOf<String>()
        try {
            track.play()
            val targetFrames = (opened.config.sampleRateHz * (durationMs / 1000.0)).toLong().coerceAtLeast(1)
            val chunkFrames = min(opened.config.bufferFrames.coerceAtLeast(128), 1024)
            var phase = 0.0
            val phaseStep = 2.0 * PI * TEST_TONE_HZ / opened.config.sampleRateHz

            while (framesWritten < targetFrames && !stopRequested.get() && isActive) {
                val frames = min(chunkFrames.toLong(), targetFrames - framesWritten).toInt()
                val result = when (opened.config.encoding) {
                    PcmEncoding.FLOAT_32 -> {
                        val buffer = FloatArray(frames * opened.config.channelCount)
                        var index = 0
                        repeat(frames) {
                            val sample = (sin(phase) * TEST_TONE_AMPLITUDE).toFloat()
                            phase += phaseStep
                            repeat(opened.config.channelCount) { buffer[index++] = sample }
                        }
                        track.write(buffer, 0, buffer.size, AudioTrack.WRITE_BLOCKING)
                    }

                    PcmEncoding.PCM_16 -> {
                        val buffer = ShortArray(frames * opened.config.channelCount)
                        var index = 0
                        repeat(frames) {
                            val sample = (sin(phase) * TEST_TONE_AMPLITUDE * Short.MAX_VALUE).toInt().toShort()
                            phase += phaseStep
                            repeat(opened.config.channelCount) { buffer[index++] = sample }
                        }
                        track.write(buffer, 0, buffer.size, AudioTrack.WRITE_BLOCKING)
                    }
                }

                if (result < 0) {
                    return@withContext failedResult(
                        AudioProbeOperation.PLAYBACK,
                        startedNs,
                        "Output write failed with AudioTrack error $result.",
                        outputConfig = opened.config
                    )
                }
                val writtenFrames = result / opened.config.channelCount
                framesWritten += writtenFrames
                if (writtenFrames == 0) {
                    zeroWrites++
                    if (zeroWrites >= MAX_STALL_COUNT) {
                        return@withContext failedResult(
                            AudioProbeOperation.PLAYBACK,
                            startedNs,
                            "Output stream stalled with repeated zero-length writes.",
                            outputConfig = opened.config
                        )
                    }
                } else {
                    zeroWrites = 0
                }
            }

            val routedKey = track.routedDevice?.id?.toString()
            val verifiedConfig = opened.config.copy(routedDeviceKey = routedKey)
            if (outputDeviceKey != null && routedKey != null && routedKey != outputDeviceKey) {
                return@withContext failedResult(
                    AudioProbeOperation.PLAYBACK,
                    startedNs,
                    "Android routed playback to device $routedKey instead of selected device $outputDeviceKey.",
                    outputConfig = verifiedConfig
                )
            }
            if (outputDeviceKey != null && routedKey == null) warnings += "Android did not expose the active output route for verification."
            val stopped = stopRequested.get()
            AudioProbeResult(
                operation = AudioProbeOperation.PLAYBACK,
                success = !stopped,
                stopped = stopped,
                elapsedMs = elapsedMs(startedNs),
                outputConfig = verifiedConfig,
                outputFrames = framesWritten,
                outputUnderruns = track.underrunCount,
                message = if (stopped) "Playback test stopped by user." else "Playback stream completed.",
                warnings = warnings.distinct()
            )
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Throwable) {
            failedResult(
                AudioProbeOperation.PLAYBACK,
                startedNs,
                error.message ?: error::class.java.simpleName,
                outputConfig = opened.config
            )
        } finally {
            safelyRelease(track)
        }
    }

    override suspend fun runRecordTest(
        inputDeviceKey: String?,
        durationMs: Long
    ): AudioProbeResult = withContext(Dispatchers.IO) {
        stopRequested.set(false)
        val startedNs = System.nanoTime()
        val device = resolveDevice(inputDeviceKey, AudioDirection.INPUT)
        if (inputDeviceKey != null && device == null) {
            return@withContext failedResult(
                AudioProbeOperation.RECORD,
                startedNs,
                "Selected input device is no longer available."
            )
        }
        microphoneBlockReason()?.let { reason ->
            return@withContext failedResult(
                AudioProbeOperation.RECORD,
                startedNs,
                reason
            )
        }
        val opened = openInput(device)
            ?: return@withContext failedResult(
                AudioProbeOperation.RECORD,
                startedNs,
                "Could not open a compatible input stream. Check RECORD_AUDIO permission, system microphone privacy/mute, and the selected device."
            )

        val recorder = opened.recorder
        try {
            recorder.startRecording()
            if (recorder.recordingState != AudioRecord.RECORDSTATE_RECORDING) {
                return@withContext failedResult(
                    AudioProbeOperation.RECORD,
                    startedNs,
                    "AudioRecord did not enter RECORDING state.",
                    inputConfig = opened.config
                )
            }

            val stats = captureStats(recorder, opened.config, durationMs)
            val routedKey = recorder.routedDevice?.id?.toString()
            val verifiedConfig = opened.config.copy(routedDeviceKey = routedKey)
            if (inputDeviceKey != null && routedKey != null && routedKey != inputDeviceKey) {
                return@withContext failedResult(
                    AudioProbeOperation.RECORD,
                    startedNs,
                    "Android routed recording to device $routedKey instead of selected device $inputDeviceKey.",
                    inputConfig = verifiedConfig
                )
            }
            val zeroSignal = stats.frames > 0 && !AudioProbePolicy.hasUsableInputSignal(stats)
            val warnings = buildList {
                if (inputDeviceKey != null && routedKey == null) add("Android did not expose the active input route for verification.")
                if (zeroSignal) {
                    add(
                        microphoneBlockReason()
                            ?: "Capture returned only digital zero. The stream is open and routed, but no usable audio reached the app; verify Android microphone privacy/mute, input source/routing, cable/device state, and Pocket Amp loopback/input state."
                    )
                }
            }
            val stopped = stopRequested.get()
            AudioProbeResult(
                operation = AudioProbeOperation.RECORD,
                success = !stopped && !zeroSignal,
                stopped = stopped,
                elapsedMs = elapsedMs(startedNs),
                inputConfig = verifiedConfig,
                inputStats = stats,
                message = when {
                    stopped -> "Record test stopped by user."
                    zeroSignal -> "Input stream completed, but captured only digital silence."
                    else -> "Input capture completed with non-zero audio."
                },
                warnings = warnings
            )
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Throwable) {
            failedResult(
                AudioProbeOperation.RECORD,
                startedNs,
                error.message ?: error::class.java.simpleName,
                inputConfig = opened.config
            )
        } finally {
            safelyRelease(recorder)
        }
    }

    override suspend fun runDuplexTest(
        inputDeviceKey: String?,
        outputDeviceKey: String?,
        durationMs: Long
    ): AudioProbeResult = withContext(Dispatchers.IO) {
        stopRequested.set(false)
        val startedNs = System.nanoTime()
        val inputDevice = resolveDevice(inputDeviceKey, AudioDirection.INPUT)
        val outputDevice = resolveDevice(outputDeviceKey, AudioDirection.OUTPUT)
        if (inputDeviceKey != null && inputDevice == null) {
            return@withContext failedResult(AudioProbeOperation.DUPLEX, startedNs, "Selected input device is no longer available.")
        }
        if (outputDeviceKey != null && outputDevice == null) {
            return@withContext failedResult(AudioProbeOperation.DUPLEX, startedNs, "Selected output device is no longer available.")
        }

        val output = openOutput(outputDevice)
            ?: return@withContext failedResult(
                AudioProbeOperation.DUPLEX,
                startedNs,
                "Could not open output stream for duplex test."
            )
        val input = openInput(inputDevice, preferredRate = output.config.sampleRateHz)
            ?: run {
                safelyRelease(output.track)
                return@withContext failedResult(
                    AudioProbeOperation.DUPLEX,
                    startedNs,
                    "Could not open input stream for duplex test.",
                    outputConfig = output.config
                )
            }

        val recorder = input.recorder
        val track = output.track
        try {
            // A streaming AudioTrack started empty can increment underrunCount before
            // the output worker gets its first scheduling slice. Prime a bounded amount
            // of tone before play() so M2 measures stream stability, not startup order.
            val primedOutput = primeOutput(track, output.config, DUPLEX_TONE_HZ)
            val underrunBaseline = track.underrunCount

            recorder.startRecording()
            track.play()

            if (recorder.recordingState != AudioRecord.RECORDSTATE_RECORDING) {
                return@withContext failedResult(
                    AudioProbeOperation.DUPLEX,
                    startedNs,
                    "Input stream failed to enter RECORDING state.",
                    inputConfig = input.config,
                    outputConfig = output.config
                )
            }

            // Run the blocking input and output pumps on separate IO workers. Alternating
            // AudioRecord.read() and AudioTrack.write() on one thread can manufacture
            // underruns that are artifacts of the probe itself instead of the USB path.
            val duplex = coroutineScope {
                val outputDeferred = async(Dispatchers.IO) {
                    pumpOutput(
                        track = track,
                        config = output.config,
                        durationMs = durationMs,
                        toneHz = DUPLEX_TONE_HZ,
                        initialFrames = primedOutput.frames,
                        initialPhase = primedOutput.nextPhase
                    )
                }
                val inputDeferred = async(Dispatchers.IO) {
                    pumpInput(recorder, input.config, durationMs)
                }
                try {
                    DuplexLoopResult(
                        input = inputDeferred.await(),
                        output = outputDeferred.await()
                    )
                } catch (error: Throwable) {
                    // Make the sibling blocking loop converge as soon as the current
                    // read/write returns, then let structured concurrency cancel it.
                    stopRequested.set(true)
                    throw error
                }
            }

            val inputFrames = duplex.input.frames
            val outputFrames = duplex.output.frames
            val peak = duplex.input.peak
            val sumSquares = duplex.input.sumSquares
            val sampleCount = duplex.input.sampleCount
            val outputUnderruns = AudioProbePolicy.underrunDelta(underrunBaseline, track.underrunCount)

            val inputRoutedKey = recorder.routedDevice?.id?.toString()
            val outputRoutedKey = track.routedDevice?.id?.toString()
            val verifiedInput = input.config.copy(routedDeviceKey = inputRoutedKey)
            val verifiedOutput = output.config.copy(routedDeviceKey = outputRoutedKey)
            if (inputDeviceKey != null && inputRoutedKey != null && inputRoutedKey != inputDeviceKey) {
                return@withContext failedResult(
                    AudioProbeOperation.DUPLEX,
                    startedNs,
                    "Duplex input routed to $inputRoutedKey instead of selected $inputDeviceKey.",
                    inputConfig = verifiedInput,
                    outputConfig = verifiedOutput
                )
            }
            if (outputDeviceKey != null && outputRoutedKey != null && outputRoutedKey != outputDeviceKey) {
                return@withContext failedResult(
                    AudioProbeOperation.DUPLEX,
                    startedNs,
                    "Duplex output routed to $outputRoutedKey instead of selected $outputDeviceKey.",
                    inputConfig = verifiedInput,
                    outputConfig = verifiedOutput
                )
            }
            val zeroSignal = inputFrames > 0 && !AudioProbePolicy.hasUsableInputSignal(
                AudioSignalStats(frames = inputFrames, peak = peak)
            )
            val stopped = stopRequested.get()
            AudioProbeResult(
                operation = AudioProbeOperation.DUPLEX,
                success = !stopped && !zeroSignal,
                stopped = stopped,
                elapsedMs = elapsedMs(startedNs),
                inputConfig = verifiedInput,
                outputConfig = verifiedOutput,
                inputStats = AudioSignalStats(
                    frames = inputFrames,
                    peak = peak,
                    rms = if (sampleCount > 0) sqrt(sumSquares / sampleCount).toFloat() else 0f
                ),
                outputFrames = outputFrames,
                outputUnderruns = outputUnderruns,
                message = when {
                    stopped -> "Duplex test stopped by user."
                    zeroSignal -> "Duplex streams ran concurrently, but input captured only digital silence."
                    else -> "Input and output streams ran concurrently with non-zero input audio."
                },
                warnings = buildList {
                    if (input.config.sampleRateHz != output.config.sampleRateHz) {
                        add("Input/output sample rates differ; final realtime engine must reconcile them explicitly.")
                    }
                    if (inputDeviceKey != null && inputRoutedKey == null) add("Android did not expose the active duplex input route for verification.")
                    if (outputDeviceKey != null && outputRoutedKey == null) add("Android did not expose the active duplex output route for verification.")
                    if (zeroSignal) add(
                        microphoneBlockReason()
                            ?: "Duplex input returned only digital zero; verify Android microphone privacy/mute and the selected input routing/loopback state."
                    )
                    if (outputUnderruns > 0) add("Duplex output reported $outputUnderruns underrun(s) after prefill. Repeat the test before approving M2.")
                }
            )
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Throwable) {
            failedResult(
                AudioProbeOperation.DUPLEX,
                startedNs,
                error.message ?: error::class.java.simpleName,
                inputConfig = input.config,
                outputConfig = output.config
            )
        } finally {
            safelyRelease(recorder)
            safelyRelease(track)
        }
    }

    override fun stopCurrentTest() {
        stopRequested.set(true)
    }

    override fun close() {
        stopRequested.set(true)
    }

    private fun scanDevices(): List<AudioDeviceDescriptor> = (
        audioManager.getDevices(AudioManager.GET_DEVICES_INPUTS) +
            audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
        )
        .map(AndroidAudioDeviceMapper::map)
        .groupBy { it.key }
        .values
        .map { descriptors ->
            descriptors.reduce { merged, descriptor ->
                merged.copy(
                    supportsInput = merged.supportsInput || descriptor.supportsInput,
                    supportsOutput = merged.supportsOutput || descriptor.supportsOutput,
                    sampleRatesHz = (merged.sampleRatesHz + descriptor.sampleRatesHz).distinct().sorted(),
                    channelCounts = (merged.channelCounts + descriptor.channelCounts).distinct().sorted(),
                    encodings = (merged.encodings + descriptor.encodings).distinct()
                )
            }
        }
        .sortedWith(
            compareByDescending<AudioDeviceDescriptor> { it.transport == studio.guitarlab.core.audio.AudioTransport.USB }
                .thenBy { it.name.lowercase() }
        )

    private fun resolveDevice(key: String?, direction: AudioDirection): AudioDeviceInfo? {
        if (key == null) return null
        val deviceFlag = when (direction) {
            AudioDirection.INPUT -> AudioManager.GET_DEVICES_INPUTS
            AudioDirection.OUTPUT -> AudioManager.GET_DEVICES_OUTPUTS
        }
        return audioManager.getDevices(deviceFlag).firstOrNull { device ->
            device.id.toString() == key && when (direction) {
                AudioDirection.INPUT -> device.isSource
                AudioDirection.OUTPUT -> device.isSink
            }
        }
    }

    private fun openOutput(device: AudioDeviceInfo?): OpenedOutput? {
        val descriptor = device?.let(AndroidAudioDeviceMapper::map)
        val rates = AudioProbePolicy.sampleRateCandidates(descriptor?.sampleRatesHz.orEmpty())
        val channels = AudioProbePolicy.channelCandidates(descriptor?.channelCounts.orEmpty(), AudioDirection.OUTPUT)
        val encodings = AudioProbePolicy.encodingCandidates(descriptor?.encodings.orEmpty())

        for (rate in rates) for (channelCount in channels) for (encoding in encodings) {
            val channelMask = outputChannelMask(channelCount) ?: continue
            val androidEncoding = androidEncoding(encoding)
            val minBytes = AudioTrack.getMinBufferSize(rate, channelMask, androidEncoding)
            if (minBytes <= 0) continue
            val bytesPerFrame = bytesPerSample(encoding) * channelCount
            val bufferBytes = max(minBytes * 2, bytesPerFrame * 256)

            val track = runCatching {
                AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(androidEncoding)
                            .setSampleRate(rate)
                            .setChannelMask(channelMask)
                            .build()
                    )
                    .setBufferSizeInBytes(bufferBytes)
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .setPerformanceMode(AudioTrack.PERFORMANCE_MODE_LOW_LATENCY)
                    .build()
            }.getOrNull() ?: continue

            if (track.state != AudioTrack.STATE_INITIALIZED) {
                safelyRelease(track)
                continue
            }
            if (device != null && !track.setPreferredDevice(device)) {
                safelyRelease(track)
                continue
            }

            return OpenedOutput(
                track,
                AudioStreamConfig(
                    deviceKey = device?.id?.toString(),
                    sampleRateHz = rate,
                    channelCount = channelCount,
                    encoding = encoding,
                    bufferFrames = bufferBytes / bytesPerFrame,
                    requestedLowLatency = true
                )
            )
        }
        return null
    }

    private fun openInput(device: AudioDeviceInfo?, preferredRate: Int? = null): OpenedInput? {
        if (appContext.checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            return null
        }
        val descriptor = device?.let(AndroidAudioDeviceMapper::map)
        val baseRates = AudioProbePolicy.sampleRateCandidates(descriptor?.sampleRatesHz.orEmpty())
        // Some Android devices advertise only legacy voice rates for the built-in
        // microphone even though AudioRecord accepts normal media rates. Prefer
        // studio/media rates first and let the platform validate each candidate.
        val mediaRates = if (device?.type == AudioDeviceInfo.TYPE_BUILTIN_MIC) {
            (listOf(48_000, 44_100) + baseRates).distinct()
        } else {
            baseRates
        }
        val rates = preferredRate?.let { preferred ->
            (listOf(preferred) + mediaRates).distinct()
        } ?: mediaRates
        val channels = AudioProbePolicy.channelCandidates(descriptor?.channelCounts.orEmpty(), AudioDirection.INPUT)
        val encodings = AudioProbePolicy.encodingCandidates(descriptor?.encodings.orEmpty())
        val unprocessed = if (supportsUnprocessedSource()) listOf(MediaRecorder.AudioSource.UNPROCESSED) else emptyList()
        val sources = if (device?.type == AudioDeviceInfo.TYPE_BUILTIN_MIC) {
            // MIC is the least ambiguous source for an actual tablet microphone.
            (listOf(MediaRecorder.AudioSource.MIC) + unprocessed + MediaRecorder.AudioSource.DEFAULT).distinct()
        } else {
            // USB interfaces should avoid platform processing when possible. MIC is
            // kept only as a compatibility fallback; routedDevice is verified later.
            (unprocessed + listOf(MediaRecorder.AudioSource.DEFAULT, MediaRecorder.AudioSource.MIC)).distinct()
        }

        for (source in sources) for (rate in rates) for (channelCount in channels) for (encoding in encodings) {
            val channelMask = inputChannelMask(channelCount) ?: continue
            val androidEncoding = androidEncoding(encoding)
            val minBytes = AudioRecord.getMinBufferSize(rate, channelMask, androidEncoding)
            if (minBytes <= 0) continue
            val bytesPerFrame = bytesPerSample(encoding) * channelCount
            val bufferBytes = max(minBytes * 2, bytesPerFrame * 256)

            val recorder = runCatching {
                AudioRecord.Builder()
                    .setAudioSource(source)
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(androidEncoding)
                            .setSampleRate(rate)
                            .setChannelMask(channelMask)
                            .build()
                    )
                    .setBufferSizeInBytes(bufferBytes)
                    .build()
            }.getOrNull() ?: continue

            if (recorder.state != AudioRecord.STATE_INITIALIZED) {
                safelyRelease(recorder)
                continue
            }
            if (device != null && !recorder.setPreferredDevice(device)) {
                safelyRelease(recorder)
                continue
            }

            return OpenedInput(
                recorder,
                AudioStreamConfig(
                    deviceKey = device?.id?.toString(),
                    sampleRateHz = rate,
                    channelCount = channelCount,
                    encoding = encoding,
                    bufferFrames = bufferBytes / bytesPerFrame,
                    requestedLowLatency = false,
                    inputSourceLabel = when (source) {
                        MediaRecorder.AudioSource.UNPROCESSED -> "UNPROCESSED"
                        MediaRecorder.AudioSource.MIC -> "MIC"
                        else -> "DEFAULT"
                    }
                )
            )
        }
        return null
    }


    private suspend fun pumpInput(
        recorder: AudioRecord,
        config: AudioStreamConfig,
        durationMs: Long
    ): InputLoopResult {
        val targetFrames = (config.sampleRateHz * (durationMs / 1000.0)).toLong().coerceAtLeast(1)
        val chunkFrames = min(config.bufferFrames.coerceAtLeast(128), 1024)
        var frames = 0L
        var peak = 0f
        var sumSquares = 0.0
        var sampleCount = 0L
        var zeroReads = 0

        while (frames < targetFrames && !stopRequested.get() && kotlinx.coroutines.currentCoroutineContext().isActive) {
            val wantedFrames = min(chunkFrames.toLong(), targetFrames - frames).toInt()
            val chunk = readChunk(recorder, config, wantedFrames)
            if (chunk.samplesRead < 0) error("AudioRecord read failed with error ${chunk.samplesRead}")
            if (chunk.samplesRead == 0) {
                zeroReads++
                if (zeroReads >= MAX_STALL_COUNT) error("AudioRecord stalled with repeated zero-length reads")
            } else {
                zeroReads = 0
            }
            frames += chunk.samplesRead / config.channelCount
            peak = max(peak, chunk.peak)
            sumSquares += chunk.sumSquares
            sampleCount += chunk.sampleCount
        }

        return InputLoopResult(frames, peak, sumSquares, sampleCount)
    }

    private suspend fun pumpOutput(
        track: AudioTrack,
        config: AudioStreamConfig,
        durationMs: Long,
        toneHz: Double,
        initialFrames: Long = 0L,
        initialPhase: Double = 0.0
    ): OutputLoopResult {
        val targetFrames = (config.sampleRateHz * (durationMs / 1000.0)).toLong().coerceAtLeast(1)
        val chunkFrames = min(config.bufferFrames.coerceAtLeast(128), 1024)
        var frames = initialFrames.coerceIn(0L, targetFrames)
        var phase = initialPhase
        var zeroWrites = 0
        val phaseStep = 2.0 * PI * toneHz / config.sampleRateHz

        while (frames < targetFrames && !stopRequested.get() && kotlinx.coroutines.currentCoroutineContext().isActive) {
            val wantedFrames = min(chunkFrames.toLong(), targetFrames - frames).toInt()
            val writtenSamples = writeTone(track, config, wantedFrames, phase, phaseStep)
            if (writtenSamples < 0) error("AudioTrack write failed with error $writtenSamples")
            val writtenFrames = writtenSamples / config.channelCount
            if (writtenFrames == 0) {
                zeroWrites++
                if (zeroWrites >= MAX_STALL_COUNT) error("AudioTrack stalled with repeated zero-length writes")
            } else {
                zeroWrites = 0
            }
            frames += writtenFrames
            phase += phaseStep * writtenFrames
        }

        return OutputLoopResult(frames)
    }

    private fun primeOutput(
        track: AudioTrack,
        config: AudioStreamConfig,
        toneHz: Double
    ): OutputPrimeResult {
        val primeFrames = AudioProbePolicy.duplexPrimeFrames(config.sampleRateHz, config.bufferFrames)
        if (primeFrames <= 0) return OutputPrimeResult(0L, 0.0)
        val phaseStep = 2.0 * PI * toneHz / config.sampleRateHz
        val writtenSamples = writeTone(track, config, primeFrames, 0.0, phaseStep)
        if (writtenSamples < 0) error("AudioTrack prefill failed with error $writtenSamples")
        val writtenFrames = writtenSamples / config.channelCount
        if (writtenFrames <= 0) error("AudioTrack prefill produced no frames")
        return OutputPrimeResult(
            frames = writtenFrames.toLong(),
            nextPhase = phaseStep * writtenFrames
        )
    }

    private suspend fun captureStats(
        recorder: AudioRecord,
        config: AudioStreamConfig,
        durationMs: Long
    ): AudioSignalStats {
        val targetFrames = (config.sampleRateHz * (durationMs / 1000.0)).toLong().coerceAtLeast(1)
        val chunkFrames = min(config.bufferFrames.coerceAtLeast(128), 1024)
        var frames = 0L
        var peak = 0f
        var sumSquares = 0.0
        var sampleCount = 0L
        var zeroReads = 0

        while (frames < targetFrames && !stopRequested.get() && kotlinx.coroutines.currentCoroutineContext().isActive) {
            val wantedFrames = min(chunkFrames.toLong(), targetFrames - frames).toInt()
            val chunk = readChunk(recorder, config, wantedFrames)
            if (chunk.samplesRead < 0) error("AudioRecord read failed with error ${chunk.samplesRead}")
            if (chunk.samplesRead == 0) {
                zeroReads++
                if (zeroReads >= MAX_STALL_COUNT) error("AudioRecord stalled with repeated zero-length reads")
            } else {
                zeroReads = 0
            }
            frames += chunk.samplesRead / config.channelCount
            peak = max(peak, chunk.peak)
            sumSquares += chunk.sumSquares
            sampleCount += chunk.sampleCount
        }

        return AudioSignalStats(
            frames = frames,
            peak = peak,
            rms = if (sampleCount > 0) sqrt(sumSquares / sampleCount).toFloat() else 0f
        )
    }

    private fun readChunk(recorder: AudioRecord, config: AudioStreamConfig, frames: Int): ChunkStats {
        val sampleCapacity = frames * config.channelCount
        return when (config.encoding) {
            PcmEncoding.FLOAT_32 -> {
                val buffer = FloatArray(sampleCapacity)
                val read = recorder.read(buffer, 0, buffer.size, AudioRecord.READ_BLOCKING)
                if (read <= 0) return ChunkStats(read, 0f, 0.0, 0)
                var peak = 0f
                var sum = 0.0
                repeat(read) { index ->
                    val value = buffer[index].coerceIn(-1f, 1f)
                    peak = max(peak, kotlin.math.abs(value))
                    sum += value.toDouble() * value
                }
                ChunkStats(read, peak, sum, read.toLong())
            }

            PcmEncoding.PCM_16 -> {
                val buffer = ShortArray(sampleCapacity)
                val read = recorder.read(buffer, 0, buffer.size, AudioRecord.READ_BLOCKING)
                if (read <= 0) return ChunkStats(read, 0f, 0.0, 0)
                var peak = 0f
                var sum = 0.0
                repeat(read) { index ->
                    val value = buffer[index] / 32768f
                    peak = max(peak, kotlin.math.abs(value))
                    sum += value.toDouble() * value
                }
                ChunkStats(read, peak, sum, read.toLong())
            }
        }
    }

    private fun writeTone(
        track: AudioTrack,
        config: AudioStreamConfig,
        frames: Int,
        phaseStart: Double,
        phaseStep: Double
    ): Int = when (config.encoding) {
        PcmEncoding.FLOAT_32 -> {
            val buffer = FloatArray(frames * config.channelCount)
            var index = 0
            var phase = phaseStart
            repeat(frames) {
                val sample = (sin(phase) * TEST_TONE_AMPLITUDE).toFloat()
                phase += phaseStep
                repeat(config.channelCount) { buffer[index++] = sample }
            }
            track.write(buffer, 0, buffer.size, AudioTrack.WRITE_BLOCKING)
        }

        PcmEncoding.PCM_16 -> {
            val buffer = ShortArray(frames * config.channelCount)
            var index = 0
            var phase = phaseStart
            repeat(frames) {
                val sample = (sin(phase) * TEST_TONE_AMPLITUDE * Short.MAX_VALUE).toInt().toShort()
                phase += phaseStep
                repeat(config.channelCount) { buffer[index++] = sample }
            }
            track.write(buffer, 0, buffer.size, AudioTrack.WRITE_BLOCKING)
        }
    }

    private fun microphoneBlockReason(): String? {
        if (audioManager.isMicrophoneMute) {
            return "Android reports the microphone as muted. Disable the system microphone mute/privacy control and retry."
        }
        return null
    }

    private fun supportsUnprocessedSource(): Boolean =
        audioManager.getProperty(AudioManager.PROPERTY_SUPPORT_AUDIO_SOURCE_UNPROCESSED)
            ?.equals("true", ignoreCase = true) == true

    private fun androidEncoding(encoding: PcmEncoding): Int = when (encoding) {
        PcmEncoding.FLOAT_32 -> AudioFormat.ENCODING_PCM_FLOAT
        PcmEncoding.PCM_16 -> AudioFormat.ENCODING_PCM_16BIT
    }

    private fun bytesPerSample(encoding: PcmEncoding): Int = when (encoding) {
        PcmEncoding.FLOAT_32 -> 4
        PcmEncoding.PCM_16 -> 2
    }

    private fun inputChannelMask(channelCount: Int): Int? = when (channelCount) {
        1 -> AudioFormat.CHANNEL_IN_MONO
        2 -> AudioFormat.CHANNEL_IN_STEREO
        else -> null
    }

    private fun outputChannelMask(channelCount: Int): Int? = when (channelCount) {
        1 -> AudioFormat.CHANNEL_OUT_MONO
        2 -> AudioFormat.CHANNEL_OUT_STEREO
        else -> null
    }

    private fun safelyRelease(track: AudioTrack) {
        runCatching { if (track.playState == AudioTrack.PLAYSTATE_PLAYING) track.stop() }
        runCatching { track.flush() }
        runCatching { track.release() }
    }

    private fun safelyRelease(recorder: AudioRecord) {
        runCatching { if (recorder.recordingState == AudioRecord.RECORDSTATE_RECORDING) recorder.stop() }
        runCatching { recorder.release() }
    }

    private fun failedResult(
        operation: AudioProbeOperation,
        startedNs: Long,
        message: String,
        inputConfig: AudioStreamConfig? = null,
        outputConfig: AudioStreamConfig? = null
    ) = AudioProbeResult(
        operation = operation,
        success = false,
        elapsedMs = elapsedMs(startedNs),
        inputConfig = inputConfig,
        outputConfig = outputConfig,
        message = message
    )

    private fun elapsedMs(startedNs: Long): Long = (System.nanoTime() - startedNs) / 1_000_000L

    private data class OpenedInput(val recorder: AudioRecord, val config: AudioStreamConfig)
    private data class OpenedOutput(val track: AudioTrack, val config: AudioStreamConfig)
    private data class InputLoopResult(
        val frames: Long,
        val peak: Float,
        val sumSquares: Double,
        val sampleCount: Long
    )
    private data class OutputPrimeResult(val frames: Long, val nextPhase: Double)
    private data class OutputLoopResult(val frames: Long)
    private data class DuplexLoopResult(val input: InputLoopResult, val output: OutputLoopResult)
    private data class ChunkStats(
        val samplesRead: Int,
        val peak: Float,
        val sumSquares: Double,
        val sampleCount: Long
    )

    private companion object {
        const val TEST_TONE_HZ = 440.0
        const val DUPLEX_TONE_HZ = 330.0
        const val TEST_TONE_AMPLITUDE = 0.08
        const val MAX_STALL_COUNT = 10
    }
}
