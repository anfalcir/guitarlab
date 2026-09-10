package studio.guitarlab.core.project

import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import studio.guitarlab.core.codec.WaveformEnvelope

class WaveformCacheStore(private val rootDirectory: File) {
    fun write(projectId: String, clipId: String, envelope: WaveformEnvelope) {
        val destination = cacheFile(projectId, clipId).also { it.parentFile?.mkdirs() }
        val temporary = File(destination.parentFile, ".${destination.name}.part")
        try {
            DataOutputStream(temporary.outputStream().buffered()).use { output ->
                output.writeInt(MAGIC)
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

    fun read(projectId: String, clipId: String): WaveformEnvelope? = runCatching {
        val file = cacheFile(projectId, clipId)
        if (!file.isFile) return null
        DataInputStream(file.inputStream().buffered()).use { input ->
            require(input.readInt() == MAGIC) { "Invalid waveform cache magic." }
            val count = input.readInt()
            require(count in 0..MAX_POINTS) { "Invalid waveform cache point count." }
            val peaks = List(count) { input.readFloat().coerceIn(0f, 1f) }
            val channelPeaks = if (input.available() >= Int.SIZE_BYTES) {
                val channels = input.readInt()
                require(channels in 0..2) { "Invalid waveform cache channel count." }
                List(channels) {
                    val channelCount = input.readInt()
                    require(channelCount in 0..MAX_POINTS) { "Invalid waveform cache channel point count." }
                    List(channelCount) { input.readFloat().coerceIn(0f, 1f) }
                }
            } else emptyList()
            WaveformEnvelope(peaks, channelPeaks)
        }
    }.getOrNull()

    fun remove(projectId: String, clipId: String) {
        cacheFile(projectId, clipId).delete()
    }

    /** Removes only regenerable waveform entries not referenced by the current project snapshot. */
    fun prune(projectId: String, retainedClipIds: Set<String>): Int {
        val retainedNames = retainedClipIds.mapTo(mutableSetOf()) { "${sanitize(it)}.glwf" }
        val directory = cacheDirectory(projectId)
        var removed = 0
        directory.listFiles().orEmpty().forEach { file ->
            if (file.isFile && file.extension == "glwf" && file.name !in retainedNames && file.delete()) removed++
        }
        return removed
    }

    private fun cacheFile(projectId: String, clipId: String): File {
        val safeProject = sanitize(projectId)
        val safeClip = sanitize(clipId)
        return File(cacheDirectory(projectId), "$safeClip.glwf")
    }

    private fun cacheDirectory(projectId: String): File =
        File(rootDirectory, "projects/${sanitize(projectId)}/media/derived/waveform")

    private fun sanitize(value: String): String = value.replace(Regex("[^A-Za-z0-9._-]"), "_")

    private companion object {
        const val MAGIC = 0x474C5746 // GLWF
        const val MAX_POINTS = 16_384
    }
}
