package studio.guitarlab.core.project

import java.io.File
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import studio.guitarlab.core.model.GuitarProject

/** Portable GuitarLab project package. Sources are preserved verbatim; proxies are included as a cache. */
class ProjectBundleWriter(
    private val codec: ProjectCodec = ProjectCodec(),
) {
    fun write(project: GuitarProject, projectDirectory: File, output: OutputStream) {
        require(projectDirectory.isDirectory) { "Project directory is missing." }
        ZipOutputStream(output.buffered()).use { zip ->
            writeText(zip, "project.json", codec.encode(project))
            writeText(
                zip,
                "manifest.properties",
                buildString {
                    appendLine("format=guitarlab-project")
                    appendLine("bundleVersion=1")
                    appendLine("projectId=${project.id}")
                    appendLine("projectSchema=${project.schemaVersion}")
                },
            )
            referencedMedia(project).forEach { relativePath ->
                val file = File(projectDirectory, relativePath).canonicalFile
                require(file.path.startsWith(projectDirectory.canonicalPath + File.separator)) { "Project media escaped package root." }
                require(file.isFile) { "Referenced project media is missing: $relativePath" }
                zip.putNextEntry(ZipEntry(relativePath.replace('\\', '/')))
                file.inputStream().buffered().use { it.copyTo(zip) }
                zip.closeEntry()
            }
        }
    }

    private fun referencedMedia(project: GuitarProject): List<String> = project.clips
        .flatMap { listOfNotNull(it.managedSourcePath, it.managedEditProxyPath) }
        .distinct()
        .sorted()

    private fun writeText(zip: ZipOutputStream, name: String, value: String) {
        zip.putNextEntry(ZipEntry(name))
        zip.write(value.toByteArray(Charsets.UTF_8))
        zip.closeEntry()
    }
}
