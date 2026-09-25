package studio.guitarlab.core.project

import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.security.MessageDigest
import studio.guitarlab.core.codec.WaveformEnvelope
import studio.guitarlab.core.model.AudioClip

/**
 * Immutable identity of the exact media window represented by one waveform cache entry.
 *
 * The cache is intentionally clip-addressed on disk for cheap lookup, but validity is content/window
 * addressed. Reusing a clip id with a new prepared asset, trimming/splitting a clip, changing proxy
 * media or changing waveform resolution invalidates the entry without scanning/rehashing the WAV.
 */
data class WaveformCacheIdentity(
    val mediaKey: String,
    val sourceStartFrame: Long,
    val lengthFrames: Long,
    val sampleRateHz: Int?,
    val totalFrames: Long?,
    val channelCount: Int?,
    val targetPoints: Int,
    val algorithmVersion: Int = CURRENT_ALGORITHM_VERSION,
) {
    init {
        require(mediaKey.isNotBlank()) { "mediaKey must not be blank" }
        require(sourceStartFrame >= 0L) { "sourceStartFrame must be non-negative" }
        require(lengthFrames >= 0L) { "lengthFrames must be non-negative" }
        require(targetPoints > 0) { "targetPoints must be positive" }
    }

    val fingerprint: String by lazy(LazyThreadSafetyMode.NONE) {
        val canonical = buildString {
            append("waveform-cache-v2\n")
            append("algorithm=").append(algorithmVersion).append('\n')
            append("media=").append(mediaKey).append('\n')
            append("start=").append(sourceStartFrame).append('\n')
            append("length=").append(lengthFrames).append('\n')
            append("rate=").append(sampleRateHz ?: -1).append('\n')
            append("total=").append(totalFrames ?: -1).append('\n')
            append("channels=").append(channelCount ?: -1).append('\n')
            append("points=").append(targetPoints)
        }
        MessageDigest.getInstance("SHA-256")
            .digest(canonical.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
    }

    companion object {
        const val CURRENT_ALGORITHM_VERSION = 2

        fun forClip(clip: AudioClip, targetPoints: Int): WaveformCacheIdentity {
            val managedPath = clip.managedEditProxyPath ?: clip.managedSourcePath
            val mediaKey = listOfNotNull(
                clip.sourceUri.takeIf { it.isNotBlank() },
                managedPath?.takeIf { it.isNotBlank() },
            ).joinToString("|").ifBlank { "clip:${clip.id}" }
            return WaveformCacheIdentity(
                mediaKey = mediaKey,
                sourceStartFrame = clip.sourceStartFrame,
                lengthFrames = clip.lengthFrames,
                sampleRateHz = clip.editingSampleRateHz ?: clip.sourceSampleRateHz,
                totalFrames = clip.editingTotalFrames ?: clip.sourceTotalFrames,
                channelCount = clip.sourceChannelCount,
                targetPoints = targetPoints,
            )
        }
    }
}

class WaveformCacheStore(private val rootDirectory: File) {
    fun write(projectId: String, clipId: String, identity: WaveformCacheIdentity, envelope: WaveformEnvelope) {
        require(envelope.peaks.size <= MAX_POINTS) { "Waveform cache point count exceeds limit." }
        require(envelope.channelPeaks.all { it.size <= MAX_POINTS }) { "Waveform cache channel point count exceeds limit." }
        val destination = cacheFile(projectId, clipId).also { it.parentFile?.mkdirs() }
        val temporary = File(destination.parentFile, ".${destination.name}.part")
        try {
            DataOutputStream(temporary.outputStream().buffered()).use { output ->
                output.writeInt(MAGIC_V2)
                output.writeUTF(identity.fingerprint)
                output.writeInt(envelope.peaks.size)
                envelope.peaks.forEach(output::writeFloat)
                output.writeInt(envelope.channelPeaks.size)
                envelope.channelPeaks.forEach { channel ->
                    output.writeInt(channel.size)
                    channel.forEach(output::writeFloat)
                }
            }
            try {
                Files.move(
                    temporary.toPath(),
                    destination.toPath(),
                    StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE,
                )
            } catch (_: Exception) {
                Files.move(temporary.toPath(), destination.toPath(), StandardCopyOption.REPLACE_EXISTING)
            }
        } finally {
            temporary.delete()
        }
    }

    /**
     * Returns null for legacy v1 entries, malformed entries or an identity mismatch.
     * A caller can then regenerate exactly once and atomically replace the stale cache.
     */
    fun read(projectId: String, clipId: String, identity: WaveformCacheIdentity): WaveformEnvelope? = runCatching {
        val file = cacheFile(projectId, clipId)
        if (!file.isFile) return null
        DataInputStream(file.inputStream().buffered()).use { input ->
            if (input.readInt() != MAGIC_V2) return null
            if (input.readUTF() != identity.fingerprint) return null
            val count = input.readInt()
            require(count in 0..MAX_POINTS) { "Invalid waveform cache point count." }
            val peaks = List(count) { input.readFloat().coerceIn(0f, 1f) }
            val channels = input.readInt()
            require(channels in 0..2) { "Invalid waveform cache channel count." }
            val channelPeaks = List(channels) {
                val channelCount = input.readInt()
                require(channelCount in 0..MAX_POINTS) { "Invalid waveform cache channel point count." }
                List(channelCount) { input.readFloat().coerceIn(0f, 1f) }
            }
            WaveformEnvelope(peaks, channelPeaks)
        }
    }.getOrNull()

    fun remove(projectId: String, clipId: String) {
        cacheFile(projectId, clipId).delete()
    }

    /** Removes only regenerable waveform entries not referenced by the current project snapshot. */
    fun prune(projectId: String, retainedClipIds: Set<String>): Int {
        val retainedNames = retainedClipIds.mapTo(mutableSetOf()) { "${ManagedStorageKey.from(it)}.glwf" }
        val directory = cacheDirectory(projectId)
        var removed = 0
        directory.listFiles().orEmpty().forEach { file ->
            if (file.isFile && file.extension == "glwf" && file.name !in retainedNames && file.delete()) removed++
        }
        return removed
    }

    private fun cacheFile(projectId: String, clipId: String): File {
        val safeClip = ManagedStorageKey.from(clipId)
        return File(cacheDirectory(projectId), "$safeClip.glwf")
    }

    private fun cacheDirectory(projectId: String): File =
        File(rootDirectory, "projects/${ManagedStorageKey.from(projectId)}/media/derived/waveform")

    private companion object {
        const val MAGIC_V2 = 0x474C5732 // GLW2
        const val MAX_POINTS = 16_384
    }
}
