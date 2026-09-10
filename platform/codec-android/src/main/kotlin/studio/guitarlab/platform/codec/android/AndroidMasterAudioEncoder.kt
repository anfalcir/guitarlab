package studio.guitarlab.platform.codec.android

import android.media.AudioFormat
import android.media.MediaCodec
import android.media.MediaFormat
import java.io.File
import java.io.FileOutputStream
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
                    encodeLoop(codec, decoder, destination)
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

    private fun encodeLoop(codec: MediaCodec, decoder: WavPcmDecoder, destination: File) {
        destination.parentFile?.mkdirs()
        FileOutputStream(destination).use { output ->
            val info = MediaCodec.BufferInfo()
            var inputEnded = false
            var outputEnded = false
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
                    MediaCodec.INFO_TRY_AGAIN_LATER, MediaCodec.INFO_OUTPUT_FORMAT_CHANGED, MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED -> Unit
                    else -> if (index >= 0) {
                        val encoded = codec.getOutputBuffer(index)
                        if (encoded != null && info.size > 0) {
                            val copy = encoded.duplicate()
                            copy.position(info.offset)
                            copy.limit(info.offset + info.size)
                            val bytes = ByteArray(copy.remaining())
                            copy.get(bytes)
                            output.write(bytes)
                        }
                        outputEnded = info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0
                        codec.releaseOutputBuffer(index, false)
                    }
                }
            }
            output.fd.sync()
        }
        require(destination.length() > 0L) { "Encoder produced an empty file." }
    }

    private const val TIMEOUT_US = 10_000L
}
