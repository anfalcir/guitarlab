package studio.guitarlab.core.codec

class WavMetadataReader : AudioMetadataReader {
    override fun read(source: SeekableByteSource): AudioMetadata {
        if (source.sizeBytes < 12) throw AudioCodecException("WAV is too small for RIFF/WAVE header")
        val riff = source.readExact(0, 12)
        if (riff.asAscii(0, 4) != "RIFF" || riff.asAscii(8, 4) != "WAVE") {
            throw AudioCodecException("Unsupported WAV container: expected RIFF/WAVE")
        }
        val riffDeclaredBytes = riff.u32le(4) + 8L
        if (riffDeclaredBytes > source.sizeBytes) {
            throw AudioCodecException("WAV RIFF size exceeds available file bytes")
        }

        var cursor = 12L
        var format: FormatChunk? = null
        var dataOffset: Long? = null
        var dataSize: Long? = null
        val scanLimit = minOf(source.sizeBytes, riffDeclaredBytes)

        while (cursor + 8 <= scanLimit) {
            val chunkHeader = source.readExact(cursor, 8)
            val id = chunkHeader.asAscii(0, 4)
            val declaredSize = chunkHeader.u32le(4)
            val payloadOffset = cursor + 8
            val payloadEnd = payloadOffset + declaredSize
            if (payloadEnd < payloadOffset || payloadEnd > scanLimit) {
                throw AudioCodecException("WAV chunk '$id' exceeds RIFF/file bounds")
            }

            when (id) {
                "fmt " -> if (format == null) format = parseFormat(source, payloadOffset, declaredSize)
                "data" -> if (dataOffset == null) {
                    dataOffset = payloadOffset
                    dataSize = declaredSize
                }
            }

            val paddedSize = declaredSize + (declaredSize and 1L)
            val next = payloadOffset + paddedSize
            if (next < payloadOffset) throw AudioCodecException("WAV chunk offset overflow")
            cursor = next
            if (format != null && dataOffset != null) break
        }

        val fmt = format ?: throw AudioCodecException("WAV is missing fmt chunk")
        val offset = dataOffset ?: throw AudioCodecException("WAV is missing data chunk")
        val size = dataSize ?: 0L
        if (fmt.blockAlign <= 0) throw AudioCodecException("WAV blockAlign must be positive")
        if (size % fmt.blockAlign != 0L) {
            throw AudioCodecException("WAV data size is not aligned to complete frames")
        }

        val totalFrames = size / fmt.blockAlign
        val durationUs = multiplyDivideFloor(totalFrames, 1_000_000L, fmt.sampleRateHz.toLong())
        return AudioMetadata(
            fileFormat = AudioFileFormat.WAV,
            sampleRateHz = fmt.sampleRateHz,
            channelCount = fmt.channelCount,
            sampleEncoding = fmt.encoding,
            bitsPerSample = fmt.bitsPerSample,
            totalFrames = totalFrames,
            durationUs = durationUs,
            dataOffsetBytes = offset,
            dataSizeBytes = size,
        )
    }

    private fun parseFormat(source: SeekableByteSource, offset: Long, size: Long): FormatChunk {
        if (size < 16) throw AudioCodecException("WAV fmt chunk is shorter than 16 bytes")
        if (size > Int.MAX_VALUE) throw AudioCodecException("WAV fmt chunk is unreasonably large")
        val bytes = source.readExact(offset, size.toInt())
        val rawFormatTag = bytes.u16le(0)
        val channels = bytes.u16le(2)
        val sampleRate = bytes.u32le(4)
        val blockAlign = bytes.u16le(12)
        val bits = bytes.u16le(14)

        if (channels !in 1..32) throw AudioCodecException("Unsupported WAV channel count: $channels")
        if (sampleRate !in 8_000L..384_000L) throw AudioCodecException("Unsupported WAV sample rate: $sampleRate")
        if (bits !in setOf(8, 16, 24, 32)) throw AudioCodecException("Unsupported WAV bit depth: $bits")

        val effectiveTag = when (rawFormatTag) {
            WAVE_FORMAT_PCM, WAVE_FORMAT_IEEE_FLOAT -> rawFormatTag
            WAVE_FORMAT_EXTENSIBLE -> {
                if (size < 40) throw AudioCodecException("WAVE_FORMAT_EXTENSIBLE fmt chunk is incomplete")
                val cbSize = bytes.u16le(16)
                if (cbSize < 22) throw AudioCodecException("WAVE_FORMAT_EXTENSIBLE extension is incomplete")
                bytes.u16le(24)
            }
            else -> throw AudioCodecException("Unsupported WAV format tag: $rawFormatTag")
        }

        val encoding = when (effectiveTag) {
            WAVE_FORMAT_PCM -> when (bits) {
                8 -> AudioSampleEncoding.PCM_U8
                16 -> AudioSampleEncoding.PCM_S16_LE
                24 -> AudioSampleEncoding.PCM_S24_LE
                32 -> AudioSampleEncoding.PCM_S32_LE
                else -> error("validated above")
            }
            WAVE_FORMAT_IEEE_FLOAT -> {
                if (bits != 32) throw AudioCodecException("Only 32-bit IEEE float WAV is supported")
                AudioSampleEncoding.FLOAT32_LE
            }
            else -> throw AudioCodecException("Unsupported WAV subformat tag: $effectiveTag")
        }

        val expectedBlockAlign = channels * (bits / 8)
        if (blockAlign != expectedBlockAlign) {
            throw AudioCodecException("WAV blockAlign=$blockAlign does not match channels/bit depth ($expectedBlockAlign)")
        }

        return FormatChunk(
            channelCount = channels,
            sampleRateHz = sampleRate.toInt(),
            bitsPerSample = bits,
            blockAlign = blockAlign.toLong(),
            encoding = encoding,
        )
    }

    private data class FormatChunk(
        val channelCount: Int,
        val sampleRateHz: Int,
        val bitsPerSample: Int,
        val blockAlign: Long,
        val encoding: AudioSampleEncoding,
    )

    private companion object {
        const val WAVE_FORMAT_PCM = 0x0001
        const val WAVE_FORMAT_IEEE_FLOAT = 0x0003
        const val WAVE_FORMAT_EXTENSIBLE = 0xFFFE
    }
}

internal fun SeekableByteSource.readExact(position: Long, length: Int): ByteArray {
    require(length >= 0)
    val out = ByteArray(length)
    var filled = 0
    while (filled < length) {
        val read = readAt(position + filled, out, filled, length - filled)
        if (read <= 0) throw AudioCodecException("Unexpected end of audio file")
        filled += read
    }
    return out
}

internal fun ByteArray.asAscii(offset: Int, length: Int): String =
    buildString(length) {
        repeat(length) { append(this@asAscii[offset + it].toInt().and(0xFF).toChar()) }
    }

internal fun ByteArray.u16le(offset: Int): Int =
    (this[offset].toInt() and 0xFF) or ((this[offset + 1].toInt() and 0xFF) shl 8)

internal fun ByteArray.u32le(offset: Int): Long =
    (this[offset].toLong() and 0xFFL) or
        ((this[offset + 1].toLong() and 0xFFL) shl 8) or
        ((this[offset + 2].toLong() and 0xFFL) shl 16) or
        ((this[offset + 3].toLong() and 0xFFL) shl 24)

internal fun multiplyDivideFloor(value: Long, multiplier: Long, divisor: Long): Long {
    require(value >= 0 && multiplier >= 0 && divisor > 0)
    val quotient = value / divisor
    val remainder = value % divisor
    return Math.addExact(Math.multiplyExact(quotient, multiplier), Math.multiplyExact(remainder, multiplier) / divisor)
}
