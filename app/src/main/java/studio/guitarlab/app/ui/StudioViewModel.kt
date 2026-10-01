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
import kotlin.math.abs
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import studio.guitarlab.app.backup.BackupScheduler
import studio.guitarlab.core.audio.LatencyFineAdjustmentPolicy
import studio.guitarlab.core.audio.MeterBallisticsPolicy
import studio.guitarlab.core.audio.MeterBallisticsState
import studio.guitarlab.core.audio.TrackMixPolicy
import studio.guitarlab.core.audio.RecordingTimingCompensationPolicy
import studio.guitarlab.core.audio.RecordingSessionHealthRecord
import studio.guitarlab.core.audio.RecordingTimingEvidenceBasis
import studio.guitarlab.core.audio.AudioRouteSessionPolicy
import studio.guitarlab.core.audio.AudioRouteSessionState
import studio.guitarlab.core.codec.AudioImportFormat
import studio.guitarlab.core.codec.AudioImportFormatPolicy
import studio.guitarlab.core.codec.FileSeekableByteSource
import studio.guitarlab.core.codec.WavMetadataReader
import studio.guitarlab.core.codec.WavPcmDecoder
import studio.guitarlab.core.codec.WaveformEnvelope
import studio.guitarlab.core.codec.WaveformEnvelopeBuilder
import studio.guitarlab.core.codec.StereoWavChannelSplitter
import studio.guitarlab.core.codec.WavSampleRateConverter
import studio.guitarlab.core.model.AudioClip
import studio.guitarlab.core.model.AudioTrack
import studio.guitarlab.core.model.BuiltInRoles
import studio.guitarlab.core.model.ChannelLayout
import studio.guitarlab.core.model.GuitarProject
import studio.guitarlab.core.model.RoleSource
import studio.guitarlab.core.model.TrackNamePolicy
import studio.guitarlab.core.model.TrackOutputRoute
import studio.guitarlab.core.model.TrackOutputRoutingPolicy
import studio.guitarlab.core.project.FileProjectRepository
import studio.guitarlab.core.project.ProjectClipEditor
import studio.guitarlab.core.project.ProjectTrackEditor
import studio.guitarlab.core.project.ProjectHistory
import studio.guitarlab.core.project.ProjectManagedMediaStore
import studio.guitarlab.core.project.PreparedReferenceBindingPolicy
import studio.guitarlab.core.project.ProjectRecordingMediaStore
import studio.guitarlab.core.project.RecordingMediaTransaction
import studio.guitarlab.core.project.RecordingRecoveryCandidate
import studio.guitarlab.core.project.RecordingRecoveryPolicy
import studio.guitarlab.core.project.RecordingRecoveryPublicationDecision
import studio.guitarlab.core.project.RecordedTakeMetadata
import studio.guitarlab.core.project.RecordedTakeProjectIntegrator
import studio.guitarlab.core.project.RecordingSampleRatePolicy
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
import studio.guitarlab.core.project.WaveformCacheIdentity
import studio.guitarlab.core.project.WaveformCacheStore
import studio.guitarlab.core.project.ActiveTakePolicy
import studio.guitarlab.core.project.GuitarAuditionMode
import studio.guitarlab.core.project.LiveWaveformAccumulator
import studio.guitarlab.core.project.LiveWaveformPoint
import studio.guitarlab.core.project.LevelAnalysis
import studio.guitarlab.core.project.PracticeRecordingMode
import studio.guitarlab.core.project.PracticeRecordingStartPlan
import studio.guitarlab.core.project.PracticeRecordingStartPolicy
import studio.guitarlab.core.project.PracticeWorkflowEditor
import studio.guitarlab.core.project.PunchCapturePlan
import studio.guitarlab.core.project.PunchRecordingPolicy
import studio.guitarlab.core.project.SectionBoundaryAnalyzer
import studio.guitarlab.core.project.SectionBoundarySuggestion
import studio.guitarlab.core.project.TrackLevelAccumulator
import studio.guitarlab.core.project.TrackLevelAdvisor
import studio.guitarlab.core.project.TrackRoleAssignmentPolicy
import studio.guitarlab.core.project.TakeManagementPolicy
import studio.guitarlab.core.project.StereoSeparationIds
import studio.guitarlab.core.project.StereoSeparationProjectPolicy
import studio.guitarlab.platform.audio.android.AndroidStudioPlaybackEngine
import studio.guitarlab.platform.audio.android.AndroidStudioRecordingEngine
import studio.guitarlab.platform.audio.android.StudioRecordingConfig
import studio.guitarlab.platform.audio.android.StudioRecordingListener
import studio.guitarlab.platform.audio.android.StudioRecordingRequest
import studio.guitarlab.platform.audio.android.StudioRecordingResult
import studio.guitarlab.platform.audio.android.StudioPlaybackClip
import studio.guitarlab.platform.audio.android.StudioPlaybackListener
import studio.guitarlab.platform.audio.android.StudioPlaybackMeter
import studio.guitarlab.platform.audio.android.StudioPlaybackStartInfo
import studio.guitarlab.platform.audio.android.StudioPlaybackRequest
import studio.guitarlab.platform.audio.android.StudioPlaybackRoutingStatus
import studio.guitarlab.platform.audio.android.StudioPlaybackTrackMeter
import studio.guitarlab.platform.audio.android.StudioPlaybackTrackMix
import studio.guitarlab.platform.codec.android.AndroidAudioImportTranscoder

data class StudioRecoveryItem(
    val transactionId: String,
    val trackName: String?,
    val approximateDurationSeconds: Double?,
    val createdAtEpochMs: Long?,
    val safePlayable: Boolean,
    val recoverable: Boolean,
    val finalizedUnpublished: Boolean,
    val diagnosticReason: String? = null,
)

data class StereoImportPrompt(
    val clipId: String,
    val fileName: String,
    val leftTrackName: String? = null,
    val rightTrackName: String? = null,
) {
    val hasGuitarPair: Boolean get() = leftTrackName != null && rightTrackName != null
}

data class StudioUiState(
    val loading: Boolean = true,
    val importing: Boolean = false,
    val editingClip: Boolean = false,
    val historyBusy: Boolean = false,
    val project: GuitarProject? = null,
    val waveforms: Map<String, List<Float>> = emptyMap(),
    val waveformChannels: Map<String, List<List<Float>>> = emptyMap(),
    val timelineControls: TimelineControlState = TimelineControlState(),
    val trimControls: TrimControlState? = null,
    val transport: TransportState = TransportState(),
    val transportEngineReady: Boolean = false,
    val recordingSession: RecordingSessionState = RecordingSessionState(),
    val loopRecordingChoiceVisible: Boolean = false,
    val recordingConfig: StudioRecordingConfig? = null,
    val recordingPeak: Float = 0f,
    val recordingRms: Float = 0f,
    val liveRecordingWaveform: List<LiveWaveformPoint> = emptyList(),
    val guitarAuditionMode: GuitarAuditionMode = GuitarAuditionMode.MIXER,
    val sectionSuggestions: List<SectionBoundarySuggestion> = emptyList(),
    val trackLevelAnalysis: Map<String, LevelAnalysis> = emptyMap(),
    val trackLevelAnalysisBusy: Set<String> = emptySet(),
    val masterGainDb: Float = 0f,
    val masterMeter: MeterBallisticsState = MeterBallisticsState(),
    val trackMeters: Map<String, MeterBallisticsState> = emptyMap(),
    val masterClipLatched: Boolean = false,
    val trackClipLatched: Set<String> = emptySet(),
    val canUndo: Boolean = false,
    val canRedo: Boolean = false,
    val recoveryBusy: Boolean = false,
    val recoveryItems: List<StudioRecoveryItem> = emptyList(),
    val error: String? = null,
    val importStatus: String? = null,
    val stereoImportPrompt: StereoImportPrompt? = null,
    val newTrackRolePromptId: String? = null,
    val clipStatus: String? = null,
    val transientNotice: String? = null,
    val transientNoticeKind: TransientFeedbackKind = TransientFeedbackKind.WARNING,
    val preparedReferenceUpdateAvailable: Boolean = false,
)

private data class ImportOutcome(
    val project: GuitarProject,
    val channelCount: Int,
    val sampleRateHz: Int,
    val peaks: List<Float>,
    val channelPeaks: List<List<Float>>,
)

private data class StudioLoadOutcome(
    val project: GuitarProject,
    val waveforms: Map<String, List<Float>>,
    val waveformChannels: Map<String, List<List<Float>>>,
    val recoveryCandidates: List<RecordingRecoveryCandidate>,
)

private data class LoadedWaveformState(
    val waveforms: Map<String, List<Float>>,
    val waveformChannels: Map<String, List<List<Float>>>,
)

private data class RecordingProgressUpdate(
    val generation: Long,
    val framesCaptured: Long,
    val peak: Float,
    val rms: Float,
)

private data class TrackMixDraft(val gainDb: Float, val pan: Float)

class StudioViewModel(application: Application) : AndroidViewModel(application) {
    private val rootDirectory = application.filesDir
    private val repository = FileProjectRepository(rootDirectory)
    private val mediaStore = ProjectManagedMediaStore(rootDirectory)
    private val recordingMediaStore = ProjectRecordingMediaStore(rootDirectory)
    private val waveformCache = WaveformCacheStore(rootDirectory)
    private val audioRoutingStore = StudioAudioRoutingStore(application)
    private val latencyCalibrationStore = StudioLatencyCalibrationStore(application)
    private val recordingSessionHealthStore = RecordingSessionHealthStore(application)
    private var activeRecordingRouteLatencyFrames: Long = 0L
    private var activeRecordingFineAdjustmentFrames: Long = 0L
    @Volatile private var activeRecordingClockOffsetFrames: Long = 0L
    private var activeRecordingCaptureStartNs: Long = 0L
    private var activeRecordingCaptureTimestampBased: Boolean = false
    private var activeRecordingCaptureAnchorJitterNs: Long? = null
    private var activeRecordingCaptureAnchorObservations: Int? = null
    private var activeRecordingBackingAnchorJitterNs: Long? = null
    private var activeRecordingBackingAnchorObservations: Int? = null
    private var activeRecordingTimingBasis: RecordingTimingEvidenceBasis = RecordingTimingEvidenceBasis.NO_BACKING_REFERENCE
    private var activeRecordingSelectedInputIdentity: String? = null
    private var activeRecordingEffectiveInputIdentity: String? = null
    private var activeRecordingSelectedOutputIdentity: String? = null
    private var activeRecordingEffectiveOutputIdentity: String? = null
    private var activeRecordingOutputFallback: Boolean = false
    private var activeRecordingRouteChanged: Boolean = false
    private var activeRecordingOutputUnderruns: Int? = null
    private var activePunchRegion: studio.guitarlab.core.model.PunchRegion? = null
    private var pendingRecordingPlan: PracticeRecordingStartPlan? = null
    private var activePunchPlan: PunchCapturePlan? = null
    private val liveWaveform = LiveWaveformAccumulator()
    private val recordingProgressChannel = Channel<RecordingProgressUpdate>(Channel.CONFLATED)
    @Volatile private var recordingProgressGeneration: Long = 0L
    private var playbackSessionId = 0L
    private val playbackEngine = AndroidStudioPlaybackEngine()
    private val recordingEngine = AndroidStudioRecordingEngine(application)
    private var recordingTransaction: RecordingMediaTransaction? = null
    private val recoveryCandidatesById = linkedMapOf<String, RecordingRecoveryCandidate>()
    private var activeRecordingRouteState: AudioRouteSessionState? = null
    private var activePlaybackRouteState: AudioRouteSessionState? = null
    private var countdownJob: Job? = null
    private val projectSaveMutex = Mutex()
    @Volatile private var projectSessionGeneration: Long = 0L
    private val projectHistory = ProjectHistory()
    private val trackMixDrafts = mutableMapOf<String, TrackMixDraft>()
    private val _state = MutableStateFlow(StudioUiState())
    private var lastTransientWarning: String? = null
    private var lastTransientWarningAtMs: Long = 0L

    init {
        viewModelScope.launch {
            for (progress in recordingProgressChannel) {
                if (progress.generation != recordingProgressGeneration) continue
                val state = _state.value
                if (state.recordingSession.phase != RecordingSessionPhase.CAPTURING) continue
                val targetId = state.recordingSession.targetTrackId
                val nowMs = System.currentTimeMillis()
                val recordingTrackMeters = RecordingTrackMeterPolicy.update(
                    previous = state.trackMeters,
                    targetTrackId = targetId,
                    rawPeak = progress.peak,
                    rawRms = progress.rms,
                    nowMs = nowMs,
                )
                _state.value = state.copy(
                    recordingSession = RecordingSessionPolicy.updateCapturedFrames(state.recordingSession, progress.framesCaptured),
                    recordingPeak = progress.peak,
                    recordingRms = progress.rms,
                    liveRecordingWaveform = liveWaveform.snapshot(),
                    trackMeters = recordingTrackMeters,
                    trackClipLatched = if (progress.peak > 1f && targetId != null) state.trackClipLatched + targetId else state.trackClipLatched,
                )
                activePunchPlan?.let { plan ->
                    if (progress.framesCaptured >= plan.automaticStopAfterFrames &&
                        _state.value.recordingSession.phase == RecordingSessionPhase.CAPTURING
                    ) stopRecording()
                }
                delay(LIVE_WAVEFORM_UI_INTERVAL_MS)
            }
        }
    }
    val state: StateFlow<StudioUiState> = _state.asStateFlow()

    fun load(projectId: String) {
        val before = _state.value
        if (before.project?.id == projectId && !before.loading) {
            when (before.recordingSession.phase) {
                RecordingSessionPhase.CAPTURING -> stopRecording()
                RecordingSessionPhase.COUNTDOWN -> cancelRecordingCountdown()
                RecordingSessionPhase.FINALIZING -> stopPlaybackSession()
                RecordingSessionPhase.IDLE -> {
                    stopPlaybackSession()
                    if (before.transport.mode != TransportMode.STOPPED) {
                        _state.value = before.copy(transport = before.transport.copy(mode = TransportMode.STOPPED))
                    }
                }
            }
            // Same project id is not enough to prove that the resident Studio snapshot is current:
            // Prepare/restore can replace reference media while this Activity-scoped ViewModel lives.
            // Read only the small project manifest first. WAV decoding happens only when the persisted
            // snapshot actually changed, and the waveform cache then limits decoding to invalid entries.
            val residentProject = before.project
            viewModelScope.launch {
                runCatching {
                    withContext(Dispatchers.IO) {
                        val latest = repository.load(projectId) ?: error("Projeto não encontrado: $projectId")
                        if (latest == residentProject) return@withContext null
                        val candidates = recordingMediaStore.recoveryCandidates(projectId)
                        val pending = candidates.filter {
                            RecordingRecoveryPolicy.publicationDecision(latest, it) != RecordingRecoveryPublicationDecision.ALREADY_PUBLISHED
                        }
                        val waveformState = loadWaveformState(latest)
                        StudioLoadOutcome(latest, waveformState.waveforms, waveformState.waveformChannels, pending)
                    }
                }.onSuccess { outcome ->
                    if (outcome == null || _state.value.project?.id != projectId) return@onSuccess
                    ++projectSessionGeneration
                    projectHistory.clear()
                    trackMixDrafts.clear()
                    val latest = outcome.project
                    latest.tracks.forEach { track ->
                        trackMixDrafts[track.id] = TrackMixDraft(track.gainDb, track.pan)
                        playbackEngine.setTrackMix(track.id, track.gainDb, track.pan)
                        playbackEngine.setTrackAudibility(track.id, track.muted, track.solo)
                        playbackEngine.setTrackOutputRoute(track.id, track.outputRoute)
                    }
                    playbackEngine.setMasterGainDb(latest.masterGainDb)
                    recoveryCandidatesById.clear()
                    outcome.recoveryCandidates.forEach { recoveryCandidatesById[it.transactionId] = it }
                    val state = _state.value
                    val end = TimelineControlPolicy.projectEndFrame(latest)
                    _state.value = state.copy(
                        project = latest,
                        waveforms = outcome.waveforms,
                        waveformChannels = outcome.waveformChannels,
                        timelineControls = TimelineControlPolicy.normalizedForProject(state.timelineControls, end),
                        transportEngineReady = playbackReadiness(latest).ready,
                        masterGainDb = latest.masterGainDb,
                        recoveryItems = recoveryItems(latest, outcome.recoveryCandidates),
                        trackLevelAnalysis = emptyMap(),
                        canUndo = false,
                        canRedo = false,
                        preparedReferenceUpdateAvailable = PreparedReferenceBindingPolicy.updateAvailable(latest),
                        clipStatus = "Studio sincronizado com a versão atual do projeto",
                        error = null,
                    )
                }.onFailure { error ->
                    if (_state.value.project?.id == projectId) {
                        _state.value = _state.value.copy(error = error.message ?: "Não foi possível atualizar o Studio.")
                    }
                }
            }
            return
        } else if (before.recordingSession.phase == RecordingSessionPhase.CAPTURING) {
            stopRecording()
            return
        } else if (before.recordingSession.phase == RecordingSessionPhase.COUNTDOWN) {
            cancelRecordingCountdown()
        }
        val sessionGeneration = ++projectSessionGeneration
        stopPlaybackSession()
        projectHistory.clear()
        trackMixDrafts.clear()
        viewModelScope.launch {
            _state.value = StudioUiState(loading = true)
            runCatching {
                withContext(Dispatchers.IO) {
                    var project = repository.load(projectId) ?: error("Projeto não encontrado: $projectId")
                    recordingMediaStore.cleanupInterrupted(projectId)
                    val candidates = recordingMediaStore.recoveryCandidates(projectId)
                    candidates.forEach { candidate ->
                        if (RecordingRecoveryPolicy.publicationDecision(project, candidate) == RecordingRecoveryPublicationDecision.ALREADY_PUBLISHED) {
                            runCatching { recordingMediaStore.markPublished(projectId, candidate.transactionId) }
                        }
                    }
                    val pending = candidates.filter {
                        RecordingRecoveryPolicy.publicationDecision(project, it) != RecordingRecoveryPublicationDecision.ALREADY_PUBLISHED
                    }
                    val waveformState = loadWaveformState(project)
                    StudioLoadOutcome(project, waveformState.waveforms, waveformState.waveformChannels, pending)
                }
            }.onSuccess { outcome ->
                val project = outcome.project
                val waveforms = outcome.waveforms
                val waveformChannels = outcome.waveformChannels
                if (sessionGeneration != projectSessionGeneration) return@onSuccess
                project.tracks.forEach { trackMixDrafts[it.id] = TrackMixDraft(it.gainDb, it.pan) }
                recoveryCandidatesById.clear()
                outcome.recoveryCandidates.forEach { recoveryCandidatesById[it.transactionId] = it }
                val recoveryItems = recoveryItems(project, outcome.recoveryCandidates)
                val end = TimelineControlPolicy.projectEndFrame(project)
                _state.value = StudioUiState(
                    loading = false,
                    project = project,
                    waveforms = waveforms,
                    waveformChannels = waveformChannels,
                    timelineControls = TimelineControlPolicy.normalizedForProject(TimelineControlState(), end),
                    transportEngineReady = playbackReadiness(project).ready,
                    masterGainDb = project.masterGainDb,
                    recoveryItems = recoveryItems,
                    preparedReferenceUpdateAvailable = PreparedReferenceBindingPolicy.updateAvailable(project),
                )
            }.onFailure { error ->
                if (sessionGeneration != projectSessionGeneration) return@onFailure
                _state.value = StudioUiState(loading = false, error = error.message ?: "Não foi possível abrir o projeto.")
            }
        }
    }

    fun applyPreparedReferenceUpdate() {
        val currentState = _state.value
        val project = currentState.project ?: return
        if (!PreparedReferenceBindingPolicy.bindingDiffersFromDesired(project) || !structuralEditingAllowed(currentState)) return
        viewModelScope.launch {
            _state.value = _state.value.copy(historyBusy = true, error = null)
            runCatching {
                val saved = saveLatest(project.id) { latest ->
                    PreparedReferenceBindingPolicy.applyUpdate(latest, System.currentTimeMillis())
                }
                withContext(Dispatchers.IO) {
                    val waveformState = loadWaveformState(saved)
                    Triple(saved, waveformState.waveforms, waveformState.waveformChannels)
                }
            }.onSuccess { (saved, waveforms, channels) ->
                val state = _state.value
                if (state.project?.id != saved.id) return@onSuccess
                val end = TimelineControlPolicy.projectEndFrame(saved)
                _state.value = state.copy(
                    historyBusy = false, project = saved, waveforms = waveforms, waveformChannels = channels,
                    timelineControls = TimelineControlPolicy.normalizedForProject(state.timelineControls, end),
                    transportEngineReady = playbackReadiness(saved).ready,
                    preparedReferenceUpdateAvailable = PreparedReferenceBindingPolicy.updateAvailable(saved),
                    canUndo = projectHistory.canUndo, canRedo = projectHistory.canRedo,
                    clipStatus = "Referências do Studio atualizadas", error = null,
                )
            }.onFailure { error ->
                _state.value = _state.value.copy(historyBusy = false, error = error.message ?: "Não foi possível atualizar as referências do Studio.")
            }
        }
    }

    fun keepCurrentPreparedReferences() {
        val currentState = _state.value
        val project = currentState.project ?: return
        if (!currentState.preparedReferenceUpdateAvailable || !structuralEditingAllowed(currentState)) return
        viewModelScope.launch {
            _state.value = _state.value.copy(historyBusy = true, error = null)
            runCatching {
                saveLatest(project.id) { latest ->
                    PreparedReferenceBindingPolicy.acknowledgeCurrent(latest, System.currentTimeMillis())
                }
            }.onSuccess { saved ->
                val state = _state.value
                if (state.project?.id != saved.id) return@onSuccess
                _state.value = state.copy(
                    historyBusy = false,
                    project = saved,
                    preparedReferenceUpdateAvailable = PreparedReferenceBindingPolicy.updateAvailable(saved),
                    canUndo = projectHistory.canUndo,
                    canRedo = projectHistory.canRedo,
                    transientNotice = "A versão atual do Studio foi mantida.",
                    transientNoticeKind = TransientFeedbackKind.ASYNC_COMPLETION,
                )
            }.onFailure { error ->
                _state.value = _state.value.copy(
                    historyBusy = false,
                    error = error.message ?: "Não foi possível manter a referência atual.",
                )
            }
        }
    }

    fun importAudio(trackId: String, uri: Uri) {
        val currentState = _state.value
        val current = currentState.project ?: return
        if (!TransportPolicy.timelineEditingEnabled(currentState.transport) || currentState.trimControls != null || currentState.historyBusy) return
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
                    var importedClipId: String? = null
                    var committed = false
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
                        val sourceMetadata = FileSeekableByteSource(editingFile).use { WavMetadataReader().read(it) }
                        require(sourceMetadata.totalFrames > 0) { "O arquivo selecionado não contém áudio completo." }
                        val existingRate = current.clips.firstNotNullOfOrNull { it.editingSampleRateHz ?: it.sourceSampleRateHz }
                        val targetRate = current.sampleRate.fixedHz ?: existingRate ?: when (sourceMetadata.sampleRateHz) {
                            44_100, 48_000, 88_200, 96_000 -> sourceMetadata.sampleRateHz
                            else -> 48_000
                        }
                        var finalEditingFile = editingFile
                        var finalProxyPath = proxyPath
                        if (sourceMetadata.sampleRateHz != targetRate) {
                            val resampleTemp = File.createTempFile("guitarlab-resample-", ".wav", getApplication<Application>().cacheDir)
                            try {
                                WavSampleRateConverter.convert(editingFile, resampleTemp, targetRate)
                                val resampledProxy = resampleTemp.inputStream().buffered().use {
                                    mediaStore.ingestEditProxy(current.id, "${displayName}-sr${targetRate}.wav", it)
                                }
                                val supersededProxyPath = finalProxyPath
                                finalProxyPath = resampledProxy.relativePath
                                proxyPath = resampledProxy.relativePath
                                finalEditingFile = resampledProxy.file
                                supersededProxyPath?.let { oldPath ->
                                    if (oldPath != resampledProxy.relativePath) {
                                        mediaStore.discardUncommitted(current.id, oldPath)
                                    }
                                }
                            } finally {
                                resampleTemp.delete()
                            }
                        }
                        val metadata = FileSeekableByteSource(finalEditingFile).use { WavMetadataReader().read(it) }
                        val clipId = UUID.randomUUID().toString().also { importedClipId = it }
                        val envelope = FileSeekableByteSource(finalEditingFile).use { source ->
                            WaveformEnvelopeBuilder.build(WavPcmDecoder(source), WAVEFORM_POINTS)
                        }
                        val sourceBits = if (originalFormat == AudioImportFormat.WAV_PCM) sourceMetadata.bitsPerSample else null
                        val sourceEncoding = if (originalFormat == AudioImportFormat.WAV_PCM) sourceMetadata.sampleEncoding.name else "COMPRESSED"
                        val clip = AudioClip(
                            id = clipId,
                            trackId = trackId,
                            name = displayName,
                            sourceUri = "managed://${sourceAsset.relativePath}",
                            managedSourcePath = sourceAsset.relativePath,
                            managedEditProxyPath = finalProxyPath,
                            originUri = uri.toString(),
                            startFrame = 0,
                            sourceStartFrame = 0,
                            lengthFrames = metadata.totalFrames,
                            sourceTotalFrames = sourceMetadata.totalFrames,
                            sourceFormat = originalFormat.displayName,
                            sourceSampleRateHz = sourceMetadata.sampleRateHz,
                            sourceChannelCount = sourceMetadata.channelCount,
                            sourceBitsPerSample = sourceBits,
                            sourceEncoding = sourceEncoding,
                            editingSampleRateHz = metadata.sampleRateHz,
                            editingTotalFrames = metadata.totalFrames,
                        )
                        waveformCache.write(
                            current.id,
                            clip.id,
                            WaveformCacheIdentity.forClip(clip, WAVEFORM_POINTS),
                            envelope,
                        )
                        val saved = withContext(NonCancellable) {
                            saveLatest(current.id) { latest ->
                                latest.copy(clips = latest.clips + clip, updatedAtEpochMs = System.currentTimeMillis())
                            }.also { committed = true }
                        }
                        ImportOutcome(saved, metadata.channelCount, metadata.sampleRateHz, envelope.peaks, envelope.channelPeaks)
                    } catch (error: Throwable) {
                        if (!committed) {
                            importedClipId?.let { waveformCache.remove(current.id, it) }
                            proxyPath?.let { mediaStore.discardUncommitted(current.id, it) }
                            mediaStore.discardUncommitted(current.id, sourceAsset.relativePath)
                        }
                        throw error
                    }
                }
            }.onSuccess { outcome ->
                val saved = outcome.project
                val imported = saved.clips.last()
                val endFrame = TimelineControlPolicy.projectEndFrame(saved)
                val previous = _state.value
                _state.value = previous.copy(
                    loading = false,
                    importing = false,
                    project = saved,
                    waveforms = previous.waveforms + (imported.id to outcome.peaks),
                    waveformChannels = if (outcome.channelPeaks.size == 2) previous.waveformChannels + (imported.id to outcome.channelPeaks) else previous.waveformChannels,
                    timelineControls = TimelineControlPolicy.normalizedForProject(previous.timelineControls, endFrame),
                    transportEngineReady = playbackReadiness(saved).ready,
                    masterGainDb = saved.masterGainDb,
                    trackLevelAnalysis = emptyMap(),
                    canUndo = projectHistory.canUndo,
                    canRedo = projectHistory.canRedo,
                    importStatus = if (imported.sourceSampleRateHz != imported.editingSampleRateHz) "${imported.sourceFormat} importado • ${outcome.channelCount} canal(is) • ${imported.sourceSampleRateHz} → ${imported.editingSampleRateHz} Hz" else "${imported.sourceFormat} importado • ${outcome.channelCount} canal(is) • ${outcome.sampleRateHz} Hz",
                    stereoImportPrompt = if (outcome.channelCount == 2) stereoPromptFor(saved, imported) else null,
                    error = null,
                )
            }.onFailure { error ->
                if (error is CancellationException) throw error
                _state.value = _state.value.copy(importing = false, error = error.message ?: "Não foi possível importar o áudio.", importStatus = null)
            }
        }
    }

    fun keepStereoImport() {
        val current = _state.value
        if (current.stereoImportPrompt == null) return
        _state.value = current.copy(stereoImportPrompt = null, importStatus = "Áudio estéreo mantido na pista")
    }

    fun separateStereoClip(clipId: String) {
        val current = _state.value
        val project = current.project ?: return
        val clip = project.clips.firstOrNull { it.id == clipId } ?: return
        if (clip.sourceChannelCount != 2 || !structuralEditingAllowed(current)) return
        viewModelScope.launch {
            _state.value = current.copy(importing = true, importStatus = "Separando canais L/R…", stereoImportPrompt = null, error = null)
            runCatching {
                withContext(Dispatchers.IO) {
                    val editingFile = mediaStore.resolveEditable(project.id, editingMediaPath(clip))
                    val leftTemp = File.createTempFile("guitarlab-L-", ".wav", getApplication<Application>().cacheDir)
                    val rightTemp = File.createTempFile("guitarlab-R-", ".wav", getApplication<Application>().cacheDir)
                    var leftProxyPath: String? = null
                    var rightProxyPath: String? = null
                    var committed = false
                    try {
                        val split = StereoWavChannelSplitter.split(editingFile, leftTemp, rightTemp)
                        val leftProxy = leftTemp.inputStream().buffered().use { mediaStore.ingestEditProxy(project.id, "${clip.name}-L.wav", it) }
                        leftProxyPath = leftProxy.relativePath
                        val rightProxy = rightTemp.inputStream().buffered().use { mediaStore.ingestEditProxy(project.id, "${clip.name}-R.wav", it) }
                        rightProxyPath = rightProxy.relativePath
                        val saved = withContext(NonCancellable) {
                            saveLatest(project.id) { latest ->
                                val sourceClip = latest.clips.firstOrNull { it.id == clipId } ?: error("O clipe estéreo não existe mais.")
                                val sourceTrack = latest.tracks.firstOrNull { it.id == sourceClip.trackId } ?: error("A pista de origem não existe mais.")
                                val pair = guitarPairFor(latest, sourceTrack)
                                val leftTrack: AudioTrack
                                val rightTrack: AudioTrack
                                val tracks: List<AudioTrack>
                                if (pair != null) {
                                    leftTrack = pair.first
                                    rightTrack = pair.second
                                    tracks = latest.tracks
                                } else {
                                    leftTrack = sourceTrack.copy(channelLayout = ChannelLayout.MONO, pan = -1f)
                                    rightTrack = sourceTrack.copy(
                                        id = UUID.randomUUID().toString(),
                                        name = "${sourceTrack.name} D",
                                        channelLayout = ChannelLayout.MONO,
                                        pan = 1f,
                                        order = sourceTrack.order + 1,
                                    )
                                    tracks = latest.tracks.map { track ->
                                        when {
                                            track.id == sourceTrack.id -> leftTrack
                                            track.order > sourceTrack.order -> track.copy(order = track.order + 1)
                                            else -> track
                                        }
                                    } + rightTrack
                                }
                                val routingProject = latest.copy(tracks = tracks.sortedBy { it.order })
                                StereoSeparationProjectPolicy.separate(
                                    project = routingProject,
                                    sourceClipId = sourceClip.id,
                                    leftTrackId = leftTrack.id,
                                    rightTrackId = rightTrack.id,
                                    leftProxyPath = leftProxy.relativePath,
                                    rightProxyPath = rightProxy.relativePath,
                                    splitTotalFrames = split.totalFrames,
                                    splitSampleRateHz = split.sampleRateHz,
                                    ids = StereoSeparationIds(
                                        leftClipId = UUID.randomUUID().toString(),
                                        rightClipId = UUID.randomUUID().toString(),
                                        leftTakeId = UUID.randomUUID().toString(),
                                        rightTakeId = UUID.randomUUID().toString(),
                                    ),
                                    nowEpochMs = System.currentTimeMillis(),
                                )
                            }.also { committed = true }
                        }
                        val waveformState = loadWaveformState(saved)
                        Triple(saved, waveformState.waveforms, waveformState.waveformChannels)
                    } catch (error: Throwable) {
                        if (!committed) {
                            leftProxyPath?.let { mediaStore.discardUncommitted(project.id, it) }
                            rightProxyPath?.let { mediaStore.discardUncommitted(project.id, it) }
                        }
                        throw error
                    } finally {
                        leftTemp.delete()
                        rightTemp.delete()
                    }
                }
            }.onSuccess { (saved, waveforms, waveformChannels) ->
                val end = TimelineControlPolicy.projectEndFrame(saved)
                _state.value = _state.value.copy(
                    importing = false,
                    project = saved,
                    waveforms = waveforms,
                    waveformChannels = waveformChannels,
                    timelineControls = TimelineControlPolicy.normalizedForProject(_state.value.timelineControls, end),
                    transportEngineReady = playbackReadiness(saved).ready,
                    trackLevelAnalysis = emptyMap(),
                    canUndo = projectHistory.canUndo,
                    canRedo = projectHistory.canRedo,
                    importStatus = "Estéreo separado em L/R mono com sincronismo preservado",
                    error = null,
                )
            }.onFailure { error ->
                if (error is CancellationException) throw error
                _state.value = _state.value.copy(importing = false, error = error.message ?: "Não foi possível separar os canais estéreo.", importStatus = null)
            }
        }
    }

    @Deprecated("Use importAudio")
    fun importWav(trackId: String, uri: Uri) = importAudio(trackId, uri)

    fun setPlayheadFrame(frame: Long) {
        val current = _state.value
        val project = current.project ?: return
        if (current.recordingSession.phase != RecordingSessionPhase.IDLE ||
            current.transport.mode == TransportMode.RECORDING ||
            current.trimControls != null ||
            current.historyBusy
        ) return
        val end = TimelineControlPolicy.projectEndFrame(project)
        val target = TransportPolicy.playbackSeekFrame(
            state = current.transport,
            requestedFrame = frame,
            projectEndFrame = end,
            loopStartFrame = current.timelineControls.loopStartFrame,
            loopEndFrame = current.timelineControls.loopEndFrame,
        )
        if (current.transport.mode == TransportMode.PLAYING) {
            playbackEngine.seekTo(target)
            _state.value = current.copy(timelineControls = current.timelineControls.copy(playheadFrame = target))
        } else {
            _state.value = current.copy(
                timelineControls = TimelineControlPolicy.movePlayhead(current.timelineControls, target, end),
            )
        }
    }

    fun setLoopStartFrame(frame: Long) = editTimelineMarker { state, end -> TimelineControlPolicy.moveLoopStart(state, frame, end) }
    fun setLoopEndFrame(frame: Long) = editTimelineMarker { state, end -> TimelineControlPolicy.moveLoopEnd(state, frame, end) }

    fun beginTrim(clipId: String) {
        val current = _state.value
        val project = current.project ?: run {
            _state.value = current.copy(error = "O projeto ainda não está disponível para corte.")
            return
        }
        val blockedReason = when {
            current.importing -> "Aguarde a importação terminar antes de cortar."
            current.editingClip -> "Aguarde a edição atual terminar antes de cortar."
            current.historyBusy -> "Aguarde Desfazer/Refazer terminar antes de cortar."
            !TransportPolicy.timelineEditingEnabled(current.transport) -> "Pare a reprodução antes de cortar."
            else -> null
        }
        if (blockedReason != null) {
            _state.value = current.copy(error = blockedReason)
            return
        }
        val clip = project.clips.firstOrNull { it.id == clipId } ?: run {
            _state.value = current.copy(error = "O clipe selecionado não existe mais.")
            return
        }
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
                    trackLevelAnalysis = emptyMap(),
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
        val current = _state.value
        if (current.historyBusy || current.recordingSession.phase != RecordingSessionPhase.IDLE || current.transport.mode == TransportMode.RECORDING) return
        if (current.transport.mode == TransportMode.PLAYING) {
            stopPlaybackSession()
            val reset = current.copy(transport = current.transport.copy(mode = TransportMode.STOPPED), timelineControls = current.timelineControls.copy(playheadFrame = 0L))
            _state.value = reset
            startPlayback(reset)
        } else setPlayheadFrame(0L)
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
        val sessionGeneration = projectSessionGeneration
        val target = projectHistory.undo(currentProject) ?: return
        _state.value = currentState.copy(historyBusy = true, error = null)
        viewModelScope.launch {
            runCatching { persistHistorySnapshot(target) }
                .onSuccess { (saved, waveformState) ->
                    if (sessionGeneration != projectSessionGeneration || _state.value.project?.id != currentProject.id) return@onSuccess
                    applyHistorySnapshot(saved, waveformState, "Alteração desfeita")
                }
                .onFailure { error ->
                    if (sessionGeneration != projectSessionGeneration || _state.value.project?.id != currentProject.id) return@onFailure
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
        val sessionGeneration = projectSessionGeneration
        val target = projectHistory.redo(currentProject) ?: return
        _state.value = currentState.copy(historyBusy = true, error = null)
        viewModelScope.launch {
            runCatching { persistHistorySnapshot(target) }
                .onSuccess { (saved, waveformState) ->
                    if (sessionGeneration != projectSessionGeneration || _state.value.project?.id != currentProject.id) return@onSuccess
                    applyHistorySnapshot(saved, waveformState, "Alteração refeita")
                }
                .onFailure { error ->
                    if (sessionGeneration != projectSessionGeneration || _state.value.project?.id != currentProject.id) return@onFailure
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
        when (current.recordingSession.phase) {
            RecordingSessionPhase.COUNTDOWN -> {
                cancelRecordingCountdown()
                return
            }
            RecordingSessionPhase.CAPTURING -> {
                stopRecording()
                return
            }
            RecordingSessionPhase.FINALIZING -> return
            RecordingSessionPhase.IDLE -> Unit
        }
        if (current.historyBusy) return
        when (current.transport.mode) {
            TransportMode.PLAYING -> {
                stopPlaybackSession()
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

    fun showLoopRecordingChoice() {
        val current = _state.value
        if (current.project == null ||
            current.recordingSession.phase != RecordingSessionPhase.IDLE ||
            !current.transport.loopEnabled
        ) return
        if (!current.loopRecordingChoiceVisible) {
            _state.value = current.copy(loopRecordingChoiceVisible = true)
        }
    }

    fun dismissLoopRecordingChoice() {
        val current = _state.value
        if (current.loopRecordingChoiceVisible) {
            _state.value = current.copy(loopRecordingChoiceVisible = false)
        }
    }

    fun startRecording(mode: PracticeRecordingMode = PracticeRecordingMode.CURRENT_PLAYHEAD) {
        val snapshot = _state.value
        val current = if (snapshot.loopRecordingChoiceVisible) {
            snapshot.copy(loopRecordingChoiceVisible = false).also { _state.value = it }
        } else snapshot
        when (current.recordingSession.phase) {
            RecordingSessionPhase.COUNTDOWN -> cancelRecordingCountdown()
            RecordingSessionPhase.CAPTURING -> stopRecording()
            RecordingSessionPhase.FINALIZING -> Unit
            RecordingSessionPhase.IDLE -> {
                val project = current.project ?: return
                val sampleRate = project.sampleRate.fixedHz ?: playbackReadiness(project).sampleRateHz ?: 48_000
                val plan = runCatching {
                    PracticeRecordingStartPolicy.plan(
                        mode = mode,
                        currentPlayheadFrame = current.timelineControls.playheadFrame,
                        loopEnabled = current.transport.loopEnabled,
                        loopStartFrame = current.timelineControls.loopStartFrame,
                        loopEndFrame = current.timelineControls.loopEndFrame,
                        sampleRateHz = sampleRate,
                    )
                }.getOrElse { error ->
                    _state.value = current.copy(error = error.message ?: "Não foi possível preparar a gravação.")
                    return
                }
                pendingRecordingPlan = plan
                val prepared = current.copy(
                    transport = current.transport.copy(loopEnabled = plan.loopEnabled),
                    timelineControls = if (mode == PracticeRecordingMode.FROM_PROJECT_START) {
                        current.timelineControls.copy(playheadFrame = 0L)
                    } else current.timelineControls,
                )
                _state.value = prepared
                beginRecordingCountdown(prepared, plan.sessionStartFrame)
            }
        }
    }

    fun onRecordPermissionResult(granted: Boolean) {
        if (granted) startRecording() else {
            pendingRecordingPlan = null
            _state.value = _state.value.copy(error = "A permissão do microfone é necessária para gravar.")
        }
    }

    private fun beginRecordingCountdown(current: StudioUiState, captureStartFrame: Long) {
        val project = current.project ?: return
        if (!structuralEditingAllowed(current)) return
        if (getApplication<Application>().checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            pendingRecordingPlan = null
            _state.value = current.copy(error = "Autorize o microfone para iniciar a gravação.")
            return
        }
        val session = runCatching {
            RecordingTargetPolicy.resolve(project)
            RecordingSessionPolicy.begin(project, captureStartFrame.coerceAtLeast(0L))
        }.getOrElse { error ->
            pendingRecordingPlan = null
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
            liveRecordingWaveform = emptyList(),
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
        pendingRecordingPlan = null
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
            recordingMediaStore.begin(
                projectId = project.id,
                suggestedName = "take-${System.currentTimeMillis()}.wav",
                targetTrackId = session.targetTrackId,
                requestedTimelineStartFrame = session.timelineStartFrame,
            )
        }.getOrElse { error ->
            resetRecording(error.message ?: "Não foi possível abrir a transação da gravação.")
            return
        }
        recordingTransaction = transaction
        activeRecordingRouteState = AudioRouteSessionPolicy.starting(
            selectedInputIdentity = audioRoutingStore.selectedInputDiagnosticIdentity(),
            selectedOutputIdentity = audioRoutingStore.selectedOutputDiagnosticIdentity(),
        )
        recordingProgressGeneration += 1L
        liveWaveform.clear()
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
        stopPlaybackSession()
        activeRecordingRouteState = activeRecordingRouteState?.let(AudioRouteSessionPolicy::stopped)
        recordingEngine.stop()
    }

    private val recordingListener = object : StudioRecordingListener {
        override fun onStarted(config: StudioRecordingConfig) = viewModelScope.launch {
            val state = _state.value
            if (!state.recordingSession.readyToOpenCapture) return@launch
            val inputSignature = audioRoutingStore.selectedInputSignature()
            val outputSignature = audioRoutingStore.selectedOutputSignature()
            activeRecordingSelectedInputIdentity = audioRoutingStore.selectedInputDiagnosticIdentity()
            activeRecordingSelectedOutputIdentity = audioRoutingStore.selectedOutputDiagnosticIdentity()
            activeRecordingEffectiveInputIdentity = config.routedInputLabel
            activeRecordingEffectiveOutputIdentity = null
            activeRecordingRouteState = AudioRouteSessionPolicy.confirmed(
                activeRecordingRouteState ?: AudioRouteSessionPolicy.starting(activeRecordingSelectedInputIdentity, activeRecordingSelectedOutputIdentity),
                effectiveInputIdentity = config.routedInputLabel,
                effectiveOutputIdentity = null,
            )
            activeRecordingCaptureAnchorJitterNs = config.captureAnchorJitterNs
            activeRecordingCaptureAnchorObservations = config.captureAnchorObservations
            activeRecordingBackingAnchorJitterNs = null
            activeRecordingBackingAnchorObservations = null
            activeRecordingTimingBasis = RecordingTimingEvidenceBasis.NO_BACKING_REFERENCE
            activeRecordingOutputFallback = false
            activeRecordingRouteChanged = false
            activeRecordingOutputUnderruns = null
            activeRecordingRouteLatencyFrames = latencyCalibrationStore.find(
                inputSignature = inputSignature,
                outputSignature = outputSignature,
                sampleRateHz = config.sampleRateHz,
            )?.takeIf { it.accepted }?.latencyFrames ?: 0L
            activeRecordingFineAdjustmentFrames = latencyCalibrationStore.fineAdjustmentFrames(
                inputSignature = inputSignature,
                outputSignature = outputSignature,
                sampleRateHz = config.sampleRateHz,
            )
            activeRecordingCaptureStartNs = config.captureStartMonotonicNs
            activeRecordingCaptureTimestampBased = config.captureTimestampBased
            activeRecordingClockOffsetFrames = 0L
            val launchPlan = pendingRecordingPlan
            activePunchRegion = launchPlan?.punchRegion
            val initialTailCompensation = (activeRecordingRouteLatencyFrames + activeRecordingFineAdjustmentFrames).coerceAtLeast(0L)
            activePunchPlan = activePunchRegion?.let { PunchRecordingPolicy.plan(it, initialTailCompensation) }
            pendingRecordingPlan = null
            val targetId = state.recordingSession.targetTrackId
            _state.value = state.copy(
                recordingSession = RecordingSessionPolicy.markCaptureStarted(state.recordingSession),
                recordingConfig = config,
                transport = TransportPolicy.startRecording(state.transport),
                recordingPeak = 0f,
                recordingRms = 0f,
                liveRecordingWaveform = emptyList(),
                trackMeters = if (targetId != null) mapOf(targetId to MeterBallisticsState()) else emptyMap(),
                masterMeter = MeterBallisticsPolicy.reset(),
                clipStatus = "Gravando • ${friendlySelectedInputLabel()} • ${config.sampleRateHz} Hz • ${config.channelCount} canal(is)",
            )
            if (!startBackingPlaybackForRecording(config.sampleRateHz)) {
                activeRecordingRouteLatencyFrames = 0L
                activeRecordingFineAdjustmentFrames = 0L
                activeRecordingClockOffsetFrames = 0L
                activePunchPlan = activePunchRegion?.let { PunchRecordingPolicy.plan(it, 0L) }
            }
        }.let { Unit }

        override fun onProgress(framesCaptured: Long, peak: Float, rms: Float) {
            liveWaveform.append(framesCaptured, peak)
            recordingProgressChannel.trySend(
                RecordingProgressUpdate(
                    generation = recordingProgressGeneration,
                    framesCaptured = framesCaptured,
                    peak = peak,
                    rms = rms,
                )
            )
        }

        override fun onStopped(result: StudioRecordingResult) {
            finalizeRecording(result)
        }

        override fun onError(message: String) = viewModelScope.launch {
            recordingTransaction?.let(recordingMediaStore::discard)
            recordingTransaction = null
            resetRecording(message)
        }.let { Unit }

        override fun onWarning(message: String) = viewModelScope.launch {
            postTransientWarning(
                message,
                "O monitoramento por software ficou indisponível; a gravação continua sem retorno pelo app.",
            )
        }.let { Unit }
    }

    private fun finalizeRecording(result: StudioRecordingResult) {
        val transaction = recordingTransaction ?: return resetRecording("A transação do take foi perdida.")
        recordingTransaction = null
        val punchRegionForTake = activePunchRegion
        val clockOffsetForTake = activeRecordingClockOffsetFrames
        val routeLatencyForTake = activeRecordingRouteLatencyFrames
        val fineAdjustmentForTake = activeRecordingFineAdjustmentFrames
        val timingBasisForTake = activeRecordingTimingBasis
        val selectedInputIdentityForTake = activeRecordingSelectedInputIdentity
        val effectiveInputIdentityForTake = activeRecordingEffectiveInputIdentity
        val selectedOutputIdentityForTake = activeRecordingSelectedOutputIdentity
        val effectiveOutputIdentityForTake = activeRecordingEffectiveOutputIdentity
        val captureAnchorJitterForTake = activeRecordingCaptureAnchorJitterNs
        val backingAnchorJitterForTake = activeRecordingBackingAnchorJitterNs
        val captureAnchorObservationsForTake = activeRecordingCaptureAnchorObservations
        val backingAnchorObservationsForTake = activeRecordingBackingAnchorObservations
        val outputFallbackForTake = activeRecordingOutputFallback
        val routeChangedForTake = activeRecordingRouteChanged || result.stopReason == studio.guitarlab.platform.audio.android.StudioRecordingStopReason.ROUTE_LOST
        val outputUnderrunsForTake = activeRecordingOutputUnderruns
        if (result.stopReason == studio.guitarlab.platform.audio.android.StudioRecordingStopReason.ROUTE_LOST) {
            activeRecordingRouteState = AudioRouteSessionPolicy.lost(activeRecordingRouteState ?: AudioRouteSessionPolicy.starting(selectedInputIdentityForTake, selectedOutputIdentityForTake), "selected input route lost")
        }
        activePunchPlan = null
        activePunchRegion = null
        pendingRecordingPlan = null
        activeRecordingCaptureStartNs = 0L
        activeRecordingCaptureTimestampBased = false
        activeRecordingClockOffsetFrames = 0L
        activeRecordingRouteLatencyFrames = 0L
        activeRecordingFineAdjustmentFrames = 0L
        activeRecordingRouteState = activeRecordingRouteState?.let(AudioRouteSessionPolicy::stopped)
        resetActiveRecordingHealthEvidence()
        viewModelScope.launch(start = CoroutineStart.UNDISPATCHED) {
            var committed = false
            runCatching {
                withContext(Dispatchers.IO) {
                    val before = repository.load(transaction.projectId) ?: error("Projeto não encontrado ao finalizar o take.")
                    val session = _state.value.recordingSession
                    val targetTrackId = requireNotNull(session.targetTrackId) { "A pista do take não está mais disponível." }
                    val placement = RecordingTimingCompensationPolicy.compensate(
                        requestedTimelineStartFrame = session.timelineStartFrame,
                        capturedFrames = result.framesCaptured,
                        startupOffsetFrames = clockOffsetForTake,
                        roundTripLatencyFrames = routeLatencyForTake,
                        fineAdjustmentFrames = fineAdjustmentForTake,
                    )
                    val punch = punchRegionForTake?.takeIf { !result.partial }?.let { region ->
                        PunchRecordingPolicy.keepWindow(
                            region = region,
                            takeTimelineStartFrame = placement.timelineStartFrame,
                            takeSourceStartFrame = placement.sourceStartFrame,
                            capturedFrames = result.framesCaptured,
                        )
                    }
                    val timelineStart = punch?.timelineStartFrame ?: placement.timelineStartFrame
                    val sourceStart = punch?.sourceStartFrame ?: placement.sourceStartFrame
                    val length = punch?.lengthFrames ?: placement.lengthFrames
                    recordingMediaStore.preparePublication(
                        transaction = transaction,
                        targetTrackId = targetTrackId,
                        finalTimelineStartFrame = timelineStart,
                        finalSourceStartFrame = sourceStart,
                        finalLengthFrames = length,
                        sampleRateHz = result.sampleRateHz,
                        channelCount = result.channelCount,
                        framesCaptured = result.framesCaptured,
                    )
                    recordingMediaStore.commit(transaction)
                    val integrated = RecordedTakeProjectIntegrator.integrate(
                        project = before,
                        targetTrackId = targetTrackId,
                        take = RecordedTakeMetadata(
                            clipId = transaction.id,
                            displayName = "Take ${before.takes.count { it.trackId == targetTrackId } + 1}",
                            managedRelativePath = transaction.relativePath,
                            timelineStartFrame = timelineStart,
                            sampleRateHz = result.sampleRateHz,
                            channelCount = result.channelCount,
                            framesCaptured = result.framesCaptured,
                        ),
                        nowEpochMs = System.currentTimeMillis(),
                    )
                    val saved = integrated.copy(
                        clips = integrated.clips.map { candidate ->
                            if (candidate.id == transaction.id) candidate.copy(
                                sourceStartFrame = sourceStart,
                                lengthFrames = length,
                            ) else candidate
                        },
                        updatedAtEpochMs = System.currentTimeMillis(),
                    )
                    val clip = saved.clips.first { it.id == transaction.id }
                    val envelope = FileSeekableByteSource(transaction.finalFile).use { source ->
                        WaveformEnvelopeBuilder.build(
                            decoder = WavPcmDecoder(source),
                            targetPoints = WAVEFORM_POINTS,
                            startFrame = clip.sourceStartFrame,
                            frameCount = clip.lengthFrames,
                        )
                    }
                    waveformCache.write(
                        saved.id,
                        clip.id,
                        WaveformCacheIdentity.forClip(clip, WAVEFORM_POINTS),
                        envelope,
                    )
                    val persisted = withContext(NonCancellable) {
                        repository.save(saved).also {
                            committed = true
                            projectHistory.record(before, it)
                            // Publication is already durable. Marker cleanup may safely converge on next open.
                            runCatching { recordingMediaStore.markPublished(transaction) }
                        }
                    }
                    Triple(persisted, clip, envelope)
                }
            }.onSuccess { (saved, clip, envelope) ->
                BackupScheduler.enqueueCoalesced(getApplication())
                val state = _state.value
                val end = TimelineControlPolicy.projectEndFrame(saved)
                recordingSessionHealthStore.append(
                    RecordingSessionHealthRecord(
                        completedAtEpochMs = System.currentTimeMillis(),
                        selectedInputIdentity = selectedInputIdentityForTake,
                        effectiveInputIdentity = effectiveInputIdentityForTake,
                        selectedOutputIdentity = selectedOutputIdentityForTake,
                        effectiveOutputIdentity = effectiveOutputIdentityForTake,
                        sampleRateHz = result.sampleRateHz,
                        timingEvidenceBasis = timingBasisForTake,
                        captureAnchorJitterNs = captureAnchorJitterForTake,
                        backingAnchorJitterNs = backingAnchorJitterForTake,
                        captureAnchorObservations = captureAnchorObservationsForTake,
                        backingAnchorObservations = backingAnchorObservationsForTake,
                        sessionDeltaFrames = clockOffsetForTake,
                        acceptedRouteLatencyFrames = routeLatencyForTake,
                        residualFineAdjustmentFrames = fineAdjustmentForTake,
                        outputFallback = outputFallbackForTake,
                        routeChanged = routeChangedForTake,
                        capturedFrames = result.framesCaptured,
                        finalTimelineStartFrame = clip.startFrame,
                        finalSourceStartFrame = clip.sourceStartFrame,
                        finalLengthFrames = clip.lengthFrames,
                        inputZeroReadEvents = result.inputZeroReadEvents,
                        outputUnderrunCount = outputUnderrunsForTake,
                        completed = !result.partial,
                        failureReason = result.message?.takeIf { result.partial },
                    )
                )
                _state.value = state.copy(
                    project = saved,
                    waveforms = state.waveforms + (clip.id to envelope.peaks),
                    waveformChannels = if (envelope.channelPeaks.size == 2) {
                        state.waveformChannels + (clip.id to envelope.channelPeaks)
                    } else {
                        state.waveformChannels - clip.id
                    },
                    timelineControls = TimelineControlPolicy.normalizedForProject(state.timelineControls.copy(playheadFrame = clip.startFrame + clip.lengthFrames), end),
                    transport = state.transport.copy(mode = TransportMode.STOPPED),
                    recordingSession = RecordingSessionPolicy.reset(),
                    recordingConfig = null,
                    recordingPeak = 0f,
                    recordingRms = 0f,
                    liveRecordingWaveform = emptyList(),
                    masterMeter = MeterBallisticsPolicy.reset(),
                    trackMeters = emptyMap(),
                    transportEngineReady = playbackReadiness(saved).ready,
                    trackLevelAnalysis = emptyMap(),
                    canUndo = projectHistory.canUndo,
                    canRedo = projectHistory.canRedo,
                    clipStatus = when {
                        result.partial -> "Take parcial preservado com segurança"
                        clockOffsetForTake != 0L || routeLatencyForTake > 0L || fineAdjustmentForTake != 0L ->
                            "Take sincronizado automaticamente"
                        else -> "Take gravado com sucesso"
                    },
                    error = result.message?.takeIf { result.partial },
                )
                liveWaveform.clear()
            }.onFailure { error ->
                if (!committed) {
                    waveformCache.remove(transaction.projectId, transaction.id)
                    recordingMediaStore.discard(transaction)
                }
                recordingSessionHealthStore.append(
                    RecordingSessionHealthRecord(
                        completedAtEpochMs = System.currentTimeMillis(),
                        selectedInputIdentity = selectedInputIdentityForTake,
                        effectiveInputIdentity = effectiveInputIdentityForTake,
                        selectedOutputIdentity = selectedOutputIdentityForTake,
                        effectiveOutputIdentity = effectiveOutputIdentityForTake,
                        sampleRateHz = result.sampleRateHz,
                        timingEvidenceBasis = timingBasisForTake,
                        captureAnchorJitterNs = captureAnchorJitterForTake,
                        backingAnchorJitterNs = backingAnchorJitterForTake,
                        captureAnchorObservations = captureAnchorObservationsForTake,
                        backingAnchorObservations = backingAnchorObservationsForTake,
                        sessionDeltaFrames = clockOffsetForTake,
                        acceptedRouteLatencyFrames = routeLatencyForTake,
                        residualFineAdjustmentFrames = fineAdjustmentForTake,
                        outputFallback = outputFallbackForTake,
                        routeChanged = routeChangedForTake,
                        capturedFrames = result.framesCaptured,
                        finalTimelineStartFrame = null,
                        finalSourceStartFrame = null,
                        finalLengthFrames = null,
                        inputZeroReadEvents = result.inputZeroReadEvents,
                        outputUnderrunCount = outputUnderrunsForTake,
                        completed = false,
                        failureReason = error.message ?: result.message,
                    )
                )
                resetRecording(error.message ?: "Não foi possível integrar o take ao projeto.")
            }
        }
    }

    private fun resetRecording(message: String) {
        countdownJob?.cancel()
        countdownJob = null
        pendingRecordingPlan = null
        activePunchPlan = null
        activePunchRegion = null
        activeRecordingCaptureStartNs = 0L
        activeRecordingCaptureTimestampBased = false
        activeRecordingClockOffsetFrames = 0L
        activeRecordingRouteLatencyFrames = 0L
        activeRecordingFineAdjustmentFrames = 0L
        resetActiveRecordingHealthEvidence()
        val state = _state.value
        _state.value = state.copy(
            transport = state.transport.copy(mode = TransportMode.STOPPED),
            recordingSession = RecordingSessionPolicy.reset(),
            recordingConfig = null,
            recordingPeak = 0f,
            recordingRms = 0f,
            liveRecordingWaveform = emptyList(),
            masterMeter = MeterBallisticsPolicy.reset(),
            trackMeters = emptyMap(),
            clipStatus = null,
            error = message,
        )
        recordingProgressGeneration += 1L
        liveWaveform.clear()
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

    fun setClipFades(clipId: String, fadeInFrames: Long, fadeOutFrames: Long) {
        editClip("Fades do clipe atualizados") { current ->
            ProjectClipEditor.setClipFades(current, clipId, fadeInFrames, fadeOutFrames, System.currentTimeMillis())
        }
    }

    fun crossfadeWithNext(clipId: String) {
        val project = _state.value.project ?: return
        val clip = project.clips.firstOrNull { it.id == clipId } ?: return
        val next = project.clips.filter { it.trackId == clip.trackId && it.id != clip.id && it.startFrame >= clip.startFrame }
            .minByOrNull { it.startFrame } ?: run {
                _state.value = _state.value.copy(error = "Não há outro clipe sobreposto à direita para crossfade.")
                return
            }
        editClip("Crossfade aplicado") { current -> ProjectClipEditor.crossfadeOverlappingClips(current, clipId, next.id, System.currentTimeMillis()) }
    }

    fun moveClipToTrack(clipId: String, targetTrackId: String) {
        val current = _state.value.project ?: return
        val clip = current.clips.firstOrNull { it.id == clipId } ?: return
        if (clip.trackId == targetTrackId) return
        val expectedSourceTrackId = clip.trackId
        editClip("Clipe movido para outra pista") { latest ->
            val latestClip = latest.clips.firstOrNull { it.id == clipId }
                ?: error("O clipe não existe mais; o movimento foi cancelado com segurança.")
            require(latestClip.trackId == expectedSourceTrackId) {
                "O clipe mudou de pista durante o arraste; tente novamente."
            }
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

    private fun postTransientFeedback(
        kind: TransientFeedbackKind,
        message: String,
        fallback: String,
        cooldownMs: Long = 0L,
    ) {
        if (!AppTransientFeedbackPolicy.shouldShowSnackbar(kind)) return
        val safe = AppTransientFeedbackPolicy.userSafe(message, fallback)
        val now = System.currentTimeMillis()
        if (kind == TransientFeedbackKind.WARNING && safe == lastTransientWarning && now - lastTransientWarningAtMs < cooldownMs) return
        if (kind == TransientFeedbackKind.WARNING) {
            lastTransientWarning = safe
            lastTransientWarningAtMs = now
        }
        _state.value = _state.value.copy(transientNotice = safe, transientNoticeKind = kind)
    }

    private fun postTransientWarning(message: String, fallback: String, cooldownMs: Long = 30_000L) =
        postTransientFeedback(TransientFeedbackKind.WARNING, message, fallback, cooldownMs)

    private fun postTransientCompletion(message: String) =
        postTransientFeedback(TransientFeedbackKind.ASYNC_COMPLETION, message, "Operação concluída.")

    fun dismissTransientMessage(message: String) {
        val current = _state.value
        when (message) {
            current.error -> _state.value = current.copy(error = null)
            current.transientNotice -> _state.value = current.copy(
                transientNotice = null,
                transientNoticeKind = TransientFeedbackKind.WARNING,
            )
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

    fun toggleTrackMuted(trackId: String) = editTrackAudibility(trackId, toggleMute = true)

    fun toggleTrackSolo(trackId: String) = editTrackAudibility(trackId, toggleMute = false)

    fun toggleTrackCue(trackId: String) {
        val state = _state.value
        val project = state.project ?: return
        if (state.importing || state.editingClip || state.historyBusy || state.trimControls != null) return
        viewModelScope.launch {
            runCatching {
                saveLatest(project.id) { latest ->
                    val target = latest.tracks.firstOrNull { it.id == trackId } ?: return@saveLatest latest
                    val updated = target.copy(
                        outputRoute = TrackOutputRoutingPolicy.toggleExclusiveCue(target.outputRoute),
                    )
                    latest.copy(
                        tracks = latest.tracks.map { if (it.id == trackId) updated else it },
                        updatedAtEpochMs = System.currentTimeMillis(),
                    )
                }
            }.onSuccess { saved ->
                val route = saved.tracks.firstOrNull { it.id == trackId }?.outputRoute ?: TrackOutputRoute.MAIN
                playbackEngine.setTrackOutputRoute(trackId, route)
                applySavedProject(
                    saved,
                    if (route == TrackOutputRoute.CUE) "Pista enviada para a saída CUE" else "Pista enviada para a saída principal",
                )
                if (TrackOutputRoutingPolicy.sendsToCue(route) && audioRoutingStore.resolveSelectedCueOutputDevice() == null) {
                    postTransientWarning(
                        "A pista está marcada para CUE, mas nenhuma saída secundária válida está disponível; ela ficará silenciosa fora do MAIN.",
                        "A pista CUE ficará silenciosa até uma saída secundária válida ser selecionada.",
                    )
                }
            }.onFailure { error ->
                _state.value = _state.value.copy(error = error.message ?: "Não foi possível atualizar a saída da pista.")
            }
        }
    }

    fun toggleTrackArmed(trackId: String) {
        editTrackStructural("Armar gravação atualizado", trackId) { it.copy(armed = !it.armed) }
    }

    fun updateTrackProperties(trackId: String, name: String, colorIndex: Int) {
        val roleId = _state.value.project?.tracks?.firstOrNull { it.id == trackId }?.roleId
        updateTrackConfiguration(trackId, name, colorIndex, roleId)
    }

    fun updateTrackConfiguration(trackId: String, name: String, colorIndex: Int, roleId: String?) {
        val currentState = _state.value
        val project = currentState.project ?: return
        if (!structuralEditingAllowed(currentState)) return
        val normalizedName = runCatching { TrackNamePolicy.requireValid(name) }.getOrElse { error ->
            _state.value = currentState.copy(error = error.message ?: "Nome de pista inválido.")
            return
        }
        viewModelScope.launch {
            runCatching {
                saveLatest(project.id) { latest ->
                    val renamed = latest.copy(
                        tracks = latest.tracks.map { track ->
                            if (track.id == trackId) track.copy(
                                name = normalizedName,
                                colorIndex = colorIndex.coerceIn(0, TRACK_COLOR_COUNT - 1),
                            ) else track
                        },
                        updatedAtEpochMs = System.currentTimeMillis(),
                    )
                    TrackRoleAssignmentPolicy.assign(
                        project = renamed,
                        trackId = trackId,
                        roleId = roleId,
                        nowEpochMs = System.currentTimeMillis(),
                    )
                }
            }.onSuccess { saved ->
                saved.tracks.firstOrNull { it.id == trackId }?.let { track ->
                    trackMixDrafts[track.id] = TrackMixDraft(track.gainDb, track.pan)
                }
                applySavedProject(saved, "Pista atualizada")
            }.onFailure { error ->
                _state.value = _state.value.copy(error = error.message ?: "Não foi possível atualizar a pista.")
            }
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
        val newTrackId = UUID.randomUUID().toString()
        viewModelScope.launch {
            runCatching {
                saveLatest(project.id) { latest ->
                    val nextOrder = (latest.tracks.maxOfOrNull { it.order } ?: -1) + 1
                    val number = latest.tracks.size + 1
                    val newTrack = AudioTrack(
                        id = newTrackId,
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
                saved.tracks.forEach { track -> trackMixDrafts[track.id] = TrackMixDraft(track.gainDb, track.pan) }
                val shouldPrompt = TrackRoleAssignmentPolicy.availableSuggestionsForNewTrack(saved, newTrackId).isNotEmpty()
                applySavedProject(saved, "Nova pista adicionada")
                if (shouldPrompt) {
                    _state.value = _state.value.copy(newTrackRolePromptId = newTrackId)
                }
            }.onFailure { error -> _state.value = _state.value.copy(error = error.message ?: "Não foi possível adicionar a pista.") }
        }
    }

    fun dismissNewTrackRolePrompt() {
        _state.value = _state.value.copy(newTrackRolePromptId = null)
    }

    fun assignNewTrackRole(trackId: String, roleId: String) {
        val current = _state.value
        val project = current.project ?: return
        if (!structuralEditingAllowed(current) || current.newTrackRolePromptId != trackId) return
        // Close first so rapid/double taps cannot queue multiple competing assignments.
        _state.value = current.copy(newTrackRolePromptId = null)
        viewModelScope.launch {
            runCatching {
                saveLatest(project.id) { latest ->
                    TrackRoleAssignmentPolicy.assign(
                        project = latest,
                        trackId = trackId,
                        roleId = roleId,
                        nowEpochMs = System.currentTimeMillis(),
                    )
                }
            }.onSuccess { saved ->
                saved.tracks.firstOrNull { it.id == trackId }?.let { track ->
                    trackMixDrafts[track.id] = TrackMixDraft(track.gainDb, track.pan)
                }
                applySavedProject(saved, "Função da pista definida")
            }.onFailure { error ->
                _state.value = _state.value.copy(error = error.message ?: "Não foi possível definir a função da pista.")
            }
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
                if (_state.value.newTrackRolePromptId == trackId) {
                    _state.value = _state.value.copy(newTrackRolePromptId = null)
                }
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
                applySavedProject(saved, "Ganho master atualizado")
            }.onFailure { error ->
                _state.value = _state.value.copy(error = error.message ?: "Não foi possível atualizar o master.")
            }
        }
    }

    fun clearMasterClipIndicator() {
        _state.value = _state.value.copy(masterClipLatched = false)
    }

    fun clearTrackClipIndicator(trackId: String) {
        _state.value = _state.value.copy(trackClipLatched = _state.value.trackClipLatched - trackId)
    }

    fun setGuitarAuditionMode(mode: GuitarAuditionMode) {
        playbackEngine.setAuditionMode(mode)
        _state.value = _state.value.copy(guitarAuditionMode = mode)
    }

    fun recoverInterruptedRecording(transactionId: String) {
        val current = _state.value
        val project = current.project ?: return
        val candidate = recoveryCandidatesById[transactionId] ?: return
        if (current.recoveryBusy || !structuralEditingAllowed(current)) return
        viewModelScope.launch {
            _state.value = _state.value.copy(recoveryBusy = true, error = null, clipStatus = "Recuperando gravação interrompida…")
            runCatching {
                withContext(Dispatchers.IO) {
                    val latest = repository.load(project.id) ?: error("Projeto não encontrado durante a recuperação.")
                    when (RecordingRecoveryPolicy.publicationDecision(latest, candidate)) {
                        RecordingRecoveryPublicationDecision.ALREADY_PUBLISHED -> {
                            runCatching { recordingMediaStore.markPublished(project.id, transactionId) }
                            val clip = latest.clips.firstOrNull { it.id == transactionId }
                            Triple(latest, clip, clip?.let { loadWaveformEnvelope(latest, it) })
                        }
                        RecordingRecoveryPublicationDecision.RECOVERABLE -> {
                            val marker = requireNotNull(candidate.marker) { "Marker de recuperação ausente." }
                            val targetTrackId = requireNotNull(marker.targetTrackId)
                            val media = recordingMediaStore.promoteRecovery(candidate)
                            val timelineStart = RecordingRecoveryPolicy.timelineStart(candidate)
                            val sourceStart = RecordingRecoveryPolicy.sourceStart(candidate)
                            val length = RecordingRecoveryPolicy.lengthFrames(candidate)
                            val sampleRate = requireNotNull(candidate.sampleRateHz)
                            val channels = requireNotNull(candidate.channelCount)
                            val frames = requireNotNull(candidate.frames)
                            val saved = saveLatest(project.id) { before ->
                                if (before.clips.any { it.id == transactionId }) before else {
                                    val integrated = RecordedTakeProjectIntegrator.integrate(
                                        project = before,
                                        targetTrackId = targetTrackId,
                                        take = RecordedTakeMetadata(
                                            clipId = transactionId,
                                            displayName = "Take recuperado ${before.takes.count { it.trackId == targetTrackId } + 1}",
                                            managedRelativePath = candidate.relativePath ?: error("Destino gerenciado ausente."),
                                            timelineStartFrame = timelineStart,
                                            sampleRateHz = sampleRate,
                                            channelCount = channels,
                                            framesCaptured = frames,
                                        ),
                                        nowEpochMs = System.currentTimeMillis(),
                                    )
                                    integrated.copy(
                                        clips = integrated.clips.map { clip ->
                                            if (clip.id == transactionId) clip.copy(sourceStartFrame = sourceStart, lengthFrames = length) else clip
                                        },
                                        updatedAtEpochMs = System.currentTimeMillis(),
                                    )
                                }
                            }
                            val clip = saved.clips.first { it.id == transactionId }
                            val envelope = FileSeekableByteSource(media).use { source ->
                                WaveformEnvelopeBuilder.build(
                                    decoder = WavPcmDecoder(source),
                                    targetPoints = WAVEFORM_POINTS,
                                    startFrame = clip.sourceStartFrame,
                                    frameCount = clip.lengthFrames,
                                )
                            }
                            waveformCache.write(
                                saved.id,
                                clip.id,
                                WaveformCacheIdentity.forClip(clip, WAVEFORM_POINTS),
                                envelope,
                            )
                            runCatching { recordingMediaStore.markPublished(saved.id, transactionId) }
                            Triple(saved, clip, envelope)
                        }
                        RecordingRecoveryPublicationDecision.TARGET_TRACK_MISSING -> error("A pista original desta gravação não existe mais. O áudio foi preservado para suporte.")
                        RecordingRecoveryPublicationDecision.UNSAFE_PAYLOAD -> error(candidate.diagnosticReason ?: "A gravação interrompida não pôde ser validada com segurança.")
                    }
                }
            }.onSuccess { (saved, clip, envelope) ->
                recoveryCandidatesById.remove(transactionId)
                val nextItems = _state.value.recoveryItems.filterNot { it.transactionId == transactionId }
                val currentWaveforms = _state.value.waveforms
                val currentChannels = _state.value.waveformChannels
                _state.value = _state.value.copy(
                    recoveryBusy = false,
                    recoveryItems = nextItems,
                    project = saved,
                    waveforms = if (clip != null && envelope != null) currentWaveforms + (clip.id to envelope.peaks) else currentWaveforms,
                    waveformChannels = if (clip != null && envelope?.channelPeaks?.size == 2) {
                        currentChannels + (clip.id to envelope.channelPeaks)
                    } else if (clip != null) {
                        currentChannels - clip.id
                    } else {
                        currentChannels
                    },
                    transportEngineReady = playbackReadiness(saved).ready,
                    canUndo = projectHistory.canUndo,
                    canRedo = projectHistory.canRedo,
                    clipStatus = "Gravação interrompida recuperada com segurança",
                    error = null,
                )
            }.onFailure { error ->
                _state.value = _state.value.copy(recoveryBusy = false, error = error.message ?: "Não foi possível recuperar a gravação.")
            }
        }
    }

    fun previewInterruptedRecording(transactionId: String) {
        val current = _state.value
        val candidate = recoveryCandidatesById[transactionId] ?: return
        val rate = candidate.sampleRateHz ?: return
        val frames = candidate.frames ?: return
        if (!candidate.safePlayable || frames <= 0L || current.recordingSession.active) return
        stopPlaybackSession()
        val outputSignature = audioRoutingStore.selectedOutputSignature()
        val cueOutputSignature = audioRoutingStore.selectedCueOutputSignature()
        val request = StudioPlaybackRequest(
            sampleRateHz = rate,
            startFrame = 0L,
            projectEndFrame = frames,
            loopEnabled = false,
            loopStartFrame = 0L,
            loopEndFrame = frames,
            clips = listOf(StudioPlaybackClip(candidate.mediaFile, "recovery-preview", 0L, 0L, frames)),
            preferredOutputDevice = audioRoutingStore.resolveSelectedOutputDevice(),
            preferredOutputRequested = !outputSignature.isNullOrBlank(),
        )
        val sessionId = ++playbackSessionId
        activePlaybackRouteState = AudioRouteSessionPolicy.starting(
            selectedInputIdentity = null,
            selectedOutputIdentity = audioRoutingStore.selectedOutputDiagnosticIdentity(),
        )
        runCatching {
            playbackEngine.start(request, object : StudioPlaybackListener {
                override fun onStarted(info: StudioPlaybackStartInfo) {
                    activePlaybackRouteState = runCatching {
                        AudioRouteSessionPolicy.confirmed(activePlaybackRouteState ?: return@runCatching null, null, info.routedOutputLabel)
                    }.getOrNull()
                }
                override fun onPosition(frame: Long) = Unit
                override fun onStopped(frame: Long) {
                    viewModelScope.launch { if (sessionId == playbackSessionId) _state.value = _state.value.copy(clipStatus = "Prévia concluída") }
                }
                override fun onError(message: String) {
                    viewModelScope.launch { if (sessionId == playbackSessionId) _state.value = _state.value.copy(error = message) }
                }
            })
            _state.value = current.copy(clipStatus = "Ouvindo gravação interrompida…", error = null)
        }.onFailure { error -> _state.value = current.copy(error = error.message ?: "Não foi possível reproduzir a prévia.") }
    }

    fun discardInterruptedRecording(transactionId: String) {
        val candidate = recoveryCandidatesById[transactionId] ?: return
        if (_state.value.recoveryBusy) return
        viewModelScope.launch {
            _state.value = _state.value.copy(recoveryBusy = true)
            runCatching { withContext(Dispatchers.IO) { recordingMediaStore.discardRecovery(candidate) } }
                .onSuccess {
                    recoveryCandidatesById.remove(transactionId)
                    _state.value = _state.value.copy(
                        recoveryBusy = false,
                        recoveryItems = _state.value.recoveryItems.filterNot { it.transactionId == transactionId },
                        clipStatus = "Gravação interrompida descartada",
                    )
                }
                .onFailure { error -> _state.value = _state.value.copy(recoveryBusy = false, error = error.message ?: "Não foi possível descartar a gravação.") }
        }
    }

    fun postponeInterruptedRecording(transactionId: String) {
        recoveryCandidatesById.remove(transactionId)
        _state.value = _state.value.copy(
            recoveryItems = _state.value.recoveryItems.filterNot { it.transactionId == transactionId },
            clipStatus = "A gravação foi mantida para recuperar depois",
        )
    }

    fun activateTake(takeId: String) = editTakeMetadata("Take ativo atualizado") { project ->
        TakeManagementPolicy.activate(project, takeId, System.currentTimeMillis())
    }

    fun renameTake(takeId: String, name: String) = editTakeMetadata("Take renomeado") { project ->
        TakeManagementPolicy.rename(project, takeId, name, System.currentTimeMillis())
    }

    fun setTakeNote(takeId: String, note: String) = editTakeMetadata("Nota do take atualizada") { project ->
        TakeManagementPolicy.setNote(project, takeId, note, System.currentTimeMillis())
    }

    fun updateTakeMetadata(takeId: String, name: String, note: String, fineAdjustmentMilliseconds: Double) =
        editTakeMetadata("Take atualizado") { project ->
            val now = System.currentTimeMillis()
            val sampleRateHz = RecordingSampleRatePolicy.resolve(project)
                ?: error("Não foi possível determinar a taxa de amostragem desta take.")
            val fineFrames = LatencyFineAdjustmentPolicy.millisecondsToFrames(fineAdjustmentMilliseconds, sampleRateHz)
            val metadata = TakeManagementPolicy.setNote(
                TakeManagementPolicy.rename(project, takeId, name, now),
                takeId,
                note,
                now,
            )
            TakeManagementPolicy.setFineAdjustmentFrames(metadata, takeId, fineFrames, now)
        }

    fun toggleTakeFavorite(takeId: String) = editTakeMetadata("Favorito atualizado") { project ->
        val take = project.takes.firstOrNull { it.id == takeId } ?: error("Take não encontrado: $takeId")
        TakeManagementPolicy.setFavorite(project, takeId, !take.favorite, System.currentTimeMillis())
    }

    fun deleteTake(takeId: String) = editTakeMetadata("Take excluído") { project ->
        TakeManagementPolicy.delete(project, takeId, System.currentTimeMillis())
    }

    private fun editTakeMetadata(status: String, transform: (GuitarProject) -> GuitarProject) {
        val current = _state.value
        val project = current.project ?: return
        if (!structuralEditingAllowed(current)) return
        viewModelScope.launch {
            runCatching { saveLatest(project.id, transform) }
                .onSuccess { saved ->
                    val waveformState = withContext(Dispatchers.IO) { loadWaveformState(saved) }
                    val end = TimelineControlPolicy.projectEndFrame(saved)
                    _state.value = _state.value.copy(
                        project = saved,
                        waveforms = waveformState.waveforms,
                        waveformChannels = waveformState.waveformChannels,
                        timelineControls = TimelineControlPolicy.normalizedForProject(_state.value.timelineControls, end),
                        transportEngineReady = playbackReadiness(saved).ready,
                        canUndo = projectHistory.canUndo,
                        canRedo = projectHistory.canRedo,
                        clipStatus = status,
                        error = null,
                    )
                }
                .onFailure { error -> _state.value = _state.value.copy(error = error.message ?: "Não foi possível atualizar o take.") }
        }
    }

    fun auditionTake(takeId: String) {
        val current = _state.value
        val project = current.project ?: return
        if (current.recordingSession.active) return
        val take = project.takes.firstOrNull { it.id == takeId } ?: return
        val clips = project.clips.filter { it.takeId == take.id }.sortedBy { it.startFrame }
        if (clips.isEmpty()) {
            _state.value = current.copy(error = "Este take não possui clipes reproduzíveis.")
            return
        }
        val sampleRate = project.sampleRate.fixedHz ?: clips.firstNotNullOfOrNull { it.editingSampleRateHz ?: it.sourceSampleRateHz } ?: 48_000
        if (clips.any { (it.editingSampleRateHz ?: it.sourceSampleRateHz ?: sampleRate) != sampleRate }) {
            _state.value = current.copy(error = "O take usa taxas de amostragem incompatíveis para audição rápida.")
            return
        }
        val end = clips.maxOf { it.startFrame + it.lengthFrames }.coerceAtLeast(1L)
        val tracks = project.tracks.associateBy { it.id }
        val playbackClips = runCatching {
            clips.map { clip ->
                StudioPlaybackClip(
                    file = mediaStore.resolveEditable(project.id, editingMediaPath(clip)),
                    trackId = clip.trackId,
                    timelineStartFrame = clip.startFrame,
                    sourceStartFrame = clip.sourceStartFrame,
                    lengthFrames = clip.lengthFrames,
                    gainDb = clip.gainDb,
                    fadeInFrames = clip.fadeInFrames,
                    fadeOutFrames = clip.fadeOutFrames,
                )
            }
        }.getOrElse { error ->
            _state.value = current.copy(error = error.message ?: "A mídia deste take não está disponível.")
            return
        }
        stopPlaybackSession()
        val outputSignature = audioRoutingStore.selectedOutputSignature()
        val request = StudioPlaybackRequest(
            sampleRateHz = sampleRate,
            startFrame = clips.minOf { it.startFrame },
            projectEndFrame = end,
            loopEnabled = false,
            loopStartFrame = 0L,
            loopEndFrame = end,
            clips = playbackClips,
            trackMixes = project.tracks.filter { track -> clips.any { it.trackId == track.id } }.map { track ->
                StudioPlaybackTrackMix(track.id, track.roleId, track.gainDb, track.pan, false, false)
            },
            preferredOutputDevice = audioRoutingStore.resolveSelectedOutputDevice(),
            preferredOutputRequested = !outputSignature.isNullOrBlank(),
            masterGainDb = project.masterGainDb,
        )
        val sessionId = ++playbackSessionId
        runCatching {
            playbackEngine.start(request, object : StudioPlaybackListener {
                override fun onPosition(frame: Long) = Unit
                override fun onStopped(frame: Long) {
                    viewModelScope.launch { if (sessionId == playbackSessionId) _state.value = _state.value.copy(clipStatus = "Audição de ${take.name} concluída") }
                }
                override fun onError(message: String) {
                    viewModelScope.launch { if (sessionId == playbackSessionId) _state.value = _state.value.copy(error = message) }
                }
            })
            _state.value = current.copy(clipStatus = "Ouvindo ${take.name}…", error = null)
        }.onFailure { error -> _state.value = current.copy(error = error.message ?: "Não foi possível ouvir o take.") }
    }

    fun addMarkerAtPlayhead() = updatePracticeProject { project ->
        PracticeWorkflowEditor.addMarker(project, "Marcador ${project.markers.size + 1}", _state.value.timelineControls.playheadFrame, System.currentTimeMillis())
    }

    fun addSectionFromLoop() {
        val state = _state.value
        if (!state.transport.loopEnabled) return
        updatePracticeProject { project -> PracticeWorkflowEditor.addSection(project, "Seção ${project.sections.size + 1}", state.timelineControls.loopStartFrame, state.timelineControls.loopEndFrame, System.currentTimeMillis()) }
    }

    fun suggestSections() {
        val state = _state.value; val project = state.project ?: return
        val source = ActiveTakePolicy.audibleClips(project).maxByOrNull { it.lengthFrames }
        val peaks = source?.let { state.waveforms[it.id] }.orEmpty()
        _state.value = state.copy(sectionSuggestions = SectionBoundaryAnalyzer.suggest(peaks, TimelineControlPolicy.projectEndFrame(project)))
    }

    fun acceptSectionSuggestions() {
        val suggestions = _state.value.sectionSuggestions
        if (suggestions.isEmpty()) return
        updatePracticeProject { project -> PracticeWorkflowEditor.acceptSuggestedSections(project, suggestions, TimelineControlPolicy.projectEndFrame(project), System.currentTimeMillis()) }
        _state.value = _state.value.copy(sectionSuggestions = emptyList())
    }

    fun discardSectionSuggestions() { _state.value = _state.value.copy(sectionSuggestions = emptyList()) }

    fun clearSections() {
        val state = _state.value
        if (state.sectionSuggestions.isNotEmpty()) _state.value = state.copy(sectionSuggestions = emptyList())
        val project = _state.value.project ?: return
        if (project.sections.isEmpty()) return
        updatePracticeProject { PracticeWorkflowEditor.clearSections(it, System.currentTimeMillis()) }
    }

    fun removeMarker(id: String) = updatePracticeProject { PracticeWorkflowEditor.removeMarker(it, id, System.currentTimeMillis()) }
    fun removeSection(id: String) = updatePracticeProject { PracticeWorkflowEditor.removeSection(it, id, System.currentTimeMillis()) }

    fun loopSection(id: String) {
        val section = _state.value.project?.sections?.firstOrNull { it.id == id } ?: return
        val state = _state.value
        if (state.transport.mode != TransportMode.STOPPED) return
        _state.value = state.copy(transport = state.transport.copy(loopEnabled = true), timelineControls = state.timelineControls.copy(playheadFrame = section.startFrame, loopStartFrame = section.startFrame, loopEndFrame = section.endFrame))
    }

    fun analyzeTrackLevel(trackId: String) {
        val state = _state.value
        val project = state.project ?: return
        if (trackId in state.trackLevelAnalysisBusy) return
        val track = project.tracks.firstOrNull { it.id == trackId } ?: return
        val analyzedClips = audibleLevelClips(project, trackId)
        if (analyzedClips.isEmpty()) {
            _state.value = state.copy(
                trackLevelAnalysis = state.trackLevelAnalysis - trackId,
                clipStatus = "A pista não possui clipes audíveis para analisar.",
                error = null,
            )
            return
        }
        _state.value = state.copy(
            trackLevelAnalysis = state.trackLevelAnalysis - trackId,
            trackLevelAnalysisBusy = state.trackLevelAnalysisBusy + trackId,
            error = null,
        )
        viewModelScope.launch {
            runCatching { analyzeLevelSnapshot(project, track, analyzedClips) }
                .onSuccess { analysis -> publishLevelAnalysis(project, track, analyzedClips, analysis) }
                .onFailure { error ->
                    val latest = _state.value
                    if (latest.project?.id == project.id) {
                        _state.value = latest.copy(error = error.message ?: "Não foi possível analisar o nível.")
                    }
                }
            val latest = _state.value
            if (latest.project?.id == project.id) {
                _state.value = latest.copy(trackLevelAnalysisBusy = latest.trackLevelAnalysisBusy - trackId)
            }
        }
    }

    fun analyzeAllTrackLevels() {
        val state = _state.value
        val project = state.project ?: return
        if (state.trackLevelAnalysisBusy.isNotEmpty() || state.importing || state.editingClip || state.historyBusy || state.recordingSession.phase != RecordingSessionPhase.IDLE) return
        val targets = project.tracks.sortedBy { it.order }.mapNotNull { track ->
            val clips = audibleLevelClips(project, track.id)
            if (clips.isEmpty()) null else Triple(track.id, track, clips)
        }
        if (targets.isEmpty()) {
            _state.value = state.copy(clipStatus = "Não há pistas com áudio audível para analisar.", error = null)
            return
        }
        val ids = targets.map { it.first }.toSet()
        _state.value = state.copy(
            trackLevelAnalysis = state.trackLevelAnalysis - ids,
            trackLevelAnalysisBusy = ids,
            clipStatus = "Analisando ${ids.size} ${if (ids.size == 1) "pista" else "pistas"}…",
            error = null,
        )
        viewModelScope.launch {
            var completed = 0
            targets.forEach { (trackId, track, clips) ->
                if (_state.value.project?.id != project.id) return@launch
                runCatching { analyzeLevelSnapshot(project, track, clips) }
                    .onSuccess { analysis ->
                        if (publishLevelAnalysis(project, track, clips, analysis)) completed++
                    }
                    .onFailure { error ->
                        val latest = _state.value
                        if (latest.project?.id == project.id) {
                            _state.value = latest.copy(error = error.message ?: "Não foi possível analisar a pista ${track.name}.")
                        }
                    }
                val latest = _state.value
                if (latest.project?.id == project.id) {
                    _state.value = latest.copy(trackLevelAnalysisBusy = latest.trackLevelAnalysisBusy - trackId)
                }
            }
            val latest = _state.value
            if (latest.project?.id == project.id) {
                _state.value = latest.copy(
                    trackLevelAnalysisBusy = latest.trackLevelAnalysisBusy - ids,
                    clipStatus = "Análise de níveis concluída em $completed ${if (completed == 1) "pista" else "pistas"}.",
                    error = null,
                )
            }
        }
    }

    fun applyTrackLevelSuggestion(trackId: String) {
        val state = _state.value
        val project = state.project ?: return
        val analysis = state.trackLevelAnalysis[trackId] ?: return
        val expectedTrack = project.tracks.firstOrNull { it.id == trackId } ?: return
        val expectedClips = audibleLevelClips(project, trackId)
        if (analysis.silent || abs(analysis.recommendedGainDb) < LEVEL_GAIN_EPSILON_DB) {
            _state.value = state.copy(
                trackLevelAnalysis = state.trackLevelAnalysis - trackId,
                clipStatus = "Nível da pista já está dentro do alvo",
                error = null,
            )
            return
        }
        viewModelScope.launch {
            runCatching {
                saveLatest(project.id) { current ->
                    val latestTrack = current.tracks.firstOrNull { it.id == trackId }
                        ?: error("A pista analisada não existe mais.")
                    val latestClips = audibleLevelClips(current, trackId)
                    require(latestTrack == expectedTrack && latestClips == expectedClips) {
                        "A pista mudou depois da análise. Analise novamente antes de aplicar o ganho."
                    }
                    current.copy(
                        tracks = current.tracks.map { track ->
                            if (track.id == trackId) track.copy(
                                gainDb = (track.gainDb + analysis.recommendedGainDb).coerceIn(-60f, 12f),
                            ) else track
                        },
                        updatedAtEpochMs = System.currentTimeMillis(),
                    )
                }
            }.onSuccess { saved ->
                applySavedProject(saved, "Ganho sugerido aplicado")
            }.onFailure { error ->
                _state.value = _state.value.copy(error = error.message ?: "Não foi possível aplicar a sugestão de nível.")
            }
        }
    }

    fun applyAllTrackLevelSuggestions() {
        val state = _state.value
        val project = state.project ?: return
        if (state.trackLevelAnalysisBusy.isNotEmpty()) return
        val candidates = state.trackLevelAnalysis.mapNotNull { (trackId, analysis) ->
            val track = project.tracks.firstOrNull { it.id == trackId } ?: return@mapNotNull null
            if (analysis.silent || abs(analysis.recommendedGainDb) < LEVEL_GAIN_EPSILON_DB) return@mapNotNull null
            val clips = audibleLevelClips(project, trackId)
            BatchLevelCandidate(track, clips, analysis)
        }
        if (candidates.isEmpty()) {
            _state.value = state.copy(clipStatus = "Nenhuma pista analisada precisa de ajuste.", error = null)
            return
        }
        viewModelScope.launch {
            runCatching {
                saveLatest(project.id) { current ->
                    candidates.forEach { candidate ->
                        val latestTrack = current.tracks.firstOrNull { it.id == candidate.track.id }
                            ?: error("A pista ${candidate.track.name} não existe mais.")
                        val latestClips = audibleLevelClips(current, candidate.track.id)
                        require(latestTrack == candidate.track && latestClips == candidate.clips) {
                            "A pista ${candidate.track.name} mudou depois da análise. Analise todas novamente antes de aplicar."
                        }
                    }
                    val suggestions = candidates.associate { it.track.id to it.analysis.recommendedGainDb }
                    current.copy(
                        tracks = current.tracks.map { track ->
                            val adjustment = suggestions[track.id]
                            if (adjustment == null) track else track.copy(gainDb = (track.gainDb + adjustment).coerceIn(-60f, 12f))
                        },
                        updatedAtEpochMs = System.currentTimeMillis(),
                    )
                }
            }.onSuccess { saved ->
                applySavedProject(saved, "Sugestões de nível aplicadas a ${candidates.size} ${if (candidates.size == 1) "pista" else "pistas"}")
            }.onFailure { error ->
                _state.value = _state.value.copy(error = error.message ?: "Não foi possível aplicar as sugestões de nível.")
            }
        }
    }

    private data class BatchLevelCandidate(
        val track: AudioTrack,
        val clips: List<AudioClip>,
        val analysis: LevelAnalysis,
    )

    private fun audibleLevelClips(project: GuitarProject, trackId: String): List<AudioClip> =
        ActiveTakePolicy.audibleClips(project).filter { it.trackId == trackId && !it.muted }

    private suspend fun analyzeLevelSnapshot(
        project: GuitarProject,
        track: AudioTrack,
        analyzedClips: List<AudioClip>,
    ): LevelAnalysis = withContext(Dispatchers.IO) {
        val accumulator = TrackLevelAccumulator()
        analyzedClips.forEach { clip ->
            FileSeekableByteSource(mediaStore.resolveEditable(project.id, editingMediaPath(clip))).use { source ->
                WavPcmDecoder(source).use { decoder ->
                    decoder.seekToFrame(clip.sourceStartFrame)
                    val buffer = FloatArray(2048 * decoder.metadata.channelCount)
                    val effectiveGain = TrackLevelAdvisor.gainLinear(track.gainDb + clip.gainDb)
                    var remaining = clip.lengthFrames
                    while (remaining > 0) {
                        val requested = minOf(2048L, remaining).toInt()
                        val read = decoder.readInterleaved(buffer, frameCount = requested)
                        if (read <= 0) break
                        accumulator.append(buffer, read * decoder.metadata.channelCount, effectiveGain)
                        remaining -= read
                    }
                }
            }
        }
        accumulator.finish()
    }

    private fun publishLevelAnalysis(
        project: GuitarProject,
        track: AudioTrack,
        analyzedClips: List<AudioClip>,
        analysis: LevelAnalysis,
    ): Boolean {
        val latestState = _state.value
        val latestProject = latestState.project ?: return false
        if (latestProject.id != project.id) return false
        val latestTrack = latestProject.tracks.firstOrNull { it.id == track.id }
        val latestClips = audibleLevelClips(latestProject, track.id)
        if (latestTrack != track || latestClips != analyzedClips) {
            _state.value = latestState.copy(
                trackLevelAnalysis = latestState.trackLevelAnalysis - track.id,
                clipStatus = "A pista ${track.name} mudou durante a análise. Analise novamente.",
                error = null,
            )
            return false
        }
        _state.value = latestState.copy(
            trackLevelAnalysis = latestState.trackLevelAnalysis + (track.id to analysis),
            error = null,
        )
        return true
    }

    private fun updatePracticeProject(transform: (GuitarProject) -> GuitarProject) {
        val state = _state.value; val project = state.project ?: return
        if (!structuralEditingAllowed(state)) return
        viewModelScope.launch {
            runCatching { saveLatest(project.id, transform) }
                .onSuccess { saved -> applySavedProject(saved) }
                .onFailure { error -> _state.value = _state.value.copy(error = error.message) }
        }
    }

    fun onStudioHidden() {
        dismissLoopRecordingChoice()
        when (_state.value.recordingSession.phase) {
            RecordingSessionPhase.COUNTDOWN -> cancelRecordingCountdown()
            RecordingSessionPhase.CAPTURING -> stopRecording()
            else -> stopPlaybackSession()
        }
    }

    private fun stopPlaybackSession() {
        playbackSessionId++
        activePlaybackRouteState = activePlaybackRouteState?.let(AudioRouteSessionPolicy::stopped)
        playbackEngine.stop()
    }

    override fun onCleared() {
        recordingProgressChannel.close()
        countdownJob?.cancel()
        pendingRecordingPlan = null
        activePunchPlan = null
        activePunchRegion = null
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
        val requestedStart = TransportPolicy.playbackStartFrame(
            state = current.transport,
            playheadFrame = current.timelineControls.playheadFrame,
            projectEndFrame = end,
            loopStartFrame = current.timelineControls.loopStartFrame,
            loopEndFrame = current.timelineControls.loopEndFrame,
        )
        val tracksById = project.tracks.associateBy { it.id }
        val selectedOutputSignature = audioRoutingStore.selectedOutputSignature()
        val preferredOutput = audioRoutingStore.resolveSelectedOutputDevice()
        val selectedCueOutputSignature = audioRoutingStore.selectedCueOutputSignature()
        val preferredCueOutput = audioRoutingStore.resolveSelectedCueOutputDevice()
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
                preferredCueOutputDevice = preferredCueOutput,
                preferredCueOutputRequested =
                    !selectedCueOutputSignature.isNullOrBlank() ||
                        project.tracks.any { TrackOutputRoutingPolicy.sendsToCue(it.outputRoute) },
                masterGainDb = project.masterGainDb,
                auditionMode = current.guitarAuditionMode,
                repeatLoop = false,
                trackMixes = project.tracks.map { track ->
                    StudioPlaybackTrackMix(
                        trackId = track.id,
                        roleId = track.roleId,
                        gainDb = track.gainDb,
                        pan = track.pan,
                        muted = track.muted,
                        solo = track.solo,
                        outputRoute = track.outputRoute,
                    )
                },
                clips = ActiveTakePolicy.audibleClips(project).mapNotNull { clip ->
                    val sourceTrack = tracksById[clip.trackId] ?: return@mapNotNull null
                    if (clip.muted) return@mapNotNull null
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
                        fadeInFrames = clip.fadeInFrames,
                        fadeOutFrames = clip.fadeOutFrames,
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
        val sessionId = ++playbackSessionId
        runCatching {
            playbackEngine.start(request, object : StudioPlaybackListener {
                override fun onPosition(frame: Long) {
                    viewModelScope.launch {
                        val state = _state.value
                        if (sessionId == playbackSessionId && state.transport.mode == TransportMode.PLAYING) {
                            val visibleFrame = TransportPolicy.playbackPositionFrame(
                                state = state.transport,
                                reportedFrame = frame,
                                projectEndFrame = end,
                                loopStartFrame = state.timelineControls.loopStartFrame,
                                loopEndFrame = state.timelineControls.loopEndFrame,
                            )
                            _state.value = state.copy(timelineControls = state.timelineControls.copy(playheadFrame = visibleFrame))
                        }
                    }
                }

                override fun onStopped(frame: Long) {
                    activePlaybackRouteState = activePlaybackRouteState?.let(AudioRouteSessionPolicy::stopped)
                    viewModelScope.launch {
                        val state = _state.value
                        if (sessionId != playbackSessionId || state.transport.mode != TransportMode.PLAYING) return@launch
                        val resetFrame = TransportPolicy.automaticPlaybackResetFrame(
                            state = state.transport,
                            projectEndFrame = end,
                            loopStartFrame = state.timelineControls.loopStartFrame,
                            loopEndFrame = state.timelineControls.loopEndFrame,
                        )
                        _state.value = state.copy(
                            transport = state.transport.copy(mode = TransportMode.STOPPED),
                            timelineControls = state.timelineControls.copy(playheadFrame = resetFrame),
                            masterMeter = MeterBallisticsPolicy.reset(),
                            trackMeters = emptyMap(),
                        )
                    }
                }

                override fun onError(message: String) {
                    activePlaybackRouteState = activePlaybackRouteState?.let { AudioRouteSessionPolicy.lost(it, message) }
                    viewModelScope.launch {
                        val state = _state.value
                        if (sessionId != playbackSessionId) return@launch
                        _state.value = state.copy(
                            transport = state.transport.copy(mode = TransportMode.STOPPED),
                            masterMeter = MeterBallisticsPolicy.reset(),
                            trackMeters = emptyMap(),
                            error = message,
                        )
                    }
                }

                override fun onRouting(status: StudioPlaybackRoutingStatus) {
                    if (status.fellBackToAuto) {
                        activePlaybackRouteState = activePlaybackRouteState?.let { AudioRouteSessionPolicy.lost(it, "selected output route fell back") }
                        viewModelScope.launch {
                            val state = _state.value
                            if (sessionId != playbackSessionId || state.transport.mode != TransportMode.PLAYING) return@launch
                            postTransientWarning(
                                "A saída principal selecionada ficou indisponível; o GuitarLab usou a saída automática.",
                                "A saída principal selecionada ficou indisponível; o GuitarLab usou a saída automática.",
                            )
                        }
                    }
                    if (status.cueSuppressed) {
                        viewModelScope.launch {
                            val state = _state.value
                            if (sessionId != playbackSessionId || state.transport.mode != TransportMode.PLAYING) return@launch
                            val detail = status.cueFailureReason ?: "A saída secundária não pôde ser confirmada."
                            postTransientWarning(
                                "CUE foi silenciado para impedir vazamento para a saída principal. $detail",
                                "CUE foi silenciado para impedir vazamento para a saída principal.",
                            )
                        }
                    }
                }

                override fun onMasterMeter(meter: StudioPlaybackMeter) {
                    viewModelScope.launch {
                        val state = _state.value
                        if (sessionId == playbackSessionId && state.transport.mode == TransportMode.PLAYING) {
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
                        if (sessionId != playbackSessionId) return@launch
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

    private fun recoveryItems(project: GuitarProject, candidates: List<RecordingRecoveryCandidate>): List<StudioRecoveryItem> =
        candidates.map { candidate ->
            val decision = RecordingRecoveryPolicy.publicationDecision(project, candidate)
            val trackName = candidate.marker?.targetTrackId?.let { id -> project.tracks.firstOrNull { it.id == id }?.name }
            StudioRecoveryItem(
                transactionId = candidate.transactionId,
                trackName = trackName,
                approximateDurationSeconds = candidate.approximateDurationSeconds,
                createdAtEpochMs = candidate.marker?.createdAtEpochMs,
                safePlayable = candidate.safePlayable,
                recoverable = decision == RecordingRecoveryPublicationDecision.RECOVERABLE,
                finalizedUnpublished = candidate.state == studio.guitarlab.core.project.RecordingRecoveryMediaState.FINALIZED_UNPUBLISHED,
                diagnosticReason = when (decision) {
                    RecordingRecoveryPublicationDecision.TARGET_TRACK_MISSING -> "A pista original não existe mais; o áudio será preservado para suporte."
                    RecordingRecoveryPublicationDecision.UNSAFE_PAYLOAD -> candidate.diagnosticReason ?: "A mídia não pôde ser validada com segurança."
                    else -> candidate.diagnosticReason
                },
            )
        }

    private fun loadWaveformForClip(project: GuitarProject, clip: AudioClip): List<Float> =
        loadWaveformEnvelope(project, clip)?.peaks.orEmpty()

    private fun friendlySelectedInputLabel(): String {
        val signature = audioRoutingStore.selectedInputSignature() ?: return "entrada automática"
        return audioRoutingStore.inputChoices().firstOrNull { it.signature == signature }?.label ?: "entrada selecionada"
    }

    private fun startBackingPlaybackForRecording(sampleRateHz: Int): Boolean {
        val state = _state.value
        val project = state.project ?: return false
        val end = TimelineControlPolicy.projectEndFrame(project)
        if (end <= 0L || project.clips.isEmpty()) return false
        val tracksById = project.tracks.associateBy { it.id }
        val clips = runCatching {
            ActiveTakePolicy.audibleClips(project).mapNotNull { clip ->
                val track = tracksById[clip.trackId] ?: return@mapNotNull null
                if (clip.muted || (clip.editingSampleRateHz ?: clip.sourceSampleRateHz) != sampleRateHz) return@mapNotNull null
                val managedPath = editingMediaPathOrNull(clip) ?: return@mapNotNull null
                StudioPlaybackClip(
                    file = mediaStore.resolveEditable(project.id, managedPath),
                    trackId = track.id,
                    timelineStartFrame = clip.startFrame,
                    sourceStartFrame = clip.sourceStartFrame,
                    lengthFrames = clip.lengthFrames,
                    gainDb = clip.gainDb,
                    fadeInFrames = clip.fadeInFrames,
                    fadeOutFrames = clip.fadeOutFrames,
                )
            }
        }.getOrElse {
            postTransientWarning(
                "A gravação continua, mas o backing não pôde ser reproduzido.",
                "A gravação continua, mas o backing não pôde ser reproduzido.",
            )
            return false
        }
        if (clips.isEmpty()) return false
        val outputSignature = audioRoutingStore.selectedOutputSignature()
        val request = StudioPlaybackRequest(
            sampleRateHz = sampleRateHz,
            startFrame = state.recordingSession.timelineStartFrame.coerceAtMost(end),
            projectEndFrame = end,
            loopEnabled = state.transport.loopEnabled,
            loopStartFrame = state.timelineControls.loopStartFrame,
            loopEndFrame = state.timelineControls.loopEndFrame,
            clips = clips,
            trackMixes = project.tracks.map {
                StudioPlaybackTrackMix(
                    trackId = it.id,
                    roleId = it.roleId,
                    gainDb = it.gainDb,
                    pan = it.pan,
                    muted = it.muted,
                    solo = it.solo,
                    outputRoute = it.outputRoute,
                )
            },
            preferredOutputDevice = audioRoutingStore.resolveSelectedOutputDevice(),
            preferredOutputRequested = !outputSignature.isNullOrBlank(),
            preferredCueOutputDevice = audioRoutingStore.resolveSelectedCueOutputDevice(),
            preferredCueOutputRequested =
                !cueOutputSignature.isNullOrBlank() ||
                    project.tracks.any { TrackOutputRoutingPolicy.sendsToCue(it.outputRoute) },
            masterGainDb = project.masterGainDb,
        )
        return runCatching {
            playbackEngine.start(request, object : StudioPlaybackListener {
                override fun onStarted(info: StudioPlaybackStartInfo) {
                    val timingEvidence = RecordingTimingCompensationPolicy.startupOffsetEvidence(
                        captureStartMonotonicNs = activeRecordingCaptureStartNs,
                        captureTimestampBased = activeRecordingCaptureTimestampBased,
                        backingPresentationStartMonotonicNs = info.presentationStartMonotonicNs,
                        backingTimestampBased = info.timestampBased,
                        sampleRateHz = sampleRateHz,
                    )
                    activeRecordingClockOffsetFrames = timingEvidence.offsetFrames
                    activeRecordingTimingBasis = timingEvidence.basis
                    activeRecordingBackingAnchorJitterNs = info.playbackAnchorJitterNs
                    activeRecordingBackingAnchorObservations = info.playbackAnchorObservations
                    activeRecordingEffectiveOutputIdentity = info.routedOutputLabel
                    activeRecordingRouteState = activeRecordingRouteState?.let { route ->
                        if (route.phase == studio.guitarlab.core.audio.AudioRouteSessionPhase.ACTIVE_CONFIRMED) {
                            AudioRouteSessionPolicy.updateEffectiveOutput(route, info.routedOutputLabel)
                        } else route
                    }
                    activeRecordingOutputUnderruns = info.outputUnderrunCount
                    val captureBeforeBackingFrames = (-activeRecordingClockOffsetFrames).coerceAtLeast(0L)
                    val compensatedTail = (activeRecordingRouteLatencyFrames + activeRecordingFineAdjustmentFrames).coerceAtLeast(0L)
                    activePunchPlan = activePunchRegion?.let { region ->
                        PunchRecordingPolicy.plan(region, captureBeforeBackingFrames + compensatedTail)
                    }
                }

                override fun onRouting(status: StudioPlaybackRoutingStatus) {
                    if (status.fellBackToAuto) {
                        // Route-specific calibration/fine adjustment is invalid once Android falls back.
                        activeRecordingOutputFallback = true
                        activeRecordingRouteChanged = true
                        activeRecordingRouteState = AudioRouteSessionPolicy.lost(
                            activeRecordingRouteState ?: AudioRouteSessionPolicy.starting(activeRecordingSelectedInputIdentity, activeRecordingSelectedOutputIdentity),
                            "selected output route fell back during recording",
                        )
                        activeRecordingRouteLatencyFrames = 0L
                        activeRecordingFineAdjustmentFrames = 0L
                        val captureBeforeBackingFrames = (-activeRecordingClockOffsetFrames).coerceAtLeast(0L)
                        activePunchPlan = activePunchRegion?.let { region ->
                            PunchRecordingPolicy.plan(region, captureBeforeBackingFrames)
                        }
                    }
                    if (status.cueSuppressed) {
                        viewModelScope.launch {
                            val current = _state.value
                            if (current.recordingSession.phase == RecordingSessionPhase.CAPTURING) {
                                postTransientWarning(
                                    "CUE foi silenciado durante a gravação para impedir vazamento no MAIN. ${status.cueFailureReason ?: "Rota secundária não confirmada."}",
                                    "CUE foi silenciado durante a gravação; o take continua normalmente.",
                                )
                            }
                        }
                    }
                }

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
                            activeRecordingRouteChanged = true
                            postTransientWarning(message, "A gravação continua sem backing porque a saída ficou indisponível.")
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
            true
        }.getOrElse {
            activeRecordingRouteLatencyFrames = 0L
            activeRecordingFineAdjustmentFrames = 0L
            activeRecordingOutputFallback = !audioRoutingStore.selectedOutputSignature().isNullOrBlank()
            activeRecordingRouteChanged = activeRecordingOutputFallback
            activeRecordingTimingBasis = RecordingTimingEvidenceBasis.NO_BACKING_REFERENCE
            postTransientWarning(
                "A gravação continua sem backing porque a saída ficou indisponível.",
                "A gravação continua sem backing porque a saída ficou indisponível.",
            )
            false
        }
    }

    private fun resetActiveRecordingHealthEvidence() {
        activeRecordingCaptureAnchorJitterNs = null
        activeRecordingCaptureAnchorObservations = null
        activeRecordingBackingAnchorJitterNs = null
        activeRecordingBackingAnchorObservations = null
        activeRecordingTimingBasis = RecordingTimingEvidenceBasis.NO_BACKING_REFERENCE
        activeRecordingSelectedInputIdentity = null
        activeRecordingEffectiveInputIdentity = null
        activeRecordingSelectedOutputIdentity = null
        activeRecordingEffectiveOutputIdentity = null
        activeRecordingOutputFallback = false
        activeRecordingRouteChanged = false
        activeRecordingOutputUnderruns = null
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
                saved to withContext(Dispatchers.IO) { loadWaveformState(saved) }
            }
                .onSuccess { (saved, waveformState) ->
                    val end = TimelineControlPolicy.projectEndFrame(saved)
                    _state.value = _state.value.copy(
                        editingClip = false,
                        project = saved,
                        waveforms = waveformState.waveforms,
                        waveformChannels = waveformState.waveformChannels,
                        timelineControls = TimelineControlPolicy.normalizedForProject(_state.value.timelineControls, end),
                        transportEngineReady = playbackReadiness(saved).ready,
                        masterGainDb = saved.masterGainDb,
                        trackLevelAnalysis = emptyMap(),
                        canUndo = projectHistory.canUndo,
                        canRedo = projectHistory.canRedo,
                        clipStatus = status,
                    )
                }
                .onFailure { error -> _state.value = _state.value.copy(editingClip = false, error = error.message ?: "Não foi possível editar o clipe.") }
        }
    }

    fun renameProject(name: String) {
        val normalized = name.trim().replace(Regex("\\s+"), " ")
        if (normalized.isBlank() || normalized.length > 80) {
            _state.value = _state.value.copy(error = "O nome do projeto deve ter entre 1 e 80 caracteres.")
            return
        }
        val current = _state.value
        val project = current.project ?: return
        if (current.importing || current.editingClip || current.historyBusy) return
        viewModelScope.launch {
            runCatching {
                saveLatest(project.id) { latest ->
                    latest.copy(name = normalized, updatedAtEpochMs = System.currentTimeMillis())
                }
            }.onSuccess { applySavedProject(it, "Projeto renomeado") }
                .onFailure { error -> _state.value = _state.value.copy(error = error.message ?: "Não foi possível renomear o projeto.") }
        }
    }

    private fun editTrackAudibility(trackId: String, toggleMute: Boolean) {
        val state = _state.value
        val project = state.project ?: return
        if (state.importing || state.editingClip || state.historyBusy || state.trimControls != null) return
        viewModelScope.launch {
            runCatching {
                saveLatest(project.id) { latest ->
                    val target = latest.tracks.firstOrNull { it.id == trackId } ?: return@saveLatest latest
                    val updated = if (toggleMute) target.copy(muted = !target.muted) else target.copy(solo = !target.solo)
                    latest.copy(
                        tracks = latest.tracks.map { if (it.id == trackId) updated else it },
                        updatedAtEpochMs = System.currentTimeMillis(),
                    )
                }
            }.onSuccess { saved ->
                applySavedProject(saved, if (toggleMute) "Mute da pista atualizado" else "Solo da pista atualizado")
            }.onFailure { error -> _state.value = _state.value.copy(error = error.message ?: "Não foi possível atualizar a audição da pista.") }
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
                applySavedProject(saved, "Mixagem da pista atualizada")
            }.onFailure { error -> _state.value = _state.value.copy(error = error.message ?: "Não foi possível salvar a mixagem da pista.") }
        }
    }

    private fun applySavedProject(saved: GuitarProject, status: String? = null) {
        if (_state.value.project?.id != saved.id) return
        val validTrackIds = saved.tracks.mapTo(mutableSetOf()) { it.id }
        trackMixDrafts.keys.retainAll(validTrackIds)
        saved.tracks.forEach { track ->
            trackMixDrafts[track.id] = TrackMixDraft(track.gainDb, track.pan)
            playbackEngine.setTrackMix(track.id, track.gainDb, track.pan)
            playbackEngine.setTrackAudibility(track.id, track.muted, track.solo)
            playbackEngine.setTrackOutputRoute(track.id, track.outputRoute)
        }
        playbackEngine.setMasterGainDb(saved.masterGainDb)
        val state = _state.value
        val end = TimelineControlPolicy.projectEndFrame(saved)
        _state.value = state.copy(
            project = saved,
            timelineControls = TimelineControlPolicy.normalizedForProject(state.timelineControls, end),
            transportEngineReady = playbackReadiness(saved).ready,
            masterGainDb = saved.masterGainDb,
            trackLevelAnalysis = emptyMap(),
            canUndo = projectHistory.canUndo,
            canRedo = projectHistory.canRedo,
            clipStatus = status,
            preparedReferenceUpdateAvailable = PreparedReferenceBindingPolicy.updateAvailable(saved),
            error = null,
        )
    }

    private fun applyHistorySnapshot(saved: GuitarProject, waveformState: LoadedWaveformState, status: String) {
        if (_state.value.project?.id != saved.id) return
        trackMixDrafts.clear()
        saved.tracks.forEach { track ->
            trackMixDrafts[track.id] = TrackMixDraft(track.gainDb, track.pan)
            playbackEngine.setTrackMix(track.id, track.gainDb, track.pan)
            playbackEngine.setTrackAudibility(track.id, track.muted, track.solo)
        }
        playbackEngine.setMasterGainDb(saved.masterGainDb)
        val state = _state.value
        val end = TimelineControlPolicy.projectEndFrame(saved)
        _state.value = state.copy(
            historyBusy = false,
            project = saved,
            waveforms = waveformState.waveforms,
            waveformChannels = waveformState.waveformChannels,
            trimControls = null,
            timelineControls = TimelineControlPolicy.normalizedForProject(state.timelineControls, end),
            transportEngineReady = playbackReadiness(saved).ready,
            masterGainDb = saved.masterGainDb,
            trackLevelAnalysis = emptyMap(),
            masterMeter = MeterBallisticsPolicy.reset(),
            trackMeters = emptyMap(),
            masterClipLatched = false,
            trackClipLatched = emptySet(),
            canUndo = projectHistory.canUndo,
            canRedo = projectHistory.canRedo,
            clipStatus = status,
            preparedReferenceUpdateAvailable = PreparedReferenceBindingPolicy.updateAvailable(saved),
            error = null,
        )
    }

    private fun structuralEditingAllowed(state: StudioUiState): Boolean =
        !state.importing && !state.editingClip && !state.historyBusy && state.trimControls == null && TransportPolicy.timelineEditingEnabled(state.transport)

    private suspend fun saveLatest(projectId: String, transform: (GuitarProject) -> GuitarProject): GuitarProject {
        val sessionGeneration = projectSessionGeneration
        return projectSaveMutex.withLock {
            val (before, saved) = withContext(Dispatchers.IO) {
                val latest = repository.load(projectId) ?: error("Projeto não encontrado: $projectId")
                val updated = transform(latest)
                latest to repository.save(updated)
            }
            BackupScheduler.enqueueCoalesced(getApplication())
            val stillActive = sessionGeneration == projectSessionGeneration && _state.value.project?.id == projectId
            if (stillActive) {
                projectHistory.record(before, saved)
                // History/readiness are derived truth. Update them at the persistence boundary so an
                // individual feature cannot accidentally leave transport or Undo/Redo visually stale.
                val state = _state.value
                _state.value = state.copy(
                    transportEngineReady = playbackReadiness(saved).ready,
                    canUndo = projectHistory.canUndo,
                    canRedo = projectHistory.canRedo,
                )
            }
            saved
        }
    }

    private suspend fun persistHistorySnapshot(snapshot: GuitarProject): Pair<GuitarProject, LoadedWaveformState> =
        projectSaveMutex.withLock {
            val result = withContext(Dispatchers.IO) {
                val saved = repository.save(snapshot.copy(updatedAtEpochMs = System.currentTimeMillis()))
                saved to loadWaveformState(saved)
            }
            BackupScheduler.enqueueCoalesced(getApplication())
            result
        }

    private fun playbackReadiness(project: GuitarProject): PlaybackReadiness {
        val anySolo = project.tracks.any { it.solo }
        val tracksById = project.tracks.associateBy { it.id }
        val audible = ActiveTakePolicy.audibleClips(project).filter { clip ->
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
        val firstRate = audible.first().let { it.editingSampleRateHz ?: it.sourceSampleRateHz } ?: return PlaybackReadiness(false, reason = "A taxa de amostragem do áudio é desconhecida.")
        val projectRate = project.sampleRate.fixedHz ?: firstRate
        if (audible.any { (it.editingSampleRateHz ?: it.sourceSampleRateHz) != projectRate }) {
            return PlaybackReadiness(false, reason = "Há clipes que ainda precisam ser preparados para a taxa de áudio do projeto.")
        }
        return PlaybackReadiness(true, projectRate)
    }

    private fun stereoPromptFor(project: GuitarProject, clip: AudioClip): StereoImportPrompt {
        val track = project.tracks.firstOrNull { it.id == clip.trackId }
        val pair = track?.let { guitarPairFor(project, it) }
        return StereoImportPrompt(
            clipId = clip.id,
            fileName = clip.name,
            leftTrackName = pair?.first?.name,
            rightTrackName = pair?.second?.name,
        )
    }

    private fun guitarPairFor(project: GuitarProject, sourceTrack: AudioTrack): Pair<AudioTrack, AudioTrack>? {
        val roles = when (sourceTrack.roleId) {
            BuiltInRoles.REFERENCE_GUITAR, BuiltInRoles.REFERENCE_GUITAR_L, BuiltInRoles.REFERENCE_GUITAR_R ->
                BuiltInRoles.REFERENCE_GUITAR_L to BuiltInRoles.REFERENCE_GUITAR_R
            BuiltInRoles.RECORDED_GUITAR, BuiltInRoles.RECORDED_GUITAR_L, BuiltInRoles.RECORDED_GUITAR_R ->
                BuiltInRoles.RECORDED_GUITAR_L to BuiltInRoles.RECORDED_GUITAR_R
            else -> return null
        }
        val left = project.tracks.firstOrNull { it.roleId == roles.first && (sourceTrack.groupId == null || it.groupId == sourceTrack.groupId) }
            ?: project.tracks.firstOrNull { it.roleId == roles.first }
        val right = project.tracks.firstOrNull { it.roleId == roles.second && (sourceTrack.groupId == null || it.groupId == sourceTrack.groupId) }
            ?: project.tracks.firstOrNull { it.roleId == roles.second }
        return if (left != null && right != null) left to right else null
    }

    private fun loadWaveformState(project: GuitarProject): LoadedWaveformState {
        val waveforms = linkedMapOf<String, List<Float>>()
        val channels = linkedMapOf<String, List<List<Float>>>()
        project.clips.forEach { clip ->
            val envelope = loadWaveformEnvelope(project, clip) ?: return@forEach
            waveforms[clip.id] = envelope.peaks
            if (envelope.channelPeaks.size == 2) channels[clip.id] = envelope.channelPeaks
        }
        waveformCache.prune(project.id, project.clips.mapTo(mutableSetOf()) { it.id })
        return LoadedWaveformState(waveforms, channels)
    }

    private fun loadWaveformEnvelope(project: GuitarProject, clip: AudioClip): WaveformEnvelope? {
        val managedPath = editingMediaPathOrNull(clip) ?: return null
        val identity = WaveformCacheIdentity.forClip(clip, WAVEFORM_POINTS)
        waveformCache.read(project.id, clip.id, identity)?.let { return it }
        return runCatching {
            val file = mediaStore.resolveEditable(project.id, managedPath)
            val envelope = FileSeekableByteSource(file).use { source ->
                WaveformEnvelopeBuilder.build(
                    decoder = WavPcmDecoder(source),
                    targetPoints = WAVEFORM_POINTS,
                    startFrame = clip.sourceStartFrame,
                    frameCount = clip.lengthFrames,
                )
            }
            waveformCache.write(project.id, clip.id, identity, envelope)
            envelope
        }.getOrNull()
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
        const val LIVE_WAVEFORM_UI_INTERVAL_MS = 33L
        const val LEVEL_GAIN_EPSILON_DB = 0.1f
        const val WAVEFORM_POINTS = 4096
        const val TRACK_COLOR_COUNT = 20
    }
}
