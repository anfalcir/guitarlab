package studio.guitarlab.core.project

import kotlin.math.roundToLong
import studio.guitarlab.core.model.GuitarProject

data class TimelineControlState(
    val playheadFrame: Long = 0,
    val loopStartFrame: Long = 0,
    val loopEndFrame: Long = 0,
)

object TimelineControlPolicy {
    fun projectEndFrame(project: GuitarProject): Long =
        ActiveTakePolicy.audibleClips(project).maxOfOrNull { it.startFrame + it.lengthFrames }?.coerceAtLeast(1L) ?: 1L

    fun clampFrame(frame: Long, projectEndFrame: Long): Long =
        frame.coerceIn(0L, projectEndFrame.coerceAtLeast(1L))

    fun movePlayhead(state: TimelineControlState, frame: Long, projectEndFrame: Long): TimelineControlState =
        state.copy(playheadFrame = clampFrame(frame, projectEndFrame))

    fun moveLoopStart(state: TimelineControlState, frame: Long, projectEndFrame: Long): TimelineControlState {
        val end = state.loopEndFrame.coerceIn(1L, projectEndFrame.coerceAtLeast(1L))
        return state.copy(loopStartFrame = clampFrame(frame, projectEndFrame).coerceAtMost(end - 1L))
    }

    fun moveLoopEnd(state: TimelineControlState, frame: Long, projectEndFrame: Long): TimelineControlState {
        val start = state.loopStartFrame.coerceIn(0L, (projectEndFrame - 1L).coerceAtLeast(0L))
        return state.copy(loopEndFrame = clampFrame(frame, projectEndFrame).coerceAtLeast(start + 1L))
    }

    fun normalizedForProject(state: TimelineControlState, projectEndFrame: Long): TimelineControlState {
        val end = projectEndFrame.coerceAtLeast(1L)
        val playhead = clampFrame(state.playheadFrame, end)
        val loopStart = clampFrame(state.loopStartFrame, end).coerceAtMost(end - 1L)
        val requestedEnd = if (state.loopEndFrame <= 0L) end else state.loopEndFrame
        val loopEnd = clampFrame(requestedEnd, end).coerceAtLeast(loopStart + 1L)
        return TimelineControlState(playhead, loopStart, loopEnd)
    }

    fun frameToFraction(frame: Long, projectEndFrame: Long): Float {
        val end = projectEndFrame.coerceAtLeast(1L)
        return clampFrame(frame, end).toDouble().div(end.toDouble()).toFloat()
    }

    fun fractionToFrame(fraction: Float, projectEndFrame: Long): Long {
        val end = projectEndFrame.coerceAtLeast(1L)
        return (fraction.coerceIn(0f, 1f) * end.toDouble()).roundToLong().coerceIn(0L, end)
    }
}
