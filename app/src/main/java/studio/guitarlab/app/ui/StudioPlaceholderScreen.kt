package studio.guitarlab.app.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import studio.guitarlab.app.ui.theme.StudioTrim
import studio.guitarlab.core.model.AudioClip
import studio.guitarlab.core.model.AudioTrack
import studio.guitarlab.core.model.BuiltInRoles
import studio.guitarlab.core.model.GuitarProject
import studio.guitarlab.core.project.TimelineControlPolicy
import studio.guitarlab.core.project.TimelineControlState
import studio.guitarlab.core.project.TransportPolicy
import studio.guitarlab.core.project.TransportState
import studio.guitarlab.core.project.TrimControlPolicy
import studio.guitarlab.core.project.TrimControlState

@Composable
fun StudioPlaceholderScreen(
    projectId: String,
    onBack: () -> Unit,
    viewModel: StudioViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsState()
    var pendingTrackId by remember { mutableStateOf<String?>(null) }
    val wavPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        val trackId = pendingTrackId
        pendingTrackId = null
        if (uri != null && trackId != null) viewModel.importWav(trackId, uri)
    }

    LaunchedEffect(projectId) { viewModel.load(projectId) }

    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        StudioHeader(project = state.project, fallbackId = projectId, onBack = onBack)

        Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
            when {
                state.loading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                state.error != null && state.project == null -> CompactStatus(
                    text = state.error.orEmpty(),
                    error = true,
                    modifier = Modifier.align(Alignment.TopStart),
                )
                state.project != null -> ProjectWorkspace(
                    project = state.project!!,
                    waveforms = state.waveforms,
                    timelineControls = state.timelineControls,
                    trimControls = state.trimControls,
                    transport = state.transport,
                    transportEngineReady = state.transportEngineReady,
                    importing = state.importing,
                    editingClip = state.editingClip,
                    importStatus = state.importStatus,
                    error = state.error,
                    onImportWav = { trackId ->
                        if (!state.importing && !state.editingClip && state.trimControls == null && TransportPolicy.timelineEditingEnabled(state.transport)) {
                            pendingTrackId = trackId
                            wavPicker.launch(arrayOf("audio/wav", "audio/x-wav", "audio/wave", "application/octet-stream"))
                        }
                    },
                    onReturnToStart = viewModel::returnToStart,
                    onPlayStop = viewModel::togglePlayStop,
                    onRecord = viewModel::startRecording,
                    onToggleLoop = viewModel::toggleLoop,
                    onPlayheadFrameChanged = viewModel::setPlayheadFrame,
                    onLoopStartFrameChanged = viewModel::setLoopStartFrame,
                    onLoopEndFrameChanged = viewModel::setLoopEndFrame,
                    onBeginTrim = viewModel::beginTrim,
                    onTrimStartFrameChanged = viewModel::setTrimStartFrame,
                    onTrimEndFrameChanged = viewModel::setTrimEndFrame,
                    onApplyTrim = viewModel::applyTrim,
                    onCancelTrim = viewModel::cancelTrim,
                    onToggleClipMuted = viewModel::toggleClipMuted,
                    onRemoveClip = viewModel::removeClip,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}

@Composable
private fun StudioHeader(project: GuitarProject?, fallbackId: String, onBack: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(project?.name ?: "Project ${fallbackId.take(8)}…", style = MaterialTheme.typography.titleLarge)
            project?.let {
                val sampleRate = it.sampleRate.fixedHz?.let { hz -> "$hz Hz" } ?: "Auto rate"
                Text(
                    "${it.tracks.size} tracks • ${it.clips.size} clips • $sampleRate",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        OutlinedButton(onClick = onBack) { Text("Home") }
    }
}

@Composable
private fun ProjectWorkspace(
    project: GuitarProject,
    waveforms: Map<String, List<Float>>,
    timelineControls: TimelineControlState,
    trimControls: TrimControlState?,
    transport: TransportState,
    transportEngineReady: Boolean,
    importing: Boolean,
    editingClip: Boolean,
    importStatus: String?,
    error: String?,
    onImportWav: (String) -> Unit,
    onReturnToStart: () -> Unit,
    onPlayStop: () -> Unit,
    onRecord: () -> Unit,
    onToggleLoop: () -> Unit,
    onPlayheadFrameChanged: (Long) -> Unit,
    onLoopStartFrameChanged: (Long) -> Unit,
    onLoopEndFrameChanged: (Long) -> Unit,
    onBeginTrim: (String) -> Unit,
    onTrimStartFrameChanged: (Long) -> Unit,
    onTrimEndFrameChanged: (Long) -> Unit,
    onApplyTrim: () -> Unit,
    onCancelTrim: () -> Unit,
    onToggleClipMuted: (String) -> Unit,
    onRemoveClip: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val baseProjectEndFrame = TimelineControlPolicy.projectEndFrame(project)
    val trimClip = trimControls?.let { state -> project.clips.firstOrNull { it.id == state.clipId } }
    val projectEndFrame = maxOf(baseProjectEndFrame, trimClip?.let(TrimControlPolicy::maximumEndFrame) ?: 0L).coerceAtLeast(1L)
    val timelineEditingEnabled = TransportPolicy.timelineEditingEnabled(transport)
    val trimActive = trimControls != null
    val clipEditingEnabled = !importing && !editingClip && timelineEditingEnabled && !trimActive

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (error != null) {
            CompactStatus(text = error, error = true)
        } else if (importing) {
            CompactStatus(text = "Importing audio into project-managed storage…")
        } else if (!importStatus.isNullOrBlank() && project.clips.isEmpty()) {
            CompactStatus(text = importStatus)
        }

        Surface(
            modifier = Modifier.fillMaxWidth().weight(1f),
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.22f),
            tonalElevation = 0.dp,
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                TimelineMarkerRail(
                    projectEndFrame = projectEndFrame,
                    playheadFrame = timelineControls.playheadFrame,
                    loopStartFrame = timelineControls.loopStartFrame,
                    loopEndFrame = timelineControls.loopEndFrame,
                    showLoopMarkers = transport.loopEnabled,
                    enabled = timelineEditingEnabled,
                    onPlayheadFrameChanged = onPlayheadFrameChanged,
                    onLoopStartFrameChanged = onLoopStartFrameChanged,
                    onLoopEndFrameChanged = onLoopEndFrameChanged,
                    trimStartFrame = trimControls?.startFrame,
                    trimEndFrame = trimControls?.endFrame,
                    onTrimStartFrameChanged = if (trimActive) onTrimStartFrameChanged else null,
                    onTrimEndFrameChanged = if (trimActive) onTrimEndFrameChanged else null,
                    modifier = Modifier.fillMaxWidth(),
                )
                TimelineRuler(project, projectEndFrame)
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    contentPadding = PaddingValues(bottom = 2.dp),
                ) {
                    items(project.tracks.sortedBy { it.order }, key = { it.id }) { track ->
                        StudioTrackLane(
                            track = track,
                            clips = project.clips.filter { it.trackId == track.id },
                            waveforms = waveforms,
                            projectEndFrame = projectEndFrame,
                            canImport = clipEditingEnabled,
                            canEditClip = clipEditingEnabled,
                            activeTrimClipId = trimControls?.clipId,
                            onImportWav = { onImportWav(track.id) },
                            onBeginTrim = onBeginTrim,
                            onToggleMuted = onToggleClipMuted,
                            onRemove = onRemoveClip,
                        )
                    }
                }
            }
        }

        if (trimControls != null && trimClip != null) {
            TrimActionBar(
                clipName = trimClip.name,
                startFrame = trimControls.startFrame,
                endFrame = trimControls.endFrame,
                enabled = !editingClip && timelineEditingEnabled,
                onApply = onApplyTrim,
                onCancel = onCancelTrim,
            )
        }

        TransportBar(
            state = transport,
            engineReady = transportEngineReady && !trimActive,
            onReturnToStart = onReturnToStart,
            onPlayStop = onPlayStop,
            onRecord = onRecord,
            onToggleLoop = onToggleLoop,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun TimelineRuler(project: GuitarProject, projectEndFrame: Long) {
    val sampleRate = project.sampleRate.fixedHz
        ?: project.clips.firstOrNull { it.sourceSampleRateHz != null }?.sourceSampleRateHz
        ?: 48_000
    Row(modifier = Modifier.fillMaxWidth().padding(start = 156.dp, end = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        for (step in 0..4) {
            val frame = projectEndFrame * step / 4
            Text(
                formatTimelineTime(frame, sampleRate),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun StudioTrackLane(
    track: AudioTrack,
    clips: List<AudioClip>,
    waveforms: Map<String, List<Float>>,
    projectEndFrame: Long,
    canImport: Boolean,
    canEditClip: Boolean,
    activeTrimClipId: String?,
    onImportWav: () -> Unit,
    onBeginTrim: (String) -> Unit,
    onToggleMuted: (String) -> Unit,
    onRemove: (String) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().height(68.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            modifier = Modifier.width(150.dp).fillMaxHeight(),
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
        ) {
            Row(
                modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                    Text(track.name, style = MaterialTheme.typography.bodyMedium, maxLines = 1)
                    Text(
                        roleName(track.roleId),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                    )
                }
                TextButton(
                    onClick = onImportWav,
                    enabled = canImport,
                    contentPadding = PaddingValues(horizontal = 5.dp, vertical = 0.dp),
                ) { Text("+") }
            }
        }

        BoxWithConstraints(
            modifier = Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.surface.copy(alpha = 0.72f)),
        ) {
            if (clips.isEmpty()) {
                Text(
                    "No audio",
                    modifier = Modifier.align(Alignment.CenterStart).padding(start = 10.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                clips.sortedBy { it.startFrame }.forEach { clip ->
                    val startFraction = (clip.startFrame.toDouble() / projectEndFrame).coerceIn(0.0, 1.0)
                    val endFrame = (clip.startFrame + clip.lengthFrames).coerceAtMost(projectEndFrame)
                    val endFraction = (endFrame.toDouble() / projectEndFrame).coerceIn(startFraction, 1.0)
                    val x = maxWidth * startFraction.toFloat()
                    val naturalWidth = maxWidth * (endFraction - startFraction).toFloat()
                    val availableWidth = (maxWidth - x).coerceAtLeast(1.dp)
                    val clipWidth = naturalWidth.coerceAtLeast(92.dp).coerceAtMost(availableWidth)
                    TimelineClipCard(
                        clip = clip,
                        peaks = waveforms[clip.id],
                        trimming = activeTrimClipId == clip.id,
                        canEdit = canEditClip,
                        onTrim = { onBeginTrim(clip.id) },
                        onMute = { onToggleMuted(clip.id) },
                        onRemove = { onRemove(clip.id) },
                        modifier = Modifier.offset(x = x).padding(vertical = 4.dp).width(clipWidth).height(60.dp).align(Alignment.CenterStart),
                    )
                }
            }
        }
    }
}

@Composable
private fun TimelineClipCard(
    clip: AudioClip,
    peaks: List<Float>?,
    trimming: Boolean,
    canEdit: Boolean,
    onTrim: () -> Unit,
    onMute: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(7.dp),
        color = if (trimming) StudioTrim.copy(alpha = 0.22f) else MaterialTheme.colorScheme.primaryContainer.copy(alpha = if (clip.muted) 0.40f else 0.82f),
        tonalElevation = 0.dp,
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 7.dp, vertical = 3.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    clip.name,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 1,
                    color = if (clip.muted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onPrimaryContainer,
                )
                TextButton(onClick = onTrim, enabled = canEdit, contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp), modifier = Modifier.height(28.dp)) { Text("Trim", style = MaterialTheme.typography.labelSmall) }
                TextButton(onClick = onMute, enabled = canEdit, contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp), modifier = Modifier.height(28.dp)) { Text(if (clip.muted) "U" else "M", style = MaterialTheme.typography.labelSmall) }
                TextButton(onClick = onRemove, enabled = canEdit, contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp), modifier = Modifier.height(28.dp)) { Text("×", style = MaterialTheme.typography.labelMedium) }
            }
            peaks?.let { WaveformMini(peaks = it, muted = clip.muted, modifier = Modifier.fillMaxWidth().height(20.dp)) }
        }
    }
}

@Composable
private fun TrimActionBar(
    clipName: String,
    startFrame: Long,
    endFrame: Long,
    enabled: Boolean,
    onApply: () -> Unit,
    onCancel: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        color = StudioTrim.copy(alpha = 0.13f),
        tonalElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Trim • $clipName", style = MaterialTheme.typography.labelLarge)
                Text(
                    "$startFrame – $endFrame frames • source remains unchanged",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            OutlinedButton(onClick = onCancel, enabled = enabled) { Text("Cancel") }
            Button(
                onClick = onApply,
                enabled = enabled,
                colors = ButtonDefaults.buttonColors(containerColor = StudioTrim, contentColor = Color.White),
            ) { Text("✓ Apply trim") }
        }
    }
}

@Composable
private fun CompactStatus(text: String, error: Boolean = false, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = if (error) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
    ) {
        Text(
            text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            style = MaterialTheme.typography.bodySmall,
            color = if (error) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
        )
    }
}

private fun roleName(roleId: String?): String {
    if (roleId == null) return "No role"
    return BuiltInRoles.definitions.firstOrNull { it.id == roleId }?.name ?: roleId
}

private fun formatTimelineTime(frame: Long, sampleRate: Int): String {
    val totalSeconds = if (sampleRate > 0) frame / sampleRate else 0L
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}
