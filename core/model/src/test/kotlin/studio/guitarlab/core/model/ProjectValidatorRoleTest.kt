package studio.guitarlab.core.model

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ProjectValidatorRoleTest {
    private fun project(vararg roles: String?): GuitarProject = GuitarProject(
        id = "p",
        name = "P",
        template = ProjectTemplate.BLANK,
        createdAtEpochMs = 1,
        updatedAtEpochMs = 1,
        tracks = roles.mapIndexed { index, role -> AudioTrack(id = "t$index", name = "T$index", roleId = role, order = index) },
    )

    @Test fun duplicateUniqueWorkflowRoleIsRejected() {
        val issues = ProjectValidator.validate(project(BuiltInRoles.BACKING, BuiltInRoles.BACKING))
        assertTrue(issues.any { it.code == "track.role.conflict" })
    }

    @Test fun referenceLeftAndRightAreCompatible() {
        val issues = ProjectValidator.validate(project(BuiltInRoles.REFERENCE_GUITAR_L, BuiltInRoles.REFERENCE_GUITAR_R))
        assertFalse(issues.any { it.code == "track.role.conflict" })
    }

    @Test fun monoReferenceAndReferenceSideConflict() {
        val issues = ProjectValidator.validate(project(BuiltInRoles.REFERENCE_GUITAR, BuiltInRoles.REFERENCE_GUITAR_L))
        assertTrue(issues.any { it.code == "track.role.conflict" })
    }

    @Test fun reusableInstrumentRolesMayRepeat() {
        val issues = ProjectValidator.validate(project(BuiltInRoles.GUITAR, BuiltInRoles.GUITAR))
        assertFalse(issues.any { it.code == "track.role.conflict" })
    }
}
