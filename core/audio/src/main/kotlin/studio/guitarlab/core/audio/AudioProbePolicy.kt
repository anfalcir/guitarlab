package studio.guitarlab.core.audio

/** Deterministic negotiation helpers kept outside Android APIs so they are unit-testable. */
object AudioProbePolicy {
    private val preferredRates = listOf(48_000, 44_100, 96_000, 88_200)

    /**
     * Prefer an explicitly advertised device rate. If the device reports no rates,
     * use common Android rates in a stable order. 48 kHz is first for generic
     * low-latency Android devices; a 44.1 kHz-only USB interface naturally chooses
     * 44.1 kHz because it advertises that value.
     */
    fun sampleRateCandidates(deviceRates: List<Int>): List<Int> {
        val normalized = deviceRates.filter { it in 8_000..384_000 }.distinct()
        if (normalized.isEmpty()) return preferredRates

        val preferred = preferredRates.filter { it in normalized }
        val remaining = normalized.filterNot { it in preferred }.sorted()
        return preferred + remaining
    }

    fun channelCandidates(deviceChannels: List<Int>, direction: AudioDirection): List<Int> {
        val normalized = deviceChannels.filter { it > 0 }.distinct()
        val preferred = when (direction) {
            AudioDirection.INPUT -> listOf(1, 2)
            AudioDirection.OUTPUT -> listOf(2, 1)
        }
        if (normalized.isEmpty()) return preferred
        return preferred.filter { it in normalized }.ifEmpty {
            listOf(normalized.minOrNull() ?: 1)
        }
    }

    fun encodingCandidates(deviceEncodings: List<PcmEncoding>): List<PcmEncoding> {
        if (deviceEncodings.isEmpty()) return listOf(PcmEncoding.FLOAT_32, PcmEncoding.PCM_16)
        return listOf(PcmEncoding.FLOAT_32, PcmEncoding.PCM_16).filter { it in deviceEncodings }
            .ifEmpty { listOf(PcmEncoding.PCM_16) }
    }

    /** A diagnostic capture is only usable when frames arrived and at least one
     * sample is non-zero. Exact digital zero must never pass the hardware gate. */
    fun hasUsableInputSignal(stats: AudioSignalStats): Boolean =
        stats.frames > 0 && stats.peak > 0f
}
