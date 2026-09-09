package studio.guitarlab.core.model

data class ValidationIssue(val code: String, val message: String)

object ProjectValidator {
    fun validate(project: GuitarProject): List<ValidationIssue> {
        val issues = mutableListOf<ValidationIssue>()
        if (project.schemaVersion <= 0) issues += ValidationIssue("schema.invalid", "A versão do projeto deve ser positiva.")
        if (project.name.isBlank()) issues += ValidationIssue("project.name.blank", "O nome do projeto não pode ficar vazio.")
        if (project.masterGainDb !in -60f..12f) issues += ValidationIssue("project.master-gain.range", "O ganho Master deve ficar entre -60 dB e +12 dB.")
        if (project.groups.map { it.id }.distinct().size != project.groups.size) issues += ValidationIssue("group.id.duplicate", "Os IDs dos grupos devem ser únicos.")
        if (project.tracks.map { it.id }.distinct().size != project.tracks.size) issues += ValidationIssue("track.id.duplicate", "Os IDs das pistas devem ser únicos.")
        if (project.clips.map { it.id }.distinct().size != project.clips.size) issues += ValidationIssue("clip.id.duplicate", "Os IDs dos clipes devem ser únicos.")

        val groupIds = project.groups.map { it.id }.toSet()
        val trackIds = project.tracks.map { it.id }.toSet()
        val validRoles = (BuiltInRoles.definitions + project.customRoles).map { it.id }.toSet()

        project.tracks.forEach { track ->
            if (track.groupId != null && track.groupId !in groupIds) issues += ValidationIssue("track.group.missing", "A pista '${track.name}' referencia um grupo inexistente.")
            if (track.roleId != null && track.roleId !in validRoles) issues += ValidationIssue("track.role.missing", "A pista '${track.name}' usa uma função desconhecida.")
            if (track.pan !in -1f..1f) issues += ValidationIssue("track.pan.range", "O pan da pista '${track.name}' deve ficar entre -1 e +1.")
            if (track.gainDb !in -60f..12f) issues += ValidationIssue("track.gain.range", "O ganho da pista '${track.name}' deve ficar entre -60 dB e +12 dB.")
            if (track.colorIndex !in -1..19) issues += ValidationIssue("track.color.range", "A cor da pista '${track.name}' é inválida.")
        }

        project.clips.forEach { clip ->
            if (clip.trackId !in trackIds) issues += ValidationIssue("clip.track.missing", "O clipe '${clip.name}' referencia uma pista inexistente.")
            if (clip.name.isBlank()) issues += ValidationIssue("clip.name.blank", "O nome do clipe não pode ficar vazio.")
            if (clip.sourceUri.isBlank()) issues += ValidationIssue("clip.source.blank", "O clipe '${clip.name}' precisa referenciar uma fonte de áudio.")
            if (clip.startFrame < 0) issues += ValidationIssue("clip.start.negative", "O início do clipe '${clip.name}' não pode ser negativo.")
            if (clip.sourceStartFrame < 0) issues += ValidationIssue("clip.source-start.negative", "O início da fonte do clipe '${clip.name}' não pode ser negativo.")
            if (clip.lengthFrames <= 0) issues += ValidationIssue("clip.length.invalid", "A duração do clipe '${clip.name}' deve ser positiva.")
            if (clip.sourceSampleRateHz != null && clip.sourceSampleRateHz <= 0) issues += ValidationIssue("clip.source-rate.invalid", "A taxa de amostragem da fonte é inválida.")
            if (clip.sourceChannelCount != null && clip.sourceChannelCount !in 1..32) issues += ValidationIssue("clip.source-channels.invalid", "A quantidade de canais da fonte é inválida.")
            if (clip.sourceBitsPerSample != null && clip.sourceBitsPerSample <= 0) issues += ValidationIssue("clip.source-bits.invalid", "A profundidade de bits da fonte é inválida.")
            if (clip.sourceTotalFrames != null && clip.sourceTotalFrames <= 0) issues += ValidationIssue("clip.source-total.invalid", "A quantidade de frames da fonte é inválida.")
            if (clip.sourceTotalFrames != null && clip.sourceStartFrame + clip.lengthFrames > clip.sourceTotalFrames) {
                issues += ValidationIssue("clip.trim.bounds", "O corte do clipe '${clip.name}' ultrapassa os limites da fonte.")
            }
            clip.managedSourcePath?.let { path ->
                if (!path.startsWith("media/source/") || path.contains("..") || path.startsWith('/')) {
                    issues += ValidationIssue("clip.managed-source.path", "O caminho interno do clipe '${clip.name}' é inválido.")
                }
            }
        }

        val customRoleIds = project.customRoles.map { it.id }
        if (customRoleIds.distinct().size != customRoleIds.size) issues += ValidationIssue("role.id.duplicate", "As funções personalizadas precisam ter IDs únicos.")
        if (customRoleIds.any { custom -> BuiltInRoles.definitions.any { it.id == custom } }) issues += ValidationIssue("role.id.builtin-collision", "Funções personalizadas não podem reutilizar IDs internos.")
        return issues
    }
}
