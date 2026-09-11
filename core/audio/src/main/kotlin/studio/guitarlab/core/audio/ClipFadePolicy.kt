package studio.guitarlab.core.audio

import kotlin.math.min

/** Deterministic linear clip-envelope shared by realtime playback and offline master rendering. */
object ClipFadePolicy {
    fun gain(localFrame: Long, clipLengthFrames: Long, fadeInFrames: Long, fadeOutFrames: Long): Float {
        if (clipLengthFrames <= 0L) return 0f
        val frame = localFrame.coerceIn(0L, clipLengthFrames - 1L)
        val fadeIn = fadeInFrames.coerceIn(0L, clipLengthFrames)
        val fadeOut = fadeOutFrames.coerceIn(0L, clipLengthFrames)
        var gain = 1f
        if (fadeIn > 0L && frame < fadeIn) gain = min(gain, frame.toFloat() / fadeIn.toFloat())
        val remaining = clipLengthFrames - 1L - frame
        if (fadeOut > 0L && remaining < fadeOut) gain = min(gain, remaining.toFloat() / fadeOut.toFloat())
        return gain.coerceIn(0f, 1f)
    }
}
