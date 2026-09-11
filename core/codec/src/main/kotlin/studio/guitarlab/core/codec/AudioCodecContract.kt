package studio.guitarlab.core.codec

enum class AudioFileFormat {
    WAV,
    FLAC,
    AIFF,
    MP3,
    AAC_M4A,
    OGG_VORBIS,
    OPUS,
}

enum class AudioSampleEncoding {
    PCM_U8,
    PCM_S16_LE,
    PCM_S24_LE,
    PCM_S32_LE,
    FLOAT32_LE,
    COMPRESSED,
}

data class AudioMetadata(
    val fileFormat: AudioFileFormat,
    val sampleRateHz: Int,
    val channelCount: Int,
    val sampleEncoding: AudioSampleEncoding,
    val bitsPerSample: Int?,
    val totalFrames: Long,
    val durationUs: Long,
    val dataOffsetBytes: Long? = null,
    val dataSizeBytes: Long? = null,
) {
    init {
        require(sampleRateHz > 0) { "sampleRateHz must be positive" }
        require(channelCount > 0) { "channelCount must be positive" }
        require(totalFrames >= 0) { "totalFrames must be non-negative" }
        require(durationUs >= 0) { "durationUs must be non-negative" }
    }
}

/** Random-access byte source keeps codec math independent from Android/storage APIs. */
interface SeekableByteSource {
    val sizeBytes: Long

    /** Returns bytes read, 0 at EOF. Implementations must never read beyond destination bounds. */
    fun readAt(position: Long, destination: ByteArray, offset: Int = 0, length: Int = destination.size - offset): Int
}

interface AudioMetadataReader {
    fun read(source: SeekableByteSource): AudioMetadata
}

/** Decoder output is interleaved normalized float PCM in source channel order. */
interface AudioFrameDecoder : AutoCloseable {
    val metadata: AudioMetadata
    val positionFrames: Long

    fun seekToFrame(frame: Long)

    /** Returns decoded frames, 0 at end of stream. */
    fun readInterleaved(destination: FloatArray, destinationFrameOffset: Int = 0, frameCount: Int): Int

    override fun close() = Unit
}

data class AudioEncoderSpec(
    val fileFormat: AudioFileFormat,
    val sampleRateHz: Int,
    val channelCount: Int,
    val sampleEncoding: AudioSampleEncoding,
    val bitsPerSample: Int? = null,
)

/** Foundation only in M3; concrete export encoders are promoted when individually verified. */
interface AudioEncoder : AutoCloseable {
    val spec: AudioEncoderSpec
    fun writeInterleaved(samples: FloatArray, sourceFrameOffset: Int = 0, frameCount: Int)
    fun finish()
    override fun close() = Unit
}

class AudioCodecException(message: String, cause: Throwable? = null) : IllegalArgumentException(message, cause)
