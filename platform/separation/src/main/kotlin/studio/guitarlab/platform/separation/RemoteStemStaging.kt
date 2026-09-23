package studio.guitarlab.platform.separation

import java.io.File
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.security.MessageDigest
import studio.guitarlab.core.separation.RemoteJobIdentity
import studio.guitarlab.core.separation.RemoteReference
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
    fun cached(identity: RemoteJobIdentity, stem: RemoteStem): FileRemoteStemPayload? =
        cached(identity, stem.name, stem.bytes, stem.sha256)

    fun cached(identity: RemoteJobIdentity, reference: RemoteReference): FileRemoteStemPayload? =
        cached(identity, reference.name, reference.bytes, reference.sha256)

    fun partial(identity: RemoteJobIdentity, stem: RemoteStem): File =
        partial(identity, stem.name)

    fun partial(identity: RemoteJobIdentity, reference: RemoteReference): File =
        partial(identity, reference.name)

    fun commit(identity: RemoteJobIdentity, stem: RemoteStem, partial: File): FileRemoteStemPayload =
        commit(identity, stem.name, stem.bytes, stem.sha256, partial)

    fun commit(identity: RemoteJobIdentity, reference: RemoteReference, partial: File): FileRemoteStemPayload =
        commit(identity, reference.name, reference.bytes, reference.sha256, partial)

    private fun cached(identity: RemoteJobIdentity, name: String, bytes: Long, sha256: String): FileRemoteStemPayload? {
        val target = target(identity, name)
        if (!target.isFile) return null
        if (target.length() != bytes || sha256(target) != sha256) {
            target.delete()
            return null
        }
        return FileRemoteStemPayload(name, target)
    }

    private fun partial(identity: RemoteJobIdentity, name: String): File {
        val target = target(identity, name)
        target.parentFile?.mkdirs()
        return File(target.parentFile, ".${target.name}.part").also { it.delete() }
    }

    private fun commit(
        identity: RemoteJobIdentity,
        name: String,
        bytes: Long,
        expectedSha256: String,
        partial: File,
    ): FileRemoteStemPayload {
        require(partial.isFile && partial.length() == bytes) { "RESULT_INVALID" }
        require(sha256(partial) == expectedSha256) { "RESULT_INVALID" }
        val target = target(identity, name)
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
        return FileRemoteStemPayload(name, target)
    }

    fun clear(identity: RemoteJobIdentity) {
        directory(identity).deleteRecursively()
    }

    fun bytes(identity: RemoteJobIdentity): Long =
        directory(identity).walkTopDown().filter { it.isFile && !it.name.endsWith(".part") }.sumOf { it.length() }

    private fun target(identity: RemoteJobIdentity, name: String): File {
        require(name.matches(Regex("[a-z0-9_-]{1,32}"))) { "RESULT_INVALID" }
        return File(directory(identity), "$name.wav")
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
