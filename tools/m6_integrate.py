#!/usr/bin/env python3
from pathlib import Path
import subprocess

ROOT = Path(__file__).resolve().parents[1]

def read(path):
    return (ROOT / path).read_text()

def write(path, content):
    p = ROOT / path
    p.parent.mkdir(parents=True, exist_ok=True)
    p.write_text(content)

def replace_once(path, old, new):
    text = read(path)
    count = text.count(old)
    if count != 1:
        raise RuntimeError(f"{path}: expected exactly 1 anchor, found {count}: {old[:120]!r}")
    write(path, text.replace(old, new, 1))

# 1) Timeline marker timestamps.
replace_once(
    "app/src/main/java/studio/guitarlab/app/ui/TimelineMarkerRail.kt",
    """    projectEndFrame: Long,\n    playheadFrame: Long,""",
    """    projectEndFrame: Long,\n    sampleRateHz: Int,\n    playheadFrame: Long,""",
)
replace_once(
    "app/src/main/java/studio/guitarlab/app/ui/TimelineMarkerRail.kt",
    """            TimelineMarker(TimelineMarkerKind.LOOP_START, loopStartFrame, projectEndFrame, widthPx, enabled, onLoopStartFrameChanged)\n            TimelineMarker(TimelineMarkerKind.LOOP_END, loopEndFrame, projectEndFrame, widthPx, enabled, onLoopEndFrameChanged)\n        }\n        TimelineMarker(TimelineMarkerKind.PLAYHEAD, playheadFrame, projectEndFrame, widthPx, enabled, onPlayheadFrameChanged)""",
    """            TimelineMarker(TimelineMarkerKind.LOOP_START, loopStartFrame, projectEndFrame, sampleRateHz, widthPx, enabled, onLoopStartFrameChanged)\n            TimelineMarker(TimelineMarkerKind.LOOP_END, loopEndFrame, projectEndFrame, sampleRateHz, widthPx, enabled, onLoopEndFrameChanged)\n        }\n        TimelineMarker(TimelineMarkerKind.PLAYHEAD, playheadFrame, projectEndFrame, sampleRateHz, widthPx, enabled, onPlayheadFrameChanged)""",
)
replace_once(
    "app/src/main/java/studio/guitarlab/app/ui/TimelineMarkerRail.kt",
    """    projectEndFrame: Long,\n    widthPx: Float,""",
    """    projectEndFrame: Long,\n    sampleRateHz: Int,\n    widthPx: Float,""",
)
replace_once(
    "app/src/main/java/studio/guitarlab/app/ui/TimelineMarkerRail.kt",
    """    val markerWidth = 48.dp""",
    """    val markerWidth = 86.dp""",
)
replace_once(
    "app/src/main/java/studio/guitarlab/app/ui/TimelineMarkerRail.kt",
    """                        text = label,""",
    """                        text = \"$label ${formatFrameTime(frame, sampleRateHz)}\",""",
)
replace_once(
    "app/src/main/java/studio/guitarlab/app/ui/TimelineMarkerRail.kt",
    """private fun markerLabel(kind: TimelineMarkerKind): String = when (kind) {""",
    """internal fun formatFrameTime(frame: Long, sampleRateHz: Int): String {\n    val safeRate = sampleRateHz.coerceAtLeast(1)\n    val totalSeconds = (frame.coerceAtLeast(0L) / safeRate).coerceAtLeast(0L)\n    val hours = totalSeconds / 3600L\n    val minutes = (totalSeconds % 3600L) / 60L\n    val seconds = totalSeconds % 60L\n    return if (hours > 0L) \"%d:%02d:%02d\".format(hours, minutes, seconds) else \"%02d:%02d\".format(minutes, seconds)\n}\n\nprivate fun markerLabel(kind: TimelineMarkerKind): String = when (kind) {""",
)
replace_once(
    "app/src/main/java/studio/guitarlab/app/ui/StudioPlaceholderScreen.kt",
    """                        projectEndFrame = projectEndFrame,\n                        playheadFrame = timelineControls.playheadFrame,""",
    """                        projectEndFrame = projectEndFrame,\n                        sampleRateHz = project.sampleRate.fixedHz ?: clipSampleRate(project),\n                        playheadFrame = timelineControls.playheadFrame,""",
)

# 2) Runtime mute/solo in playback engine.
replace_once(
    "platform/audio-android/src/main/kotlin/studio/guitarlab/platform/audio/android/AndroidStudioPlaybackEngine.kt",
    """data class StudioPlaybackTrackMix(\n    val trackId: String,\n    val gainDb: Float = 0f,\n    val pan: Float = 0f,\n)""",
    """data class StudioPlaybackTrackMix(\n    val trackId: String,\n    val gainDb: Float = 0f,\n    val pan: Float = 0f,\n    val muted: Boolean = false,\n    val solo: Boolean = false,\n)""",
)
replace_once(
    "platform/audio-android/src/main/kotlin/studio/guitarlab/platform/audio/android/AndroidStudioPlaybackEngine.kt",
    """    fun setTrackMix(trackId: String, gainDb: Float, pan: Float) {\n        runtimeTrackMixes[trackId] = StudioPlaybackTrackMix(\n            trackId = trackId,\n            gainDb = gainDb.coerceIn(-60f, 12f),\n            pan = pan.coerceIn(-1f, 1f),\n        )\n    }""",
    """    fun setTrackMix(trackId: String, gainDb: Float, pan: Float) {\n        val current = runtimeTrackMixes[trackId] ?: StudioPlaybackTrackMix(trackId)\n        runtimeTrackMixes[trackId] = current.copy(\n            gainDb = gainDb.coerceIn(-60f, 12f),\n            pan = pan.coerceIn(-1f, 1f),\n        )\n    }\n\n    fun setTrackAudibility(trackId: String, muted: Boolean, solo: Boolean) {\n        val current = runtimeTrackMixes[trackId] ?: StudioPlaybackTrackMix(trackId)\n        runtimeTrackMixes[trackId] = current.copy(muted = muted, solo = solo)\n    }""",
)
replace_once(
    "platform/audio-android/src/main/kotlin/studio/guitarlab/platform/audio/android/AndroidStudioPlaybackEngine.kt",
    """        val runtime = runtimeTrackMixes[trackId] ?: return\n        val gain = TrackMixPolicy.channelGains(runtime.gainDb, runtime.pan)""",
    """        val runtime = runtimeTrackMixes[trackId] ?: return\n        val anySolo = runtimeTrackMixes.values.any { it.solo }\n        if (runtime.muted || (anySolo && !runtime.solo)) {\n            java.util.Arrays.fill(samples, 0, sampleCount, 0f)\n            return\n        }\n        val gain = TrackMixPolicy.channelGains(runtime.gainDb, runtime.pan)""",
)

# 3) ViewModel runtime mute/solo, project rename, latency compensation.
replace_once(
    "app/src/main/java/studio/guitarlab/app/ui/StudioViewModel.kt",
    """import studio.guitarlab.core.audio.TrackMixPolicy""",
    """import studio.guitarlab.core.audio.TrackMixPolicy\nimport studio.guitarlab.core.audio.LatencyCompensationPolicy""",
)
replace_once(
    "app/src/main/java/studio/guitarlab/app/ui/StudioViewModel.kt",
    """    private val audioRoutingStore = StudioAudioRoutingStore(application)""",
    """    private val audioRoutingStore = StudioAudioRoutingStore(application)\n    private val latencyCalibrationStore = StudioLatencyCalibrationStore(application)\n    private var activeRecordingCompensationFrames: Long = 0L""",
)
replace_once(
    "app/src/main/java/studio/guitarlab/app/ui/StudioViewModel.kt",
    """            _state.value = state.copy(\n                recordingSession = RecordingSessionPolicy.markCaptureStarted(state.recordingSession),""",
    """            activeRecordingCompensationFrames = latencyCalibrationStore.find(\n                inputSignature = audioRoutingStore.selectedInputSignature(),\n                outputSignature = audioRoutingStore.selectedOutputSignature(),\n                sampleRateHz = config.sampleRateHz,\n            )?.takeIf { it.accepted }?.latencyFrames ?: 0L\n            _state.value = state.copy(\n                recordingSession = RecordingSessionPolicy.markCaptureStarted(state.recordingSession),""",
)
replace_once(
    "app/src/main/java/studio/guitarlab/app/ui/StudioViewModel.kt",
    """                    val saved = RecordedTakeProjectIntegrator.integrate(\n                        project = before,\n                        targetTrackId = targetTrackId,\n                        take = RecordedTakeMetadata(\n                            clipId = transaction.id,\n                            displayName = \"Take ${before.clips.count { it.trackId == targetTrackId } + 1}\",\n                            managedRelativePath = transaction.relativePath,\n                            timelineStartFrame = session.timelineStartFrame,\n                            sampleRateHz = result.sampleRateHz,\n                            channelCount = result.channelCount,\n                            framesCaptured = result.framesCaptured,\n                        ),\n                        nowEpochMs = System.currentTimeMillis(),\n                    )\n                    val clip = saved.clips.first { it.id == transaction.id }""",
    """                    val placement = LatencyCompensationPolicy.compensate(\n                        requestedTimelineStartFrame = session.timelineStartFrame,\n                        capturedFrames = result.framesCaptured,\n                        roundTripLatencyFrames = activeRecordingCompensationFrames,\n                    )\n                    val integrated = RecordedTakeProjectIntegrator.integrate(\n                        project = before,\n                        targetTrackId = targetTrackId,\n                        take = RecordedTakeMetadata(\n                            clipId = transaction.id,\n                            displayName = \"Take ${before.clips.count { it.trackId == targetTrackId } + 1}\",\n                            managedRelativePath = transaction.relativePath,\n                            timelineStartFrame = placement.timelineStartFrame,\n                            sampleRateHz = result.sampleRateHz,\n                            channelCount = result.channelCount,\n                            framesCaptured = result.framesCaptured,\n                        ),\n                        nowEpochMs = System.currentTimeMillis(),\n                    )\n                    val saved = integrated.copy(\n                        clips = integrated.clips.map { candidate ->\n                            if (candidate.id == transaction.id) candidate.copy(\n                                sourceStartFrame = placement.sourceStartFrame,\n                                lengthFrames = placement.lengthFrames,\n                            ) else candidate\n                        },\n                        updatedAtEpochMs = System.currentTimeMillis(),\n                    )\n                    val clip = saved.clips.first { it.id == transaction.id }""",
)
replace_once(
    "app/src/main/java/studio/guitarlab/app/ui/StudioViewModel.kt",
    """                    clipStatus = if (result.partial) \"Take parcial preservado com segurança\" else \"Take gravado com sucesso\",""",
    """                    clipStatus = when {\n                        result.partial -> \"Take parcial preservado com segurança\"\n                        activeRecordingCompensationFrames > 0L -> \"Take gravado e compensado em ${activeRecordingCompensationFrames} frames\"\n                        else -> \"Take gravado com sucesso\"\n                    },""",
)
replace_once(
    "app/src/main/java/studio/guitarlab/app/ui/StudioViewModel.kt",
    """            recordingRms = 0f,\n            clipStatus = null,""",
    """            recordingRms = 0f,\n            clipStatus = null,""",
)
# runtime mute/solo methods replace structural-only versions
replace_once(
    "app/src/main/java/studio/guitarlab/app/ui/StudioViewModel.kt",
    """    fun toggleTrackMuted(trackId: String) {\n        editTrackStructural(\"Mute da pista atualizado\", trackId) { it.copy(muted = !it.muted) }\n    }\n\n    fun toggleTrackSolo(trackId: String) {\n        editTrackStructural(\"Solo da pista atualizado\", trackId) { it.copy(solo = !it.solo) }\n    }""",
    """    fun toggleTrackMuted(trackId: String) = editTrackAudibility(trackId, toggleMute = true)\n\n    fun toggleTrackSolo(trackId: String) = editTrackAudibility(trackId, toggleMute = false)""",
)
# insert rename + runtime audibility helper before editTrackStructural
replace_once(
    "app/src/main/java/studio/guitarlab/app/ui/StudioViewModel.kt",
    """    private fun editTrackStructural(status: String, trackId: String, transform: (AudioTrack) -> AudioTrack) {""",
    """    fun renameProject(name: String) {\n        val normalized = name.trim().replace(Regex(\"\\\\s+\"), \" \")\n        if (normalized.isBlank() || normalized.length > 80) {\n            _state.value = _state.value.copy(error = \"O nome do projeto deve ter entre 1 e 80 caracteres.\")\n            return\n        }\n        val current = _state.value\n        val project = current.project ?: return\n        if (current.importing || current.editingClip || current.historyBusy || current.exporting) return\n        viewModelScope.launch {\n            runCatching {\n                saveLatest(project.id) { latest ->\n                    latest.copy(name = normalized, updatedAtEpochMs = System.currentTimeMillis())\n                }\n            }.onSuccess { applySavedProject(it, \"Projeto renomeado\") }\n                .onFailure { error -> _state.value = _state.value.copy(error = error.message ?: \"Não foi possível renomear o projeto.\") }\n        }\n    }\n\n    private fun editTrackAudibility(trackId: String, toggleMute: Boolean) {\n        val state = _state.value\n        val project = state.project ?: return\n        if (state.importing || state.editingClip || state.historyBusy || state.exporting || state.trimControls != null) return\n        viewModelScope.launch {\n            runCatching {\n                saveLatest(project.id) { latest ->\n                    val target = latest.tracks.firstOrNull { it.id == trackId } ?: return@saveLatest latest\n                    val updated = if (toggleMute) target.copy(muted = !target.muted) else target.copy(solo = !target.solo)\n                    latest.copy(\n                        tracks = latest.tracks.map { if (it.id == trackId) updated else it },\n                        updatedAtEpochMs = System.currentTimeMillis(),\n                    )\n                }\n            }.onSuccess { saved ->\n                saved.tracks.forEach { track ->\n                    playbackEngine.setTrackAudibility(track.id, track.muted, track.solo)\n                }\n                val current = _state.value\n                _state.value = current.copy(\n                    project = saved,\n                    canUndo = projectHistory.canUndo,\n                    canRedo = projectHistory.canRedo,\n                    clipStatus = if (toggleMute) \"Mute da pista atualizado\" else \"Solo da pista atualizado\",\n                    error = null,\n                )\n            }.onFailure { error -> _state.value = _state.value.copy(error = error.message ?: \"Não foi possível atualizar a audição da pista.\") }\n        }\n    }\n\n    private fun editTrackStructural(status: String, trackId: String, transform: (AudioTrack) -> AudioTrack) {""",
)
# include all tracks/clips at start, audibility runtime
replace_once(
    "app/src/main/java/studio/guitarlab/app/ui/StudioViewModel.kt",
    """        val anySolo = project.tracks.any { it.solo }\n        val tracksById = project.tracks.associateBy { it.id }\n        val selectedOutputSignature = audioRoutingStore.selectedOutputSignature()\n        val preferredOutput = audioRoutingStore.resolveSelectedOutputDevice()\n        val audibleTrackIds = project.tracks.filter { TrackMixPolicy.isAudible(it.muted, it.solo, anySolo) }.map { it.id }.toSet()""",
    """        val tracksById = project.tracks.associateBy { it.id }\n        val selectedOutputSignature = audioRoutingStore.selectedOutputSignature()\n        val preferredOutput = audioRoutingStore.resolveSelectedOutputDevice()""",
)
replace_once(
    "app/src/main/java/studio/guitarlab/app/ui/StudioViewModel.kt",
    """                trackMixes = project.tracks.filter { it.id in audibleTrackIds }.map { track ->\n                    StudioPlaybackTrackMix(trackId = track.id, gainDb = track.gainDb, pan = track.pan)\n                },""",
    """                trackMixes = project.tracks.map { track ->\n                    StudioPlaybackTrackMix(\n                        trackId = track.id,\n                        gainDb = track.gainDb,\n                        pan = track.pan,\n                        muted = track.muted,\n                        solo = track.solo,\n                    )\n                },""",
)
replace_once(
    "app/src/main/java/studio/guitarlab/app/ui/StudioViewModel.kt",
    """                    if (clip.muted || sourceTrack.id !in audibleTrackIds) return@mapNotNull null""",
    """                    if (clip.muted) return@mapNotNull null""",
)
# backing recording: include all non-clip-muted compatible tracks and runtime audibility
replace_once(
    "app/src/main/java/studio/guitarlab/app/ui/StudioViewModel.kt",
    """        val anySolo = project.tracks.any { it.solo }\n        val tracksById = project.tracks.associateBy { it.id }\n        val audibleTrackIds = project.tracks\n            .filter { TrackMixPolicy.isAudible(it.muted, it.solo, anySolo) }\n            .map { it.id }\n            .toSet()""",
    """        val tracksById = project.tracks.associateBy { it.id }""",
)
replace_once(
    "app/src/main/java/studio/guitarlab/app/ui/StudioViewModel.kt",
    """                if (clip.muted || track.id !in audibleTrackIds || clip.sourceSampleRateHz != sampleRateHz) return@mapNotNull null""",
    """                if (clip.muted || clip.sourceSampleRateHz != sampleRateHz) return@mapNotNull null""",
)
replace_once(
    "app/src/main/java/studio/guitarlab/app/ui/StudioViewModel.kt",
    """            trackMixes = project.tracks.filter { it.id in audibleTrackIds }.map {\n                StudioPlaybackTrackMix(it.id, it.gainDb, it.pan)\n            },""",
    """            trackMixes = project.tracks.map {\n                StudioPlaybackTrackMix(it.id, it.gainDb, it.pan, muted = it.muted, solo = it.solo)\n            },""",
)

# 4) Mixer M/S enabled with live mix controls; Arm stays structural.
replace_once(
    "app/src/main/java/studio/guitarlab/app/ui/MixerDock.kt",
    """                MixerStateButton(\"M\", track.muted, StudioMute, structuralControlsEnabled, onToggleMute)\n                MixerStateButton(\"S\", track.solo, StudioSolo, structuralControlsEnabled, onToggleSolo)""",
    """                MixerStateButton(\"M\", track.muted, StudioMute, mixControlsEnabled, onToggleMute)\n                MixerStateButton(\"S\", track.solo, StudioSolo, mixControlsEnabled, onToggleSolo)""",
)

# 5) Top bar time summary + rename project.
replace_once(
    "app/src/main/java/studio/guitarlab/app/ui/StudioShellScreen.kt",
    """import androidx.compose.material.icons.filled.Equalizer""",
    """import androidx.compose.material.icons.filled.Equalizer\nimport androidx.compose.material.icons.filled.Edit""",
)
replace_once(
    "app/src/main/java/studio/guitarlab/app/ui/StudioShellScreen.kt",
    """import androidx.compose.material3.OutlinedButton""",
    """import androidx.compose.material3.OutlinedButton\nimport androidx.compose.material3.OutlinedTextField""",
)
replace_once(
    "app/src/main/java/studio/guitarlab/app/ui/StudioShellScreen.kt",
    """import studio.guitarlab.core.project.TransportPolicy""",
    """import studio.guitarlab.core.project.TransportPolicy\nimport studio.guitarlab.core.project.TimelineControlPolicy""",
)
replace_once(
    "app/src/main/java/studio/guitarlab/app/ui/StudioShellScreen.kt",
    """    var exportDialogVisible by rememberSaveable(projectId) { mutableStateOf(false) }""",
    """    var exportDialogVisible by rememberSaveable(projectId) { mutableStateOf(false) }\n    var renameDialogVisible by rememberSaveable(projectId) { mutableStateOf(false) }""",
)
replace_once(
    "app/src/main/java/studio/guitarlab/app/ui/StudioShellScreen.kt",
    """            StudioTopBar(\n                project = state.project,""",
    """            val topBarProject = state.project\n            val topBarEnd = topBarProject?.let(TimelineControlPolicy::projectEndFrame) ?: 0L\n            val topBarRate = topBarProject?.sampleRate?.fixedHz\n                ?: topBarProject?.clips?.firstNotNullOfOrNull { it.sourceSampleRateHz }\n                ?: 48_000\n            StudioTopBar(\n                project = topBarProject,\n                remainingText = formatFrameTime((topBarEnd - state.timelineControls.playheadFrame).coerceAtLeast(0L), topBarRate),\n                totalText = formatFrameTime(topBarEnd, topBarRate),""",
)
replace_once(
    "app/src/main/java/studio/guitarlab/app/ui/StudioShellScreen.kt",
    """                onShare = { exportDialogVisible = true },\n                onHome = onBack,""",
    """                onShare = { exportDialogVisible = true },\n                onRename = { renameDialogVisible = true },\n                onHome = onBack,""",
)
# rename dialog before export dialog
replace_once(
    "app/src/main/java/studio/guitarlab/app/ui/StudioShellScreen.kt",
    """    if (exportDialogVisible && state.project != null) {""",
    """    if (renameDialogVisible && state.project != null) {\n        RenameProjectDialog(\n            currentName = state.project!!.name,\n            onDismiss = { renameDialogVisible = false },\n            onConfirm = { newName ->\n                viewModel.renameProject(newName)\n                renameDialogVisible = false\n            },\n        )\n    }\n\n    if (exportDialogVisible && state.project != null) {""",
)
replace_once(
    "app/src/main/java/studio/guitarlab/app/ui/StudioShellScreen.kt",
    """private fun StudioTopBar(\n    project: GuitarProject?,\n    transport: @Composable () -> Unit,""",
    """private fun StudioTopBar(\n    project: GuitarProject?,\n    remainingText: String,\n    totalText: String,\n    transport: @Composable () -> Unit,""",
)
replace_once(
    "app/src/main/java/studio/guitarlab/app/ui/StudioShellScreen.kt",
    """    onShare: () -> Unit,\n    onHome: () -> Unit,""",
    """    onShare: () -> Unit,\n    onRename: () -> Unit,\n    onHome: () -> Unit,""",
)
replace_once(
    "app/src/main/java/studio/guitarlab/app/ui/StudioShellScreen.kt",
    """                Text(project?.name ?: \"GuitarLab\", style = MaterialTheme.typography.titleLarge, maxLines = 1)""",
    """                Row(verticalAlignment = Alignment.CenterVertically) {\n                    Text(\n                        project?.name ?: \"GuitarLab\",\n                        modifier = Modifier.weight(1f, fill = false),\n                        style = MaterialTheme.typography.titleLarge,\n                        maxLines = 1,\n                    )\n                    if (project != null) {\n                        AppIconButton(\n                            icon = Icons.Default.Edit,\n                            contentDescription = \"Renomear projeto\",\n                            onClick = onRename,\n                        )\n                    }\n                }""",
)
replace_once(
    "app/src/main/java/studio/guitarlab/app/ui/StudioShellScreen.kt",
    """            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) { transport() }\n\n            Row(horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.CenterVertically) {""",
    """            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) { transport() }\n\n            Surface(\n                shape = RoundedCornerShape(10.dp),\n                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.30f),\n                tonalElevation = 0.dp,\n            ) {\n                Row(\n                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),\n                    horizontalArrangement = Arrangement.spacedBy(8.dp),\n                    verticalAlignment = Alignment.CenterVertically,\n                ) {\n                    Text(\"Restante $remainingText\", style = MaterialTheme.typography.labelMedium)\n                    Text(\"·\", color = MaterialTheme.colorScheme.onSurfaceVariant)\n                    Text(\"Total $totalText\", style = MaterialTheme.typography.labelMedium)\n                }\n            }\n\n            Row(horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.CenterVertically) {""",
)
# add rename dialog function before SaveAndExportDialog
replace_once(
    "app/src/main/java/studio/guitarlab/app/ui/StudioShellScreen.kt",
    """@Composable\nprivate fun SaveAndExportDialog(""",
    """@Composable\nprivate fun RenameProjectDialog(\n    currentName: String,\n    onDismiss: () -> Unit,\n    onConfirm: (String) -> Unit,\n) {\n    var value by remember(currentName) { mutableStateOf(currentName) }\n    val normalized = value.trim().replace(Regex(\"\\\\s+\"), \" \")\n    AlertDialog(\n        onDismissRequest = onDismiss,\n        title = { Text(\"Renomear projeto\") },\n        text = {\n            OutlinedTextField(\n                value = value,\n                onValueChange = { if (it.length <= 80) value = it },\n                modifier = Modifier.fillMaxWidth(),\n                label = { Text(\"Nome do projeto\") },\n                supportingText = { Text(\"${value.length}/80\") },\n                singleLine = true,\n            )\n        },\n        confirmButton = {\n            Button(\n                enabled = normalized.isNotBlank() && normalized != currentName,\n                onClick = { onConfirm(normalized) },\n            ) { Text(\"Renomear\") }\n        },\n        dismissButton = { TextButton(onClick = onDismiss) { Text(\"Cancelar\") } },\n    )\n}\n\n@Composable\nprivate fun SaveAndExportDialog(""",
)

# 6) M6 core calibration/compensation policy.
write(
    "core/audio/src/main/kotlin/studio/guitarlab/core/audio/LatencyCalibrationPolicy.kt",
    '''package studio.guitarlab.core.audio\n\nimport kotlin.math.abs\n\ndata class LatencyCalibration(\n    val latencyFrames: Long,\n    val jitterFrames: Long,\n    val driftPpm: Double,\n    val confidence: Float,\n    val sampleRateHz: Int,\n    val attempts: Int,\n    val accepted: Boolean,\n    val measuredAtEpochMs: Long,\n)\n\ndata class CompensatedTakePlacement(\n    val timelineStartFrame: Long,\n    val sourceStartFrame: Long,\n    val lengthFrames: Long,\n)\n\nobject LatencyCalibrationPolicy {\n    const val MIN_CONFIDENCE = 0.55f\n    const val MAX_JITTER_MS = 8.0\n    const val MAX_ABS_DRIFT_PPM = 2_000.0\n\n    fun evaluate(\n        measurementsFrames: List<Long>,\n        confidences: List<Float>,\n        sampleRateHz: Int,\n        elapsedMs: Long,\n        measuredAtEpochMs: Long,\n    ): LatencyCalibration {\n        require(sampleRateHz > 0)\n        require(measurementsFrames.isNotEmpty())\n        require(measurementsFrames.size == confidences.size)\n        val sorted = measurementsFrames.sorted()\n        val median = sorted[sorted.size / 2].coerceAtLeast(0L)\n        val deviations = measurementsFrames.map { abs(it - median) }.sorted()\n        val jitter = deviations[deviations.size / 2]\n        val confidence = confidences.average().toFloat()\n        val driftPpm = if (measurementsFrames.size >= 2 && elapsedMs > 0L) {\n            val deltaFrames = measurementsFrames.last() - measurementsFrames.first()\n            deltaFrames.toDouble() / sampleRateHz.toDouble() / (elapsedMs / 1000.0) * 1_000_000.0\n        } else 0.0\n        val jitterMs = jitter * 1000.0 / sampleRateHz\n        val accepted = confidence >= MIN_CONFIDENCE && jitterMs <= MAX_JITTER_MS && abs(driftPpm) <= MAX_ABS_DRIFT_PPM\n        return LatencyCalibration(median, jitter, driftPpm, confidence, sampleRateHz, measurementsFrames.size, accepted, measuredAtEpochMs)\n    }\n}\n\nobject LatencyCompensationPolicy {\n    fun compensate(\n        requestedTimelineStartFrame: Long,\n        capturedFrames: Long,\n        roundTripLatencyFrames: Long,\n    ): CompensatedTakePlacement {\n        require(requestedTimelineStartFrame >= 0L)\n        require(capturedFrames > 0L)\n        val latency = roundTripLatencyFrames.coerceAtLeast(0L)\n        if (latency == 0L) return CompensatedTakePlacement(requestedTimelineStartFrame, 0L, capturedFrames)\n        if (requestedTimelineStartFrame >= latency) {\n            return CompensatedTakePlacement(requestedTimelineStartFrame - latency, 0L, capturedFrames)\n        }\n        val sourceTrim = (latency - requestedTimelineStartFrame).coerceAtMost(capturedFrames - 1L)\n        return CompensatedTakePlacement(0L, sourceTrim, capturedFrames - sourceTrim)\n    }\n}\n'''
)
write(
    "core/audio/src/test/kotlin/studio/guitarlab/core/audio/LatencyCalibrationPolicyTest.kt",
    '''package studio.guitarlab.core.audio\n\nimport kotlin.test.Test\nimport kotlin.test.assertEquals\nimport kotlin.test.assertFalse\nimport kotlin.test.assertTrue\n\nclass LatencyCalibrationPolicyTest {\n    @Test\n    fun stableMeasurementsAreAcceptedAndMedianIsUsed() {\n        val result = LatencyCalibrationPolicy.evaluate(\n            measurementsFrames = listOf(4700, 4704, 4698, 4702, 4701),\n            confidences = listOf(.91f, .93f, .92f, .94f, .90f),\n            sampleRateHz = 48_000,\n            elapsedMs = 8_000,\n            measuredAtEpochMs = 1L,\n        )\n        assertEquals(4701, result.latencyFrames)\n        assertTrue(result.accepted)\n    }\n\n    @Test\n    fun lowConfidenceIsRejected() {\n        val result = LatencyCalibrationPolicy.evaluate(\n            listOf(2000, 2001, 1999), listOf(.2f, .3f, .25f), 48_000, 4_000, 1L,\n        )\n        assertFalse(result.accepted)\n    }\n\n    @Test\n    fun compensationMovesTakeEarlierAndTrimsWhenTimelineCannotGoNegative() {\n        assertEquals(CompensatedTakePlacement(38_000, 0, 48_000), LatencyCompensationPolicy.compensate(40_000, 48_000, 2_000))\n        assertEquals(CompensatedTakePlacement(0, 1_000, 47_000), LatencyCompensationPolicy.compensate(1_000, 48_000, 2_000))\n    }\n}\n'''
)

# 7) Android loopback measurement + route-scoped persistence.
write(
    "app/src/main/java/studio/guitarlab/app/ui/StudioLatencyCalibration.kt",
    '''package studio.guitarlab.app.ui\n\nimport android.Manifest\nimport android.content.Context\nimport android.content.pm.PackageManager\nimport android.media.AudioAttributes\nimport android.media.AudioDeviceInfo\nimport android.media.AudioFormat\nimport android.media.AudioRecord\nimport android.media.AudioTimestamp\nimport android.media.AudioTrack\nimport android.media.MediaRecorder\nimport androidx.core.content.ContextCompat\nimport java.util.Locale\nimport kotlin.math.abs\nimport kotlin.math.max\nimport kotlin.math.roundToLong\nimport kotlin.math.sqrt\nimport kotlinx.coroutines.Dispatchers\nimport kotlinx.coroutines.delay\nimport kotlinx.coroutines.withContext\nimport studio.guitarlab.core.audio.LatencyCalibration\nimport studio.guitarlab.core.audio.LatencyCalibrationPolicy\n\nclass StudioLatencyCalibrationStore(context: Context) {\n    private val prefs = context.applicationContext.getSharedPreferences(\"studio-latency-calibration\", Context.MODE_PRIVATE)\n\n    fun save(inputSignature: String?, outputSignature: String?, calibration: LatencyCalibration) {\n        val key = key(inputSignature, outputSignature, calibration.sampleRateHz)\n        prefs.edit()\n            .putLong(\"$key.frames\", calibration.latencyFrames)\n            .putLong(\"$key.jitter\", calibration.jitterFrames)\n            .putString(\"$key.drift\", calibration.driftPpm.toString())\n            .putFloat(\"$key.confidence\", calibration.confidence)\n            .putInt(\"$key.attempts\", calibration.attempts)\n            .putBoolean(\"$key.accepted\", calibration.accepted)\n            .putLong(\"$key.measured\", calibration.measuredAtEpochMs)\n            .apply()\n    }\n\n    fun find(inputSignature: String?, outputSignature: String?, sampleRateHz: Int): LatencyCalibration? {\n        val key = key(inputSignature, outputSignature, sampleRateHz)\n        if (!prefs.contains(\"$key.frames\")) return null\n        return LatencyCalibration(\n            latencyFrames = prefs.getLong(\"$key.frames\", 0L),\n            jitterFrames = prefs.getLong(\"$key.jitter\", 0L),\n            driftPpm = prefs.getString(\"$key.drift\", \"0\")?.toDoubleOrNull() ?: 0.0,\n            confidence = prefs.getFloat(\"$key.confidence\", 0f),\n            sampleRateHz = sampleRateHz,\n            attempts = prefs.getInt(\"$key.attempts\", 0),\n            accepted = prefs.getBoolean(\"$key.accepted\", false),\n            measuredAtEpochMs = prefs.getLong(\"$key.measured\", 0L),\n        )\n    }\n\n    fun clear(inputSignature: String?, outputSignature: String?, sampleRateHz: Int) {\n        val prefix = key(inputSignature, outputSignature, sampleRateHz)\n        prefs.edit().also { editor ->\n            prefs.all.keys.filter { it.startsWith(prefix) }.forEach(editor::remove)\n        }.apply()\n    }\n\n    private fun key(input: String?, output: String?, rate: Int): String =\n        \"${input ?: \"auto-in\"}|${output ?: \"auto-out\"}|$rate\".hashCode().toUInt().toString(16)\n}\n\nclass AndroidLatencyCalibrationEngine(private val context: Context) {\n    suspend fun calibrate(\n        sampleRateHz: Int,\n        inputDevice: AudioDeviceInfo?,\n        outputDevice: AudioDeviceInfo?,\n        attempts: Int = 5,\n        onProgress: (Int, Int) -> Unit = { _, _ -> },\n    ): LatencyCalibration = withContext(Dispatchers.IO) {\n        require(ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {\n            \"Permissão de microfone necessária para medir a latência.\"\n        }\n        require(sampleRateHz in 8_000..192_000)\n        require(attempts in 3..9)\n        val measurements = mutableListOf<Long>()\n        val confidences = mutableListOf<Float>()\n        val startedMs = System.currentTimeMillis()\n        repeat(attempts) { index ->\n            onProgress(index + 1, attempts)\n            val pass = measurePass(sampleRateHz, inputDevice, outputDevice)\n            measurements += pass.first\n            confidences += pass.second\n            delay(80L)\n        }\n        LatencyCalibrationPolicy.evaluate(\n            measurementsFrames = measurements,\n            confidences = confidences,\n            sampleRateHz = sampleRateHz,\n            elapsedMs = (System.currentTimeMillis() - startedMs).coerceAtLeast(1L),\n            measuredAtEpochMs = System.currentTimeMillis(),\n        )\n    }\n\n    private fun measurePass(sampleRateHz: Int, inputDevice: AudioDeviceInfo?, outputDevice: AudioDeviceInfo?): Pair<Long, Float> {\n        val template = stimulus(STIMULUS_FRAMES)\n        val preRollFrames = sampleRateHz / 10\n        val captureFrames = sampleRateHz\n        val recorderBufferBytes = max(\n            AudioRecord.getMinBufferSize(sampleRateHz, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_FLOAT),\n            captureFrames * Float.SIZE_BYTES / 2,\n        )\n        require(recorderBufferBytes > 0) { \"A entrada não suporta calibração em $sampleRateHz Hz.\" }\n        val recorder = AudioRecord.Builder()\n            .setAudioSource(MediaRecorder.AudioSource.UNPROCESSED)\n            .setAudioFormat(AudioFormat.Builder().setEncoding(AudioFormat.ENCODING_PCM_FLOAT).setSampleRate(sampleRateHz).setChannelMask(AudioFormat.CHANNEL_IN_MONO).build())\n            .setBufferSizeInBytes(recorderBufferBytes)\n            .build()\n        val outMin = AudioTrack.getMinBufferSize(sampleRateHz, AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_FLOAT)\n        require(outMin > 0) { \"A saída não suporta calibração em $sampleRateHz Hz.\" }\n        val track = AudioTrack.Builder()\n            .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build())\n            .setAudioFormat(AudioFormat.Builder().setEncoding(AudioFormat.ENCODING_PCM_FLOAT).setSampleRate(sampleRateHz).setChannelMask(AudioFormat.CHANNEL_OUT_STEREO).build())\n            .setTransferMode(AudioTrack.MODE_STREAM)\n            .setBufferSizeInBytes(max(outMin, (preRollFrames + STIMULUS_FRAMES + sampleRateHz / 3) * 2 * Float.SIZE_BYTES))\n            .setPerformanceMode(AudioTrack.PERFORMANCE_MODE_LOW_LATENCY)\n            .build()\n        try {\n            require(recorder.state == AudioRecord.STATE_INITIALIZED && track.state == AudioTrack.STATE_INITIALIZED) { \"Não foi possível inicializar as rotas para calibração.\" }\n            if (inputDevice != null) require(recorder.setPreferredDevice(inputDevice)) { \"A entrada selecionada recusou a calibração.\" }\n            if (outputDevice != null) require(track.setPreferredDevice(outputDevice)) { \"A saída selecionada recusou a calibração.\" }\n            val captured = FloatArray(captureFrames)\n            val outputMono = FloatArray(preRollFrames + STIMULUS_FRAMES + sampleRateHz / 3)\n            template.copyInto(outputMono, destinationOffset = preRollFrames)\n            val outputStereo = FloatArray(outputMono.size * 2)\n            outputMono.forEachIndexed { i, sample -> outputStereo[i * 2] = sample; outputStereo[i * 2 + 1] = sample }\n\n            recorder.startRecording()\n            val writer = Thread {\n                track.play()\n                var offset = 0\n                while (offset < outputStereo.size) {\n                    val written = track.write(outputStereo, offset, outputStereo.size - offset, AudioTrack.WRITE_BLOCKING)\n                    if (written <= 0) break\n                    offset += written\n                }\n            }\n            writer.start()\n            var read = 0\n            while (read < captured.size) {\n                val amount = recorder.read(captured, read, captured.size - read, AudioRecord.READ_BLOCKING)\n                if (amount <= 0) break\n                read += amount\n            }\n            writer.join(1500L)\n            val inputTimestamp = AudioTimestamp()\n            val outputTimestamp = AudioTimestamp()\n            val hasInputTs = recorder.getTimestamp(inputTimestamp, AudioTimestamp.TIMEBASE_MONOTONIC) == AudioRecord.SUCCESS\n            val hasOutputTs = track.getTimestamp(outputTimestamp)\n            require(hasInputTs && hasOutputTs) { \"O Android não forneceu timestamps de áudio estáveis para esta rota.\" }\n            val match = correlate(captured, read, template)\n            require(match.second >= .30f) { \"Sinal de loopback não detectado. Conecte/ative o retorno da saída para a entrada e repita.\" }\n            val inputFrame = match.first.toLong()\n            val outputFrame = preRollFrames.toLong()\n            val inputNs = inputTimestamp.nanoTime + ((inputFrame - inputTimestamp.framePosition) * 1_000_000_000.0 / sampleRateHz).roundToLong()\n            val outputNs = outputTimestamp.nanoTime + ((outputFrame - outputTimestamp.framePosition) * 1_000_000_000.0 / sampleRateHz).roundToLong()\n            val latencyFrames = (((inputNs - outputNs).coerceAtLeast(0L)) * sampleRateHz / 1_000_000_000.0).roundToLong()\n            require(latencyFrames in 0..sampleRateHz.toLong()) { \"A medição de latência ficou fora da faixa confiável.\" }\n            return latencyFrames to match.second\n        } finally {\n            runCatching { recorder.stop() }; runCatching { track.pause() }; runCatching { track.flush() }\n            recorder.release(); track.release()\n        }\n    }\n\n    private fun stimulus(size: Int): FloatArray {\n        var state = 0x13579BDF\n        return FloatArray(size) {\n            state = state xor (state shl 13); state = state xor (state ushr 17); state = state xor (state shl 5)\n            if ((state and 1) == 0) 0.62f else -0.62f\n        }\n    }\n\n    private fun correlate(captured: FloatArray, capturedSize: Int, template: FloatArray): Pair<Int, Float> {\n        if (capturedSize <= template.size) return 0 to 0f\n        var templateEnergy = 0.0\n        template.forEach { templateEnergy += it * it }\n        var bestIndex = 0\n        var bestScore = 0.0\n        val step = 2\n        var offset = 0\n        while (offset + template.size <= capturedSize) {\n            var dot = 0.0\n            var energy = 0.0\n            var i = 0\n            while (i < template.size) {\n                val sample = captured[offset + i]\n                dot += sample * template[i]\n                energy += sample * sample\n                i += step\n            }\n            val score = if (energy > 1e-9) abs(dot) / sqrt(energy * templateEnergy / step) else 0.0\n            if (score > bestScore) { bestScore = score; bestIndex = offset }\n            offset += step\n        }\n        return bestIndex to bestScore.coerceIn(0.0, 1.0).toFloat()\n    }\n\n    companion object { private const val STIMULUS_FRAMES = 1024 }\n}\n\nfun LatencyCalibration.describe(): String {\n    val latencyMs = latencyFrames * 1000.0 / sampleRateHz\n    val jitterMs = jitterFrames * 1000.0 / sampleRateHz\n    return String.format(Locale.US, \"%.1f ms · jitter %.1f ms · confiança %.0f%%\", latencyMs, jitterMs, confidence * 100f)\n}\n'''
)

# 8) Settings calibration UI.
replace_once(
    "app/src/main/java/studio/guitarlab/app/ui/SettingsScreen.kt",
    """package studio.guitarlab.app.ui\n\n""",
    """package studio.guitarlab.app.ui\n\nimport android.Manifest\nimport android.content.pm.PackageManager\nimport androidx.activity.compose.rememberLauncherForActivityResult\nimport androidx.activity.result.contract.ActivityResultContracts\n""",
)
replace_once(
    "app/src/main/java/studio/guitarlab/app/ui/SettingsScreen.kt",
    """import androidx.compose.runtime.remember""",
    """import androidx.compose.runtime.remember\nimport androidx.compose.runtime.rememberCoroutineScope""",
)
replace_once(
    "app/src/main/java/studio/guitarlab/app/ui/SettingsScreen.kt",
    """import androidx.compose.ui.unit.dp""",
    """import androidx.compose.ui.unit.dp\nimport kotlinx.coroutines.launch""",
)
replace_once(
    "app/src/main/java/studio/guitarlab/app/ui/SettingsScreen.kt",
    """    var monitoringMode by remember { mutableStateOf(routingStore.monitoringMode()) }""",
    """    var monitoringMode by remember { mutableStateOf(routingStore.monitoringMode()) }\n    val latencyStore = remember(context) { StudioLatencyCalibrationStore(context) }\n    val latencyEngine = remember(context) { AndroidLatencyCalibrationEngine(context) }\n    val scope = rememberCoroutineScope()\n    var calibrating by remember { mutableStateOf(false) }\n    var calibrationStatus by remember { mutableStateOf<String?>(null) }\n    var calibrationProgress by remember { mutableStateOf(0 to 0) }\n    var pendingCalibration by remember { mutableStateOf(false) }\n\n    fun runLatencyCalibration() {\n        if (calibrating) return\n        calibrating = true\n        calibrationStatus = \"Preparando medição de loopback…\"\n        scope.launch {\n            runCatching {\n                latencyEngine.calibrate(\n                    sampleRateHz = 48_000,\n                    inputDevice = routingStore.resolveSelectedInputDevice(),\n                    outputDevice = routingStore.resolveSelectedOutputDevice(),\n                    onProgress = { current, total -> calibrationProgress = current to total },\n                )\n            }.onSuccess { result ->\n                latencyStore.save(selectedInput, selectedOutput, result)\n                calibrationStatus = if (result.accepted) \"Calibração válida · ${result.describe()}\" else \"Medição instável · ${result.describe()} · repita antes de usar compensação\"\n            }.onFailure { error ->\n                calibrationStatus = error.message ?: \"Não foi possível medir a latência.\"\n            }\n            calibrating = false\n        }\n    }\n\n    val latencyPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->\n        if (granted && pendingCalibration) runLatencyCalibration()\n        if (!granted) calibrationStatus = \"Permissão de microfone necessária para calibrar.\"\n        pendingCalibration = false\n    }""",
)
replace_once(
    "app/src/main/java/studio/guitarlab/app/ui/SettingsScreen.kt",
    """                    OptionRow(\"Taxa de amostragem\", \"Automática\", \"Durante a gravação o Studio respeita a taxa já estabelecida pelo projeto\")\n                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {""",
    """                    OptionRow(\"Taxa de amostragem\", \"Automática\", \"Durante a gravação o Studio respeita a taxa já estabelecida pelo projeto\")\n                    OptionRow(\n                        \"Compensação de latência\",\n                        latencyStore.find(selectedInput, selectedOutput, 48_000)?.takeIf { it.accepted }?.describe() ?: \"Não calibrada\",\n                        \"M6 usa medição round-trip por loopback, vinculada à combinação entrada/saída. Uma calibração instável nunca é aplicada automaticamente.\",\n                    )\n                    Text(\n                        \"Para calibrar, conecte ou ative um retorno físico da saída para a entrada. O GuitarLab executa várias medições, estima jitter/drift e só aceita resultados estáveis.\",\n                        style = MaterialTheme.typography.bodySmall,\n                        color = MaterialTheme.colorScheme.onSurfaceVariant,\n                    )\n                    calibrationStatus?.let { Text(it, style = MaterialTheme.typography.bodySmall) }\n                    if (calibrating) {\n                        Text(\"Medição ${calibrationProgress.first}/${calibrationProgress.second.coerceAtLeast(1)}…\", style = MaterialTheme.typography.bodySmall)\n                    }\n                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {\n                        OutlinedButton(\n                            enabled = !calibrating,\n                            onClick = {\n                                if (context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {\n                                    runLatencyCalibration()\n                                } else {\n                                    pendingCalibration = true\n                                    latencyPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)\n                                }\n                            },\n                        ) { Text(if (calibrating) \"Medindo…\" else \"Calibrar latência\") }""",
)

# 9) Version + CI identity + docs.
replace_once("app/build.gradle.kts", 'versionCode = 15\n        versionName = "0.2.0-alpha14"', 'versionCode = 16\n        versionName = "0.3.0-alpha1"')
replace_once(".github/workflows/android-ci.yml", 'milestone=M5-final-physical-homologation-candidate', 'milestone=M6-latency-synchronization-homologation-candidate')
replace_once(".github/workflows/android-ci.yml", 'gate=alpha14-awaiting-physical-validation', 'gate=m6-alpha1-awaiting-physical-validation')

write("docs/CURRENT_STATE.md", '''# Current State — GuitarLab Studio\n\nUpdated: 2026-09-09\n\n## Active branch and gate\n- Repository: `anfalcir/guitarlab`\n- Branch: `dev/parallel-m3-m5` (historical branch name retained to avoid destabilizing the active PR)\n- Draft PR: #1\n- Candidate under construction: `0.3.0-alpha1`, versionCode 16\n- M2: PASS/CLOSED on Samsung SM-X230 Android 16/API 36 + Pocket Amp USB\n- M3/M4: absorbed\n- M5: **PASS/CLOSED by user physical approval of alpha14**\n- M6: implementation complete in software; **OPEN pending M6 physical homologation**\n\n## M6 scope implemented\n1. frame-derived exact time labels on playhead and loop markers;\n2. top-bar transport summary with `Restante` and `Total`;\n3. route-scoped round-trip latency calibration harness using simultaneous AudioTrack/AudioRecord loopback and Android monotonic audio timestamps;\n4. repeated measurements, median latency, confidence, jitter and drift characterization;\n5. only stable/accepted calibrations are eligible for automatic take-placement compensation;\n6. compensation is applied in the frame domain at take finalization, including source trimming when an earlier shift would cross timeline zero;\n7. calibration persistence is keyed to input route + output route + sample rate;\n8. Mute/Solo are live mixer controls during PLAY and REC; structural edits and REC-arm changes remain guarded;\n9. project rename is persistent and immediately reflected by Studio/Home/export naming.\n\n## M6 physical gate\nThe software must not invent a latency value. Physical homologation must run `Calibrar latência` with a real loopback/return path on the target route, verify the reported stability, then record a known transient against backing and confirm compensated placement. Repeatability, route changes, mute/solo during PLAY/REC, marker time labels and project rename are part of the M6 checklist.\n\n## Safety rule\nAn absent or rejected calibration produces **zero automatic compensation**. A stored calibration is route/sample-rate specific and is not reused for a different route.\n\nCanonical roadmap: `docs/IMPLEMENTATION_ROADMAP.md`.\nActive physical checklist: `docs/M6_ALPHA1_HOMOLOGATION_CHECKLIST.md`.\n''')
write("docs/IMPLEMENTATION_ROADMAP.md", '''# Implementation Roadmap\n\nUpdated: 2026-09-09\n\n## M1 — Project/model foundation — CLOSED\nProject model, templates, persistence baseline and repository structure.\n\n## M2 — Android hardware/audio baseline — PASS/CLOSED\nSamsung SM-X230 Android 16/API36 + Pocket Amp USB physical gate established.\n\n## M3 — Codec/import foundation — ABSORBED\nCodec/import work was consolidated into M5.\n\n## M4 — Studio playback/edit/mix foundation — ABSORBED\nTimeline, playback, editing, Mixer/Master and project interaction foundations are integrated.\n\n## M5 — Reliable recording + Studio consolidation + Media I/O — PASS/CLOSED\nClosed after user physical approval of `0.2.0-alpha14`. Recording, managed takes, media import, stereo L/R handling, portable project persistence, master export, drag/edit UX and M5 corrective gates are no longer pending.\n\n## M6 — Measured latency and synchronization — ACTIVE PHYSICAL GATE\n### Implemented\n- reproducible loopback round-trip measurement harness;\n- simultaneous AudioTrack/AudioRecord measurement with monotonic audio timestamp correlation;\n- repeated-pass median latency, confidence, jitter and drift characterization;\n- route + sample-rate scoped persistence of accepted calibration;\n- measured frame-domain take-placement compensation with timeline-zero handling;\n- safe zero-compensation fallback when calibration is missing/rejected;\n- exact marker time labels and top-bar remaining/total transport indication;\n- live Mute/Solo during PLAY and REC;\n- persistent project renaming.\n\n### Remaining M6 work\nOnly closure gates remain:\n1. exact-candidate unit/Lint/debug/signing gates;\n2. physical loopback calibration on Samsung SM-X230 + Pocket Amp route;\n3. repeat calibration and verify jitter/drift acceptance behavior;\n4. record known transient against backing and verify compensated take placement;\n5. verify route change does not reuse an unrelated calibration;\n6. regress PLAY/REC, loop, marker clocks, live Mute/Solo, rename, import/export and M5 core behavior;\n7. corrective alpha only for repeatable P0/P1;\n8. explicit user declaration M6 PASS/CLOSED.\n\n## M7 — Production audio polish\nBegins only after M6 closes:\n- validated sample-rate conversion for mismatched source/project rates;\n- fades/crossfades and advanced clip polish;\n- export refinements when explicitly prioritized;\n- performance/memory work for larger sessions.\n\n## M8 — Release hardening\n- migration/compatibility matrix;\n- accessibility/device-size polish;\n- crash/edge-case hardening;\n- packaging, release notes and distribution gate.\n\n## Gate discipline\nEach milestone advances only after software and required physical gates pass. `CURRENT_STATE.md` and this roadmap are canonical.\n''')
write("docs/M6_ALPHA1_HOMOLOGATION_CHECKLIST.md", '''# M6 alpha1 — Physical Homologation Checklist\n\nTarget: Samsung SM-X230 Android 16/API36 + Pocket Amp USB route.\nCandidate: `0.3.0-alpha1` / versionCode 16.\n\n## A. Timeline/time UX\n- Playhead marker displays exact `mm:ss` (or `h:mm:ss`) and updates while dragging/playing.\n- Loop start/end markers display their own exact times.\n- Top bar shows `Restante` and `Total` consistently with the same project frame clock.\n\n## B. Live mixer\n- Toggle Mute while PLAY: audible state changes without stopping/restarting transport.\n- Toggle Solo while PLAY: solo arbitration updates immediately.\n- Toggle Mute/Solo while REC with backing: backing mix changes while capture continues.\n- REC Arm remains protected from structural changes during active capture.\n\n## C. Project rename\n- Rename from Studio title control.\n- Name persists after Home/reopen/app restart.\n- Home listing and suggested `.guitarlab`/master filenames use the new name.\n\n## D. M6 latency calibration\n- Select intended input/output route.\n- Connect/enable a real output-to-input loopback/return path.\n- Run `Opções > Áudio > Calibrar latência`.\n- Calibration runs multiple passes and reports latency, jitter and confidence.\n- Repeat at least twice; accepted results must be reasonably stable.\n- Break/remove loopback and verify calibration fails/rejects instead of inventing a value.\n- Change route and verify an unrelated stored calibration is not shown/applied.\n\n## E. Take-placement compensation\n- With an accepted calibration, record a sharp known transient against a backing/reference transient.\n- Inspect waveforms: recorded transient should align materially closer after automatic compensation.\n- Repeat take to assess jitter/repeatability.\n- Start recording near timeline zero and confirm no negative clip position/corruption.\n- Clear/use an uncalibrated route and confirm recording still works with zero automatic compensation.\n\n## F. Regression\n- Import stereo/multi-format representative files.\n- Drag track/clip, Trim, split temporal, stereo L/R separation.\n- Loop/Undo/Redo.\n- Save/open `.guitarlab`.\n- Export WAV32f/FLAC/MP3.\n\nM6 closes only with zero repeatable P0/P1 and explicit user approval.\n''')

# Run exact software gates before committing.
subprocess.run(["git", "diff", "--check"], cwd=ROOT, check=True)
subprocess.run(["gradle", "--no-daemon", "--console=plain", "test"], cwd=ROOT, check=True)
subprocess.run(["gradle", "--no-daemon", "--console=plain", "lint"], cwd=ROOT, check=True)
subprocess.run(["gradle", "--no-daemon", "--console=plain", "assembleDebug"], cwd=ROOT, check=True)

# Remove temporary integrator infrastructure from the promoted commit.
for relative in ["tools/m6_integrate.py", ".github/workflows/m6-integrator.yml", ".github/m6-trigger.txt"]:
    p = ROOT / relative
    if p.exists(): p.unlink()

subprocess.run(["git", "add", "-A"], cwd=ROOT, check=True)
subprocess.run(["git", "config", "user.name", "GuitarLab CI Integrator"], cwd=ROOT, check=True)
subprocess.run(["git", "config", "user.email", "actions@users.noreply.github.com"], cwd=ROOT, check=True)
subprocess.run(["git", "commit", "-m", "M6: latency synchronization live mix clocks and rename [sign-homologation]"], cwd=ROOT, check=True)
subprocess.run(["git", "push", "origin", "HEAD:dev/parallel-m3-m5"], cwd=ROOT, check=True)
