package studio.guitarlab.app.ui

import java.security.MessageDigest

data class StudioAudioDeviceChoice(
    val signature: String,
    val label: String,
    val deviceId: Int,
    val type: Int,
    val channelCounts: List<Int>,
    val sampleRates: List<Int>,
    val productName: String = label,
    val address: String = "",
    val transportFamily: String = "type:$type",
    val candidateDeviceIds: List<Int> = listOf(deviceId),
    val legacySignatures: List<String> = listOf(signature),
)

/**
 * Converts low-level Android AudioDeviceInfo endpoints into user-facing physical routes.
 *
 * OEMs may publish one physical microphone/speaker/interface as several logical endpoints. Those
 * implementation details (for example 0/back/bottom, remote-submix or USB endpoint indices) must
 * never leak into the selector. A user chooses a physical destination/source; endpoint resolution
 * stays internal.
 */
internal object StudioAudioRoutePolicy {
    fun canonicalizeInputs(raw: List<StudioAudioDeviceChoice>): List<StudioAudioDeviceChoice> =
        canonicalize(raw, Direction.INPUT)

    fun canonicalizeOutputs(raw: List<StudioAudioDeviceChoice>): List<StudioAudioDeviceChoice> =
        canonicalize(raw, Direction.OUTPUT)

    fun canonicalInputSignature(raw: List<StudioAudioDeviceChoice>, storedSignature: String): String? =
        canonicalSignatureFor(canonicalizeInputs(raw), storedSignature)

    fun canonicalSignature(raw: List<StudioAudioDeviceChoice>, storedSignature: String): String? =
        canonicalSignatureFor(canonicalizeOutputs(raw), storedSignature)

    fun candidateInputIdsFor(raw: List<StudioAudioDeviceChoice>, selectedSignature: String): List<Int> =
        candidateIdsForCanonical(canonicalizeInputs(raw), selectedSignature)

    fun candidateIdsFor(raw: List<StudioAudioDeviceChoice>, selectedSignature: String): List<Int> =
        candidateIdsForCanonical(canonicalizeOutputs(raw), selectedSignature)

    fun compatibilityScore(choice: StudioAudioDeviceChoice): Int {
        var score = 0
        if (choice.channelCounts.isEmpty() || 2 in choice.channelCounts) score += 80
        if (choice.sampleRates.isEmpty() || 48_000 in choice.sampleRates) score += 60
        if (44_100 in choice.sampleRates) score += 20
        if (choice.channelCounts.isNotEmpty()) score += 5
        if (choice.sampleRates.isNotEmpty()) score += 5
        if (choice.address.isBlank()) score += 2
        return score
    }

    private fun canonicalize(raw: List<StudioAudioDeviceChoice>, direction: Direction): List<StudioAudioDeviceChoice> {
        val visible = raw.filterNot { it.transportFamily == HIDDEN_SYSTEM_FAMILY }
        val grouped = linkedMapOf<String, MutableList<StudioAudioDeviceChoice>>()
        visible.forEach { choice -> grouped.getOrPut(groupKey(choice, direction)) { mutableListOf() } += choice }
        return grouped.values.map { mergePhysicalGroup(it, direction) }.sortedBy { it.label.lowercase() }
    }

    private fun groupKey(choice: StudioAudioDeviceChoice, direction: Direction): String = when (choice.transportFamily) {
        USB_FAMILY -> listOf(direction.id, USB_FAMILY, normalize(choice.productName), usbPhysicalAddress(choice.address)).joinToString("\u001f")
        BUILTIN_SPEAKER_FAMILY -> listOf(direction.id, BUILTIN_SPEAKER_FAMILY, normalize(choice.productName)).joinToString("\u001f")
        BUILTIN_MIC_FAMILY -> listOf(direction.id, BUILTIN_MIC_FAMILY, normalize(choice.productName)).joinToString("\u001f")
        else -> "raw\u001f${choice.signature}"
    }

    private fun mergePhysicalGroup(group: List<StudioAudioDeviceChoice>, direction: Direction): StudioAudioDeviceChoice {
        require(group.isNotEmpty())
        val family = group.first().transportFamily
        if (group.size == 1 && family !in CANONICAL_PHYSICAL_FAMILIES) return group.first()

        val representative = group.maxWithOrNull(
            compareBy<StudioAudioDeviceChoice> { compatibilityScore(it) }
                .thenBy { it.channelCounts.size }
                .thenBy { it.sampleRates.size }
        ) ?: group.first()
        val product = representative.productName
        val physicalAddress = if (family == USB_FAMILY) usbPhysicalAddress(representative.address) else ""
        val physicalIdentity = listOf(direction.id, family, normalize(product), normalize(physicalAddress)).joinToString("\u0000")
        val currentSignature = "route3:$family:${shortDigest(physicalIdentity)}"
        val priorCanonicalSignatures = group.map { previousRoute2Signature(it) }

        return representative.copy(
            signature = currentSignature,
            label = friendlyCanonicalLabel(representative, direction),
            address = physicalAddress,
            channelCounts = group.flatMap { it.channelCounts }.filter { it > 0 }.distinct().sorted(),
            sampleRates = group.flatMap { it.sampleRates }.filter { it > 0 }.distinct().sorted(),
            candidateDeviceIds = group.map { it.deviceId }.distinct(),
            legacySignatures = (group.flatMap { it.legacySignatures } + priorCanonicalSignatures).distinct(),
        )
    }

    private fun friendlyCanonicalLabel(choice: StudioAudioDeviceChoice, direction: Direction): String = when (choice.transportFamily) {
        BUILTIN_MIC_FAMILY -> "Microfone do tablet"
        BUILTIN_SPEAKER_FAMILY -> "Alto-falante do tablet"
        USB_FAMILY -> choice.productName.ifBlank { if (direction == Direction.INPUT) "Interface USB" else "Saída USB" }
        else -> choice.label
    }

    private fun previousRoute2Signature(choice: StudioAudioDeviceChoice): String {
        val oldAddress = if (choice.transportFamily == BUILTIN_SPEAKER_FAMILY) "" else choice.address
        val oldIdentity = listOf(choice.transportFamily, normalize(choice.productName), normalize(oldAddress)).joinToString("\u0000")
        return "route2:${choice.transportFamily}:${shortDigest(oldIdentity)}"
    }

    private fun canonicalSignatureFor(canonical: List<StudioAudioDeviceChoice>, storedSignature: String): String? =
        canonical.firstOrNull { choice ->
            choice.signature == storedSignature || storedSignature in choice.legacySignatures
        }?.signature

    private fun candidateIdsForCanonical(canonical: List<StudioAudioDeviceChoice>, selectedSignature: String): List<Int> =
        canonical.firstOrNull { choice ->
            choice.signature == selectedSignature || selectedSignature in choice.legacySignatures
        }?.candidateDeviceIds.orEmpty()

    private fun usbPhysicalAddress(address: String): String {
        val normalized = normalize(address)
        if (normalized.isBlank()) return ""
        Regex("(?:^|[;,\\s])card\\s*=\\s*([^;,\\s]+)", RegexOption.IGNORE_CASE)
            .find(normalized)?.groupValues?.getOrNull(1)?.let { return "card=$it" }
        return normalized
            .replace(Regex("(?:[;,\\s]+)?device\\s*=\\s*[^;,\\s]+", RegexOption.IGNORE_CASE), "")
            .replace(Regex("(?:[;,\\s]+)?endpoint\\s*=\\s*[^;,\\s]+", RegexOption.IGNORE_CASE), "")
            .trim(' ', ';', ',')
    }

    private fun shortDigest(value: String): String = MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray())
        .take(10)
        .joinToString("") { "%02x".format(it) }

    private fun normalize(value: String): String = value.trim().lowercase()

    private enum class Direction(val id: String) { INPUT("in"), OUTPUT("out") }

    const val USB_FAMILY = "usb"
    const val BUILTIN_SPEAKER_FAMILY = "builtin-speaker"
    const val BUILTIN_MIC_FAMILY = "builtin-mic"
    const val HIDDEN_SYSTEM_FAMILY = "hidden-system"
    private val CANONICAL_PHYSICAL_FAMILIES = setOf(USB_FAMILY, BUILTIN_SPEAKER_FAMILY, BUILTIN_MIC_FAMILY)
}
