package studio.guitarlab.core.audio

object PlaybackClockPolicy {
    fun timelineFrame(
        startFrame: Long,
        presentedFrames: Long,
        projectEndFrame: Long,
        loopEnabled: Boolean,
        loopStartFrame: Long,
        loopEndFrame: Long,
    ): Long {
        val end = projectEndFrame.coerceAtLeast(1L)
        val start = startFrame.coerceIn(0L, end)
        val presented = presentedFrames.coerceAtLeast(0L)
        if (!loopEnabled) return (start + presented).coerceAtMost(end)

        val loopStart = loopStartFrame.coerceIn(0L, end - 1L)
        val loopEnd = loopEndFrame.coerceIn(loopStart + 1L, end)
        val loopLength = loopEnd - loopStart
        val normalizedStart = if (start >= loopEnd) loopStart else start
        val untilLoopEnd = loopEnd - normalizedStart
        return if (presented < untilLoopEnd) {
            normalizedStart + presented
        } else {
            loopStart + ((presented - untilLoopEnd) % loopLength)
        }
    }
}
