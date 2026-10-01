package studio.guitarlab.app.ui

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import java.text.DateFormat
import java.util.Date
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import studio.guitarlab.app.BuildConfig
import studio.guitarlab.core.audio.AudioDeviceDescriptor
import studio.guitarlab.core.audio.AudioDirection
import studio.guitarlab.core.audio.AudioProbeOperation
import studio.guitarlab.core.audio.AudioProbeReportFormatter
import studio.guitarlab.core.audio.AudioProbeResult
import studio.guitarlab.core.audio.AudioTransport
import studio.guitarlab.core.audio.RecordingSessionHealthRecord
import studio.guitarlab.platform.audio.android.AndroidAudioProbeEngine

data class AudioProbeUiState(
    val permissionGranted: Boolean = false,
    val devices: List<AudioDeviceDescriptor> = emptyList(),
    val selectedInputKey: String? = null,
    val selectedOutputKey: String? = null,
    val running: AudioProbeOperation? = null,
    val stopping: Boolean = false,
    val lastResult: AudioProbeResult? = null,
    val eventLog: List<String> = emptyList(),
    val sessionHealth: List<RecordingSessionHealthRecord> = emptyList(),
    val error: String? = null,
) {
    val inputs: List<AudioDeviceDescriptor> get() = devices.filter { it.supports(AudioDirection.INPUT) }
    val outputs: List<AudioDeviceDescriptor> get() = devices.filter { it.supports(AudioDirection.OUTPUT) }
}

class AudioProbeViewModel(application: Application) : AndroidViewModel(application) {
    private val engine = AndroidAudioProbeEngine(application)
    private val healthStore = RecordingSessionHealthStore(application)
    private val routingStore = StudioAudioRoutingStore(application)
    private val latencyStore = StudioLatencyCalibrationStore(application)
    private val _state = MutableStateFlow(
        AudioProbeUiState(
            permissionGranted = hasRecordPermission(application),
            sessionHealth = healthStore.history(),
        )
    )
    val state: StateFlow<AudioProbeUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            engine.deviceUpdates
                .catch { error -> _state.update { it.copy(error = error.message ?: "Falha ao monitorar dispositivos de áudio.") } }
                .collect { devices ->
                    _state.update { current ->
                        val input = retainOrChoose(current.selectedInputKey, devices, AudioDirection.INPUT)
                        val output = retainOrChoose(current.selectedOutputKey, devices, AudioDirection.OUTPUT)
                        current.copy(
                            devices = devices,
                            selectedInputKey = input,
                            selectedOutputKey = output,
                            eventLog = appendLog(current.eventLog, "Dispositivos de áudio atualizados (${devices.size})."),
                        )
                    }
                }
        }
    }

    fun onPermissionStateChanged(granted: Boolean) {
        _state.update { it.copy(permissionGranted = granted, error = null) }
    }

    fun selectInput(key: String?) {
        _state.update { it.copy(selectedInputKey = key, lastResult = null) }
    }

    fun selectOutput(key: String?) {
        _state.update { it.copy(selectedOutputKey = key, lastResult = null) }
    }

    fun refresh() {
        viewModelScope.launch {
            runCatching { engine.refreshDevices() }
                .onSuccess { devices ->
                    _state.update { current ->
                        current.copy(
                            devices = devices,
                            selectedInputKey = retainOrChoose(current.selectedInputKey, devices, AudioDirection.INPUT),
                            selectedOutputKey = retainOrChoose(current.selectedOutputKey, devices, AudioDirection.OUTPUT),
                            sessionHealth = healthStore.history(),
                            error = null,
                            eventLog = appendLog(current.eventLog, "Atualização manual concluída (${devices.size})."),
                        )
                    }
                }
                .onFailure { error -> _state.update { it.copy(error = error.message ?: "Falha ao atualizar os dispositivos de áudio.") } }
        }
    }

    fun runPlayback() = run(AudioProbeOperation.PLAYBACK)
    fun runRecord() = run(AudioProbeOperation.RECORD)
    fun runDuplex() = run(AudioProbeOperation.DUPLEX)

    fun stop() {
        if (_state.value.running == null) return
        engine.stopCurrentTest()
        _state.update { it.copy(stopping = true, eventLog = appendLog(it.eventLog, "Parada do teste solicitada.")) }
    }

    fun diagnosticsReport(): String {
        val snapshot = _state.value
        return buildString {
            appendLine("Aplicativo: GuitarLab Studio ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
            appendLine("Android: ${Build.VERSION.RELEASE} / API ${Build.VERSION.SDK_INT}")
            appendLine("Dispositivo: ${Build.MANUFACTURER} ${Build.MODEL}")
            appendLine("Rota do Studio — entrada: ${routingStore.selectedInputDiagnosticIdentity() ?: "automática"}")
            appendLine("Rota do Studio — saída principal: ${routingStore.selectedOutputDiagnosticIdentity() ?: "automática"}")
            appendLine("Rota do Studio — saída CUE: ${routingStore.selectedCueOutputDiagnosticIdentity() ?: "desativada"}")
            val inputSignature = routingStore.selectedInputSignature()
            val outputSignature = routingStore.selectedOutputSignature()
            listOf(44_100, 48_000, 88_200, 96_000).forEach { rate ->
                latencyStore.find(inputSignature, outputSignature, rate)?.let { calibration ->
                    appendLine(
                        "Calibração $rate Hz: latency=${calibration.latencyFrames}f jitter=${calibration.jitterFrames}f " +
                            "drift=${"%.1f".format(calibration.driftPpm)}ppm confidence=${"%.0f".format(calibration.confidence * 100f)}% " +
                            "attempts=${calibration.attempts} accepted=${calibration.accepted}"
                    )
                }
            }
            appendLine()
            append(
                AudioProbeReportFormatter.format(
                    devices = snapshot.devices,
                    selectedInputKey = snapshot.selectedInputKey,
                    selectedOutputKey = snapshot.selectedOutputKey,
                    result = snapshot.lastResult,
                )
            )
            appendLine()
            appendLine()
            appendLine("Recording Session Health (mais recente primeiro)")
            if (snapshot.sessionHealth.isEmpty()) {
                appendLine("- Nenhuma sessão de gravação registrada.")
            } else {
                snapshot.sessionHealth.asReversed().forEachIndexed { index, record ->
                    appendLine("- #${index + 1}: ${formatSessionHealth(record)}")
                }
            }
        }
    }

    private fun formatSessionHealth(record: RecordingSessionHealthRecord): String = buildString {
        append(record.healthClass.name)
        append(" | ${record.sampleRateHz} Hz")
        append(" | evidence=${record.timingEvidenceBasis}")
        append(" | delta=${record.sessionDeltaFrames}f")
        append(" | routeLatency=${record.acceptedRouteLatencyFrames}f")
        append(" | fine=${record.residualFineAdjustmentFrames}f")
        append(" | captured=${record.capturedFrames}f")
        append(" | in=${record.effectiveInputIdentity ?: record.selectedInputIdentity ?: "auto"}")
        append(" | out=${record.effectiveOutputIdentity ?: record.selectedOutputIdentity ?: "auto"}")
        if (record.outputFallback) append(" | outputFallback=true")
        if (record.routeChanged) append(" | routeChanged=true")
        if (record.inputZeroReadEvents > 0) append(" | zeroReads=${record.inputZeroReadEvents}")
        record.outputUnderrunCount?.takeIf { it > 0 }?.let { append(" | underruns=$it") }
        record.failureReason?.let { append(" | failure=$it") }
    }

    private fun run(operation: AudioProbeOperation) {
        if (_state.value.running != null) return
        if (operation != AudioProbeOperation.PLAYBACK && !_state.value.permissionGranted) {
            _state.update { it.copy(error = "A permissão para gravar áudio é necessária nos testes de entrada.") }
            return
        }

        viewModelScope.launch {
            val snapshot = _state.value
            _state.update {
                it.copy(
                    running = operation,
                    stopping = false,
                    lastResult = null,
                    error = null,
                    eventLog = appendLog(it.eventLog, "Teste de ${operation.label()} iniciado."),
                )
            }

            val result = runCatching {
                when (operation) {
                    AudioProbeOperation.PLAYBACK -> engine.runPlaybackTest(snapshot.selectedOutputKey)
                    AudioProbeOperation.RECORD -> engine.runRecordTest(snapshot.selectedInputKey)
                    AudioProbeOperation.DUPLEX -> engine.runDuplexTest(snapshot.selectedInputKey, snapshot.selectedOutputKey)
                }
            }

            result.onSuccess { probe ->
                _state.update {
                    it.copy(
                        running = null,
                        stopping = false,
                        lastResult = probe,
                        eventLog = appendLog(
                            it.eventLog,
                            "${operation.label().replaceFirstChar { ch -> ch.uppercase() }} ${when { probe.stopped -> "PARADO"; probe.success -> "APROVADO"; else -> "FALHA" }}: ${probe.message}",
                        ),
                    )
                }
            }.onFailure { error ->
                _state.update {
                    it.copy(
                        running = null,
                        stopping = false,
                        error = error.message ?: "O teste falhou.",
                        eventLog = appendLog(it.eventLog, "${operation.label()} FALHA: ${error.message ?: error::class.java.simpleName}"),
                    )
                }
            }
        }
    }

    override fun onCleared() {
        engine.close()
        super.onCleared()
    }

    private fun retainOrChoose(
        currentKey: String?,
        devices: List<AudioDeviceDescriptor>,
        direction: AudioDirection,
    ): String? {
        val eligible = devices.filter { it.supports(direction) }
        if (eligible.any { it.key == currentKey }) return currentKey
        return eligible.firstOrNull { it.transport == AudioTransport.USB }?.key ?: eligible.firstOrNull()?.key
    }

    private fun appendLog(current: List<String>, message: String): List<String> {
        val timestamp = DateFormat.getTimeInstance(DateFormat.MEDIUM).format(Date())
        return (current + "$timestamp  $message").takeLast(60)
    }

    private fun AudioProbeOperation.label(): String = when (this) {
        AudioProbeOperation.PLAYBACK -> "saída"
        AudioProbeOperation.RECORD -> "entrada"
        AudioProbeOperation.DUPLEX -> "duplex"
    }

    private companion object {
        fun hasRecordPermission(application: Application): Boolean =
            application.checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
    }
}
