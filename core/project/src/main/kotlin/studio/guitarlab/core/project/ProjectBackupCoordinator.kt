package studio.guitarlab.core.project

import java.io.File
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import studio.guitarlab.core.model.GuitarProject

/**
 * Provider-neutral backup/restore coordinator.
 *
 * The remote store exposes only COMMITTED versions. A package is generated from the last persisted
 * project snapshot, hashed locally, and the remote implementation is responsible for publishing a
 * version atomically through a commit marker only after re-reading and verifying the remote bytes.
 */
class ProjectBackupCoordinator(
    private val rootDirectory: File,
    private val remoteStore: ProjectBackupRemoteStore,
    private val repository: ProjectRepository = FileProjectRepository(rootDirectory),
    private val bundleWriter: ProjectBundleWriter = ProjectBundleWriter(),
    private val bundleReader: ProjectBundleReader = ProjectBundleReader(rootDirectory),
    private val mediaStore: ProjectManagedMediaStore = ProjectManagedMediaStore(rootDirectory),
    private val nowEpochMs: () -> Long = System::currentTimeMillis,
) {
    suspend fun backupAll(
        force: Boolean,
        retentionPolicy: BackupRetentionPolicy,
    ): BackupRunReport = backupProjects(
        projects = withContext(Dispatchers.IO) { repository.list() },
        force = force,
        retentionPolicy = retentionPolicy,
        cleanupAllProjects = true,
    )

    suspend fun backupProject(
        projectId: String,
        force: Boolean,
        retentionPolicy: BackupRetentionPolicy,
    ): BackupRunReport {
        val project = withContext(Dispatchers.IO) { repository.load(projectId) }
        if (project == null) {
            return BackupRunReport(
                attempts = listOf(ProjectBackupAttempt(projectId, projectId, ProjectBackupAttempt.Status.FAILED, error = "Projeto não encontrado.")),
                deletedVersions = 0,
                retentionDeleteFailures = 0,
                cleanedIncompleteVersions = 0,
                retentionSuppressed = true,
            )
        }
        return backupProjects(listOf(project), force, retentionPolicy, cleanupAllProjects = false)
    }

    suspend fun restoreVersion(version: BackupVersionDescriptor): RestoreAttempt = withContext(Dispatchers.IO) {
        cleanupLocalStaging(nowEpochMs() - INCOMPLETE_GRACE_MS)
        val staged = File.createTempFile("guitarlab-restore-", ".guitarlab", stagingDirectory())
        try {
            remoteStore.copyPackage(version, staged)
            require(staged.isFile && staged.length() == version.sizeBytes) { "Backup remoto incompleto: tamanho divergente." }
            val hash = BackupHashing.sha256(staged)
            require(hash.equals(version.sha256, ignoreCase = true)) { "O backup remoto não passou na verificação de integridade." }
            val restored = staged.inputStream().buffered().use { bundleReader.read(it, nowEpochMs()) }
            RestoreAttempt(version = version, restoredProject = restored)
        } catch (error: Throwable) {
            if (error is CancellationException) throw error
            RestoreAttempt(version = version, error = error.message ?: "Não foi possível restaurar o backup.")
        } finally {
            staged.delete()
        }
    }

    suspend fun restoreLatestAll(): List<RestoreAttempt> {
        val latest = remoteStore.listCommittedVersions()
            .groupBy { it.projectId }
            .values
            .mapNotNull { versions -> versions.maxWithOrNull(compareBy<BackupVersionDescriptor> { it.backupCreatedAtEpochMs }.thenBy { it.remoteId }) }
            .sortedBy { it.projectName.lowercase() }
        return latest.map { restoreVersion(it) }
    }

    private suspend fun backupProjects(
        projects: List<GuitarProject>,
        force: Boolean,
        retentionPolicy: BackupRetentionPolicy,
        cleanupAllProjects: Boolean,
    ): BackupRunReport {
        val startedAt = nowEpochMs()
        withContext(Dispatchers.IO) { cleanupLocalStaging(startedAt - INCOMPLETE_GRACE_MS) }
        val initialCatalog = remoteStore.listCommittedVersions()
        val attempts = mutableListOf<ProjectBackupAttempt>()
        val failedProjectIds = mutableSetOf<String>()
        var attemptedWrites = 0
        var committedWrites = 0

        for (project in projects) {
            val revisionId = BackupRevisionIdentity.forProject(project)
            val existing = initialCatalog.filter { it.projectId == project.id }
            val confirmedExisting = existing.firstOrNull { version ->
                version.revisionId == revisionId ||
                    (version.formatVersion < 2 && version.projectUpdatedAtEpochMs == project.updatedAtEpochMs)
            }
            if (!force && confirmedExisting != null) {
                attempts += ProjectBackupAttempt(
                    project.id,
                    project.name,
                    ProjectBackupAttempt.Status.SKIPPED_UP_TO_DATE,
                    version = confirmedExisting,
                )
                continue
            }
            // Document providers such as cloud-backed SAF implementations may publish directory
            // listings after the bytes themselves are already durable. Before creating another
            // package, perform a targeted revision lookup with provider-specific settling logic.
            val targetedConfirmation = if (force) null else remoteStore.findCommittedVersion(project.id, revisionId)
            if (targetedConfirmation != null) {
                require(targetedConfirmation.projectId == project.id && targetedConfirmation.revisionId == revisionId) {
                    "O armazenamento remoto confirmou uma revisão diferente da solicitada."
                }
                attempts += ProjectBackupAttempt(
                    project.id,
                    project.name,
                    ProjectBackupAttempt.Status.SKIPPED_UP_TO_DATE,
                    version = targetedConfirmation,
                )
                continue
            }
            attemptedWrites++
            val staged = File.createTempFile("guitarlab-backup-", ".guitarlab", stagingDirectory())
            try {
                withContext(Dispatchers.IO) {
                    staged.outputStream().buffered().use { output ->
                        bundleWriter.write(project, mediaStore.projectDirectoryForExport(project.id), output)
                    }
                }
                require(staged.length() > 0L) { "Pacote GuitarLab gerado está vazio." }
                val request = BackupCommitRequest(
                    projectId = project.id,
                    projectName = project.name,
                    projectUpdatedAtEpochMs = project.updatedAtEpochMs,
                    backupCreatedAtEpochMs = nowEpochMs(),
                    sizeBytes = staged.length(),
                    sha256 = withContext(Dispatchers.IO) { BackupHashing.sha256(staged) },
                    packageFile = staged,
                    revisionId = revisionId,
                )
                val committed = remoteStore.commit(request)
                require(
                    committed.projectId == request.projectId &&
                        committed.revisionId == request.revisionId &&
                        committed.sizeBytes == request.sizeBytes &&
                        committed.sha256.equals(request.sha256, true)
                ) {
                    "O armazenamento remoto confirmou uma versão incompatível."
                }
                committedWrites++
                attempts += ProjectBackupAttempt(project.id, project.name, ProjectBackupAttempt.Status.COMMITTED, version = committed)
            } catch (error: Throwable) {
                if (error is CancellationException) throw error
                failedProjectIds += project.id
                attempts += ProjectBackupAttempt(project.id, project.name, ProjectBackupAttempt.Status.FAILED, error = error.message ?: "Falha de backup.")
            } finally {
                staged.delete()
            }
        }

        // If every write that was attempted failed, do not reduce the safety margin by deleting old backups.
        val suppressRetention = attemptedWrites > 0 && committedWrites == 0
        var deleted = 0
        var deleteFailures = 0
        if (!suppressRetention) {
            val freshCatalog = remoteStore.listCommittedVersions()
            val scopedCatalog = if (cleanupAllProjects) freshCatalog else {
                val ids = projects.mapTo(mutableSetOf()) { it.id }
                freshCatalog.filter { it.projectId in ids }
            }
            val deletions = BackupRetentionPlanner.deletions(scopedCatalog, retentionPolicy, nowEpochMs())
                .filterNot { it.projectId in failedProjectIds }
            deletions.forEach { version ->
                try {
                    if (remoteStore.deleteVersion(version)) deleted++ else deleteFailures++
                } catch (error: Throwable) {
                    if (error is CancellationException) throw error
                    deleteFailures++
                }
            }
        }

        val cleanedIncomplete = try {
            remoteStore.cleanupIncomplete(startedAt - INCOMPLETE_GRACE_MS)
        } catch (error: Throwable) {
            if (error is CancellationException) throw error
            0
        }

        return BackupRunReport(
            attempts = attempts,
            deletedVersions = deleted,
            retentionDeleteFailures = deleteFailures,
            cleanedIncompleteVersions = cleanedIncomplete,
            retentionSuppressed = suppressRetention,
        )
    }


    private fun stagingDirectory(): File = rootDirectory.resolve("backup-staging").also { it.mkdirs() }

    private fun cleanupLocalStaging(olderThanEpochMs: Long): Int {
        var removed = 0
        stagingDirectory().listFiles().orEmpty().forEach { candidate ->
            if (candidate.isFile && candidate.lastModified() < olderThanEpochMs && candidate.delete()) removed++
        }
        return removed
    }

    private companion object {
        const val INCOMPLETE_GRACE_MS = 24L * 60L * 60L * 1000L
    }
}
