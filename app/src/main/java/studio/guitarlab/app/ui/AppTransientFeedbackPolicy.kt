package studio.guitarlab.app.ui

/**
 * Contract for transient user feedback in GuitarLab.
 *
 * Snackbars are reserved for errors, meaningful degradation/warnings and asynchronous completion
 * that is not already obvious from the UI. Routine state transitions (play, stop, cut mode,
 * countdown, recording state, mute/solo, navigation, route success) stay in their owning UI.
 */
enum class TransientFeedbackKind { ERROR, WARNING, ASYNC_COMPLETION, OPERATIONAL_STATUS }

object AppTransientFeedbackPolicy {
    fun shouldShowSnackbar(kind: TransientFeedbackKind): Boolean = when (kind) {
        TransientFeedbackKind.ERROR,
        TransientFeedbackKind.WARNING,
        TransientFeedbackKind.ASYNC_COMPLETION -> true
        TransientFeedbackKind.OPERATIONAL_STATUS -> false
    }

    fun userSafe(message: String, fallback: String = "O áudio mudou de rota."): String {
        val normalized = message.trim()
        if (normalized.isBlank()) return fallback
        return if (containsLowLevelAudioIdentifier(normalized)) fallback else normalized
    }

    fun containsLowLevelAudioIdentifier(message: String): Boolean {
        val lower = message.lowercase()
        return TECHNICAL_TOKENS.any(lower::contains) ||
            TECHNICAL_PATTERNS.any { it.containsMatchIn(message) }
    }

    private val TECHNICAL_TOKENS = listOf(
        "remote-submix",
        "hsp:",
        "route2:",
        "route3:",
        "audiodeviceinfo",
        "deviceid=",
        "device id",
        "productname=",
        "product name",
        "address=",
        "address:",
        "endpoint=",
        "endpoint:",
    )

    private val TECHNICAL_PATTERNS = listOf(
        Regex("[•·]\\s*(?:0|back|bottom)\\b", RegexOption.IGNORE_CASE),
        Regex("(?:^|\\s)(?:back|bottom)(?:\\s|$)", RegexOption.IGNORE_CASE),
        Regex("\\bdevice\\s*[#:=]\\s*\\d+\\b", RegexOption.IGNORE_CASE),
        Regex("\\b(?:endpoint|device)\\s+index\\s*[#:=]?\\s*\\d+\\b", RegexOption.IGNORE_CASE),
    )
}
