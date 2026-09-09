package studio.guitarlab.app.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.scrollBy
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.CallSplit
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.zIndex
import kotlinx.coroutines.launch
import androidx.lifecycle.viewmodel.compose.viewModel
import studio.guitarlab.app.ui.theme.StudioTrim
import studio.guitarlab.app.ui.theme.StudioLoop
import studio.guitarlab.app.ui.theme.StudioPlayhead
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
import studio.guitarlab.core.project.RecordingSessionPhase

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
                recordingPhase = state.recordingSession.phase,
                countdownSeconds = state.recordingSession.countdownSecondsRemaining,
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
                onClearTrack = viewModel::clearTrackContents,
                onDuplicateClip = viewModel::duplicateClip,
                onSplitClip = viewModel::splitClipAtPlayhead,
                onReorderTrack = viewModel::reorderTrack,
                onMoveClipToTrack = viewModel::moveClipToTrack,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }

    val project = state.project
    val settingsTrack = settingsTrackId?.let { id -> project?.tracks?.firstOrNull { it.id == id } }
    if (settingsTrack != null && project != null) {
        TrackSettingsDialog(
            track = settingsTrack,
            clips = project.clips.filter { it.trackId == settingsTrack.id },
            onDismiss = { settingsTrackId = null },
            onSave = { name, colorIndex ->
                viewModel.updateTrackProperties(settingsTrack.id, name, colorIndex)
                settingsTrackId = null
            },
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
    recordingPhase: RecordingSessionPhase,
    countdownSeconds: Int,
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
    onClearTrack: (String) -> Unit,
    onDuplicateClip: (String) -> Unit,
    onSplitClip: (String) -> Unit,
    onReorderTrack: (String, Int) -> Unit,
    onMoveClipToTrack: (String, String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val baseProjectEndFrame = TimelineControlPolicy.projectEndFrame(project)
    val trimClip = trimControls?.let { state -> project.clips.firstOrNull { it.id == state.clipId } }
    val projectEndFrame = maxOf(baseProjectEndFrame, trimClip?.let(TrimControlPolicy::maximumEndFrame) ?: 0L).coerceAtLeast(1L)
    val timelineEditingEnabled = TransportPolicy.timelineEditingEnabled(transport)
    val trimActive = trimControls != null
    val clipEditingEnabled = !importing && !editingClip && timelineEditingEnabled && !trimActive

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
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
                        modifier = Modifier.weight(1f),
                    )
                }

                TimelineRuler(project, projectEndFrame)

                val orderedTracks = project.tracks.sortedBy { it.order }
                val trackListState = rememberLazyListState()
                val dragScope = rememberCoroutineScope()
                Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                  LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    state = trackListState,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    contentPadding = PaddingValues(bottom = 2.dp),
                  ) {
                    items(orderedTracks, key = { it.id }) { track ->
                        val trackIndex = orderedTracks.indexOfFirst { it.id == track.id }
                        StudioTrackLane(
                            track = track,
                            clips = project.clips.filter { it.trackId == track.id },
                            waveforms = waveforms,
                            projectEndFrame = projectEndFrame,
                            selected = track.id == selectedTrackId,
                            canImport = clipEditingEnabled,
                            canEditClip = clipEditingEnabled,
                            activeTrimClipId = trimControls?.clipId,
                            trimControls = trimControls,
                            sampleRate = project.sampleRate.fixedHz ?: clipSampleRate(project),
                            onTrimStartFrameChanged = onTrimStartFrameChanged,
                            onTrimEndFrameChanged = onTrimEndFrameChanged,
                            onSelect = { onSelectTrack(track.id) },
                            onSettings = { onOpenTrackSettings(track.id) },
                            onImportWav = { onImportWav(track.id) },
                            onBeginTrim = onBeginTrim,
                            onRemove = onRemoveClip,
                            onClearTrack = { onClearTrack(track.id) },
                            onDuplicate = onDuplicateClip,
                            onSplit = onSplitClip,
                            trackIndex = trackIndex,
                            trackCount = orderedTracks.size,
                            onReorder = { onReorderTrack(track.id, it) },
                            onMoveClip = { clipId, targetIndex -> onMoveClipToTrack(clipId, orderedTracks[targetIndex].id) },
                            onAutoScroll = { direction ->
                                if ((direction < 0 && trackListState.canScrollBackward) || (direction > 0 && trackListState.canScrollForward)) {
                                    dragScope.launch { trackListState.scrollBy(direction * 52f) }
                                }
                            },
                        )
                    }
                  }
                  GlobalTimelineLines(
                      playheadFrame = timelineControls.playheadFrame,
                      loopStartFrame = timelineControls.loopStartFrame,
                      loopEndFrame = timelineControls.loopEndFrame,
                      projectEndFrame = projectEndFrame,
                      showLoop = transport.loopEnabled,
                      modifier = Modifier.fillMaxSize(),
                  )
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

        if (recordingPhase == RecordingSessionPhase.COUNTDOWN) {
            Surface(
                modifier = Modifier.align(Alignment.CenterHorizontally),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.errorContainer,
                tonalElevation = 8.dp,
            ) {
                Text(
                    countdownSeconds.coerceAtLeast(1).toString(),
                    modifier = Modifier.padding(horizontal = 28.dp, vertical = 14.dp),
                    style = MaterialTheme.typography.displayMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                )
            }
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
private fun GlobalTimelineLines(
    playheadFrame: Long,
    loopStartFrame: Long,
    loopEndFrame: Long,
    projectEndFrame: Long,
    showLoop: Boolean,
    modifier: Modifier = Modifier,
) {
    val sidebarPx = with(LocalDensity.current) { (TrackSidebarWidth + TrackLaneGap).toPx() }
    Canvas(modifier) {
        val timelineWidth = (size.width - sidebarPx).coerceAtLeast(1f)
        fun x(frame: Long): Float = sidebarPx + TimelineControlPolicy.frameToFraction(frame, projectEndFrame) * timelineWidth
        if (showLoop) {
            drawLine(StudioLoop.copy(alpha = 0.62f), start = androidx.compose.ui.geometry.Offset(x(loopStartFrame), 0f), end = androidx.compose.ui.geometry.Offset(x(loopStartFrame), size.height), strokeWidth = 2f)
            drawLine(StudioLoop.copy(alpha = 0.62f), start = androidx.compose.ui.geometry.Offset(x(loopEndFrame), 0f), end = androidx.compose.ui.geometry.Offset(x(loopEndFrame), size.height), strokeWidth = 2f)
        }
        drawLine(StudioPlayhead.copy(alpha = 0.78f), start = androidx.compose.ui.geometry.Offset(x(playheadFrame), 0f), end = androidx.compose.ui.geometry.Offset(x(playheadFrame), size.height), strokeWidth = 2f)
    }
}

@Composable
private fun ClipMenuItem(
    icon: ImageVector,
    label: String,
    destructive: Boolean = false,
    onClick: () -> Unit,
) {
    val color = if (destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
    DropdownMenuItem(
        text = { Text(label, color = color) },
        leadingIcon = { Icon(icon, contentDescription = null, tint = color) },
        onClick = onClick,
    )
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
    trimControls: TrimControlState?,
    sampleRate: Int,
    onTrimStartFrameChanged: (Long) -> Unit,
    onTrimEndFrameChanged: (Long) -> Unit,
    onSelect: () -> Unit,
    onSettings: () -> Unit,
    onImportWav: () -> Unit,
    onBeginTrim: (String) -> Unit,
    onRemove: (String) -> Unit,
    onClearTrack: () -> Unit,
    onDuplicate: (String) -> Unit,
    onSplit: (String) -> Unit,
    trackIndex: Int,
    trackCount: Int,
    onReorder: (Int) -> Unit,
    onMoveClip: (String, Int) -> Unit,
    onAutoScroll: (Int) -> Unit,
) {
    var trackDragY by remember(track.id) { mutableFloatStateOf(0f) }
    var trackOrigin by remember(track.id) { mutableStateOf(IntOffset.Zero) }
    var trackMeasuredSize by remember(track.id) { mutableStateOf(IntSize.Zero) }
    var clipMenuExpanded by remember(track.id) { mutableStateOf(false) }
    val primaryClip = clips.minByOrNull { it.startFrame }
    val density = LocalDensity.current
    val rowStepPx = with(density) { (TrackLaneHeight + 4.dp).toPx() }
    val trackDropIndex = (trackIndex + kotlin.math.round(trackDragY / rowStepPx).toInt()).coerceIn(0, trackCount - 1)
    val trackColor = track.resolvedStudioColor()
    val trackNameStyle = if (track.name.length > 34) MaterialTheme.typography.labelLarge else MaterialTheme.typography.bodyMedium
    Row(
        modifier = Modifier.fillMaxWidth().height(TrackLaneHeight),
        horizontalArrangement = Arrangement.spacedBy(TrackLaneGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            modifier = Modifier.width(TrackSidebarWidth).fillMaxHeight()
                .zIndex(if (trackDragY != 0f) 2f else 0f)
                .alpha(if (trackDragY != 0f) 0.30f else 1f)
                .onGloballyPositioned { coordinates ->
                    val origin = coordinates.positionInWindow()
                    trackOrigin = IntOffset(origin.x.toInt(), origin.y.toInt())
                    trackMeasuredSize = coordinates.size
                }
                .pointerInput(track.id, canEditClip, trackIndex, trackCount) {
                    if (canEditClip) detectDragGesturesAfterLongPress(
                        onDragStart = { onSelect() },
                        onDragCancel = { trackDragY = 0f },
                        onDragEnd = {
                            val target = (trackIndex + kotlin.math.round(trackDragY / rowStepPx).toInt()).coerceIn(0, trackCount - 1)
                            trackDragY = 0f
                            if (target != trackIndex) onReorder(target)
                        },
                        onDrag = { change, amount ->
                            change.consume()
                            trackDragY += amount.y
                            val edge = 24.dp.toPx()
                            when {
                                change.position.y < edge -> { onAutoScroll(-1); trackDragY -= rowStepPx * 0.12f }
                                change.position.y > size.height - edge -> { onAutoScroll(1); trackDragY += rowStepPx * 0.12f }
                            }
                        },
                    )
                }
                .clickable(onClick = onSelect),
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
                        Box {
                            if (primaryClip == null) {
                                AppIconButton(
                                    icon = Icons.Default.Add,
                                    contentDescription = "Importar áudio",
                                    enabled = canImport,
                                    onClick = onImportWav,
                                )
                            } else {
                                AppIconButton(
                                    icon = Icons.Default.MoreVert,
                                    contentDescription = "Ações do áudio",
                                    enabled = canEditClip,
                                    onClick = { clipMenuExpanded = true },
                                )
                                DropdownMenu(expanded = clipMenuExpanded, onDismissRequest = { clipMenuExpanded = false }) {
                                    clips.sortedBy { it.startFrame }.forEach { clip ->
                                        val suffix = if (clips.size > 1) " · ${clip.name}" else ""
                                        ClipMenuItem(Icons.Default.ContentCopy, "Duplicar$suffix") { clipMenuExpanded = false; onDuplicate(clip.id) }
                                        ClipMenuItem(Icons.Default.CallSplit, "Dividir no cursor$suffix") { clipMenuExpanded = false; onSplit(clip.id) }
                                        ClipMenuItem(Icons.Default.ContentCut, "Cortar$suffix") { clipMenuExpanded = false; onBeginTrim(clip.id) }
                                    }
                                    ClipMenuItem(Icons.Default.Delete, "Limpar pista", destructive = true) { clipMenuExpanded = false; onClearTrack() }
                                }
                            }
                        }
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
                        trimControls = trimControls?.takeIf { it.clipId == clip.id },
                        sampleRate = sampleRate,
                        onTrimStartFrameChanged = onTrimStartFrameChanged,
                        onTrimEndFrameChanged = onTrimEndFrameChanged,
                        canEdit = canEditClip,
                        onTrim = { onBeginTrim(clip.id) },
                        onRemove = { onRemove(clip.id) },
                        onDuplicate = { onDuplicate(clip.id) },
                        onSplit = { onSplit(clip.id) },
                        trackIndex = trackIndex,
                        trackCount = trackCount,
                        onMoveTrack = { target -> onMoveClip(clip.id, target) },
                        onAutoScroll = onAutoScroll,
                        modifier = Modifier.offset(x = x).width(clipWidth).fillMaxHeight().padding(vertical = 5.dp).align(Alignment.CenterStart),
                    )
                }
            }
        }
    }
    if (trackDragY != 0f && trackMeasuredSize != IntSize.Zero) {
        val snappedY = trackOrigin.y + ((trackDropIndex - trackIndex) * rowStepPx).toInt()
        Popup(alignment = Alignment.TopStart, offset = IntOffset(trackOrigin.x, snappedY)) {
            Surface(
                modifier = Modifier.width(with(density) { trackMeasuredSize.width.toDp() }).height(TrackLaneHeight),
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(2.dp, trackColor),
                tonalElevation = 10.dp,
            ) {
                Column(Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalArrangement = Arrangement.SpaceBetween) {
                    Text(track.name, style = MaterialTheme.typography.titleSmall, maxLines = 1)
                    Text(roleName(track.roleId), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                    Text("Solte na posição ${trackDropIndex + 1}", style = MaterialTheme.typography.labelMedium, color = trackColor)
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
    trimControls: TrimControlState?,
    sampleRate: Int,
    onTrimStartFrameChanged: (Long) -> Unit,
    onTrimEndFrameChanged: (Long) -> Unit,
    canEdit: Boolean,
    onTrim: () -> Unit,
    onRemove: () -> Unit,
    onDuplicate: () -> Unit,
    onSplit: () -> Unit,
    trackIndex: Int,
    trackCount: Int,
    onMoveTrack: (Int) -> Unit,
    onAutoScroll: (Int) -> Unit,
    modifier: Modifier,
) {
    var clipDragY by remember(clip.id) { mutableFloatStateOf(0f) }
    var clipOrigin by remember(clip.id) { mutableStateOf(IntOffset.Zero) }
    var clipMeasuredSize by remember(clip.id) { mutableStateOf(IntSize.Zero) }
    val density = LocalDensity.current
    val rowStepPx = with(density) { (TrackLaneHeight + 4.dp).toPx() }
    val clipDropIndex = (trackIndex + kotlin.math.round(clipDragY / rowStepPx).toInt()).coerceIn(0, trackCount - 1)
    Surface(
        modifier = modifier
            .alpha(if (clipDragY != 0f) 0.24f else 1f)
            .onGloballyPositioned { coordinates ->
                val origin = coordinates.positionInWindow()
                clipOrigin = IntOffset(origin.x.toInt(), origin.y.toInt())
                clipMeasuredSize = coordinates.size
            }
            .pointerInput(clip.id, canEdit, trackIndex, trackCount) {
                if (canEdit) detectDragGesturesAfterLongPress(
                    onDragCancel = { clipDragY = 0f },
                    onDragEnd = {
                        val target = (trackIndex + kotlin.math.round(clipDragY / rowStepPx).toInt()).coerceIn(0, trackCount - 1)
                        clipDragY = 0f
                        if (target != trackIndex) onMoveTrack(target)
                    },
                    onDrag = { change, amount ->
                        change.consume()
                        clipDragY += amount.y
                        val edge = 24.dp.toPx()
                        when {
                            change.position.y < edge -> { onAutoScroll(-1); clipDragY -= rowStepPx * 0.12f }
                            change.position.y > size.height - edge -> { onAutoScroll(1); clipDragY += rowStepPx * 0.12f }
                        }
                    },
                )
            },
        shape = RoundedCornerShape(7.dp),
        color = trackColor.copy(alpha = if (clip.muted) 0.10f else 0.20f),
        border = BorderStroke(1.dp, if (trimming) StudioTrim else trackColor.copy(alpha = 0.72f)),
        tonalElevation = 0.dp,
    ) {
        Box(modifier = Modifier.fillMaxSize().padding(horizontal = 7.dp, vertical = 4.dp)) {
            peaks?.let {
                if (trimming && trimControls != null) {
                    TrimWaveformEditor(
                        clip = clip,
                        trim = trimControls,
                        peaks = it,
                        trackColor = trackColor,
                        sampleRate = sampleRate,
                        onStartChanged = onTrimStartFrameChanged,
                        onEndChanged = onTrimEndFrameChanged,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    WaveformMini(peaks = it, muted = clip.muted, color = trackColor, modifier = Modifier.fillMaxSize())
                }
            }
        }
    }
    if (clipDragY != 0f && clipMeasuredSize != IntSize.Zero) {
        val snappedY = clipOrigin.y + ((clipDropIndex - trackIndex) * rowStepPx).toInt()
        Popup(alignment = Alignment.TopStart, offset = IntOffset(clipOrigin.x, snappedY)) {
            Surface(
                modifier = Modifier
                    .width(with(density) { clipMeasuredSize.width.toDp() })
                    .height(with(density) { clipMeasuredSize.height.toDp() }),
                shape = RoundedCornerShape(7.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(2.dp, trackColor),
                tonalElevation = 12.dp,
            ) {
                Box(Modifier.fillMaxSize().padding(6.dp)) {
                    peaks?.let { WaveformMini(peaks = it, muted = clip.muted, color = trackColor, modifier = Modifier.fillMaxSize()) }
                    Surface(
                        modifier = Modifier.align(Alignment.TopEnd),
                        shape = RoundedCornerShape(5.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
                        border = BorderStroke(1.dp, trackColor),
                    ) {
                        Text("Mover para pista ${clipDropIndex + 1}", modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp), style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }
}

@Composable
private fun TrimWaveformEditor(
    clip: AudioClip,
    trim: TrimControlState,
    peaks: List<Float>,
    trackColor: Color,
    sampleRate: Int,
    onStartChanged: (Long) -> Unit,
    onEndChanged: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val clipEnd = clip.startFrame + clip.lengthFrames
    val startFraction = ((trim.startFrame - clip.startFrame).toFloat() / clip.lengthFrames.coerceAtLeast(1L)).coerceIn(0f, 1f)
    val endFraction = ((trim.endFrame - clip.startFrame).toFloat() / clip.lengthFrames.coerceAtLeast(1L)).coerceIn(startFraction, 1f)
    BoxWithConstraints(modifier) {
        val bubbleWidth = 88.dp
        val leftBubbleX = (maxWidth * startFraction - bubbleWidth / 2).coerceIn(0.dp, (maxWidth - bubbleWidth).coerceAtLeast(0.dp))
        val rightBubbleX = (maxWidth * endFraction - bubbleWidth / 2).coerceIn(0.dp, (maxWidth - bubbleWidth).coerceAtLeast(0.dp))
        val markersAreClose = endFraction - startFraction < 0.20f
        Canvas(Modifier.fillMaxSize()) {
            drawRect(
                color = StudioTrim.copy(alpha = 0.24f),
                topLeft = androidx.compose.ui.geometry.Offset(size.width * startFraction, 0f),
                size = androidx.compose.ui.geometry.Size(size.width * (endFraction - startFraction), size.height),
            )
        }
        WaveformMini(peaks = peaks, color = trackColor, modifier = Modifier.fillMaxSize())
        RangeSlider(
            value = startFraction..endFraction,
            onValueChange = { range ->
                val start = clip.startFrame + (clip.lengthFrames * range.start).toLong()
                val end = clip.startFrame + (clip.lengthFrames * range.endInclusive).toLong()
                onStartChanged(start.coerceAtMost(clipEnd - 1L))
                onEndChanged(end.coerceAtLeast(start + 1L))
            },
            valueRange = 0f..1f,
            colors = SliderDefaults.colors(
                thumbColor = StudioTrim,
                activeTrackColor = StudioTrim.copy(alpha = 0.58f),
                inactiveTrackColor = Color.Transparent,
            ),
            modifier = Modifier.fillMaxSize(),
        )
        TrimTimeBubble(
            label = "Início",
            time = formatPreciseTime(trim.startFrame, sampleRate),
            modifier = Modifier.offset(x = leftBubbleX, y = 2.dp).width(bubbleWidth),
        )
        TrimTimeBubble(
            label = "Fim",
            time = formatPreciseTime(trim.endFrame, sampleRate),
            modifier = Modifier.offset(x = rightBubbleX, y = if (markersAreClose) 44.dp else 2.dp).width(bubbleWidth),
        )
    }
}

@Composable
private fun TrimTimeBubble(label: String, time: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(6.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
        border = BorderStroke(1.dp, StudioTrim),
        tonalElevation = 6.dp,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(0.dp),
        ) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = StudioTrim, maxLines = 1)
            Text(time, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface, maxLines = 1)
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
    clips: List<AudioClip>,
    onDismiss: () -> Unit,
    onSave: (String, Int) -> Unit,
    onDelete: () -> Unit,
) {
    val hasClips = clips.isNotEmpty()
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
                if (clips.isNotEmpty()) {
                    Text("Áudio fonte", style = MaterialTheme.typography.labelLarge)
                    clips.sortedBy { it.startFrame }.forEach { clip ->
                        val format = listOfNotNull(
                            clip.sourceFormat,
                            clip.sourceSampleRateHz?.let { "$it Hz" },
                            clip.sourceChannelCount?.let { "$it canal(is)" },
                            clip.sourceBitsPerSample?.let { "$it-bit" },
                        ).joinToString(" · ")
                        Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                            Text(clip.name, style = MaterialTheme.typography.bodyMedium, maxLines = 1)
                            if (format.isNotBlank()) Text(format, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                if (hasClips) {
                    Text("Use “Limpar pista” no menu de três pontos antes de excluir a pista.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                OutlinedButton(
                    onClick = onDelete,
                    enabled = !hasClips,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null)
                    Text("Excluir pista", modifier = Modifier.padding(start = 8.dp))
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

private fun formatPreciseTime(frame: Long, sampleRate: Int): String {
    val millis = if (sampleRate > 0) frame * 1_000L / sampleRate else 0L
    return "%02d:%02d.%03d".format(millis / 60_000L, (millis / 1_000L) % 60L, millis % 1_000L)
}

private fun clipSampleRate(project: GuitarProject): Int =
    project.clips.firstNotNullOfOrNull { it.sourceSampleRateHz } ?: 48_000
