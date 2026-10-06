package studio.guitarlab.core.audio

/** Platform adapter must feed zeroes non-blockingly; never supply project audio during admission. */
interface CueProbeOutput {
    fun feedSilence(): Int
    fun routedPhysicalKeys(): Set<String>
    fun clockObservation(): AudioClockObservation?
}

data class CueRouteObservation(
    val elapsedNs: Long,
    val mainWriteResult: Int,
    val cueWriteResult: Int,
    val mainPhysicalKeys: Set<String>,
    val cuePhysicalKeys: Set<String>,
)

enum class CueStartupFailure {
    CANCELLED,
    WRITE_FAILED,
    MISSING_EFFECTIVE_ROUTE,
    CONVERGED_TO_MAIN,
    WRONG_OR_MIRRORED_ROUTE,
    ROUTE_CHANGED,
    CLOCK_UNSTABLE,
    OFFSET_EXCEEDED,
}

/** Interleaved bounded warmup: both sinks remain fed while independent timestamps advance. */
object CueStartupProbe {
    const val ROUTE_SETTLE_TIMEOUT_NS = 5_000_000_000L
    const val CLOCK_QUALIFICATION_TIMEOUT_NS = 2_000_000_000L
    const val TIMEOUT_NS = ROUTE_SETTLE_TIMEOUT_NS + CLOCK_QUALIFICATION_TIMEOUT_NS
    private const val POLL_MS = 8L
    private const val MAX_OBSERVATIONS = 12
    private const val ROUTE_STABLE_POLLS = 4

    fun verify(
        main: CueProbeOutput, cue: CueProbeOutput,
        expectedMainPhysicalKey: String, expectedCuePhysicalKey: String, sampleRateHz: Int,
        nowNs: () -> Long, sleepMs: (Long) -> Unit, keepRunning: () -> Boolean,
        routeObserver: ((CueRouteObservation) -> Unit)? = null,
    ): CueStartupFailure? {
        require(sampleRateHz > 0 && expectedMainPhysicalKey != expectedCuePhysicalKey)
        val started = nowNs()
        var routeQualifiedAtNs: Long? = null
        var stableRoutePolls = 0
        var lastRoute = CueRouteObservation(0L, 0, 0, emptySet(), emptySet())
        val mainClock = ArrayList<AudioClockObservation>()
        val cueClock = ArrayList<AudioClockObservation>()
        fun routesOnlyTo(expected: String, actual: Set<String>): Boolean =
            actual.isNotEmpty() && actual.all { it == expected }
        fun routeFailure(observation: CueRouteObservation): CueStartupFailure = when {
            observation.mainPhysicalKeys.isEmpty() || observation.cuePhysicalKeys.isEmpty() ->
                CueStartupFailure.MISSING_EFFECTIVE_ROUTE
            routesOnlyTo(expectedMainPhysicalKey, observation.mainPhysicalKeys) &&
                routesOnlyTo(expectedMainPhysicalKey, observation.cuePhysicalKeys) ->
                CueStartupFailure.CONVERGED_TO_MAIN
            else -> CueStartupFailure.WRONG_OR_MIRRORED_ROUTE
        }
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
        while (true) {
            if (!keepRunning()) return CueStartupFailure.CANCELLED
            // Zero/partial writes are normal during startup. Retry next poll, never block either sink.
            val mainWrite = main.feedSilence()
            val cueWrite = cue.feedSilence()
            if (mainWrite < 0 || cueWrite < 0) return CueStartupFailure.WRITE_FAILED
            val now = nowNs()
            lastRoute = CueRouteObservation(
                elapsedNs = now - started,
                mainWriteResult = mainWrite,
                cueWriteResult = cueWrite,
                mainPhysicalKeys = main.routedPhysicalKeys().toSet(),
                cuePhysicalKeys = cue.routedPhysicalKeys().toSet(),
            )
            routeObserver?.invoke(lastRoute)
            val distinct = routesOnlyTo(expectedMainPhysicalKey, lastRoute.mainPhysicalKeys) &&
                routesOnlyTo(expectedCuePhysicalKey, lastRoute.cuePhysicalKeys)
            val qualifiedAt = routeQualifiedAtNs
            if (qualifiedAt == null) {
                // Android/OEM routing may settle asynchronously after play(). Transient convergence
                // during this bounded phase is not accepted, but it is not treated as a runtime
                // route loss until the pair has first been stable for several consecutive polls.
                stableRoutePolls = if (distinct) stableRoutePolls + 1 else 0
                if (stableRoutePolls >= ROUTE_STABLE_POLLS) {
                    routeQualifiedAtNs = now
                    mainClock.clear()
                    cueClock.clear()
                } else if (now - started >= ROUTE_SETTLE_TIMEOUT_NS) {
                    return routeFailure(lastRoute)
                }
            } else {
                if (!distinct) return CueStartupFailure.ROUTE_CHANGED
                if (!collect(mainClock, main.clockObservation()) || !collect(cueClock, cue.clockObservation())) {
                    return CueStartupFailure.CLOCK_UNSTABLE
                }
                val mainAnchor = AudioClockAnchorPolicy.estimate(mainClock, sampleRateHz, 4_000_000L)
                val cueAnchor = AudioClockAnchorPolicy.estimate(cueClock, sampleRateHz, 4_000_000L)
                if (mainAnchor != null && cueAnchor != null && mainAnchor.observations >= 3 && cueAnchor.observations >= 3) {
                    return if (CueRouteSafetyPolicy.initialOffsetWithinLimit(mainAnchor.streamOriginMonotonicNs,
                            cueAnchor.streamOriginMonotonicNs)) null else CueStartupFailure.OFFSET_EXCEEDED
                }
                if (now - qualifiedAt >= CLOCK_QUALIFICATION_TIMEOUT_NS) {
                    return CueStartupFailure.CLOCK_UNSTABLE
                }
            }
            sleepMs(POLL_MS)
        }
    }
}
