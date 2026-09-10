package studio.guitarlab.app.ui

import android.content.Context
import android.net.Uri
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import studio.guitarlab.core.audio.TrackMixPolicy
import studio.guitarlab.core.model.AudioClip
import studio.guitarlab.core.model.GuitarProject
import studio.guitarlab.core.project.FileProjectRepository
import studio.guitarlab.core.project.ProjectBundleWriter
import studio.guitarlab.core.project.ProjectManagedMediaStore
import studio.guitarlab.core.project.TimelineControlPolicy
import studio.guitarlab.platform.audio.android.StudioMasterRenderClip
import studio.guitarlab.platform.audio.android.StudioMasterRenderRequest
import studio.guitarlab.platform.audio.android.StudioMasterRenderTrack
import studio.guitarlab.platform.audio.android.StudioMasterRenderer
import studio.guitarlab.platform.codec.android.AndroidMasterAudioEncoder
import studio.guitarlab.platform.codec.android.MasterExportFormat

class ProjectExportService(private val context: Context) {
    private val repository = FileProjectRepository(context.filesDir)
    private val mediaStore = ProjectManagedMediaStore(context.filesDir)

    suspend fun saveProject(projectId: String, uri: Uri) = withContext(Dispatchers.IO) {
        val project = repository.load(projectId) ?: error("Projeto não encontrado.")
        val output = context.contentResolver.openOutputStream(uri, "w") ?: error("O Android não conseguiu criar o arquivo do projeto.")
        output.use { ProjectBundleWriter().write(project, mediaStore.projectDirectoryForExport(project.id), it) }
    }

    suspend fun exportMaster(projectId: String, uri: Uri, format: MasterExportFormat) = withContext(Dispatchers.IO) {
        val project = repository.load(projectId) ?: error("Projeto não encontrado.")
        val rate = project.sampleRate.fixedHz ?: project.clips.firstNotNullOfOrNull { it.editingSampleRateHz ?: it.sourceSampleRateHz }
            ?: error("Projeto sem taxa de amostragem exportável.")
        val request = buildRequest(project, rate)
        val floatWav = File.createTempFile("guitarlab-home-master-", ".wav", context.cacheDir)
        val encoded = if (format == MasterExportFormat.WAV_FLOAT32) floatWav else File.createTempFile("guitarlab-home-master-", ".${format.extension}", context.cacheDir)
        try {
            StudioMasterRenderer.renderFloatWav(request, floatWav)
            if (format != MasterExportFormat.WAV_FLOAT32) AndroidMasterAudioEncoder.encode(floatWav, encoded, format)
            val source = if (format == MasterExportFormat.WAV_FLOAT32) floatWav else encoded
            val output = context.contentResolver.openOutputStream(uri, "w") ?: error("O Android não conseguiu criar o arquivo exportado.")
            output.use { target -> source.inputStream().buffered().use { it.copyTo(target) } }
        } finally {
            floatWav.delete()
            if (encoded != floatWav) encoded.delete()
        }
    }

    private fun buildRequest(project: GuitarProject, rate: Int): StudioMasterRenderRequest {
        val anySolo = project.tracks.any { it.solo }
        val audibleTracks = project.tracks.filter { TrackMixPolicy.isAudible(it.muted, it.solo, anySolo) }
        val ids = audibleTracks.map { it.id }.toSet()
        val clips = project.clips.mapNotNull { clip ->
            if (clip.muted || clip.trackId !in ids) return@mapNotNull null
            val path = editingPath(clip) ?: return@mapNotNull null
            val editingRate = clip.editingSampleRateHz ?: clip.sourceSampleRateHz
            require(editingRate == rate) { "Há clipe sem conversão para a taxa do projeto." }
            StudioMasterRenderClip(
                file = mediaStore.resolveEditable(project.id, path),
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
            sampleRateHz = rate,
            projectEndFrame = TimelineControlPolicy.projectEndFrame(project),
            clips = clips,
            trackMixes = audibleTracks.map { StudioMasterRenderTrack(it.id, it.gainDb, it.pan) },
            masterGainDb = project.masterGainDb,
        )
    }

    private fun editingPath(clip: AudioClip): String? = clip.managedEditProxyPath ?: clip.managedSourcePath
}
