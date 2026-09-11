package studio.guitarlab.core.codec

import java.io.File
import java.io.RandomAccessFile

enum class FloatWavRecoveryStatus {
    NO_PAYLOAD,
    NO_COMPLETE_FRAME,
    NOT_CANONICAL_GUITARLAB_FLOAT_WAV,
    ALREADY_CONSISTENT,
    REPAIRED,
}

data class FloatWavRecoveryResult(
    val status: FloatWavRecoveryStatus,
    val framesRecovered: Long = 0,
    val trailingBytesDiscarded: Int = 0,
)

/**
 * Repairs the fixed 44-byte IEEE-float WAV layout emitted by [FloatWavFileWriter]
 * after an abrupt process death prevented finish() from publishing RIFF/data sizes.
 *
 * The repair is deliberately fail-closed: files that do not match GuitarLab's exact
 * recording header are never modified. At most one incomplete audio frame at the tail
 * is discarded, because it cannot represent a valid mono/stereo sample frame.
 */
object FloatWavRecovery {
    fun repairInterrupted(file: File): FloatWavRecoveryResult {
        if (!file.isFile || file.length() <= HEADER_BYTES) {
            return FloatWavRecoveryResult(FloatWavRecoveryStatus.NO_PAYLOAD)
        }

        return RandomAccessFile(file, "rw").use { output ->
            if (output.length() <= HEADER_BYTES) {
                return@use FloatWavRecoveryResult(FloatWavRecoveryStatus.NO_PAYLOAD)
            }
            val header = ByteArray(HEADER_BYTES.toInt())
            output.seek(0)
            output.readFully(header)
            val layout = canonicalLayout(header)
                ?: return@use FloatWavRecoveryResult(FloatWavRecoveryStatus.NOT_CANONICAL_GUITARLAB_FLOAT_WAV)

            val payloadBytes = output.length() - HEADER_BYTES
            val alignedDataBytes = payloadBytes - (payloadBytes % layout.blockAlign)
            if (alignedDataBytes <= 0L) {
                return@use FloatWavRecoveryResult(FloatWavRecoveryStatus.NO_COMPLETE_FRAME)
            }
            if (alignedDataBytes > RIFF_MAX_DATA_BYTES) {
                return@use FloatWavRecoveryResult(FloatWavRecoveryStatus.NOT_CANONICAL_GUITARLAB_FLOAT_WAV)
            }

            val trailingBytes = (payloadBytes - alignedDataBytes).toInt()
            val expectedRiffSize = 36L + alignedDataBytes
            val declaredRiffSize = header.u32le(4)
            val declaredDataSize = header.u32le(40)
            val needsRepair = trailingBytes > 0 ||
                declaredRiffSize != expectedRiffSize ||
                declaredDataSize != alignedDataBytes

            if (!needsRepair) {
                return@use FloatWavRecoveryResult(
                    status = FloatWavRecoveryStatus.ALREADY_CONSISTENT,
                    framesRecovered = alignedDataBytes / layout.blockAlign,
                )
            }

            if (trailingBytes > 0) output.setLength(HEADER_BYTES + alignedDataBytes)
            output.seek(4)
            writeLeInt(output, expectedRiffSize.toInt())
            output.seek(40)
            writeLeInt(output, alignedDataBytes.toInt())
            output.fd.sync()

            FloatWavRecoveryResult(
                status = FloatWavRecoveryStatus.REPAIRED,
                framesRecovered = alignedDataBytes / layout.blockAlign,
                trailingBytesDiscarded = trailingBytes,
            )
        }
    }

    private fun canonicalLayout(header: ByteArray): Layout? {
        if (header.asAscii(0, 4) != "RIFF" || header.asAscii(8, 4) != "WAVE") return null
        if (header.asAscii(12, 4) != "fmt " || header.u32le(16) != 16L) return null
        if (header.u16le(20) != WAVE_FORMAT_IEEE_FLOAT) return null
        val channels = header.u16le(22)
        val sampleRate = header.u32le(24)
        val byteRate = header.u32le(28)
        val blockAlign = header.u16le(32)
        val bitsPerSample = header.u16le(34)
        if (header.asAscii(36, 4) != "data") return null
        if (channels !in 1..2 || sampleRate !in 8_000L..384_000L || bitsPerSample != 32) return null
        val expectedBlockAlign = channels * Float.SIZE_BYTES
        if (blockAlign != expectedBlockAlign) return null
        if (byteRate != sampleRate * expectedBlockAlign) return null
        return Layout(blockAlign.toLong())
    }

    private fun writeLeInt(output: RandomAccessFile, value: Int) {
        output.write(value and 0xFF)
        output.write((value ushr 8) and 0xFF)
        output.write((value ushr 16) and 0xFF)
        output.write((value ushr 24) and 0xFF)
    }

    private data class Layout(val blockAlign: Long)

    private const val HEADER_BYTES = 44L
    private const val RIFF_MAX_DATA_BYTES = 0xFFFF_FFFFL - 36L
    private const val WAVE_FORMAT_IEEE_FLOAT = 0x0003
}
