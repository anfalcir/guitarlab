package studio.guitarlab.core.project

import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.UUID
import studio.guitarlab.core.codec.FloatWavRecovery
import studio.guitarlab.core.codec.FloatWavRecoveryStatus

data class RecordingMediaTransaction(
    val projectId: String,
    val id: String,
    val temporaryFile: File,
    val finalFile: File,
    val relativePath: String,
)

enum class RecordingAbandonResult {
    NOTHING_TO_DO,
    EMPTY_TEMP_REMOVED,
    RECOVERABLE_PARTIAL_PRESERVED,
    FINALIZED_UNPUBLISHED_PRESERVED,
}

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

    /**
     * Legacy rollback entry point. It is intentionally lossless once audio payload exists:
     * callers cannot silently delete a captured partial or finalized unpublished take.
     */
    fun discard(transaction: RecordingMediaTransaction) {
        abandon(transaction)
    }

    /**
     * Abandons an interrupted transaction without silently deleting captured audio.
     * Empty/header-only temporary files are safe to remove. Any file with an audio
     * payload, or an already-finalized unpublished take, is retained for recovery.
     */
    fun abandon(transaction: RecordingMediaTransaction): RecordingAbandonResult = when {
        transaction.finalFile.isFile -> RecordingAbandonResult.FINALIZED_UNPUBLISHED_PRESERVED
        transaction.temporaryFile.isFile && transaction.temporaryFile.length() > MIN_VALID_WAV_BYTES ->
            RecordingAbandonResult.RECOVERABLE_PARTIAL_PRESERVED
        transaction.temporaryFile.exists() -> {
            runCatching { transaction.temporaryFile.delete() }
            RecordingAbandonResult.EMPTY_TEMP_REMOVED
        }
        else -> RecordingAbandonResult.NOTHING_TO_DO
    }

    /**
     * Repairs GuitarLab-owned float WAV headers left unfinished by abrupt process death.
     * Unrecognized payload-bearing files are retained unchanged for forensic/manual recovery.
     */
    fun repairInterrupted(projectId: String): Int = recoverableInterrupted(projectId).count { file ->
        runCatching { FloatWavRecovery.repairInterrupted(file).status == FloatWavRecoveryStatus.REPAIRED }
            .getOrDefault(false)
    }

    /**
     * Removes only interrupted recording temporaries that cannot contain an audio
     * payload. Payload-bearing .part files are deliberately retained. Before cleanup,
     * any canonical GuitarLab float WAV payload is repaired best-effort.
     */
    fun cleanupInterrupted(projectId: String): Int {
        repairInterrupted(projectId)
        val directory = File(projectDirectory(projectId), RECORDING_DIRECTORY)
        if (!directory.isDirectory) return 0
        var removed = 0
        directory.listFiles().orEmpty().forEach { file ->
            if (file.isFile &&
                file.name.endsWith(RECORDING_PART_SUFFIX) &&
                file.length() <= MIN_VALID_WAV_BYTES &&
                file.delete()
            ) removed++
        }
        return removed
    }

    /** Non-destructive inventory of interrupted takes that still contain payload. */
    fun recoverableInterrupted(projectId: String): List<File> {
        val directory = File(projectDirectory(projectId), RECORDING_DIRECTORY)
        if (!directory.isDirectory) return emptyList()
        return directory.listFiles().orEmpty()
            .filter { it.isFile && it.name.endsWith(RECORDING_PART_SUFFIX) && it.length() > MIN_VALID_WAV_BYTES }
            .sortedBy { it.name }
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
        const val RECORDING_PART_SUFFIX = ".recording.part.wav"
        const val MIN_VALID_WAV_BYTES = 44L
    }
}
