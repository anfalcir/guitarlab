package studio.guitarlab.platform.separation

import java.io.File
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.security.MessageDigest
import studio.guitarlab.core.separation.RemoteJobIdentity
import studio.guitarlab.core.separation.RemoteStem
import studio.guitarlab.core.separation.RemoteStemPayload

internal class FileRemoteStemPayload(
    override val name: String,
    private val file: File,
) : RemoteStemPayload {
    override val byteCount: Long get() = file.length()
    override fun openStream(): InputStream = file.inputStream()
}

internal class RemoteStemStaging(private val root: File) {
    fun cached(identity: RemoteJobIdentity, stem: RemoteStem): FileRemoteStemPayload? {
        val target = target(identity, stem)
        if (!target.isFile) return null
        if (target.length() != stem.bytes || sha256(target) != stem.sha256) {
            target.delete()
            return null
        }
        return FileRemoteStemPayload(stem.name, target)
    }

    fun partial(identity: RemoteJobIdentity, stem: RemoteStem): File {
        val target = target(identity, stem)
        target.parentFile?.mkdirs()
        return File(target.parentFile, ".${target.name}.part").also { it.delete() }
    }

    fun commit(identity: RemoteJobIdentity, stem: RemoteStem, partial: File): FileRemoteStemPayload {
        require(partial.isFile && partial.length() == stem.bytes) { "RESULT_INVALID" }
        require(sha256(partial) == stem.sha256) { "RESULT_INVALID" }
        val target = target(identity, stem)
        try {
            Files.move(
                partial.toPath(),
                target.toPath(),
                StandardCopyOption.REPLACE_EXISTING,
                StandardCopyOption.ATOMIC_MOVE,
            )
        } catch (_: Exception) {
            Files.move(partial.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING)
        }
        return FileRemoteStemPayload(stem.name, target)
    }

    fun clear(identity: RemoteJobIdentity) {
        directory(identity).deleteRecursively()
    }

    fun bytes(identity: RemoteJobIdentity): Long =
        directory(identity).walkTopDown().filter { it.isFile && !it.name.endsWith(".part") }.sumOf { it.length() }

    private fun target(identity: RemoteJobIdentity, stem: RemoteStem): File {
        require(stem.name.matches(Regex("[a-z0-9_-]{1,32}"))) { "RESULT_INVALID" }
        return File(directory(identity), "${stem.name}.wav")
    }

    private fun directory(identity: RemoteJobIdentity): File =
        File(root, identity.jobId).also { it.mkdirs() }

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                if (read > 0) digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}
