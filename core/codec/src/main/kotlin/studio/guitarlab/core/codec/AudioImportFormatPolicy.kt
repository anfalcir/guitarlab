package studio.guitarlab.core.codec

enum class AudioImportFormat(
    val displayName: String,
    val extensions: Set<String>,
    val pickerMimeTypes: Set<String>,
) {
    WAV_PCM(
        displayName = "WAV PCM",
        extensions = setOf("wav", "wave"),
        pickerMimeTypes = setOf("audio/wav", "audio/x-wav", "audio/wave"),
    ),
    FLAC(
        displayName = "FLAC",
        extensions = setOf("flac"),
        pickerMimeTypes = setOf("audio/flac", "audio/x-flac"),
    ),
    AIFF(
        displayName = "AIFF",
        extensions = setOf("aif", "aiff", "aifc"),
        pickerMimeTypes = setOf("audio/aiff", "audio/x-aiff"),
    ),
    MP3(
        displayName = "MP3",
        extensions = setOf("mp3"),
        pickerMimeTypes = setOf("audio/mpeg", "audio/mp3"),
    ),
    AAC_M4A(
        displayName = "AAC/M4A",
        extensions = setOf("aac", "m4a"),
        pickerMimeTypes = setOf("audio/aac", "audio/mp4", "audio/m4a", "audio/x-m4a"),
    ),
    OGG_VORBIS(
        displayName = "OGG Vorbis",
        extensions = setOf("ogg", "oga"),
        pickerMimeTypes = setOf("audio/ogg", "application/ogg"),
    ),
    OPUS(
        displayName = "Opus",
        extensions = setOf("opus"),
        pickerMimeTypes = setOf("audio/opus"),
    ),
}

object AudioImportFormatPolicy {
    val allFormats: List<AudioImportFormat> = AudioImportFormat.entries

    val pickerMimeTypes: Array<String> = allFormats
        .flatMap { it.pickerMimeTypes }
        .distinct()
        .toTypedArray()

    val supportedExtensionsDescription: String =
        "WAV, FLAC, AIFF, MP3, AAC/M4A, OGG e Opus"

    fun detect(fileName: String?, mimeType: String?): AudioImportFormat? {
        val extension = fileName
            ?.substringAfterLast('.', missingDelimiterValue = "")
            ?.trim()
            ?.lowercase()
            .orEmpty()
        if (extension.isNotEmpty()) {
            allFormats.firstOrNull { extension in it.extensions }?.let { return it }
        }

        val normalizedMime = mimeType?.substringBefore(';')?.trim()?.lowercase().orEmpty()
        if (normalizedMime.isNotEmpty()) {
            allFormats.firstOrNull { format ->
                format.pickerMimeTypes.any { it.lowercase() == normalizedMime }
            }?.let { return it }
        }
        return null
    }

    fun managedWavName(originalName: String): String {
        val stem = originalName.substringBeforeLast('.', missingDelimiterValue = originalName).ifBlank { "audio" }
        return "$stem.wav"
    }
}
