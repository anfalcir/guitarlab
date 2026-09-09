package studio.guitarlab.platform.codec.android

import android.content.Context
import android.media.AudioFormat
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import java.io.Closeable
import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.pow
import studio.guitarlab.core.codec.AudioCodecException
import studio.guitarlab.core.codec.AudioImportFormat

/**
 * Normalizes external audio into an immutable PCM16 WAV before it enters the project media store.
 * This deliberately keeps playback, waveform, trim and recording on the already-validated WAV path.
 */
object AndroidAudioImportTranscoder {
    data class PreparedAudio(
        val wavFile: File,
        val originalFormat: AudioImportFormat,
    ) : Closeable {
        override fun close() {
            runCatching { wavFile.delete() }
        }
    }

    fun prepare(
        context: Context,
        uri: Uri,
        originalFormat: AudioImportFormat,
    ): PreparedAudio {
        val output = File.createTempFile("guitarlab-import-", ".wav", context.cacheDir)
        try {
            when (originalFormat) {
                AudioImportFormat.WAV_PCM -> copyUri(context, uri, output)
                AudioImportFormat.AIFF -> transcodeAiff(context, uri, output)
                AudioImportFormat.FLAC,
                AudioImportFormat.MP3,
                AudioImportFormat.AAC_M4A,
                AudioImportFormat.OGG_VORBIS,
                AudioImportFormat.OPUS,
                -> decodeWithAndroidMediaCodec(context, uri, output)
            }
            require(output.length() > WAV_HEADER_BYTES) { "O arquivo decodificado não contém áudio utilizável." }
            return PreparedAudio(output, originalFormat)
        } catch (error: Throwable) {
            output.delete()
            if (error is AudioCodecException) throw error
            throw AudioCodecException(
                "Não foi possível decodificar ${originalFormat.displayName}: ${error.message ?: "erro desconhecido"}",
                error,
            )
        }
    }

    private fun copyUri(context: Context, uri: Uri, output: File) {
        context.contentResolver.openInputStream(uri)?.use { input ->
            output.outputStream().buffered().use { target -> input.copyTo(target) }
        } ?: throw AudioCodecException("O Android não conseguiu abrir o arquivo selecionado.")
    }

    private fun decodeWithAndroidMediaCodec(context: Context, uri: Uri, output: File) {
        val extractor = MediaExtractor()
        var codec: MediaCodec? = null
        try {
            extractor.setDataSource(context, uri, null)
            val trackIndex = (0 until extractor.trackCount).firstOrNull { index ->
                extractor.getTrackFormat(index).getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true
            } ?: throw AudioCodecException("O arquivo não contém uma faixa de áudio compatível.")

            extractor.selectTrack(trackIndex)
            val inputFormat = extractor.getTrackFormat(trackIndex)
            val mime = inputFormat.getString(MediaFormat.KEY_MIME)
                ?: throw AudioCodecException("O Android não informou o codec da faixa de áudio.")
            inputFormat.setInteger(MediaFormat.KEY_PCM_ENCODING, AudioFormat.ENCODING_PCM_16BIT)

            codec = MediaCodec.createDecoderByType(mime)
            codec.configure(inputFormat, null, null, 0)
            codec.start()

            RandomAccessFile(output, "rw").use { wav ->
                wav.setLength(0L)
                repeat(WAV_HEADER_BYTES) { wav.write(0) }

                var inputEnded = false
                var outputEnded = false
                var sampleRate = inputFormat.intOrNull(MediaFormat.KEY_SAMPLE_RATE) ?: 0
                var channelCount = inputFormat.intOrNull(MediaFormat.KEY_CHANNEL_COUNT) ?: 0
                var pcmEncoding = AudioFormat.ENCODING_PCM_16BIT
                var pcmBytes = 0L
                val info = MediaCodec.BufferInfo()

                while (!outputEnded) {
                    if (!inputEnded) {
                        val inputIndex = codec.dequeueInputBuffer(CODEC_TIMEOUT_US)
                        if (inputIndex >= 0) {
                            val inputBuffer = codec.getInputBuffer(inputIndex)
                                ?: throw AudioCodecException("O decoder não forneceu buffer de entrada.")
                            val size = extractor.readSampleData(inputBuffer, 0)
                            if (size < 0) {
                                codec.queueInputBuffer(
                                    inputIndex,
                                    0,
                                    0,
                                    0L,
                                    MediaCodec.BUFFER_FLAG_END_OF_STREAM,
                                )
                                inputEnded = true
                            } else {
                                codec.queueInputBuffer(
                                    inputIndex,
                                    0,
                                    size,
                                    extractor.sampleTime.coerceAtLeast(0L),
                                    0,
                                )
                                extractor.advance()
                            }
                        }
                    }

                    when (val outputIndex = codec.dequeueOutputBuffer(info, CODEC_TIMEOUT_US)) {
                        MediaCodec.INFO_TRY_AGAIN_LATER -> Unit
                        MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                            val decodedFormat = codec.outputFormat
                            sampleRate = decodedFormat.intOrNull(MediaFormat.KEY_SAMPLE_RATE) ?: sampleRate
                            channelCount = decodedFormat.intOrNull(MediaFormat.KEY_CHANNEL_COUNT) ?: channelCount
                            pcmEncoding = decodedFormat.intOrNull(MediaFormat.KEY_PCM_ENCODING)
                                ?: AudioFormat.ENCODING_PCM_16BIT
                        }
                        else -> if (outputIndex >= 0) {
                            val outputBuffer = codec.getOutputBuffer(outputIndex)
                            if (outputBuffer != null && info.size > 0 && info.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG == 0) {
                                pcmBytes += writePcm16(outputBuffer, info, pcmEncoding, wav)
                            }
                            outputEnded = info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0
                            codec.releaseOutputBuffer(outputIndex, false)
                        }
                    }
                }

                require(sampleRate > 0) { "Sample rate inválido após decodificação." }
                require(channelCount in 1..2) { "A importação atual suporta áudio mono ou estéreo; recebido: $channelCount canais." }
                require(pcmBytes > 0) { "O decoder não produziu amostras de áudio." }
                finalizeWavHeader(wav, channelCount, sampleRate, pcmBytes)
            }
        } catch (error: Throwable) {
            if (error is AudioCodecException) throw error
            throw AudioCodecException("Falha no decoder de áudio do Android.", error)
        } finally {
            runCatching { codec?.stop() }
            runCatching { codec?.release() }
            runCatching { extractor.release() }
        }
    }

    private fun writePcm16(
        sourceBuffer: ByteBuffer,
        info: MediaCodec.BufferInfo,
        pcmEncoding: Int,
        output: RandomAccessFile,
    ): Long {
        val buffer = sourceBuffer.duplicate().order(ByteOrder.LITTLE_ENDIAN)
        buffer.position(info.offset)
        buffer.limit(info.offset + info.size)
        return when (pcmEncoding) {
            AudioFormat.ENCODING_PCM_16BIT -> {
                val bytes = ByteArray(buffer.remaining())
                buffer.get(bytes)
                output.write(bytes)
                bytes.size.toLong()
            }
            AudioFormat.ENCODING_PCM_FLOAT -> {
                var written = 0L
                while (buffer.remaining() >= 4) {
                    val sample = buffer.float.coerceIn(-1f, 1f)
                    val pcm = (sample * 32767f).toInt().coerceIn(-32768, 32767)
                    writeLe16(output, pcm)
                    written += 2L
                }
                written
            }
            else -> throw AudioCodecException("Encoding PCM de saída não suportado pelo decoder: $pcmEncoding")
        }
    }

    private fun transcodeAiff(context: Context, uri: Uri, output: File) {
        val inputFile = File.createTempFile("guitarlab-aiff-", ".aiff", context.cacheDir)
        try {
            copyUri(context, uri, inputFile)
            RandomAccessFile(inputFile, "r").use { input ->
                require(readFourCc(input) == "FORM") { "Arquivo AIFF inválido: FORM ausente." }
                readBeU32(input) // FORM size
                val formType = readFourCc(input)
                require(formType == "AIFF" || formType == "AIFC") { "Formato AIFF/AIFC inválido." }

                var channels = 0
                var frames = 0L
                var bits = 0
                var sampleRate = 0
                var littleEndian = false
                var audioOffset = -1L
                var audioBytes = -1L

                while (input.filePointer + 8L <= input.length()) {
                    val chunkId = readFourCc(input)
                    val chunkSize = readBeU32(input)
                    val chunkStart = input.filePointer
                    when (chunkId) {
                        "COMM" -> {
                            require(chunkSize >= 18L) { "Chunk COMM incompleto." }
                            channels = input.readUnsignedShort()
                            frames = readBeU32(input)
                            bits = input.readUnsignedShort()
                            sampleRate = readExtended80(input).toInt()
                            if (formType == "AIFC" && chunkSize >= 22L) {
                                val compression = readFourCc(input)
                                require(compression == "NONE" || compression == "twos" || compression == "sowt") {
                                    "AIFC comprimido ($compression) não é suportado pelo caminho PCM."
                                }
                                littleEndian = compression == "sowt"
                            }
                        }
                        "SSND" -> {
                            require(chunkSize >= 8L) { "Chunk SSND incompleto." }
                            val offset = readBeU32(input)
                            readBeU32(input) // block size
                            audioOffset = input.filePointer + offset
                            audioBytes = (chunkSize - 8L - offset).coerceAtLeast(0L)
                        }
                    }
                    input.seek((chunkStart + chunkSize + (chunkSize and 1L)).coerceAtMost(input.length()))
                }

                require(channels in 1..2) { "A importação atual suporta AIFF mono ou estéreo; recebido: $channels canais." }
                require(frames > 0L && audioOffset >= 0L && audioBytes > 0L) { "AIFF sem áudio PCM completo." }
                require(bits in setOf(8, 16, 24, 32)) { "Bit depth AIFF não suportado: $bits-bit." }
                require(sampleRate > 0) { "Sample rate AIFF inválido." }

                RandomAccessFile(output, "rw").use { wav ->
                    wav.setLength(0L)
                    repeat(WAV_HEADER_BYTES) { wav.write(0) }
                    input.seek(audioOffset)
                    val bytesPerSample = (bits + 7) / 8
                    val samples = minOf(frames * channels, audioBytes / bytesPerSample)
                    repeatLong(samples) {
                        val sample16 = readAiffSampleAsPcm16(input, bits, littleEndian)
                        writeLe16(wav, sample16)
                    }
                    val pcmBytes = samples * 2L
                    require(pcmBytes > 0L) { "AIFF não produziu amostras PCM." }
                    finalizeWavHeader(wav, channels, sampleRate, pcmBytes)
                }
            }
        } finally {
            inputFile.delete()
        }
    }

    private fun readAiffSampleAsPcm16(input: RandomAccessFile, bits: Int, littleEndian: Boolean): Int {
        return when (bits) {
            8 -> input.readByte().toInt() shl 8
            16 -> {
                val a = input.readUnsignedByte()
                val b = input.readUnsignedByte()
                val raw = if (littleEndian) (b shl 8) or a else (a shl 8) or b
                raw.toShort().toInt()
            }
            24 -> {
                val b0 = input.readUnsignedByte()
                val b1 = input.readUnsignedByte()
                val b2 = input.readUnsignedByte()
                val raw = if (littleEndian) (b2 shl 16) or (b1 shl 8) or b0 else (b0 shl 16) or (b1 shl 8) or b2
                val signed = if (raw and 0x800000 != 0) raw or -0x1000000 else raw
                signed shr 8
            }
            32 -> {
                val b0 = input.readUnsignedByte()
                val b1 = input.readUnsignedByte()
                val b2 = input.readUnsignedByte()
                val b3 = input.readUnsignedByte()
                val raw = if (littleEndian) {
                    (b3 shl 24) or (b2 shl 16) or (b1 shl 8) or b0
                } else {
                    (b0 shl 24) or (b1 shl 16) or (b2 shl 8) or b3
                }
                raw shr 16
            }
            else -> error("unsupported AIFF bit depth")
        }
    }

    private fun finalizeWavHeader(wav: RandomAccessFile, channels: Int, sampleRate: Int, pcmBytes: Long) {
        require(pcmBytes <= 0xFFFF_FFFFL - 36L) { "Áudio grande demais para WAV RIFF clássico." }
        wav.seek(0L)
        wav.writeBytes("RIFF")
        writeLe32(wav, 36L + pcmBytes)
        wav.writeBytes("WAVE")
        wav.writeBytes("fmt ")
        writeLe32(wav, 16L)
        writeLe16(wav, 1)
        writeLe16(wav, channels)
        writeLe32(wav, sampleRate.toLong())
        val byteRate = sampleRate.toLong() * channels * 2L
        writeLe32(wav, byteRate)
        writeLe16(wav, channels * 2)
        writeLe16(wav, 16)
        wav.writeBytes("data")
        writeLe32(wav, pcmBytes)
    }

    private fun MediaFormat.intOrNull(key: String): Int? =
        if (containsKey(key)) runCatching { getInteger(key) }.getOrNull() else null

    private fun readFourCc(input: RandomAccessFile): String {
        val bytes = ByteArray(4)
        input.readFully(bytes)
        return bytes.toString(Charsets.US_ASCII)
    }

    private fun readBeU32(input: RandomAccessFile): Long = input.readInt().toLong() and 0xFFFF_FFFFL

    private fun readExtended80(input: RandomAccessFile): Double {
        val exponentWord = input.readUnsignedShort()
        val negative = exponentWord and 0x8000 != 0
        val exponent = exponentWord and 0x7FFF
        val highMantissa = readBeU32(input)
        val lowMantissa = readBeU32(input)
        if (exponent == 0 && highMantissa == 0L && lowMantissa == 0L) return 0.0
        val mantissa = highMantissa * 4294967296.0 + lowMantissa.toDouble()
        val value = mantissa * 2.0.pow(exponent - 16383 - 63)
        return if (negative) -value else value
    }

    private fun writeLe16(output: RandomAccessFile, value: Int) {
        output.write(value and 0xFF)
        output.write((value ushr 8) and 0xFF)
    }

    private fun writeLe32(output: RandomAccessFile, value: Long) {
        output.write((value and 0xFF).toInt())
        output.write(((value ushr 8) and 0xFF).toInt())
        output.write(((value ushr 16) and 0xFF).toInt())
        output.write(((value ushr 24) and 0xFF).toInt())
    }

    private inline fun repeatLong(count: Long, block: (Long) -> Unit) {
        var index = 0L
        while (index < count) {
            block(index)
            index++
        }
    }

    private const val WAV_HEADER_BYTES = 44L
    private const val CODEC_TIMEOUT_US = 10_000L
}
