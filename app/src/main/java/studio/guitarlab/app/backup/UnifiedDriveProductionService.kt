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
import studio.guitarlab.core.project.DriveManifestRetentionPlanner
import studio.guitarlab.core.project.DriveProjectRevisionManifest
import studio.guitarlab.core.project.DrivePublishedHead
import studio.guitarlab.core.project.DriveReconciliation
import studio.guitarlab.core.project.UnifiedDriveBackupResult
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
    private val transportMetrics = U8mDriveTransportMetrics()
    private val http = DriveV3HttpClient(tokenProvider, observer = transportMetrics)
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

    internal suspend fun runU8mRealDriveAcceptance(): U8mRealDriveAcceptanceReport =
        U8mRealDriveAcceptanceCampaign(
            rootDirectory = rootDirectory,
            repository = repository,
            confirmedRevisions = confirmedRevisions,
            remote = remote,
            service = this,
            nowEpochMs = nowEpochMs,
        ).run()

    data class CatalogSnapshot(
        val versions: List<BackupVersionDescriptor>,
        val reconciliations: Map<String, DriveReconciliation>,
        val remoteTips: Map<String, List<BackupVersionDescriptor>>,
    )

    /**
     * Reads the Drive catalog once and derives version history plus local reconciliation from the
     * same remote snapshot. The previous UI path re-listed heads once per local project, turning a
     * screen refresh into N+1 Drive scans.
     */
    suspend fun loadCatalog(
        localProjects: List<GuitarProject>,
        retentionPolicy: BackupRetentionPolicy,
        knownVersions: List<BackupVersionDescriptor> = emptyList(),
    ): CatalogSnapshot {
        val heads = remote.listAllHeads()
        val headsByProject = heads.groupBy { it.descriptor.projectId }
        val manifestBySha = linkedMapOf<String, studio.guitarlab.core.project.DriveProjectRevisionManifest>()
        heads.forEach { head ->
            if (head.descriptor.manifestSha256 !in manifestBySha) {
                val manifest = remote.loadManifest(head.descriptor)
                manifestBySha[manifest.manifestSha256] = manifest
            }
        }

        val retained = DriveManifestRetentionPlanner.retained(
            manifests = manifestBySha.values.toList(),
            policy = retentionPolicy,
            nowEpochMs = nowEpochMs(),
        )
        val descriptorCache = knownVersions
            .filter { it.formatVersion == 3 && it.remoteId == "u8:${it.sha256}" }
            .associateByTo(linkedMapOf()) { it.sha256.lowercase() }

        suspend fun describe(manifest: studio.guitarlab.core.project.DriveProjectRevisionManifest): BackupVersionDescriptor {
            descriptorCache[manifest.manifestSha256.lowercase()]?.let { cached ->
                if (cached.projectId == manifest.projectId && cached.revisionId == manifest.revisionId) return cached
            }
            val descriptor = versionDescriptor(
                manifest,
                loadRemoteProject(manifest.projectStateAsset, manifest),
            )
            descriptorCache[manifest.manifestSha256.lowercase()] = descriptor
            return descriptor
        }

        val versions = retained.map { describe(it) }
            .distinctBy { it.deduplicationKey }
            .sortedWith(
                compareByDescending<BackupVersionDescriptor> { it.backupCreatedAtEpochMs }
                    .thenByDescending { it.revisionId },
            )

        val reconciliations = linkedMapOf<String, DriveReconciliation>()
        val remoteTips = linkedMapOf<String, List<BackupVersionDescriptor>>()
        localProjects.forEach { project ->
            val tips = currentTips(headsByProject[project.id].orEmpty())
            val reconciliation = if (tips.size > 1) {
                DriveReconciliation.CONFLICT
            } else {
                DriveConflictResolver.resolve(
                    localRevisionId = BackupRevisionIdentity.forProject(project),
                    confirmedRevisionId = confirmedRevisions.confirmedRevision(project.id),
                    remoteRevisionId = tips.singleOrNull()?.descriptor?.revisionId,
                )
            }
            reconciliations[project.id] = reconciliation
            if (reconciliation == DriveReconciliation.CONFLICT || reconciliation == DriveReconciliation.DOWNLOAD_REMOTE) {
                remoteTips[project.id] = tips.map { head ->
                    val manifest = manifestBySha[head.descriptor.manifestSha256]
                        ?: remote.loadManifest(head.descriptor).also { manifestBySha[it.manifestSha256] = it }
                    describe(manifest)
                }
            }
        }
        return CatalogSnapshot(versions, reconciliations, remoteTips)
    }

    suspend fun listCommittedVersions(
        retentionPolicy: BackupRetentionPolicy,
    ): List<BackupVersionDescriptor> {
        val manifests = remote.listAllHeads()
            .map { remote.loadManifest(it.descriptor) }
            .distinctBy { it.manifestSha256 }
        val retained = DriveManifestRetentionPlanner.retained(
            manifests = manifests,
            policy = retentionPolicy,
            nowEpochMs = nowEpochMs(),
        )
        return retained
            .map { manifest ->
                val project = loadRemoteProject(manifest.projectStateAsset, manifest)
                versionDescriptor(manifest, project)
            }
            .distinctBy { it.deduplicationKey }
            .sortedWith(
                compareByDescending<BackupVersionDescriptor> { it.backupCreatedAtEpochMs }
                    .thenByDescending { it.revisionId },
            )
    }

    suspend fun deleteProjectBackups(projectId: String): Int {
        val deleted = remote.deleteProject(projectId)
        clearProjectLocalState(projectId)
        return deleted
    }

    suspend fun backupAll(
        retentionPolicy: BackupRetentionPolicy,
    ): BackupRunReport {
        snapshotStore.cleanupTemporaryDirectories()
        val attempts = mutableListOf<ProjectBackupAttempt>()
        for (project in repository.list()) {
            try {
                attempts += backupProjectInternal(project.id, null)
            } catch (error: Throwable) {
                if (
                    error is CancellationException ||
                        error is DriveAuthorizationRequiredException ||
                        isTransientDriveFailure(error)
                ) {
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
            if (
                error is CancellationException ||
                    error is DriveAuthorizationRequiredException ||
                    isTransientDriveFailure(error)
            ) {
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
        val versions = listCommittedVersions(BackupRetentionPolicy(maxAgeDays = null, maximumVersionsPerProject = Int.MAX_VALUE))
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


    internal suspend fun u8mCommitProject(
        projectId: String,
        explicitBaseRevisionId: String? = null,
    ): U8mBackupEvidence {
        snapshotStore.cleanupTemporaryDirectories()
        val project = requireNotNull(repository.load(projectId)) { "Projeto local não encontrado." }
        val metricsBefore = transportMetrics.snapshot()
        val started = System.nanoTime()
        val mediaBefore = project.assets
            .distinctBy { it.sha256 }
            .associate { asset -> asset.sha256 to (remote.findAsset(asset.sha256) != null) }
        var committed: Pair<DriveProjectRevisionManifest, UnifiedDriveBackupResult>? = null
        val attempt = backupProjectInternal(projectId, explicitBaseRevisionId) { manifest, result ->
            committed = manifest to result
        }
        val elapsedMs = (System.nanoTime() - started) / 1_000_000L
        val (manifest, result) = requireNotNull(committed) {
            "U8m expected a committed Drive revision but the production path skipped it."
        }
        val newMedia = project.assets
            .distinctBy { it.sha256 }
            .filter { mediaBefore[it.sha256] == false }
        val metricsAfter = transportMetrics.snapshot()
        return U8mBackupEvidence(
            attempt = attempt,
            manifest = manifest,
            result = result,
            mediaUploadedObjects = newMedia.size,
            mediaUploadedBytes = newMedia.sumOf { it.byteSize },
            durationMs = elapsedMs,
            retryCount = metricsAfter.retryCount - metricsBefore.retryCount,
            authorizationRefreshCount =
                metricsAfter.authorizationRefreshCount - metricsBefore.authorizationRefreshCount,
            observedHeadRevisions =
                remote.listHeads(projectId).map { it.descriptor.revisionId }.distinct(),
            confirmedRevision = confirmedRevisions.confirmedRevision(projectId),
        )
    }

    internal suspend fun u8mKeepLocal(
        projectId: String,
        remoteVersion: BackupVersionDescriptor,
    ): U8mBackupEvidence {
        val remoteDescriptor = descriptor(remoteVersion)
        val tips = currentTips(remote.listHeads(projectId))
        require(tips.size == 1 && tips.single().descriptor == remoteDescriptor) {
            "U8m keep-local requires one exact remote tip."
        }
        clearPendingTransaction(projectId)
        return u8mCommitProject(projectId, remoteDescriptor.revisionId)
    }

    internal suspend fun u8mAdvanceRemote(
        project: GuitarProject,
        baseRevisionId: String,
    ): DriveCurrentDescriptor =
        snapshotBuilder.freeze(project, baseRevisionId).use { frozen ->
            UnifiedDriveBackupCoordinator(remote)
                .commit(frozen.manifest, frozen.localAssets, baseRevisionId)
                .descriptor
        }

    /**
     * U8m adversarial constructor for the documented append-only head race.
     *
     * This intentionally bypasses the coordinator's pre-publish base check only after the
     * immutable objects and manifest have been verified through the production remote store.
     * It exists solely to create the second concurrent sibling required to prove that the
     * production reconciliation path fails closed when multiple tips are actually present.
     */
    internal suspend fun u8mPublishConcurrentSiblingForAcceptance(
        project: GuitarProject,
        baseRevisionId: String,
    ): DriveCurrentDescriptor =
        snapshotBuilder.freeze(project, baseRevisionId).use { frozen ->
            frozen.localAssets.forEach { local ->
                val receipt = remote.findAsset(local.identity.sha256)
                    ?: remote.uploadAsset(local)
                require(receipt.matches(local.identity)) {
                    "U8m concurrent sibling asset verification failed."
                }
            }
            val manifestIdentity = studio.guitarlab.core.project.DriveAssetObject(
                frozen.manifest.manifestSha256,
                frozen.manifest.canonicalBytes().size.toLong(),
            )
            val manifestReceipt = remote.findManifest(frozen.manifest.manifestSha256)
                ?: remote.uploadManifest(frozen.manifest)
            require(manifestReceipt.matches(manifestIdentity)) {
                "U8m concurrent sibling manifest verification failed."
            }
            val descriptor = DriveCurrentDescriptor(
                frozen.manifest.projectId,
                frozen.manifest.revisionId,
                frozen.manifest.manifestSha256,
            )
            remote.publishHead(DrivePublishedHead(descriptor, baseRevisionId))
            val exact = remote.listHeads(project.id).filter {
                it.descriptor.revisionId == descriptor.revisionId
            }
            require(exact.size == 1 && exact.single().descriptor == descriptor) {
                "U8m concurrent sibling head was not read back exactly."
            }
            descriptor
        }

    private suspend fun backupProjectInternal(
        projectId: String,
        explicitBaseRevisionId: String?,
        onCommitted: ((DriveProjectRevisionManifest, UnifiedDriveBackupResult) -> Unit)? = null,
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
        onCommitted?.invoke(persisted.manifest, result)
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
