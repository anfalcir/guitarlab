package studio.guitarlab.core.audio

object PlaybackClockPolicy {
    /**
     * Converts the AudioTrack presentation clock to a count of frames that have actually reached
     * the presentation timeline. Before the stream origin, the visible position stays anchored.
     * The result is capped by frames already accepted by AudioTrack so UI can never outrun audio.
     */
    fun presentedFramesAt(
        nowMonotonicNs: Long,
        presentationOriginMonotonicNs: Long,
        sampleRateHz: Int,
        maxWrittenFrames: Long,
    ): Long {
        require(sampleRateHz > 0) { "Sample rate must be positive." }
        val written = maxWrittenFrames.coerceAtLeast(0L)
        if (written == 0L || nowMonotonicNs <= presentationOriginMonotonicNs) return 0L

        val elapsedNs = try {
            Math.subtractExact(nowMonotonicNs, presentationOriginMonotonicNs)
        } catch (_: ArithmeticException) {
            Long.MAX_VALUE
        }
        if (elapsedNs <= 0L) return 0L

        val presented = (elapsedNs.toDouble() * sampleRateHz.toDouble() / 1_000_000_000.0).toLong()
        return presented.coerceIn(0L, written)
    }

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
