package studio.guitarlab.app.ui

import android.app.Application
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runInterruptible
import studio.guitarlab.app.diagnostics.DiagnosticJournal
import studio.guitarlab.core.audio.CueRouteCapabilityState
import studio.guitarlab.platform.audio.android.AndroidCueRouteVerifier
import studio.guitarlab.platform.audio.android.CuePreflightResult
import studio.guitarlab.platform.audio.android.CuePreflightStatus
import studio.guitarlab.platform.audio.android.CueOutputStrategy
import studio.guitarlab.platform.audio.android.CommunicationSplitPhaseProbe
import studio.guitarlab.platform.audio.android.CommunicationSplitProbeAcousticOutcome
import studio.guitarlab.platform.audio.android.CommunicationSplitProbePhase

data class CueRouteControllerState(
    val capability: CueRouteCapabilityState = CueRouteCapabilityState.NOT_CONFIGURED,
    val message: String? = null,
    val selectedCueSignature: String? = null,
    val candidateCueSignature: String? = null,
    val canRetry: Boolean = false,
    val communicationProbeRunning: Boolean = false,
    val communicationProbeMessage: String? = null,
    val communicationProbeAwaitingFeedback: Boolean = false,
) {
    val verifying: Boolean get() = capability == CueRouteCapabilityState.VERIFYING
}

/**
 * Lifecycle owner for CUE admission. Results are cached only in this process and invalidated by
 * topology/selection changes. Unsupported candidates are never persisted as active CUE routes.
 */
class CueRouteController(application: Application) : AndroidViewModel(application) {
    private data class CacheKey(val mainSignature: String, val cueSignature: String, val sampleRateHz: Int)

    private val routing = StudioAudioRoutingStore(application)
    private val audioManager = application.getSystemService(AudioManager::class.java)
    private val journal = DiagnosticJournal(application)
    private val generation = AtomicLong()
    private val sessionCache = mutableMapOf<CacheKey, CuePreflightResult>()
    private var verificationJob: Job? = null
    private var communicationProbeJob: Job? = null
    private var lastRequest: CacheKey? = null
    private var lastProjectId: String? = null
    private val _state = MutableStateFlow(CueRouteControllerState())
    val state: StateFlow<CueRouteControllerState> = _state.asStateFlow()

    private val deviceCallback = object : AudioDeviceCallback() {
        override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>) = topologyChanged()
        override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>) = topologyChanged()
    }

    init {
        // A previous process cannot carry route proof into this process. Playback also performs
        // its own admission, but Settings must never present a stale selection as already proven.
        routing.selectCueOutput(null)
        audioManager.registerAudioDeviceCallback(deviceCallback, Handler(Looper.getMainLooper()))
    }

    fun validate(
        mainSignature: String,
        cueSignature: String,
        sampleRateHz: Int,
        projectId: String?,
        force: Boolean = false,
    ) {
        require(sampleRateHz > 0)
        val key = CacheKey(mainSignature, cueSignature, sampleRateHz)
        lastRequest = key
        lastProjectId = projectId
        val ticket = generation.incrementAndGet()
        verificationJob?.cancel()
        communicationProbeJob?.cancel()
        routing.selectCueOutput(null)
        _state.value = CueRouteControllerState(
            capability = CueRouteCapabilityState.VERIFYING,
            message = "Verificando MAIN + CUE…",
            candidateCueSignature = cueSignature,
        )
        journal.append(
            eventType = "audio.cue_preflight",
            projectId = projectId,
            state = "STARTED",
            summary = "Verificação silenciosa MAIN/CUE iniciada.",
            technicalDetail = "sampleRateHz=$sampleRateHz",
        )

        val cached = if (force) null else synchronized(sessionCache) { sessionCache[key] }
        if (cached != null) {
            applyResult(ticket, key, cached, fromCache = true)
            return
        }

        verificationJob = viewModelScope.launch {
            val result = try {
                runInterruptible(Dispatchers.IO) {
                    val main = routing.resolveSelectedOutputDevice()
                    val cue = routing.resolveCandidateCueOutputDevice(cueSignature)
                    if (main == null || cue == null) null
                    else AndroidCueRouteVerifier.verifyDevices(audioManager, main, cue, sampleRateHz) {
                        generation.get() == ticket && !Thread.currentThread().isInterrupted
                    }
                }
            } catch (_: CancellationException) {
                return@launch
            }
            if (generation.get() != ticket) return@launch
            if (result == null) {
                applyUnavailable(ticket, key)
            } else {
                synchronized(sessionCache) { sessionCache[key] = result }
                applyResult(ticket, key, result, fromCache = false)
            }
        }
    }

    fun retry() {
        val request = lastRequest ?: return
        synchronized(sessionCache) { sessionCache.remove(request) }
        validate(
            mainSignature = request.mainSignature,
            cueSignature = request.cueSignature,
            sampleRateHz = request.sampleRateHz,
            projectId = lastProjectId,
            force = true,
        )
    }

    fun runCommunicationProbe() {
        val request = lastRequest
        if (request == null || _state.value.capability != CueRouteCapabilityState.SUPPORTED) {
            _state.value = _state.value.copy(
                communicationProbeMessage = "Valide primeiro uma combinação MAIN + CUE.",
                communicationProbeAwaitingFeedback = false,
            )
            return
        }
        val ticket = generation.get()
        communicationProbeJob?.cancel()
        _state.value = _state.value.copy(
            communicationProbeRunning = true,
            communicationProbeMessage = "Iniciando diagnóstico audível A/B/C/D…",
            communicationProbeAwaitingFeedback = false,
        )
        journal.append(
            eventType = "audio.cue_communication_probe",
            projectId = lastProjectId,
            state = "STARTED",
            summary = "Diagnóstico audível MAIN/CUE A/B/C/D iniciado.",
            technicalDetail = "sampleRateHz=${request.sampleRateHz}",
        )
        communicationProbeJob = viewModelScope.launch {
            val result = try {
                runInterruptible(Dispatchers.IO) {
                    val main = routing.resolveSelectedOutputDevice()
                    val cue = routing.resolveSelectedCueOutputDevice()
                    if (main == null || cue == null) null
                    else {
                        val profile = AndroidCueRouteVerifier.negotiatedProfile(main, cue, request.sampleRateHz)
                        if (profile?.strategy != CueOutputStrategy.COMMUNICATION_SPLIT) {
                            error("A combinação atual não usa Communication Split.")
                        }
                        CommunicationSplitPhaseProbe.run(
                            audioManager = audioManager,
                            main = main,
                            cue = cue,
                            profile = profile,
                            keepRunning = {
                                generation.get() == ticket && !Thread.currentThread().isInterrupted
                            },
                            onPhase = { phase ->
                                if (generation.get() == ticket) {
                                    _state.value = _state.value.copy(
                                        communicationProbeMessage = communicationProbePhaseMessage(phase),
                                    )
                                }
                            },
                        )
                    }
                }
            } catch (_: CancellationException) {
                return@launch
            } catch (error: Throwable) {
                if (generation.get() == ticket) {
                    _state.value = _state.value.copy(
                        communicationProbeRunning = false,
                        communicationProbeMessage = error.message ?: "Não foi possível executar o diagnóstico.",
                        communicationProbeAwaitingFeedback = false,
                    )
                }
                return@launch
            }
            if (generation.get() != ticket) return@launch
            if (result == null) {
                _state.value = _state.value.copy(
                    communicationProbeRunning = false,
                    communicationProbeMessage = "As saídas selecionadas não estão mais disponíveis.",
                    communicationProbeAwaitingFeedback = false,
                )
                return@launch
            }
            journal.append(
                eventType = "audio.cue_communication_probe",
                projectId = lastProjectId,
                state = if (result.completed) "COMPLETED" else "FAILED",
                summary = if (result.completed) {
                    "Diagnóstico audível A/B/C/D concluído."
                } else {
                    "Diagnóstico audível A/B/C/D interrompido."
                },
                technicalDetail = result.diagnosticSummary(),
            )
            _state.value = _state.value.copy(
                communicationProbeRunning = false,
                communicationProbeMessage = if (result.completed) {
                    "Teste concluído. Informe abaixo em qual etapa o tom da MAIN deixou de ser ouvido."
                } else {
                    "Teste interrompido: ${result.error ?: "causa não identificada"}."
                },
                communicationProbeAwaitingFeedback = result.completed,
            )
        }
    }

    fun recordCommunicationProbeOutcome(outcome: CommunicationSplitProbeAcousticOutcome) {
        CommunicationSplitPhaseProbe.recordAcousticOutcome(outcome)
        val message = when (outcome) {
            CommunicationSplitProbeAcousticOutcome.MAIN_NOT_AUDIBLE_IN_A ->
                "Registrado: MAIN já não estava audível na etapa A."
            CommunicationSplitProbeAcousticOutcome.MAIN_LOST_IN_B ->
                "Registrado: MAIN sumiu ao selecionar o dispositivo de comunicação (B)."
            CommunicationSplitProbeAcousticOutcome.MAIN_LOST_IN_C ->
                "Registrado: MAIN sumiu quando o AudioTrack CUE entrou ativo com silêncio (C)."
            CommunicationSplitProbeAcousticOutcome.MAIN_LOST_IN_D ->
                "Registrado: MAIN sumiu somente quando o CUE começou a tocar sinal (D)."
            CommunicationSplitProbeAcousticOutcome.MAIN_AUDIBLE_ALL_PHASES ->
                "Registrado: MAIN permaneceu audível em todas as etapas."
            CommunicationSplitProbeAcousticOutcome.UNABLE_TO_TELL ->
                "Registrado: não foi possível determinar acusticamente a etapa."
        }
        journal.append(
            eventType = "audio.cue_communication_probe",
            projectId = lastProjectId,
            state = "ACOUSTIC_OUTCOME",
            summary = message,
            technicalDetail = CommunicationSplitPhaseProbe.lastResult()?.diagnosticSummary(),
        )
        _state.value = _state.value.copy(
            communicationProbeMessage = "$message Exporte o diagnóstico para análise.",
            communicationProbeAwaitingFeedback = false,
        )
    }

    private fun communicationProbePhaseMessage(phase: CommunicationSplitProbePhase): String = when (phase) {
        CommunicationSplitProbePhase.A_MAIN_ONLY ->
            "A/4 · MAIN apenas: deve tocar um tom grave na saída principal."
        CommunicationSplitProbePhase.B_COMMUNICATION_DEVICE_SELECTED ->
            "B/4 · CUE preparado, ainda sem AudioTrack CUE: o tom MAIN deve continuar."
        CommunicationSplitProbePhase.C_CUE_TRACK_SILENT ->
            "C/4 · AudioTrack CUE ativo com silêncio: o tom MAIN deve continuar."
        CommunicationSplitProbePhase.D_CUE_TONE_ACTIVE ->
            "D/4 · MAIN grave + CUE agudo devem tocar simultaneamente em saídas diferentes."
    }

    fun disable() {
        generation.incrementAndGet()
        verificationJob?.cancel()
        communicationProbeJob?.cancel()
        routing.selectCueOutput(null)
        lastRequest = null
        _state.value = CueRouteControllerState()
    }

    fun onMainSelectionChanged() {
        generation.incrementAndGet()
        verificationJob?.cancel()
        communicationProbeJob?.cancel()
        synchronized(sessionCache) { sessionCache.clear() }
        AndroidCueRouteVerifier.invalidateNegotiatedProfiles()
        routing.selectCueOutput(null)
        lastRequest = null
        _state.value = CueRouteControllerState(
            capability = CueRouteCapabilityState.NOT_CONFIGURED,
            message = "Selecione novamente a saída CUE para validar a nova combinação.",
        )
    }

    fun invalidateForManualRefresh() = topologyChanged()

    override fun onCleared() {
        generation.incrementAndGet()
        verificationJob?.cancel()
        communicationProbeJob?.cancel()
        runCatching { audioManager.unregisterAudioDeviceCallback(deviceCallback) }
        super.onCleared()
    }

    private fun topologyChanged() {
        generation.incrementAndGet()
        verificationJob?.cancel()
        communicationProbeJob?.cancel()
        synchronized(sessionCache) { sessionCache.clear() }
        AndroidCueRouteVerifier.invalidateNegotiatedProfiles()
        val wasSupported = _state.value.capability == CueRouteCapabilityState.SUPPORTED
        routing.selectCueOutput(null)
        _state.value = CueRouteControllerState(
            capability = if (wasSupported) CueRouteCapabilityState.LOST else CueRouteCapabilityState.CANDIDATE_AVAILABLE,
            message = if (wasSupported) {
                "A topologia de áudio mudou; CUE foi desativado por segurança. Selecione e teste novamente."
            } else {
                "Dispositivos de áudio alterados; selecione a combinação MAIN/CUE novamente."
            },
        )
    }

    private fun applyUnavailable(ticket: Long, key: CacheKey) {
        if (generation.get() != ticket) return
        val message = "Uma das saídas selecionadas não está mais disponível."
        _state.value = CueRouteControllerState(
            capability = CueRouteCapabilityState.MISSING_EFFECTIVE_ROUTE,
            message = message,
            candidateCueSignature = key.cueSignature,
            canRetry = true,
        )
        journal.append(
            eventType = "audio.cue_preflight",
            projectId = lastProjectId,
            state = "MISSING_EFFECTIVE_ROUTE",
            summary = message,
        )
    }

    private fun applyResult(ticket: Long, key: CacheKey, result: CuePreflightResult, fromCache: Boolean) {
        if (generation.get() != ticket) return
        val stillCurrent = routing.selectedOutputSignature() == key.mainSignature &&
            routing.cueOutputChoices().any { it.signature == key.cueSignature }
        val supported = result.supported && stillCurrent
        if (supported) routing.selectCueOutput(key.cueSignature) else routing.selectCueOutput(null)
        val capability = if (!stillCurrent) {
            CueRouteCapabilityState.LOST
        } else {
            result.status.toCapabilityState()
        }
        val resultMessage = if (result.status == CuePreflightStatus.CONVERGED_TO_MAIN) {
            val mainLabel = routing.outputChoices()
                .firstOrNull { it.signature == key.mainSignature }
                ?.label
                ?: "a saída principal"
            "Não suportado nesta combinação: o Android redirecionou CUE para $mainLabel."
        } else {
            result.userMessage
        }
        val message = when {
            !stillCurrent -> "A combinação mudou durante a verificação; CUE permaneceu desativado."
            fromCache -> "Resultado desta sessão: $resultMessage"
            else -> resultMessage
        }
        _state.value = CueRouteControllerState(
            capability = capability,
            message = message,
            selectedCueSignature = key.cueSignature.takeIf { supported },
            candidateCueSignature = key.cueSignature,
            canRetry = !supported,
        )
        journal.append(
            eventType = "audio.cue_preflight",
            projectId = lastProjectId,
            state = capability.name,
            summary = message,
            technicalDetail = result.diagnosticSummary(),
        )
    }

    private fun CuePreflightStatus.toCapabilityState(): CueRouteCapabilityState = when (this) {
        CuePreflightStatus.SUPPORTED -> CueRouteCapabilityState.SUPPORTED
        CuePreflightStatus.CONVERGED_TO_MAIN -> CueRouteCapabilityState.CONVERGED_TO_MAIN
        CuePreflightStatus.MISSING_EFFECTIVE_ROUTE -> CueRouteCapabilityState.MISSING_EFFECTIVE_ROUTE
        CuePreflightStatus.WRONG_OR_MIRRORED_ROUTE -> CueRouteCapabilityState.WRONG_OR_MIRRORED_ROUTE
        CuePreflightStatus.CLOCK_UNSTABLE -> CueRouteCapabilityState.CLOCK_UNAVAILABLE
        CuePreflightStatus.OFFSET_EXCEEDED -> CueRouteCapabilityState.OFFSET_EXCEEDED
        CuePreflightStatus.ROUTE_CHANGED -> CueRouteCapabilityState.LOST
        else -> CueRouteCapabilityState.FAILED
    }
}
