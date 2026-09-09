package studio.guitarlab.core.project

import java.io.File
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.Properties
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
            val manifest = readAndValidateManifest(temporary)
            val sourceProject = codec.decode(sourceProjectFile.readText(Charsets.UTF_8))
            require(manifest.getProperty("projectId") == sourceProject.id) {
                "Pacote GuitarLab inconsistente: o manifesto não corresponde ao projeto."
            }
            manifest.getProperty("projectSchema")?.toIntOrNull()?.let { schema ->
                require(schema == sourceProject.schemaVersion) {
                    "Pacote GuitarLab inconsistente: schema do manifesto e project.json divergem."
                }
            }
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
            rewriteManifest(File(temporary, MANIFEST_FILE), manifest, restored)

            val destination = File(projectsDirectory, sanitize(restoredId))
            require(!destination.exists()) { "Conflito inesperado ao restaurar o projeto." }
            publish(temporary, destination)
            return restored
        } catch (error: Throwable) {
            temporary.deleteRecursively()
            throw error
        }
    }

    private fun readAndValidateManifest(projectDirectory: File): Properties {
        val file = File(projectDirectory, MANIFEST_FILE)
        require(file.isFile) { "Pacote GuitarLab inválido: manifest.properties ausente." }
        val properties = Properties().also { values -> file.inputStream().buffered().use(values::load) }
        require(properties.getProperty("format") == PACKAGE_FORMAT) { "Formato de pacote GuitarLab inválido." }
        val version = properties.getProperty("bundleVersion")?.toIntOrNull()
        require(version == SUPPORTED_BUNDLE_VERSION) {
            "Versão de pacote GuitarLab não suportada: ${version ?: "ausente"}."
        }
        require(!properties.getProperty("projectId").isNullOrBlank()) { "Manifesto GuitarLab sem projectId." }
        return properties
    }

    private fun rewriteManifest(file: File, original: Properties, project: GuitarProject) {
        val properties = Properties().apply {
            putAll(original)
            setProperty("format", PACKAGE_FORMAT)
            setProperty("bundleVersion", SUPPORTED_BUNDLE_VERSION.toString())
            setProperty("projectId", project.id)
            setProperty("projectSchema", project.schemaVersion.toString())
        }
        file.outputStream().buffered().use { output -> properties.store(output, null) }
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
        val root = projectDirectory.canonicalFile
        project.clips
            .flatMap { listOfNotNull(it.managedSourcePath, it.managedEditProxyPath) }
            .distinct()
            .forEach { relativePath ->
                val media = File(root, relativePath).canonicalFile
                require(media.path.startsWith(root.path + File.separator) && media.isFile) {
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
        const val MANIFEST_FILE = "manifest.properties"
        const val PACKAGE_FORMAT = "guitarlab-project"
        const val SUPPORTED_BUNDLE_VERSION = 1
        const val MAX_ENTRIES = 10_000
        const val MAX_UNCOMPRESSED_BYTES = 8L * 1024L * 1024L * 1024L
    }
}
