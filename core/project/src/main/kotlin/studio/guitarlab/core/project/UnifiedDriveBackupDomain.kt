package studio.guitarlab.core.project

import java.security.MessageDigest

private val SHA256_PATTERN = Regex("[0-9a-f]{64}")

data class DriveAssetObject(
    val sha256: String,
    val sizeBytes: Long,
) {
    init {
        require(SHA256_PATTERN.matches(sha256)) { "Asset SHA-256 must be canonical lowercase hex." }
        require(sizeBytes > 0L) { "Asset size must be positive." }
    }

    val objectKey: String get() = "assets/$sha256"
}

data class DriveProjectRevisionManifest(
    val projectId: String,
    val revisionId: String,
    val baseRevisionId: String?,
    val createdAtEpochMs: Long,
    val canonicalProjectStateSha256: String,
    val assets: List<DriveAssetObject>,
    val schemaVersion: Int = SCHEMA_VERSION,
) {
    init {
        require(projectId.isNotBlank())
        require(revisionId.isNotBlank())
        require(baseRevisionId != revisionId)
        require(createdAtEpochMs >= 0L)
        require(SHA256_PATTERN.matches(canonicalProjectStateSha256))
        require(schemaVersion == SCHEMA_VERSION)
        require(assets.distinctBy { it.sha256 }.size == assets.size) { "Manifest contains duplicate asset identities." }
    }

    fun canonicalBytes(): ByteArray = buildString {
        append("guitarlab-drive-manifest-v3\n")
        append("schema=").append(schemaVersion).append('\n')
        append("project=").append(projectId).append('\n')
        append("revision=").append(revisionId).append('\n')
        append("base=").append(baseRevisionId.orEmpty()).append('\n')
        append("created=").append(createdAtEpochMs).append('\n')
        append("state=").append(canonicalProjectStateSha256).append('\n')
        assets.sortedWith(compareBy<DriveAssetObject> { it.sha256 }.thenBy { it.sizeBytes }).forEach {
            append("asset=").append(it.sha256).append(':').append(it.sizeBytes).append('\n')
        }
    }.toByteArray(Charsets.UTF_8)

    val manifestSha256: String get() = canonicalBytes().sha256()

    companion object { const val SCHEMA_VERSION = 3 }
}

data class DriveCurrentDescriptor(
    val projectId: String,
    val revisionId: String,
    val manifestSha256: String,
) {
    init {
        require(projectId.isNotBlank() && revisionId.isNotBlank())
        require(SHA256_PATTERN.matches(manifestSha256))
    }
}

enum class DriveReconciliation { NO_OP, UPLOAD_LOCAL, DOWNLOAD_REMOTE, LOCAL_ONLY, REMOTE_ONLY, CONFLICT }

object DriveConflictResolver {
    fun resolve(
        localRevisionId: String?,
        confirmedRevisionId: String?,
        remoteRevisionId: String?,
    ): DriveReconciliation {
        if (localRevisionId == null) return if (remoteRevisionId == null) DriveReconciliation.NO_OP else DriveReconciliation.REMOTE_ONLY
        if (remoteRevisionId == null) return if (confirmedRevisionId == null) DriveReconciliation.LOCAL_ONLY else DriveReconciliation.UPLOAD_LOCAL
        if (localRevisionId == remoteRevisionId) return DriveReconciliation.NO_OP
        if (remoteRevisionId == confirmedRevisionId) return DriveReconciliation.UPLOAD_LOCAL
        if (localRevisionId == confirmedRevisionId) return DriveReconciliation.DOWNLOAD_REMOTE
        return DriveReconciliation.CONFLICT
    }
}

enum class DriveCommitStage {
    SNAPSHOT_FROZEN, ASSETS_VERIFIED, MANIFEST_VERIFIED, HEAD_PUBLISHED, HEAD_VERIFIED,
}

data class DriveCommitProgress(
    val desiredRevisionId: String,
    val confirmedRevisionId: String?,
    val stage: DriveCommitStage,
) {
    val isServerConfirmed: Boolean
        get() = stage == DriveCommitStage.HEAD_VERIFIED && confirmedRevisionId == desiredRevisionId
}

data class DriveGcCandidate(
    val asset: DriveAssetObject,
    val uploadedAtEpochMs: Long,
)

object DriveGarbageCollectionPlanner {
    fun deletions(
        candidates: List<DriveGcCandidate>,
        retainedManifests: List<DriveProjectRevisionManifest>,
        pendingAssetHashes: Set<String>,
        nowEpochMs: Long,
        gracePeriodMs: Long,
    ): List<DriveGcCandidate> {
        require(gracePeriodMs >= 0L)
        val reachable = retainedManifests.flatMapTo(mutableSetOf()) { manifest -> manifest.assets.map { it.sha256 } }
        return candidates.filter { candidate ->
            candidate.asset.sha256 !in reachable &&
                candidate.asset.sha256 !in pendingAssetHashes &&
                candidate.uploadedAtEpochMs <= nowEpochMs - gracePeriodMs
        }
    }
}

object DriveUploadPlanner {
    fun missingAssets(manifest: DriveProjectRevisionManifest, remoteHashes: Set<String>): List<DriveAssetObject> =
        manifest.assets.filterNot { it.sha256 in remoteHashes }.sortedBy { it.sha256 }
}

private fun ByteArray.sha256(): String = MessageDigest.getInstance("SHA-256")
    .digest(this)
    .joinToString("") { "%02x".format(it) }
