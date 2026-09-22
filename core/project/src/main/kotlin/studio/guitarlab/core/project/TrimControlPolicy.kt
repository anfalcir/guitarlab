package studio.guitarlab.core.project

import kotlin.math.max
import studio.guitarlab.core.model.AudioClip

data class TrimControlState(
    val clipId: String,
    val startFrame: Long,
    val endFrame: Long,
)

object TrimControlPolicy {
    /** Opens a fresh trim draft around the useful middle of the visible clip (35%..65%). */
    fun fromClip(clip: AudioClip): TrimControlState {
        require(clip.lengthFrames > 0L) { "Trim requires a non-empty clip." }
        val visibleStart = clip.startFrame
        val visibleEnd = clip.startFrame + clip.lengthFrames
        val start = visibleStart + (clip.lengthFrames * 35L / 100L)
        val end = visibleStart + (clip.lengthFrames * 65L / 100L)
        return TrimControlState(
            clipId = clip.id,
            startFrame = start.coerceIn(minimumStartFrame(clip), visibleEnd - 1L),
            endFrame = end.coerceIn((start + 1L).coerceAtMost(visibleEnd), maximumEndFrame(clip)),
        )
    }

    fun minimumStartFrame(clip: AudioClip): Long = max(0L, clip.startFrame - clip.sourceStartFrame)

    fun maximumEndFrame(clip: AudioClip): Long {
        val sourceTotal = clip.sourceTotalFrames ?: (clip.sourceStartFrame + clip.lengthFrames)
        val remainingFromCurrentSourceStart = (sourceTotal - clip.sourceStartFrame).coerceAtLeast(clip.lengthFrames)
        return clip.startFrame + remainingFromCurrentSourceStart
    }

    /** Maps a horizontal pointer position to the visible timeline domain of a clip. */
    fun visibleFrameAtPointerX(clip: AudioClip, pointerXPx: Float, widthPx: Float): Long {
        require(clip.lengthFrames > 0L) { "Trim requires a non-empty clip." }
        require(widthPx > 0f && widthPx.isFinite()) { "Trim width must be finite and positive." }
        val fraction = (pointerXPx / widthPx).coerceIn(0f, 1f)
        return clip.startFrame + (clip.lengthFrames.toDouble() * fraction.toDouble()).toLong()
    }

    /** Converts a visible timeline frame back to the normalized horizontal position. */
    fun visibleFractionForFrame(clip: AudioClip, frame: Long): Float {
        require(clip.lengthFrames > 0L) { "Trim requires a non-empty clip." }
        return ((frame - clip.startFrame).toDouble() / clip.lengthFrames.toDouble()).toFloat().coerceIn(0f, 1f)
    }

    fun moveStart(state: TrimControlState, requestedFrame: Long, clip: AudioClip): TrimControlState {
        require(state.clipId == clip.id) { "Trim state does not belong to clip '${clip.id}'." }
        val maxStart = (state.endFrame - 1L).coerceAtLeast(minimumStartFrame(clip))
        return state.copy(startFrame = requestedFrame.coerceIn(minimumStartFrame(clip), maxStart))
    }

    fun moveEnd(state: TrimControlState, requestedFrame: Long, clip: AudioClip): TrimControlState {
        require(state.clipId == clip.id) { "Trim state does not belong to clip '${clip.id}'." }
        val minEnd = state.startFrame + 1L
        return state.copy(endFrame = requestedFrame.coerceIn(minEnd, maximumEndFrame(clip).coerceAtLeast(minEnd)))
    }
}
