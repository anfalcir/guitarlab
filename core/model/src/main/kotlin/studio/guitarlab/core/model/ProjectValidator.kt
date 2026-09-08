package studio.guitarlab.core.model

data class ValidationIssue(val code: String, val message: String)

object ProjectValidator {
    fun validate(project: GuitarProject): List<ValidationIssue> {
        val issues = mutableListOf<ValidationIssue>()
        if (project.schemaVersion <= 0) issues += ValidationIssue("schema.invalid", "Schema version must be positive.")
        if (project.name.isBlank()) issues += ValidationIssue("project.name.blank", "Project name must not be blank.")
        if (project.groups.map { it.id }.distinct().size != project.groups.size) issues += ValidationIssue("group.id.duplicate", "Group IDs must be unique inside a project.")
        if (project.tracks.map { it.id }.distinct().size != project.tracks.size) issues += ValidationIssue("track.id.duplicate", "Track IDs must be unique inside a project.")
        if (project.clips.map { it.id }.distinct().size != project.clips.size) issues += ValidationIssue("clip.id.duplicate", "Clip IDs must be unique inside a project.")

        val groupIds = project.groups.map { it.id }.toSet()
        val trackIds = project.tracks.map { it.id }.toSet()
        val validRoles = (BuiltInRoles.definitions + project.customRoles).map { it.id }.toSet()

        project.tracks.forEach { track ->
            if (track.groupId != null && track.groupId !in groupIds) issues += ValidationIssue("track.group.missing", "Track '${track.name}' references a missing group.")
            if (track.roleId != null && track.roleId !in validRoles) issues += ValidationIssue("track.role.missing", "Track '${track.name}' references an unknown role.")
            if (track.pan !in -1f..1f) issues += ValidationIssue("track.pan.range", "Track '${track.name}' pan must be between -1 and +1.")
        }

        project.clips.forEach { clip ->
            if (clip.trackId !in trackIds) issues += ValidationIssue("clip.track.missing", "Clip '${clip.name}' references a missing track.")
            if (clip.name.isBlank()) issues += ValidationIssue("clip.name.blank", "Clip name must not be blank.")
            if (clip.sourceUri.isBlank()) issues += ValidationIssue("clip.source.blank", "Clip '${clip.name}' must reference an audio source.")
            if (clip.startFrame < 0) issues += ValidationIssue("clip.start.negative", "Clip '${clip.name}' start frame must be non-negative.")
            if (clip.sourceStartFrame < 0) issues += ValidationIssue("clip.source-start.negative", "Clip '${clip.name}' source start frame must be non-negative.")
            if (clip.lengthFrames <= 0) issues += ValidationIssue("clip.length.invalid", "Clip '${clip.name}' length must be positive.")
            if (clip.sourceSampleRateHz != null && clip.sourceSampleRateHz <= 0) issues += ValidationIssue("clip.source-rate.invalid", "Clip '${clip.name}' source sample rate must be positive when known.")
            if (clip.sourceChannelCount != null && clip.sourceChannelCount !in 1..32) issues += ValidationIssue("clip.source-channels.invalid", "Clip '${clip.name}' source channel count is invalid.")
            if (clip.sourceBitsPerSample != null && clip.sourceBitsPerSample <= 0) issues += ValidationIssue("clip.source-bits.invalid", "Clip '${clip.name}' source bit depth must be positive when known.")
            if (clip.sourceTotalFrames != null && clip.sourceTotalFrames <= 0) issues += ValidationIssue("clip.source-total.invalid", "Clip '${clip.name}' source total frames must be positive when known.")
            if (clip.sourceTotalFrames != null && clip.sourceStartFrame + clip.lengthFrames > clip.sourceTotalFrames) {
                issues += ValidationIssue("clip.trim.bounds", "Clip '${clip.name}' trim exceeds the immutable source bounds.")
            }
            clip.managedSourcePath?.let { path ->
                if (!path.startsWith("media/source/") || path.contains("..") || path.startsWith('/')) {
                    issues += ValidationIssue("clip.managed-source.path", "Clip '${clip.name}' managed source path is invalid.")
                }
            }
        }

        val customRoleIds = project.customRoles.map { it.id }
        if (customRoleIds.distinct().size != customRoleIds.size) issues += ValidationIssue("role.id.duplicate", "Custom role IDs must be unique.")
        if (customRoleIds.any { custom -> BuiltInRoles.definitions.any { it.id == custom } }) issues += ValidationIssue("role.id.builtin-collision", "Custom roles must not reuse built-in role IDs.")
        return issues
    }
}
