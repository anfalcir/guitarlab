package studio.guitarlab.app.ui

import android.app.Application
import android.media.AudioManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.runInterruptible
import studio.guitarlab.app.diagnostics.DiagnosticJournal
import studio.guitarlab.platform.audio.android.AndroidCueRouteVerifier
import studio.guitarlab.platform.audio.android.CommunicationSplitLegacyScenario
import studio.guitarlab.platform.audio.android.CommunicationSplitLegacySequenceProbe
import studio.guitarlab.platform.audio.android.CommunicationSplitProbePairOutcome
import studio.guitarlab.platform.audio.android.CueOutputStrategy

data class CueLegacySequenceProbeUiState(
    val running: Boolean = false,
    val runningScenario: CommunicationSplitLegacyScenario? = null,
    val awaitingFeedback: CommunicationSplitLegacyScenario? = null,
    val gOutcome: CommunicationSplitProbePairOutcome? = null,
    val hOutcome: CommunicationSplitProbePairOutcome? = null,
    val pairKey: String? = null,
    val message: String? = null,
) {
    val nextScenario: CommunicationSplitLegacyScenario?
        get() = when {
            running || awaitingFeedback != null -> null
            gOutcome == null -> CommunicationSplitLegacyScenario.G_COMMUNICATION_BEFORE_OPEN
            hOutcome == null -> CommunicationSplitLegacyScenario.H_MEDIA_PRECONDITION_THEN_COMMUNICATION
            else -> null
        }
    val complete: Boolean get() = gOutcome != null && hOutcome != null
}

class CueLegacySequenceProbeViewModel(application: Application) : AndroidViewModel(application) {
    private val routing = StudioAudioRoutingStore(application)
    private val audioManager = application.getSystemService(AudioManager::class.java)
    private val journal = DiagnosticJournal(application)
    private val _state = MutableStateFlow(CueLegacySequenceProbeUiState())
    val state: StateFlow<CueLegacySequenceProbeUiState> = _state.asStateFlow()
    private var job: Job? = null

    fun runNext(sampleRateHz: Int, projectId: String?) {
        val scenario = _state.value.nextScenario ?: return
        if (job?.isActive == true) return
        val main = routing.resolveSelectedOutputDevice()
        val cue = routing.resolveSelectedCueOutputDevice()
        if (main == null || cue == null) {
            _state.update { it.copy(message = "Selecione e valide MAIN + CUE antes do G/H.") }
            return
        }
        val currentPairKey = "${main.id}:${cue.id}:$sampleRateHz"
        if (
            scenario == CommunicationSplitLegacyScenario.H_MEDIA_PRECONDITION_THEN_COMMUNICATION &&
            _state.value.pairKey != currentPairKey
        ) {
            _state.value = CueLegacySequenceProbeUiState(
                message = "A combinação MAIN/CUE mudou desde G. Execute G novamente antes de H.",
            )
            return
        }
        val profile = AndroidCueRouteVerifier.negotiatedProfile(main, cue, sampleRateHz)
        if (profile?.strategy != CueOutputStrategy.COMMUNICATION_SPLIT) {
            _state.update { it.copy(message = "G/H exige uma combinação Communication Split validada.") }
            return
        }

        job = viewModelScope.launch {
            _state.update {
                it.copy(
                    running = true,
                    runningScenario = scenario,
                    awaitingFeedback = null,
                    message = "Preparando ${scenario.name.substringBefore('_')}…",
                )
            }
            journal.append(
                eventType = "audio.cue_legacy_sequence_probe",
                projectId = projectId,
                state = "STARTED",
                summary = "Probe de ordem histórica ${scenario.name} iniciado.",
                technicalDetail = "sampleRateHz=$sampleRateHz",
            )
            val evidence = try {
                runInterruptible(Dispatchers.IO) {
                    CommunicationSplitLegacySequenceProbe.runScenario(
                        audioManager = audioManager,
                        main = main,
                        cue = cue,
                        profile = profile,
                        scenario = scenario,
                        keepRunning = { !Thread.currentThread().isInterrupted },
                        onMessage = { message -> _state.update { it.copy(message = message) } },
                    )
                }
            } catch (_: CancellationException) {
                return@launch
            } catch (error: Throwable) {
                _state.update {
                    it.copy(
                        running = false,
                        runningScenario = null,
                        message = error.message ?: "Falha no probe G/H.",
                    )
                }
                return@launch
            }

            journal.append(
                eventType = "audio.cue_legacy_sequence_probe",
                projectId = projectId,
                state = if (evidence.completed) "COMPLETED" else "FAILED",
                summary = "Probe ${scenario.name} ${if (evidence.completed) "concluído" else "falhou"}.",
                technicalDetail = CommunicationSplitLegacySequenceProbe.lastResult()?.diagnosticSummary(),
            )
            _state.update {
                it.copy(
                    running = false,
                    runningScenario = null,
                    awaitingFeedback = scenario.takeIf { evidence.completed },
                    pairKey = currentPairKey.takeIf { evidence.completed } ?: it.pairKey,
                    message = if (evidence.completed) {
                        "Etapa ${scenario.name.substringBefore('_')} concluída. Informe o que ficou audível."
                    } else {
                        "Etapa ${scenario.name.substringBefore('_')} falhou: ${evidence.error ?: "causa não identificada"}."
                    },
                )
            }
        }
    }

    fun recordOutcome(
        scenario: CommunicationSplitLegacyScenario,
        outcome: CommunicationSplitProbePairOutcome,
        projectId: String?,
    ) {
        if (_state.value.awaitingFeedback != scenario) return
        CommunicationSplitLegacySequenceProbe.recordPairOutcome(scenario, outcome)
        journal.append(
            eventType = "audio.cue_legacy_sequence_probe",
            projectId = projectId,
            state = "ACOUSTIC_${scenario.name.substringBefore('_')}",
            summary = "Observação ${scenario.name.substringBefore('_')}: ${outcome.name}.",
            technicalDetail = CommunicationSplitLegacySequenceProbe.lastResult()?.diagnosticSummary(),
        )
        _state.update {
            when (scenario) {
                CommunicationSplitLegacyScenario.G_COMMUNICATION_BEFORE_OPEN ->
                    it.copy(
                        awaitingFeedback = null,
                        gOutcome = outcome,
                        message = "G registrado. Execute H para testar o precondicionamento dual-MEDIA do rc31.",
                    )
                CommunicationSplitLegacyScenario.H_MEDIA_PRECONDITION_THEN_COMMUNICATION ->
                    it.copy(
                        awaitingFeedback = null,
                        hOutcome = outcome,
                        message = "G/H concluído. Exporte o diagnóstico.",
                    )
            }
        }
    }

    fun restart() {
        job?.cancel()
        _state.value = CueLegacySequenceProbeUiState()
    }

    override fun onCleared() {
        job?.cancel()
        super.onCleared()
    }
}
