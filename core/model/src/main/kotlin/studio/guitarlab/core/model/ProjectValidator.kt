package studio.guitarlab.core.model

data class ValidationIssue(val code: String, val message: String)

object ProjectValidator {
    fun validate(project: GuitarProject): List<ValidationIssue> {
        val issues = mutableListOf<ValidationIssue>()
        if (project.schemaVersion <= 0) issues += ValidationIssue("schema.invalid", "Schema version must be positive.")
        if (project.name.isBlank()) issues += ValidationIssue("project.name.blank", "Project name must not be blank.")
        if (project.groups.map { it.id }.distinct().size != project.groups.size) issues += ValidationIssue("group.id.duplicate", "Group IDs must be unique inside a project.")
        if (project.tracks.map { it.id }.distinct().size != project.tracks.size) issues += ValidationIssue("track.id.duplicate", "Track IDs must be unique inside a project.")
        val groupIds = project.groups.map { it.id }.toSet()
        val validRoles = (BuiltInRoles.definitions + project.customRoles).map { it.id }.toSet()
        project.tracks.forEach { track ->
            if (track.groupId != null && track.groupId !in groupIds) issues += ValidationIssue("track.group.missing", "Track '${track.name}' references a missing group.")
            if (track.roleId != null && track.roleId !in validRoles) issues += ValidationIssue("track.role.missing", "Track '${track.name}' references an unknown role.")
            if (track.pan !in -1f..1f) issues += ValidationIssue("track.pan.range", "Track '${track.name}' pan must be between -1 and +1.")
        }
        val customRoleIds = project.customRoles.map { it.id }
        if (customRoleIds.distinct().size != customRoleIds.size) issues += ValidationIssue("role.id.duplicate", "Custom role IDs must be unique.")
        if (customRoleIds.any { custom -> BuiltInRoles.definitions.any { it.id == custom } }) issues += ValidationIssue("role.id.builtin-collision", "Custom roles must not reuse built-in role IDs.")
        return issues
    }
}
