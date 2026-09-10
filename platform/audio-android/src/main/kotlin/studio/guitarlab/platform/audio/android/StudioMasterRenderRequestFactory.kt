package studio.guitarlab.platform.audio.android

import java.io.File
import studio.guitarlab.core.audio.TrackMixPolicy
import studio.guitarlab.core.model.GuitarProject
import studio.guitarlab.core.project.TimelineControlPolicy

/** Canonical request construction shared by every master-export entry point. */
object StudioMasterRenderRequestFactory {
    fun create(
        project: GuitarProject,
        sampleRateHz: Int,
        resolveEditable: (String) -> File,
    ): StudioMasterRenderRequest {
        require(sampleRateHz > 0) { "Taxa de amostragem inválida." }
        val anySolo = project.tracks.any { it.solo }
        val audibleTracks = project.tracks.filter {
            TrackMixPolicy.isAudible(it.muted, it.solo, anySolo)
        }
        val audibleIds = audibleTracks.mapTo(mutableSetOf()) { it.id }
        val clips = project.clips.mapNotNull { clip ->
            if (clip.muted || clip.trackId !in audibleIds) return@mapNotNull null
            val path = clip.managedEditProxyPath ?: clip.managedSourcePath ?: return@mapNotNull null
            val editingRate = clip.editingSampleRateHz ?: clip.sourceSampleRateHz
            require(editingRate == sampleRateHz) {
                "Há clipe sem conversão para a taxa do projeto."
            }
            StudioMasterRenderClip(
                file = resolveEditable(path),
                trackId = clip.trackId,
                timelineStartFrame = clip.startFrame,
                sourceStartFrame = clip.sourceStartFrame,
                lengthFrames = clip.lengthFrames,
                gainDb = clip.gainDb,
                fadeInFrames = clip.fadeInFrames,
                fadeOutFrames = clip.fadeOutFrames,
            )
        }
        require(clips.isNotEmpty()) { "Não há áudio audível para exportar." }
        return StudioMasterRenderRequest(
            sampleRateHz = sampleRateHz,
            projectEndFrame = TimelineControlPolicy.projectEndFrame(project),
            clips = clips,
            trackMixes = audibleTracks.map { StudioMasterRenderTrack(it.id, it.gainDb, it.pan) },
            masterGainDb = project.masterGainDb,
        )
    }
}
