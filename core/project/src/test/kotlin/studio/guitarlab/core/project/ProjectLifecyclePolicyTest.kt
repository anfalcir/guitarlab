package studio.guitarlab.core.project

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import studio.guitarlab.core.model.GuitarProject
import studio.guitarlab.core.model.PreparationState
import studio.guitarlab.core.model.PreparationStatus
import studio.guitarlab.core.model.ProjectTemplate

class ProjectLifecyclePolicyTest {
    @Test fun duplicateKeepsPreparedReferenceV2ProjectReadyWithoutLegacyStems() {
        val source = GuitarProject(
            id = "source",
            name = "Source",
            template = ProjectTemplate.GUITAR,
            createdAtEpochMs = 1,
            updatedAtEpochMs = 2,
            preparation = PreparationState(
                status = PreparationStatus.READY,
                sourceAssetId = "source-asset",
                activeStemAssetIds = emptyMap(),
                activeBackingAssetId = "backing",
                activeGuitarAssetId = "guitar",
                availableReferenceAssetIds = listOf("backing", "guitar"),
            ),
        )
        val duplicate = ProjectLifecyclePolicy.duplicateSnapshot(source, "copy", "Copy", 10)
        assertEquals(PreparationStatus.READY, duplicate.preparation?.status)
        assertTrue(duplicate.preparation?.activeStemAssetIds.orEmpty().isEmpty())
        assertEquals("backing", duplicate.preparation?.activeBackingAssetId)
        assertEquals("guitar", duplicate.preparation?.activeGuitarAssetId)
    }
}
