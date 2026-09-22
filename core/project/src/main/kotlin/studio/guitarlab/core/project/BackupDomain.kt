package studio.guitarlab.core.project

import java.io.File
import java.security.MessageDigest
import java.util.concurrent.TimeUnit
import studio.guitarlab.core.model.GuitarProject

data class BackupRetentionPolicy(
    val maxAgeDays: Int? = 90,
    val maximumVersionsPerProject: Int = 3,
) {
    init {
        require(maxAgeDays == null || maxAgeDays > 0) { "Retention days must be positive or null." }
        require(maximumVersionsPerProject >= 1) { "At least one backup version must be retained." }
    }
}

/**
 * Stable backup identities are deliberately independent from the project display name.
 *
 * - projectId is the immutable project identity already persisted by GuitarProject.
 * - revisionId is derived from projectId + the persisted edit timestamp + canonical project state.
 *   The timestamp is kept verbatim (epoch milliseconds), while the state digest prevents two edits
 *   made in the same millisecond from collapsing into one revision.
 * - sha256 remains the package/content-integrity identity. Together, revisionId + sha256 provide a
 *   strong deduplication key without treating a rename as a different project.
 */
object BackupRevisionIdentity {
    private const val PREFIX = "r_"
    private val REVISION_PATTERN = Regex("r_[0-9]+_[0-9a-f]{24}")

    fun forProject(project: GuitarProject): String {
        val canonicalState = ProjectCodec().encode(project)
        return derive(project.id, project.updatedAtEpochMs, canonicalState)
    }

    /** Compatibility identity for H26/H27 metadata that did not persist a revisionId. */
    fun legacy(projectId: String, projectUpdatedAtEpochMs: Long): String =
        derive(projectId, projectUpdatedAtEpochMs, "legacy")

    private fun derive(projectId: String, projectUpdatedAtEpochMs: Long, state: String): String {
        require(projectId.isNotBlank()) { "Project id must not be blank." }
        require(projectUpdatedAtEpochMs >= 0L) { "Project revision time must not be negative." }
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(
                "guitarlab-project-revision-v2\u0000$projectId\u0000$projectUpdatedAtEpochMs\u0000$state"
                    .toByteArray(Charsets.UTF_8),
            )
            .joinToString("") { "%02x".format(it) }
            .take(24)
        return "$PREFIX${projectUpdatedAtEpochMs}_$digest"
    }

    fun isValid(value: String): Boolean = REVISION_PATTERN.matches(value)
}

data class BackupVersionDescriptor(
    val remoteId: String,
    val projectId: String,
    val projectName: String,
    val projectUpdatedAtEpochMs: Long,
    val backupCreatedAtEpochMs: Long,
    val sizeBytes: Long,
    val sha256: String,
    val revisionId: String = BackupRevisionIdentity.legacy(projectId, projectUpdatedAtEpochMs),
    val formatVersion: Int = 1,
) {
    val deduplicationKey: String get() = "$revisionId:${sha256.lowercase()}"
}

data class BackupCommitRequest(
    val projectId: String,
    val projectName: String,
    val projectUpdatedAtEpochMs: Long,
    val backupCreatedAtEpochMs: Long,
    val sizeBytes: Long,
    val sha256: String,
    val packageFile: File,
    val revisionId: String = BackupRevisionIdentity.legacy(projectId, projectUpdatedAtEpochMs),
)

interface ProjectBackupRemoteStore {
    suspend fun listCommittedVersions(projectId: String? = null): List<BackupVersionDescriptor>

    /**
     * Targeted lookup used to tolerate eventually-consistent document providers. Implementations
     * may retry briefly before returning null. The default remains provider-neutral.
     */
    suspend fun findCommittedVersion(projectId: String, revisionId: String): BackupVersionDescriptor? =
        listCommittedVersions(projectId).firstOrNull { it.revisionId == revisionId }

    suspend fun commit(request: BackupCommitRequest): BackupVersionDescriptor
    suspend fun copyPackage(version: BackupVersionDescriptor, destination: File)
    suspend fun deleteVersion(version: BackupVersionDescriptor): Boolean
    suspend fun cleanupIncomplete(olderThanEpochMs: Long): Int
}

object BackupIncrementalPolicy {
    fun needsBackup(project: GuitarProject, committed: List<BackupVersionDescriptor>): Boolean {
        val revisionId = BackupRevisionIdentity.forProject(project)
        return committed.none { version ->
            version.projectId == project.id &&
                (version.revisionId == revisionId ||
                    (version.formatVersion < 2 && version.projectUpdatedAtEpochMs == project.updatedAtEpochMs))
        }
    }
}

object BackupRetentionPlanner {
    /**
     * Keeps a bounded history per project. The newest committed version is always preserved,
     * duplicate copies of the same persisted project revision/content are collapsed to the newest
     * copy, and older unique revisions are removed when they exceed either count or age limits.
     */
    fun deletions(
        committed: List<BackupVersionDescriptor>,
        policy: BackupRetentionPolicy,
        nowEpochMs: Long,
    ): List<BackupVersionDescriptor> {
        val cutoff = policy.maxAgeDays?.let { days -> nowEpochMs - TimeUnit.DAYS.toMillis(days.toLong()) }
        return committed
            .groupBy { it.projectId }
            .values
            .flatMap { versions ->
                val ordered = versions.sortedWith(
                    compareByDescending<BackupVersionDescriptor> { it.backupCreatedAtEpochMs }
                        .thenByDescending { it.projectUpdatedAtEpochMs }
                        .thenByDescending { it.remoteId },
                )
                val seenRevisions = mutableSetOf<String>()
                var retainedUniqueVersions = 0
                buildList {
                    ordered.forEach { version ->
                        val duplicateRevision = !seenRevisions.add(version.deduplicationKey)
                        if (duplicateRevision) {
                            add(version)
                            return@forEach
                        }

                        // Never automatically remove the newest valid version of a project.
                        if (retainedUniqueVersions == 0) {
                            retainedUniqueVersions++
                            return@forEach
                        }

                        val beyondVersionLimit = retainedUniqueVersions >= policy.maximumVersionsPerProject
                        val expiredByAge = cutoff != null && version.backupCreatedAtEpochMs < cutoff
                        if (beyondVersionLimit || expiredByAge) {
                            add(version)
                        } else {
                            retainedUniqueVersions++
                        }
                    }
                }
            }
            .sortedBy { it.backupCreatedAtEpochMs }
    }
}

object BackupHashing {
    fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().buffered().use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                if (read > 0) digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}

data class ProjectBackupAttempt(
    val projectId: String,
    val projectName: String,
    val status: Status,
    val version: BackupVersionDescriptor? = null,
    val error: String? = null,
) {
    enum class Status { COMMITTED, SKIPPED_UP_TO_DATE, FAILED }
}

data class BackupRunReport(
    val attempts: List<ProjectBackupAttempt>,
    val deletedVersions: Int,
    val retentionDeleteFailures: Int,
    val cleanedIncompleteVersions: Int,
    val retentionSuppressed: Boolean,
) {
    val committedCount: Int get() = attempts.count { it.status == ProjectBackupAttempt.Status.COMMITTED }
    val skippedCount: Int get() = attempts.count { it.status == ProjectBackupAttempt.Status.SKIPPED_UP_TO_DATE }
    val failedCount: Int get() = attempts.count { it.status == ProjectBackupAttempt.Status.FAILED }
}

data class RestoreAttempt(
    val version: BackupVersionDescriptor,
    val restoredProject: GuitarProject? = null,
    val error: String? = null,
) {
    val succeeded: Boolean get() = restoredProject != null && error == null
}

object BackupCommitContract {
    const val METADATA_FORMAT = "guitarlab-cloud-backup"
    const val COMMIT_FORMAT = "guitarlab-cloud-backup-commit"
    const val FORMAT_VERSION = 2
    private const val LEGACY_FORMAT_VERSION = 1

    fun metadata(request: BackupCommitRequest): java.util.Properties = java.util.Properties().apply {
        setProperty("format", METADATA_FORMAT)
        setProperty("version", FORMAT_VERSION.toString())
        setProperty("projectId", request.projectId)
        setProperty("projectName", request.projectName)
        setProperty("projectUpdatedAtEpochMs", request.projectUpdatedAtEpochMs.toString())
        setProperty("revisionId", request.revisionId)
        setProperty("backupCreatedAtEpochMs", request.backupCreatedAtEpochMs.toString())
        setProperty("sizeBytes", request.sizeBytes.toString())
        setProperty("sha256", request.sha256.lowercase())
    }

    fun commitMarker(request: BackupCommitRequest): java.util.Properties = java.util.Properties().apply {
        setProperty("format", COMMIT_FORMAT)
        setProperty("version", FORMAT_VERSION.toString())
        setProperty("projectId", request.projectId)
        setProperty("projectUpdatedAtEpochMs", request.projectUpdatedAtEpochMs.toString())
        setProperty("revisionId", request.revisionId)
        setProperty("backupCreatedAtEpochMs", request.backupCreatedAtEpochMs.toString())
        setProperty("sizeBytes", request.sizeBytes.toString())
        setProperty("sha256", request.sha256.lowercase())
    }

    fun parseCommittedOrNull(
        remoteId: String,
        metadata: java.util.Properties,
        commit: java.util.Properties,
        observedPackageSize: Long? = null,
        expectedProjectId: String? = null,
    ): BackupVersionDescriptor? = runCatching {
        require(metadata.getProperty("format") == METADATA_FORMAT)
        val metadataVersion = metadata.getProperty("version")?.toIntOrNull() ?: error("version ausente")
        val commitVersion = commit.getProperty("version")?.toIntOrNull() ?: error("commit version ausente")
        require(metadataVersion in LEGACY_FORMAT_VERSION..FORMAT_VERSION)
        require(commit.getProperty("format") == COMMIT_FORMAT)
        require(commitVersion == metadataVersion)
        val projectId = metadata.getProperty("projectId")?.takeIf { it.isNotBlank() } ?: error("projectId ausente")
        if (expectedProjectId != null) require(projectId == expectedProjectId)
        val projectName = metadata.getProperty("projectName").orEmpty().ifBlank { "Projeto" }
        val updated = metadata.getProperty("projectUpdatedAtEpochMs")?.toLongOrNull() ?: error("updated ausente")
        val created = metadata.getProperty("backupCreatedAtEpochMs")?.toLongOrNull() ?: error("created ausente")
        val size = metadata.getProperty("sizeBytes")?.toLongOrNull()?.takeIf { it > 0L } ?: error("size ausente")
        val hash = metadata.getProperty("sha256")?.lowercase()?.takeIf { it.matches(Regex("[0-9a-f]{64}")) } ?: error("hash ausente")
        val revisionId = if (metadataVersion >= 2) {
            metadata.getProperty("revisionId")?.takeIf(BackupRevisionIdentity::isValid) ?: error("revisionId ausente")
        } else BackupRevisionIdentity.legacy(projectId, updated)
        require(commit.getProperty("projectId") == projectId)
        require(commit.getProperty("projectUpdatedAtEpochMs")?.toLongOrNull() == updated)
        if (metadataVersion >= 2) require(commit.getProperty("revisionId") == revisionId)
        require(commit.getProperty("backupCreatedAtEpochMs")?.toLongOrNull() == created)
        require(commit.getProperty("sizeBytes")?.toLongOrNull() == size)
        require(commit.getProperty("sha256")?.equals(hash, ignoreCase = true) == true)
        if (observedPackageSize != null && observedPackageSize >= 0L) require(observedPackageSize == size)
        BackupVersionDescriptor(remoteId, projectId, projectName, updated, created, size, hash, revisionId, metadataVersion)
    }.getOrNull()
}
