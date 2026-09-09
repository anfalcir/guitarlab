package studio.guitarlab.core.project

import studio.guitarlab.core.model.AudioClip
import studio.guitarlab.core.model.GuitarProject

data class RecordingTarget(
    val trackId: String,
    val preferredSampleRateHz: Int?,
)

/**
 * Product policy for the current single-global-input recording architecture.
 *
 * One capture source maps to exactly one armed target track per take. This avoids silently
 * duplicating the same input into several tracks, which would create confusing double-level
 * playback and ambiguous ownership. Multi-input/multi-track capture can be added later as an
 * explicit routing feature rather than inferred from several armed tracks.
 */
object RecordingTargetPolicy {
    fun resolve(project: GuitarProject): RecordingTarget {
        val armed = project.tracks.filter { it.armed }
        require(armed.isNotEmpty()) { "Arme uma pista antes de gravar." }
        require(armed.size == 1) { "Arme somente uma pista por vez para a entrada global atual." }

        val fixedRate = project.sampleRate.fixedHz
        val sourceRates = project.clips.mapNotNull { it.sourceSampleRateHz }.distinct()
        if (fixedRate == null) {
            require(sourceRates.size <= 1) {
                "O projeto contém taxas de amostragem diferentes; defina uma taxa única antes de gravar."
            }
        }
        return RecordingTarget(
            trackId = armed.single().id,
            preferredSampleRateHz = fixedRate ?: sourceRates.singleOrNull(),
        )
    }
}

data class RecordedTakeMetadata(
    val clipId: String,
    val displayName: String,
    val managedRelativePath: String,
    val timelineStartFrame: Long,
    val sampleRateHz: Int,
    val channelCount: Int,
    val framesCaptured: Long,
)

/**
 * Converts a finalized managed WAV take into project metadata without rewriting audio bytes.
 * The file must already have been atomically promoted to media/source by ProjectRecordingMediaStore.
 */
object RecordedTakeProjectIntegrator {
    fun integrate(
        project: GuitarProject,
        targetTrackId: String,
        take: RecordedTakeMetadata,
        nowEpochMs: Long,
    ): GuitarProject {
        require(project.tracks.any { it.id == targetTrackId }) { "A pista de gravação não existe mais." }
        require(take.clipId.isNotBlank()) { "O ID do take não pode ficar vazio." }
        require(project.clips.none { it.id == take.clipId }) { "Já existe um clipe com o ID informado." }
        require(take.displayName.isNotBlank()) { "O take precisa ter um nome." }
        require(take.managedRelativePath.startsWith("media/source/")) { "O take precisa estar no armazenamento gerenciado do projeto." }
        require(!take.managedRelativePath.contains("..")) { "O caminho gerenciado do take é inválido." }
        require(take.timelineStartFrame >= 0L) { "O início do take na timeline não pode ser negativo." }
        require(take.sampleRateHz > 0) { "A taxa de amostragem do take deve ser positiva." }
        require(take.channelCount in 1..2) { "A gravação atual suporta takes mono ou estéreo." }
        require(take.framesCaptured > 0L) { "O take precisa conter áudio." }

        project.sampleRate.fixedHz?.let { fixed ->
            require(fixed == take.sampleRateHz) {
                "O take foi gravado em ${take.sampleRateHz} Hz, mas o projeto exige $fixed Hz."
            }
        }
        val existingRates = project.clips.mapNotNull { it.sourceSampleRateHz }.distinct()
        if (project.sampleRate.fixedHz == null && existingRates.isNotEmpty()) {
            require(existingRates.size == 1 && existingRates.single() == take.sampleRateHz) {
                "O take não corresponde à taxa de amostragem usada pelos clipes do projeto."
            }
        }

        val clip = AudioClip(
            id = take.clipId,
            trackId = targetTrackId,
            name = take.displayName.trim(),
            sourceUri = "managed://${take.managedRelativePath}",
            managedSourcePath = take.managedRelativePath,
            startFrame = take.timelineStartFrame,
            sourceStartFrame = 0L,
            lengthFrames = take.framesCaptured,
            sourceTotalFrames = take.framesCaptured,
            sourceFormat = "WAV",
            sourceSampleRateHz = take.sampleRateHz,
            sourceChannelCount = take.channelCount,
            sourceBitsPerSample = 32,
            sourceEncoding = "FLOAT32_LE",
        )
        return project.copy(
            clips = project.clips + clip,
            updatedAtEpochMs = nowEpochMs,
        )
    }
}
