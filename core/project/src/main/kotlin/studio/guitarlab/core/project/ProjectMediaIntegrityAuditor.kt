package studio.guitarlab.core.project

import java.io.File
import studio.guitarlab.core.model.GuitarProject

data class ProjectMediaAuditReport(
    val missingReferencedMedia: List<String>,
    val unreferencedSourcesRetained: List<String>,
    val unreferencedProxies: List<String>,
    val recoverableInterruptedRecordings: List<String>,
    val removableTemporaries: List<String>,
    val orphanWaveforms: List<String>,
) {
    val hasIntegrityFailure: Boolean get() = missingReferencedMedia.isNotEmpty()
}

data class ProjectMediaCleanupResult(
    val temporaryFilesRemoved: Int,
    val waveformFilesRemoved: Int,
)

/**
 * Non-destructive media inventory for recovery/hardening.
 *
 * Immutable sources and payload-bearing interrupted recordings are never deleted here.
 * Unreferenced proxies are reported but retained because regeneration is not yet an
 * automatic load-time contract. Cleanup is restricted to provably temporary files and
 * regenerable waveform cache entries.
 */
class ProjectMediaIntegrityAuditor(private val rootDirectory: File) {
    fun audit(project: GuitarProject): ProjectMediaAuditReport {
        val root = projectDirectory(project.id).canonicalFile
        val sourceReferences = project.clips.mapNotNull { it.managedSourcePath }.toSet()
        val proxyReferences = project.clips.mapNotNull { it.managedEditProxyPath }.toSet()
        val allReferences = sourceReferences + proxyReferences

        val missing = allReferences
            .filterNot { relative -> confinedFile(root, relative)?.isFile == true }
            .sorted()

        val sourceFiles = filesIn(root, SOURCE_DIRECTORY)
        val proxyFiles = filesIn(root, PROXY_DIRECTORY)
        val recordingFiles = filesIn(root, RECORDING_DIRECTORY)
        val waveformFiles = filesIn(root, WAVEFORM_DIRECTORY)

        val unreferencedSources = sourceFiles
            .filterNot(::isManagedIngestTemporary)
            .map { relative(root, it) }
            .filterNot(sourceReferences::contains)
            .sorted()

        val unreferencedProxies = proxyFiles
            .filterNot(::isManagedIngestTemporary)
            .map { relative(root, it) }
            .filterNot(proxyReferences::contains)
            .sorted()

        val recoverableRecordings = recordingFiles
            .filter { it.name.endsWith(RECORDING_PART_SUFFIX) && it.length() > MIN_VALID_WAV_BYTES }
            .map { relative(root, it) }
            .sorted()

        val removableTemporaries = buildList {
            addAll(sourceFiles.filter(::isManagedIngestTemporary))
            addAll(proxyFiles.filter(::isManagedIngestTemporary))
            addAll(recordingFiles.filter { it.name.endsWith(RECORDING_PART_SUFFIX) && it.length() <= MIN_VALID_WAV_BYTES })
        }.map { relative(root, it) }.sorted()

        val retainedWaveforms = project.clips
            .mapTo(mutableSetOf()) { "${ManagedStorageKey.from(it.id)}.glwf" }
        val orphanWaveforms = waveformFiles
            .filter { it.extension == "glwf" && it.name !in retainedWaveforms }
            .map { relative(root, it) }
            .sorted()

        return ProjectMediaAuditReport(
            missingReferencedMedia = missing,
            unreferencedSourcesRetained = unreferencedSources,
            unreferencedProxies = unreferencedProxies,
            recoverableInterruptedRecordings = recoverableRecordings,
            removableTemporaries = removableTemporaries,
            orphanWaveforms = orphanWaveforms,
        )
    }

    fun cleanupSafeDerived(project: GuitarProject): ProjectMediaCleanupResult {
        val root = projectDirectory(project.id).canonicalFile
        val report = audit(project)
        val temporaryRemoved = report.removableTemporaries.count { relative ->
            confinedFile(root, relative)?.let { it.isFile && it.delete() } == true
        }
        val waveformRemoved = report.orphanWaveforms.count { relative ->
            confinedFile(root, relative)?.let { it.isFile && it.delete() } == true
        }
        return ProjectMediaCleanupResult(temporaryRemoved, waveformRemoved)
    }

    private fun filesIn(projectRoot: File, relativeDirectory: String): List<File> {
        val directory = confinedFile(projectRoot, relativeDirectory) ?: return emptyList()
        return directory.listFiles().orEmpty().filter { it.isFile }
    }

    private fun confinedFile(projectRoot: File, relativePath: String): File? = runCatching {
        val candidate = File(projectRoot, relativePath).canonicalFile
        candidate.takeIf { candidate.path == projectRoot.path || candidate.path.startsWith(projectRoot.path + File.separator) }
    }.getOrNull()

    private fun relative(projectRoot: File, file: File): String =
        file.canonicalFile.relativeTo(projectRoot).invariantSeparatorsPath

    private fun isManagedIngestTemporary(file: File): Boolean =
        file.name.startsWith(".") && file.name.endsWith(".part")

    private fun projectDirectory(projectId: String): File =
        File(File(rootDirectory, "projects"), ManagedStorageKey.from(projectId)).also { it.mkdirs() }

    private companion object {
        const val SOURCE_DIRECTORY = "media/source"
        const val PROXY_DIRECTORY = "media/proxy"
        const val RECORDING_DIRECTORY = "media/recording"
        const val WAVEFORM_DIRECTORY = "media/derived/waveform"
        const val RECORDING_PART_SUFFIX = ".recording.part.wav"
        const val MIN_VALID_WAV_BYTES = 44L
    }
}
