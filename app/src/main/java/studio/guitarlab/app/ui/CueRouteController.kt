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

data class CueRouteControllerState(
    val capability: CueRouteCapabilityState = CueRouteCapabilityState.NOT_CONFIGURED,
    val message: String? = null,
    val selectedCueSignature: String? = null,
    val candidateCueSignature: String? = null,
    val canRetry: Boolean = false,
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
                    else AndroidCueRouteVerifier.verifyDevices(main, cue, sampleRateHz) {
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

    fun disable() {
        generation.incrementAndGet()
        verificationJob?.cancel()
        routing.selectCueOutput(null)
        lastRequest = null
        _state.value = CueRouteControllerState()
    }

    fun onMainSelectionChanged() {
        generation.incrementAndGet()
        verificationJob?.cancel()
        synchronized(sessionCache) { sessionCache.clear() }
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
        runCatching { audioManager.unregisterAudioDeviceCallback(deviceCallback) }
        super.onCleared()
    }

    private fun topologyChanged() {
        generation.incrementAndGet()
        verificationJob?.cancel()
        synchronized(sessionCache) { sessionCache.clear() }
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
