package studio.guitarlab.app.backup

import android.content.Context
import java.io.File
import kotlinx.coroutines.CancellationException
import studio.guitarlab.core.model.GuitarProject
import studio.guitarlab.core.project.BackupRetentionPolicy
import studio.guitarlab.core.project.BackupRevisionIdentity
import studio.guitarlab.core.project.BackupRunReport
import studio.guitarlab.core.project.BackupVersionDescriptor
import studio.guitarlab.core.project.DriveCommitStage
import studio.guitarlab.core.project.DriveConflictAction
import studio.guitarlab.core.project.DriveConflictException
import studio.guitarlab.core.project.DriveConflictResolver
import studio.guitarlab.core.project.DriveCurrentDescriptor
import studio.guitarlab.core.project.DriveReconciliation
import studio.guitarlab.core.project.DriveRestorePublisher
import studio.guitarlab.core.project.DurableUnifiedDriveBackupCoordinator
import studio.guitarlab.core.project.FileDriveTransactionJournal
import studio.guitarlab.core.project.FileProjectRepository
import studio.guitarlab.core.project.FileUnifiedDriveSnapshotStore
import studio.guitarlab.core.project.ProjectBackupAttempt
import studio.guitarlab.core.project.ProjectCodec
import studio.guitarlab.core.project.RestoreAttempt
import studio.guitarlab.core.project.UnifiedDriveBackupCoordinator
import studio.guitarlab.core.project.UnifiedDriveGarbageCollector
import studio.guitarlab.core.project.UnifiedDriveLocalProjectPublisher
import studio.guitarlab.core.project.UnifiedDriveLocalPublication
import studio.guitarlab.core.project.UnifiedDriveLocalRestoreValidator
import studio.guitarlab.core.project.UnifiedDriveProjectRestoreLayout
import studio.guitarlab.core.project.UnifiedDriveProjectSnapshotBuilder
import studio.guitarlab.core.project.UnifiedDriveRestoreCoordinator
import studio.guitarlab.core.project.UnifiedProjectRevision

internal class UnifiedDriveProductionService(
    context: Context,
    tokenProvider: DriveAccessTokenProvider = GoogleDriveAccessTokenProvider(context),
    private val nowEpochMs: () -> Long = System::currentTimeMillis,
) {
    private val applicationContext = context.applicationContext
    private val rootDirectory = applicationContext.filesDir
    private val repository = FileProjectRepository(rootDirectory)
    private val codec = ProjectCodec()
    private val confirmedRevisions = ConfirmedRevisionStore(applicationContext)
    private val journal = FileDriveTransactionJournal(File(rootDirectory, JOURNAL_DIRECTORY))
    private val snapshotStore =
        FileUnifiedDriveSnapshotStore(File(rootDirectory, SNAPSHOT_DIRECTORY))
    private val uploadState = SharedPreferencesUnifiedDriveUploadState(applicationContext)
    private val http = DriveV3HttpClient(tokenProvider)
    private val rootAccess = UnifiedDriveRootAccess(applicationContext, http)
    private val remote = UnifiedDriveV3RemoteStore(
        http = http,
        rootFolderId = rootAccess::rootFolderId,
        uploadState = uploadState,
    )
    private val durableCoordinator = DurableUnifiedDriveBackupCoordinator(
        UnifiedDriveBackupCoordinator(remote),
        journal,
    )
    private val snapshotBuilder =
        UnifiedDriveProjectSnapshotBuilder(rootDirectory, codec, nowEpochMs)
    private val localPublisher =
        UnifiedDriveLocalProjectPublisher(rootDirectory, codec, nowEpochMs = nowEpochMs)

    suspend fun probeReadWriteDelete(): String = rootAccess.probeReadWriteDelete()

    suspend fun listCommittedVersions(): List<BackupVersionDescriptor> =
        remote.listAllHeads()
            .map { head ->
                val manifest = remote.loadManifest(head.descriptor)
                val project = loadRemoteProject(manifest.projectStateAsset, manifest)
                versionDescriptor(manifest, project)
            }
            .distinctBy { it.deduplicationKey }
            .sortedWith(
                compareByDescending<BackupVersionDescriptor> { it.backupCreatedAtEpochMs }
                    .thenByDescending { it.revisionId },
            )

    suspend fun backupAll(
        retentionPolicy: BackupRetentionPolicy,
    ): BackupRunReport {
        snapshotStore.cleanupTemporaryDirectories()
        val attempts = mutableListOf<ProjectBackupAttempt>()
        for (project in repository.list()) {
            try {
                attempts += backupProjectInternal(project.id, null)
            } catch (error: Throwable) {
                if (error is CancellationException || error is DriveAuthorizationRequiredException) {
                    throw error
                }
                attempts += ProjectBackupAttempt(
                    projectId = project.id,
                    projectName = project.name,
                    status = ProjectBackupAttempt.Status.FAILED,
                    error = error.message ?: "Falha inesperada no backup vNext.",
                )
            }
        }
        val gc = runCatching {
            collectGarbage(retentionPolicy)
        }.getOrNull()
        return BackupRunReport(
            attempts = attempts,
            deletedVersions = 0,
            retentionDeleteFailures = gc?.deleteFailures ?: 0,
            cleanedIncompleteVersions = 0,
            retentionSuppressed = gc == null,
        )
    }

    suspend fun backupProject(
        projectId: String,
        retentionPolicy: BackupRetentionPolicy,
    ): BackupRunReport {
        snapshotStore.cleanupTemporaryDirectories()
        val attempt = try {
            backupProjectInternal(projectId, null)
        } catch (error: Throwable) {
            if (error is CancellationException || error is DriveAuthorizationRequiredException) {
                throw error
            }
            val projectName = repository.load(projectId)?.name ?: "Projeto"
            ProjectBackupAttempt(
                projectId = projectId,
                projectName = projectName,
                status = ProjectBackupAttempt.Status.FAILED,
                error = error.message ?: "Falha inesperada no backup vNext.",
            )
        }
        val gc = if (attempt.status == ProjectBackupAttempt.Status.FAILED) {
            null
        } else {
            runCatching { collectGarbage(retentionPolicy) }.getOrNull()
        }
        return BackupRunReport(
            attempts = listOf(attempt),
            deletedVersions = 0,
            retentionDeleteFailures = gc?.deleteFailures ?: 0,
            cleanedIncompleteVersions = 0,
            retentionSuppressed = gc == null,
        )
    }

    suspend fun reconciliation(projectId: String): DriveReconciliation {
        val localRevision = repository.load(projectId)?.let(BackupRevisionIdentity::forProject)
        val confirmed = confirmedRevisions.confirmedRevision(projectId)
        val tips = currentTips(remote.listHeads(projectId))
        if (tips.size > 1) return DriveReconciliation.CONFLICT
        return DriveConflictResolver.resolve(
            localRevisionId = localRevision,
            confirmedRevisionId = confirmed,
            remoteRevisionId = tips.singleOrNull()?.descriptor?.revisionId,
        )
    }

    suspend fun currentRemoteTips(projectId: String): List<BackupVersionDescriptor> =
        currentTips(remote.listHeads(projectId)).map { head ->
            val manifest = remote.loadManifest(head.descriptor)
            versionDescriptor(
                manifest,
                loadRemoteProject(manifest.projectStateAsset, manifest),
            )
        }

    suspend fun keepLocal(
        projectId: String,
        remoteVersion: BackupVersionDescriptor,
        retentionPolicy: BackupRetentionPolicy,
    ): BackupRunReport {
        val remoteDescriptor = descriptor(remoteVersion)
        val tips = currentTips(remote.listHeads(projectId))
        require(tips.size == 1 && tips.single().descriptor == remoteDescriptor) {
            "A versão remota mudou; atualize o conflito antes de manter a versão local."
        }
        clearPendingTransaction(projectId)
        val project = requireNotNull(repository.load(projectId)) { "Projeto local não encontrado." }
        val attempt = backupProjectInternal(project.id, remoteDescriptor.revisionId)
        val gc = runCatching { collectGarbage(retentionPolicy) }.getOrNull()
        return BackupRunReport(
            attempts = listOf(attempt),
            deletedVersions = 0,
            retentionDeleteFailures = gc?.deleteFailures ?: 0,
            cleanedIncompleteVersions = 0,
            retentionSuppressed = gc == null,
        )
    }

    suspend fun restoreVersion(
        version: BackupVersionDescriptor,
        action: DriveConflictAction,
    ): GuitarProject {
        require(action != DriveConflictAction.KEEP_LOCAL) {
            "KEEP_LOCAL does not download a remote project."
        }
        val descriptor = descriptor(version)
        var publication: UnifiedDriveLocalPublication? = null
        val coordinator = UnifiedDriveRestoreCoordinator(
            source = remote,
            layout = UnifiedDriveProjectRestoreLayout,
            validator = UnifiedDriveLocalRestoreValidator(codec),
            publisher = DriveRestorePublisher { staging, plan ->
                publication = localPublisher.publish(staging, plan, action)
            },
            stagingRoot = File(rootDirectory, RESTORE_DIRECTORY),
        )
        coordinator.restore(descriptor)
        val published = requireNotNull(publication) {
            "Restore finished without a local publication."
        }

        if (action == DriveConflictAction.USE_DRIVE) {
            require(published.project.id == descriptor.projectId)
            clearPendingTransaction(descriptor.projectId)
            confirmedRevisions.record(descriptor)
        }
        return published.project
    }

    suspend fun restoreLatestAll(): List<RestoreAttempt> {
        val versions = listCommittedVersions()
        val heads = remote.listAllHeads().groupBy { it.descriptor.projectId }
        val selected = mutableListOf<BackupVersionDescriptor>()
        heads.forEach { (projectId, projectHeads) ->
            val tips = currentTips(projectHeads)
            if (tips.size != 1) {
                val representative = versions.firstOrNull { it.projectId == projectId }
                if (representative != null) {
                    selected += representative.copy(
                        remoteId = "conflict:${representative.remoteId}",
                    )
                }
            } else {
                val tip = tips.single().descriptor
                versions.firstOrNull {
                    it.projectId == tip.projectId &&
                        it.revisionId == tip.revisionId &&
                        it.sha256 == tip.manifestSha256
                }?.let(selected::add)
            }
        }

        return selected.map { version ->
            if (version.remoteId.startsWith("conflict:")) {
                RestoreAttempt(
                    version = version,
                    error = "O projeto possui versões remotas concorrentes e requer resolução explícita.",
                )
            } else {
                try {
                    RestoreAttempt(
                        version = version,
                        restoredProject = restoreVersion(
                            version,
                            DriveConflictAction.IMPORT_AS_COPY,
                        ),
                    )
                } catch (error: Throwable) {
                    if (error is CancellationException) throw error
                    RestoreAttempt(
                        version = version,
                        error = error.message ?: "Não foi possível restaurar o projeto.",
                    )
                }
            }
        }
    }

    fun clearProjectLocalState(projectId: String) {
        journal.clear(projectId)
        snapshotStore.remove(projectId)
        confirmedRevisions.clearProject(projectId)
    }

    fun clearAllLocalState() {
        journal.clearAll()
        snapshotStore.clearAll()
        uploadState.clearAll()
        rootAccess.clearLocalState()
        File(rootDirectory, RESTORE_DIRECTORY).deleteRecursively()
    }

    private suspend fun backupProjectInternal(
        projectId: String,
        explicitBaseRevisionId: String?,
    ): ProjectBackupAttempt {
        var confirmed = explicitBaseRevisionId ?: confirmedRevisions.confirmedRevision(projectId)
        var record = journal.load(projectId)
        var staged = snapshotStore.load(projectId)

        if (record?.stage == DriveCommitStage.HEAD_VERIFIED) {
            val exact = requireNotNull(record.confirmedRevisionId) {
                "Verified Drive transaction is missing its confirmed revision."
            }
            require(exact == record.desiredRevisionId) {
                "Verified Drive transaction confirmed the wrong revision."
            }
            if (explicitBaseRevisionId == null) {
                confirmedRevisions.record(
                    DriveCurrentDescriptor(
                        projectId,
                        exact,
                        requireNotNull(
                            staged?.manifest?.takeIf {
                                it.revisionId == record.desiredRevisionId &&
                                    it.manifestSha256 == record.manifestSha256
                            }?.manifestSha256 ?: record.manifestSha256,
                        ),
                    ),
                )
            }
            confirmed = exact
            if (
                staged != null &&
                    staged.manifest.revisionId == record.desiredRevisionId &&
                    staged.manifest.manifestSha256 == record.manifestSha256
            ) {
                snapshotStore.remove(projectId)
                staged = null
            }
            record = journal.load(projectId)
        }

        if (record != null && record.stage != DriveCommitStage.HEAD_VERIFIED) {
            val pending = requireNotNull(staged) {
                "Drive transaction journal exists but its frozen snapshot is missing."
            }
            require(
                pending.manifest.revisionId == record.desiredRevisionId &&
                    pending.manifest.manifestSha256 == record.manifestSha256 &&
                    pending.manifest.baseRevisionId == record.baseRevisionId
            ) { "Frozen Drive snapshot does not match the durable transaction journal." }
            val recovered = durableCoordinator.commit(
                pending.manifest,
                pending.localAssets,
                record.baseRevisionId,
            )
            confirmedRevisions.record(recovered.descriptor)
            confirmed = recovered.descriptor.revisionId
            snapshotStore.remove(projectId)
            staged = null
        }

        if (staged != null) {
            require(staged.manifest.baseRevisionId == confirmed) {
                "Orphaned frozen Drive snapshot has an unexpected base revision."
            }
            val recovered = durableCoordinator.commit(
                staged.manifest,
                staged.localAssets,
                confirmed,
            )
            confirmedRevisions.record(recovered.descriptor)
            confirmed = recovered.descriptor.revisionId
            snapshotStore.remove(projectId)
        }

        val project = requireNotNull(repository.load(projectId)) {
            "Projeto local não encontrado."
        }
        val desired = BackupRevisionIdentity.forProject(project)
        var commitBase = explicitBaseRevisionId ?: confirmed

        if (explicitBaseRevisionId == null) {
            val remoteTips = currentTips(remote.listHeads(project.id))
            if (remoteTips.size > 1) {
                throw DriveConflictException(
                    "O Google Drive possui versões concorrentes deste projeto.",
                )
            }
            val remoteRevision = remoteTips.singleOrNull()?.descriptor?.revisionId
            when (
                DriveConflictResolver.resolve(
                    localRevisionId = desired,
                    confirmedRevisionId = confirmed,
                    remoteRevisionId = remoteRevision,
                )
            ) {
                DriveReconciliation.NO_OP -> {
                    return ProjectBackupAttempt(
                        projectId = project.id,
                        projectName = project.name,
                        status = ProjectBackupAttempt.Status.SKIPPED_UP_TO_DATE,
                    )
                }
                DriveReconciliation.UPLOAD_LOCAL -> {
                    commitBase = if (remoteRevision == null) null else confirmed
                }
                DriveReconciliation.LOCAL_ONLY -> commitBase = null
                DriveReconciliation.DOWNLOAD_REMOTE,
                DriveReconciliation.REMOTE_ONLY,
                DriveReconciliation.CONFLICT,
                -> throw DriveConflictException(
                    "O projeto mudou localmente ou no Google Drive e requer resolução explícita.",
                )
            }
        }

        val persisted = snapshotBuilder.freeze(project, commitBase).use { frozen ->
            snapshotStore.persist(frozen)
        }
        val result = durableCoordinator.commit(
            persisted.manifest,
            persisted.localAssets,
            commitBase,
        )
        require(result.descriptor.revisionId == desired) {
            "Drive confirmed a revision different from the frozen local project."
        }
        confirmedRevisions.record(result.descriptor)
        snapshotStore.remove(project.id)

        return ProjectBackupAttempt(
            projectId = project.id,
            projectName = project.name,
            status = ProjectBackupAttempt.Status.COMMITTED,
            version = versionDescriptor(persisted.manifest, project),
        )
    }

    private suspend fun collectGarbage(retentionPolicy: BackupRetentionPolicy) =
        UnifiedDriveGarbageCollector(remote).collect(
            policy = retentionPolicy,
            pendingAssetHashes = snapshotStore.pendingAssetHashes(),
            nowEpochMs = nowEpochMs(),
            gracePeriodMs = GC_GRACE_PERIOD_MS,
        )

    private suspend fun loadRemoteProject(
        stateAsset: studio.guitarlab.core.project.DriveAssetObject?,
        manifest: studio.guitarlab.core.project.DriveProjectRevisionManifest,
    ): GuitarProject {
        val identity = requireNotNull(stateAsset) {
            "Drive manifest does not contain project.json."
        }
        val temporary = kotlin.io.path.createTempFile("guitarlab-u8-state-", ".json").toFile()
        return try {
            remote.downloadAsset(identity, temporary)
            val project = codec.decode(temporary.readText(Charsets.UTF_8))
            require(project.id == manifest.projectId) {
                "Drive project.json identity does not match its manifest."
            }
            require(UnifiedProjectRevision.sha256(project) == manifest.canonicalProjectStateSha256) {
                "Drive project.json state digest does not match its manifest."
            }
            project
        } finally {
            temporary.delete()
        }
    }

    private fun versionDescriptor(
        manifest: studio.guitarlab.core.project.DriveProjectRevisionManifest,
        project: GuitarProject,
    ): BackupVersionDescriptor = BackupVersionDescriptor(
        remoteId = "u8:${manifest.manifestSha256}",
        projectId = manifest.projectId,
        projectName = project.name,
        projectUpdatedAtEpochMs = project.updatedAtEpochMs,
        backupCreatedAtEpochMs = manifest.createdAtEpochMs,
        sizeBytes = manifest.assets.sumOf { it.sizeBytes },
        sha256 = manifest.manifestSha256,
        revisionId = manifest.revisionId,
        formatVersion = 3,
    )

    private fun descriptor(version: BackupVersionDescriptor): DriveCurrentDescriptor {
        require(version.formatVersion == 3 && version.remoteId == "u8:${version.sha256}") {
            "Selected backup is not a vNext Drive revision."
        }
        return DriveCurrentDescriptor(
            version.projectId,
            version.revisionId,
            version.sha256.lowercase(),
        )
    }

    private fun currentTips(
        heads: List<studio.guitarlab.core.project.DrivePublishedHead>,
    ): List<studio.guitarlab.core.project.DrivePublishedHead> {
        val referencedBases = heads.mapNotNullTo(mutableSetOf()) { it.baseRevisionId }
        return heads
            .filter { it.descriptor.revisionId !in referencedBases }
            .distinctBy {
                Triple(
                    it.descriptor.revisionId,
                    it.descriptor.manifestSha256,
                    it.baseRevisionId,
                )
            }
    }

    private fun clearPendingTransaction(projectId: String) {
        journal.clear(projectId)
        snapshotStore.remove(projectId)
    }

    private companion object {
        const val JOURNAL_DIRECTORY = "drive-vnext/journal"
        const val SNAPSHOT_DIRECTORY = "drive-vnext/snapshots"
        const val RESTORE_DIRECTORY = "drive-vnext/restore-staging"
        const val GC_GRACE_PERIOD_MS = 7L * 24L * 60L * 60L * 1000L
    }
}
