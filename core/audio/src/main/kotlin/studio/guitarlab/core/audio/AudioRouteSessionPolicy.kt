package studio.guitarlab.core.audio

/**
 * Semantic audio-route lifecycle shared by orchestration. Durable identity must never be an
 * Android numeric device id: selected/effective identities are stable descriptors supplied by
 * the platform adapter.
 */
enum class AudioRouteSessionPhase {
    SELECTED_AVAILABLE,
    STARTING,
    ACTIVE_CONFIRMED,
    DEGRADED_LOST,
    FINALIZING_STOPPED,
    COMPATIBLE_REAPPEARED,
}

data class AudioRouteSessionState(
    val selectedInputIdentity: String?,
    val selectedOutputIdentity: String?,
    val effectiveInputIdentity: String? = null,
    val effectiveOutputIdentity: String? = null,
    val phase: AudioRouteSessionPhase = AudioRouteSessionPhase.SELECTED_AVAILABLE,
    val compensationInvalidated: Boolean = false,
    val invalidationCount: Int = 0,
    val lastReason: String? = null,
) {
    val canStart: Boolean get() = phase == AudioRouteSessionPhase.SELECTED_AVAILABLE || phase == AudioRouteSessionPhase.COMPATIBLE_REAPPEARED
}

object AudioRouteSessionPolicy {
    fun starting(selectedInputIdentity: String?, selectedOutputIdentity: String?): AudioRouteSessionState =
        starting(AudioRouteSessionState(selectedInputIdentity, selectedOutputIdentity))

    fun starting(state: AudioRouteSessionState): AudioRouteSessionState {
        require(state.canStart) { "Route session cannot start from ${state.phase}." }
        return state.copy(
            phase = AudioRouteSessionPhase.STARTING,
            effectiveInputIdentity = null,
            effectiveOutputIdentity = null,
            lastReason = null,
        )
    }

    fun confirmed(
        state: AudioRouteSessionState,
        effectiveInputIdentity: String?,
        effectiveOutputIdentity: String?,
    ): AudioRouteSessionState {
        require(state.phase == AudioRouteSessionPhase.STARTING) { "Only a starting route can be confirmed." }
        return state.copy(
            phase = AudioRouteSessionPhase.ACTIVE_CONFIRMED,
            effectiveInputIdentity = effectiveInputIdentity,
            effectiveOutputIdentity = effectiveOutputIdentity,
        )
    }

    fun updateEffectiveOutput(state: AudioRouteSessionState, effectiveOutputIdentity: String?): AudioRouteSessionState {
        require(state.phase == AudioRouteSessionPhase.ACTIVE_CONFIRMED) { "Only an active route can update effective output." }
        return state.copy(effectiveOutputIdentity = effectiveOutputIdentity)
    }

    /** Route loss invalidates route-specific timing exactly once for this session. */
    fun lost(state: AudioRouteSessionState, reason: String): AudioRouteSessionState {
        if (state.phase == AudioRouteSessionPhase.DEGRADED_LOST || state.phase == AudioRouteSessionPhase.FINALIZING_STOPPED) {
            return state.copy(lastReason = state.lastReason ?: reason)
        }
        return state.copy(
            phase = AudioRouteSessionPhase.DEGRADED_LOST,
            compensationInvalidated = true,
            invalidationCount = state.invalidationCount + if (state.compensationInvalidated) 0 else 1,
            lastReason = reason,
        )
    }

    fun stopped(state: AudioRouteSessionState): AudioRouteSessionState =
        state.copy(phase = AudioRouteSessionPhase.FINALIZING_STOPPED)

    /** A reconnect is available for a future explicit operation; recording never auto-resumes. */
    fun compatibleReappeared(
        state: AudioRouteSessionState,
        selectedInputIdentity: String? = state.selectedInputIdentity,
        selectedOutputIdentity: String? = state.selectedOutputIdentity,
    ): AudioRouteSessionState {
        require(state.phase == AudioRouteSessionPhase.DEGRADED_LOST || state.phase == AudioRouteSessionPhase.FINALIZING_STOPPED)
        return state.copy(
            selectedInputIdentity = selectedInputIdentity,
            selectedOutputIdentity = selectedOutputIdentity,
            effectiveInputIdentity = null,
            effectiveOutputIdentity = null,
            phase = AudioRouteSessionPhase.COMPATIBLE_REAPPEARED,
            lastReason = "compatible-route-reappeared",
        )
    }
}
