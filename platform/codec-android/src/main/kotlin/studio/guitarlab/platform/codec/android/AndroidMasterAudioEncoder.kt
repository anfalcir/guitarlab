package studio.guitarlab.platform.codec.android

import android.media.AudioFormat
import android.media.MediaCodec
import android.media.MediaFormat
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import studio.guitarlab.core.codec.AudioCodecException
import studio.guitarlab.core.codec.AudioEncodingTimeline
import studio.guitarlab.core.codec.FileSeekableByteSource
import studio.guitarlab.core.codec.WavPcmDecoder

enum class MasterExportFormat(val extension: String, val mimeType: String) {
    WAV_FLOAT32("wav", "audio/wav"),
    FLAC("flac", MediaFormat.MIMETYPE_AUDIO_FLAC),
    MP3("mp3", MediaFormat.MIMETYPE_AUDIO_MPEG),
}

/** Encodes a rendered float WAV to Android-supported lossless/lossy delivery formats. */
object AndroidMasterAudioEncoder {
    fun encode(renderedFloatWav: File, destination: File, format: MasterExportFormat) {
        require(format != MasterExportFormat.WAV_FLOAT32) { "WAV is already the render format." }
        FileSeekableByteSource(renderedFloatWav).use { source ->
            WavPcmDecoder(source).use { decoder ->
                val metadata = decoder.metadata
                require(metadata.channelCount in 1..2)
                val codec = try {
                    MediaCodec.createEncoderByType(format.mimeType)
                } catch (error: Throwable) {
                    throw AudioCodecException("Este dispositivo não oferece encoder ${format.name} compatível.", error)
                }
                try {
                    val mediaFormat = MediaFormat.createAudioFormat(format.mimeType, metadata.sampleRateHz, metadata.channelCount).apply {
                        setInteger(MediaFormat.KEY_PCM_ENCODING, AudioFormat.ENCODING_PCM_16BIT)
                        if (format == MasterExportFormat.MP3) setInteger(MediaFormat.KEY_BIT_RATE, 320_000)
                    }
                    codec.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
                    codec.start()
                    encodeLoop(codec, decoder, destination, format)
                } catch (error: Throwable) {
                    destination.delete()
                    if (error is AudioCodecException) throw error
                    throw AudioCodecException("Falha ao codificar ${format.name}.", error)
                } finally {
                    runCatching { codec.stop() }
                    codec.release()
                }
            }
        }
    }

    private fun encodeLoop(
        codec: MediaCodec,
        decoder: WavPcmDecoder,
        destination: File,
        format: MasterExportFormat,
    ) {
        destination.parentFile?.mkdirs()
        FileOutputStream(destination).use { output ->
            val info = MediaCodec.BufferInfo()
            var inputEnded = false
            var outputEnded = false
            var flacHeaderWritten = format != MasterExportFormat.FLAC
            val channels = decoder.metadata.channelCount
            while (!outputEnded) {
                if (!inputEnded) {
                    val index = codec.dequeueInputBuffer(TIMEOUT_US)
                    if (index >= 0) {
                        val buffer = codec.getInputBuffer(index) ?: error("Encoder input buffer unavailable")
                        buffer.clear()
                        buffer.order(ByteOrder.LITTLE_ENDIAN)
                        val maxFrames = buffer.remaining() / (channels * 2)
                        val floats = FloatArray(maxFrames * channels)
                        val startFrame = decoder.positionFrames
                        val frames = decoder.readInterleaved(floats, frameCount = maxFrames)
                        if (frames <= 0) {
                            val endPtsUs = AudioEncodingTimeline.presentationTimeUs(decoder.positionFrames, decoder.metadata.sampleRateHz)
                            codec.queueInputBuffer(index, 0, 0, endPtsUs, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            inputEnded = true
                        } else {
                            var sample = 0
                            val sampleCount = frames * channels
                            while (sample < sampleCount) {
                                val pcm = (floats[sample].coerceIn(-1f, 1f) * 32767f).toInt().coerceIn(-32768, 32767)
                                buffer.putShort(pcm.toShort())
                                sample++
                            }
                            val ptsUs = AudioEncodingTimeline.presentationTimeUs(startFrame, decoder.metadata.sampleRateHz)
                            codec.queueInputBuffer(index, 0, sampleCount * 2, ptsUs, 0)
                        }
                    }
                }

                when (val index = codec.dequeueOutputBuffer(info, TIMEOUT_US)) {
                    MediaCodec.INFO_TRY_AGAIN_LATER, MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED -> Unit
                    MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                        if (format == MasterExportFormat.FLAC && !flacHeaderWritten) {
                            val codecSpecificData = codec.outputFormat.getByteBuffer(CSD_0)
                                ?: throw AudioCodecException("Encoder FLAC não forneceu o cabeçalho STREAMINFO obrigatório.")
                            val headerBytes = codecSpecificData.toByteArrayPreservingPosition()
                            validateFlacHeader(headerBytes)
                            output.write(headerBytes)
                            flacHeaderWritten = true
                        }
                    }
                    else -> if (index >= 0) {
                        val encoded = codec.getOutputBuffer(index)
                        if (encoded != null && info.size > 0) {
                            val copy = encoded.duplicate()
                            copy.position(info.offset)
                            copy.limit(info.offset + info.size)
                            val bytes = copy.toByteArrayPreservingPosition()
                            val isCodecConfig = info.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0

                            if (format == MasterExportFormat.FLAC && isCodecConfig) {
                                // Legacy encoders may deliver FLAC codec-specific data as an output
                                // buffer instead of only exposing it in outputFormat/csd-0.
                                if (!flacHeaderWritten) {
                                    validateFlacHeader(bytes)
                                    output.write(bytes)
                                    flacHeaderWritten = true
                                }
                            } else {
                                if (format == MasterExportFormat.FLAC && !flacHeaderWritten) {
                                    throw AudioCodecException("Encoder FLAC produziu áudio antes do cabeçalho STREAMINFO obrigatório.")
                                }
                                output.write(bytes)
                            }
                        }
                        outputEnded = info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0
                        codec.releaseOutputBuffer(index, false)
                    }
                }
            }

            if (format == MasterExportFormat.FLAC && !flacHeaderWritten) {
                throw AudioCodecException("Encoder FLAC terminou sem fornecer o cabeçalho STREAMINFO obrigatório.")
            }
            output.fd.sync()
        }
        require(destination.length() > 0L) { "Encoder produced an empty file." }
    }

    private fun ByteBuffer.toByteArrayPreservingPosition(): ByteArray {
        val copy = duplicate()
        val bytes = ByteArray(copy.remaining())
        copy.get(bytes)
        return bytes
    }

    private fun validateFlacHeader(bytes: ByteArray) {
        if (bytes.size < FLAC_MINIMUM_HEADER_BYTES ||
            bytes[0] != 'f'.code.toByte() ||
            bytes[1] != 'L'.code.toByte() ||
            bytes[2] != 'a'.code.toByte() ||
            bytes[3] != 'C'.code.toByte()
        ) {
            throw AudioCodecException("Encoder FLAC forneceu codec-specific data sem marcador fLaC válido.")
        }

        val metadataType = bytes[4].toInt() and 0x7F
        val metadataLength = ((bytes[5].toInt() and 0xFF) shl 16) or
            ((bytes[6].toInt() and 0xFF) shl 8) or
            (bytes[7].toInt() and 0xFF)
        if (metadataType != FLAC_STREAMINFO_TYPE || metadataLength != FLAC_STREAMINFO_BYTES || bytes.size < 8 + metadataLength) {
            throw AudioCodecException("Encoder FLAC forneceu STREAMINFO inválido.")
        }
    }

    private const val TIMEOUT_US = 10_000L
    private const val CSD_0 = "csd-0"
    private const val FLAC_STREAMINFO_TYPE = 0
    private const val FLAC_STREAMINFO_BYTES = 34
    private const val FLAC_MINIMUM_HEADER_BYTES = 8 + FLAC_STREAMINFO_BYTES
}
