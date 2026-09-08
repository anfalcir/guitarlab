package studio.guitarlab.core.project

enum class TransportMode {
    STOPPED,
    PLAYING,
    RECORDING,
}

data class TransportState(
    val mode: TransportMode = TransportMode.STOPPED,
    val loopEnabled: Boolean = false,
)

object TransportPolicy {
    fun isActive(state: TransportState): Boolean = state.mode != TransportMode.STOPPED

    fun timelineEditingEnabled(state: TransportState): Boolean = state.mode == TransportMode.STOPPED

    fun togglePlayStop(state: TransportState): TransportState = when (state.mode) {
        TransportMode.STOPPED -> state.copy(mode = TransportMode.PLAYING)
        TransportMode.PLAYING, TransportMode.RECORDING -> state.copy(mode = TransportMode.STOPPED)
    }

    fun startRecording(state: TransportState): TransportState =
        if (state.mode == TransportMode.STOPPED) state.copy(mode = TransportMode.RECORDING) else state

    fun toggleLoop(state: TransportState): TransportState =
        if (timelineEditingEnabled(state)) state.copy(loopEnabled = !state.loopEnabled) else state

    fun returnToStartFrame(state: TransportState, currentFrame: Long): Long =
        if (timelineEditingEnabled(state)) 0L else currentFrame
}
