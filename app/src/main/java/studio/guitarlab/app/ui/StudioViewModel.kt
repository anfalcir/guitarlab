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
import kotlinx.coroutines.CoroutineStart
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
import studio.guitarlab.core.audio.MeterBallisticsPolicy
import studio.guitarlab.core.audio.MeterBallisticsState
import studio.guitarlab.core.audio.TrackMixPolicy
import studio.guitarlab.core.audio.LatencyCompensationPolicy
import studio.guitarlab.core.codec.AudioImportFormat
import studio.guitarlab.core.codec.AudioImportFormatPolicy
import studio.guitarlab.core.codec.FileSeekableByteSource
import studio.guitarlab.core.codec.WavMetadataReader
import studio.guitarlab.core.codec.WavPcmDecoder
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
import studio.guitarlab.core.project.FileProjectRepository
import studio.guitarlab.core.project.ProjectClipEditor
import studio.guitarlab.core.project.ProjectTrackEditor
import studio.guitarlab.core.project.ProjectHistory
import studio.guitarlab.core.project.ProjectManagedMediaStore
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
import studio.guitarlab.core.project.ActiveTakePolicy
import studio.guitarlab.core.project.GuitarAuditionMode
import studio.guitarlab.core.project.LiveWaveformAccumulator
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
import studio.guitarlab.platform.codec.android.AndroidAudioImportTranscoder
import studio.guitarlab.platform.codec.android.MasterExportFormat

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
    val exporting: Boolean = false,
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
    val recordingConfig: StudioRecordingConfig? = null,
    val recordingPeak: Float = 0f,
    val recordingRms: Float = 0f,
    val liveRecordingPeaks: List<Float> = emptyList(),
    val guitarAuditionMode: GuitarAuditionMode = GuitarAuditionMode.MIXER,
    val sectionSuggestions: List<SectionBoundarySuggestion> = emptyList(),
    val trackLevelAnalysis: Map<String, LevelAnalysis> = emptyMap(),
    val masterGainDb: Float = 0f,
    val masterMeter: MeterBallisticsState = MeterBallisticsState(),
    val trackMeters: Map<String, MeterBallisticsState> = emptyMap(),
    val masterClipLatched: Boolean = false,
    val trackClipLatched: Set<String> = emptySet(),
    val canUndo: Boolean = false,
    val canRedo: Boolean = false,
    val error: String? = null,
    val importStatus: String? = null,
    val stereoImportPrompt: StereoImportPrompt? = null,
    val exportStatus: String? = null,
    val clipStatus: String? = null,
)

private data class ImportOutcome(
    val project: GuitarProject,
    val channelCount: Int,
    val sampleRateHz: Int,
    val peaks: List<Float>,
    val channelPeaks: List<List<Float>>,
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
    private val exportService = ProjectExportService(application)
    private var activeRecordingCompensationFrames: Long = 0L
    private var pendingRecordingPlan: PracticeRecordingStartPlan? = null
    private var activePunchPlan: PunchCapturePlan? = null
    private val liveWaveform = LiveWaveformAccumulator()
    private var playbackSessionId = 0L
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
        val before = _state.value
        if (before.project?.id == projectId && !before.loading && before.recordingSession.phase == RecordingSessionPhase.IDLE) {
            stopPlaybackSession()
            _state.value = before.copy(transport = before.transport.copy(mode = TransportMode.STOPPED))
        } else if (before.recordingSession.phase == RecordingSessionPhase.CAPTURING) {
            stopRecording()
            return
        } else if (before.recordingSession.phase == RecordingSessionPhase.COUNTDOWN) {
            cancelRecordingCountdown()
        }
        stopPlaybackSession()
        projectHistory.clear()
        trackMixDrafts.clear()
        viewModelScope.launch {
            _state.value = StudioUiState(loading = true)
            runCatching {
                withContext(Dispatchers.IO) {
                    val project = repository.load(projectId) ?: error("Projeto não encontrado: $projectId")
                    Triple(project, loadWaveforms(project), loadWaveformChannels(project))
                }
            }.onSuccess { (project, waveforms, waveformChannels) ->
                project.tracks.forEach { trackMixDrafts[it.id] = TrackMixDraft(it.gainDb, it.pan) }
                val end = TimelineControlPolicy.projectEndFrame(project)
                _state.value = StudioUiState(
                    loading = false,
                    project = project,
                    waveforms = waveforms,
                    waveformChannels = waveformChannels,
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
                        val envelope = FileSeekableByteSource(finalEditingFile).use { source -> WaveformEnvelopeBuilder.build(WavPcmDecoder(source), WAVEFORM_POINTS) }
                        waveformCache.write(current.id, clipId, envelope)
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
                    canUndo = projectHistory.canUndo,
                    canRedo = projectHistory.canRedo,
                    importStatus = if (imported.sourceSampleRateHz != imported.editingSampleRateHz) "${imported.sourceFormat} importado • ${outcome.channelCount} canal(is) • ${imported.sourceSampleRateHz} → ${imported.editingSampleRateHz} Hz" else "${imported.sourceFormat} importado • ${outcome.channelCount} canal(is) • ${outcome.sampleRateHz} Hz",
                    stereoImportPrompt = if (outcome.channelCount == 2) stereoPromptFor(saved, imported) else null,
                    error = null,
                )
            }.onFailure { error ->
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
                        StereoWavChannelSplitter.split(editingFile, leftTemp, rightTemp)
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
                                val leftClip = sourceClip.copy(
                                    id = UUID.randomUUID().toString(), trackId = leftTrack.id,
                                    name = "${sourceClip.name} · L", managedEditProxyPath = leftProxy.relativePath,
                                    sourceChannelCount = 1, sourceBitsPerSample = 32,
                                    sourceEncoding = "IEEE_FLOAT · canal L derivado",
                                    editingSampleRateHz = sourceClip.editingSampleRateHz ?: sourceClip.sourceSampleRateHz,
                                    editingTotalFrames = sourceClip.editingTotalFrames ?: sourceClip.lengthFrames,
                                )
                                val rightClip = sourceClip.copy(
                                    id = UUID.randomUUID().toString(), trackId = rightTrack.id,
                                    name = "${sourceClip.name} · R", managedEditProxyPath = rightProxy.relativePath,
                                    sourceChannelCount = 1, sourceBitsPerSample = 32,
                                    sourceEncoding = "IEEE_FLOAT · canal R derivado",
                                    editingSampleRateHz = sourceClip.editingSampleRateHz ?: sourceClip.sourceSampleRateHz,
                                    editingTotalFrames = sourceClip.editingTotalFrames ?: sourceClip.lengthFrames,
                                )
                                latest.copy(
                                    tracks = tracks.sortedBy { it.order },
                                    clips = latest.clips.filterNot { it.id == sourceClip.id } + leftClip + rightClip,
                                    updatedAtEpochMs = System.currentTimeMillis(),
                                )
                            }.also { committed = true }
                        }
                        val waveforms = loadWaveforms(saved)
                        Triple(saved, waveforms, loadWaveformChannels(saved))
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
                    canUndo = projectHistory.canUndo,
                    canRedo = projectHistory.canRedo,
                    importStatus = "Estéreo separado em L/R mono com sincronismo preservado",
                    error = null,
                )
            }.onFailure { error ->
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

    fun startRecording(mode: PracticeRecordingMode = PracticeRecordingMode.CURRENT_PLAYHEAD) {
        val current = _state.value
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
            liveRecordingPeaks = emptyList(),
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
            recordingMediaStore.begin(project.id, "take-${System.currentTimeMillis()}.wav")
        }.getOrElse { error ->
            resetRecording(error.message ?: "Não foi possível abrir a transação da gravação.")
            return
        }
        recordingTransaction = transaction
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
        recordingEngine.stop()
    }

    private val recordingListener = object : StudioRecordingListener {
        override fun onStarted(config: StudioRecordingConfig) = viewModelScope.launch {
            val state = _state.value
            if (!state.recordingSession.readyToOpenCapture) return@launch
            activeRecordingCompensationFrames = latencyCalibrationStore.find(
                inputSignature = audioRoutingStore.selectedInputSignature(),
                outputSignature = audioRoutingStore.selectedOutputSignature(),
                sampleRateHz = config.sampleRateHz,
            )?.takeIf { it.accepted }?.latencyFrames ?: 0L
            val launchPlan = pendingRecordingPlan
            activePunchPlan = launchPlan?.punchRegion?.let { PunchRecordingPolicy.plan(it, activeRecordingCompensationFrames) }
            pendingRecordingPlan = null
            _state.value = state.copy(
                recordingSession = RecordingSessionPolicy.markCaptureStarted(state.recordingSession),
                recordingConfig = config,
                transport = TransportPolicy.startRecording(state.transport),
                liveRecordingPeaks = emptyList(),
                clipStatus = "Gravando • ${config.routedInputLabel ?: "entrada automática"} • ${config.sampleRateHz} Hz • ${config.channelCount} canal(is)",
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
                liveRecordingPeaks = liveWaveform.append(peak),
                trackClipLatched = if (peak > 1f && targetId != null) state.trackClipLatched + targetId else state.trackClipLatched,
            )
            activePunchPlan?.let { plan ->
                if (framesCaptured >= plan.automaticStopAfterFrames && _state.value.recordingSession.phase == RecordingSessionPhase.CAPTURING) stopRecording()
            }
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
        val punchPlanForTake = activePunchPlan
        activePunchPlan = null
        pendingRecordingPlan = null
        viewModelScope.launch(start = CoroutineStart.UNDISPATCHED) {
            var committed = false
            runCatching {
                withContext(Dispatchers.IO) {
                    recordingMediaStore.commit(transaction)
                    val before = repository.load(transaction.projectId) ?: error("Projeto não encontrado ao finalizar o take.")
                    val session = _state.value.recordingSession
                    val targetTrackId = requireNotNull(session.targetTrackId) { "A pista do take não está mais disponível." }
                    val placement = LatencyCompensationPolicy.compensate(
                        requestedTimelineStartFrame = session.timelineStartFrame,
                        capturedFrames = result.framesCaptured,
                        roundTripLatencyFrames = activeRecordingCompensationFrames,
                    )
                    val punch = punchPlanForTake?.takeIf { !result.partial && result.framesCaptured >= it.keptSourceStartFrame + it.keptLengthFrames }
                    val timelineStart = punch?.keptTimelineStartFrame ?: placement.timelineStartFrame
                    val sourceStart = punch?.keptSourceStartFrame ?: placement.sourceStartFrame
                    val length = punch?.keptLengthFrames ?: placement.lengthFrames
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
                        WaveformEnvelopeBuilder.build(WavPcmDecoder(source), WAVEFORM_POINTS)
                    }
                    waveformCache.write(saved.id, clip.id, envelope)
                    val persisted = withContext(NonCancellable) {
                        repository.save(saved).also {
                            committed = true
                            projectHistory.record(before, it)
                        }
                    }
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
                    liveRecordingPeaks = emptyList(),
                    transportEngineReady = playbackReadiness(saved).ready,
                    canUndo = projectHistory.canUndo,
                    canRedo = projectHistory.canRedo,
                    clipStatus = when {
                        result.partial -> "Take parcial preservado com segurança"
                        activeRecordingCompensationFrames > 0L -> "Take gravado e compensado em ${activeRecordingCompensationFrames} frames"
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
                resetRecording(error.message ?: "Não foi possível integrar o take ao projeto.")
            }
        }
    }

    private fun resetRecording(message: String) {
        countdownJob?.cancel()
        countdownJob = null
        pendingRecordingPlan = null
        activePunchPlan = null
        val state = _state.value
        _state.value = state.copy(
            transport = state.transport.copy(mode = TransportMode.STOPPED),
            recordingSession = RecordingSessionPolicy.reset(),
            recordingConfig = null,
            recordingPeak = 0f,
            recordingRms = 0f,
            liveRecordingPeaks = emptyList(),
            clipStatus = null,
            error = message,
        )
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

    fun toggleTrackMuted(trackId: String) = editTrackAudibility(trackId, toggleMute = true)

    fun toggleTrackSolo(trackId: String) = editTrackAudibility(trackId, toggleMute = false)

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
                saved.tracks.forEach { track -> trackMixDrafts[track.id] = TrackMixDraft(track.gainDb, track.pan) }
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

    fun setGuitarAuditionMode(mode: GuitarAuditionMode) {
        playbackEngine.setAuditionMode(mode)
        _state.value = _state.value.copy(guitarAuditionMode = mode)
    }

    fun activateTake(takeId: String) {
        val project = _state.value.project ?: return
        if (!structuralEditingAllowed(_state.value)) return
        viewModelScope.launch {
            runCatching { saveLatest(project.id) { ActiveTakePolicy.activate(it, takeId, System.currentTimeMillis()) } }
                .onSuccess { saved -> _state.value = _state.value.copy(project = saved, transportEngineReady = playbackReadiness(saved).ready) }
                .onFailure { error -> _state.value = _state.value.copy(error = error.message) }
        }
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
        val state = _state.value; val project = state.project ?: return
        viewModelScope.launch {
            runCatching { withContext(Dispatchers.IO) {
                val accumulator = TrackLevelAccumulator()
                ActiveTakePolicy.audibleClips(project).filter { it.trackId == trackId && !it.muted }.forEach { clip ->
                    FileSeekableByteSource(mediaStore.resolveEditable(project.id, editingMediaPath(clip))).use { source ->
                        WavPcmDecoder(source).use { decoder ->
                            decoder.seekToFrame(clip.sourceStartFrame)
                            val buffer = FloatArray(2048 * decoder.metadata.channelCount)
                            var remaining = clip.lengthFrames
                            while (remaining > 0) {
                                val requested = minOf(2048L, remaining).toInt(); val read = decoder.readInterleaved(buffer, frameCount = requested)
                                if (read <= 0) break
                                accumulator.append(buffer, read * decoder.metadata.channelCount); remaining -= read
                            }
                        }
                    }
                }
                accumulator.finish()
            } }.onSuccess { analysis -> _state.value = _state.value.copy(trackLevelAnalysis = _state.value.trackLevelAnalysis + (trackId to analysis)) }
                .onFailure { error -> _state.value = _state.value.copy(error = error.message ?: "Não foi possível analisar o nível.") }
        }
    }

    fun applyTrackLevelSuggestion(trackId: String) {
        val project = _state.value.project ?: return; val analysis = _state.value.trackLevelAnalysis[trackId] ?: return
        viewModelScope.launch {
            runCatching { saveLatest(project.id) { current -> current.copy(tracks = current.tracks.map { if (it.id == trackId) it.copy(gainDb = (it.gainDb + analysis.recommendedGainDb).coerceIn(-60f, 12f)) else it }, updatedAtEpochMs = System.currentTimeMillis()) } }
                .onSuccess { saved -> _state.value = _state.value.copy(project = saved, trackLevelAnalysis = _state.value.trackLevelAnalysis - trackId) }
                .onFailure { error -> _state.value = _state.value.copy(error = error.message) }
        }
    }

    private fun updatePracticeProject(transform: (GuitarProject) -> GuitarProject) {
        val state = _state.value; val project = state.project ?: return
        if (!structuralEditingAllowed(state)) return
        viewModelScope.launch {
            runCatching { saveLatest(project.id, transform) }
                .onSuccess { saved -> _state.value = _state.value.copy(project = saved, transportEngineReady = playbackReadiness(saved).ready) }
                .onFailure { error -> _state.value = _state.value.copy(error = error.message) }
        }
    }

    fun onStudioHidden() {
        when (_state.value.recordingSession.phase) {
            RecordingSessionPhase.COUNTDOWN -> cancelRecordingCountdown()
            RecordingSessionPhase.CAPTURING -> stopRecording()
            else -> stopPlaybackSession()
        }
    }

    private fun stopPlaybackSession() {
        playbackSessionId++
        playbackEngine.stop()
    }

    override fun onCleared() {
        countdownJob?.cancel()
        pendingRecordingPlan = null
        activePunchPlan = null
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
                    viewModelScope.launch {
                        val state = _state.value
                        if (sessionId != playbackSessionId) return@launch
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

    private fun startBackingPlaybackForRecording(sampleRateHz: Int) {
        val state = _state.value
        val project = state.project ?: return
        val end = TimelineControlPolicy.projectEndFrame(project)
        if (end <= 0L || project.clips.isEmpty()) return
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
            trackMixes = project.tracks.map {
                StudioPlaybackTrackMix(trackId = it.id, roleId = it.roleId, gainDb = it.gainDb, pan = it.pan, muted = it.muted, solo = it.solo)
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
                saved to withContext(Dispatchers.IO) {
                    loadWaveforms(saved).also {
                        waveformCache.prune(saved.id, saved.clips.mapTo(mutableSetOf()) { clip -> clip.id })
                    }
                }
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

    fun renameProject(name: String) {
        val normalized = name.trim().replace(Regex("\\s+"), " ")
        if (normalized.isBlank() || normalized.length > 80) {
            _state.value = _state.value.copy(error = "O nome do projeto deve ter entre 1 e 80 caracteres.")
            return
        }
        val current = _state.value
        val project = current.project ?: return
        if (current.importing || current.editingClip || current.historyBusy || current.exporting) return
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
        if (state.importing || state.editingClip || state.historyBusy || state.exporting || state.trimControls != null) return
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
                saved.tracks.forEach { track ->
                    playbackEngine.setTrackAudibility(track.id, track.muted, track.solo)
                }
                val current = _state.value
                _state.value = current.copy(
                    project = saved,
                    canUndo = projectHistory.canUndo,
                    canRedo = projectHistory.canRedo,
                    clipStatus = if (toggleMute) "Mute da pista atualizado" else "Solo da pista atualizado",
                    error = null,
                )
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
        saved.tracks.forEach { track -> trackMixDrafts[track.id] = TrackMixDraft(track.gainDb, track.pan) }
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
            return PlaybackReadiness(false, reason = "Há clipes sem proxy convertido para a taxa do projeto.")
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

    private fun loadWaveformChannels(project: GuitarProject): Map<String, List<List<Float>>> = buildMap {
        project.clips.forEach { clip ->
            val cached = waveformCache.read(project.id, clip.id)
            if (cached != null && cached.channelPeaks.size > 1) {
                put(clip.id, cached.channelPeaks)
                return@forEach
            }
            if (clip.sourceChannelCount != 2) return@forEach
            val managedPath = editingMediaPathOrNull(clip) ?: return@forEach
            runCatching {
                val file = mediaStore.resolveEditable(project.id, managedPath)
                val envelope = FileSeekableByteSource(file).use { source -> WaveformEnvelopeBuilder.build(WavPcmDecoder(source), WAVEFORM_POINTS) }
                waveformCache.write(project.id, clip.id, envelope)
                if (envelope.channelPeaks.size == 2) put(clip.id, envelope.channelPeaks)
            }
        }
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
                exportService.saveProject(project.id, uri)
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
                    require(readiness.ready) { readiness.reason ?: "Projeto sem áudio exportável." }
                    exportService.exportMaster(project.id, uri, format)
                }
            }.onSuccess {
                _state.value = _state.value.copy(exporting = false, exportStatus = "Master ${format.name} exportado com sucesso")
            }.onFailure { error ->
                _state.value = _state.value.copy(exporting = false, exportStatus = null, error = error.message ?: "Não foi possível exportar o master.")
            }
        }
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
