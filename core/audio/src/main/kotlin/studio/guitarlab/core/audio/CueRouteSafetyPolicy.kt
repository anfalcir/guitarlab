package studio.guitarlab.core.audio

enum class CueRouteBlockReason {
    NOT_REQUESTED,
    MAIN_NOT_EXPLICIT,
    MAIN_NOT_ACCEPTED,
    CUE_UNAVAILABLE,
    SAME_ENDPOINT,
}

data class CueRouteAdmission(
    val allowed: Boolean,
    val blockReason: CueRouteBlockReason? = null,
)

/**
 * Pure fail-closed policy for a secondary playback bus.
 *
 * Android device ids are transient and must never be persisted, but during one live session
 * they are the authoritative endpoint identity used to prove that MAIN and CUE remain distinct.
 */
object CueRouteSafetyPolicy {
    const val DEFAULT_INITIAL_OFFSET_LIMIT_NS = 12_000_000L
    private const val MIN_DRIFT_LIMIT_FRAMES = 2_048L
    private const val DRIFT_DIVISOR = 20

    fun admit(
        cueRequested: Boolean,
        mainRequested: Boolean,
        mainAccepted: Boolean,
        mainDeviceId: Int?,
        cueDeviceId: Int?,
    ): CueRouteAdmission = when {
        !cueRequested -> CueRouteAdmission(false, CueRouteBlockReason.NOT_REQUESTED)
        !mainRequested || mainDeviceId == null -> CueRouteAdmission(false, CueRouteBlockReason.MAIN_NOT_EXPLICIT)
        !mainAccepted -> CueRouteAdmission(false, CueRouteBlockReason.MAIN_NOT_ACCEPTED)
        cueDeviceId == null -> CueRouteAdmission(false, CueRouteBlockReason.CUE_UNAVAILABLE)
        cueDeviceId == mainDeviceId -> CueRouteAdmission(false, CueRouteBlockReason.SAME_ENDPOINT)
        else -> CueRouteAdmission(true)
    }

    fun routedPairMatches(
        expectedMainDeviceId: Int,
        expectedCueDeviceId: Int,
        actualMainDeviceId: Int?,
        actualCueDeviceId: Int?,
    ): Boolean =
        actualMainDeviceId == expectedMainDeviceId &&
            actualCueDeviceId == expectedCueDeviceId &&
            actualMainDeviceId != actualCueDeviceId

    fun driftLimitFrames(sampleRateHz: Int): Long {
        require(sampleRateHz > 0) { "sampleRateHz must be positive" }
        return maxOf(MIN_DRIFT_LIMIT_FRAMES, (sampleRateHz / DRIFT_DIVISOR).toLong())
    }

    fun driftExceeded(mainPresentedFrames: Long, cuePresentedFrames: Long, sampleRateHz: Int): Boolean =
        kotlin.math.abs(mainPresentedFrames - cuePresentedFrames) > driftLimitFrames(sampleRateHz)

    fun initialOffsetWithinLimit(
        mainStreamOriginNs: Long,
        cueStreamOriginNs: Long,
        maxOffsetNs: Long = DEFAULT_INITIAL_OFFSET_LIMIT_NS,
    ): Boolean {
        require(maxOffsetNs >= 0L)
        return absoluteDistance(mainStreamOriginNs, cueStreamOriginNs) <= maxOffsetNs
    }

    /**
     * CUE is a secondary sink and may never back-pressure MAIN. The render loop therefore writes
     * CUE in non-blocking mode and keeps it active only when one call accepts the complete chunk.
     * Partial/zero/error writes are treated as secondary-sink backpressure and fail closed.
     */
    fun secondaryWriteComplete(requestedSamples: Int, writtenSamples: Int): Boolean =
        requestedSamples > 0 && writtenSamples == requestedSamples

    private fun absoluteDistance(left: Long, right: Long): Long {
        val delta = try {
            Math.subtractExact(left, right)
        } catch (_: ArithmeticException) {
            return Long.MAX_VALUE
        }
        return when {
            delta == Long.MIN_VALUE -> Long.MAX_VALUE
            delta < 0L -> -delta
            else -> delta
        }
    }
}
