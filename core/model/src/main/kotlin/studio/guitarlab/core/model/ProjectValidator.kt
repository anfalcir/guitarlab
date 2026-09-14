package studio.guitarlab.core.model

data class ValidationIssue(val code: String, val message: String)

object ProjectValidator {
    fun validate(project: GuitarProject): List<ValidationIssue> {
        val issues = mutableListOf<ValidationIssue>()
        if (project.schemaVersion <= 0) issues += ValidationIssue("schema.invalid", "A versão do projeto deve ser positiva.")
        if (project.id.isBlank()) issues += ValidationIssue("project.id.blank", "O ID do projeto não pode ficar vazio.")
        if (project.name.isBlank()) issues += ValidationIssue("project.name.blank", "O nome do projeto não pode ficar vazio.")
        if (project.masterGainDb !in -60f..12f) issues += ValidationIssue("project.master-gain.range", "O ganho Master deve ficar entre -60 dB e +12 dB.")
        if (project.groups.map { it.id }.distinct().size != project.groups.size) issues += ValidationIssue("group.id.duplicate", "Os IDs dos grupos devem ser únicos.")
        if (project.tracks.map { it.id }.distinct().size != project.tracks.size) issues += ValidationIssue("track.id.duplicate", "Os IDs das pistas devem ser únicos.")
        if (project.clips.map { it.id }.distinct().size != project.clips.size) issues += ValidationIssue("clip.id.duplicate", "Os IDs dos clipes devem ser únicos.")
        if (project.markers.map { it.id }.distinct().size != project.markers.size) issues += ValidationIssue("marker.id.duplicate", "Os IDs dos marcadores devem ser únicos.")
        if (project.sections.map { it.id }.distinct().size != project.sections.size) issues += ValidationIssue("section.id.duplicate", "Os IDs das seções devem ser únicos.")
        if (project.takes.map { it.id }.distinct().size != project.takes.size) issues += ValidationIssue("take.id.duplicate", "Os IDs dos takes devem ser únicos.")

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
            if (clip.takeId != null && project.takes.none { it.id == clip.takeId }) issues += ValidationIssue("clip.take.missing", "O clipe '${clip.name}' referencia um take inexistente.")
            if (clip.name.isBlank()) issues += ValidationIssue("clip.name.blank", "O nome do clipe não pode ficar vazio.")
            if (clip.sourceUri.isBlank()) issues += ValidationIssue("clip.source.blank", "O clipe '${clip.name}' precisa referenciar uma fonte de áudio.")
            if (clip.startFrame < 0) issues += ValidationIssue("clip.start.negative", "O início do clipe '${clip.name}' não pode ser negativo.")
            if (clip.sourceStartFrame < 0) issues += ValidationIssue("clip.source-start.negative", "O início da fonte do clipe '${clip.name}' não pode ser negativo.")
            if (clip.lengthFrames <= 0) issues += ValidationIssue("clip.length.invalid", "A duração do clipe '${clip.name}' deve ser positiva.")
            if (clip.startFrame > Long.MAX_VALUE - clip.lengthFrames.coerceAtLeast(0L)) issues += ValidationIssue("clip.timeline.overflow", "O intervalo do clipe '${clip.name}' excede a timeline suportada.")
            if (clip.sourceStartFrame > Long.MAX_VALUE - clip.lengthFrames.coerceAtLeast(0L)) issues += ValidationIssue("clip.source.overflow", "O intervalo de fonte do clipe '${clip.name}' excede o limite suportado.")
            if (clip.sourceSampleRateHz != null && clip.sourceSampleRateHz <= 0) issues += ValidationIssue("clip.source-rate.invalid", "A taxa de amostragem da fonte é inválida.")
            if (clip.sourceChannelCount != null && clip.sourceChannelCount !in 1..32) issues += ValidationIssue("clip.source-channels.invalid", "A quantidade de canais da fonte é inválida.")
            if (clip.sourceBitsPerSample != null && clip.sourceBitsPerSample <= 0) issues += ValidationIssue("clip.source-bits.invalid", "A profundidade de bits da fonte é inválida.")
            if (clip.sourceTotalFrames != null && clip.sourceTotalFrames <= 0) issues += ValidationIssue("clip.source-total.invalid", "A quantidade de frames da fonte é inválida.")
            if (clip.editingSampleRateHz != null && clip.editingSampleRateHz <= 0) issues += ValidationIssue("clip.editing-rate.invalid", "A taxa de amostragem de edição é inválida.")
            if (clip.editingTotalFrames != null && clip.editingTotalFrames <= 0) issues += ValidationIssue("clip.editing-total.invalid", "A quantidade de frames de edição é inválida.")
            if (clip.fadeInFrames < 0 || clip.fadeOutFrames < 0 || clip.fadeInFrames > clip.lengthFrames || clip.fadeOutFrames > clip.lengthFrames) {
                issues += ValidationIssue("clip.fade.bounds", "Os fades do clipe '${clip.name}' excedem sua duração.")
            }
            val editingBound = clip.editingTotalFrames ?: clip.sourceTotalFrames
            if (editingBound != null && (clip.sourceStartFrame > editingBound || clip.lengthFrames > editingBound - clip.sourceStartFrame)) {
                issues += ValidationIssue("clip.trim.bounds", "O corte do clipe '${clip.name}' ultrapassa os limites da fonte.")
            }
            clip.managedSourcePath?.let { path ->
                if (!path.startsWith("media/source/") || path.contains("..") || path.startsWith('/')) {
                    issues += ValidationIssue("clip.managed-source.path", "O caminho interno do clipe '${clip.name}' é inválido.")
                }
            }
            clip.managedEditProxyPath?.let { path ->
                if (!path.startsWith("media/proxy/") || path.contains("..") || path.startsWith('/')) {
                    issues += ValidationIssue("clip.managed-proxy.path", "O caminho interno do proxy do clipe '${clip.name}' é inválido.")
                }
            }
        }

        project.markers.forEach { marker ->
            if (marker.name.isBlank() || marker.frame < 0L) issues += ValidationIssue("marker.invalid", "Marcador inválido.")
        }
        project.sections.forEach { section ->
            if (section.name.isBlank() || section.startFrame < 0L || section.endFrame <= section.startFrame) issues += ValidationIssue("section.invalid", "Seção inválida.")
            if (section.confidence != null && section.confidence !in 0f..1f) issues += ValidationIssue("section.confidence.range", "A confiança da seção deve ficar entre 0 e 1.")
        }
        project.takes.forEach { take ->
            val clip = project.clips.firstOrNull { it.id == take.clipId }
            if (take.trackId !in trackIds || clip == null || clip.trackId != take.trackId || clip.takeId != take.id || take.name.isBlank()) {
                issues += ValidationIssue("take.reference.invalid", "O take '${take.name}' possui referências inconsistentes.")
            }
        }
        project.takes.groupBy { it.trackId }.filterValues { takes -> takes.count { it.active } > 1 }.forEach { (trackId, _) ->
            issues += ValidationIssue("take.active.multiple", "A pista '$trackId' possui mais de um take ativo.")
        }
        project.punchRegion?.let { punch ->
            if (punch.startFrame < 0L || punch.endFrame <= punch.startFrame || punch.preRollFrames < 0L || punch.postRollFrames < 0L) {
                issues += ValidationIssue("punch.invalid", "A região de punch é inválida.")
            }
        }

        val customRoleIds = project.customRoles.map { it.id }
        if (customRoleIds.distinct().size != customRoleIds.size) issues += ValidationIssue("role.id.duplicate", "As funções personalizadas precisam ter IDs únicos.")
        if (customRoleIds.any { custom -> BuiltInRoles.definitions.any { it.id == custom } }) issues += ValidationIssue("role.id.builtin-collision", "Funções personalizadas não podem reutilizar IDs internos.")
        return issues
    }
}
