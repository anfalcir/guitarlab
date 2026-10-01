package studio.guitarlab.core.audio

/** Platform adapter must feed zeroes non-blockingly; never supply project audio during admission. */
interface CueProbeOutput {
    fun feedSilence(): Int
    fun routedDeviceId(): Int?
    fun clockObservation(): AudioClockObservation?
}

enum class CueStartupFailure { CANCELLED, WRITE_FAILED, ROUTE_UNCONFIRMED, ROUTE_CHANGED, CLOCK_UNSTABLE, OFFSET_EXCEEDED }

/** Interleaved bounded warmup: both sinks remain fed while independent timestamps advance. */
object CueStartupProbe {
    const val TIMEOUT_NS = 1_500_000_000L
    private const val POLL_MS = 8L
    private const val MAX_OBSERVATIONS = 12

    fun verify(
        main: CueProbeOutput, cue: CueProbeOutput,
        expectedMainId: Int, expectedCueId: Int, sampleRateHz: Int,
        nowNs: () -> Long, sleepMs: (Long) -> Unit, keepRunning: () -> Boolean,
    ): CueStartupFailure? {
        require(sampleRateHz > 0 && expectedMainId != expectedCueId)
        val started = nowNs()
        var routesConfirmed = false
        val mainClock = ArrayList<AudioClockObservation>()
        val cueClock = ArrayList<AudioClockObservation>()
        fun collect(target: MutableList<AudioClockObservation>, sample: AudioClockObservation?): Boolean {
            if (sample == null || sample.framePosition < 0 || sample.nanoTime <= 0) return true
            val previous = target.lastOrNull()
            if (previous != null) {
                if (sample.framePosition < previous.framePosition || sample.nanoTime < previous.nanoTime) return false
                if (sample.framePosition == previous.framePosition || sample.nanoTime == previous.nanoTime) return true
            }
            target += sample
            if (target.size > MAX_OBSERVATIONS) target.removeAt(0)
            return true
        }
        while (nowNs() - started < TIMEOUT_NS) {
            if (!keepRunning()) return CueStartupFailure.CANCELLED
            // Zero/partial writes are normal during startup. Retry next poll, never block either sink.
            if (main.feedSilence() < 0 || cue.feedSilence() < 0) return CueStartupFailure.WRITE_FAILED
            val distinct = CueRouteSafetyPolicy.routedPairMatches(expectedMainId, expectedCueId,
                main.routedDeviceId(), cue.routedDeviceId())
            if (!distinct && routesConfirmed) return CueStartupFailure.ROUTE_CHANGED
            if (distinct) {
                routesConfirmed = true
                if (!collect(mainClock, main.clockObservation()) || !collect(cueClock, cue.clockObservation())) {
                    return CueStartupFailure.CLOCK_UNSTABLE
                }
                val mainAnchor = AudioClockAnchorPolicy.estimate(mainClock, sampleRateHz, 4_000_000L)
                val cueAnchor = AudioClockAnchorPolicy.estimate(cueClock, sampleRateHz, 4_000_000L)
                if (mainAnchor != null && cueAnchor != null && mainAnchor.observations >= 3 && cueAnchor.observations >= 3) {
                    return if (CueRouteSafetyPolicy.initialOffsetWithinLimit(mainAnchor.streamOriginMonotonicNs,
                            cueAnchor.streamOriginMonotonicNs)) null else CueStartupFailure.OFFSET_EXCEEDED
                }
            }
            sleepMs(POLL_MS)
        }
        return if (routesConfirmed) CueStartupFailure.CLOCK_UNSTABLE else CueStartupFailure.ROUTE_UNCONFIRMED
    }
}
