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

    /**
     * User playback treats an active loop as a bounded play range: L▶ is the automatic end of
     * that Play pass. Recording is intentionally not governed by this policy; punch pre/post-roll
     * keeps using the recording-specific engine request.
     */
    fun playbackEndFrame(
        state: TransportState,
        projectEndFrame: Long,
        loopStartFrame: Long,
        loopEndFrame: Long,
    ): Long {
        val end = projectEndFrame.coerceAtLeast(0L)
        if (!state.loopEnabled || end == 0L) return end
        val loopStart = loopStartFrame.coerceIn(0L, end)
        val loopEnd = loopEndFrame.coerceIn(loopStart, end)
        return if (loopEnd > loopStart) loopEnd else end
    }

    /** Frame where the playhead must rest after automatic playback completion. */
    fun automaticPlaybackResetFrame(
        state: TransportState,
        projectEndFrame: Long,
        loopStartFrame: Long,
        loopEndFrame: Long,
    ): Long {
        val end = projectEndFrame.coerceAtLeast(0L)
        if (!state.loopEnabled || end == 0L) return 0L
        val loopStart = loopStartFrame.coerceIn(0L, end)
        val loopEnd = loopEndFrame.coerceIn(loopStart, end)
        return if (loopEnd > loopStart) loopStart else 0L
    }

    /**
     * Normalizes an interactive seek requested while Play is running. In an active loop range the
     * user may seek anywhere from L◀ up to the last frame before L▶, but never outside it.
     */
    fun playbackSeekFrame(
        state: TransportState,
        requestedFrame: Long,
        projectEndFrame: Long,
        loopStartFrame: Long,
        loopEndFrame: Long,
    ): Long {
        val end = projectEndFrame.coerceAtLeast(0L)
        if (end == 0L) return 0L
        val requested = requestedFrame.coerceIn(0L, end)
        if (state.mode != TransportMode.PLAYING || !state.loopEnabled) return requested

        val loopStart = loopStartFrame.coerceIn(0L, end)
        val loopEnd = loopEndFrame.coerceIn(loopStart, end)
        if (loopEnd <= loopStart) return requested
        val lastPlayableFrame = (loopEnd - 1L).coerceAtLeast(loopStart)
        return requested.coerceIn(loopStart, lastPlayableFrame)
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
