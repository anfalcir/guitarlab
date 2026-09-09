package studio.guitarlab.core.project

import studio.guitarlab.core.model.GuitarProject

/**
 * Deterministic state machine for one Studio recording pass.
 *
 * M5 records one physical input into exactly one armed track. Multi-track capture is intentionally
 * rejected until the product has explicit per-track input routing; duplicating one input silently
 * into several armed tracks would misrepresent what was actually captured.
 */
enum class RecordingSessionPhase {
    IDLE,
    COUNTDOWN,
    CAPTURING,
    FINALIZING,
}

data class RecordingSessionState(
    val phase: RecordingSessionPhase = RecordingSessionPhase.IDLE,
    val targetTrackId: String? = null,
    val timelineStartFrame: Long = 0L,
    val countdownSecondsRemaining: Int = 0,
    val framesCaptured: Long = 0L,
) {
    val active: Boolean get() = phase != RecordingSessionPhase.IDLE
    val readyToOpenCapture: Boolean get() = phase == RecordingSessionPhase.COUNTDOWN && countdownSecondsRemaining == 0
}

sealed interface RecordingStartDecision {
    data class Allowed(val targetTrackId: String, val timelineStartFrame: Long) : RecordingStartDecision
    data class Rejected(val message: String) : RecordingStartDecision
}

object RecordingSessionPolicy {
    fun validateStart(project: GuitarProject, timelineStartFrame: Long): RecordingStartDecision {
        if (timelineStartFrame < 0L) return RecordingStartDecision.Rejected("A posição inicial da gravação é inválida.")
        val armed = project.tracks.filter { it.armed }
        return when (armed.size) {
            0 -> RecordingStartDecision.Rejected("Arme uma pista antes de gravar.")
            1 -> RecordingStartDecision.Allowed(armed.single().id, timelineStartFrame)
            else -> RecordingStartDecision.Rejected(
                "A M5 grava uma entrada por vez. Deixe apenas uma pista armada para iniciar a gravação."
            )
        }
    }

    fun begin(project: GuitarProject, timelineStartFrame: Long): RecordingSessionState =
        when (val decision = validateStart(project, timelineStartFrame)) {
            is RecordingStartDecision.Rejected -> error(decision.message)
            is RecordingStartDecision.Allowed -> RecordingSessionState(
                phase = RecordingSessionPhase.COUNTDOWN,
                targetTrackId = decision.targetTrackId,
                timelineStartFrame = decision.timelineStartFrame,
                countdownSecondsRemaining = RecordingStartPolicy.START_DELAY_SECONDS,
            )
        }

    /**
     * Advances exactly one displayed second. The transition reaches zero but remains COUNTDOWN;
     * the Android coordinator must revalidate permission/routing and explicitly mark capture started.
     */
    fun tickCountdown(state: RecordingSessionState): RecordingSessionState {
        require(state.phase == RecordingSessionPhase.COUNTDOWN) { "A contagem só pode avançar durante o countdown." }
        if (state.countdownSecondsRemaining == 0) return state
        return state.copy(countdownSecondsRemaining = state.countdownSecondsRemaining - 1)
    }

    fun markCaptureStarted(state: RecordingSessionState): RecordingSessionState {
        require(state.readyToOpenCapture) { "A captura só pode iniciar depois do countdown completo." }
        return state.copy(phase = RecordingSessionPhase.CAPTURING, framesCaptured = 0L)
    }

    fun updateCapturedFrames(state: RecordingSessionState, framesCaptured: Long): RecordingSessionState {
        require(state.phase == RecordingSessionPhase.CAPTURING) { "Frames só podem avançar durante a captura." }
        require(framesCaptured >= state.framesCaptured) { "A contagem de frames da gravação não pode retroceder." }
        return state.copy(framesCaptured = framesCaptured)
    }

    fun beginFinalizing(state: RecordingSessionState): RecordingSessionState {
        require(state.phase == RecordingSessionPhase.CAPTURING) { "Só uma captura ativa pode ser finalizada." }
        return state.copy(phase = RecordingSessionPhase.FINALIZING)
    }

    fun cancelCountdown(state: RecordingSessionState): RecordingSessionState {
        require(state.phase == RecordingSessionPhase.COUNTDOWN) { "Só o countdown pode ser cancelado antes da captura." }
        return RecordingSessionState()
    }

    fun reset(): RecordingSessionState = RecordingSessionState()
}
