package studio.guitarlab.app.ui

import studio.guitarlab.core.audio.MeterBallisticsPolicy
import studio.guitarlab.core.audio.MeterBallisticsState

/** Projects raw recording input metering onto the currently captured track. */
internal object RecordingTrackMeterPolicy {
    fun update(
        previous: Map<String, MeterBallisticsState>,
        targetTrackId: String?,
        rawPeak: Float,
        rawRms: Float,
        nowMs: Long,
    ): Map<String, MeterBallisticsState> {
        if (targetTrackId == null) return previous
        return previous + (
            targetTrackId to MeterBallisticsPolicy.update(
                previous = previous[targetTrackId] ?: MeterBallisticsState(),
                rawPeak = rawPeak,
                rawRms = rawRms,
                nowMs = nowMs,
            )
        )
    }
}
