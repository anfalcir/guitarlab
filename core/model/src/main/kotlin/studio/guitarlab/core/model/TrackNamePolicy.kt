package studio.guitarlab.core.model

/**
 * Canonical product rule for user-editable track names.
 *
 * The Studio sidebar is designed to render every valid track name in full, in at most two lines.
 * Character counting is Unicode code-point based so accented characters are not accidentally
 * penalized by UTF-16 storage details.
 */
object TrackNamePolicy {
    const val MAX_CHARACTERS: Int = 24

    /** Normalizes storage text without silently truncating a name. */
    fun normalize(value: String): String = value
        .replace(Regex("[\\r\\n\\t]+"), " ")
        .replace(Regex(" +"), " ")
        .trim()

    fun characterCount(value: String): Int {
        val normalized = normalize(value)
        return normalized.codePointCount(0, normalized.length)
    }

    fun isValid(value: String): Boolean {
        val normalized = normalize(value)
        return normalized.isNotBlank() && normalized.codePointCount(0, normalized.length) <= MAX_CHARACTERS
    }

    /**
     * Used while typing. Newlines are converted to spaces and input beyond the product limit is
     * ignored rather than stored and later ellipsized.
     */
    fun constrainDraft(value: String): String {
        val normalizedLineBreaks = value.replace(Regex("[\\r\\n\\t]+"), " ")
        val count = normalizedLineBreaks.codePointCount(0, normalizedLineBreaks.length)
        if (count <= MAX_CHARACTERS) return normalizedLineBreaks
        val end = normalizedLineBreaks.offsetByCodePoints(0, MAX_CHARACTERS)
        return normalizedLineBreaks.substring(0, end)
    }

    /** Returns the canonical persisted value or throws for a blank/oversized name. */
    fun requireValid(value: String): String {
        val normalized = normalize(value)
        require(normalized.isNotBlank()) { "O nome da pista não pode ficar vazio." }
        require(normalized.codePointCount(0, normalized.length) <= MAX_CHARACTERS) {
            "O nome da pista deve ter no máximo $MAX_CHARACTERS caracteres."
        }
        return normalized
    }
}
