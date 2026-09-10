package studio.guitarlab.core.project

import studio.guitarlab.core.model.GuitarProject
import studio.guitarlab.core.model.ProjectValidator
import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.StandardCopyOption

class FileProjectRepository(
    rootDirectory: File,
    private val codec: ProjectCodec = ProjectCodec()
) : ProjectRepository {
    private val projectsDirectory = File(rootDirectory, "projects").also { it.mkdirs() }

    override fun list(): List<GuitarProject> = projectsDirectory
        .listFiles { file -> file.isDirectory }
        .orEmpty()
        .mapNotNull { directory -> readProjectFile(File(directory, PROJECT_FILE)) }
        .sortedByDescending { it.updatedAtEpochMs }

    override fun load(projectId: String): GuitarProject? =
        readProjectFile(projectFile(projectId))

    override fun save(project: GuitarProject): GuitarProject {
        val issues = ProjectValidator.validate(project)
        require(issues.isEmpty()) { "Refusing to persist invalid project: ${issues.joinToString { it.code }}" }

        val directory = projectDirectory(project.id).also { it.mkdirs() }
        val destination = File(directory, PROJECT_FILE)
        val temporary = File(directory, "$PROJECT_FILE.tmp")
        temporary.writeText(codec.encode(project), StandardCharsets.UTF_8)

        try {
            Files.move(
                temporary.toPath(),
                destination.toPath(),
                StandardCopyOption.REPLACE_EXISTING,
                StandardCopyOption.ATOMIC_MOVE
            )
        } catch (_: Exception) {
            Files.move(
                temporary.toPath(),
                destination.toPath(),
                StandardCopyOption.REPLACE_EXISTING
            )
        }
        return project
    }

    override fun delete(projectId: String): Boolean {
        val directory = projectDirectory(projectId)
        if (!directory.exists()) return false
        return directory.deleteRecursively()
    }

    override fun duplicate(
        projectId: String,
        newName: String,
        newProjectId: String,
        nowEpochMs: Long
    ): GuitarProject {
        val source = requireNotNull(load(projectId)) { "Project '$projectId' not found." }
        require(newProjectId != projectId) { "Duplicate project id must differ from source." }
        val sourceDirectory = projectDirectory(projectId)
        val destinationDirectory = projectDirectory(newProjectId)
        require(!destinationDirectory.exists()) { "Project '$newProjectId' already exists." }
        val duplicate = source.copy(
            id = newProjectId,
            name = newName.trim(),
            createdAtEpochMs = nowEpochMs,
            updatedAtEpochMs = nowEpochMs
        )
        try {
            copyReferencedMedia(source, sourceDirectory, destinationDirectory)
            return save(duplicate)
        } catch (error: Throwable) {
            destinationDirectory.deleteRecursively()
            throw error
        }
    }

    private fun copyReferencedMedia(project: GuitarProject, sourceDirectory: File, destinationDirectory: File) {
        val sourceRoot = sourceDirectory.canonicalFile
        val destinationRoot = destinationDirectory.canonicalFile
        project.clips
            .flatMap { listOfNotNull(it.managedSourcePath, it.managedEditProxyPath) }
            .distinct()
            .forEach { relativePath ->
                val source = File(sourceRoot, relativePath).canonicalFile
                val destination = File(destinationRoot, relativePath).canonicalFile
                require(source.path.startsWith(sourceRoot.path + File.separator) && source.isFile) {
                    "Referenced project media is missing: $relativePath"
                }
                require(destination.path.startsWith(destinationRoot.path + File.separator)) {
                    "Project media escaped duplicate root."
                }
                destination.parentFile?.let { require(it.mkdirs() || it.isDirectory) }
                source.copyTo(destination, overwrite = false)
            }
    }

    private fun readProjectFile(file: File): GuitarProject? = runCatching {
        if (!file.isFile) return null
        codec.decode(file.readText(StandardCharsets.UTF_8))
    }.getOrNull()

    private fun projectDirectory(projectId: String) = File(projectsDirectory, sanitize(projectId))
    private fun projectFile(projectId: String) = File(projectDirectory(projectId), PROJECT_FILE)

    private fun sanitize(value: String): String = value.replace(Regex("[^A-Za-z0-9._-]"), "_")

    private companion object {
        const val PROJECT_FILE = "project.json"
    }
}
