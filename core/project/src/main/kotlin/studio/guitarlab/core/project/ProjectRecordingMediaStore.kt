package studio.guitarlab.core.project

import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.security.MessageDigest
import java.util.UUID
import kotlinx.serialization.json.Json
import studio.guitarlab.core.codec.FileSeekableByteSource
import studio.guitarlab.core.codec.FloatWavRecovery
import studio.guitarlab.core.codec.FloatWavRecoveryStatus
import studio.guitarlab.core.codec.WavMetadataReader

data class RecordingMediaTransaction(
    val projectId: String,
    val id: String,
    val temporaryFile: File,
    val finalFile: File,
    val markerFile: File,
    val relativePath: String,
)

enum class RecordingAbandonResult {
    NOTHING_TO_DO,
    EMPTY_TEMP_REMOVED,
    RECOVERABLE_PARTIAL_PRESERVED,
    FINALIZED_UNPUBLISHED_PRESERVED,
}

/** Owns temporary/final recording takes plus a durable publication marker. */
class ProjectRecordingMediaStore(
    private val rootDirectory: File,
    private val idFactory: () -> String = { UUID.randomUUID().toString() },
    private val nowEpochMs: () -> Long = System::currentTimeMillis,
) {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    fun begin(
        projectId: String,
        suggestedName: String = "gravacao.wav",
        targetTrackId: String? = null,
        requestedTimelineStartFrame: Long? = null,
    ): RecordingMediaTransaction {
        val project = projectDirectory(projectId)
        val temporaryDirectory = File(project, RECORDING_DIRECTORY).also { it.mkdirs() }
        val sourceDirectory = File(project, SOURCE_DIRECTORY).also { it.mkdirs() }
        val id = idFactory()
        val safeName = sanitizeFileName(suggestedName).ifBlank { "gravacao.wav" }
        val finalFile = File(sourceDirectory, "$id-$safeName")
        val temporaryFile = File(temporaryDirectory, "$id$RECORDING_PART_SUFFIX")
        val markerFile = File(temporaryDirectory, "$id$RECOVERY_MARKER_SUFFIX")
        require(!temporaryFile.exists() && !finalFile.exists() && !markerFile.exists()) { "Recording transaction path already exists" }
        val transaction = RecordingMediaTransaction(
            projectId = projectId,
            id = id,
            temporaryFile = temporaryFile,
            finalFile = finalFile,
            markerFile = markerFile,
            relativePath = "$SOURCE_DIRECTORY/${finalFile.name}",
        )
        writeMarker(
            transaction,
            RecordingRecoveryMarker(
                projectId = projectId,
                transactionId = id,
                suggestedFinalName = finalFile.name,
                targetTrackId = targetTrackId,
                requestedTimelineStartFrame = requestedTimelineStartFrame,
                createdAtEpochMs = nowEpochMs(),
            )
        )
        return transaction
    }

    /** Stores exact placement/capture facts before the media rename so a crash can converge later. */
    fun preparePublication(
        transaction: RecordingMediaTransaction,
        targetTrackId: String,
        finalTimelineStartFrame: Long,
        finalSourceStartFrame: Long,
        finalLengthFrames: Long,
        sampleRateHz: Int,
        channelCount: Int,
        framesCaptured: Long,
    ) {
        require(finalTimelineStartFrame >= 0L && finalSourceStartFrame >= 0L && finalLengthFrames > 0L)
        require(sampleRateHz > 0 && channelCount in 1..2 && framesCaptured > 0L)
        val prior = readMarker(transaction.markerFile) ?: RecordingRecoveryMarker(
            projectId = transaction.projectId,
            transactionId = transaction.id,
            suggestedFinalName = transaction.finalFile.name,
            createdAtEpochMs = nowEpochMs(),
        )
        writeMarker(
            transaction,
            prior.copy(
                targetTrackId = targetTrackId,
                finalTimelineStartFrame = finalTimelineStartFrame,
                finalSourceStartFrame = finalSourceStartFrame,
                finalLengthFrames = finalLengthFrames,
                sampleRateHz = sampleRateHz,
                channelCount = channelCount,
                framesCaptured = framesCaptured,
            )
        )
    }

    /**
     * Promotes media without deleting the publication marker. The marker is removed only after
     * project metadata is durably saved, closing the rename->project-save crash window.
     */
    fun commit(transaction: RecordingMediaTransaction): File {
        require(transaction.temporaryFile.isFile || transaction.finalFile.isFile) { "Temporary recording file is missing" }
        if (transaction.finalFile.isFile) {
            if (transaction.temporaryFile.isFile) {
                require(sameBytes(transaction.temporaryFile, transaction.finalFile)) {
                    "Recording recovery collision: final and temporary media differ"
                }
                require(transaction.temporaryFile.delete()) { "Could not remove identical duplicate temporary recording" }
            }
            require(transaction.finalFile.length() > MIN_VALID_WAV_BYTES) { "Recording contains no audio payload" }
            return transaction.finalFile
        }
        require(transaction.temporaryFile.length() > MIN_VALID_WAV_BYTES) { "Recording contains no audio payload" }
        transaction.finalFile.parentFile?.mkdirs()
        try {
            Files.move(transaction.temporaryFile.toPath(), transaction.finalFile.toPath(), StandardCopyOption.ATOMIC_MOVE)
        } catch (_: Exception) {
            Files.move(transaction.temporaryFile.toPath(), transaction.finalFile.toPath())
        }
        return transaction.finalFile
    }

    fun markPublished(transaction: RecordingMediaTransaction) {
        markPublished(transaction.projectId, transaction.id)
    }

    fun markPublished(projectId: String, transactionId: String) {
        val marker = File(File(projectDirectory(projectId), RECORDING_DIRECTORY), "$transactionId$RECOVERY_MARKER_SUFFIX")
        if (marker.exists()) require(marker.delete()) { "Could not clear recording publication marker" }
    }

    /** Legacy rollback entry point: payload-bearing media is never silently deleted. */
    fun discard(transaction: RecordingMediaTransaction) {
        abandon(transaction)
    }

    fun abandon(transaction: RecordingMediaTransaction): RecordingAbandonResult = when {
        transaction.finalFile.isFile -> RecordingAbandonResult.FINALIZED_UNPUBLISHED_PRESERVED
        transaction.temporaryFile.isFile && transaction.temporaryFile.length() > MIN_VALID_WAV_BYTES ->
            RecordingAbandonResult.RECOVERABLE_PARTIAL_PRESERVED
        transaction.temporaryFile.exists() -> {
            runCatching { transaction.temporaryFile.delete() }
            runCatching { transaction.markerFile.delete() }
            RecordingAbandonResult.EMPTY_TEMP_REMOVED
        }
        else -> {
            if (!transaction.finalFile.exists()) runCatching { transaction.markerFile.delete() }
            RecordingAbandonResult.NOTHING_TO_DO
        }
    }

    fun repairInterrupted(projectId: String): Int = recoverableInterrupted(projectId).count { file ->
        runCatching { FloatWavRecovery.repairInterrupted(file).status == FloatWavRecoveryStatus.REPAIRED }
            .getOrDefault(false)
    }

    fun cleanupInterrupted(projectId: String): Int {
        repairInterrupted(projectId)
        val directory = File(projectDirectory(projectId), RECORDING_DIRECTORY)
        if (!directory.isDirectory) return 0
        var removed = 0
        directory.listFiles().orEmpty().forEach { file ->
            if (file.isFile && file.name.endsWith(RECORDING_PART_SUFFIX) && file.length() <= MIN_VALID_WAV_BYTES) {
                val id = transactionIdFromPart(file.name)
                if (file.delete()) {
                    removed++
                    File(directory, "$id$RECOVERY_MARKER_SUFFIX").delete()
                }
            }
        }
        // A marker with no temporary or finalized media cannot contain captured audio.
        markerFiles(projectId).forEach { marker ->
            val id = transactionIdFromMarker(marker.name)
            val parsed = readMarker(marker)
            val final = parsed?.let { File(File(projectDirectory(projectId), SOURCE_DIRECTORY), it.suggestedFinalName) }
                ?: findFinalById(projectId, id)
            val temp = File(directory, "$id$RECORDING_PART_SUFFIX")
            if (!temp.exists() && final?.exists() != true) marker.delete()
        }
        return removed
    }

    fun recoverableInterrupted(projectId: String): List<File> {
        val directory = File(projectDirectory(projectId), RECORDING_DIRECTORY)
        if (!directory.isDirectory) return emptyList()
        return directory.listFiles().orEmpty()
            .filter { it.isFile && it.name.endsWith(RECORDING_PART_SUFFIX) && it.length() > MIN_VALID_WAV_BYTES }
            .sortedBy { it.name }
    }

    /** Non-destructive inventory. Unsafe payload remains untouched and is reported as such. */
    fun recoveryCandidates(projectId: String): List<RecordingRecoveryCandidate> {
        val recordingDirectory = File(projectDirectory(projectId), RECORDING_DIRECTORY).also { it.mkdirs() }
        val markerById = markerFiles(projectId).associateBy { transactionIdFromMarker(it.name) }
        val ids = linkedSetOf<String>()
        ids += markerById.keys
        recordingDirectory.listFiles().orEmpty()
            .filter { it.isFile && it.name.endsWith(RECORDING_PART_SUFFIX) && it.length() > MIN_VALID_WAV_BYTES }
            .forEach { ids += transactionIdFromPart(it.name) }

        return ids.mapNotNull { id ->
            val markerFile = markerById[id]
            val marker = markerFile?.let(::readMarker)
            val temp = File(recordingDirectory, "$id$RECORDING_PART_SUFFIX")
            val final = marker?.let { File(File(projectDirectory(projectId), SOURCE_DIRECTORY), it.suggestedFinalName) }
                ?: findFinalById(projectId, id)
            val media = when {
                final?.isFile == true -> final
                temp.isFile -> temp
                else -> return@mapNotNull null
            }
            val finalized = final?.isFile == true
            var safe = false
            var reason: String? = null
            if (!finalized) {
                val recovery = runCatching { FloatWavRecovery.repairInterrupted(media) }.getOrNull()
                when (recovery?.status) {
                    FloatWavRecoveryStatus.REPAIRED,
                    FloatWavRecoveryStatus.ALREADY_CONSISTENT -> safe = true
                    else -> reason = "Payload temporário não corresponde a um WAV float GuitarLab recuperável."
                }
            } else {
                safe = true
            }
            val metadata = if (safe) runCatching {
                FileSeekableByteSource(media).use { WavMetadataReader().read(it) }
            }.getOrNull() else null
            if (metadata == null || metadata.totalFrames <= 0L || metadata.channelCount !in 1..2) {
                safe = false
                if (reason == null) reason = "A mídia não pôde ser validada para reprodução segura."
            }
            val effectiveSafe = safe && metadata != null
            RecordingRecoveryCandidate(
                projectId = projectId,
                transactionId = id,
                mediaFile = media,
                finalFile = final,
                relativePath = final?.let { "$SOURCE_DIRECTORY/${it.name}" },
                marker = marker,
                state = when {
                    finalized -> RecordingRecoveryMediaState.FINALIZED_UNPUBLISHED
                    effectiveSafe -> RecordingRecoveryMediaState.TEMPORARY_REPAIRED
                    else -> RecordingRecoveryMediaState.TEMPORARY_UNSAFE
                },
                sampleRateHz = metadata?.sampleRateHz,
                channelCount = metadata?.channelCount,
                frames = metadata?.totalFrames,
                safePlayable = effectiveSafe,
                diagnosticReason = if (markerFile != null && marker == null) {
                    "Marker transacional inválido; mídia preservada sem publicação automática."
                } else reason,
            )
        }.sortedWith(compareBy<RecordingRecoveryCandidate> { it.marker?.createdAtEpochMs ?: Long.MAX_VALUE }.thenBy { it.transactionId })
    }

    /** Promotes a validated temporary candidate. A finalized candidate is returned unchanged. */
    fun promoteRecovery(candidate: RecordingRecoveryCandidate): File {
        require(candidate.safePlayable) { "Recovery candidate is not safe to publish" }
        val marker = requireNotNull(candidate.marker) { "Recovery candidate has no trusted transaction marker" }
        val final = requireNotNull(candidate.finalFile) { "Recovery candidate has no final destination" }
        val tx = RecordingMediaTransaction(
            projectId = candidate.projectId,
            id = candidate.transactionId,
            temporaryFile = File(File(projectDirectory(candidate.projectId), RECORDING_DIRECTORY), "${candidate.transactionId}$RECORDING_PART_SUFFIX"),
            finalFile = final,
            markerFile = File(File(projectDirectory(candidate.projectId), RECORDING_DIRECTORY), "${candidate.transactionId}$RECOVERY_MARKER_SUFFIX"),
            relativePath = "$SOURCE_DIRECTORY/${marker.suggestedFinalName}",
        )
        return commit(tx)
    }

    /** Explicit destructive action. Call only after user confirmation. */
    fun discardRecovery(candidate: RecordingRecoveryCandidate) {
        val project = projectDirectory(candidate.projectId).canonicalFile
        listOfNotNull(candidate.mediaFile, candidate.finalFile).distinct().forEach { file ->
            val canonical = file.canonicalFile
            require(canonical.toPath().startsWith(project.toPath())) { "Recovery media escaped project directory" }
            if (canonical.exists()) require(canonical.delete()) { "Could not discard interrupted recording" }
        }
        File(File(project, RECORDING_DIRECTORY), "${candidate.transactionId}$RECOVERY_MARKER_SUFFIX").delete()
    }

    private fun writeMarker(transaction: RecordingMediaTransaction, marker: RecordingRecoveryMarker) {
        require(marker.projectId == transaction.projectId && marker.transactionId == transaction.id)
        transaction.markerFile.parentFile?.mkdirs()
        val staged = File(transaction.markerFile.parentFile, ".${transaction.markerFile.name}.${UUID.randomUUID()}.tmp")
        try {
            staged.writeText(json.encodeToString(RecordingRecoveryMarker.serializer(), marker))
            try {
                Files.move(staged.toPath(), transaction.markerFile.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
            } catch (_: Exception) {
                Files.move(staged.toPath(), transaction.markerFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
            }
        } finally {
            staged.delete()
        }
    }

    private fun readMarker(file: File): RecordingRecoveryMarker? = runCatching {
        json.decodeFromString(RecordingRecoveryMarker.serializer(), file.readText()).takeIf {
            it.schemaVersion == 1 && it.projectId.isNotBlank() && it.transactionId.isNotBlank() &&
                it.suggestedFinalName.isNotBlank() && !it.suggestedFinalName.contains('/') && !it.suggestedFinalName.contains('\\')
        }
    }.getOrNull()

    private fun markerFiles(projectId: String): List<File> {
        val directory = File(projectDirectory(projectId), RECORDING_DIRECTORY)
        return directory.listFiles().orEmpty().filter { it.isFile && it.name.endsWith(RECOVERY_MARKER_SUFFIX) }
    }

    private fun findFinalById(projectId: String, id: String): File? =
        File(projectDirectory(projectId), SOURCE_DIRECTORY).listFiles().orEmpty()
            .firstOrNull { it.isFile && it.name.startsWith("$id-") }

    private fun sameBytes(left: File, right: File): Boolean =
        left.length() == right.length() && sha256(left).contentEquals(sha256(right))

    private fun sha256(file: File): ByteArray {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().buffered().use { input ->
            val buffer = ByteArray(64 * 1024)
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                if (read > 0) digest.update(buffer, 0, read)
            }
        }
        return digest.digest()
    }

    private fun transactionIdFromPart(name: String): String = name.removeSuffix(RECORDING_PART_SUFFIX)
    private fun transactionIdFromMarker(name: String): String = name.removeSuffix(RECOVERY_MARKER_SUFFIX)

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
        const val RECOVERY_MARKER_SUFFIX = ".recording.pending.json"
        const val MIN_VALID_WAV_BYTES = 44L
    }
}
