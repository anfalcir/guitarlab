package studio.guitarlab.core.project

data class DriveGarbageCollectionReport(
    val retainedManifests: Int,
    val candidates: Int,
    val deleted: Int,
    val deleteFailures: Int,
)

interface UnifiedDriveGarbageCollectionStore {
    suspend fun listCommittedManifests(): List<DriveProjectRevisionManifest>
    suspend fun listAssets(): List<DriveGcCandidate>
    suspend fun deleteAsset(asset: DriveAssetObject): Boolean
}

object DriveManifestRetentionPlanner {
    fun retained(
        manifests: List<DriveProjectRevisionManifest>,
        policy: BackupRetentionPolicy,
        nowEpochMs: Long,
    ): List<DriveProjectRevisionManifest> = manifests
        .groupBy { it.projectId }
        .values
        .flatMap { projectManifests ->
            projectManifests
                .sortedWith(
                    compareByDescending<DriveProjectRevisionManifest> { it.createdAtEpochMs }
                        .thenByDescending { it.revisionId },
                )
                .filterIndexed { index, manifest ->
                    index == 0 || (
                        index < policy.maximumVersionsPerProject &&
                            policy.maxAgeDays?.let { days ->
                                manifest.createdAtEpochMs >= nowEpochMs - java.util.concurrent.TimeUnit.DAYS.toMillis(days.toLong())
                            } != false
                        )
                }
        }
}

class UnifiedDriveGarbageCollector(
    private val store: UnifiedDriveGarbageCollectionStore,
) {
    suspend fun collect(
        policy: BackupRetentionPolicy,
        pendingAssetHashes: Set<String>,
        nowEpochMs: Long,
        gracePeriodMs: Long,
    ): DriveGarbageCollectionReport {
        val retained = DriveManifestRetentionPlanner.retained(store.listCommittedManifests(), policy, nowEpochMs)
        val candidates = DriveGarbageCollectionPlanner.deletions(
            store.listAssets(), retained, pendingAssetHashes, nowEpochMs, gracePeriodMs,
        )
        var deleted = 0
        var failures = 0
        candidates.forEach { candidate ->
            runCatching { store.deleteAsset(candidate.asset) }
                .onSuccess { if (it) deleted++ else failures++ }
                .onFailure { failures++ }
        }
        return DriveGarbageCollectionReport(retained.size, candidates.size, deleted, failures)
    }
}
