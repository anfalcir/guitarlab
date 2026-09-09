package studio.guitarlab.core.project

import java.io.File
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.UUID
import java.util.zip.ZipInputStream
import studio.guitarlab.core.model.GuitarProject
import studio.guitarlab.core.model.ProjectValidator

/**
 * Restores a portable .guitarlab package as an independent managed project.
 * The package never overwrites an existing project id and every ZIP path is constrained to the
 * temporary project root before the restored directory is published atomically when possible.
 */
class ProjectBundleReader(
    private val rootDirectory: File,
    private val codec: ProjectCodec = ProjectCodec(),
) {
    private val projectsDirectory = File(rootDirectory, "projects").also { it.mkdirs() }

    fun read(input: InputStream, nowEpochMs: Long = System.currentTimeMillis()): GuitarProject {
        val temporary = File(projectsDirectory, ".import-${UUID.randomUUID()}")
        require(temporary.mkdirs()) { "Não foi possível preparar a importação do projeto." }
        try {
            extractSafely(input, temporary)
            val sourceProjectFile = File(temporary, PROJECT_FILE)
            require(sourceProjectFile.isFile) { "Pacote GuitarLab inválido: project.json ausente." }
            val sourceProject = codec.decode(sourceProjectFile.readText(Charsets.UTF_8))
            val issues = ProjectValidator.validate(sourceProject)
            require(issues.isEmpty()) { "Pacote GuitarLab contém projeto inválido: ${issues.joinToString { it.code }}" }
            verifyReferencedMedia(sourceProject, temporary)

            val restoredId = UUID.randomUUID().toString()
            val restored = sourceProject.copy(
                id = restoredId,
                createdAtEpochMs = nowEpochMs,
                updatedAtEpochMs = nowEpochMs,
            )
            sourceProjectFile.writeText(codec.encode(restored), Charsets.UTF_8)

            val destination = File(projectsDirectory, sanitize(restoredId))
            require(!destination.exists()) { "Conflito inesperado ao restaurar o projeto." }
            publish(temporary, destination)
            return restored
        } catch (error: Throwable) {
            temporary.deleteRecursively()
            throw error
        }
    }

    private fun extractSafely(input: InputStream, destination: File) {
        val root = destination.canonicalFile
        var totalBytes = 0L
        var entries = 0
        ZipInputStream(input.buffered()).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                entries++
                require(entries <= MAX_ENTRIES) { "Pacote GuitarLab contém arquivos demais." }
                val name = entry.name.replace('\\', '/')
                require(name.isNotBlank() && !name.startsWith("/") && !name.contains("../")) {
                    "Pacote GuitarLab contém caminho inválido."
                }
                val target = File(root, name).canonicalFile
                require(target.path == root.path || target.path.startsWith(root.path + File.separator)) {
                    "Pacote GuitarLab tentou escrever fora da pasta do projeto."
                }
                if (entry.isDirectory) {
                    require(target.mkdirs() || target.isDirectory)
                } else {
                    target.parentFile?.let { require(it.mkdirs() || it.isDirectory) }
                    target.outputStream().buffered().use { output ->
                        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                        while (true) {
                            val read = zip.read(buffer)
                            if (read <= 0) break
                            totalBytes += read
                            require(totalBytes <= MAX_UNCOMPRESSED_BYTES) { "Pacote GuitarLab excede o limite de segurança." }
                            output.write(buffer, 0, read)
                        }
                    }
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }
        require(entries > 0) { "Arquivo GuitarLab vazio ou corrompido." }
    }

    private fun verifyReferencedMedia(project: GuitarProject, projectDirectory: File) {
        project.clips
            .flatMap { listOfNotNull(it.managedSourcePath, it.managedEditProxyPath) }
            .distinct()
            .forEach { relativePath ->
                val media = File(projectDirectory, relativePath).canonicalFile
                require(media.path.startsWith(projectDirectory.canonicalPath + File.separator) && media.isFile) {
                    "Pacote GuitarLab incompleto: mídia referenciada ausente ($relativePath)."
                }
            }
    }

    private fun publish(source: File, destination: File) {
        try {
            Files.move(source.toPath(), destination.toPath(), StandardCopyOption.ATOMIC_MOVE)
        } catch (_: Exception) {
            Files.move(source.toPath(), destination.toPath())
        }
    }

    private fun sanitize(value: String): String = value.replace(Regex("[^A-Za-z0-9._-]"), "_")

    private companion object {
        const val PROJECT_FILE = "project.json"
        const val MAX_ENTRIES = 10_000
        const val MAX_UNCOMPRESSED_BYTES = 8L * 1024L * 1024L * 1024L
    }
}
