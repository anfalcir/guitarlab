package studio.guitarlab.core.project

import studio.guitarlab.core.model.GuitarProject
import studio.guitarlab.core.model.PreparationState
import studio.guitarlab.core.model.PreparationStatus

/**
 * C3 lifecycle rules shared by repository/UI integration.
 * Runtime job ownership never travels with a duplicate; durable creative/media state may.
 */
object ProjectLifecyclePolicy {
    fun duplicateSnapshot(source: GuitarProject, newProjectId: String, newName: String, nowEpochMs: Long): GuitarProject =
        source.copy(
            id = newProjectId,
            name = newName.trim(),
            createdAtEpochMs = nowEpochMs,
            updatedAtEpochMs = nowEpochMs,
            preparation = source.preparation?.stableCopy(),
        )

    fun withPublishedSource(project: GuitarProject, sourceAssetId: String, nowEpochMs: Long): GuitarProject =
        project.copy(
            updatedAtEpochMs = nowEpochMs,
            preparation = (project.preparation ?: PreparationState()).copy(
                status = PreparationStatus.SOURCE_READY,
                sourceAssetId = sourceAssetId,
                activeStemAssetIds = emptyMap(),
                activeBackingAssetId = null,
                activeGuitarAssetId = null,
                availableReferenceAssetIds = emptyList(),
            ),
        )

    private fun PreparationState.stableCopy(): PreparationState = copy(
        status = when {
            sourceAssetId == null -> PreparationStatus.NOT_STARTED
            activeBackingAssetId != null && activeGuitarAssetId != null -> PreparationStatus.READY
            activeStemAssetIds.size == 6 -> PreparationStatus.READY
            else -> PreparationStatus.SOURCE_READY
        },
    )
}
