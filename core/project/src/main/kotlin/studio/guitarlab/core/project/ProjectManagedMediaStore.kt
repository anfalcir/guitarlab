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
 * Owns project-managed audio sources. Import always copies external media into the project.
 * Finalized source files are treated as immutable: this API exposes no overwrite operation.
 * Edits such as trim/move/gain stay in project metadata; derived/cache files live elsewhere.
 */
class ProjectManagedMediaStore(
    private val rootDirectory: File,
    private val idFactory: () -> String = { UUID.randomUUID().toString() },
) {
    fun ingest(
        projectId: String,
        suggestedName: String,
        input: InputStream,
    ): ManagedMediaAsset {
        val projectDirectory = projectDirectory(projectId)
        val sourceDirectory = File(projectDirectory, SOURCE_DIRECTORY).also { it.mkdirs() }
        val safeName = sanitizeFileName(suggestedName).ifBlank { "audio.bin" }
        val destination = File(sourceDirectory, "${idFactory()}-$safeName")
        val temporary = File(sourceDirectory, ".${destination.name}.part")

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
            require(copied > 0L) { "Imported audio source is empty." }

            try {
                Files.move(
                    temporary.toPath(),
                    destination.toPath(),
                    StandardCopyOption.ATOMIC_MOVE,
                )
            } catch (_: Exception) {
                Files.move(temporary.toPath(), destination.toPath())
            }

            return ManagedMediaAsset(
                relativePath = "$SOURCE_DIRECTORY/${destination.name}",
                file = destination,
                byteCount = copied,
            )
        } catch (error: Throwable) {
            temporary.delete()
            destination.delete()
            throw error
        }
    }

    fun resolve(projectId: String, relativePath: String): File {
        require(relativePath.startsWith("$SOURCE_DIRECTORY/")) { "Managed source path is outside the source area." }
        require(!relativePath.contains("..")) { "Managed source path must not traverse directories." }
        val projectDirectory = projectDirectory(projectId).canonicalFile
        val candidate = File(projectDirectory, relativePath).canonicalFile
        require(candidate.path.startsWith(projectDirectory.path + File.separator)) { "Managed source escaped project storage." }
        require(candidate.isFile) { "Managed source file is missing: $relativePath" }
        return candidate
    }

    /** Only for rollback of an import transaction that failed before the project references it. */
    fun discardUncommitted(projectId: String, relativePath: String) {
        runCatching { resolve(projectId, relativePath).delete() }
    }

    private fun projectDirectory(projectId: String): File =
        File(File(rootDirectory, "projects"), sanitizeProjectId(projectId)).also { it.mkdirs() }

    private fun sanitizeProjectId(value: String): String = value.replace(Regex("[^A-Za-z0-9._-]"), "_")

    private fun sanitizeFileName(value: String): String = value
        .substringAfterLast('/')
        .substringAfterLast('\\')
        .replace(Regex("[^A-Za-z0-9._ -]"), "_")
        .take(120)

    private companion object {
        const val SOURCE_DIRECTORY = "media/source"
    }
}
