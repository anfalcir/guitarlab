package studio.guitarlab.app.ui

import android.app.Application
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import studio.guitarlab.core.codec.FileSeekableByteSource
import studio.guitarlab.core.codec.WavMetadataReader
import studio.guitarlab.core.codec.WavPcmDecoder
import studio.guitarlab.core.codec.WaveformEnvelopeBuilder
import studio.guitarlab.core.model.AudioClip
import studio.guitarlab.core.model.GuitarProject
import studio.guitarlab.core.project.FileProjectRepository
import studio.guitarlab.core.project.ProjectClipEditor
import studio.guitarlab.core.project.ProjectManagedMediaStore
import studio.guitarlab.core.project.TimelineControlPolicy
import studio.guitarlab.core.project.TimelineControlState
import studio.guitarlab.core.project.WaveformCacheStore

data class StudioUiState(
    val loading: Boolean = true,
    val importing: Boolean = false,
    val editingClip: Boolean = false,
    val project: GuitarProject? = null,
    val waveforms: Map<String, List<Float>> = emptyMap(),
    val timelineControls: TimelineControlState = TimelineControlState(),
    val error: String? = null,
    val importStatus: String? = null,
    val clipStatus: String? = null,
)

class StudioViewModel(application: Application) : AndroidViewModel(application) {
    private val rootDirectory = application.filesDir
    private val repository = FileProjectRepository(rootDirectory)
    private val mediaStore = ProjectManagedMediaStore(rootDirectory)
    private val waveformCache = WaveformCacheStore(rootDirectory)
    private val _state = MutableStateFlow(StudioUiState())
    val state: StateFlow<StudioUiState> = _state.asStateFlow()

    fun load(projectId: String) {
        if (_state.value.project?.id == projectId && !_state.value.loading) return
        viewModelScope.launch {
            _state.value = StudioUiState(loading = true)
            runCatching {
                withContext(Dispatchers.IO) {
                    val project = repository.load(projectId) ?: error("Project not found: $projectId")
                    project to loadWaveforms(project)
                }
            }.onSuccess { (project, waveforms) ->
                val end = TimelineControlPolicy.projectEndFrame(project)
                _state.value = StudioUiState(
                    loading = false,
                    project = project,
                    waveforms = waveforms,
                    timelineControls = TimelineControlPolicy.normalizedForProject(TimelineControlState(), end),
                )
            }.onFailure { error ->
                _state.value = StudioUiState(
                    loading = false,
                    error = error.message ?: "Unable to load project.",
                )
            }
        }
    }

    fun importWav(trackId: String, uri: Uri) {
        val current = _state.value.project ?: return
        if (current.tracks.none { it.id == trackId }) {
            _state.value = _state.value.copy(error = "The selected destination track no longer exists.")
            return
        }

        viewModelScope.launch {
            _state.value = _state.value.copy(
                importing = true,
                error = null,
                importStatus = "Copying into project storage…",
                clipStatus = null,
            )
            runCatching {
                withContext(Dispatchers.IO) {
                    val context = getApplication<Application>()
                    val displayName = queryDisplayName(uri)?.takeIf { it.isNotBlank() } ?: "Imported WAV.wav"
                    val input = context.contentResolver.openInputStream(uri)
                        ?: error("Android could not read the selected audio document.")
                    val asset = input.use { mediaStore.ingest(current.id, displayName, it) }

                    try {
                        val metadata = FileSeekableByteSource(asset.file).use { source ->
                            WavMetadataReader().read(source)
                        }
                        require(metadata.totalFrames > 0) { "Selected WAV contains no complete audio frames." }

                        val clipId = UUID.randomUUID().toString()
                        val envelope = FileSeekableByteSource(asset.file).use { source ->
                            WaveformEnvelopeBuilder.build(WavPcmDecoder(source), WAVEFORM_POINTS)
                        }
                        waveformCache.write(current.id, clipId, envelope)

                        val clip = AudioClip(
                            id = clipId,
                            trackId = trackId,
                            name = displayName,
                            sourceUri = "managed://${asset.relativePath}",
                            managedSourcePath = asset.relativePath,
                            originUri = uri.toString(),
                            startFrame = 0,
                            sourceStartFrame = 0,
                            lengthFrames = metadata.totalFrames,
                            sourceTotalFrames = metadata.totalFrames,
                            sourceFormat = metadata.fileFormat.name,
                            sourceSampleRateHz = metadata.sampleRateHz,
                            sourceChannelCount = metadata.channelCount,
                            sourceBitsPerSample = metadata.bitsPerSample,
                            sourceEncoding = metadata.sampleEncoding.name,
                        )
                        val saved = repository.save(
                            current.copy(
                                clips = current.clips + clip,
                                updatedAtEpochMs = System.currentTimeMillis(),
                            )
                        )
                        Triple(saved, metadata, envelope.peaks)
                    } catch (error: Throwable) {
                        mediaStore.discardUncommitted(current.id, asset.relativePath)
                        throw error
                    }
                }
            }.onSuccess { (saved, metadata, peaks) ->
                val imported = saved.clips.last()
                val end = TimelineControlPolicy.projectEndFrame(saved)
                _state.value = StudioUiState(
                    loading = false,
                    project = saved,
                    waveforms = _state.value.waveforms + (imported.id to peaks),
                    timelineControls = TimelineControlPolicy.normalizedForProject(_state.value.timelineControls, end),
                    importStatus = "Imported safely • ${metadata.channelCount}ch • ${metadata.sampleRateHz} Hz • immutable project copy",
                )
            }.onFailure { error ->
                _state.value = _state.value.copy(
                    importing = false,
                    error = error.message ?: "The WAV could not be imported.",
                    importStatus = null,
                )
            }
        }
    }

    fun setPlayheadFrame(frame: Long) {
        val project = _state.value.project ?: return
        val end = TimelineControlPolicy.projectEndFrame(project)
        _state.value = _state.value.copy(
            timelineControls = TimelineControlPolicy.movePlayhead(_state.value.timelineControls, frame, end)
        )
    }

    fun setLoopStartFrame(frame: Long) {
        val project = _state.value.project ?: return
        val end = TimelineControlPolicy.projectEndFrame(project)
        _state.value = _state.value.copy(
            timelineControls = TimelineControlPolicy.moveLoopStart(_state.value.timelineControls, frame, end)
        )
    }

    fun setLoopEndFrame(frame: Long) {
        val project = _state.value.project ?: return
        val end = TimelineControlPolicy.projectEndFrame(project)
        _state.value = _state.value.copy(
            timelineControls = TimelineControlPolicy.moveLoopEnd(_state.value.timelineControls, frame, end)
        )
    }

    fun toggleClipMuted(clipId: String) {
        editClip("Clip mute updated") { current ->
            val clip = current.clips.firstOrNull { it.id == clipId } ?: error("Clip not found: $clipId")
            ProjectClipEditor.setClipMuted(current, clipId, !clip.muted, System.currentTimeMillis())
        }
    }

    fun removeClip(clipId: String) {
        editClip("Clip removed") { current ->
            ProjectClipEditor.removeClip(current, clipId, System.currentTimeMillis())
        }
    }

    private fun editClip(status: String, transform: (GuitarProject) -> GuitarProject) {
        val current = _state.value.project ?: return
        if (_state.value.importing || _state.value.editingClip) return
        viewModelScope.launch {
            _state.value = _state.value.copy(editingClip = true, error = null, clipStatus = null)
            runCatching {
                withContext(Dispatchers.IO) { repository.save(transform(current)) }
            }.onSuccess { saved ->
                val validClipIds = saved.clips.map { it.id }.toSet()
                val end = TimelineControlPolicy.projectEndFrame(saved)
                _state.value = _state.value.copy(
                    editingClip = false,
                    project = saved,
                    waveforms = _state.value.waveforms.filterKeys { it in validClipIds },
                    timelineControls = TimelineControlPolicy.normalizedForProject(_state.value.timelineControls, end),
                    clipStatus = status,
                )
            }.onFailure { error ->
                _state.value = _state.value.copy(
                    editingClip = false,
                    error = error.message ?: "Clip edit failed.",
                )
            }
        }
    }

    private fun loadWaveforms(project: GuitarProject): Map<String, List<Float>> = buildMap {
        project.clips.forEach { clip ->
            val cached = waveformCache.read(project.id, clip.id)
            if (cached != null) {
                put(clip.id, cached.peaks)
                return@forEach
            }
            val managedPath = clip.managedSourcePath ?: return@forEach
            runCatching {
                val file = mediaStore.resolve(project.id, managedPath)
                val envelope = FileSeekableByteSource(file).use { source ->
                    WaveformEnvelopeBuilder.build(WavPcmDecoder(source), WAVEFORM_POINTS)
                }
                waveformCache.write(project.id, clip.id, envelope)
                put(clip.id, envelope.peaks)
            }
        }
    }

    private fun queryDisplayName(uri: Uri): String? {
        val resolver = getApplication<Application>().contentResolver
        return runCatching {
            resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                if (!cursor.moveToFirst()) return@use null
                val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index >= 0) cursor.getString(index) else null
            }
        }.getOrNull()
    }

    private companion object {
        const val WAVEFORM_POINTS = 320
    }
}
