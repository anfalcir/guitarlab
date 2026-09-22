package studio.guitarlab.core.model

data class ValidationIssue(val code: String, val message: String)

object ProjectValidator {
    fun validate(project: GuitarProject): List<ValidationIssue> {
        val issues = mutableListOf<ValidationIssue>()
        if (project.schemaVersion !in 1..CURRENT_PROJECT_SCHEMA_VERSION) issues += ValidationIssue("schema.invalid", "A versão do projeto não é suportada.")
        if (project.id.isBlank()) issues += ValidationIssue("project.id.blank", "O ID do projeto não pode ficar vazio.")
        if (project.name.isBlank()) issues += ValidationIssue("project.name.blank", "O nome do projeto não pode ficar vazio.")
        if (project.masterGainDb !in -60f..12f) issues += ValidationIssue("project.master-gain.range", "O ganho Master deve ficar entre -60 dB e +12 dB.")
        if (project.groups.map { it.id }.distinct().size != project.groups.size) issues += ValidationIssue("group.id.duplicate", "Os IDs dos grupos devem ser únicos.")
        if (project.tracks.map { it.id }.distinct().size != project.tracks.size) issues += ValidationIssue("track.id.duplicate", "Os IDs das pistas devem ser únicos.")
        if (project.clips.map { it.id }.distinct().size != project.clips.size) issues += ValidationIssue("clip.id.duplicate", "Os IDs dos clipes devem ser únicos.")
        if (project.markers.map { it.id }.distinct().size != project.markers.size) issues += ValidationIssue("marker.id.duplicate", "Os IDs dos marcadores devem ser únicos.")
        if (project.sections.map { it.id }.distinct().size != project.sections.size) issues += ValidationIssue("section.id.duplicate", "Os IDs das seções devem ser únicos.")
        if (project.takes.map { it.id }.distinct().size != project.takes.size) issues += ValidationIssue("take.id.duplicate", "Os IDs dos takes devem ser únicos.")
        if (project.assets.map { it.assetId }.distinct().size != project.assets.size) issues += ValidationIssue("asset.id.duplicate", "Os IDs dos assets devem ser únicos.")
        if (project.referenceBindings.map { it.bindingId }.distinct().size != project.referenceBindings.size) issues += ValidationIssue("binding.id.duplicate", "Os IDs dos vínculos devem ser únicos.")

        val groupIds = project.groups.map { it.id }.toSet()
        val trackIds = project.tracks.map { it.id }.toSet()
        val validRoles = (BuiltInRoles.definitions + project.customRoles).map { it.id }.toSet()
        val assetIds = project.assets.map { it.assetId }.toSet()

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
                if (!(path.startsWith("media/source/") || path.startsWith("media/references/")) || path.contains("..") || path.startsWith('/')) {
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
            if (take.name.length > 48) issues += ValidationIssue("take.name.length", "O nome do take deve ter no máximo 48 caracteres.")
            if (take.note.length > 160) issues += ValidationIssue("take.note.length", "A nota do take deve ter no máximo 160 caracteres.")
        }
        project.takes.groupBy { it.trackId }.forEach { (trackId, takes) ->
            when (takes.count { it.active }) {
                0 -> issues += ValidationIssue("take.active.missing", "A pista '$trackId' possui takes, mas nenhum está ativo.")
                1 -> Unit
                else -> issues += ValidationIssue("take.active.multiple", "A pista '$trackId' possui mais de um take ativo.")
            }
        }
        project.punchRegion?.let { punch ->
            if (punch.startFrame < 0L || punch.endFrame <= punch.startFrame || punch.preRollFrames < 0L || punch.postRollFrames < 0L) {
                issues += ValidationIssue("punch.invalid", "A região de punch é inválida.")
            }
        }

        val sha256Pattern = Regex("[0-9a-f]{64}")
        project.assets.forEach { asset ->
            if (asset.assetId.isBlank()) issues += ValidationIssue("asset.id.blank", "O ID do asset não pode ficar vazio.")
            if (asset.relativePath.isBlank() || asset.relativePath.startsWith('/') || asset.relativePath.contains('\\') || asset.relativePath.split('/').any { it == "." || it == ".." || it.isBlank() }) {
                issues += ValidationIssue("asset.path.invalid", "O caminho do asset '${asset.assetId}' é inválido.")
            }
            if (!sha256Pattern.matches(asset.sha256)) issues += ValidationIssue("asset.sha256.invalid", "O SHA-256 do asset '${asset.assetId}' é inválido.")
            if (asset.byteSize <= 0L) issues += ValidationIssue("asset.size.invalid", "O tamanho do asset '${asset.assetId}' deve ser positivo.")
            if (asset.format.isBlank()) issues += ValidationIssue("asset.format.blank", "O formato do asset '${asset.assetId}' deve ser informado.")
            if (asset.sampleRateHz != null && asset.sampleRateHz <= 0) issues += ValidationIssue("asset.sample-rate.invalid", "A taxa do asset '${asset.assetId}' é inválida.")
            if (asset.channelCount != null && asset.channelCount !in 1..32) issues += ValidationIssue("asset.channels.invalid", "Os canais do asset '${asset.assetId}' são inválidos.")
            if (asset.frameCount != null && asset.frameCount <= 0L) issues += ValidationIssue("asset.frames.invalid", "Os frames do asset '${asset.assetId}' são inválidos.")
            if (asset.lifecycle == AssetLifecycle.STAGING) issues += ValidationIssue("asset.lifecycle.staging", "Um projeto publicado não pode referenciar asset em staging.")
            asset.provenance?.let { provenance ->
                if (provenance.kind.isBlank() || provenance.contractVersion <= 0) issues += ValidationIssue("asset.provenance.invalid", "A provenance do asset '${asset.assetId}' é inválida.")
                if (asset.assetId in provenance.inputAssetIds || provenance.inputAssetIds.any { it !in assetIds }) issues += ValidationIssue("asset.provenance.reference", "A provenance do asset '${asset.assetId}' contém referência inválida.")
                if (provenance.inputSha256.any { !sha256Pattern.matches(it) }) issues += ValidationIssue("asset.provenance.sha256", "A provenance do asset '${asset.assetId}' contém hash inválido.")
            }
        }

        project.preparation?.let { preparation ->
            val references = buildList {
                preparation.sourceAssetId?.let(::add)
                addAll(preparation.activeStemAssetIds.values)
                preparation.activeBackingAssetId?.let(::add)
                preparation.activeGuitarAssetId?.let(::add)
                addAll(preparation.availableReferenceAssetIds)
            }
            if (references.any { it !in assetIds }) issues += ValidationIssue("preparation.asset.missing", "A preparação referencia asset inexistente.")
            if (preparation.activeStemAssetIds.keys.any { it !in EXPECTED_STEM_ROLES }) issues += ValidationIssue("preparation.stem.role", "A preparação contém função de stem inválida.")
        }
        project.referenceBindings.forEach { binding ->
            if (binding.bindingId.isBlank() || binding.trackId !in trackIds || binding.assetId !in assetIds) issues += ValidationIssue("binding.reference.invalid", "O vínculo '${binding.bindingId}' possui referência inválida.")
        }

        val customRoleIds = project.customRoles.map { it.id }
        if (customRoleIds.distinct().size != customRoleIds.size) issues += ValidationIssue("role.id.duplicate", "As funções personalizadas precisam ter IDs únicos.")
        if (customRoleIds.any { custom -> BuiltInRoles.definitions.any { it.id == custom } }) issues += ValidationIssue("role.id.builtin-collision", "Funções personalizadas não podem reutilizar IDs internos.")
        return issues
    }

    private val EXPECTED_STEM_ROLES = setOf(
        AssetRole.STEM_DRUMS,
        AssetRole.STEM_BASS,
        AssetRole.STEM_OTHER,
        AssetRole.STEM_VOCALS,
        AssetRole.STEM_GUITAR,
        AssetRole.STEM_PIANO,
    )
}
