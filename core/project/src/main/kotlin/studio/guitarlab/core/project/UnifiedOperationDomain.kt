package studio.guitarlab.core.project

enum class UnifiedOperationKind { SOURCE_ACQUISITION, SEPARATION, REFERENCE_PREPARATION, EXPORT, BACKUP, RESTORE }

enum class UnifiedOperationState {
    QUEUED, RUNNING, RETRYING, SUCCEEDED, FAILED, CANCELLED;

    val isActive: Boolean get() = this == QUEUED || this == RUNNING || this == RETRYING
}

data class UnifiedOperationRecord(
    val operationId: String,
    val projectId: String?,
    val kind: UnifiedOperationKind,
    val state: UnifiedOperationState,
    val progressPercent: Int?,
    val updatedAtEpochMs: Long,
    val summary: String,
    val technicalDetail: String? = null,
) {
    init {
        require(operationId.isNotBlank())
        require(projectId == null || projectId.isNotBlank())
        require(progressPercent == null || progressPercent in 0..100)
        require(updatedAtEpochMs >= 0)
        require(summary.isNotBlank())
    }
}

object UnifiedActivityPolicy {
    fun ordered(records: Collection<UnifiedOperationRecord>): List<UnifiedOperationRecord> = records
        .groupBy { it.operationId }
        .values
        .map { versions -> versions.maxBy { it.updatedAtEpochMs } }
        .sortedWith(
            compareByDescending<UnifiedOperationRecord> { it.state.isActive }
                .thenByDescending { it.updatedAtEpochMs }
                .thenBy { it.operationId },
        )

    fun compactActive(records: Collection<UnifiedOperationRecord>): UnifiedOperationRecord? =
        ordered(records).firstOrNull { it.state.isActive }

    fun isClearable(record: UnifiedOperationRecord): Boolean =
        record.state == UnifiedOperationState.SUCCEEDED || record.state == UnifiedOperationState.CANCELLED

    fun clearResolved(records: Collection<UnifiedOperationRecord>): List<UnifiedOperationRecord> =
        records.filterNot(::isClearable)

    fun terminalizeMissingProjects(
        records: Collection<UnifiedOperationRecord>,
        existingProjectIds: Set<String>,
        nowEpochMs: Long,
    ): List<UnifiedOperationRecord> = records.map { record ->
        if (record.projectId != null && record.projectId !in existingProjectIds && record.state.isActive) {
            record.copy(
                state = UnifiedOperationState.CANCELLED,
                progressPercent = null,
                updatedAtEpochMs = nowEpochMs,
                summary = "Operação encerrada porque o projeto não existe mais",
                technicalDetail = "PROJECT_REMOVED",
            )
        } else {
            record
        }
    }

    fun terminalizeProject(
        records: Collection<UnifiedOperationRecord>,
        projectId: String,
        nowEpochMs: Long,
    ): List<UnifiedOperationRecord> = records.map { record ->
        if (record.projectId == projectId && record.state.isActive) {
            record.copy(
                state = UnifiedOperationState.CANCELLED,
                progressPercent = null,
                updatedAtEpochMs = nowEpochMs,
                summary = "Operação encerrada porque o projeto foi excluído",
                technicalDetail = "PROJECT_DELETED",
            )
        } else {
            record
        }
    }
}

enum class ProjectSyncState { NOT_CONNECTED, LOCAL_ONLY, PENDING, SYNCING, SYNCED, ERROR, CONFLICT }

object ProjectSyncStatePolicy {
    fun derive(
        driveConnected: Boolean,
        localRevisionId: String,
        confirmedRevisionId: String?,
        reconciliation: DriveReconciliation? = null,
        backupOperation: UnifiedOperationRecord? = null,
    ): ProjectSyncState {
        require(localRevisionId.isNotBlank())
        if (!driveConnected) return ProjectSyncState.NOT_CONNECTED
        if (reconciliation == DriveReconciliation.CONFLICT) return ProjectSyncState.CONFLICT
        return when (backupOperation?.state) {
            UnifiedOperationState.QUEUED -> ProjectSyncState.PENDING
            UnifiedOperationState.RUNNING, UnifiedOperationState.RETRYING -> ProjectSyncState.SYNCING
            UnifiedOperationState.FAILED -> ProjectSyncState.ERROR
            UnifiedOperationState.CANCELLED, null ->
                if (confirmedRevisionId == null) ProjectSyncState.LOCAL_ONLY
                else if (confirmedRevisionId == localRevisionId) ProjectSyncState.SYNCED
                else ProjectSyncState.PENDING
            UnifiedOperationState.SUCCEEDED ->
                if (confirmedRevisionId == localRevisionId) ProjectSyncState.SYNCED else ProjectSyncState.PENDING
        }
    }
}
