package studio.guitarlab.app.ui

import kotlin.test.assertEquals
import org.junit.Test
import studio.guitarlab.core.model.GuitarProject
import studio.guitarlab.core.model.PreparationState
import studio.guitarlab.core.model.PreparationStatus
import studio.guitarlab.core.model.ProjectTemplate

class UnifiedProjectShellPolicyTest {
    private fun project(
        status: PreparationStatus? = null,
        backingId: String? = null,
        guitarId: String? = null,
    ) = GuitarProject(
        id = "legacy-project",
        name = "Legacy",
        template = ProjectTemplate.GUITAR,
        createdAtEpochMs = 1,
        updatedAtEpochMs = 2,
        preparation = status?.let {
            PreparationState(
                status = it,
                activeBackingAssetId = backingId,
                activeGuitarAssetId = guitarId,
            )
        },
    )

    @Test fun initialWorkspaceFollowsRelevantProjectState() {
        assertEquals(AppScreen.Studio("legacy-project"), UnifiedProjectShellPolicy.initialScreen(project()))
        assertEquals(AppScreen.Studio("legacy-project"), UnifiedProjectShellPolicy.initialScreen(project(PreparationStatus.NOT_STARTED)))
        assertEquals(AppScreen.Prepare("legacy-project"), UnifiedProjectShellPolicy.initialScreen(project(PreparationStatus.SOURCE_READY)))
        assertEquals(AppScreen.Prepare("legacy-project"), UnifiedProjectShellPolicy.initialScreen(project(PreparationStatus.SEPARATING)))
        assertEquals(AppScreen.Prepare("legacy-project"), UnifiedProjectShellPolicy.initialScreen(project(PreparationStatus.ERROR)))
        assertEquals(AppScreen.Prepare("legacy-project"), UnifiedProjectShellPolicy.initialScreen(project(PreparationStatus.READY)))
        assertEquals(
            AppScreen.Studio("legacy-project"),
            UnifiedProjectShellPolicy.initialScreen(project(PreparationStatus.READY, backingId = "backing", guitarId = "guitar")),
        )
    }

    @Test fun cardStatusReflectsPreparationWithoutMutatingProject() {
        val expected = mapOf(
            null to UnifiedProjectCardStatus.STUDIO_READY,
            PreparationStatus.NOT_STARTED to UnifiedProjectCardStatus.STUDIO_READY,
            PreparationStatus.SOURCE_READY to UnifiedProjectCardStatus.SOURCE_READY,
            PreparationStatus.SEPARATING to UnifiedProjectCardStatus.SEPARATING,
            PreparationStatus.READY to UnifiedProjectCardStatus.PREPARED,
            PreparationStatus.ERROR to UnifiedProjectCardStatus.PREPARATION_ERROR,
        )
        expected.forEach { (status, badge) -> assertEquals(badge, UnifiedProjectShellPolicy.cardStatus(project(status))) }
    }
}
