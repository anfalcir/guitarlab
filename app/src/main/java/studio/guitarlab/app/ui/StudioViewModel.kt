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
import studio.guitarlab.core.audio.TrackMixPolicy
import studio.guitarlab.core.codec.FileSeekableByteSource
import studio.guitarlab.core.codec.WavMetadataReader
import studio.guitarlab.core.codec.WavPcmDecoder
import studio.guitarlab.core.codec.WaveformEnvelopeBuilder
import studio.guitarlab.core.model.AudioClip
import studio.guitarlab.core.model.AudioTrack
import studio.guitarlab.core.model.GuitarProject
import studio.guitarlab.core.project.FileProjectRepository
import studio.guitarlab.core.project.ProjectClipEditor
import studio.guitarlab.core.project.ProjectManagedMediaStore
import studio.guitarlab.core.project.TimelineControlPolicy
import studio.guitarlab.core.project.TimelineControlState
import studio.guitarlab.core.project.TransportMode
import studio.guitarlab.core.project.TransportPolicy
import studio.guitarlab.core.project.TransportState
import studio.guitarlab.core.project.TrimControlPolicy
import studio.guitarlab.core.project.TrimControlState
import studio.guitarlab.core.project.WaveformCacheStore
import studio.guitarlab.platform.audio.android.AndroidStudioPlaybackEngine
import studio.guitarlab.platform.audio.android.StudioPlaybackClip
import studio.guitarlab.platform.audio.android.StudioPlaybackListener
import studio.guitarlab.platform.audio.android.StudioPlaybackRequest
import studio.guitarlab.platform.audio.android.StudioPlaybackRoutingStatus

data class StudioUiState(
    val loading: Boolean = true,
    val importing: Boolean = false,
    val editingClip: Boolean = false,
    val project: GuitarProject? = null,
    val waveforms: Map<String, List<Float>> = emptyMap(),
    val timelineControls: TimelineControlState = TimelineControlState(),
    val trimControls: TrimControlState? = null,
    val transport: TransportState = TransportState(),
    val transportEngineReady: Boolean = false,
    val error: String? = null,
    val importStatus: String? = null,
    val clipStatus: String? = null,
)

class StudioViewModel(application: Application) : AndroidViewModel(application) {
    private val rootDirectory = application.filesDir
    private val repository = FileProjectRepository(rootDirectory)
    private val mediaStore = ProjectManagedMediaStore(rootDirectory)
    private val waveformCache = WaveformCacheStore(rootDirectory)
    private val audioRoutingStore = StudioAudioRoutingStore(application)
    private val playbackEngine = AndroidStudioPlaybackEngine()
    private val _state = MutableStateFlow(StudioUiState())
    val state: StateFlow<StudioUiState> = _state.asStateFlow()

    fun load(projectId: String) {
        if (_state.value.project?.id == projectId && !_state.value.loading) return
        playbackEngine.stop()
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
                    transportEngineReady = playbackReadiness(project).ready,
                )
            }.onFailure { error ->
                _state.value = StudioUiState(loading = false, error = error.message ?: "Unable to load project.")
            }
        }
    }

    fun importWav(trackId: String, uri: Uri) {
        val currentState = _state.value
        val current = currentState.project ?: return
        if (!TransportPolicy.timelineEditingEnabled(currentState.transport) || currentState.trimControls != null) return
        if (current.tracks.none { it.id == trackId }) {
            _state.value = currentState.copy(error = "The selected destination track no longer exists.")
            return
        }
        viewModelScope.launch {
            _state.value = _state.value.copy(importing = true, error = null, importStatus = "Copying into project storage…", clipStatus = null)
            runCatching {
                withContext(Dispatchers.IO) {
                    val context = getApplication<Application>()
                    val displayName = queryDisplayName(uri)?.takeIf { it.isNotBlank() } ?: "Imported WAV.wav"
                    val input = context.contentResolver.openInputStream(uri) ?: error("Android could not read the selected audio document.")
                    val asset = input.use { mediaStore.ingest(current.id, displayName, it) }
                    try {
                        val metadata = FileSeekableByteSource(asset.file).use { WavMetadataReader().read(it) }
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
                        val saved = repository.save(current.copy(clips = current.clips + clip, updatedAtEpochMs = System.currentTimeMillis()))
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
                    transport = _state.value.transport,
                    transportEngineReady = playbackReadiness(saved).ready,
                    importStatus = "Imported safely • ${metadata.channelCount}ch • ${metadata.sampleRateHz} Hz • immutable project copy",
                )
            }.onFailure { error ->
                _state.value = _state.value.copy(importing = false, error = error.message ?: "The WAV could not be imported.", importStatus = null)
            }
        }
    }

    fun setPlayheadFrame(frame: Long) = editTimelineMarker { state, end -> TimelineControlPolicy.movePlayhead(state, frame, end) }
    fun setLoopStartFrame(frame: Long) = editTimelineMarker { state, end -> TimelineControlPolicy.moveLoopStart(state, frame, end) }
    fun setLoopEndFrame(frame: Long) = editTimelineMarker { state, end -> TimelineControlPolicy.moveLoopEnd(state, frame, end) }

    fun beginTrim(clipId: String) {
        val current = _state.value
        val project = current.project ?: return
        if (current.importing || current.editingClip || !TransportPolicy.timelineEditingEnabled(current.transport)) return
        val clip = project.clips.firstOrNull { it.id == clipId } ?: return
        _state.value = current.copy(trimControls = TrimControlPolicy.fromClip(clip), clipStatus = "Trim mode • drag mustard T◀ / T▶ markers, then Apply or Cancel", error = null)
    }

    fun setTrimStartFrame(frame: Long) {
        val current = _state.value
        val trim = current.trimControls ?: return
        if (!TransportPolicy.timelineEditingEnabled(current.transport)) return
        val clip = current.project?.clips?.firstOrNull { it.id == trim.clipId } ?: return
        _state.value = current.copy(trimControls = TrimControlPolicy.moveStart(trim, frame, clip))
    }

    fun setTrimEndFrame(frame: Long) {
        val current = _state.value
        val trim = current.trimControls ?: return
        if (!TransportPolicy.timelineEditingEnabled(current.transport)) return
        val clip = current.project?.clips?.firstOrNull { it.id == trim.clipId } ?: return
        _state.value = current.copy(trimControls = TrimControlPolicy.moveEnd(trim, frame, clip))
    }

    fun cancelTrim() {
        val current = _state.value
        if (!TransportPolicy.timelineEditingEnabled(current.transport)) return
        _state.value = current.copy(trimControls = null, clipStatus = "Trim cancelled")
    }

    fun applyTrim() {
        val current = _state.value
        val project = current.project ?: return
        val trim = current.trimControls ?: return
        if (current.importing || current.editingClip || !TransportPolicy.timelineEditingEnabled(current.transport)) return
        viewModelScope.launch {
            _state.value = current.copy(editingClip = true, error = null)
            runCatching {
                withContext(Dispatchers.IO) {
                    repository.save(
                        ProjectClipEditor.trimClipToTimelineEdges(
                            project = project,
                            clipId = trim.clipId,
                            timelineStartFrame = trim.startFrame,
                            timelineEndFrame = trim.endFrame,
                            nowEpochMs = System.currentTimeMillis(),
                        )
                    )
                }
            }.onSuccess { saved ->
                val end = TimelineControlPolicy.projectEndFrame(saved)
                _state.value = _state.value.copy(
                    editingClip = false,
                    project = saved,
                    trimControls = null,
                    timelineControls = TimelineControlPolicy.normalizedForProject(_state.value.timelineControls, end),
                    transportEngineReady = playbackReadiness(saved).ready,
                    clipStatus = "Trim applied non-destructively • immutable source unchanged",
                )
            }.onFailure { error ->
                _state.value = _state.value.copy(editingClip = false, error = error.message ?: "Trim could not be applied.")
            }
        }
    }

    fun returnToStart() {
        if (!TransportPolicy.timelineEditingEnabled(_state.value.transport)) return
        setPlayheadFrame(0L)
    }

    fun toggleLoop() {
        if (_state.value.trimControls != null) return
        _state.value = _state.value.copy(transport = TransportPolicy.toggleLoop(_state.value.transport))
    }

    fun togglePlayStop() {
        val current = _state.value
        when (current.transport.mode) {
            TransportMode.PLAYING -> {
                playbackEngine.stop()
                _state.value = current.copy(transport = current.transport.copy(mode = TransportMode.STOPPED))
            }
            TransportMode.RECORDING -> return
            TransportMode.STOPPED -> {
                if (current.trimControls != null) {
                    _state.value = current.copy(error = "Apply or cancel the current trim before playback.")
                    return
                }
                startPlayback(current)
            }
        }
    }

    fun startRecording() {
    }

    fun toggleClipMuted(clipId: String) {
        editClip("Clip mute updated") { current ->
            val clip = current.clips.firstOrNull { it.id == clipId } ?: error("Clip not found: $clipId")
            ProjectClipEditor.setClipMuted(current, clipId, !clip.muted, System.currentTimeMillis())
        }
    }

    fun removeClip(clipId: String) = editClip("Clip removed") { current ->
        ProjectClipEditor.removeClip(current, clipId, System.currentTimeMillis())
    }

    fun setTrackGainDb(trackId: String, gainDb: Float) {
        editTrack("Track level updated", trackId) { it.copy(gainDb = gainDb.coerceIn(-60f, 12f)) }
    }

    fun setTrackPan(trackId: String, pan: Float) {
        editTrack("Track pan updated", trackId) { it.copy(pan = pan.coerceIn(-1f, 1f)) }
    }

    fun toggleTrackMuted(trackId: String) {
        editTrack("Track mute updated", trackId) { it.copy(muted = !it.muted) }
    }

    fun toggleTrackSolo(trackId: String) {
        editTrack("Track solo updated", trackId) { it.copy(solo = !it.solo) }
    }

    override fun onCleared() {
        playbackEngine.close()
        super.onCleared()
    }

    private fun startPlayback(current: StudioUiState) {
        val project = current.project ?: return
        val readiness = playbackReadiness(project)
        if (!readiness.ready || readiness.sampleRateHz == null) {
            _state.value = current.copy(error = readiness.reason ?: "Project is not ready for playback.")
            return
        }
        val end = TimelineControlPolicy.projectEndFrame(project)
        val requestedStart = if (!current.transport.loopEnabled && current.timelineControls.playheadFrame >= end) 0L else current.timelineControls.playheadFrame
        val anySolo = project.tracks.any { it.solo }
        val tracksById = project.tracks.associateBy { it.id }
        val selectedOutputSignature = audioRoutingStore.selectedOutputSignature()
        val preferredOutput = audioRoutingStore.resolveSelectedOutputDevice()
        val request = runCatching {
            StudioPlaybackRequest(
                sampleRateHz = readiness.sampleRateHz,
                startFrame = requestedStart,
                projectEndFrame = end,
                loopEnabled = current.transport.loopEnabled,
                loopStartFrame = current.timelineControls.loopStartFrame,
                loopEndFrame = current.timelineControls.loopEndFrame,
                preferredOutputDevice = preferredOutput,
                preferredOutputRequested = !selectedOutputSignature.isNullOrBlank(),
                clips = project.clips.mapNotNull { clip ->
                    val sourceTrack = tracksById[clip.trackId] ?: return@mapNotNull null
                    if (clip.muted || !TrackMixPolicy.isAudible(sourceTrack.muted, sourceTrack.solo, anySolo)) return@mapNotNull null
                    val managedPath = requireNotNull(clip.managedSourcePath) { "Clip '${clip.name}' is not project-managed." }
                    StudioPlaybackClip(
                        file = mediaStore.resolve(project.id, managedPath),
                        timelineStartFrame = clip.startFrame,
                        sourceStartFrame = clip.sourceStartFrame,
                        lengthFrames = clip.lengthFrames,
                        gainDb = clip.gainDb + sourceTrack.gainDb,
                        pan = sourceTrack.pan,
                        muted = false,
                    )
                },
            )
        }.getOrElse { error ->
            _state.value = current.copy(error = error.message ?: "Unable to prepare managed media for playback.")
            return
        }

        _state.value = current.copy(
            timelineControls = TimelineControlPolicy.movePlayhead(current.timelineControls, requestedStart, end),
            transport = current.transport.copy(mode = TransportMode.PLAYING),
            error = null,
        )
        runCatching {
            playbackEngine.start(request, object : StudioPlaybackListener {
                override fun onPosition(frame: Long) {
                    viewModelScope.launch {
                        val state = _state.value
                        if (state.transport.mode == TransportMode.PLAYING) {
                            _state.value = state.copy(timelineControls = state.timelineControls.copy(playheadFrame = frame))
                        }
                    }
                }

                override fun onStopped(frame: Long) {
                    viewModelScope.launch {
                        val state = _state.value
                        _state.value = state.copy(
                            transport = state.transport.copy(mode = TransportMode.STOPPED),
                            timelineControls = state.timelineControls.copy(playheadFrame = frame),
                        )
                    }
                }

                override fun onError(message: String) {
                    viewModelScope.launch {
                        val state = _state.value
                        _state.value = state.copy(transport = state.transport.copy(mode = TransportMode.STOPPED), error = message)
                    }
                }

                override fun onRouting(status: StudioPlaybackRoutingStatus) {
                    viewModelScope.launch {
                        val state = _state.value
                        val routeStatus = when {
                            status.usingPreferredOutput -> "Output • ${status.deviceLabel ?: "preferred device"}"
                            status.fellBackToAuto -> "Preferred output unavailable • using Android Auto routing"
                            else -> null
                        }
                        if (routeStatus != null && state.transport.mode == TransportMode.PLAYING) {
                            _state.value = state.copy(clipStatus = routeStatus)
                        }
                    }
                }
            })
        }.onFailure { error ->
            _state.value = _state.value.copy(
                transport = _state.value.transport.copy(mode = TransportMode.STOPPED),
                error = error.message ?: "Studio playback could not start.",
            )
        }
    }

    private fun editTimelineMarker(transform: (TimelineControlState, Long) -> TimelineControlState) {
        val current = _state.value
        val project = current.project ?: return
        if (!TransportPolicy.timelineEditingEnabled(current.transport) || current.trimControls != null) return
        val end = TimelineControlPolicy.projectEndFrame(project)
        _state.value = current.copy(timelineControls = transform(current.timelineControls, end))
    }

    private fun editClip(status: String, transform: (GuitarProject) -> GuitarProject) {
        val currentState = _state.value
        val current = currentState.project ?: return
        if (currentState.importing || currentState.editingClip || currentState.trimControls != null || !TransportPolicy.timelineEditingEnabled(currentState.transport)) return
        viewModelScope.launch {
            _state.value = _state.value.copy(editingClip = true, error = null, clipStatus = null)
            runCatching { withContext(Dispatchers.IO) { repository.save(transform(current)) } }
                .onSuccess { saved ->
                    val validClipIds = saved.clips.map { it.id }.toSet()
                    val end = TimelineControlPolicy.projectEndFrame(saved)
                    _state.value = _state.value.copy(
                        editingClip = false,
                        project = saved,
                        waveforms = _state.value.waveforms.filterKeys { it in validClipIds },
                        timelineControls = TimelineControlPolicy.normalizedForProject(_state.value.timelineControls, end),
                        transportEngineReady = playbackReadiness(saved).ready,
                        clipStatus = status,
                    )
                }
                .onFailure { error -> _state.value = _state.value.copy(editingClip = false, error = error.message ?: "Clip edit failed.") }
        }
    }

    private fun editTrack(status: String, trackId: String, transform: (AudioTrack) -> AudioTrack) {
        val currentState = _state.value
        val current = currentState.project ?: return
        if (currentState.importing || currentState.editingClip || currentState.trimControls != null || !TransportPolicy.timelineEditingEnabled(currentState.transport)) return
        val target = current.tracks.firstOrNull { it.id == trackId } ?: return
        val updated = transform(target)
        viewModelScope.launch {
            _state.value = _state.value.copy(editingClip = true, error = null, clipStatus = null)
            runCatching {
                withContext(Dispatchers.IO) {
                    repository.save(
                        current.copy(
                            tracks = current.tracks.map { if (it.id == trackId) updated else it },
                            updatedAtEpochMs = System.currentTimeMillis(),
                        )
                    )
                }
            }.onSuccess { saved ->
                _state.value = _state.value.copy(
                    editingClip = false,
                    project = saved,
                    transportEngineReady = playbackReadiness(saved).ready,
                    clipStatus = status,
                )
            }.onFailure { error ->
                _state.value = _state.value.copy(editingClip = false, error = error.message ?: "Track mix update failed.")
            }
        }
    }

    private fun playbackReadiness(project: GuitarProject): PlaybackReadiness {
        val anySolo = project.tracks.any { it.solo }
        val tracksById = project.tracks.associateBy { it.id }
        val audible = project.clips.filter { clip ->
            if (clip.muted) return@filter false
            val track = tracksById[clip.trackId] ?: return@filter false
            TrackMixPolicy.isAudible(track.muted, track.solo, anySolo)
        }
        if (audible.isEmpty()) return PlaybackReadiness(false, reason = "Add or unmute a playable track before playback.")
        if (audible.any { it.managedSourcePath.isNullOrBlank() || it.sourceFormat != "WAV" }) {
            return PlaybackReadiness(false, reason = "M4 playback currently requires project-managed WAV clips.")
        }
        if (audible.any { it.sourceChannelCount !in 1..2 }) {
            return PlaybackReadiness(false, reason = "M4 playback currently supports mono/stereo sources.")
        }
        val firstRate = audible.first().sourceSampleRateHz ?: return PlaybackReadiness(false, reason = "Playback source sample rate is unknown.")
        val projectRate = project.sampleRate.fixedHz ?: firstRate
        if (audible.any { it.sourceSampleRateHz != projectRate }) {
            return PlaybackReadiness(false, reason = "Playback is gated because one or more clips need resampling. Source media remains immutable; resampling will use derived media when its gate is implemented.")
        }
        return PlaybackReadiness(true, projectRate)
    }

    private fun loadWaveforms(project: GuitarProject): Map<String, List<Float>> = buildMap {
        project.clips.forEach { clip ->
            waveformCache.read(project.id, clip.id)?.let { put(clip.id, it.peaks); return@forEach }
            val managedPath = clip.managedSourcePath ?: return@forEach
            runCatching {
                val file = mediaStore.resolve(project.id, managedPath)
                val envelope = FileSeekableByteSource(file).use { source -> WaveformEnvelopeBuilder.build(WavPcmDecoder(source), WAVEFORM_POINTS) }
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

    private data class PlaybackReadiness(val ready: Boolean, val sampleRateHz: Int? = null, val reason: String? = null)

    private companion object { const val WAVEFORM_POINTS = 320 }
}
