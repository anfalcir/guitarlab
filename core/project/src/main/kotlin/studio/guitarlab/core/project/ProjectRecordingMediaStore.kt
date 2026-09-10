package studio.guitarlab.core.project

import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.UUID

data class RecordingMediaTransaction(
    val projectId: String,
    val id: String,
    val temporaryFile: File,
    val finalFile: File,
    val relativePath: String,
)

/** Owns temporary and finalized project recording takes without exposing overwrite of final media. */
class ProjectRecordingMediaStore(
    private val rootDirectory: File,
    private val idFactory: () -> String = { UUID.randomUUID().toString() },
) {
    fun begin(projectId: String, suggestedName: String = "gravacao.wav"): RecordingMediaTransaction {
        val project = projectDirectory(projectId)
        val temporaryDirectory = File(project, RECORDING_DIRECTORY).also { it.mkdirs() }
        val sourceDirectory = File(project, SOURCE_DIRECTORY).also { it.mkdirs() }
        val id = idFactory()
        val safeName = sanitizeFileName(suggestedName).ifBlank { "gravacao.wav" }
        val finalFile = File(sourceDirectory, "$id-$safeName")
        val temporaryFile = File(temporaryDirectory, "$id.recording.part.wav")
        require(!temporaryFile.exists() && !finalFile.exists()) { "Recording transaction path already exists" }
        return RecordingMediaTransaction(
            projectId = projectId,
            id = id,
            temporaryFile = temporaryFile,
            finalFile = finalFile,
            relativePath = "$SOURCE_DIRECTORY/${finalFile.name}",
        )
    }

    fun commit(transaction: RecordingMediaTransaction): File {
        require(transaction.temporaryFile.isFile) { "Temporary recording file is missing" }
        require(transaction.temporaryFile.length() > MIN_VALID_WAV_BYTES) { "Recording contains no audio payload" }
        require(!transaction.finalFile.exists()) { "Final recording file already exists" }
        transaction.finalFile.parentFile?.mkdirs()
        try {
            Files.move(
                transaction.temporaryFile.toPath(),
                transaction.finalFile.toPath(),
                StandardCopyOption.ATOMIC_MOVE,
            )
        } catch (_: Exception) {
            Files.move(transaction.temporaryFile.toPath(), transaction.finalFile.toPath())
        }
        return transaction.finalFile
    }

    fun discard(transaction: RecordingMediaTransaction) {
        runCatching { transaction.temporaryFile.delete() }
        runCatching { transaction.finalFile.delete() }
    }

    /** Removes crash/interruption leftovers that were never committed into project metadata. */
    fun cleanupInterrupted(projectId: String): Int {
        val directory = File(projectDirectory(projectId), RECORDING_DIRECTORY)
        if (!directory.isDirectory) return 0
        var removed = 0
        directory.listFiles().orEmpty().forEach { file ->
            if (file.isFile && file.name.endsWith(".recording.part.wav") && file.delete()) removed++
        }
        return removed
    }

    private fun projectDirectory(projectId: String): File =
        File(File(rootDirectory, "projects"), ManagedStorageKey.from(projectId)).also { it.mkdirs() }

    private fun sanitizeFileName(value: String): String = value
        .substringAfterLast('/')
        .substringAfterLast('\\')
        .replace(Regex("[^A-Za-z0-9._ -]"), "_")
        .take(100)
        .let { if (it.endsWith(".wav", ignoreCase = true)) it else "$it.wav" }

    private companion object {
        const val SOURCE_DIRECTORY = "media/source"
        const val RECORDING_DIRECTORY = "media/recording"
        const val MIN_VALID_WAV_BYTES = 44L
    }
}
