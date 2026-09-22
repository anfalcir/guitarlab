package studio.guitarlab.core.project

import java.io.File
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.Properties

data class DriveTransactionRecord(
    val projectId: String,
    val desiredRevisionId: String,
    val baseRevisionId: String?,
    val manifestSha256: String,
    val stage: DriveCommitStage,
    val confirmedRevisionId: String?,
) {
    init {
        require(projectId.isNotBlank() && desiredRevisionId.isNotBlank())
        require(Regex("[0-9a-f]{64}").matches(manifestSha256))
        if (stage == DriveCommitStage.HEAD_VERIFIED) require(confirmedRevisionId == desiredRevisionId)
    }
}

interface DriveTransactionJournal {
    fun load(projectId: String): DriveTransactionRecord?
    fun save(record: DriveTransactionRecord)
}

class FileDriveTransactionJournal(private val directory: File) : DriveTransactionJournal {
    override fun load(projectId: String): DriveTransactionRecord? {
        val source = file(projectId)
        if (!source.isFile) return null
        return try {
            val properties = Properties().apply { source.inputStream().buffered().use(::load) }
            require(properties.getProperty("format") == FORMAT)
            require(properties.getProperty("projectId") == projectId)
            DriveTransactionRecord(
                projectId = projectId,
                desiredRevisionId = properties.required("desiredRevisionId"),
                baseRevisionId = properties.getProperty("baseRevisionId").orEmpty().ifBlank { null },
                manifestSha256 = properties.required("manifestSha256"),
                stage = DriveCommitStage.valueOf(properties.required("stage")),
                confirmedRevisionId = properties.getProperty("confirmedRevisionId").orEmpty().ifBlank { null },
            )
        } catch (error: Throwable) {
            throw IllegalStateException("Drive transaction journal is corrupt for project '$projectId'.", error)
        }
    }

    override fun save(record: DriveTransactionRecord) {
        directory.mkdirs()
        val destination = file(record.projectId)
        val temporary = File.createTempFile("drive-transaction-", ".tmp", directory)
        try {
            val properties = Properties().apply {
                setProperty("format", FORMAT)
                setProperty("projectId", record.projectId)
                setProperty("desiredRevisionId", record.desiredRevisionId)
                setProperty("baseRevisionId", record.baseRevisionId.orEmpty())
                setProperty("manifestSha256", record.manifestSha256)
                setProperty("stage", record.stage.name)
                setProperty("confirmedRevisionId", record.confirmedRevisionId.orEmpty())
            }
            temporary.outputStream().buffered().use { properties.store(it, null) }
            try {
                Files.move(
                    temporary.toPath(),
                    destination.toPath(),
                    StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING,
                )
            } catch (_: AtomicMoveNotSupportedException) {
                Files.move(temporary.toPath(), destination.toPath(), StandardCopyOption.REPLACE_EXISTING)
            }
        } finally {
            temporary.delete()
        }
    }

    fun clear(projectId: String) {
        file(projectId).delete()
    }

    fun clearAll() {
        directory.deleteRecursively()
    }

    private fun file(projectId: String): File = directory.resolve("${ManagedStorageKey.from(projectId)}.properties")
    private fun Properties.required(key: String): String = getProperty(key)?.takeIf(String::isNotBlank) ?: error("Missing $key")

    private companion object { const val FORMAT = "guitarlab-drive-transaction-v1" }
}

class DurableUnifiedDriveBackupCoordinator(
    private val coordinator: UnifiedDriveBackupCoordinator,
    private val journal: DriveTransactionJournal,
) {
    suspend fun commit(
        manifest: DriveProjectRevisionManifest,
        assets: List<DriveLocalAsset>,
        confirmedRevisionId: String?,
    ): UnifiedDriveBackupResult {
        val prior = journal.load(manifest.projectId)
        if (prior != null && prior.stage != DriveCommitStage.HEAD_VERIFIED) {
            require(prior.desiredRevisionId == manifest.revisionId && prior.manifestSha256 == manifest.manifestSha256) {
                "A different Drive revision is already pending for this project."
            }
            require(prior.baseRevisionId == confirmedRevisionId) { "Pending Drive transaction base changed unexpectedly." }
        }
        fun record(stage: DriveCommitStage, confirmed: String? = null) = journal.save(
            DriveTransactionRecord(
                manifest.projectId, manifest.revisionId, confirmedRevisionId,
                manifest.manifestSha256, stage, confirmed,
            ),
        )
        record(DriveCommitStage.SNAPSHOT_FROZEN)
        val result = coordinator.commit(manifest, assets, confirmedRevisionId) { stage ->
            record(stage, if (stage == DriveCommitStage.HEAD_VERIFIED) manifest.revisionId else null)
        }
        return result
    }
}
