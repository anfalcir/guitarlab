package studio.guitarlab.platform.audio.android

internal object CueRuntimeWarmupPolicy {
    fun shouldFeed(
        queuedFrames: Long,
        presentedFrames: Long,
        targetOutstandingFrames: Long,
    ): Boolean {
        require(queuedFrames >= 0L)
        require(presentedFrames >= 0L)
        require(targetOutstandingFrames > 0L)
        val outstanding = (queuedFrames - presentedFrames).coerceAtLeast(0L)
        return outstanding < targetOutstandingFrames
    }

    fun drained(queuedFrames: Long, presentedFrames: Long): Boolean {
        require(queuedFrames >= 0L)
        require(presentedFrames >= 0L)
        return presentedFrames >= queuedFrames
    }
}

internal data class CueRelativeDriftObservation(
    val armed: Boolean,
    val baselineDeltaFrames: Long?,
    val currentDeltaFrames: Long,
    val relativeDriftFrames: Long,
    val unsafe: Boolean,
    val failed: Boolean,
)

/**
 * Measures change in MAIN↔CUE separation after both sinks are presenting real playback.
 *
 * A fixed pipeline offset is part of the baseline, not drift. Only subsequent movement away from
 * that baseline is supervised. This is intentionally independent of physical-route validation.
 */
internal class CueRelativeDriftMonitor(
    private val minPresentedFrames: Long,
    private val maxRelativeDriftFrames: Long,
    private val failureGraceNs: Long,
) {
    private var baselineDeltaFrames: Long? = null
    private var unsafeSinceNs: Long? = null

    init {
        require(minPresentedFrames > 0L)
        require(maxRelativeDriftFrames >= 0L)
        require(failureGraceNs >= 0L)
    }

    fun reset() {
        baselineDeltaFrames = null
        unsafeSinceNs = null
    }

    fun observe(
        mainPresentedFrames: Long,
        cuePresentedFrames: Long,
        nowNs: Long,
    ): CueRelativeDriftObservation {
        require(mainPresentedFrames >= 0L)
        require(cuePresentedFrames >= 0L)

        val currentDelta = cuePresentedFrames - mainPresentedFrames
        val existingBaseline = baselineDeltaFrames
        if (existingBaseline == null) {
            if (mainPresentedFrames < minPresentedFrames || cuePresentedFrames < minPresentedFrames) {
                return CueRelativeDriftObservation(
                    armed = false,
                    baselineDeltaFrames = null,
                    currentDeltaFrames = currentDelta,
                    relativeDriftFrames = 0L,
                    unsafe = false,
                    failed = false,
                )
            }
            baselineDeltaFrames = currentDelta
            unsafeSinceNs = null
            return CueRelativeDriftObservation(
                armed = true,
                baselineDeltaFrames = currentDelta,
                currentDeltaFrames = currentDelta,
                relativeDriftFrames = 0L,
                unsafe = false,
                failed = false,
            )
        }

        val relativeDrift = absoluteDistance(currentDelta, existingBaseline)
        val unsafe = relativeDrift > maxRelativeDriftFrames
        if (unsafe) {
            if (unsafeSinceNs == null) unsafeSinceNs = nowNs
        } else {
            unsafeSinceNs = null
        }
        val failed = unsafe && unsafeSinceNs?.let { since ->
            elapsedNs(nowNs, since) >= failureGraceNs
        } == true
        return CueRelativeDriftObservation(
            armed = true,
            baselineDeltaFrames = existingBaseline,
            currentDeltaFrames = currentDelta,
            relativeDriftFrames = relativeDrift,
            unsafe = unsafe,
            failed = failed,
        )
    }

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

    private fun elapsedNs(nowNs: Long, sinceNs: Long): Long =
        if (nowNs >= sinceNs) nowNs - sinceNs else Long.MAX_VALUE
}
