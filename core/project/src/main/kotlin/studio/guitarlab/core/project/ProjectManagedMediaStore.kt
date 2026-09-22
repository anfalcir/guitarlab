package studio.guitarlab.core.project

import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.UUID

data class ManagedMediaAsset(
    val relativePath: String,
    val file: File,
    val byteCount: Long,
)

/**
 * Owns project-managed audio. Imported originals live under media/source and are immutable.
 * Derived PCM editing proxies live under media/proxy and may be regenerated from the source.
 * Timeline edits are metadata-only and never rewrite either file in place.
 */
class ProjectManagedMediaStore(
    private val rootDirectory: File,
    private val idFactory: () -> String = { UUID.randomUUID().toString() },
) {
    fun ingest(projectId: String, suggestedName: String, input: InputStream): ManagedMediaAsset =
        ingestInto(projectId, SOURCE_DIRECTORY, suggestedName, input)

    fun ingestEditProxy(projectId: String, suggestedName: String, input: InputStream): ManagedMediaAsset =
        ingestInto(projectId, PROXY_DIRECTORY, suggestedName, input)

    fun ingestStem(projectId: String, suggestedName: String, input: InputStream): ManagedMediaAsset =
        ingestInto(projectId, STEM_DIRECTORY, suggestedName, input)

    fun ingestReference(projectId: String, suggestedName: String, input: InputStream): ManagedMediaAsset =
        ingestInto(projectId, REFERENCE_DIRECTORY, suggestedName, input)

    fun resolve(projectId: String, relativePath: String): File = resolveManaged(projectId, relativePath, SOURCE_DIRECTORY)

    fun resolveEditable(projectId: String, relativePath: String): File =
        resolveManaged(projectId, relativePath, SOURCE_DIRECTORY, PROXY_DIRECTORY, REFERENCE_DIRECTORY)

    fun resolveAsset(projectId: String, relativePath: String): File =
        resolveManaged(projectId, relativePath, SOURCE_DIRECTORY, PROXY_DIRECTORY, STEM_DIRECTORY, REFERENCE_DIRECTORY)

    /** Only for rollback of an import transaction that failed before the project references it. */
    fun discardUncommitted(projectId: String, relativePath: String) {
        runCatching { resolveEditable(projectId, relativePath).delete() }
    }

    /** Removes only unreferenced staging files owned by one remote separation job. */
    fun discardAbandonedStemSet(projectId: String, jobId: String) {
        require(jobId.matches(Regex("[A-Za-z0-9-]{1,64}"))) { "Invalid remote job id." }
        val directory = File(projectDirectory(projectId), STEM_DIRECTORY)
        directory.listFiles { file -> file.isFile && file.name.contains("-$jobId-") }
            .orEmpty()
            .forEach { file -> runCatching { file.delete() } }
    }

    private fun ingestInto(projectId: String, directoryName: String, suggestedName: String, input: InputStream): ManagedMediaAsset {
        val projectDirectory = projectDirectory(projectId)
        val mediaDirectory = File(projectDirectory, directoryName).also { it.mkdirs() }
        val safeName = sanitizeFileName(suggestedName).ifBlank { "audio.bin" }
        val destination = File(mediaDirectory, "${idFactory()}-$safeName")
        val temporary = File(mediaDirectory, ".${destination.name}.part")

        try {
            var copied = 0L
            FileOutputStream(temporary).use { output ->
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                while (true) {
                    val read = input.read(buffer)
                    if (read < 0) break
                    if (read == 0) continue
                    output.write(buffer, 0, read)
                    copied += read
                }
                output.fd.sync()
            }
            require(copied > 0L) { "Managed audio asset is empty." }

            try {
                Files.move(temporary.toPath(), destination.toPath(), StandardCopyOption.ATOMIC_MOVE)
            } catch (_: Exception) {
                Files.move(temporary.toPath(), destination.toPath())
            }

            return ManagedMediaAsset(
                relativePath = "$directoryName/${destination.name}",
                file = destination,
                byteCount = copied,
            )
        } catch (error: Throwable) {
            temporary.delete()
            destination.delete()
            throw error
        }
    }

    private fun resolveManaged(projectId: String, relativePath: String, vararg allowedDirectories: String): File {
        require(allowedDirectories.any { relativePath.startsWith("$it/") }) { "Managed media path is outside the allowed media area." }
        require(!relativePath.contains("..")) { "Managed media path must not traverse directories." }
        val projectDirectory = projectDirectory(projectId).canonicalFile
        val candidate = File(projectDirectory, relativePath).canonicalFile
        require(candidate.path.startsWith(projectDirectory.path + File.separator)) { "Managed media escaped project storage." }
        require(candidate.isFile) { "Managed media file is missing: $relativePath" }
        return candidate
    }

    fun projectDirectoryForExport(projectId: String): File = projectDirectory(projectId)

    private fun projectDirectory(projectId: String): File =
        File(File(rootDirectory, "projects"), ManagedStorageKey.from(projectId)).also { it.mkdirs() }

    private fun sanitizeFileName(value: String): String = value
        .substringAfterLast('/')
        .substringAfterLast('\\')
        .replace(Regex("[^A-Za-z0-9._ -]"), "_")
        .take(120)

    private companion object {
        const val SOURCE_DIRECTORY = "media/source"
        const val PROXY_DIRECTORY = "media/proxy"
        const val STEM_DIRECTORY = "media/stems"
        const val REFERENCE_DIRECTORY = "media/references"
    }
}
