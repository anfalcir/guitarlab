package studio.guitarlab.core.project

import java.io.File
import kotlinx.coroutines.CancellationException

data class DriveLocalAsset(
    val identity: DriveAssetObject,
    val file: File,
) {
    init {
        require(file.isFile && file.length() == identity.sizeBytes) { "Local asset does not match its declared size." }
    }
}

data class DriveRemoteObjectReceipt(
    val sha256: String,
    val sizeBytes: Long,
) {
    fun matches(asset: DriveAssetObject): Boolean = sha256 == asset.sha256 && sizeBytes == asset.sizeBytes
}

data class DrivePublishedHead(
    val descriptor: DriveCurrentDescriptor,
    val baseRevisionId: String?,
)

interface UnifiedDriveRemoteStore {
    suspend fun findAsset(sha256: String): DriveRemoteObjectReceipt?
    suspend fun uploadAsset(asset: DriveLocalAsset): DriveRemoteObjectReceipt
    suspend fun findManifest(manifestSha256: String): DriveRemoteObjectReceipt?
    suspend fun uploadManifest(manifest: DriveProjectRevisionManifest): DriveRemoteObjectReceipt
    suspend fun listHeads(projectId: String): List<DrivePublishedHead>
    suspend fun publishHead(head: DrivePublishedHead)
}

data class UnifiedDriveBackupResult(
    val descriptor: DriveCurrentDescriptor,
    val uploadedAssets: Int,
    val uploadedBytes: Long,
    val recoveredLostPublishResponse: Boolean,
)

class DriveConflictException(message: String) : IllegalStateException(message)

/**
 * Provider-neutral implementation of the U8 transaction order.
 *
 * Heads are append-only records because Drive v3 does not document a portable atomic
 * compare-and-swap for appProperties. We read the head set before and after publication;
 * divergent descendants are surfaced as a conflict and never resolved by timestamps.
 */
class UnifiedDriveBackupCoordinator(
    private val remote: UnifiedDriveRemoteStore,
) {
    suspend fun commit(
        manifest: DriveProjectRevisionManifest,
        assets: List<DriveLocalAsset>,
        confirmedRevisionId: String?,
        onStage: (DriveCommitStage) -> Unit = {},
    ): UnifiedDriveBackupResult {
        require(assets.map { it.identity }.toSet() == manifest.assets.toSet()) {
            "Frozen snapshot assets do not match the manifest."
        }
        assets.forEach { local ->
            require(BackupHashing.sha256(local.file) == local.identity.sha256) {
                "Local asset failed SHA-256 validation."
            }
        }
        onStage(DriveCommitStage.SNAPSHOT_FROZEN)

        val initialHeads = remote.listHeads(manifest.projectId)
        ensureExpectedBase(initialHeads, confirmedRevisionId, manifest.revisionId)

        var uploadedAssets = 0
        var uploadedBytes = 0L
        for (local in assets.sortedBy { it.identity.sha256 }) {
            val existing = remote.findAsset(local.identity.sha256)
            val receipt = existing ?: remote.uploadAsset(local).also {
                uploadedAssets++
                uploadedBytes += local.identity.sizeBytes
            }
            require(receipt.matches(local.identity)) { "Drive asset verification failed." }
        }
        onStage(DriveCommitStage.ASSETS_VERIFIED)

        val manifestIdentity = DriveAssetObject(manifest.manifestSha256, manifest.canonicalBytes().size.toLong())
        val manifestReceipt = remote.findManifest(manifest.manifestSha256) ?: remote.uploadManifest(manifest)
        require(manifestReceipt.matches(manifestIdentity)) { "Drive manifest verification failed." }
        onStage(DriveCommitStage.MANIFEST_VERIFIED)

        // Close the race window as far as the documented Drive surface permits.
        ensureExpectedBase(remote.listHeads(manifest.projectId), confirmedRevisionId, manifest.revisionId)
        val descriptor = DriveCurrentDescriptor(manifest.projectId, manifest.revisionId, manifest.manifestSha256)
        val head = DrivePublishedHead(descriptor, confirmedRevisionId)
        var recovered = false
        try {
            remote.publishHead(head)
        } catch (error: Throwable) {
            if (error is CancellationException) throw error
            val observed = remote.listHeads(manifest.projectId)
            if (observed.none { it == head }) throw error
            recovered = true
        }
        onStage(DriveCommitStage.HEAD_PUBLISHED)

        val finalHeads = remote.listHeads(manifest.projectId)
        val exact = finalHeads.filter { it.descriptor.revisionId == manifest.revisionId }
        if (exact.size != 1 || exact.single() != head) throw DriveConflictException("Published Drive head could not be verified uniquely.")
        ensureNoDivergentDescendants(finalHeads, confirmedRevisionId, manifest.revisionId)
        onStage(DriveCommitStage.HEAD_VERIFIED)
        return UnifiedDriveBackupResult(descriptor, uploadedAssets, uploadedBytes, recovered)
    }

    private fun ensureExpectedBase(heads: List<DrivePublishedHead>, base: String?, desired: String) {
        if (heads.any { it.descriptor.revisionId == desired }) return
        val current = currentTips(heads)
        when {
            base == null && current.isNotEmpty() -> throw DriveConflictException("Drive already has a project history unknown locally.")
            base != null && current.map { it.descriptor.revisionId }.toSet() != setOf(base) ->
                throw DriveConflictException("Drive head changed since the last confirmed revision.")
        }
    }

    private fun ensureNoDivergentDescendants(heads: List<DrivePublishedHead>, base: String?, desired: String) {
        val siblings = heads.filter { it.baseRevisionId == base }.map { it.descriptor.revisionId }.toSet()
        if (siblings.any { it != desired }) throw DriveConflictException("Concurrent Drive revisions require explicit resolution.")
    }

    private fun currentTips(heads: List<DrivePublishedHead>): List<DrivePublishedHead> {
        val referencedBases = heads.mapNotNullTo(mutableSetOf()) { it.baseRevisionId }
        return heads.filter { it.descriptor.revisionId !in referencedBases }
    }
}
