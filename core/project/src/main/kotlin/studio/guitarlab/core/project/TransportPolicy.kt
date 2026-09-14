package studio.guitarlab.core.project

enum class TransportMode { STOPPED, PLAYING, RECORDING }

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
        if (state.mode != TransportMode.RECORDING) 0L else currentFrame

    fun playStopEnabled(state: TransportState, engineReady: Boolean): Boolean =
        state.mode == TransportMode.PLAYING || (state.mode == TransportMode.STOPPED && engineReady)

    /**
     * Resolves the frame used when the user explicitly presses Play.
     *
     * This rule intentionally belongs to the playback transport policy instead of the shared
     * audio engine: recording may legitimately start before loopStartFrame to provide punch
     * pre-roll. When loop playback is enabled, however, a user Play action must never begin
     * outside [loopStartFrame, loopEndFrame).
     */
    fun playbackStartFrame(
        state: TransportState,
        playheadFrame: Long,
        projectEndFrame: Long,
        loopStartFrame: Long,
        loopEndFrame: Long,
    ): Long {
        val end = projectEndFrame.coerceAtLeast(0L)
        if (end == 0L) return 0L

        val playhead = playheadFrame.coerceIn(0L, end)
        val nonLoopStart = if (playhead >= end) 0L else playhead
        if (!state.loopEnabled) return nonLoopStart

        val loopStart = loopStartFrame.coerceIn(0L, end)
        val loopEnd = loopEndFrame.coerceIn(loopStart, end)
        if (loopEnd <= loopStart) return nonLoopStart

        return if (playhead >= loopStart && playhead < loopEnd) playhead else loopStart
    }

    /** Keeps the visible playback cursor inside an active loop even if an engine callback lands
     * exactly on, or momentarily beyond, a loop boundary. This applies only to PLAYING state. */
    fun playbackPositionFrame(
        state: TransportState,
        reportedFrame: Long,
        projectEndFrame: Long,
        loopStartFrame: Long,
        loopEndFrame: Long,
    ): Long {
        val end = projectEndFrame.coerceAtLeast(0L)
        val reported = reportedFrame.coerceIn(0L, end)
        if (state.mode != TransportMode.PLAYING || !state.loopEnabled) return reported

        val loopStart = loopStartFrame.coerceIn(0L, end)
        val loopEnd = loopEndFrame.coerceIn(loopStart, end)
        if (loopEnd <= loopStart) return reported
        return if (reported >= loopStart && reported < loopEnd) reported else loopStart
    }
}
