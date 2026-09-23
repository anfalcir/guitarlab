package studio.guitarlab.platform.separation

import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.MessageDigest
import kotlin.math.abs
import studio.guitarlab.core.model.AssetRole
import studio.guitarlab.core.project.PreparedReferenceProjectPublisher
import studio.guitarlab.core.project.PreparedReferencePublicationRequest
import studio.guitarlab.core.project.StemSetProjectPublisher
import studio.guitarlab.core.project.StemSetPublicationRequest
import studio.guitarlab.core.project.ValidatedPreparedReference
import studio.guitarlab.core.project.ValidatedStem
import studio.guitarlab.core.separation.RemoteJobIdentity
import studio.guitarlab.core.separation.RemoteReference
import studio.guitarlab.core.separation.RemoteResultManifest
import studio.guitarlab.core.separation.RemoteStemPayload

class ManagedStemSetPublisher(
    private val delegate: StemSetProjectPublisher,
    private val preparedDelegate: PreparedReferenceProjectPublisher? = null,
) : studio.guitarlab.core.separation.RemoteStemPublisher {
    override fun publish(
        identity: RemoteJobIdentity,
        manifest: RemoteResultManifest,
        manifestSha256: String,
        stems: Map<String, RemoteStemPayload>,
    ): Boolean {
        val validated = manifest.stems.map { entry ->
            val payload = requireNotNull(stems[entry.name])
            require(payload.byteCount == entry.bytes && sha(payload.openStream()) == entry.sha256) {
                "stem integrity mismatch: ${entry.name}"
            }
            val wav = payload.openStream().use(WavStructure::read)
            require(wav.sampleRate == manifest.sampleRate && wav.channels == manifest.channels) {
                "stem audio contract mismatch: ${entry.name}"
            }
            require(abs(wav.frames - manifest.frames) <= 2L) {
                "stem duration mismatch: ${entry.name}"
            }
            ValidatedStem(
                name = entry.name,
                byteCount = payload.byteCount,
                sha256 = entry.sha256,
                sampleRate = wav.sampleRate,
                channels = wav.channels,
                frames = wav.frames,
                openStream = payload::openStream,
            )
        }
        return delegate.publish(
            StemSetPublicationRequest(
                identity.projectId,
                identity.jobId,
                identity.sourceAssetId,
                identity.inputSha256,
                manifestSha256,
                manifest.engine,
                manifest.model,
                manifest.modelSha256,
                validated,
            ),
        )
    }

    override fun publishReferences(
        identity: RemoteJobIdentity,
        manifest: RemoteResultManifest,
        manifestSha256: String,
        references: Map<String, RemoteStemPayload>,
    ): Boolean {
        require(manifest.schemaVersion == 2) { "prepared reference publisher requires manifest v2" }
        val recipe = requireNotNull(manifest.referenceRecipe)
        val validated = manifest.deliverables.map { entry ->
            val payload = requireNotNull(references[entry.name])
            require(payload.byteCount == entry.bytes && sha(payload.openStream()) == entry.sha256) {
                "prepared reference integrity mismatch: ${entry.name}"
            }
            val wav = payload.openStream().use(WavStructure::read)
            require(
                wav.sampleRate == entry.sampleRate &&
                    wav.channels == entry.channels &&
                    wav.frames == entry.frames &&
                    wav.sampleRate == manifest.sampleRate &&
                    wav.channels == manifest.channels &&
                    wav.frames == manifest.frames,
            ) { "prepared reference audio contract mismatch: ${entry.name}" }
            ValidatedPreparedReference(
                name = entry.name,
                role = role(entry),
                byteCount = payload.byteCount,
                sha256 = entry.sha256,
                sampleRate = wav.sampleRate,
                channels = wav.channels,
                frames = wav.frames,
                openStream = payload::openStream,
            )
        }
        return requireNotNull(preparedDelegate) { "prepared reference delegate is not configured" }.publish(
            PreparedReferencePublicationRequest(
                projectId = identity.projectId,
                jobId = identity.jobId,
                sourceAssetId = identity.sourceAssetId,
                sourceSha256 = identity.inputSha256,
                manifestSha256 = manifestSha256,
                engine = manifest.engine,
                model = manifest.model,
                modelSha256 = manifest.modelSha256,
                recipeVersion = recipe.version,
                targetPeakDbfs = recipe.targetPeakDbfs,
                sharedGainDb = recipe.sharedGainDb,
                references = validated,
            ),
        )
    }

    private fun role(reference: RemoteReference): AssetRole = when (reference.role) {
        "REFERENCE_BACKING" -> AssetRole.REFERENCE_BACKING
        "REFERENCE_GUITAR" -> AssetRole.REFERENCE_GUITAR
        else -> error("unsupported prepared reference role: ${reference.role}")
    }


    private fun sha(input: InputStream): String {
        val digest = MessageDigest.getInstance("SHA-256")
        input.use { stream ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val read = stream.read(buffer)
                if (read < 0) break
                if (read > 0) digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}

internal data class WavStructure(val sampleRate: Int, val channels: Int, val frames: Long) {
    companion object {
        fun read(input: InputStream): WavStructure {
            val header = ByteArray(12)
            require(readFully(input, header)) { "truncated WAV" }
            require(
                header.copyOfRange(0, 4).toString(Charsets.US_ASCII) == "RIFF" &&
                    header.copyOfRange(8, 12).toString(Charsets.US_ASCII) == "WAVE",
            ) { "invalid WAV" }

            var rate = 0
            var channels = 0
            var alignment = 0
            var dataBytes = -1L

            while (true) {
                val chunkHeader = ByteArray(8)
                if (!readFully(input, chunkHeader)) break
                val id = chunkHeader.copyOfRange(0, 4).toString(Charsets.US_ASCII)
                val size = ByteBuffer.wrap(chunkHeader, 4, 4).order(ByteOrder.LITTLE_ENDIAN).int
                require(size >= 0) { "invalid WAV chunk" }

                if (id == "fmt ") {
                    require(size in 16..1_048_576) { "invalid WAV fmt chunk" }
                    val chunk = ByteArray(size)
                    require(readFully(input, chunk)) { "truncated WAV" }
                    val buffer = ByteBuffer.wrap(chunk).order(ByteOrder.LITTLE_ENDIAN)
                    val format = buffer.short.toInt() and 0xffff
                    require(format == 1 || format == 3)
                    channels = buffer.short.toInt() and 0xffff
                    rate = buffer.int
                    buffer.int
                    alignment = buffer.short.toInt() and 0xffff
                } else if (id == "data") {
                    dataBytes = size.toLong()
                    if (rate > 0 && channels > 0 && alignment > 0) break
                    skipFully(input, size.toLong())
                } else {
                    skipFully(input, size.toLong())
                }

                if ((size and 1) != 0) skipFully(input, 1)
            }

            require(rate > 0 && channels > 0 && alignment > 0 && dataBytes >= 0 && dataBytes % alignment == 0L)
            return WavStructure(rate, channels, dataBytes / alignment)
        }

        private fun readFully(input: InputStream, bytes: ByteArray): Boolean {
            var offset = 0
            while (offset < bytes.size) {
                val read = input.read(bytes, offset, bytes.size - offset)
                if (read < 0) return false
                if (read > 0) offset += read
            }
            return true
        }

        private fun skipFully(input: InputStream, count: Long) {
            var remaining = count
            val buffer = ByteArray(8192)
            while (remaining > 0) {
                val skipped = input.skip(remaining)
                if (skipped > 0) {
                    remaining -= skipped
                    continue
                }
                val read = input.read(buffer, 0, minOf(buffer.size.toLong(), remaining).toInt())
                require(read >= 0) { "truncated WAV" }
                remaining -= read
            }
        }
    }
}
