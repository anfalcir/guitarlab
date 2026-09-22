package studio.guitarlab.core.project

import java.io.File
import java.nio.file.Files

data class DriveRestoreAssetTarget(
    val identity: DriveAssetObject,
    val relativePath: String,
) {
    init {
        require(relativePath.isNotBlank())
        val portable = relativePath.replace('\\', '/')
        require(
            !File(relativePath).isAbsolute &&
                !portable.startsWith('/') &&
                !Regex("^[A-Za-z]:").containsMatchIn(portable) &&
                portable.split('/').none { it.isEmpty() || it == "." || it == ".." },
        ) {
            "Restore target must remain inside staging."
        }
    }
}

data class DriveRestorePlan(
    val descriptor: DriveCurrentDescriptor,
    val manifest: DriveProjectRevisionManifest,
    val targets: List<DriveRestoreAssetTarget>,
) {
    init {
        require(descriptor.projectId == manifest.projectId)
        require(descriptor.revisionId == manifest.revisionId)
        require(descriptor.manifestSha256 == manifest.manifestSha256)
        require(targets.map { it.identity }.toSet() == manifest.assets.toSet())
        require(targets.distinctBy { it.relativePath.replace('\\', '/') }.size == targets.size)
    }
}

interface UnifiedDriveRestoreSource {
    suspend fun loadManifest(descriptor: DriveCurrentDescriptor): DriveProjectRevisionManifest
    suspend fun downloadAsset(asset: DriveAssetObject, destination: File)
}

fun interface DriveRestoreLayout {
    fun targets(manifest: DriveProjectRevisionManifest): List<DriveRestoreAssetTarget>
}

fun interface DriveRestoreValidator {
    fun validate(stagingDirectory: File, plan: DriveRestorePlan)
}

/** Publishes an already validated staging tree as one atomic local transaction. */
fun interface DriveRestorePublisher {
    fun publish(stagingDirectory: File, plan: DriveRestorePlan)
}

data class UnifiedDriveRestoreResult(
    val descriptor: DriveCurrentDescriptor,
    val downloadedAssets: Int,
    val downloadedBytes: Long,
)

class UnifiedDriveRestoreCoordinator(
    private val source: UnifiedDriveRestoreSource,
    private val layout: DriveRestoreLayout,
    private val validator: DriveRestoreValidator,
    private val publisher: DriveRestorePublisher,
    private val stagingRoot: File,
) {
    suspend fun restore(descriptor: DriveCurrentDescriptor): UnifiedDriveRestoreResult {
        val manifest = source.loadManifest(descriptor)
        val plan = DriveRestorePlan(descriptor, manifest, layout.targets(manifest))
        stagingRoot.mkdirs()
        val staging = Files.createTempDirectory(stagingRoot.toPath(), "drive-restore-").toFile()
        try {
            var downloadedBytes = 0L
            for (target in plan.targets.sortedBy { it.relativePath }) {
                val destination = staging.resolve(target.relativePath).canonicalFile
                require(destination.path.startsWith(staging.canonicalPath + File.separator))
                destination.parentFile?.mkdirs()
                source.downloadAsset(target.identity, destination)
                require(destination.isFile && destination.length() == target.identity.sizeBytes) {
                    "Restored asset size mismatch."
                }
                require(BackupHashing.sha256(destination) == target.identity.sha256) {
                    "Restored asset checksum mismatch."
                }
                downloadedBytes += destination.length()
            }
            validator.validate(staging, plan)
            publisher.publish(staging, plan)
            return UnifiedDriveRestoreResult(descriptor, plan.targets.size, downloadedBytes)
        } finally {
            staging.deleteRecursively()
        }
    }
}

enum class DriveConflictAction { KEEP_LOCAL, USE_DRIVE, IMPORT_AS_COPY }

object DriveConflictActionPolicy {
    fun allowed(reconciliation: DriveReconciliation): Set<DriveConflictAction> = when (reconciliation) {
        DriveReconciliation.CONFLICT -> DriveConflictAction.entries.toSet()
        DriveReconciliation.DOWNLOAD_REMOTE, DriveReconciliation.REMOTE_ONLY ->
            setOf(DriveConflictAction.USE_DRIVE, DriveConflictAction.IMPORT_AS_COPY)
        DriveReconciliation.UPLOAD_LOCAL, DriveReconciliation.LOCAL_ONLY -> setOf(DriveConflictAction.KEEP_LOCAL)
        DriveReconciliation.NO_OP -> emptySet()
    }
}
