package studio.guitarlab.core.codec

enum class CodecSupportState {
    IMPLEMENTED_PENDING_ANDROID_GATE,
    VERIFIED,
    PLANNED_UNVERIFIED,
}

data class CodecSupportEntry(
    val format: AudioFileFormat,
    val state: CodecSupportState,
    val decoderPath: String,
    val notes: String,
)

/**
 * Product-facing code must only advertise VERIFIED entries. Until Android/golden
 * evidence exists, an implemented path remains explicitly pending rather than
 * becoming an accidental support claim.
 */
object CodecSupportMatrix {
    val import: List<CodecSupportEntry> = listOf(
        CodecSupportEntry(
            format = AudioFileFormat.WAV,
            state = CodecSupportState.IMPLEMENTED_PENDING_ANDROID_GATE,
            decoderPath = "core:codec/WavPcmDecoder",
            notes = "RIFF/WAVE PCM 8/16/24/32-bit and IEEE float32; mono/multichannel; Android gate still required.",
        ),
        planned(AudioFileFormat.FLAC),
        planned(AudioFileFormat.AIFF),
        planned(AudioFileFormat.MP3),
        planned(AudioFileFormat.AAC_M4A),
        planned(AudioFileFormat.OGG_VORBIS),
        planned(AudioFileFormat.OPUS),
    )

    val advertisedImportFormats: Set<AudioFileFormat>
        get() = import.filter { it.state == CodecSupportState.VERIFIED }.mapTo(linkedSetOf()) { it.format }

    private fun planned(format: AudioFileFormat) = CodecSupportEntry(
        format = format,
        state = CodecSupportState.PLANNED_UNVERIFIED,
        decoderPath = "not selected",
        notes = "V1 target only; do not advertise before Android decode, metadata, seek and license gates pass.",
    )
}
