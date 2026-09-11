package studio.guitarlab.core.audio

import kotlin.math.pow

data class StereoGain(val left: Float, val right: Float)

object TrackMixPolicy {
    fun isAudible(muted: Boolean, solo: Boolean, anySolo: Boolean): Boolean =
        !muted && (!anySolo || solo)

    fun channelGains(gainDb: Float, pan: Float): StereoGain {
        val safePan = pan.coerceIn(-1f, 1f)
        val amplitude = 10.0.pow(gainDb.coerceIn(-120f, 24f).toDouble() / 20.0).toFloat()
        val leftBalance = if (safePan > 0f) 1f - safePan else 1f
        val rightBalance = if (safePan < 0f) 1f + safePan else 1f
        return StereoGain(amplitude * leftBalance, amplitude * rightBalance)
    }
}
