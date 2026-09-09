package studio.guitarlab.app.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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

private val TrackSidebarWidth = 224.dp
private val TrackLaneGap = 6.dp
private val TrackLaneHeight = 96.dp

@Composable
fun StudioPlaceholderScreen(
    viewModel: StudioViewModel = viewModel(),
    selectedTrackId: String? = null,
    onSelectTrack: (String) -> Unit = {},
) {
    val state by viewModel.state.collectAsState()
    var pendingTrackId by remember { mutableStateOf<String?>(null) }
    var settingsTrackId by remember { mutableStateOf<String?>(null) }
    val wavPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        val trackId = pendingTrackId
        pendingTrackId = null
        if (uri != null && trackId != null) viewModel.importWav(trackId, uri)
    }

    Box(modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp, vertical = 8.dp)) {
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
                importing = state.importing,
                editingClip = state.editingClip,
                error = state.error,
                selectedTrackId = selectedTrackId,
                onSelectTrack = onSelectTrack,
                onOpenTrackSettings = { settingsTrackId = it },
                onAddTrack = viewModel::addTrack,
                onImportWav = { trackId ->
                    if (!state.importing && !state.editingClip && state.trimControls == null && TransportPolicy.timelineEditingEnabled(state.transport)) {
                        pendingTrackId = trackId
                        wavPicker.launch(arrayOf("audio/wav", "audio/x-wav", "audio/wave", "application/octet-stream"))
                    }
                },
                onPlayheadFrameChanged = viewModel::setPlayheadFrame,
                onLoopStartFrameChanged = viewModel::setLoopStartFrame,
                onLoopEndFrameChanged = viewModel::setLoopEndFrame,
                onBeginTrim = viewModel::beginTrim,
                onTrimStartFrameChanged = viewModel::setTrimStartFrame,
                onTrimEndFrameChanged = viewModel::setTrimEndFrame,
                onApplyTrim = viewModel::applyTrim,
                onCancelTrim = viewModel::cancelTrim,
                onRemoveClip = viewModel::removeClip,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }

    val project = state.project
    val settingsTrack = settingsTrackId?.let { id -> project?.tracks?.firstOrNull { it.id == id } }
    if (settingsTrack != null && project != null) {
        val sorted = project.tracks.sortedBy { it.order }
        val index = sorted.indexOfFirst { it.id == settingsTrack.id }
        TrackSettingsDialog(
            track = settingsTrack,
            hasClips = project.clips.any { it.trackId == settingsTrack.id },
            canMoveUp = index > 0,
            canMoveDown = index in 0 until sorted.lastIndex,
            onDismiss = { settingsTrackId = null },
            onSave = { name, colorIndex ->
                viewModel.updateTrackProperties(settingsTrack.id, name, colorIndex)
                settingsTrackId = null
            },
            onMoveUp = { viewModel.moveTrack(settingsTrack.id, -1) },
            onMoveDown = { viewModel.moveTrack(settingsTrack.id, 1) },
            onDelete = {
                viewModel.deleteTrack(settingsTrack.id)
                settingsTrackId = null
            },
        )
    }
}

@Composable
private fun ProjectWorkspace(
    project: GuitarProject,
    waveforms: Map<String, List<Float>>,
    timelineControls: TimelineControlState,
    trimControls: TrimControlState?,
    transport: TransportState,
    importing: Boolean,
    editingClip: Boolean,
    error: String?,
    selectedTrackId: String?,
    onSelectTrack: (String) -> Unit,
    onOpenTrackSettings: (String) -> Unit,
    onAddTrack: () -> Unit,
    onImportWav: (String) -> Unit,
    onPlayheadFrameChanged: (Long) -> Unit,
    onLoopStartFrameChanged: (Long) -> Unit,
    onLoopEndFrameChanged: (Long) -> Unit,
    onBeginTrim: (String) -> Unit,
    onTrimStartFrameChanged: (Long) -> Unit,
    onTrimEndFrameChanged: (Long) -> Unit,
    onApplyTrim: () -> Unit,
    onCancelTrim: () -> Unit,
    onRemoveClip: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val baseProjectEndFrame = TimelineControlPolicy.projectEndFrame(project)
    val trimClip = trimControls?.let { state -> project.clips.firstOrNull { it.id == state.clipId } }
    val projectEndFrame = maxOf(baseProjectEndFrame, trimClip?.let(TrimControlPolicy::maximumEndFrame) ?: 0L).coerceAtLeast(1L)
    val timelineEditingEnabled = TransportPolicy.timelineEditingEnabled(transport)
    val trimActive = trimControls != null
    val clipEditingEnabled = !importing && !editingClip && timelineEditingEnabled && !trimActive

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (error != null) {
            CompactStatus(text = error, error = true)
        } else if (importing) {
            CompactStatus(text = "Importando áudio…")
        }

        Surface(
            modifier = Modifier.fillMaxWidth().weight(1f),
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.20f),
            tonalElevation = 0.dp,
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(TrackLaneGap),
                ) {
                    Surface(
                        modifier = Modifier.width(TrackSidebarWidth).height(62.dp),
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.82f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.38f)),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text("Pistas", style = MaterialTheme.typography.labelLarge)
                            AppIconButton(
                                icon = Icons.Default.Add,
                                contentDescription = "Adicionar pista",
                                enabled = clipEditingEnabled,
                                onClick = onAddTrack,
                            )
                        }
                    }
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
                        modifier = Modifier.weight(1f),
                    )
                }

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
                            selected = track.id == selectedTrackId,
                            canImport = clipEditingEnabled,
                            canEditClip = clipEditingEnabled,
                            activeTrimClipId = trimControls?.clipId,
                            onSelect = { onSelectTrack(track.id) },
                            onSettings = { onOpenTrackSettings(track.id) },
                            onImportWav = { onImportWav(track.id) },
                            onBeginTrim = onBeginTrim,
                            onRemove = onRemoveClip,
                        )
                    }
                }
            }
        }

        if (trimControls != null && trimClip != null) {
            TrimActionBar(
                clipName = trimClip.name,
                enabled = !editingClip && timelineEditingEnabled,
                onApply = onApplyTrim,
                onCancel = onCancelTrim,
            )
        }
    }
}

@Composable
private fun TimelineRuler(project: GuitarProject, projectEndFrame: Long) {
    val sampleRate = project.sampleRate.fixedHz
        ?: project.clips.firstOrNull { it.sourceSampleRateHz != null }?.sourceSampleRateHz
        ?: 48_000
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = TrackSidebarWidth + TrackLaneGap, end = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
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
    selected: Boolean,
    canImport: Boolean,
    canEditClip: Boolean,
    activeTrimClipId: String?,
    onSelect: () -> Unit,
    onSettings: () -> Unit,
    onImportWav: () -> Unit,
    onBeginTrim: (String) -> Unit,
    onRemove: (String) -> Unit,
) {
    val trackColor = track.resolvedStudioColor()
    val trackNameStyle = if (track.name.length > 34) MaterialTheme.typography.labelLarge else MaterialTheme.typography.bodyMedium
    Row(
        modifier = Modifier.fillMaxWidth().height(TrackLaneHeight),
        horizontalArrangement = Arrangement.spacedBy(TrackLaneGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            modifier = Modifier.width(TrackSidebarWidth).fillMaxHeight().clickable(onClick = onSelect),
            shape = RoundedCornerShape(8.dp),
            color = if (selected) trackColor.copy(alpha = 0.11f) else MaterialTheme.colorScheme.surface.copy(alpha = 0.86f),
            border = BorderStroke(if (selected) 1.5.dp else 1.dp, if (selected) trackColor else MaterialTheme.colorScheme.outline.copy(alpha = 0.38f)),
        ) {
            Row(modifier = Modifier.fillMaxSize()) {
                Box(Modifier.width(4.dp).fillMaxHeight().background(trackColor))
                Column(
                    modifier = Modifier.weight(1f).fillMaxHeight().padding(start = 8.dp, end = 4.dp, top = 5.dp, bottom = 5.dp),
                    verticalArrangement = Arrangement.SpaceEvenly,
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        Text(
                            text = track.name,
                            modifier = Modifier.weight(1f),
                            style = trackNameStyle,
                            maxLines = 2,
                            softWrap = true,
                        )
                        AppIconButton(
                            icon = Icons.Default.Add,
                            contentDescription = "Importar áudio",
                            enabled = canImport,
                            onClick = onImportWav,
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        Text(
                            text = roleName(track.roleId),
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            softWrap = true,
                        )
                        AppIconButton(
                            icon = Icons.Default.Settings,
                            contentDescription = "Configurar pista",
                            enabled = canEditClip,
                            onClick = onSettings,
                        )
                    }
                }
            }
        }

        BoxWithConstraints(
            modifier = Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.surface.copy(alpha = 0.70f)),
        ) {
            if (clips.isEmpty()) {
                Text(
                    "Sem áudio",
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
                        trackColor = trackColor,
                        trimming = activeTrimClipId == clip.id,
                        canEdit = canEditClip,
                        onTrim = { onBeginTrim(clip.id) },
                        onRemove = { onRemove(clip.id) },
                        modifier = Modifier.offset(x = x).padding(vertical = 4.dp).width(clipWidth).height(64.dp).align(Alignment.CenterStart),
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
    trackColor: Color,
    trimming: Boolean,
    canEdit: Boolean,
    onTrim: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(7.dp),
        color = if (trimming) StudioTrim.copy(alpha = 0.20f) else trackColor.copy(alpha = if (clip.muted) 0.10f else 0.20f),
        border = BorderStroke(1.dp, if (trimming) StudioTrim else trackColor.copy(alpha = 0.72f)),
        tonalElevation = 0.dp,
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 7.dp, vertical = 3.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    clip.name,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 1,
                    color = if (clip.muted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                )
                AppIconButton(
                    icon = Icons.Default.ContentCut,
                    contentDescription = "Cortar clipe",
                    enabled = canEdit,
                    onClick = onTrim,
                )
                AppIconButton(
                    icon = Icons.Default.Delete,
                    contentDescription = "Excluir clipe",
                    enabled = canEdit,
                    tint = MaterialTheme.colorScheme.error,
                    onClick = onRemove,
                )
            }
            peaks?.let { WaveformMini(peaks = it, muted = clip.muted, color = trackColor, modifier = Modifier.fillMaxWidth().height(20.dp)) }
        }
    }
}

@Composable
private fun TrimActionBar(
    clipName: String,
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
                Text("Corte • $clipName", style = MaterialTheme.typography.labelLarge)
                Text(
                    "Ajuste os marcadores amarelos e confirme.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            OutlinedButton(onClick = onCancel, enabled = enabled) { Text("Cancelar") }
            Button(
                onClick = onApply,
                enabled = enabled,
                colors = ButtonDefaults.buttonColors(containerColor = StudioTrim, contentColor = Color.White),
            ) { Text("Aplicar corte") }
        }
    }
}

@Composable
private fun TrackSettingsDialog(
    track: AudioTrack,
    hasClips: Boolean,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onDismiss: () -> Unit,
    onSave: (String, Int) -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onDelete: () -> Unit,
) {
    var name by remember(track.id, track.name) { mutableStateOf(track.name) }
    var colorIndex by remember(track.id, track.colorIndex) {
        mutableIntStateOf(if (track.colorIndex in StudioTrackPalette.indices) track.colorIndex else track.order % StudioTrackPalette.size)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Configurar pista") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    singleLine = true,
                    label = { Text("Nome") },
                    modifier = Modifier.fillMaxWidth(),
                )
                Text("Cor", style = MaterialTheme.typography.labelLarge)
                StudioTrackPalette.chunked(5).forEachIndexed { rowIndex, colors ->
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        colors.forEachIndexed { columnIndex, color ->
                            val index = rowIndex * 5 + columnIndex
                            Surface(
                                modifier = Modifier.size(30.dp).clip(CircleShape).clickable { colorIndex = index },
                                shape = CircleShape,
                                color = color,
                                border = BorderStroke(if (colorIndex == index) 3.dp else 1.dp, if (colorIndex == index) MaterialTheme.colorScheme.onSurface else color.copy(alpha = 0.55f)),
                            ) {}
                        }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Ordem", modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelLarge)
                    AppIconButton(icon = Icons.Default.KeyboardArrowUp, contentDescription = "Mover pista para cima", enabled = canMoveUp, onClick = onMoveUp)
                    AppIconButton(icon = Icons.Default.KeyboardArrowDown, contentDescription = "Mover pista para baixo", enabled = canMoveDown, onClick = onMoveDown)
                    AppIconButton(icon = Icons.Default.Delete, contentDescription = "Excluir pista", enabled = !hasClips, tint = MaterialTheme.colorScheme.error, onClick = onDelete)
                }
                if (hasClips) {
                    Text("Remova os clipes antes de excluir esta pista.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        },
        confirmButton = {
            Button(onClick = { onSave(name, colorIndex) }, enabled = name.isNotBlank()) { Text("Salvar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
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
    if (roleId == null) return "Sem função"
    return BuiltInRoles.definitions.firstOrNull { it.id == roleId }?.name ?: roleId
}

private fun formatTimelineTime(frame: Long, sampleRate: Int): String {
    val totalSeconds = if (sampleRate > 0) frame / sampleRate else 0L
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}
