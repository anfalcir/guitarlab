package studio.guitarlab.core.project

import studio.guitarlab.core.model.BuiltInRoles
import studio.guitarlab.core.model.GuitarProject
import studio.guitarlab.core.model.RoleResolver
import studio.guitarlab.core.model.TrackRoleDefinition

/**
 * Central authority for user-facing track-function assignment.
 *
 * Workflow functions are conflict-aware: e.g. a mono reference role cannot coexist with the
 * reference L/R pair, and a unique role cannot be assigned to two tracks. Reusable instrument
 * roles (Guitar, Bass, Vocals, Generic, custom roles) remain intentionally repeatable.
 */
object TrackRoleAssignmentPolicy {
    /** Compact set worth suggesting immediately after a new track is created. */
    val suggestedWorkflowRoleIds: List<String> = listOf(
        BuiltInRoles.BACKING,
        BuiltInRoles.REFERENCE_GUITAR_L,
        BuiltInRoles.REFERENCE_GUITAR_R,
        BuiltInRoles.RECORDED_GUITAR_L,
        BuiltInRoles.RECORDED_GUITAR_R,
    )

    fun definitions(project: GuitarProject): List<TrackRoleDefinition> =
        (BuiltInRoles.definitions + project.customRoles).distinctBy { it.id }

    fun definition(project: GuitarProject, roleId: String?): TrackRoleDefinition? =
        roleId?.let { id -> definitions(project).firstOrNull { it.id == id } }

    fun availableForTrack(project: GuitarProject, trackId: String): List<TrackRoleDefinition> {
        require(project.tracks.any { it.id == trackId }) { "Track '$trackId' not found." }
        val otherRoleIds = project.tracks.asSequence()
            .filterNot { it.id == trackId }
            .mapNotNull { it.roleId }
            .toList()
        return definitions(project).filter { candidate ->
            otherRoleIds.none { assigned -> BuiltInRoles.rolesConflict(candidate.id, assigned) }
        }
    }

    fun availableSuggestionsForNewTrack(project: GuitarProject, trackId: String): List<TrackRoleDefinition> {
        val available = availableForTrack(project, trackId).associateBy { it.id }
        return suggestedWorkflowRoleIds.mapNotNull(available::get)
    }

    fun assign(project: GuitarProject, trackId: String, roleId: String?, nowEpochMs: Long): GuitarProject {
        val track = project.tracks.firstOrNull { it.id == trackId }
            ?: throw IllegalArgumentException("Track '$trackId' not found.")
        val definition = definition(project, roleId)
        require(roleId == null || definition != null) { "Unknown track role '$roleId'." }

        if (roleId != null) {
            val conflict = project.tracks.firstOrNull { other ->
                other.id != trackId && other.roleId?.let { BuiltInRoles.rolesConflict(roleId, it) } == true
            }
            require(conflict == null) {
                "A função '${definition!!.name}' já está representada por outra pista do projeto."
            }
        }

        val workflowRole = roleId != null && BuiltInRoles.isWorkflowRole(roleId)
        val updatedTrack = RoleResolver.applyUserRole(
            track = track,
            roleId = roleId,
            channelLayout = if (workflowRole) definition?.defaultChannelLayout else null,
            pan = if (workflowRole) definition?.defaultPan else null,
        )
        return project.copy(
            tracks = project.tracks.map { if (it.id == trackId) updatedTrack else it },
            updatedAtEpochMs = nowEpochMs,
        )
    }
}
