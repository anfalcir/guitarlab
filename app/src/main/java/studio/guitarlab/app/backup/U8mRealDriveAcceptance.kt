package studio.guitarlab.app.backup

import java.io.File
import java.security.MessageDigest
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import studio.guitarlab.core.model.AssetClassification
import studio.guitarlab.core.model.AssetLifecycle
import studio.guitarlab.core.model.AssetProvenance
import studio.guitarlab.core.model.AssetRole
import studio.guitarlab.core.model.AudioClip
import studio.guitarlab.core.model.AudioTrack
import studio.guitarlab.core.model.BuiltInRoles
import studio.guitarlab.core.model.GuitarProject
import studio.guitarlab.core.model.ManagedAsset
import studio.guitarlab.core.model.PreparationState
import studio.guitarlab.core.model.PreparationStatus
import studio.guitarlab.core.model.ProjectTemplate
import studio.guitarlab.core.model.ProjectValidator
import studio.guitarlab.core.model.RecordingTake
import studio.guitarlab.core.model.RoleSource
import studio.guitarlab.core.project.BackupHashing
import studio.guitarlab.core.project.BackupRetentionPolicy
import studio.guitarlab.core.project.DriveAssetObject
import studio.guitarlab.core.project.DriveConflictAction
import studio.guitarlab.core.project.DriveConflictActionPolicy
import studio.guitarlab.core.project.DriveGarbageCollectionReport
import studio.guitarlab.core.project.DriveLocalAsset
import studio.guitarlab.core.project.DriveReconciliation
import studio.guitarlab.core.project.FileProjectRepository
import studio.guitarlab.core.project.ProjectBackupAttempt
import studio.guitarlab.core.project.UnifiedDriveBackupResult
import studio.guitarlab.core.project.UnifiedDriveGarbageCollectionStore
import studio.guitarlab.core.project.UnifiedDriveGarbageCollector

internal object U8mRealDriveAcceptanceContract {
    const val PROJECT_PREFIX = "u8m-real-drive-"
    const val REPORT_DIRECTORY = "diagnostics/u8m-real-drive"
    const val GC_GRACE_PERIOD_MS = 7L * 24L * 60L * 60L * 1000L

    fun isCampaignProjectId(projectId: String): Boolean =
        projectId.startsWith(PROJECT_PREFIX) &&
            projectId.length >= PROJECT_PREFIX.length + 32 &&
            projectId.none(Char::isWhitespace)
}

internal data class U8mTransportMetrics(
    val retryCount: Int,
    val authorizationRefreshCount: Int,
)

internal class U8mDriveTransportMetrics : DriveTransportObserver {
    private var retries = 0
    private var authorizationRefreshes = 0

    @Synchronized override fun onRetry(operation: String) {
        retries++
    }

    @Synchronized override fun onAuthorizationRefresh() {
        authorizationRefreshes++
    }

    @Synchronized fun snapshot(): U8mTransportMetrics =
        U8mTransportMetrics(retries, authorizationRefreshes)
}

internal data class U8mBackupEvidence(
    val attempt: ProjectBackupAttempt,
    val manifest: studio.guitarlab.core.project.DriveProjectRevisionManifest,
    val result: UnifiedDriveBackupResult,
    val mediaUploadedObjects: Int,
    val mediaUploadedBytes: Long,
    val durationMs: Long,
    val retryCount: Int,
    val authorizationRefreshCount: Int,
    val observedHeadRevisions: List<String>,
    val confirmedRevision: String?,
) {
    val metadataAndManifestBytes: Long
        get() = result.uploadedBytes - mediaUploadedBytes + manifest.canonicalBytes().size

    val deduplicatedPathCount: Int
        get() = manifest.fileEntries.size -
            manifest.fileEntries.map { it.asset.sha256 }.distinct().size
}

internal data class U8mRemoteCleanupResult(
    val deletedHeads: Int,
    val deletedManifests: Int,
    val deletedAssets: Int,
    val protectedSharedAssets: Int,
)

internal data class U8mRealDriveAcceptanceReport(
    val campaignId: String,
    val projectId: String,
    val passed: Boolean,
    val phase: String,
    val first: U8mBackupEvidence? = null,
    val metadataOnly: U8mBackupEvidence? = null,
    val newTake: U8mBackupEvidence? = null,
    val restoreVerified: Boolean = false,
    val partialPublicationProtected: Boolean = false,
    val importAsCopyVerified: Boolean = false,
    val conflictVerified: Boolean = false,
    val conflictActionsVerified: Boolean = false,
    val keepLocalVerified: Boolean = false,
    val useDriveVerified: Boolean = false,
    val ambiguousHeadsFailClosed: Boolean = false,
    val gcRecent: DriveGarbageCollectionReport? = null,
    val gcFuture: DriveGarbageCollectionReport? = null,
    val confirmedRevisionAfterGc: String? = null,
    val cleanup: U8mRemoteCleanupResult? = null,
    val cleanupLocalProjects: Int = 0,
    val error: String? = null,
    val reportFile: String? = null,
    val reportSha256: String? = null,
) {
    fun summary(): String = if (passed) {
        "U8m PASS · ${newTake?.result?.descriptor?.revisionId?.take(12)} · " +
            "cleanup ${cleanup?.deletedHeads ?: 0}/${cleanup?.deletedManifests ?: 0}/${cleanup?.deletedAssets ?: 0}"
    } else {
        "U8m FAIL em $phase${error?.let { ": $it" }.orEmpty()}"
    }

    fun toSanitizedJson(): String = buildJsonObject {
        put("schema", 1)
        put("campaignId", campaignId)
        put("projectId", projectId)
        put("passed", passed)
        put("phase", phase)
        put("restoreVerified", restoreVerified)
        put("partialPublicationProtected", partialPublicationProtected)
        put("importAsCopyVerified", importAsCopyVerified)
        put("conflictVerified", conflictVerified)
        put("conflictActionsVerified", conflictActionsVerified)
        put("keepLocalVerified", keepLocalVerified)
        put("useDriveVerified", useDriveVerified)
        put("ambiguousHeadsFailClosed", ambiguousHeadsFailClosed)
        confirmedRevisionAfterGc?.let { put("confirmedRevisionAfterGc", it) }
        error?.let { put("error", it.take(500)) }
        first?.let { put("firstSync", it.json()) }
        metadataOnly?.let { put("metadataOnly", it.json()) }
        newTake?.let { put("newTake", it.json()) }
        gcRecent?.let { put("gcRecent", it.json()) }
        gcFuture?.let { put("gcFuture", it.json()) }
        cleanup?.let {
            put("cleanup", buildJsonObject {
                put("deletedHeads", it.deletedHeads)
                put("deletedManifests", it.deletedManifests)
                put("deletedAssets", it.deletedAssets)
                put("protectedSharedAssets", it.protectedSharedAssets)
                put("localProjectsDeleted", cleanupLocalProjects)
            })
        }
    }.toString()

    private fun U8mBackupEvidence.json() = buildJsonObject {
        put("revisionId", result.descriptor.revisionId)
        put("manifestSha256", manifest.manifestSha256)
        put("objectSha256", buildJsonArray {
            manifest.assets.map { it.sha256 }.distinct().sorted().forEach {
                add(JsonPrimitive(it))
            }
        })
        put("uploadedObjects", result.uploadedAssets)
        put("uploadedContentObjectBytes", result.uploadedBytes)
        put("mediaUploadedObjects", mediaUploadedObjects)
        put("mediaUploadedBytes", mediaUploadedBytes)
        put("metadataAndManifestBytes", metadataAndManifestBytes)
        put("deduplicatedPaths", deduplicatedPathCount)
        put("durationMs", durationMs)
        put("retryCount", retryCount)
        put("authorizationRefreshCount", authorizationRefreshCount)
        put("recoveredLostPublishResponse", result.recoveredLostPublishResponse)
        confirmedRevision?.let { put("confirmedRevision", it) }
        put("headsObserved", buildJsonArray {
            observedHeadRevisions.forEach { add(JsonPrimitive(it)) }
        })
    }

    private fun DriveGarbageCollectionReport.json() = buildJsonObject {
        put("retainedManifests", retainedManifests)
        put("candidates", candidates)
        put("deleted", deleted)
        put("deleteFailures", deleteFailures)
    }
}

internal class U8mRealDriveAcceptanceCampaign(
    private val rootDirectory: File,
    private val repository: FileProjectRepository,
    private val confirmedRevisions: ConfirmedRevisionStore,
    private val remote: UnifiedDriveV3RemoteStore,
    private val service: UnifiedDriveProductionService,
    private val nowEpochMs: () -> Long,
) {
    suspend fun run(): U8mRealDriveAcceptanceReport {
        val campaignId = UUID.randomUUID().toString()
        val projectId = U8mRealDriveAcceptanceContract.PROJECT_PREFIX + campaignId
        val localIds = linkedSetOf(projectId)
        val extraOwnedHashes = linkedSetOf<String>()
        var phase = "fixture"
        var first: U8mBackupEvidence? = null
        var metadata: U8mBackupEvidence? = null
        var newTake: U8mBackupEvidence? = null
        var restoreVerified = false
        var partialProtected = false
        var copyVerified = false
        var conflictVerified = false
        var conflictActionsVerified = false
        var keepLocalVerified = false
        var useDriveVerified = false
        var ambiguousHeadsFailClosed = false
        var gcRecent: DriveGarbageCollectionReport? = null
        var gcFuture: DriveGarbageCollectionReport? = null
        var confirmedAfterGc: String? = null
        var failure: Throwable? = null
        var cleanup: U8mRemoteCleanupResult? = null
        var localDeleted = 0

        try {
            val fixture = U8mRealDriveFixture.create(
                rootDirectory, projectId, campaignId, nowEpochMs(),
            )
            repository.save(fixture.project)

            phase = "first-sync"
            first = service.u8mCommitProject(projectId)
            require(first.attempt.status == ProjectBackupAttempt.Status.COMMITTED)
            require(first.confirmedRevision == first.result.descriptor.revisionId)
            require(first.deduplicatedPathCount >= 1) {
                "First sync did not prove duplicate-content path deduplication."
            }
            require(first.mediaUploadedObjects == fixture.uniqueMediaHashes.size) {
                "First sync did not upload the expected unique media object set."
            }

            phase = "metadata-only"
            repository.save(requireNotNull(repository.load(projectId)).copy(
                masterGainDb = -1.25f,
                updatedAtEpochMs = nowEpochMs() + 1L,
            ))
            metadata = service.u8mCommitProject(projectId)
            require(metadata.mediaUploadedObjects == 0 && metadata.result.uploadedAssets == 1) {
                "Metadata-only backup re-uploaded media or skipped the project-state object."
            }
            require(metadata.confirmedRevision == metadata.result.descriptor.revisionId)

            phase = "new-take"
            val withTake = U8mRealDriveFixture.addSecondTake(
                rootDirectory,
                requireNotNull(repository.load(projectId)),
                campaignId,
                nowEpochMs() + 2L,
            )
            repository.save(withTake.project)
            newTake = service.u8mCommitProject(projectId)
            require(newTake.mediaUploadedObjects == 1 && newTake.result.uploadedAssets == 2) {
                "New-take backup did not upload one media object plus project state."
            }
            require(newTake.confirmedRevision == newTake.result.descriptor.revisionId)
            require(newTake.manifest.fileEntries.any { it.asset.sha256 == withTake.newTakeHash })

            val latestVersion = requireNotNull(newTake.attempt.version)
            val expectedRemote = requireNotNull(repository.load(projectId))

            repository.save(
                expectedRemote.copy(
                    masterGainDb = -7f,
                    updatedAtEpochMs = nowEpochMs() + 3L,
                ),
            )

            phase = "partial-restore"
            val beforeFailedRestore = requireNotNull(repository.load(projectId))
            val missingEntry = newTake.manifest.fileEntries.single {
                it.asset.sha256 == withTake.newTakeHash
            }
            val missingAsset = missingEntry.asset
            val referencedOutsideCampaign = remote.listCommittedManifests()
                .filter { it.projectId != projectId }
                .any { manifest ->
                    manifest.assets.any { it.sha256 == missingAsset.sha256 }
                }
            require(!referencedOutsideCampaign) {
                "U8m partial-restore injection refused a cross-project reachable object."
            }
            val localMissingFile = File(
                File(File(rootDirectory, "projects"), projectId),
                missingEntry.relativePath,
            )
            require(remote.deleteAsset(missingAsset)) {
                "U8m could not remove the isolated test object for partial-restore injection."
            }
            try {
                val failed = runCatching {
                    service.restoreVersion(latestVersion, DriveConflictAction.USE_DRIVE)
                }.isFailure
                partialProtected = failed &&
                    repository.load(projectId) == beforeFailedRestore &&
                    U8mRealDriveFixture.noRestorePublicationStaging(rootDirectory)
                require(partialProtected) {
                    "Failed restore left partial local publication."
                }
            } finally {
                remote.uploadAsset(DriveLocalAsset(missingAsset, localMissingFile))
            }

            phase = "restore"
            val restored = service.restoreVersion(latestVersion, DriveConflictAction.USE_DRIVE)
            restoreVerified = restored == expectedRemote &&
                repository.load(projectId) == expectedRemote &&
                U8mRealDriveFixture.verifyFiles(rootDirectory, restored)
            require(restoreVerified) { "Exact restore verification failed." }

            phase = "import-as-copy"
            val copy = service.restoreVersion(latestVersion, DriveConflictAction.IMPORT_AS_COPY)
            localIds += copy.id
            copyVerified = copy.id != projectId &&
                repository.load(projectId) == expectedRemote &&
                repository.load(copy.id) == copy &&
                copy.assets.map { it.sha256 to it.relativePath }.toSet() ==
                    expectedRemote.assets.map { it.sha256 to it.relativePath }.toSet() &&
                U8mRealDriveFixture.verifyFiles(rootDirectory, copy)
            require(copyVerified) { "Import-as-copy verification failed." }

            phase = "conflict-1"
            val baseRevision = requireNotNull(confirmedRevisions.confirmedRevision(projectId))
            val remoteProject = expectedRemote.copy(
                masterGainDb = 2f,
                updatedAtEpochMs = nowEpochMs() + 4L,
            )
            val remoteAdvance = service.u8mAdvanceRemote(remoteProject, baseRevision)
            val localProject = expectedRemote.copy(
                masterGainDb = -3f,
                updatedAtEpochMs = nowEpochMs() + 5L,
            )
            repository.save(localProject)
            val reconciliation = service.reconciliation(projectId)
            conflictVerified = reconciliation == DriveReconciliation.CONFLICT
            conflictActionsVerified =
                DriveConflictActionPolicy.allowed(reconciliation) ==
                    DriveConflictAction.entries.toSet()
            require(conflictVerified && conflictActionsVerified) {
                "Real Drive conflict was not exposed with all safe resolution actions."
            }
            val remoteTip = service.currentRemoteTips(projectId).single().also {
                require(it.revisionId == remoteAdvance.revisionId)
            }
            val conflictCopy = service.restoreVersion(
                remoteTip,
                DriveConflictAction.IMPORT_AS_COPY,
            )
            localIds += conflictCopy.id
            require(repository.load(projectId) == localProject) {
                "Conflict import-as-copy modified the original local project."
            }

            phase = "keep-local"
            val kept = service.u8mKeepLocal(projectId, remoteTip)
            keepLocalVerified =
                kept.confirmedRevision == kept.result.descriptor.revisionId &&
                    kept.manifest.baseRevisionId == remoteTip.revisionId &&
                    repository.load(projectId) == localProject
            require(keepLocalVerified) { "Keep-local conflict resolution failed." }

            phase = "conflict-2"
            val keepRevision = requireNotNull(confirmedRevisions.confirmedRevision(projectId))
            val remoteProject2 = localProject.copy(
                masterGainDb = 3f,
                updatedAtEpochMs = nowEpochMs() + 6L,
            )
            val remoteAdvance2 = service.u8mAdvanceRemote(remoteProject2, keepRevision)
            repository.save(
                localProject.copy(
                    masterGainDb = -4f,
                    updatedAtEpochMs = nowEpochMs() + 7L,
                ),
            )
            require(service.reconciliation(projectId) == DriveReconciliation.CONFLICT)
            val remoteTip2 = service.currentRemoteTips(projectId).single().also {
                require(it.revisionId == remoteAdvance2.revisionId)
            }

            phase = "use-drive"
            val usedDrive = service.restoreVersion(remoteTip2, DriveConflictAction.USE_DRIVE)
            useDriveVerified =
                usedDrive == remoteProject2 &&
                    confirmedRevisions.confirmedRevision(projectId) == remoteTip2.revisionId &&
                    service.currentRemoteTips(projectId).single().revisionId == remoteTip2.revisionId
            require(useDriveVerified) { "Use-Drive conflict resolution failed." }

            phase = "gc"
            val gcDelete = uploadOrphan(campaignId, "delete")
            val gcFail = uploadOrphan(campaignId, "fail")
            val gcPending = uploadOrphan(campaignId, "pending")
            extraOwnedHashes += setOf(gcDelete.sha256, gcFail.sha256, gcPending.sha256)
            val currentTipForGc = service.currentRemoteTips(projectId).single()
            val currentManifestForGc = remote.loadManifest(
                studio.guitarlab.core.project.DriveCurrentDescriptor(
                    projectId,
                    currentTipForGc.revisionId,
                    currentTipForGc.sha256,
                ),
            )
            val reachableOwnedHashes = currentManifestForGc.assets.mapTo(mutableSetOf()) {
                it.sha256
            }
            val owned = buildSet {
                add(gcDelete.sha256)
                add(gcFail.sha256)
                add(gcPending.sha256)
                addAll(reachableOwnedHashes)
            }
            val policy = BackupRetentionPolicy(
                maxAgeDays = null,
                maximumVersionsPerProject = 2,
            )
            val scoped = U8mScopedGcStore(remote, projectId, owned, gcFail.sha256)
            gcRecent = UnifiedDriveGarbageCollector(scoped).collect(
                policy = policy,
                pendingAssetHashes = setOf(gcPending.sha256),
                nowEpochMs = nowEpochMs(),
                gracePeriodMs = U8mRealDriveAcceptanceContract.GC_GRACE_PERIOD_MS,
            )
            require(gcRecent.candidates == 0) {
                "GC grace period did not protect fresh U8m objects."
            }
            gcFuture = UnifiedDriveGarbageCollector(scoped).collect(
                policy = policy,
                pendingAssetHashes = setOf(gcPending.sha256),
                nowEpochMs = nowEpochMs() +
                    U8mRealDriveAcceptanceContract.GC_GRACE_PERIOD_MS + 1L,
                gracePeriodMs = U8mRealDriveAcceptanceContract.GC_GRACE_PERIOD_MS,
            )
            require(
                gcFuture.candidates == 2 &&
                    gcFuture.deleted == 1 &&
                    gcFuture.deleteFailures == 1
            ) { "Scoped real-Drive GC did not prove delete/best-effort semantics." }
            reachableOwnedHashes.forEach { hash ->
                require(remote.findAsset(hash) != null) {
                    "GC deleted an object reachable from the newest retained manifest."
                }
            }
            confirmedAfterGc = confirmedRevisions.confirmedRevision(projectId)
            require(confirmedAfterGc == remoteTip2.revisionId)
            require(service.currentRemoteTips(projectId).single().revisionId == remoteTip2.revisionId)

            phase = "ambiguous-heads"
            val ambiguityBase = remoteTip2.revisionId
            val branchAProject = remoteProject2.copy(
                masterGainDb = 4f,
                updatedAtEpochMs = nowEpochMs() + 8L,
            )
            val branchA = service.u8mAdvanceRemote(branchAProject, ambiguityBase)
            val branchBProject = remoteProject2.copy(
                masterGainDb = 5f,
                updatedAtEpochMs = nowEpochMs() + 9L,
            )
            val branchB = service.u8mPublishConcurrentSiblingForAcceptance(
                branchBProject,
                ambiguityBase,
            )
            val ambiguousTips = service.currentRemoteTips(projectId)
            ambiguousHeadsFailClosed =
                ambiguousTips.map { it.revisionId }.toSet() ==
                    setOf(branchA.revisionId, branchB.revisionId) &&
                    service.reconciliation(projectId) == DriveReconciliation.CONFLICT
            require(ambiguousHeadsFailClosed) {
                "Multiple real Drive tips were not rejected fail-closed."
            }
        } catch (error: Throwable) {
            failure = error
        } finally {
            withContext(NonCancellable) {
                phase = if (failure == null) "cleanup" else "cleanup-after-$phase"
                cleanup = runCatching {
                    remote.cleanupAcceptanceProject(projectId, extraOwnedHashes)
                }.getOrElse { cleanupError ->
                    if (failure == null) {
                        failure = cleanupError
                    } else {
                        failure?.addSuppressed(cleanupError)
                    }
                    null
                }
                localIds.forEach { id ->
                    if (repository.delete(id)) localDeleted++
                    service.clearProjectLocalState(id)
                }
            }
        }

        val passed = failure == null &&
            restoreVerified &&
            partialProtected &&
            copyVerified &&
            conflictVerified &&
            conflictActionsVerified &&
            keepLocalVerified &&
            useDriveVerified &&
            ambiguousHeadsFailClosed &&
            cleanup != null
        val report = U8mRealDriveAcceptanceReport(
            campaignId = campaignId,
            projectId = projectId,
            passed = passed,
            phase = if (passed) "complete" else phase,
            first = first,
            metadataOnly = metadata,
            newTake = newTake,
            restoreVerified = restoreVerified,
            partialPublicationProtected = partialProtected,
            importAsCopyVerified = copyVerified,
            conflictVerified = conflictVerified,
            conflictActionsVerified = conflictActionsVerified,
            keepLocalVerified = keepLocalVerified,
            useDriveVerified = useDriveVerified,
            ambiguousHeadsFailClosed = ambiguousHeadsFailClosed,
            gcRecent = gcRecent,
            gcFuture = gcFuture,
            confirmedRevisionAfterGc = confirmedAfterGc,
            cleanup = cleanup,
            cleanupLocalProjects = localDeleted,
            error = failure?.message,
        )
        val reportFile = withContext(NonCancellable) { persist(report) }
        val finalReport = report.copy(
            reportFile = reportFile.absolutePath,
            reportSha256 = BackupHashing.sha256(reportFile),
        )
        if (failure is CancellationException) throw failure as CancellationException
        return finalReport
    }

    private suspend fun uploadOrphan(campaignId: String, label: String): DriveAssetObject {
        val temporary = kotlin.io.path.createTempFile(
            "guitarlab-u8m-$label-",
            ".bin",
        ).toFile()
        return try {
            temporary.writeBytes(
                ("U8m:$campaignId:$label:" + "x".repeat(64)).toByteArray(),
            )
            val identity = DriveAssetObject(
                BackupHashing.sha256(temporary),
                temporary.length(),
            )
            remote.uploadAsset(DriveLocalAsset(identity, temporary))
            identity
        } finally {
            temporary.delete()
        }
    }

    private fun persist(report: U8mRealDriveAcceptanceReport): File {
        val directory = File(
            rootDirectory,
            U8mRealDriveAcceptanceContract.REPORT_DIRECTORY,
        ).apply { mkdirs() }
        return File(directory, "latest.json").apply {
            writeText(report.toSanitizedJson(), Charsets.UTF_8)
        }
    }
}

private class U8mScopedGcStore(
    private val remote: UnifiedDriveV3RemoteStore,
    private val projectId: String,
    private val ownedHashes: Set<String>,
    private val failHash: String,
) : UnifiedDriveGarbageCollectionStore {
    override suspend fun listCommittedManifests() =
        remote.listCommittedManifests()

    override suspend fun listAssets() =
        remote.listAssets().filter { it.asset.sha256 in ownedHashes }

    override suspend fun deleteAsset(asset: DriveAssetObject): Boolean {
        require(asset.sha256 in ownedHashes) {
            "Scoped U8m GC attempted to delete a foreign object."
        }
        if (asset.sha256 == failHash) return false
        val referencedOutside = remote.listCommittedManifests()
            .filter { it.projectId != projectId }
            .any { manifest -> manifest.assets.any { it.sha256 == asset.sha256 } }
        require(!referencedOutside) {
            "Scoped U8m GC refused a cross-project reachable object."
        }
        return remote.deleteAsset(asset)
    }
}

internal object U8mRealDriveFixture {
    data class Created(
        val project: GuitarProject,
        val uniqueMediaHashes: Set<String>,
    )

    data class WithTake(
        val project: GuitarProject,
        val newTakeHash: String,
    )

    fun create(
        rootDirectory: File,
        projectId: String,
        campaignId: String,
        now: Long,
    ): Created {
        require(U8mRealDriveAcceptanceContract.isCampaignProjectId(projectId))
        val projectDir = File(File(rootDirectory, "projects"), projectId).apply {
            mkdirs()
        }
        val source = writeAsset(
            projectDir, "source", AssetRole.SOURCE_ORIGINAL,
            "media/source/source.wav", bytes(campaignId, "duplicate"), now,
            AssetClassification.AUTHORITATIVE,
        )
        val guitar = writeAsset(
            projectDir, "guitar", AssetRole.REFERENCE_GUITAR,
            "media/references/guitar.wav", bytes(campaignId, "duplicate"), now,
            AssetClassification.DERIVED,
            AssetProvenance(
                kind = "u8m-reference",
                inputAssetIds = listOf(source.assetId),
                inputSha256 = listOf(source.sha256),
            ),
        )
        val backing = writeAsset(
            projectDir, "backing", AssetRole.REFERENCE_BACKING,
            "media/references/backing.wav", bytes(campaignId, "backing"), now,
            AssetClassification.DERIVED,
            AssetProvenance(
                kind = "u8m-backing",
                inputAssetIds = listOf(source.assetId),
                inputSha256 = listOf(source.sha256),
            ),
        )
        val take = writeAsset(
            projectDir, "take-1-asset", AssetRole.RECORDING_TAKE,
            "media/source/take-1.wav", bytes(campaignId, "take-1"), now,
            AssetClassification.AUTHORITATIVE,
        )
        val track = AudioTrack(
            id = "u8m-track",
            name = "Minha guitarra",
            roleId = BuiltInRoles.RECORDED_GUITAR,
            roleSource = RoleSource.AUTO,
            order = 0,
        )
        val clip = AudioClip(
            id = "u8m-clip-1",
            trackId = track.id,
            name = "Take 1",
            sourceUri = "guitarlab://u8m/take-1",
            startFrame = 0L,
            lengthFrames = 64L,
            managedSourcePath = take.relativePath,
            takeId = "u8m-take-1",
        )
        val recordingTake = RecordingTake(
            id = "u8m-take-1",
            trackId = track.id,
            clipId = clip.id,
            name = "Take 1",
            createdAtEpochMs = now,
            active = true,
        )
        val project = GuitarProject(
            id = projectId,
            name = "U8m Drive",
            template = ProjectTemplate.GUITAR,
            createdAtEpochMs = now,
            updatedAtEpochMs = now,
            tracks = listOf(track),
            clips = listOf(clip),
            takes = listOf(recordingTake),
            assets = listOf(source, guitar, backing, take),
            preparation = PreparationState(
                status = PreparationStatus.READY,
                sourceAssetId = source.assetId,
                activeBackingAssetId = backing.assetId,
                activeGuitarAssetId = guitar.assetId,
                availableReferenceAssetIds = listOf(backing.assetId, guitar.assetId),
            ),
        )
        require(ProjectValidator.validate(project).isEmpty())
        return Created(project, project.assets.map { it.sha256 }.toSet())
    }

    fun addSecondTake(
        rootDirectory: File,
        project: GuitarProject,
        campaignId: String,
        now: Long,
    ): WithTake {
        val projectDir = File(File(rootDirectory, "projects"), project.id)
        val asset = writeAsset(
            projectDir, "take-2-asset", AssetRole.RECORDING_TAKE,
            "media/source/take-2.wav", bytes(campaignId, "take-2"), now,
            AssetClassification.AUTHORITATIVE,
        )
        val track = project.tracks.single { it.id == "u8m-track" }
        val clip = AudioClip(
            id = "u8m-clip-2",
            trackId = track.id,
            name = "Take 2",
            sourceUri = "guitarlab://u8m/take-2",
            startFrame = 64L,
            lengthFrames = 64L,
            managedSourcePath = asset.relativePath,
            takeId = "u8m-take-2",
        )
        val take = RecordingTake(
            id = "u8m-take-2",
            trackId = track.id,
            clipId = clip.id,
            name = "Take 2",
            createdAtEpochMs = now,
            active = true,
        )
        val updated = project.copy(
            updatedAtEpochMs = now,
            clips = project.clips + clip,
            takes = project.takes.map { it.copy(active = false) } + take,
            assets = project.assets + asset,
        )
        require(ProjectValidator.validate(updated).isEmpty())
        return WithTake(updated, asset.sha256)
    }

    fun verifyFiles(rootDirectory: File, project: GuitarProject): Boolean {
        val projectDir = File(File(rootDirectory, "projects"), project.id)
        return project.assets.all { asset ->
            val file = File(projectDir, asset.relativePath)
            file.isFile &&
                file.length() == asset.byteSize &&
                BackupHashing.sha256(file) == asset.sha256
        }
    }

    fun noRestorePublicationStaging(rootDirectory: File): Boolean {
        val projects = File(rootDirectory, "projects")
        val publication = projects.listFiles().orEmpty().any {
            it.name.startsWith(".restore-publish-") ||
                it.name.startsWith(".restore-previous-")
        }
        val restoreRoot = File(rootDirectory, "drive-vnext/restore-staging")
        val restoreFiles = restoreRoot.isDirectory &&
            restoreRoot.walkTopDown().drop(1).any()
        return !publication && !restoreFiles
    }

    private fun writeAsset(
        projectDir: File,
        assetId: String,
        role: AssetRole,
        relativePath: String,
        payload: ByteArray,
        now: Long,
        classification: AssetClassification,
        provenance: AssetProvenance? = null,
    ): ManagedAsset {
        val file = File(projectDir, relativePath).apply {
            parentFile?.mkdirs()
            writeBytes(payload)
        }
        return ManagedAsset(
            assetId = assetId,
            role = role,
            relativePath = relativePath,
            sha256 = BackupHashing.sha256(file),
            byteSize = file.length(),
            format = "wav",
            sampleRateHz = 48_000,
            channelCount = 1,
            frameCount = 64L,
            createdAtEpochMs = now,
            classification = classification,
            lifecycle = AssetLifecycle.MANAGED,
            provenance = provenance,
        )
    }

    private fun bytes(campaignId: String, label: String): ByteArray =
        MessageDigest.getInstance("SHA-256")
            .digest("$campaignId:$label".toByteArray())
            .let { digest ->
                ByteArray(512) { index -> digest[index % digest.size] }
            }
}
