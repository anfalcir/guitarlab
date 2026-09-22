package studio.guitarlab.app.backup

import android.content.Context
import java.io.File
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import studio.guitarlab.core.project.BackupCommitRequest
import studio.guitarlab.core.project.BackupHashing

data class LegacySafMigrationReport(
    val migrated: Int,
    val skippedExisting: Int,
    val failed: Int,
    val errors: List<String>,
)

/**
 * One-time bridge from the H28 SAF catalog to Drive v3. No legacy data is deleted. The persisted
 * SAF permission is released only after every committed version has either migrated or already
 * exists in Drive with matching revision identity.
 */
internal class LegacySafBackupMigrator(private val context: Context) {
    suspend fun migrate(
        saf: SafBackupRemoteStore,
        drive: DriveV3BackupRemoteStore,
    ): LegacySafMigrationReport = withContext(Dispatchers.IO) {
        var migrated = 0
        var skipped = 0
        var failed = 0
        val errors = mutableListOf<String>()
        val staging = File(context.cacheDir, "legacy-saf-migration").also { it.mkdirs() }

        saf.listCommittedVersions().forEach { version ->
            try {
                val existing = drive.findCommittedVersion(version.projectId, version.revisionId)
                if (existing != null) {
                    require(existing.sizeBytes == version.sizeBytes && existing.sha256.equals(version.sha256, true)) {
                        "A revisão ${version.projectName} já existe no Drive com conteúdo divergente."
                    }
                    skipped++
                    return@forEach
                }

                val temp = File.createTempFile("legacy-", ".guitarlab", staging)
                try {
                    saf.copyPackage(version, temp)
                    require(temp.length() == version.sizeBytes) { "tamanho divergente" }
                    require(BackupHashing.sha256(temp).equals(version.sha256, true)) { "SHA-256 divergente" }
                    drive.commit(
                        BackupCommitRequest(
                            projectId = version.projectId,
                            projectName = version.projectName,
                            projectUpdatedAtEpochMs = version.projectUpdatedAtEpochMs,
                            backupCreatedAtEpochMs = version.backupCreatedAtEpochMs,
                            sizeBytes = version.sizeBytes,
                            sha256 = version.sha256,
                            packageFile = temp,
                            revisionId = version.revisionId,
                        ),
                    )
                    migrated++
                } finally {
                    temp.delete()
                }
            } catch (error: Throwable) {
                if (error is CancellationException) throw error
                failed++
                errors += "${version.projectName}: ${error.message ?: "falha desconhecida"}"
            }
        }
        staging.listFiles().orEmpty().forEach { if (it.isFile) it.delete() }
        LegacySafMigrationReport(migrated, skipped, failed, errors)
    }
}
