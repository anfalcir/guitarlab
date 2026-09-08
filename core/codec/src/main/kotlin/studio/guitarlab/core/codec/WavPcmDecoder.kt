package studio.guitarlab.core.codec

import kotlin.math.max
import kotlin.math.min

class WavPcmDecoder(
    private val source: SeekableByteSource,
    override val metadata: AudioMetadata = WavMetadataReader().read(source),
) : AudioFrameDecoder {
    private val dataOffset = metadata.dataOffsetBytes
        ?: throw AudioCodecException("WAV metadata is missing data offset")
    private val bytesPerSample = when (metadata.sampleEncoding) {
        AudioSampleEncoding.PCM_U8 -> 1
        AudioSampleEncoding.PCM_S16_LE -> 2
        AudioSampleEncoding.PCM_S24_LE -> 3
        AudioSampleEncoding.PCM_S32_LE, AudioSampleEncoding.FLOAT32_LE -> 4
        AudioSampleEncoding.COMPRESSED -> throw AudioCodecException("Compressed WAV is not supported")
    }
    private val bytesPerFrame = bytesPerSample * metadata.channelCount

    override var positionFrames: Long = 0L
        private set

    init {
        require(metadata.fileFormat == AudioFileFormat.WAV)
        if (bytesPerFrame <= 0) throw AudioCodecException("Invalid WAV frame size")
    }

    override fun seekToFrame(frame: Long) {
        positionFrames = frame.coerceIn(0L, metadata.totalFrames)
    }

    override fun readInterleaved(
        destination: FloatArray,
        destinationFrameOffset: Int,
        frameCount: Int,
    ): Int {
        require(destinationFrameOffset >= 0) { "destinationFrameOffset must be non-negative" }
        require(frameCount >= 0) { "frameCount must be non-negative" }
        val requiredSamples = (destinationFrameOffset.toLong() + frameCount.toLong()) * metadata.channelCount
        require(requiredSamples <= destination.size) { "destination is too small for requested frames" }
        if (frameCount == 0 || positionFrames >= metadata.totalFrames) return 0

        val readableFrames = min(frameCount.toLong(), metadata.totalFrames - positionFrames).toInt()
        val byteCount = Math.multiplyExact(readableFrames, bytesPerFrame)
        val raw = source.readExact(dataOffset + positionFrames * bytesPerFrame, byteCount)
        var rawIndex = 0
        var outputIndex = destinationFrameOffset * metadata.channelCount
        val sampleCount = readableFrames * metadata.channelCount

        repeat(sampleCount) {
            destination[outputIndex++] = decodeSample(raw, rawIndex)
            rawIndex += bytesPerSample
        }
        positionFrames += readableFrames
        return readableFrames
    }

    private fun decodeSample(bytes: ByteArray, offset: Int): Float = when (metadata.sampleEncoding) {
        AudioSampleEncoding.PCM_U8 -> ((bytes[offset].toInt() and 0xFF) - 128) / 128f
        AudioSampleEncoding.PCM_S16_LE -> {
            val value = (bytes[offset].toInt() and 0xFF) or (bytes[offset + 1].toInt() shl 8)
            value.toShort() / 32768f
        }
        AudioSampleEncoding.PCM_S24_LE -> {
            var value = (bytes[offset].toInt() and 0xFF) or
                ((bytes[offset + 1].toInt() and 0xFF) shl 8) or
                ((bytes[offset + 2].toInt() and 0xFF) shl 16)
            if ((value and 0x0080_0000) != 0) value = value or -0x0100_0000
            value / 8_388_608f
        }
        AudioSampleEncoding.PCM_S32_LE -> {
            val value = (bytes[offset].toInt() and 0xFF) or
                ((bytes[offset + 1].toInt() and 0xFF) shl 8) or
                ((bytes[offset + 2].toInt() and 0xFF) shl 16) or
                (bytes[offset + 3].toInt() shl 24)
            (value.toDouble() / 2_147_483_648.0).toFloat()
        }
        AudioSampleEncoding.FLOAT32_LE -> {
            val bits = (bytes[offset].toInt() and 0xFF) or
                ((bytes[offset + 1].toInt() and 0xFF) shl 8) or
                ((bytes[offset + 2].toInt() and 0xFF) shl 16) or
                (bytes[offset + 3].toInt() shl 24)
            val value = Float.fromBits(bits)
            if (value.isFinite()) max(-1f, min(1f, value)) else 0f
        }
        AudioSampleEncoding.COMPRESSED -> error("validated at construction")
    }
}
