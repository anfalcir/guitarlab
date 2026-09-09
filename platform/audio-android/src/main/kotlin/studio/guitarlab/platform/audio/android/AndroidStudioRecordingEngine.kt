package studio.guitarlab.platform.audio.android

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.AudioDeviceInfo
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import java.io.File
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sqrt
import studio.guitarlab.core.audio.AudioTransport
import studio.guitarlab.core.audio.MonitoringMode
import studio.guitarlab.core.audio.SoftwareMonitoringPolicy
import studio.guitarlab.core.codec.FloatWavFileWriter

data class StudioRecordingRequest(
    val temporaryFile: File,
    val preferredSampleRateHz: Int? = null,
    val preferredInputDevice: AudioDeviceInfo? = null,
    val preferredInputRequested: Boolean = false,
    val monitoringMode: MonitoringMode = MonitoringMode.AUTO,
    val preferredOutputDevice: AudioDeviceInfo? = null,
)

data class StudioRecordingConfig(
    val sampleRateHz: Int,
    val channelCount: Int,
    val inputEncodingLabel: String,
    val routedInputLabel: String?,
    val softwareMonitoringEnabled: Boolean,
)

enum class StudioRecordingStopReason { USER_STOP, ROUTE_LOST, INPUT_ERROR }

data class StudioRecordingResult(
    val temporaryFile: File,
    val sampleRateHz: Int,
    val channelCount: Int,
    val framesCaptured: Long,
    val peak: Float,
    val rms: Float,
    val stopReason: StudioRecordingStopReason,
    val partial: Boolean,
    val message: String? = null,
)

interface StudioRecordingListener {
    fun onStarted(config: StudioRecordingConfig)
    fun onProgress(framesCaptured: Long, peak: Float, rms: Float)
    fun onStopped(result: StudioRecordingResult)
    fun onError(message: String)
    fun onWarning(message: String) {}
}

/** Android M5 capture engine. Final media ownership/commit remains in core:project. */
class AndroidStudioRecordingEngine(context: Context) : AutoCloseable {
    private val appContext = context.applicationContext
    @Volatile private var running = false
    @Volatile private var worker: Thread? = null
    @Volatile private var activeRecorder: AudioRecord? = null
    @Volatile private var activeMonitor: AudioTrack? = null

    @Synchronized
    fun start(request: StudioRecordingRequest, listener: StudioRecordingListener) {
        check(!running) { "A gravação já está em execução." }
        check(appContext.checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            "Permissão para gravar áudio não concedida."
        }
        require(request.preferredSampleRateHz == null || request.preferredSampleRateHz in 8_000..384_000) {
            "Taxa de amostragem de gravação inválida."
        }
        if (request.preferredInputRequested) {
            requireNotNull(request.preferredInputDevice) { "A entrada selecionada não está disponível." }
        }
        running = true
        worker = Thread({ runCapture(request, listener) }, "GuitarLab-StudioRecording").also { it.start() }
    }

    @Synchronized
    fun stop() {
        running = false
        runCatching { activeRecorder?.stop() }
        runCatching { activeMonitor?.pause() }
    }

    override fun close() = stop()

    private fun runCapture(request: StudioRecordingRequest, listener: StudioRecordingListener) {
        val opened = openInput(request)
        if (opened == null) {
            running = false
            synchronized(this) { worker = null }
            listener.onError(
                request.preferredSampleRateHz?.let { "A entrada não conseguiu abrir na taxa de $it Hz exigida pelo projeto." }
                    ?: "Não foi possível abrir uma entrada de áudio compatível."
            )
            return
        }

        val recorder = opened.recorder
        activeRecorder = recorder
        var monitor: AudioTrack? = null
        var writer: FloatWavFileWriter? = null
        var framesCaptured = 0L
        var overallPeak = 0f
        var overallSquares = 0.0
        var overallSamples = 0L
        var stopReason = StudioRecordingStopReason.USER_STOP
        var failureMessage: String? = null

        try {
            val takeWriter = FloatWavFileWriter(request.temporaryFile, opened.sampleRateHz, opened.channelCount)
            writer = takeWriter

            recorder.startRecording()
            if (recorder.recordingState != AudioRecord.RECORDSTATE_RECORDING) error("A entrada não entrou no estado de gravação.")

            val routedAtStart = recorder.routedDevice
            if (request.preferredInputDevice != null && routedAtStart != null && routedAtStart.id != request.preferredInputDevice.id) {
                stopReason = StudioRecordingStopReason.ROUTE_LOST
                failureMessage = "O Android abriu uma entrada diferente da selecionada."
            }

            val shouldMonitor = SoftwareMonitoringPolicy.shouldMonitor(
                mode = request.monitoringMode,
                inputTransport = transportOf(routedAtStart ?: request.preferredInputDevice),
                outputTransport = transportOf(request.preferredOutputDevice),
            )
            if (shouldMonitor && stopReason == StudioRecordingStopReason.USER_STOP) {
                monitor = openMonitor(opened.sampleRateHz, request.preferredOutputDevice)
                if (monitor == null) {
                    listener.onWarning("O monitoramento por software não pôde ser aberto; a gravação continuará sem retorno pelo app.")
                } else {
                    activeMonitor = monitor
                    monitor.play()
                }
            }

            if (stopReason == StudioRecordingStopReason.USER_STOP) {
                listener.onStarted(
                    StudioRecordingConfig(
                        sampleRateHz = opened.sampleRateHz,
                        channelCount = opened.channelCount,
                        inputEncodingLabel = opened.encoding.label,
                        routedInputLabel = routedAtStart?.productName?.toString() ?: request.preferredInputDevice?.productName?.toString(),
                        softwareMonitoringEnabled = monitor != null,
                    )
                )
            }

            var zeroReads = 0
            while (running && stopReason == StudioRecordingStopReason.USER_STOP) {
                if (request.preferredInputDevice != null) {
                    val routed = recorder.routedDevice
                    if (routed != null && routed.id != request.preferredInputDevice.id) {
                        stopReason = StudioRecordingStopReason.ROUTE_LOST
                        failureMessage = "A entrada selecionada foi desconectada ou a rota mudou durante a gravação."
                        break
                    }
                }

                val chunk = opened.readFloatChunk(recorder)
                if (chunk.samplesRead < 0) {
                    if (!running) break
                    stopReason = StudioRecordingStopReason.INPUT_ERROR
                    failureMessage = "Falha de leitura da entrada de áudio: código ${chunk.samplesRead}."
                    break
                }
                if (chunk.samplesRead == 0) {
                    zeroReads++
                    if (zeroReads >= MAX_ZERO_READS) {
                        stopReason = StudioRecordingStopReason.INPUT_ERROR
                        failureMessage = "A entrada de áudio parou de fornecer dados."
                        break
                    }
                    continue
                }
                zeroReads = 0

                val completeSamples = chunk.samplesRead - (chunk.samplesRead % opened.channelCount)
                val framesRead = completeSamples / opened.channelCount
                if (framesRead <= 0) continue
                takeWriter.writeInterleaved(chunk.samples, frameCount = framesRead)
                framesCaptured += framesRead

                var chunkPeak = 0f
                var chunkSquares = 0.0
                for (index in 0 until completeSamples) {
                    val sample = chunk.samples[index]
                    chunkPeak = max(chunkPeak, abs(sample))
                    chunkSquares += sample.toDouble() * sample.toDouble()
                }
                val chunkRms = sqrt(chunkSquares / completeSamples).toFloat()
                overallPeak = max(overallPeak, chunkPeak)
                overallSquares += chunkSquares
                overallSamples += completeSamples
                listener.onProgress(framesCaptured, chunkPeak, chunkRms)
                monitor?.let { writeMonitor(it, chunk.samples, completeSamples, opened.channelCount) }
            }
        } catch (error: Throwable) {
            if (running) {
                stopReason = StudioRecordingStopReason.INPUT_ERROR
                failureMessage = error.message ?: "Falha inesperada durante a gravação."
            }
        } finally {
            running = false
            runCatching { recorder.stop() }
            runCatching { recorder.release() }
            runCatching { monitor?.pause() }
            runCatching { monitor?.flush() }
            runCatching { monitor?.release() }
            activeRecorder = null
            activeMonitor = null
            runCatching { writer?.close() }.onFailure { error ->
                stopReason = StudioRecordingStopReason.INPUT_ERROR
                failureMessage = error.message ?: "Falha ao finalizar o arquivo WAV."
            }
            synchronized(this) { worker = null }
        }

        if (framesCaptured <= 0L || !request.temporaryFile.isFile || request.temporaryFile.length() <= 44L) {
            listener.onError(failureMessage ?: "Nenhum áudio foi capturado.")
            return
        }

        val rms = if (overallSamples > 0) sqrt(overallSquares / overallSamples).toFloat() else 0f
        listener.onStopped(
            StudioRecordingResult(
                temporaryFile = request.temporaryFile,
                sampleRateHz = opened.sampleRateHz,
                channelCount = opened.channelCount,
                framesCaptured = framesCaptured,
                peak = overallPeak,
                rms = rms,
                stopReason = stopReason,
                partial = stopReason != StudioRecordingStopReason.USER_STOP,
                message = failureMessage,
            )
        )
    }

    private fun openInput(request: StudioRecordingRequest): OpenedInput? {
        if (appContext.checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) return null
        val device = request.preferredInputDevice
        val advertisedRates = device?.sampleRates?.filter { it in 8_000..384_000 }.orEmpty()
        val rates = request.preferredSampleRateHz?.let(::listOf) ?: run {
            val preferred = listOf(48_000, 44_100, 96_000, 88_200)
            if (advertisedRates.isEmpty()) preferred else preferred.filter { it in advertisedRates } + advertisedRates.filterNot { it in preferred }.sorted()
        }
        val advertisedChannels = device?.channelCounts?.filter { it in 1..2 }.orEmpty()
        val channels = if (advertisedChannels.isEmpty()) listOf(1, 2) else listOf(1, 2).filter { it in advertisedChannels }.ifEmpty { listOf(advertisedChannels.first()) }
        val advertisedEncodings = device?.encodings?.toSet().orEmpty()
        val encodings = listOf(InputEncoding.FLOAT32, InputEncoding.PCM16).filter {
            advertisedEncodings.isEmpty() || it.androidEncoding in advertisedEncodings
        }.ifEmpty { listOf(InputEncoding.PCM16) }
        val sources = if (device?.type == AudioDeviceInfo.TYPE_BUILTIN_MIC) {
            listOf(MediaRecorder.AudioSource.MIC, MediaRecorder.AudioSource.UNPROCESSED, MediaRecorder.AudioSource.DEFAULT)
        } else {
            listOf(MediaRecorder.AudioSource.UNPROCESSED, MediaRecorder.AudioSource.DEFAULT, MediaRecorder.AudioSource.MIC)
        }.distinct()

        for (source in sources) for (rate in rates.distinct()) for (channelCount in channels) for (encoding in encodings) {
            val channelMask = if (channelCount == 1) AudioFormat.CHANNEL_IN_MONO else AudioFormat.CHANNEL_IN_STEREO
            val minBytes = AudioRecord.getMinBufferSize(rate, channelMask, encoding.androidEncoding)
            if (minBytes <= 0) continue
            val bytesPerFrame = encoding.bytesPerSample * channelCount
            val bufferBytes = max(minBytes * 2, bytesPerFrame * 512)
            val recorder = try {
                AudioRecord.Builder()
                    .setAudioSource(source)
                    .setAudioFormat(
                        AudioFormat.Builder().setEncoding(encoding.androidEncoding).setSampleRate(rate).setChannelMask(channelMask).build()
                    )
                    .setBufferSizeInBytes(bufferBytes)
                    .build()
            } catch (_: SecurityException) {
                return null
            } catch (_: IllegalArgumentException) {
                continue
            } catch (_: UnsupportedOperationException) {
                continue
            }
            if (recorder.state != AudioRecord.STATE_INITIALIZED) {
                runCatching { recorder.release() }
                continue
            }
            if (device != null && !recorder.setPreferredDevice(device)) {
                runCatching { recorder.release() }
                continue
            }
            return OpenedInput(recorder, rate, channelCount, encoding, bufferBytes / bytesPerFrame)
        }
        return null
    }

    private fun openMonitor(sampleRateHz: Int, preferredOutputDevice: AudioDeviceInfo?): AudioTrack? {
        val minBytes = AudioTrack.getMinBufferSize(sampleRateHz, AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_FLOAT)
        if (minBytes <= 0) return null
        val track = runCatching {
            AudioTrack.Builder()
                .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build())
                .setAudioFormat(
                    AudioFormat.Builder().setEncoding(AudioFormat.ENCODING_PCM_FLOAT).setSampleRate(sampleRateHz).setChannelMask(AudioFormat.CHANNEL_OUT_STEREO).build()
                )
                .setTransferMode(AudioTrack.MODE_STREAM)
                .setBufferSizeInBytes(max(minBytes * 2, 4096))
                .setPerformanceMode(AudioTrack.PERFORMANCE_MODE_LOW_LATENCY)
                .build()
        }.getOrNull() ?: return null
        if (track.state != AudioTrack.STATE_INITIALIZED) {
            runCatching { track.release() }
            return null
        }
        if (preferredOutputDevice != null && !track.setPreferredDevice(preferredOutputDevice)) {
            runCatching { track.release() }
            return null
        }
        return track
    }

    private fun writeMonitor(track: AudioTrack, input: FloatArray, sampleCount: Int, channels: Int) {
        val stereo = if (channels == 2) input.copyOf(sampleCount) else FloatArray(sampleCount * 2).also { output ->
            repeat(sampleCount) { index ->
                val sample = input[index]
                output[index * 2] = sample
                output[index * 2 + 1] = sample
            }
        }
        runCatching { track.write(stereo, 0, stereo.size, AudioTrack.WRITE_NON_BLOCKING) }
    }

    private fun transportOf(device: AudioDeviceInfo?): AudioTransport? = when (device?.type) {
        AudioDeviceInfo.TYPE_USB_DEVICE, AudioDeviceInfo.TYPE_USB_ACCESSORY, AudioDeviceInfo.TYPE_USB_HEADSET -> AudioTransport.USB
        AudioDeviceInfo.TYPE_BUILTIN_EARPIECE, AudioDeviceInfo.TYPE_BUILTIN_SPEAKER, AudioDeviceInfo.TYPE_BUILTIN_MIC, AudioDeviceInfo.TYPE_BUILTIN_SPEAKER_SAFE -> AudioTransport.BUILT_IN
        AudioDeviceInfo.TYPE_BLUETOOTH_SCO, AudioDeviceInfo.TYPE_BLUETOOTH_A2DP, AudioDeviceInfo.TYPE_HEARING_AID, AudioDeviceInfo.TYPE_BLE_HEADSET, AudioDeviceInfo.TYPE_BLE_SPEAKER, AudioDeviceInfo.TYPE_BLE_BROADCAST -> AudioTransport.BLUETOOTH
        AudioDeviceInfo.TYPE_HDMI, AudioDeviceInfo.TYPE_HDMI_ARC, AudioDeviceInfo.TYPE_HDMI_EARC -> AudioTransport.HDMI
        AudioDeviceInfo.TYPE_WIRED_HEADSET, AudioDeviceInfo.TYPE_WIRED_HEADPHONES, AudioDeviceInfo.TYPE_LINE_ANALOG, AudioDeviceInfo.TYPE_LINE_DIGITAL, AudioDeviceInfo.TYPE_AUX_LINE -> AudioTransport.WIRED
        null -> null
        else -> AudioTransport.OTHER
    }

    private data class FloatChunk(val samples: FloatArray, val samplesRead: Int)

    private data class OpenedInput(
        val recorder: AudioRecord,
        val sampleRateHz: Int,
        val channelCount: Int,
        val encoding: InputEncoding,
        val bufferFrames: Int,
    ) {
        fun readFloatChunk(recorder: AudioRecord): FloatChunk {
            val wantedSamples = bufferFrames.coerceIn(128, 1024) * channelCount
            return when (encoding) {
                InputEncoding.FLOAT32 -> {
                    val samples = FloatArray(wantedSamples)
                    FloatChunk(samples, recorder.read(samples, 0, samples.size, AudioRecord.READ_BLOCKING))
                }
                InputEncoding.PCM16 -> {
                    val shorts = ShortArray(wantedSamples)
                    val read = recorder.read(shorts, 0, shorts.size, AudioRecord.READ_BLOCKING)
                    if (read <= 0) FloatChunk(FloatArray(0), read) else {
                        val samples = FloatArray(read)
                        repeat(read) { index -> samples[index] = shorts[index] / 32768f }
                        FloatChunk(samples, read)
                    }
                }
            }
        }
    }

    private enum class InputEncoding(val androidEncoding: Int, val bytesPerSample: Int, val label: String) {
        FLOAT32(AudioFormat.ENCODING_PCM_FLOAT, 4, "float 32 bits"),
        PCM16(AudioFormat.ENCODING_PCM_16BIT, 2, "PCM 16 bits"),
    }

    private companion object { const val MAX_ZERO_READS = 8 }
}
