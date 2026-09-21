package studio.guitarlab.platform.separation

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.MessageDigest
import kotlin.math.abs
import studio.guitarlab.core.project.StemSetProjectPublisher
import studio.guitarlab.core.project.StemSetPublicationRequest
import studio.guitarlab.core.project.ValidatedStem
import studio.guitarlab.core.separation.*

class ManagedStemSetPublisher(private val delegate: StemSetProjectPublisher) : RemoteStemPublisher {
    override fun publish(identity: RemoteJobIdentity, manifest: RemoteResultManifest, manifestSha256: String, stems: Map<String, ByteArray>): Boolean {
        val validated = manifest.stems.map { entry ->
            val bytes = requireNotNull(stems[entry.name])
            require(bytes.size.toLong() == entry.bytes && sha(bytes) == entry.sha256) { "stem integrity mismatch: ${entry.name}" }
            val wav = WavStructure.read(bytes)
            require(wav.sampleRate == manifest.sampleRate && wav.channels == manifest.channels) { "stem audio contract mismatch: ${entry.name}" }
            require(abs(wav.frames - manifest.frames) <= 2L) { "stem duration mismatch: ${entry.name}" }
            ValidatedStem(entry.name, bytes, entry.sha256, wav.sampleRate, wav.channels, wav.frames)
        }
        return delegate.publish(StemSetPublicationRequest(identity.projectId, identity.jobId, identity.sourceAssetId, identity.inputSha256, manifestSha256, manifest.engine, manifest.model, manifest.modelSha256, validated))
    }
    private fun sha(bytes: ByteArray) = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
}

internal data class WavStructure(val sampleRate: Int, val channels: Int, val frames: Long) {
    companion object {
        fun read(bytes: ByteArray): WavStructure {
            require(bytes.size >= 44 && bytes.copyOfRange(0, 4).toString(Charsets.US_ASCII) == "RIFF" && bytes.copyOfRange(8, 12).toString(Charsets.US_ASCII) == "WAVE") { "invalid WAV" }
            var offset = 12; var rate = 0; var channels = 0; var alignment = 0; var dataBytes = -1
            while (offset + 8 <= bytes.size) {
                val id = bytes.copyOfRange(offset, offset + 4).toString(Charsets.US_ASCII)
                val size = ByteBuffer.wrap(bytes, offset + 4, 4).order(ByteOrder.LITTLE_ENDIAN).int
                require(size >= 0 && offset + 8L + size <= bytes.size) { "truncated WAV" }
                if (id == "fmt ") {
                    require(size >= 16); val chunk = ByteBuffer.wrap(bytes, offset + 8, size).order(ByteOrder.LITTLE_ENDIAN)
                    val format = chunk.short.toInt() and 0xffff; require(format == 1 || format == 3)
                    channels = chunk.short.toInt() and 0xffff; rate = chunk.int; chunk.int; alignment = chunk.short.toInt() and 0xffff
                } else if (id == "data") dataBytes = size
                offset += 8 + size + (size and 1)
            }
            require(rate > 0 && channels > 0 && alignment > 0 && dataBytes >= 0 && dataBytes % alignment == 0)
            return WavStructure(rate, channels, dataBytes.toLong() / alignment)
        }
    }
}
