package studio.guitarlab.app.ui

import android.app.Application
import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.OpenableColumns
import java.io.File
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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
import studio.guitarlab.core.codec.AudioImportFormat
import studio.guitarlab.core.codec.AudioImportFormatPolicy
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
import studio.guitarlab.core.model.TrackNamePolicy
import studio.guitarlab.core.project.FileProjectRepository
import studio.guitarlab.core.project.ProjectClipEditor
import studio.guitarlab.core.project.ProjectTrackEditor
import studio.guitarlab.core.project.ProjectHistory
import studio.guitarlab.core.project.ProjectManagedMediaStore
import studio.guitarlab.core.project.ProjectBundleWriter
import studio.guitarlab.core.project.ProjectRecordingMediaStore
import studio.guitarlab.core.project.RecordingMediaTransaction
import studio.guitarlab.core.project.RecordedTakeMetadata
import studio.guitarlab.core.project.RecordedTakeProjectIntegrator
import studio.guitarlab.core.project.RecordingSessionPhase
import studio.guitarlab.core.project.RecordingSessionPolicy
import studio.guitarlab.core.project.RecordingSessionState
import studio.guitarlab.core.project.RecordingTargetPolicy
import studio.guitarlab.core.project.TimelineControlPolicy
import studio.guitarlab.core.project.TimelineControlState
import studio.guitarlab.core.project.TransportMode
import studio.guitarlab.core.project.TransportPolicy
import studio.guitarlab.core.project.TransportState
import studio.guitarlab.core.project.TrimControlPolicy
import studio.guitarlab.core.project.TrimControlState
import studio.guitarlab.core.project.WaveformCacheStore
import studio.guitarlab.platform.audio.android.AndroidStudioPlaybackEngine
import studio.guitarlab.platform.audio.android.AndroidStudioRecordingEngine
import studio.guitarlab.platform.audio.android.StudioRecordingConfig
import studio.guitarlab.platform.audio.android.StudioRecordingListener
import studio.guitarlab.platform.audio.android.StudioRecordingRequest
import studio.guitarlab.platform.audio.android.StudioRecordingResult
import studio.guitarlab.platform.audio.android.StudioPlaybackClip
import studio.guitarlab.platform.audio.android.StudioPlaybackListener
import studio.guitarlab.platform.audio.android.StudioPlaybackMeter
import studio.guitarlab.platform.audio.android.StudioPlaybackRequest
import studio.guitarlab.platform.audio.android.StudioPlaybackRoutingStatus
import studio.guitarlab.platform.audio.android.StudioPlaybackTrackMeter
import studio.guitarlab.platform.audio.android.StudioPlaybackTrackMix
import studio.guitarlab.platform.audio.android.StudioMasterRenderClip
import studio.guitarlab.platform.audio.android.StudioMasterRenderRequest
import studio.guitarlab.platform.audio.android.StudioMasterRenderTrack
import studio.guitarlab.platform.audio.android.StudioMasterRenderer
import studio.guitarlab.platform.codec.android.AndroidAudioImportTranscoder
import studio.guitarlab.platform.codec.android.AndroidMasterAudioEncoder
import studio.guitarlab.platform.codec.android.MasterExportFormat

data class StudioUiState(
    val loading: Boolean = true,
    val importing: Boolean = false,
    val exporting: Boolean = false,
    val editingClip: Boolean = false,
    val historyBusy: Boolean = false,
    val project: GuitarProject? = null,
    val waveforms: Map<String, List<Float>> = emptyMap(),
    val timelineControls: TimelineControlState = TimelineControlState(),
    val trimControls: TrimControlState? = null,
    val transport: TransportState = TransportState(),
    val transportEngineReady: Boolean = false,
    val recordingSession: RecordingSessionState = RecordingSessionState(),
    val recordingConfig: StudioRecordingConfig? = null,
    val recordingPeak: Float = 0f,
    val recordingRms: Float = 0f,
    val masterGainDb: Float = 0f,
    val masterMeter: MeterBallisticsState = MeterBallisticsState(),
    val trackMeters: Map<String, MeterBallisticsState> = emptyMap(),
    val masterClipLatched: Boolean = false,
    val trackClipLatched: Set<String> = emptySet(),
    val canUndo: Boolean = false,
    val canRedo: Boolean = false,
    val error: String? = null,
    val importStatus: String? = null,
    val exportStatus: String? = null,
    val clipStatus: String? = null,
)

private data class TrackMixDraft(val gainDb: Float, val pan: Float)

class StudioViewModel(application: Application) : AndroidViewModel(application) {
    private val rootDirectory = application.filesDir
    private val repository = FileProjectRepository(rootDirectory)
    private val mediaStore = ProjectManagedMediaStore(rootDirectory)
    private val recordingMediaStore = ProjectRecordingMediaStore(rootDirectory)
    private val waveformCache = WaveformCacheStore(rootDirectory)
    private val audioRoutingStore = StudioAudioRoutingStore(application)
    private val playbackEngine = AndroidStudioPlaybackEngine()
    private val recordingEngine = AndroidStudioRecordingEngine(application)
    private var recordingTransaction: RecordingMediaTransaction? = null
    private var countdownJob: Job? = null
    private val projectSaveMutex = Mutex()
    private val projectHistory = ProjectHistory()
    private val trackMixDrafts = mutableMapOf<String, TrackMixDraft>()
    private val _state = MutableStateFlow(StudioUiState())
    val state: StateFlow<StudioUiState> = _state.asStateFlow()

    fun load(projectId: String) {
        if (_state.value.project?.id == projectId && !_state.value.loading) return
        playbackEngine.stop()
        projectHistory.clear()
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

    fun importAudio(trackId: String, uri: Uri) {
        val currentState = _state.value
        val current = currentState.project ?: return
        if (!TransportPolicy.timelineEditingEnabled(currentState.transport) || currentState.trimControls != null || currentState.historyBusy || currentState.exporting) return
        if (current.tracks.none { it.id == trackId }) {
            _state.value = currentState.copy(error = "A pista selecionada não existe mais.")
            return
        }
        viewModelScope.launch {
            _state.value = _state.value.copy(importing = true, error = null, importStatus = "Importando áudio…", clipStatus = null)
            runCatching {
                withContext(Dispatchers.IO) {
                    val context = getApplication<Application>()
                    val displayName = queryDisplayName(uri)?.takeIf { it.isNotBlank() } ?: "Áudio importado"
                    val mimeType = context.contentResolver.getType(uri)
                    val originalFormat = AudioImportFormatPolicy.detect(displayName, mimeType)
                        ?: error("Formato não suportado. Use ${AudioImportFormatPolicy.supportedExtensionsDescription}.")
                    val input = context.contentResolver.openInputStream(uri) ?: error("O Android não conseguiu ler o arquivo selecionado.")
                    val sourceAsset = input.use { mediaStore.ingest(current.id, displayName, it) }
                    var proxyPath: String? = null
                    try {
                        val editingFile = if (originalFormat == AudioImportFormat.WAV_PCM) {
                            sourceAsset.file
                        } else {
                            AndroidAudioImportTranscoder.prepare(context, uri, originalFormat).use { prepared ->
                                val proxyName = AudioImportFormatPolicy.managedWavName(displayName)
                                val proxy = prepared.wavFile.inputStream().buffered().use { mediaStore.ingestEditProxy(current.id, proxyName, it) }
                                proxyPath = proxy.relativePath
                                proxy.file
                            }
                        }
                        val metadata = FileSeekableByteSource(editingFile).use { WavMetadataReader().read(it) }
                        require(metadata.totalFrames > 0) { "O arquivo selecionado não contém áudio completo." }
                        val clipId = UUID.randomUUID().toString()
                        val envelope = FileSeekableByteSource(editingFile).use { source ->
                            WaveformEnvelopeBuilder.build(WavPcmDecoder(source), WAVEFORM_POINTS)
                        }
                        waveformCache.write(current.id, clipId, envelope)
                        val sourceBits = if (originalFormat == AudioImportFormat.WAV_PCM) metadata.bitsPerSample else null
                        val sourceEncoding = if (originalFormat == AudioImportFormat.WAV_PCM) metadata.sampleEncoding.name else "COMPRESSED"
                        val clip = AudioClip(
                            id = clipId,
                            trackId = trackId,
                            name = displayName,
                            sourceUri = "managed://${sourceAsset.relativePath}",
                            managedSourcePath = sourceAsset.relativePath,
                            managedEditProxyPath = proxyPath,
                            originUri = uri.toString(),
                            startFrame = 0,
                            sourceStartFrame = 0,
                            lengthFrames = metadata.totalFrames,
                            sourceTotalFrames = metadata.totalFrames,
                            sourceFormat = originalFormat.displayName,
                            sourceSampleRateHz = metadata.sampleRateHz,
                            sourceChannelCount = metadata.channelCount,
                            sourceBitsPerSample = sourceBits,
                            sourceEncoding = sourceEncoding,
                        )
                        val saved = saveLatest(current.id) { latest ->
                            latest.copy(clips = latest.clips + clip, updatedAtEpochMs = System.currentTimeMillis())
                        }
                        Triple(saved, metadata, envelope.peaks)
                    } catch (error: Throwable) {
                        proxyPath?.let { mediaStore.discardUncommitted(current.id, it) }
                        mediaStore.discardUncommitted(current.id, sourceAsset.relativePath)
                        throw error
                    }
                }
            }.onSuccess { (saved, metadata, peaks) ->
                val imported = saved.clips.last()
                val endFrame = TimelineControlPolicy.projectEndFrame(saved)
                val previous = _state.value
                _state.value = previous.copy(
                    loading = false,
                    importing = false,
                    project = saved,
                    waveforms = previous.waveforms + (imported.id to peaks),
                    timelineControls = TimelineControlPolicy.normalizedForProject(previous.timelineControls, endFrame),
                    transportEngineReady = playbackReadiness(saved).ready,
                    masterGainDb = saved.masterGainDb,
                    canUndo = projectHistory.canUndo,
                    canRedo = projectHistory.canRedo,
                    importStatus = "${imported.sourceFormat} importado • ${metadata.channelCount} canal(is) • ${metadata.sampleRateHz} Hz",
                    error = null,
                )
            }.onFailure { error ->
                _state.value = _state.value.copy(importing = false, error = error.message ?: "Não foi possível importar o áudio.", importStatus = null)
            }
        }
    }

    @Deprecated("Use importAudio")
    fun importWav(trackId: String, uri: Uri) = importAudio(trackId, uri)

    fun setPlayheadFrame(frame: Long) = editTimelineMarker { state, end -> TimelineControlPolicy.movePlayhead(state, frame, end) }
    fun setLoopStartFrame(frame: Long) = editTimelineMarker { state, end -> TimelineControlPolicy.moveLoopStart(state, frame, end) }
    fun setLoopEndFrame(frame: Long) = editTimelineMarker { state, end -> TimelineControlPolicy.moveLoopEnd(state, frame, end) }

    fun beginTrim(clipId: String) {
        val current = _state.value
        val project = current.project ?: return
        if (current.importing || current.editingClip || current.historyBusy || !TransportPolicy.timelineEditingEnabled(current.transport)) return
        val clip = project.clips.firstOrNull { it.id == clipId } ?: return
        _state.value = current.copy(trimControls = TrimControlPolicy.fromClip(clip), clipStatus = "Modo de corte ativo", error = null)
    }

    fun setTrimStartFrame(frame: Long) {
        val current = _state.value
        val trim = current.trimControls ?: return
        if (!TransportPolicy.timelineEditingEnabled(current.transport) || current.historyBusy) return
        val clip = current.project?.clips?.firstOrNull { it.id == trim.clipId } ?: return
        _state.value = current.copy(trimControls = TrimControlPolicy.moveStart(trim, frame, clip))
    }

    fun setTrimEndFrame(frame: Long) {
        val current = _state.value
        val trim = current.trimControls ?: return
        if (!TransportPolicy.timelineEditingEnabled(current.transport) || current.historyBusy) return
        val clip = current.project?.clips?.firstOrNull { it.id == trim.clipId } ?: return
        _state.value = current.copy(trimControls = TrimControlPolicy.moveEnd(trim, frame, clip))
    }

    fun cancelTrim() {
        val current = _state.value
        if (!TransportPolicy.timelineEditingEnabled(current.transport) || current.historyBusy) return
        _state.value = current.copy(trimControls = null, clipStatus = null)
    }

    fun applyTrim() {
        val current = _state.value
        val project = current.project ?: return
        val trim = current.trimControls ?: return
        if (current.importing || current.editingClip || current.historyBusy || !TransportPolicy.timelineEditingEnabled(current.transport)) return
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
                    canUndo = projectHistory.canUndo,
                    canRedo = projectHistory.canRedo,
                    clipStatus = "Corte aplicado",
                )
            }.onFailure { error ->
                _state.value = _state.value.copy(editingClip = false, error = error.message ?: "Não foi possível aplicar o corte.")
            }
        }
    }

    fun returnToStart() {
        if (!TransportPolicy.timelineEditingEnabled(_state.value.transport) || _state.value.historyBusy) return
        setPlayheadFrame(0L)
    }

    fun toggleLoop() {
        if (_state.value.trimControls != null || _state.value.historyBusy) return
        val current = _state.value
        val project = current.project ?: return
        val projectEnd = TimelineControlPolicy.projectEndFrame(project)
        val controls = current.timelineControls
        val firstActivation = !current.transport.loopEnabled && controls.loopStartFrame == 0L && controls.loopEndFrame >= projectEnd
        _state.value = current.copy(
            transport = TransportPolicy.toggleLoop(current.transport),
            timelineControls = if (firstActivation) controls.copy(loopEndFrame = (projectEnd / 10L).coerceAtLeast(1L)) else controls,
        )
    }

    fun undo() {
        val currentState = _state.value
        val currentProject = currentState.project ?: return
        if (!structuralEditingAllowed(currentState) || !projectHistory.canUndo) return
        val target = projectHistory.undo(currentProject) ?: return
        _state.value = currentState.copy(historyBusy = true, error = null)
        viewModelScope.launch {
            runCatching { persistHistorySnapshot(target) }
                .onSuccess { (saved, waveforms) -> applyHistorySnapshot(saved, waveforms, "Alteração desfeita") }
                .onFailure { error ->
                    projectHistory.redo(target)
                    _state.value = _state.value.copy(
                        historyBusy = false,
                        canUndo = projectHistory.canUndo,
                        canRedo = projectHistory.canRedo,
                        error = error.message ?: "Não foi possível desfazer a alteração.",
                    )
                }
        }
    }

    fun redo() {
        val currentState = _state.value
        val currentProject = currentState.project ?: return
        if (!structuralEditingAllowed(currentState) || !projectHistory.canRedo) return
        val target = projectHistory.redo(currentProject) ?: return
        _state.value = currentState.copy(historyBusy = true, error = null)
        viewModelScope.launch {
            runCatching { persistHistorySnapshot(target) }
                .onSuccess { (saved, waveforms) -> applyHistorySnapshot(saved, waveforms, "Alteração refeita") }
                .onFailure { error ->
                    projectHistory.undo(target)
                    _state.value = _state.value.copy(
                        historyBusy = false,
                        canUndo = projectHistory.canUndo,
                        canRedo = projectHistory.canRedo,
                        error = error.message ?: "Não foi possível refazer a alteração.",
                    )
                }
        }
    }

    fun togglePlayStop() {
        val current = _state.value
        if (current.historyBusy) return
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
        when (current.recordingSession.phase) {
            RecordingSessionPhase.COUNTDOWN -> cancelRecordingCountdown()
            RecordingSessionPhase.CAPTURING -> stopRecording()
            RecordingSessionPhase.FINALIZING -> Unit
            RecordingSessionPhase.IDLE -> beginRecordingCountdown(current)
        }
    }

    fun onRecordPermissionResult(granted: Boolean) {
        if (granted) startRecording() else {
            _state.value = _state.value.copy(error = "A permissão do microfone é necessária para gravar.")
        }
    }

    private fun beginRecordingCountdown(current: StudioUiState) {
        val project = current.project ?: return
        if (!structuralEditingAllowed(current)) return
        if (getApplication<Application>().checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            _state.value = current.copy(error = "Autorize o microfone para iniciar a gravação.")
            return
        }
        val session = runCatching {
            RecordingTargetPolicy.resolve(project)
            RecordingSessionPolicy.begin(project, current.timelineControls.playheadFrame)
        }.getOrElse { error ->
            _state.value = current.copy(error = error.message ?: "Não foi possível preparar a gravação.")
            return
        }
        countdownJob?.cancel()
        _state.value = current.copy(
            recordingSession = session,
            masterClipLatched = false,
            trackClipLatched = emptySet(),
            recordingPeak = 0f,
            recordingRms = 0f,
            error = null,
            clipStatus = "Gravação inicia em ${session.countdownSecondsRemaining}",
        )
        countdownJob = viewModelScope.launch {
            while (_state.value.recordingSession.phase == RecordingSessionPhase.COUNTDOWN &&
                _state.value.recordingSession.countdownSecondsRemaining > 0
            ) {
                delay(1_000)
                val state = _state.value
                if (state.recordingSession.phase != RecordingSessionPhase.COUNTDOWN) return@launch
                val ticked = RecordingSessionPolicy.tickCountdown(state.recordingSession)
                _state.value = state.copy(
                    recordingSession = ticked,
                    clipStatus = if (ticked.countdownSecondsRemaining > 0) "Gravação inicia em ${ticked.countdownSecondsRemaining}" else "Iniciando gravação…",
                )
            }
            if (_state.value.recordingSession.readyToOpenCapture) openRecordingCapture()
        }
    }

    private fun cancelRecordingCountdown() {
        countdownJob?.cancel()
        countdownJob = null
        val state = _state.value
        if (state.recordingSession.phase != RecordingSessionPhase.COUNTDOWN) return
        _state.value = state.copy(
            recordingSession = RecordingSessionPolicy.cancelCountdown(state.recordingSession),
            clipStatus = "Gravação cancelada",
        )
    }

    private fun openRecordingCapture() {
        val state = _state.value
        val project = state.project ?: return
        val session = state.recordingSession
        if (!session.readyToOpenCapture) return
        val target = runCatching { RecordingTargetPolicy.resolve(project) }.getOrElse { error ->
            resetRecording(error.message ?: "A pista armada mudou durante a contagem.")
            return
        }
        if (target.trackId != session.targetTrackId ||
            getApplication<Application>().checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED
        ) {
            resetRecording("A permissão ou a pista armada mudou durante a contagem.")
            return
        }
        val selectedInputSignature = audioRoutingStore.selectedInputSignature()
        val selectedInput = audioRoutingStore.resolveSelectedInputDevice()
        if (!selectedInputSignature.isNullOrBlank() && selectedInput == null) {
            resetRecording("A entrada selecionada não está disponível.")
            return
        }
        val transaction = runCatching {
            recordingMediaStore.cleanupInterrupted(project.id)
            recordingMediaStore.begin(project.id, "take-${System.currentTimeMillis()}.wav")
        }.getOrElse { error ->
            resetRecording(error.message ?: "Não foi possível abrir a transação da gravação.")
            return
        }
        recordingTransaction = transaction
        val request = StudioRecordingRequest(
            temporaryFile = transaction.temporaryFile,
            preferredSampleRateHz = target.preferredSampleRateHz,
            preferredInputDevice = selectedInput,
            preferredInputRequested = !selectedInputSignature.isNullOrBlank(),
            monitoringMode = audioRoutingStore.monitoringMode(),
            preferredOutputDevice = audioRoutingStore.resolveSelectedOutputDevice(),
        )
        runCatching {
            recordingEngine.start(request, recordingListener)
        }.onFailure { error ->
            recordingMediaStore.discard(transaction)
            recordingTransaction = null
            resetRecording(error.message ?: "Não foi possível iniciar a captura.")
        }
    }

    private fun stopRecording() {
        val state = _state.value
        if (state.recordingSession.phase != RecordingSessionPhase.CAPTURING) return
        _state.value = state.copy(
            recordingSession = RecordingSessionPolicy.beginFinalizing(state.recordingSession),
            clipStatus = "Finalizando take…",
        )
        playbackEngine.stop()
        recordingEngine.stop()
    }

    private val recordingListener = object : StudioRecordingListener {
        override fun onStarted(config: StudioRecordingConfig) = viewModelScope.launch {
            val state = _state.value
            if (!state.recordingSession.readyToOpenCapture) return@launch
            _state.value = state.copy(
                recordingSession = RecordingSessionPolicy.markCaptureStarted(state.recordingSession),
                recordingConfig = config,
                transport = TransportPolicy.startRecording(state.transport),
                clipStatus = "Gravando • ${config.sampleRateHz} Hz • ${config.channelCount} canal(is)",
            )
            startBackingPlaybackForRecording(config.sampleRateHz)
        }.let { Unit }

        override fun onProgress(framesCaptured: Long, peak: Float, rms: Float) = viewModelScope.launch {
            val state = _state.value
            if (state.recordingSession.phase != RecordingSessionPhase.CAPTURING) return@launch
            val targetId = state.recordingSession.targetTrackId
            _state.value = state.copy(
                recordingSession = RecordingSessionPolicy.updateCapturedFrames(state.recordingSession, framesCaptured),
                recordingPeak = peak,
                recordingRms = rms,
                trackClipLatched = if (peak > 1f && targetId != null) state.trackClipLatched + targetId else state.trackClipLatched,
            )
        }.let { Unit }

        override fun onStopped(result: StudioRecordingResult) {
            finalizeRecording(result)
        }

        override fun onError(message: String) = viewModelScope.launch {
            recordingTransaction?.let(recordingMediaStore::discard)
            recordingTransaction = null
            resetRecording(message)
        }.let { Unit }

        override fun onWarning(message: String) = viewModelScope.launch {
            _state.value = _state.value.copy(clipStatus = message)
        }.let { Unit }
    }

    private fun finalizeRecording(result: StudioRecordingResult) {
        val transaction = recordingTransaction ?: return resetRecording("A transação do take foi perdida.")
        recordingTransaction = null
        viewModelScope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    recordingMediaStore.commit(transaction)
                    val before = repository.load(transaction.projectId) ?: error("Projeto não encontrado ao finalizar o take.")
                    val session = _state.value.recordingSession
                    val targetTrackId = requireNotNull(session.targetTrackId) { "A pista do take não está mais disponível." }
                    val saved = RecordedTakeProjectIntegrator.integrate(
                        project = before,
                        targetTrackId = targetTrackId,
                        take = RecordedTakeMetadata(
                            clipId = transaction.id,
                            displayName = "Take ${before.clips.count { it.trackId == targetTrackId } + 1}",
                            managedRelativePath = transaction.relativePath,
                            timelineStartFrame = session.timelineStartFrame,
                            sampleRateHz = result.sampleRateHz,
                            channelCount = result.channelCount,
                            framesCaptured = result.framesCaptured,
                        ),
                        nowEpochMs = System.currentTimeMillis(),
                    )
                    val clip = saved.clips.first { it.id == transaction.id }
                    val envelope = FileSeekableByteSource(transaction.finalFile).use { source ->
                        WaveformEnvelopeBuilder.build(WavPcmDecoder(source), WAVEFORM_POINTS)
                    }
                    waveformCache.write(saved.id, clip.id, envelope)
                    val persisted = repository.save(saved)
                    projectHistory.record(before, persisted)
                    Triple(persisted, clip, envelope.peaks)
                }
            }.onSuccess { (saved, clip, peaks) ->
                val state = _state.value
                val end = TimelineControlPolicy.projectEndFrame(saved)
                _state.value = state.copy(
                    project = saved,
                    waveforms = state.waveforms + (clip.id to peaks),
                    timelineControls = TimelineControlPolicy.normalizedForProject(state.timelineControls.copy(playheadFrame = clip.startFrame + clip.lengthFrames), end),
                    transport = state.transport.copy(mode = TransportMode.STOPPED),
                    recordingSession = RecordingSessionPolicy.reset(),
                    recordingConfig = null,
                    recordingPeak = 0f,
                    recordingRms = 0f,
                    transportEngineReady = playbackReadiness(saved).ready,
                    canUndo = projectHistory.canUndo,
                    canRedo = projectHistory.canRedo,
                    clipStatus = if (result.partial) "Take parcial preservado com segurança" else "Take gravado com sucesso",
                    error = result.message?.takeIf { result.partial },
                )
            }.onFailure { error ->
                recordingMediaStore.discard(transaction)
                resetRecording(error.message ?: "Não foi possível integrar o take ao projeto.")
            }
        }
    }

    private fun resetRecording(message: String) {
        countdownJob?.cancel()
        countdownJob = null
        val state = _state.value
        _state.value = state.copy(
            transport = state.transport.copy(mode = TransportMode.STOPPED),
            recordingSession = RecordingSessionPolicy.reset(),
            recordingConfig = null,
            recordingPeak = 0f,
            recordingRms = 0f,
            clipStatus = null,
            error = message,
        )
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

    fun clearTrackContents(trackId: String) = editClip("Pista limpa") { current ->
        ProjectTrackEditor.clearTrackContents(current, trackId, System.currentTimeMillis())
    }

    fun duplicateClip(clipId: String) {
        val source = _state.value.project?.clips?.firstOrNull { it.id == clipId } ?: return
        val newId = UUID.randomUUID().toString()
        editClip("Clipe duplicado") { current ->
            ProjectClipEditor.duplicateClip(
                project = current,
                clipId = clipId,
                newClipId = newId,
                startFrame = source.startFrame + source.lengthFrames,
                nowEpochMs = System.currentTimeMillis(),
            )
        }
    }

    fun splitClipAtPlayhead(clipId: String) {
        val splitFrame = _state.value.timelineControls.playheadFrame
        editClip("Clipe dividido") { current ->
            ProjectClipEditor.splitClipAtTimelineFrame(
                project = current,
                clipId = clipId,
                splitFrame = splitFrame,
                newRightClipId = UUID.randomUUID().toString(),
                nowEpochMs = System.currentTimeMillis(),
            )
        }
    }

    fun moveClipToTrack(clipId: String, targetTrackId: String) {
        val current = _state.value.project ?: return
        val clip = current.clips.firstOrNull { it.id == clipId } ?: return
        if (clip.trackId == targetTrackId) return
        editClip("Clipe movido para outra pista") { latest ->
            ProjectClipEditor.moveClipToTrack(latest, clipId, targetTrackId, System.currentTimeMillis())
        }
    }

    fun reorderTrack(trackId: String, targetIndex: Int) {
        val current = _state.value
        val project = current.project ?: return
        if (!structuralEditingAllowed(current)) return
        val boundedTarget = targetIndex.coerceIn(0, project.tracks.lastIndex)
        viewModelScope.launch {
            runCatching {
                saveLatest(project.id) { latest ->
                    ProjectTrackEditor.reorderTrack(latest, trackId, boundedTarget, System.currentTimeMillis())
                }
            }.onSuccess { applySavedProject(it, "Pistas reordenadas") }
                .onFailure { error -> _state.value = _state.value.copy(error = error.message ?: "Não foi possível reordenar a pista.") }
        }
    }

    fun dismissTransientMessage(message: String) {
        val current = _state.value
        if (message == current.error || message == current.clipStatus || message == current.importStatus) {
            _state.value = current.copy(error = null, clipStatus = null, importStatus = null)
        }
    }

    fun previewTrackGainDb(trackId: String, gainDb: Float) {
        if (_state.value.historyBusy) return
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
        if (_state.value.historyBusy) return
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
        val normalizedName = runCatching { TrackNamePolicy.requireValid(name) }.getOrElse { error ->
            _state.value = _state.value.copy(error = error.message ?: "Nome de pista inválido.")
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
                        name = TrackNamePolicy.requireValid("Nova pista $number"),
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
        if (_state.value.historyBusy) return
        playbackEngine.setMasterGainDb(gainDb.coerceIn(-60f, 12f))
    }

    fun commitMasterGainDb(gainDb: Float) {
        val currentState = _state.value
        val project = currentState.project ?: return
        if (currentState.importing || currentState.editingClip || currentState.historyBusy || currentState.trimControls != null) return
        val normalized = gainDb.coerceIn(-60f, 12f)
        viewModelScope.launch {
            runCatching {
                saveLatest(project.id) { latest ->
                    latest.copy(masterGainDb = normalized, updatedAtEpochMs = System.currentTimeMillis())
                }
            }.onSuccess { saved ->
                val state = _state.value
                _state.value = state.copy(
                    project = saved,
                    masterGainDb = saved.masterGainDb,
                    canUndo = projectHistory.canUndo,
                    canRedo = projectHistory.canRedo,
                    error = null,
                )
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
        countdownJob?.cancel()
        recordingEngine.close()
        recordingTransaction?.let(recordingMediaStore::discard)
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
                    val managedPath = editingMediaPath(clip)
                    StudioPlaybackClip(
                        file = mediaStore.resolveEditable(project.id, managedPath),
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

    private fun startBackingPlaybackForRecording(sampleRateHz: Int) {
        val state = _state.value
        val project = state.project ?: return
        val end = TimelineControlPolicy.projectEndFrame(project)
        if (end <= 0L || project.clips.isEmpty()) return
        val anySolo = project.tracks.any { it.solo }
        val tracksById = project.tracks.associateBy { it.id }
        val audibleTrackIds = project.tracks
            .filter { TrackMixPolicy.isAudible(it.muted, it.solo, anySolo) }
            .map { it.id }
            .toSet()
        val clips = runCatching {
            project.clips.mapNotNull { clip ->
                val track = tracksById[clip.trackId] ?: return@mapNotNull null
                if (clip.muted || track.id !in audibleTrackIds || clip.sourceSampleRateHz != sampleRateHz) return@mapNotNull null
                val managedPath = editingMediaPathOrNull(clip) ?: return@mapNotNull null
                StudioPlaybackClip(
                    file = mediaStore.resolveEditable(project.id, managedPath),
                    trackId = track.id,
                    timelineStartFrame = clip.startFrame,
                    sourceStartFrame = clip.sourceStartFrame,
                    lengthFrames = clip.lengthFrames,
                    gainDb = clip.gainDb,
                )
            }
        }.getOrElse {
            _state.value = state.copy(clipStatus = "Gravando sem backing: não foi possível preparar a reprodução.")
            return
        }
        if (clips.isEmpty()) return
        val outputSignature = audioRoutingStore.selectedOutputSignature()
        val request = StudioPlaybackRequest(
            sampleRateHz = sampleRateHz,
            startFrame = state.recordingSession.timelineStartFrame.coerceAtMost(end),
            projectEndFrame = end,
            loopEnabled = state.transport.loopEnabled,
            loopStartFrame = state.timelineControls.loopStartFrame,
            loopEndFrame = state.timelineControls.loopEndFrame,
            clips = clips,
            trackMixes = project.tracks.filter { it.id in audibleTrackIds }.map {
                StudioPlaybackTrackMix(it.id, it.gainDb, it.pan)
            },
            preferredOutputDevice = audioRoutingStore.resolveSelectedOutputDevice(),
            preferredOutputRequested = !outputSignature.isNullOrBlank(),
            masterGainDb = project.masterGainDb,
        )
        runCatching {
            playbackEngine.start(request, object : StudioPlaybackListener {
                override fun onPosition(frame: Long) {
                    viewModelScope.launch {
                        val current = _state.value
                        if (current.recordingSession.phase == RecordingSessionPhase.CAPTURING) {
                            _state.value = current.copy(timelineControls = current.timelineControls.copy(playheadFrame = frame))
                        }
                    }
                }

                override fun onStopped(frame: Long) = Unit

                override fun onError(message: String) {
                    viewModelScope.launch {
                        val current = _state.value
                        if (current.recordingSession.phase == RecordingSessionPhase.CAPTURING) {
                            _state.value = current.copy(clipStatus = "Gravação continua sem backing: $message")
                        }
                    }
                }

                override fun onMasterMeter(meter: StudioPlaybackMeter) {
                    viewModelScope.launch {
                        val current = _state.value
                        if (current.recordingSession.phase == RecordingSessionPhase.CAPTURING) {
                            _state.value = current.copy(
                                masterMeter = MeterBallisticsPolicy.update(
                                    current.masterMeter,
                                    meter.peak,
                                    meter.rms,
                                    System.currentTimeMillis(),
                                )
                            )
                        }
                    }
                }
            })
        }.onFailure {
            _state.value = _state.value.copy(clipStatus = "Gravando sem backing: ${it.message ?: "saída indisponível"}")
        }
    }

    private fun editTimelineMarker(transform: (TimelineControlState, Long) -> TimelineControlState) {
        val current = _state.value
        val project = current.project ?: return
        if (!TransportPolicy.timelineEditingEnabled(current.transport) || current.trimControls != null || current.historyBusy) return
        val end = TimelineControlPolicy.projectEndFrame(project)
        _state.value = current.copy(timelineControls = transform(current.timelineControls, end))
    }

    private fun editClip(status: String, transform: (GuitarProject) -> GuitarProject) {
        val currentState = _state.value
        val current = currentState.project ?: return
        if (!structuralEditingAllowed(currentState)) return
        viewModelScope.launch {
            _state.value = _state.value.copy(editingClip = true, error = null, clipStatus = null)
            runCatching {
                val saved = saveLatest(current.id, transform)
                saved to withContext(Dispatchers.IO) { loadWaveforms(saved) }
            }
                .onSuccess { (saved, waveforms) ->
                    val end = TimelineControlPolicy.projectEndFrame(saved)
                    _state.value = _state.value.copy(
                        editingClip = false,
                        project = saved,
                        waveforms = waveforms,
                        timelineControls = TimelineControlPolicy.normalizedForProject(_state.value.timelineControls, end),
                        transportEngineReady = playbackReadiness(saved).ready,
                        masterGainDb = saved.masterGainDb,
                        canUndo = projectHistory.canUndo,
                        canRedo = projectHistory.canRedo,
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
        if (currentState.importing || currentState.editingClip || currentState.historyBusy || currentState.trimControls != null) return
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
                    canUndo = projectHistory.canUndo,
                    canRedo = projectHistory.canRedo,
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
            canUndo = projectHistory.canUndo,
            canRedo = projectHistory.canRedo,
            clipStatus = status,
            error = null,
        )
    }

    private fun applyHistorySnapshot(saved: GuitarProject, waveforms: Map<String, List<Float>>, status: String) {
        trackMixDrafts.clear()
        saved.tracks.forEach { trackMixDrafts[it.id] = TrackMixDraft(it.gainDb, it.pan) }
        playbackEngine.setMasterGainDb(saved.masterGainDb)
        val state = _state.value
        val end = TimelineControlPolicy.projectEndFrame(saved)
        _state.value = state.copy(
            historyBusy = false,
            project = saved,
            waveforms = waveforms,
            trimControls = null,
            timelineControls = TimelineControlPolicy.normalizedForProject(state.timelineControls, end),
            transportEngineReady = playbackReadiness(saved).ready,
            masterGainDb = saved.masterGainDb,
            masterMeter = MeterBallisticsPolicy.reset(),
            trackMeters = emptyMap(),
            masterClipLatched = false,
            trackClipLatched = emptySet(),
            canUndo = projectHistory.canUndo,
            canRedo = projectHistory.canRedo,
            clipStatus = status,
            error = null,
        )
    }

    private fun structuralEditingAllowed(state: StudioUiState): Boolean =
        !state.importing && !state.editingClip && !state.historyBusy && state.trimControls == null && TransportPolicy.timelineEditingEnabled(state.transport)

    private suspend fun saveLatest(projectId: String, transform: (GuitarProject) -> GuitarProject): GuitarProject =
        projectSaveMutex.withLock {
            withContext(Dispatchers.IO) {
                val latest = repository.load(projectId) ?: error("Projeto não encontrado: $projectId")
                val updated = transform(latest)
                val saved = repository.save(updated)
                projectHistory.record(latest, saved)
                saved
            }
        }

    private suspend fun persistHistorySnapshot(snapshot: GuitarProject): Pair<GuitarProject, Map<String, List<Float>>> =
        projectSaveMutex.withLock {
            withContext(Dispatchers.IO) {
                val saved = repository.save(snapshot.copy(updatedAtEpochMs = System.currentTimeMillis()))
                saved to loadWaveforms(saved)
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
        if (audible.any { editingMediaPathOrNull(it).isNullOrBlank() }) {
            return PlaybackReadiness(false, reason = "Há clipes sem mídia de edição disponível no projeto.")
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
            val managedPath = editingMediaPathOrNull(clip) ?: return@forEach
            runCatching {
                val file = mediaStore.resolveEditable(project.id, managedPath)
                val envelope = FileSeekableByteSource(file).use { source -> WaveformEnvelopeBuilder.build(WavPcmDecoder(source), WAVEFORM_POINTS) }
                waveformCache.write(project.id, clip.id, envelope)
                put(clip.id, envelope.peaks)
            }
        }
    }

    fun exportProjectPackage(uri: Uri) {
        val project = _state.value.project ?: return
        if (_state.value.importing || _state.value.exporting || _state.value.historyBusy) return
        viewModelScope.launch {
            _state.value = _state.value.copy(exporting = true, exportStatus = "Salvando projeto…", error = null)
            runCatching {
                withContext(Dispatchers.IO) {
                    val output = getApplication<Application>().contentResolver.openOutputStream(uri, "w")
                        ?: error("O Android não conseguiu criar o arquivo do projeto.")
                    output.use { ProjectBundleWriter().write(project, mediaStore.projectDirectoryForExport(project.id), it) }
                }
            }.onSuccess {
                _state.value = _state.value.copy(exporting = false, exportStatus = "Projeto GuitarLab salvo com sucesso")
            }.onFailure { error ->
                _state.value = _state.value.copy(exporting = false, exportStatus = null, error = error.message ?: "Não foi possível salvar o projeto.")
            }
        }
    }

    fun exportMaster(uri: Uri, format: MasterExportFormat) {
        val state = _state.value
        val project = state.project ?: return
        if (state.importing || state.exporting || state.historyBusy || state.trimControls != null || !TransportPolicy.timelineEditingEnabled(state.transport)) return
        viewModelScope.launch {
            _state.value = _state.value.copy(exporting = true, exportStatus = "Renderizando ${format.name}…", error = null)
            runCatching {
                withContext(Dispatchers.IO) {
                    val readiness = playbackReadiness(project)
                    val rate = readiness.sampleRateHz ?: error(readiness.reason ?: "Projeto sem áudio exportável.")
                    require(readiness.ready) { readiness.reason ?: "Projeto sem áudio exportável." }
                    val request = masterRenderRequest(project, rate)
                    val floatWav = File.createTempFile("guitarlab-master-", ".wav", getApplication<Application>().cacheDir)
                    val encoded = if (format == MasterExportFormat.WAV_FLOAT32) floatWav else File.createTempFile("guitarlab-master-", ".${format.extension}", getApplication<Application>().cacheDir)
                    try {
                        StudioMasterRenderer.renderFloatWav(request, floatWav)
                        if (format != MasterExportFormat.WAV_FLOAT32) AndroidMasterAudioEncoder.encode(floatWav, encoded, format)
                        val source = if (format == MasterExportFormat.WAV_FLOAT32) floatWav else encoded
                        val output = getApplication<Application>().contentResolver.openOutputStream(uri, "w")
                            ?: error("O Android não conseguiu criar o arquivo exportado.")
                        output.use { target -> source.inputStream().buffered().use { it.copyTo(target) } }
                    } finally {
                        floatWav.delete()
                        if (encoded != floatWav) encoded.delete()
                    }
                }
            }.onSuccess {
                _state.value = _state.value.copy(exporting = false, exportStatus = "Master ${format.name} exportado com sucesso")
            }.onFailure { error ->
                _state.value = _state.value.copy(exporting = false, exportStatus = null, error = error.message ?: "Não foi possível exportar o master.")
            }
        }
    }

    private fun masterRenderRequest(project: GuitarProject, sampleRateHz: Int): StudioMasterRenderRequest {
        val anySolo = project.tracks.any { it.solo }
        val audibleTracks = project.tracks.filter { TrackMixPolicy.isAudible(it.muted, it.solo, anySolo) }
        val audibleIds = audibleTracks.map { it.id }.toSet()
        val clips = project.clips.mapNotNull { clip ->
            if (clip.muted || clip.trackId !in audibleIds) return@mapNotNull null
            val path = editingMediaPathOrNull(clip) ?: return@mapNotNull null
            StudioMasterRenderClip(
                file = mediaStore.resolveEditable(project.id, path),
                trackId = clip.trackId,
                timelineStartFrame = clip.startFrame,
                sourceStartFrame = clip.sourceStartFrame,
                lengthFrames = clip.lengthFrames,
                gainDb = clip.gainDb,
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

    private fun editingMediaPath(clip: AudioClip): String = requireNotNull(editingMediaPathOrNull(clip)) {
        "O clipe '${clip.name}' não possui mídia de edição gerenciada."
    }

    private fun editingMediaPathOrNull(clip: AudioClip): String? = clip.managedEditProxyPath ?: clip.managedSourcePath

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
