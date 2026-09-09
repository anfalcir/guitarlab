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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import studio.guitarlab.core.audio.MeterBallisticsPolicy
import studio.guitarlab.core.audio.MeterBallisticsState
import studio.guitarlab.core.audio.TrackMixPolicy
import studio.guitarlab.core.codec.FileSeekableByteSource
import studio.guitarlab.core.codec.WavMetadataReader
import studio.guitarlab.core.codec.WavPcmDecoder
import studio.guitarlab.core.codec.WaveformEnvelopeBuilder
import studio.guitarlab.core.model.AudioClip
import studio.guitarlab.core.model.AudioTrack
import studio.guitarlab.core.model.BuiltInRoles
import studio.guitarlab.core.model.ChannelLayout
import studio.guitarlab.core.model.GuitarProject
import studio.guitarlab.core.model.RoleSource
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
import studio.guitarlab.platform.audio.android.StudioPlaybackMeter
import studio.guitarlab.platform.audio.android.StudioPlaybackRequest
import studio.guitarlab.platform.audio.android.StudioPlaybackRoutingStatus
import studio.guitarlab.platform.audio.android.StudioPlaybackTrackMeter
import studio.guitarlab.platform.audio.android.StudioPlaybackTrackMix

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
    val masterGainDb: Float = 0f,
    val masterMeter: MeterBallisticsState = MeterBallisticsState(),
    val trackMeters: Map<String, MeterBallisticsState> = emptyMap(),
    val masterClipLatched: Boolean = false,
    val trackClipLatched: Set<String> = emptySet(),
    val error: String? = null,
    val importStatus: String? = null,
    val clipStatus: String? = null,
)

private data class TrackMixDraft(val gainDb: Float, val pan: Float)

class StudioViewModel(application: Application) : AndroidViewModel(application) {
    private val rootDirectory = application.filesDir
    private val repository = FileProjectRepository(rootDirectory)
    private val mediaStore = ProjectManagedMediaStore(rootDirectory)
    private val waveformCache = WaveformCacheStore(rootDirectory)
    private val audioRoutingStore = StudioAudioRoutingStore(application)
    private val playbackEngine = AndroidStudioPlaybackEngine()
    private val projectSaveMutex = Mutex()
    private val trackMixDrafts = mutableMapOf<String, TrackMixDraft>()
    private val _state = MutableStateFlow(StudioUiState())
    val state: StateFlow<StudioUiState> = _state.asStateFlow()

    fun load(projectId: String) {
        if (_state.value.project?.id == projectId && !_state.value.loading) return
        playbackEngine.stop()
        trackMixDrafts.clear()
        viewModelScope.launch {
            _state.value = StudioUiState(loading = true)
            runCatching {
                withContext(Dispatchers.IO) {
                    val project = repository.load(projectId) ?: error("Projeto não encontrado: $projectId")
                    project to loadWaveforms(project)
                }
            }.onSuccess { (project, waveforms) ->
                project.tracks.forEach { trackMixDrafts[it.id] = TrackMixDraft(it.gainDb, it.pan) }
                val end = TimelineControlPolicy.projectEndFrame(project)
                _state.value = StudioUiState(
                    loading = false,
                    project = project,
                    waveforms = waveforms,
                    timelineControls = TimelineControlPolicy.normalizedForProject(TimelineControlState(), end),
                    transportEngineReady = playbackReadiness(project).ready,
                    masterGainDb = project.masterGainDb,
                )
            }.onFailure { error ->
                _state.value = StudioUiState(loading = false, error = error.message ?: "Não foi possível abrir o projeto.")
            }
        }
    }

    fun importWav(trackId: String, uri: Uri) {
        val currentState = _state.value
        val current = currentState.project ?: return
        if (!TransportPolicy.timelineEditingEnabled(currentState.transport) || currentState.trimControls != null) return
        if (current.tracks.none { it.id == trackId }) {
            _state.value = currentState.copy(error = "A pista selecionada não existe mais.")
            return
        }
        viewModelScope.launch {
            _state.value = _state.value.copy(importing = true, error = null, importStatus = "Importando áudio…", clipStatus = null)
            runCatching {
                withContext(Dispatchers.IO) {
                    val context = getApplication<Application>()
                    val displayName = queryDisplayName(uri)?.takeIf { it.isNotBlank() } ?: "Áudio importado.wav"
                    val input = context.contentResolver.openInputStream(uri) ?: error("O Android não conseguiu ler o arquivo selecionado.")
                    val asset = input.use { mediaStore.ingest(current.id, displayName, it) }
                    try {
                        val metadata = FileSeekableByteSource(asset.file).use { WavMetadataReader().read(it) }
                        require(metadata.totalFrames > 0) { "O WAV selecionado não contém áudio completo." }
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
                        val saved = saveLatest(current.id) { latest ->
                            latest.copy(clips = latest.clips + clip, updatedAtEpochMs = System.currentTimeMillis())
                        }
                        Triple(saved, metadata, envelope.peaks)
                    } catch (error: Throwable) {
                        mediaStore.discardUncommitted(current.id, asset.relativePath)
                        throw error
                    }
                }
            }.onSuccess { (saved, metadata, peaks) ->
                val imported = saved.clips.last()
                val end = TimelineControlPolicy.projectEndFrame(saved)
                val previous = _state.value
                _state.value = previous.copy(
                    loading = false,
                    importing = false,
                    project = saved,
                    waveforms = previous.waveforms + (imported.id to peaks),
                    timelineControls = TimelineControlPolicy.normalizedForProject(previous.timelineControls, end),
                    transportEngineReady = playbackReadiness(saved).ready,
                    masterGainDb = saved.masterGainDb,
                    importStatus = "Áudio importado • ${metadata.channelCount} canal(is) • ${metadata.sampleRateHz} Hz",
                    error = null,
                )
            }.onFailure { error ->
                _state.value = _state.value.copy(importing = false, error = error.message ?: "Não foi possível importar o WAV.", importStatus = null)
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
        _state.value = current.copy(trimControls = TrimControlPolicy.fromClip(clip), clipStatus = "Modo de corte ativo", error = null)
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
        _state.value = current.copy(trimControls = null, clipStatus = null)
    }

    fun applyTrim() {
        val current = _state.value
        val project = current.project ?: return
        val trim = current.trimControls ?: return
        if (current.importing || current.editingClip || !TransportPolicy.timelineEditingEnabled(current.transport)) return
        viewModelScope.launch {
            _state.value = current.copy(editingClip = true, error = null)
            runCatching {
                saveLatest(project.id) { latest ->
                    ProjectClipEditor.trimClipToTimelineEdges(
                        project = latest,
                        clipId = trim.clipId,
                        timelineStartFrame = trim.startFrame,
                        timelineEndFrame = trim.endFrame,
                        nowEpochMs = System.currentTimeMillis(),
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
                    clipStatus = "Corte aplicado",
                )
            }.onFailure { error ->
                _state.value = _state.value.copy(editingClip = false, error = error.message ?: "Não foi possível aplicar o corte.")
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
                    _state.value = current.copy(error = "Aplique ou cancele o corte antes de reproduzir.")
                    return
                }
                startPlayback(current)
            }
        }
    }

    fun startRecording() {
        val current = _state.value
        _state.value = current.copy(masterClipLatched = false, trackClipLatched = emptySet())
    }

    fun toggleClipMuted(clipId: String) {
        editClip("Estado do clipe atualizado") { current ->
            val clip = current.clips.firstOrNull { it.id == clipId } ?: error("Clipe não encontrado: $clipId")
            ProjectClipEditor.setClipMuted(current, clipId, !clip.muted, System.currentTimeMillis())
        }
    }

    fun removeClip(clipId: String) = editClip("Clipe removido") { current ->
        ProjectClipEditor.removeClip(current, clipId, System.currentTimeMillis())
    }

    fun previewTrackGainDb(trackId: String, gainDb: Float) {
        val track = _state.value.project?.tracks?.firstOrNull { it.id == trackId } ?: return
        val previous = trackMixDrafts[trackId] ?: TrackMixDraft(track.gainDb, track.pan)
        val draft = previous.copy(gainDb = gainDb.coerceIn(-60f, 12f))
        trackMixDrafts[trackId] = draft
        playbackEngine.setTrackMix(trackId, draft.gainDb, draft.pan)
    }

    fun commitTrackGainDb(trackId: String, gainDb: Float) {
        commitTrackMix(trackId) { it.copy(gainDb = gainDb.coerceIn(-60f, 12f)) }
    }

    fun previewTrackPan(trackId: String, pan: Float) {
        val track = _state.value.project?.tracks?.firstOrNull { it.id == trackId } ?: return
        val previous = trackMixDrafts[trackId] ?: TrackMixDraft(track.gainDb, track.pan)
        val draft = previous.copy(pan = pan.coerceIn(-1f, 1f))
        trackMixDrafts[trackId] = draft
        playbackEngine.setTrackMix(trackId, draft.gainDb, draft.pan)
    }

    fun commitTrackPan(trackId: String, pan: Float) {
        commitTrackMix(trackId) { it.copy(pan = pan.coerceIn(-1f, 1f)) }
    }

    fun toggleTrackMuted(trackId: String) {
        editTrackStructural("Mute da pista atualizado", trackId) { it.copy(muted = !it.muted) }
    }

    fun toggleTrackSolo(trackId: String) {
        editTrackStructural("Solo da pista atualizado", trackId) { it.copy(solo = !it.solo) }
    }

    fun toggleTrackArmed(trackId: String) {
        editTrackStructural("Armar gravação atualizado", trackId) { it.copy(armed = !it.armed) }
    }

    fun updateTrackProperties(trackId: String, name: String, colorIndex: Int) {
        val normalizedName = name.trim()
        if (normalizedName.isBlank()) {
            _state.value = _state.value.copy(error = "O nome da pista não pode ficar vazio.")
            return
        }
        editTrackStructural("Pista atualizada", trackId) {
            it.copy(name = normalizedName, colorIndex = colorIndex.coerceIn(0, TRACK_COLOR_COUNT - 1))
        }
    }

    fun moveTrack(trackId: String, direction: Int) {
        if (direction == 0) return
        val current = _state.value
        val project = current.project ?: return
        if (!structuralEditingAllowed(current)) return
        viewModelScope.launch {
            runCatching {
                saveLatest(project.id) { latest ->
                    val sorted = latest.tracks.sortedBy { it.order }.toMutableList()
                    val index = sorted.indexOfFirst { it.id == trackId }
                    if (index < 0) return@saveLatest latest
                    val target = (index + direction.sign()).coerceIn(0, sorted.lastIndex)
                    if (target == index) return@saveLatest latest
                    val swap = sorted[index]
                    sorted[index] = sorted[target]
                    sorted[target] = swap
                    latest.copy(
                        tracks = sorted.mapIndexed { order, track -> track.copy(order = order) },
                        updatedAtEpochMs = System.currentTimeMillis(),
                    )
                }
            }.onSuccess(::applySavedProject)
                .onFailure { error -> _state.value = _state.value.copy(error = error.message ?: "Não foi possível reordenar a pista.") }
        }
    }

    fun addTrack() {
        val current = _state.value
        val project = current.project ?: return
        if (!structuralEditingAllowed(current)) return
        viewModelScope.launch {
            runCatching {
                saveLatest(project.id) { latest ->
                    val nextOrder = (latest.tracks.maxOfOrNull { it.order } ?: -1) + 1
                    val number = latest.tracks.size + 1
                    val newTrack = AudioTrack(
                        id = UUID.randomUUID().toString(),
                        name = "Nova pista $number",
                        roleId = BuiltInRoles.GENERIC,
                        roleSource = RoleSource.USER,
                        channelLayout = ChannelLayout.MONO,
                        order = nextOrder,
                        colorIndex = nextOrder % TRACK_COLOR_COUNT,
                    )
                    latest.copy(
                        tracks = latest.tracks + newTrack,
                        updatedAtEpochMs = System.currentTimeMillis(),
                    )
                }
            }.onSuccess { saved ->
                saved.tracks.forEach { trackMixDrafts[it.id] = TrackMixDraft(it.gainDb, it.pan) }
                applySavedProject(saved, "Nova pista adicionada")
            }.onFailure { error -> _state.value = _state.value.copy(error = error.message ?: "Não foi possível adicionar a pista.") }
        }
    }

    fun deleteTrack(trackId: String) {
        val current = _state.value
        val project = current.project ?: return
        if (!structuralEditingAllowed(current)) return
        if (project.clips.any { it.trackId == trackId }) {
            _state.value = current.copy(error = "Remova os clipes desta pista antes de excluí-la.")
            return
        }
        viewModelScope.launch {
            runCatching {
                saveLatest(project.id) { latest ->
                    require(latest.clips.none { it.trackId == trackId }) { "A pista ainda contém clipes." }
                    latest.copy(
                        tracks = latest.tracks.filterNot { it.id == trackId }
                            .sortedBy { it.order }
                            .mapIndexed { order, track -> track.copy(order = order) },
                        updatedAtEpochMs = System.currentTimeMillis(),
                    )
                }
            }.onSuccess { saved ->
                trackMixDrafts.remove(trackId)
                applySavedProject(saved, "Pista excluída")
            }.onFailure { error -> _state.value = _state.value.copy(error = error.message ?: "Não foi possível excluir a pista.") }
        }
    }

    fun previewMasterGainDb(gainDb: Float) {
        playbackEngine.setMasterGainDb(gainDb.coerceIn(-60f, 12f))
    }

    fun commitMasterGainDb(gainDb: Float) {
        val currentState = _state.value
        val project = currentState.project ?: return
        if (currentState.importing || currentState.editingClip || currentState.trimControls != null) return
        val normalized = gainDb.coerceIn(-60f, 12f)
        viewModelScope.launch {
            runCatching {
                saveLatest(project.id) { latest ->
                    latest.copy(masterGainDb = normalized, updatedAtEpochMs = System.currentTimeMillis())
                }
            }.onSuccess { saved ->
                val state = _state.value
                _state.value = state.copy(project = saved, masterGainDb = saved.masterGainDb, error = null)
            }.onFailure { error ->
                _state.value = _state.value.copy(error = error.message ?: "Não foi possível atualizar o Master.")
            }
        }
    }

    fun clearMasterClipIndicator() {
        _state.value = _state.value.copy(masterClipLatched = false)
    }

    fun clearTrackClipIndicator(trackId: String) {
        _state.value = _state.value.copy(trackClipLatched = _state.value.trackClipLatched - trackId)
    }

    override fun onCleared() {
        playbackEngine.close()
        super.onCleared()
    }

    private fun startPlayback(current: StudioUiState) {
        val project = current.project ?: return
        val readiness = playbackReadiness(project)
        if (!readiness.ready || readiness.sampleRateHz == null) {
            _state.value = current.copy(error = readiness.reason ?: "O projeto não está pronto para reprodução.")
            return
        }
        val end = TimelineControlPolicy.projectEndFrame(project)
        val requestedStart = if (!current.transport.loopEnabled && current.timelineControls.playheadFrame >= end) 0L else current.timelineControls.playheadFrame
        val anySolo = project.tracks.any { it.solo }
        val tracksById = project.tracks.associateBy { it.id }
        val selectedOutputSignature = audioRoutingStore.selectedOutputSignature()
        val preferredOutput = audioRoutingStore.resolveSelectedOutputDevice()
        val audibleTrackIds = project.tracks.filter { TrackMixPolicy.isAudible(it.muted, it.solo, anySolo) }.map { it.id }.toSet()
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
                masterGainDb = project.masterGainDb,
                trackMixes = project.tracks.filter { it.id in audibleTrackIds }.map { track ->
                    StudioPlaybackTrackMix(trackId = track.id, gainDb = track.gainDb, pan = track.pan)
                },
                clips = project.clips.mapNotNull { clip ->
                    val sourceTrack = tracksById[clip.trackId] ?: return@mapNotNull null
                    if (clip.muted || sourceTrack.id !in audibleTrackIds) return@mapNotNull null
                    val managedPath = requireNotNull(clip.managedSourcePath) { "O clipe '${clip.name}' não está no armazenamento do projeto." }
                    StudioPlaybackClip(
                        file = mediaStore.resolve(project.id, managedPath),
                        trackId = sourceTrack.id,
                        timelineStartFrame = clip.startFrame,
                        sourceStartFrame = clip.sourceStartFrame,
                        lengthFrames = clip.lengthFrames,
                        gainDb = clip.gainDb,
                        pan = 0f,
                        muted = false,
                    )
                },
            )
        }.getOrElse { error ->
            _state.value = current.copy(error = error.message ?: "Não foi possível preparar o áudio para reprodução.")
            return
        }

        project.tracks.forEach { trackMixDrafts[it.id] = TrackMixDraft(it.gainDb, it.pan) }
        _state.value = current.copy(
            timelineControls = TimelineControlPolicy.movePlayhead(current.timelineControls, requestedStart, end),
            transport = current.transport.copy(mode = TransportMode.PLAYING),
            masterMeter = MeterBallisticsPolicy.reset(),
            trackMeters = emptyMap(),
            masterClipLatched = false,
            trackClipLatched = emptySet(),
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
                            masterMeter = MeterBallisticsPolicy.reset(),
                            trackMeters = emptyMap(),
                        )
                    }
                }

                override fun onError(message: String) {
                    viewModelScope.launch {
                        val state = _state.value
                        _state.value = state.copy(
                            transport = state.transport.copy(mode = TransportMode.STOPPED),
                            masterMeter = MeterBallisticsPolicy.reset(),
                            trackMeters = emptyMap(),
                            error = message,
                        )
                    }
                }

                override fun onRouting(status: StudioPlaybackRoutingStatus) {
                    viewModelScope.launch {
                        val state = _state.value
                        val routeStatus = when {
                            status.usingPreferredOutput -> "Saída • ${status.deviceLabel ?: "dispositivo preferido"}"
                            status.fellBackToAuto -> "Saída preferida indisponível • usando rota automática"
                            else -> null
                        }
                        if (routeStatus != null && state.transport.mode == TransportMode.PLAYING) {
                            _state.value = state.copy(clipStatus = routeStatus)
                        }
                    }
                }

                override fun onMasterMeter(meter: StudioPlaybackMeter) {
                    viewModelScope.launch {
                        val state = _state.value
                        if (state.transport.mode == TransportMode.PLAYING) {
                            _state.value = state.copy(
                                masterMeter = MeterBallisticsPolicy.update(
                                    previous = state.masterMeter,
                                    rawPeak = meter.peak,
                                    rawRms = meter.rms,
                                    nowMs = System.currentTimeMillis(),
                                ),
                                masterClipLatched = state.masterClipLatched || meter.peak > 1f,
                            )
                        }
                    }
                }

                override fun onTrackMeters(meters: List<StudioPlaybackTrackMeter>) {
                    viewModelScope.launch {
                        val state = _state.value
                        if (state.transport.mode != TransportMode.PLAYING) return@launch
                        val now = System.currentTimeMillis()
                        val incoming = meters.associateBy { it.trackId }
                        val trackIds = state.project?.tracks?.map { it.id }.orEmpty()
                        val next = buildMap {
                            trackIds.forEach { trackId ->
                                val raw = incoming[trackId]?.meter ?: StudioPlaybackMeter(0f, 0f)
                                put(
                                    trackId,
                                    MeterBallisticsPolicy.update(
                                        previous = state.trackMeters[trackId] ?: MeterBallisticsState(),
                                        rawPeak = raw.peak,
                                        rawRms = raw.rms,
                                        nowMs = now,
                                    )
                                )
                            }
                        }
                        val newlyClipped = meters.asSequence().filter { it.meter.peak > 1f }.map { it.trackId }.toSet()
                        _state.value = state.copy(
                            trackMeters = next,
                            trackClipLatched = state.trackClipLatched + newlyClipped,
                        )
                    }
                }
            })
        }.onFailure { error ->
            _state.value = _state.value.copy(
                transport = _state.value.transport.copy(mode = TransportMode.STOPPED),
                masterMeter = MeterBallisticsPolicy.reset(),
                trackMeters = emptyMap(),
                error = error.message ?: "Não foi possível iniciar a reprodução.",
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
        if (!structuralEditingAllowed(currentState)) return
        viewModelScope.launch {
            _state.value = _state.value.copy(editingClip = true, error = null, clipStatus = null)
            runCatching { saveLatest(current.id, transform) }
                .onSuccess { saved ->
                    val validClipIds = saved.clips.map { it.id }.toSet()
                    val end = TimelineControlPolicy.projectEndFrame(saved)
                    _state.value = _state.value.copy(
                        editingClip = false,
                        project = saved,
                        waveforms = _state.value.waveforms.filterKeys { it in validClipIds },
                        timelineControls = TimelineControlPolicy.normalizedForProject(_state.value.timelineControls, end),
                        transportEngineReady = playbackReadiness(saved).ready,
                        masterGainDb = saved.masterGainDb,
                        clipStatus = status,
                    )
                }
                .onFailure { error -> _state.value = _state.value.copy(editingClip = false, error = error.message ?: "Não foi possível editar o clipe.") }
        }
    }

    private fun editTrackStructural(status: String, trackId: String, transform: (AudioTrack) -> AudioTrack) {
        val currentState = _state.value
        val project = currentState.project ?: return
        if (!structuralEditingAllowed(currentState)) return
        viewModelScope.launch {
            runCatching {
                saveLatest(project.id) { latest ->
                    val target = latest.tracks.firstOrNull { it.id == trackId } ?: return@saveLatest latest
                    val updated = transform(target)
                    latest.copy(
                        tracks = latest.tracks.map { if (it.id == trackId) updated else it },
                        updatedAtEpochMs = System.currentTimeMillis(),
                    )
                }
            }.onSuccess { saved ->
                val track = saved.tracks.firstOrNull { it.id == trackId }
                if (track != null) trackMixDrafts[track.id] = TrackMixDraft(track.gainDb, track.pan)
                applySavedProject(saved, status)
            }.onFailure { error -> _state.value = _state.value.copy(error = error.message ?: "Não foi possível atualizar a pista.") }
        }
    }

    private fun commitTrackMix(trackId: String, transform: (AudioTrack) -> AudioTrack) {
        val currentState = _state.value
        val project = currentState.project ?: return
        if (currentState.importing || currentState.editingClip || currentState.trimControls != null) return
        viewModelScope.launch {
            runCatching {
                saveLatest(project.id) { latest ->
                    val target = latest.tracks.firstOrNull { it.id == trackId } ?: return@saveLatest latest
                    val updated = transform(target)
                    latest.copy(
                        tracks = latest.tracks.map { if (it.id == trackId) updated else it },
                        updatedAtEpochMs = System.currentTimeMillis(),
                    )
                }
            }.onSuccess { saved ->
                val track = saved.tracks.firstOrNull { it.id == trackId }
                if (track != null) {
                    trackMixDrafts[track.id] = TrackMixDraft(track.gainDb, track.pan)
                    playbackEngine.setTrackMix(track.id, track.gainDb, track.pan)
                }
                val state = _state.value
                _state.value = state.copy(
                    project = saved,
                    transportEngineReady = playbackReadiness(saved).ready,
                    masterGainDb = saved.masterGainDb,
                    error = null,
                )
            }.onFailure { error -> _state.value = _state.value.copy(error = error.message ?: "Não foi possível salvar a mixagem da pista.") }
        }
    }

    private fun applySavedProject(saved: GuitarProject, status: String? = null) {
        val state = _state.value
        _state.value = state.copy(
            project = saved,
            transportEngineReady = playbackReadiness(saved).ready,
            masterGainDb = saved.masterGainDb,
            clipStatus = status,
            error = null,
        )
    }

    private fun structuralEditingAllowed(state: StudioUiState): Boolean =
        !state.importing && !state.editingClip && state.trimControls == null && TransportPolicy.timelineEditingEnabled(state.transport)

    private suspend fun saveLatest(projectId: String, transform: (GuitarProject) -> GuitarProject): GuitarProject =
        projectSaveMutex.withLock {
            withContext(Dispatchers.IO) {
                val latest = repository.load(projectId) ?: error("Projeto não encontrado: $projectId")
                repository.save(transform(latest))
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
        if (audible.isEmpty()) return PlaybackReadiness(false, reason = "Adicione ou desative o mute de uma pista com áudio para reproduzir.")
        if (audible.any { it.managedSourcePath.isNullOrBlank() || it.sourceFormat != "WAV" }) {
            return PlaybackReadiness(false, reason = "A reprodução atual exige clipes WAV armazenados no projeto.")
        }
        if (audible.any { it.sourceChannelCount !in 1..2 }) {
            return PlaybackReadiness(false, reason = "A reprodução atual suporta fontes mono ou estéreo.")
        }
        val firstRate = audible.first().sourceSampleRateHz ?: return PlaybackReadiness(false, reason = "A taxa de amostragem do áudio é desconhecida.")
        val projectRate = project.sampleRate.fixedHz ?: firstRate
        if (audible.any { it.sourceSampleRateHz != projectRate }) {
            return PlaybackReadiness(false, reason = "Há clipes com taxa de amostragem diferente da taxa do projeto.")
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

    private fun Int.sign(): Int = when {
        this > 0 -> 1
        this < 0 -> -1
        else -> 0
    }

    private data class PlaybackReadiness(val ready: Boolean, val sampleRateHz: Int? = null, val reason: String? = null)

    private companion object {
        const val WAVEFORM_POINTS = 320
        const val TRACK_COLOR_COUNT = 20
    }
}
