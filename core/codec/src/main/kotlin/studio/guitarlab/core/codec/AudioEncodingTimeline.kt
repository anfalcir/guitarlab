package studio.guitarlab.core.codec

/** Deterministic frame-domain timestamps for feeding streaming audio encoders. */
object AudioEncodingTimeline {
    fun presentationTimeUs(framePosition: Long, sampleRateHz: Int): Long {
        require(framePosition >= 0L) { "Frame position must be non-negative." }
        require(sampleRateHz > 0) { "Sample rate must be positive." }
        return framePosition * 1_000_000L / sampleRateHz.toLong()
    }
}
