package studio.guitarlab.core.project

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue
import studio.guitarlab.core.model.AudioTrack
import studio.guitarlab.core.model.BuiltInRoles
import studio.guitarlab.core.model.ChannelLayout
import studio.guitarlab.core.model.GuitarProject
import studio.guitarlab.core.model.ProjectTemplate

class TrackRoleAssignmentPolicyTest {
    private fun project(vararg roles: String?): GuitarProject = GuitarProject(
        id = "p", name = "P", template = ProjectTemplate.BLANK,
        createdAtEpochMs = 1, updatedAtEpochMs = 1,
        tracks = roles.mapIndexed { index, role -> AudioTrack(id = "t$index", name = "T$index", roleId = role, order = index) },
    )

    @Test fun usedUniqueRoleIsUnavailableToAnotherTrack() {
        val project = project(BuiltInRoles.BACKING, null)
        val available = TrackRoleAssignmentPolicy.availableForTrack(project, "t1").map { it.id }
        assertTrue(BuiltInRoles.BACKING !in available)
    }

    @Test fun currentTrackMayKeepItsExclusiveRole() {
        val project = project(BuiltInRoles.BACKING, null)
        val available = TrackRoleAssignmentPolicy.availableForTrack(project, "t0").map { it.id }
        assertTrue(BuiltInRoles.BACKING in available)
    }

    @Test fun monoReferenceConflictsWithPairButLeftAndRightMayCoexist() {
        val withLeft = project(BuiltInRoles.REFERENCE_GUITAR_L, null)
        val forSecond = TrackRoleAssignmentPolicy.availableForTrack(withLeft, "t1").map { it.id }
        assertTrue(BuiltInRoles.REFERENCE_GUITAR_R in forSecond)
        assertTrue(BuiltInRoles.REFERENCE_GUITAR !in forSecond)

        val withMono = project(BuiltInRoles.REFERENCE_GUITAR, null)
        val afterMono = TrackRoleAssignmentPolicy.availableForTrack(withMono, "t1").map { it.id }
        assertTrue(BuiltInRoles.REFERENCE_GUITAR_L !in afterMono)
        assertTrue(BuiltInRoles.REFERENCE_GUITAR_R !in afterMono)
    }

    @Test fun reusableRolesMayRepeat() {
        val project = project(BuiltInRoles.GUITAR, null)
        val assigned = TrackRoleAssignmentPolicy.assign(project, "t1", BuiltInRoles.GUITAR, 5)
        assertEquals(listOf(BuiltInRoles.GUITAR, BuiltInRoles.GUITAR), assigned.tracks.map { it.roleId })
    }

    @Test fun workflowRoleAppliesItsLayoutAndPanDefaults() {
        val assigned = TrackRoleAssignmentPolicy.assign(project(null), "t0", BuiltInRoles.REFERENCE_GUITAR_R, 7)
        val track = assigned.tracks.single()
        assertEquals(ChannelLayout.MONO, track.channelLayout)
        assertEquals(1f, track.pan)
        assertEquals(7, assigned.updatedAtEpochMs)
    }

    @Test fun removingRoleKeepsMixGeometryButClearsFunction() {
        val withRole = TrackRoleAssignmentPolicy.assign(project(null), "t0", BuiltInRoles.REFERENCE_GUITAR_L, 2)
        val removed = TrackRoleAssignmentPolicy.assign(withRole, "t0", null, 3)
        assertNull(removed.tracks.single().roleId)
        assertEquals(-1f, removed.tracks.single().pan)
    }

    @Test fun duplicateExclusiveAssignmentFailsClosed() {
        val project = project(BuiltInRoles.BACKING, null)
        assertFailsWith<IllegalArgumentException> {
            TrackRoleAssignmentPolicy.assign(project, "t1", BuiltInRoles.BACKING, 4)
        }
    }

    @Test fun newTrackSuggestionsStayFocusedOnCoreGuitarWorkflow() {
        val blank = project(null)
        val suggested = TrackRoleAssignmentPolicy.availableSuggestionsForNewTrack(blank, "t0").map { it.id }
        assertEquals(TrackRoleAssignmentPolicy.suggestedWorkflowRoleIds, suggested)
    }
}
