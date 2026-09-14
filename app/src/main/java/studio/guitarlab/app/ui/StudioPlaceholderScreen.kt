package studio.guitarlab.app.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CallSplit
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay
import studio.guitarlab.app.ui.theme.StudioLoop
import studio.guitarlab.app.ui.theme.StudioPlayhead
import studio.guitarlab.app.ui.theme.StudioRecord
import studio.guitarlab.app.ui.theme.StudioTrim
import studio.guitarlab.core.model.AudioClip
import studio.guitarlab.core.model.AudioTrack
import studio.guitarlab.core.model.BuiltInRoles
import studio.guitarlab.core.model.GuitarProject
import studio.guitarlab.core.project.RecordingSessionPhase
import studio.guitarlab.core.project.ActiveTakePolicy
import studio.guitarlab.core.project.GuitarAuditionMode
import studio.guitarlab.core.project.GuitarAuditionPolicy
import studio.guitarlab.core.project.GuitarAuditionTrackState
import studio.guitarlab.core.project.TimelineControlPolicy
import studio.guitarlab.core.project.TimelineControlState
import studio.guitarlab.core.project.TimelineDragPolicy
import studio.guitarlab.core.project.TransportPolicy
import studio.guitarlab.core.project.TransportState
import studio.guitarlab.core.project.TrimControlPolicy
import studio.guitarlab.core.project.TrimControlState

private val TrackSidebarWidth = 224.dp
private val TrackLaneGap = 6.dp
private val TrackLaneHeight = 96.dp

enum class WorkspaceDragKind { TRACK, CLIP }

private data class WorkspaceDragState(
    val kind: WorkspaceDragKind,
    val itemId: String,
    val sourceTrackId: String,
    val sourceIndex: Int,
    val pointer: Offset,
    val grabOffset: Offset,
    val itemSize: IntSize,
    val color: Color,
    val label: String,
    val secondaryLabel: String? = null,
    val peaks: List<Float>? = null,
    val targetIndex: Int? = null,
    val targetTrackId: String? = null,
)

@Composable
fun StudioPlaceholderScreen(
    viewModel: StudioViewModel = viewModel(),
    selectedTrackId: String? = null,
    onSelectTrack: (String) -> Unit = {},
) {
    val state by viewModel.state.collectAsState()
    var pendingTrackId by remember { mutableStateOf<String?>(null) }
    var settingsTrackId by remember { mutableStateOf<String?>(null) }
    var fadeClipId by remember { mutableStateOf<String?>(null) }
    val audioPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        val trackId = pendingTrackId
        pendingTrackId = null
        if (uri != null && trackId != null) viewModel.importAudio(trackId, uri)
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
                waveformChannels = state.waveformChannels,
                timelineControls = state.timelineControls,
                trimControls = state.trimControls,
                transport = state.transport,
                importing = state.importing,
                editingClip = state.editingClip,
                recordingPhase = state.recordingSession.phase,
                countdownSeconds = state.recordingSession.countdownSecondsRemaining,
                liveRecordingPeaks = state.liveRecordingPeaks,
                recordingTrackId = state.recordingSession.targetTrackId,
                recordingStartFrame = state.recordingSession.timelineStartFrame,
                recordingFrames = state.recordingSession.framesCaptured,
                auditionMode = state.guitarAuditionMode,
                sectionSuggestions = state.sectionSuggestions,
                selectedTrackId = selectedTrackId,
                onSelectTrack = onSelectTrack,
                onOpenTrackSettings = { settingsTrackId = it },
                onAddTrack = viewModel::addTrack,
                onImportWav = { trackId ->
                    if (!state.importing && !state.editingClip && state.trimControls == null && TransportPolicy.timelineEditingEnabled(state.transport)) {
                        pendingTrackId = trackId
                        audioPicker.launch(studio.guitarlab.core.codec.AudioImportFormatPolicy.pickerMimeTypes)
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
                onSplitStereo = viewModel::separateStereoClip,
                onOpenFades = { fadeClipId = it },
                onCrossfade = viewModel::crossfadeWithNext,
                onReorderTrack = viewModel::reorderTrack,
                onMoveClipToTrack = viewModel::moveClipToTrack,
                onAuditionMode = viewModel::setGuitarAuditionMode,
                onAddMarker = viewModel::addMarkerAtPlayhead,
                onAddSection = viewModel::addSectionFromLoop,
                onSuggestSections = viewModel::suggestSections,
                onAcceptSections = viewModel::acceptSectionSuggestions,
                onDiscardSections = viewModel::discardSectionSuggestions,
                onLoopSection = viewModel::loopSection,
                onRemoveMarker = viewModel::removeMarker,
                onRemoveSection = viewModel::removeSection,
                onSetPunch = viewModel::setPunchFromLoop,
                onClearPunch = viewModel::clearPunch,
                modifier = Modifier.fillMaxSize(),
            )
        }
        if (state.importing) {
            ImportProcessingOverlay(
                message = state.importStatus ?: "Processando áudio…",
                modifier = Modifier.fillMaxSize().zIndex(20f),
            )
        }
    }

    val project = state.project
    val settingsTrack = settingsTrackId?.let { id -> project?.tracks?.firstOrNull { it.id == id } }
    if (settingsTrack != null && project != null) {
        TrackSettingsDialog(
            track = settingsTrack,
            clips = project.clips.filter { it.trackId == settingsTrack.id },
            sampleRate = project.sampleRate.fixedHz ?: clipSampleRate(project),
            takes = project.takes.filter { it.trackId == settingsTrack.id },
            levelAnalysis = state.trackLevelAnalysis[settingsTrack.id],
            onActivateTake = viewModel::activateTake,
            onAnalyzeLevel = { viewModel.analyzeTrackLevel(settingsTrack.id) },
            onApplyLevel = { viewModel.applyTrackLevelSuggestion(settingsTrack.id) },
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

    fadeClipId?.let { id ->
        project?.clips?.firstOrNull { it.id == id }?.let { clip ->
            ClipFadeDialog(
                clip = clip,
                sampleRateHz = clip.editingSampleRateHz ?: clip.sourceSampleRateHz ?: project.sampleRate.fixedHz ?: 48_000,
                onDismiss = { fadeClipId = null },
                onApply = { fadeIn, fadeOut -> viewModel.setClipFades(clip.id, fadeIn, fadeOut); fadeClipId = null },
            )
        }
    }

    state.stereoImportPrompt?.let { prompt ->
        AlertDialog(
            onDismissRequest = {},
            title = { Text("Áudio estéreo detectado") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("${prompt.fileName} possui dois canais independentes (L/R).")
                    if (prompt.hasGuitarPair) {
                        Text("Esta é uma função de guitarra. O GuitarLab pode distribuir L para ${prompt.leftTrackName} e R para ${prompt.rightTrackName}, mantendo início, duração e sincronismo.")
                    } else {
                        Text("Você pode manter o arquivo estéreo nesta pista — recomendado para Base — ou separá-lo em duas pistas mono L/R.")
                    }
                }
            },
            confirmButton = {
                Button(onClick = { viewModel.separateStereoClip(prompt.clipId) }) {
                    Text(if (prompt.hasGuitarPair) "Distribuir L/R" else "Separar em 2 mono")
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::keepStereoImport) { Text("Manter estéreo") }
            },
        )
    }
}

@Composable
private fun PracticeControls(
    project: GuitarProject,
    auditionMode: GuitarAuditionMode,
    suggestions: Int,
    enabled: Boolean,
    onAuditionMode: (GuitarAuditionMode) -> Unit,
    onAddMarker: () -> Unit,
    onAddSection: () -> Unit,
    onSuggestSections: () -> Unit,
    onAcceptSections: () -> Unit,
    onDiscardSections: () -> Unit,
    onLoopSection: (String) -> Unit,
    onRemoveMarker: (String) -> Unit,
    onRemoveSection: (String) -> Unit,
    onSetPunch: () -> Unit,
    onClearPunch: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PracticeControlGroup("Comparação") {
            GuitarAuditionMode.entries.forEach { mode ->
                val label = when(mode) { GuitarAuditionMode.MIXER -> "Mixer"; GuitarAuditionMode.REFERENCE -> "Referência"; GuitarAuditionMode.MY_GUITAR -> "Minha"; GuitarAuditionMode.BOTH -> "Ambas" }
                if (mode == auditionMode) Button(onClick={onAuditionMode(mode)}, contentPadding=PaddingValues(horizontal=10.dp,vertical=2.dp), modifier=Modifier.semantics { selected = true }) { Text(label) }
                else OutlinedButton(onClick={onAuditionMode(mode)}, contentPadding=PaddingValues(horizontal=10.dp,vertical=2.dp)) { Text(label) }
            }
        }
        PracticeControlGroup("Timeline") {
            OutlinedButton(onClick=onAddMarker, enabled=enabled, contentPadding=PaddingValues(horizontal=10.dp,vertical=2.dp)) { Text("+ Marcador") }
            OutlinedButton(onClick=onAddSection, enabled=enabled, contentPadding=PaddingValues(horizontal=10.dp,vertical=2.dp)) { Text("Criar seção do loop") }
            OutlinedButton(onClick=onSuggestSections, enabled=enabled, contentPadding=PaddingValues(horizontal=10.dp,vertical=2.dp)) { Text("Detectar seções") }
            if (suggestions > 0) {
                Button(onClick=onAcceptSections, enabled=enabled, contentPadding=PaddingValues(horizontal=10.dp,vertical=2.dp)) { Text("Aceitar $suggestions") }
                TextButton(onClick=onDiscardSections) { Text("Descartar") }
            }
        }
        PracticeControlGroup("Gravação punch") {
            if (project.punchRegion == null) OutlinedButton(onClick=onSetPunch, enabled=enabled, contentPadding=PaddingValues(horizontal=10.dp,vertical=2.dp)) { Text("Definir pelo loop") }
            else OutlinedButton(onClick=onClearPunch, enabled=enabled, contentPadding=PaddingValues(horizontal=10.dp,vertical=2.dp)) { Text("Limpar punch") }
        }
    }
}

@Composable
private fun PracticeControlGroup(title: String, content: @Composable RowScope.() -> Unit) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.34f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            content()
        }
    }
}

@Composable
private fun ProjectWorkspace(
    project: GuitarProject,
    waveforms: Map<String, List<Float>>,
    waveformChannels: Map<String, List<List<Float>>>,
    timelineControls: TimelineControlState,
    trimControls: TrimControlState?,
    transport: TransportState,
    importing: Boolean,
    editingClip: Boolean,
    recordingPhase: RecordingSessionPhase,
    countdownSeconds: Int,
    liveRecordingPeaks: List<Float>,
    recordingTrackId: String?,
    recordingStartFrame: Long,
    recordingFrames: Long,
    auditionMode: GuitarAuditionMode,
    sectionSuggestions: List<studio.guitarlab.core.project.SectionBoundarySuggestion>,
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
    onSplitStereo: (String) -> Unit,
    onOpenFades: (String) -> Unit,
    onCrossfade: (String) -> Unit,
    onReorderTrack: (String, Int) -> Unit,
    onMoveClipToTrack: (String, String) -> Unit,
    onAuditionMode: (GuitarAuditionMode) -> Unit,
    onAddMarker: () -> Unit,
    onAddSection: () -> Unit,
    onSuggestSections: () -> Unit,
    onAcceptSections: () -> Unit,
    onDiscardSections: () -> Unit,
    onLoopSection: (String) -> Unit,
    onRemoveMarker: (String) -> Unit,
    onRemoveSection: (String) -> Unit,
    onSetPunch: () -> Unit,
    onClearPunch: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val baseProjectEndFrame = TimelineControlPolicy.projectEndFrame(project)
    val trimClip = trimControls?.let { state -> project.clips.firstOrNull { it.id == state.clipId } }
    val liveEndFrame = if (recordingPhase == RecordingSessionPhase.CAPTURING) recordingStartFrame + recordingFrames else 0L
    val projectEndFrame = maxOf(baseProjectEndFrame, trimClip?.let(TrimControlPolicy::maximumEndFrame) ?: 0L, liveEndFrame).coerceAtLeast(1L)
    val timelineEditingEnabled = TransportPolicy.timelineEditingEnabled(transport)
    val trimActive = trimControls != null
    val clipEditingEnabled = !importing && !editingClip && timelineEditingEnabled && !trimActive

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        PracticeControls(
            project = project,
            auditionMode = auditionMode,
            suggestions = sectionSuggestions.size,
            enabled = timelineEditingEnabled && !trimActive,
            onAuditionMode = onAuditionMode,
            onAddMarker = onAddMarker,
            onAddSection = onAddSection,
            onSuggestSections = onSuggestSections,
            onAcceptSections = onAcceptSections,
            onDiscardSections = onDiscardSections,
            onLoopSection = onLoopSection,
            onRemoveMarker = onRemoveMarker,
            onRemoveSection = onRemoveSection,
            onSetPunch = onSetPunch,
            onClearPunch = onClearPunch,
        )
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
                        Column(
                            modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                        ) {
                            Text("Pistas", style = MaterialTheme.typography.labelLarge)
                            Text(
                                "${project.tracks.size} ${if (project.tracks.size == 1) "pista" else "pistas"} · " +
                                    "${project.clips.size} ${if (project.clips.size == 1) "clipe" else "clipes"} · " +
                                    formatFrameTime(baseProjectEndFrame, project.sampleRate.fixedHz ?: clipSampleRate(project)),
                                modifier = Modifier.testTag("project-track-summary"),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    TimelineMarkerRail(
                        projectEndFrame = projectEndFrame,
                        sampleRateHz = project.sampleRate.fixedHz ?: clipSampleRate(project),
                        playheadFrame = timelineControls.playheadFrame,
                        loopStartFrame = timelineControls.loopStartFrame,
                        loopEndFrame = timelineControls.loopEndFrame,
                        showLoopMarkers = transport.loopEnabled,
                        sections = project.sections,
                        markers = project.markers,
                        punchRegion = project.punchRegion,
                        enabled = timelineEditingEnabled,
                        onPlayheadFrameChanged = onPlayheadFrameChanged,
                        onLoopStartFrameChanged = onLoopStartFrameChanged,
                        onLoopEndFrameChanged = onLoopEndFrameChanged,
                        onSectionClick = onLoopSection,
                        onSectionRemove = onRemoveSection,
                        onMarkerRemove = onRemoveMarker,
                        modifier = Modifier.weight(1f),
                    )
                }

                TimelineRuler(project, projectEndFrame)

                val orderedTracks = project.tracks.sortedBy { it.order }
                val orderedIds = orderedTracks.map { it.id }
                val trackListState = rememberLazyListState()
                var dragState by remember { mutableStateOf<WorkspaceDragState?>(null) }
                var workspaceWindowOrigin by remember { mutableStateOf(Offset.Zero) }
                var workspaceSize by remember { mutableStateOf(IntSize.Zero) }
                val density = LocalDensity.current
                val edgeZonePx = with(density) { 72.dp.toPx() }
                val maxScrollStepPx = with(density) { 18.dp.toPx() }

                fun visibleLanes(): List<TimelineDragPolicy.LaneBounds> =
                    trackListState.layoutInfo.visibleItemsInfo.mapNotNull { item ->
                        val track = orderedTracks.getOrNull(item.index) ?: return@mapNotNull null
                        TimelineDragPolicy.LaneBounds(
                            trackId = track.id,
                            topPx = item.offset.toFloat(),
                            bottomPx = item.offset.toFloat() + item.size,
                            order = item.index,
                        )
                    }

                fun retarget(state: WorkspaceDragState): WorkspaceDragState {
                    val lanes = visibleLanes()
                    return when (state.kind) {
                        WorkspaceDragKind.TRACK -> state.copy(
                            targetIndex = TimelineDragPolicy.trackTargetIndex(
                                sourceTrackId = state.sourceTrackId,
                                pointerYPx = state.pointer.y,
                                visibleLanes = lanes,
                                orderedTrackIds = orderedIds,
                            ),
                            targetTrackId = null,
                        )
                        WorkspaceDragKind.CLIP -> state.copy(
                            targetTrackId = TimelineDragPolicy.clipTargetTrackId(state.pointer.y, lanes),
                            targetIndex = null,
                        )
                    }
                }

                fun beginTrackDrag(track: AudioTrack, trackIndex: Int, itemOriginWindow: Offset, touch: Offset, itemSize: IntSize) {
                    if (!clipEditingEnabled) return
                    val origin = itemOriginWindow - workspaceWindowOrigin
                    val state = WorkspaceDragState(
                        kind = WorkspaceDragKind.TRACK,
                        itemId = track.id,
                        sourceTrackId = track.id,
                        sourceIndex = trackIndex,
                        pointer = origin + touch,
                        grabOffset = touch,
                        itemSize = itemSize,
                        color = track.resolvedStudioColor(),
                        label = track.name,
                        secondaryLabel = roleName(track.roleId),
                    )
                    dragState = retarget(state)
                    onSelectTrack(track.id)
                }

                fun beginClipDrag(clip: AudioClip, trackIndex: Int, color: Color, peaks: List<Float>?, itemOriginWindow: Offset, touch: Offset, itemSize: IntSize) {
                    if (!clipEditingEnabled) return
                    val origin = itemOriginWindow - workspaceWindowOrigin
                    val state = WorkspaceDragState(
                        kind = WorkspaceDragKind.CLIP,
                        itemId = clip.id,
                        sourceTrackId = clip.trackId,
                        sourceIndex = trackIndex,
                        pointer = origin + touch,
                        grabOffset = touch,
                        itemSize = itemSize,
                        color = color,
                        label = clip.name,
                        peaks = peaks,
                    )
                    dragState = retarget(state)
                }

                fun dragBy(amount: Offset) {
                    val current = dragState ?: return
                    dragState = retarget(current.copy(pointer = current.pointer + amount))
                }

                fun cancelDrag() {
                    dragState = null
                }

                fun commitDrag() {
                    val current = dragState ?: return
                    dragState = null
                    when (current.kind) {
                        WorkspaceDragKind.TRACK -> {
                            val target = current.targetIndex ?: current.sourceIndex
                            if (target != current.sourceIndex) onReorderTrack(current.itemId, target)
                        }
                        WorkspaceDragKind.CLIP -> {
                            val targetTrackId = current.targetTrackId
                            if (targetTrackId != null && targetTrackId != current.sourceTrackId) {
                                onMoveClipToTrack(current.itemId, targetTrackId)
                            }
                        }
                    }
                }

                LaunchedEffect(dragState?.kind, dragState?.itemId) {
                    while (dragState != null) {
                        val current = dragState ?: break
                        val auto = TimelineDragPolicy.autoScroll(
                            pointerYPx = current.pointer.y,
                            viewportTopPx = 0f,
                            viewportBottomPx = workspaceSize.height.toFloat(),
                            edgeZonePx = edgeZonePx,
                            maxStepPx = maxScrollStepPx,
                            canScrollBackward = trackListState.canScrollBackward,
                            canScrollForward = trackListState.canScrollForward,
                        )
                        if (auto.deltaPx != 0f) {
                            trackListState.scrollBy(auto.deltaPx)
                            dragState?.let { dragState = retarget(it) }
                        }
                        delay(16L)
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .onGloballyPositioned { coordinates ->
                            workspaceWindowOrigin = coordinates.positionInWindow()
                            workspaceSize = coordinates.size
                        },
                ) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        state = trackListState,
                        userScrollEnabled = dragState == null,
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        contentPadding = PaddingValues(bottom = 2.dp),
                    ) {
                        items(orderedTracks, key = { it.id }) { track ->
                            val trackIndex = orderedTracks.indexOfFirst { it.id == track.id }
                            StudioTrackLane(
                                track = track,
                                auditionMode = auditionMode,
                                clips = ActiveTakePolicy.audibleClips(project).filter { it.trackId == track.id },
                                livePeaks = if (recordingTrackId == track.id && recordingPhase == RecordingSessionPhase.CAPTURING) liveRecordingPeaks else emptyList(),
                                liveStartFrame = recordingStartFrame,
                                liveFrames = recordingFrames,
                                waveforms = waveforms,
                                waveformChannels = waveformChannels,
                                projectEndFrame = projectEndFrame,
                                selected = track.id == selectedTrackId,
                                canImport = clipEditingEnabled && dragState == null,
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
                                onSplitStereo = onSplitStereo,
                                onOpenFades = onOpenFades,
                                onCrossfade = onCrossfade,
                                trackIndex = trackIndex,
                                draggingTrackId = dragState?.takeIf { it.kind == WorkspaceDragKind.TRACK }?.itemId,
                                draggingClipId = dragState?.takeIf { it.kind == WorkspaceDragKind.CLIP }?.itemId,
                                onStartTrackDrag = { origin, touch, size -> beginTrackDrag(track, trackIndex, origin, touch, size) },
                                onStartClipDrag = { clip, color, peaks, origin, touch, size -> beginClipDrag(clip, trackIndex, color, peaks, origin, touch, size) },
                                onDrag = ::dragBy,
                                onDragEnd = ::commitDrag,
                                onDragCancel = ::cancelDrag,
                            )
                        }
                        item(key = "add-track-footer") {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(TrackLaneGap),
                            ) {
                                OutlinedButton(
                                    onClick = onAddTrack,
                                    enabled = clipEditingEnabled && dragState == null,
                                    modifier = Modifier
                                        .width(TrackSidebarWidth)
                                        .height(52.dp)
                                        .testTag("add-track-footer"),
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.38f)),
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null)
                                    Text("Adicionar pista", modifier = Modifier.padding(start = 8.dp))
                                }
                                Box(modifier = Modifier.weight(1f))
                            }
                        }
                    }

                    GlobalTimelineLines(
                        project = project,
                        playheadFrame = timelineControls.playheadFrame,
                        loopStartFrame = timelineControls.loopStartFrame,
                        loopEndFrame = timelineControls.loopEndFrame,
                        projectEndFrame = projectEndFrame,
                        showLoop = transport.loopEnabled,
                        modifier = Modifier.fillMaxSize(),
                    )

                    DragOverlay(
                        drag = dragState,
                        visibleLanes = visibleLanes(),
                        orderedTracks = orderedTracks,
                        modifier = Modifier.fillMaxSize().zIndex(20f),
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
private fun DragOverlay(
    drag: WorkspaceDragState?,
    visibleLanes: List<TimelineDragPolicy.LaneBounds>,
    orderedTracks: List<AudioTrack>,
    modifier: Modifier = Modifier,
) {
    if (drag == null) return
    val density = LocalDensity.current
    val ghostX = drag.pointer.x - drag.grabOffset.x
    val ghostY = drag.pointer.y - drag.grabOffset.y
    val orderedIds = orderedTracks.map { it.id }

    Box(modifier = modifier) {
        if (drag.kind == WorkspaceDragKind.TRACK) {
            val indicatorY = drag.targetIndex?.let {
                TimelineDragPolicy.insertionIndicatorYPx(it, visibleLanes, orderedIds)
            }
            if (indicatorY != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .offset { IntOffset(0, indicatorY.toInt()) }
                        .height(3.dp)
                        .background(drag.color),
                )
                Surface(
                    modifier = Modifier
                        .offset { IntOffset(with(density) { 8.dp.roundToPx() }, (indicatorY - with(density) { 28.dp.toPx() }).toInt()) },
                    shape = RoundedCornerShape(5.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
                    border = BorderStroke(1.dp, drag.color),
                ) {
                    Text(
                        "Posição ${(drag.targetIndex ?: drag.sourceIndex) + 1}",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = drag.color,
                    )
                }
            }
        } else {
            val targetBounds = visibleLanes.firstOrNull { it.trackId == drag.targetTrackId }
            if (targetBounds != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .offset { IntOffset(0, targetBounds.topPx.toInt()) }
                        .height(with(density) { targetBounds.heightPx.toDp() })
                        .border(2.dp, drag.color, RoundedCornerShape(8.dp)),
                )
                val targetName = orderedTracks.firstOrNull { it.id == drag.targetTrackId }?.name ?: "pista"
                Surface(
                    modifier = Modifier.offset { IntOffset(with(density) { (TrackSidebarWidth + 10.dp).roundToPx() }, (targetBounds.topPx + 6f).toInt()) },
                    shape = RoundedCornerShape(5.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
                    border = BorderStroke(1.dp, drag.color),
                ) {
                    Text(
                        "Mover para $targetName",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            }
        }

        Surface(
            modifier = Modifier
                .offset { IntOffset(ghostX.toInt(), ghostY.toInt()) }
                .width(with(density) { drag.itemSize.width.toDp() })
                .height(with(density) { drag.itemSize.height.toDp() }),
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f),
            border = BorderStroke(2.dp, drag.color),
            tonalElevation = 12.dp,
        ) {
            if (drag.kind == WorkspaceDragKind.CLIP) {
                Box(Modifier.fillMaxSize().padding(6.dp)) {
                    drag.peaks?.let { WaveformMini(peaks = it, color = drag.color, modifier = Modifier.fillMaxSize()) }
                    Text(
                        drag.label,
                        modifier = Modifier.align(Alignment.BottomStart),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                    )
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(drag.label, style = MaterialTheme.typography.titleSmall, maxLines = 1)
                    drag.secondaryLabel?.let {
                        Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                    }
                    Text("Arraste e solte", style = MaterialTheme.typography.labelMedium, color = drag.color)
                }
            }
        }
    }
}

@Composable
private fun ImportProcessingOverlay(message: String, modifier: Modifier = Modifier) {
    Box(modifier.background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.48f)), contentAlignment = Alignment.Center) {
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 12.dp,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 22.dp, vertical = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                CircularProgressIndicator(modifier = Modifier.size(28.dp), strokeWidth = 3.dp)
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text("Preparando áudio", style = MaterialTheme.typography.titleSmall)
                    Text(message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun StereoWaveformMini(channelPeaks: List<List<Float>>, muted: Boolean, color: Color, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        channelPeaks.take(2).forEachIndexed { index, peaks ->
            Box(Modifier.weight(1f).fillMaxWidth()) {
                WaveformMini(peaks = peaks, muted = muted, color = color, modifier = Modifier.fillMaxSize())
                Text(
                    if (index == 0) "L" else "R",
                    modifier = Modifier.align(Alignment.TopStart).padding(start = 2.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = color.copy(alpha = 0.78f),
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
    project: GuitarProject,
    playheadFrame: Long,
    loopStartFrame: Long,
    loopEndFrame: Long,
    projectEndFrame: Long,
    showLoop: Boolean,
    modifier: Modifier = Modifier,
) {
    val sidebarPx = with(LocalDensity.current) { (TrackSidebarWidth + TrackLaneGap).toPx() }
    val sectionColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.055f)
    val markerColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.58f)
    Canvas(modifier) {
        val timelineWidth = (size.width - sidebarPx).coerceAtLeast(1f)
        fun x(frame: Long): Float = sidebarPx + TimelineControlPolicy.frameToFraction(frame, projectEndFrame) * timelineWidth
        project.sections.forEach { section -> drawRect(sectionColor, topLeft = Offset(x(section.startFrame), 0f), size = androidx.compose.ui.geometry.Size((x(section.endFrame) - x(section.startFrame)).coerceAtLeast(1f), size.height)) }
        project.punchRegion?.let { punch -> drawRect(StudioRecord.copy(alpha = 0.07f), topLeft = Offset(x(punch.startFrame), 0f), size = androidx.compose.ui.geometry.Size((x(punch.endFrame) - x(punch.startFrame)).coerceAtLeast(1f), size.height)) }
        project.markers.forEach { marker -> drawLine(markerColor, Offset(x(marker.frame), 0f), Offset(x(marker.frame), size.height), strokeWidth = 1.5f) }
        if (showLoop) {
            drawLine(StudioLoop.copy(alpha = 0.62f), start = Offset(x(loopStartFrame), 0f), end = Offset(x(loopStartFrame), size.height), strokeWidth = 2f)
            drawLine(StudioLoop.copy(alpha = 0.62f), start = Offset(x(loopEndFrame), 0f), end = Offset(x(loopEndFrame), size.height), strokeWidth = 2f)
        }
        drawLine(StudioPlayhead.copy(alpha = 0.78f), start = Offset(x(playheadFrame), 0f), end = Offset(x(playheadFrame), size.height), strokeWidth = 2f)
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
    auditionMode: GuitarAuditionMode,
    clips: List<AudioClip>,
    livePeaks: List<Float>,
    liveStartFrame: Long,
    liveFrames: Long,
    waveforms: Map<String, List<Float>>,
    waveformChannels: Map<String, List<List<Float>>>,
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
    onSplitStereo: (String) -> Unit,
    onOpenFades: (String) -> Unit,
    onCrossfade: (String) -> Unit,
    trackIndex: Int,
    draggingTrackId: String?,
    draggingClipId: String?,
    onStartTrackDrag: (Offset, Offset, IntSize) -> Unit,
    onStartClipDrag: (AudioClip, Color, List<Float>?, Offset, Offset, IntSize) -> Unit,
    onDrag: (Offset) -> Unit,
    onDragEnd: () -> Unit,
    onDragCancel: () -> Unit,
) {
    var trackOrigin by remember(track.id) { mutableStateOf(Offset.Zero) }
    var trackMeasuredSize by remember(track.id) { mutableStateOf(IntSize.Zero) }
    var clipMenuExpanded by remember(track.id) { mutableStateOf(false) }
    val currentTrackOrigin by rememberUpdatedState(trackOrigin)
    val currentTrackSize by rememberUpdatedState(trackMeasuredSize)
    val primaryClip = clips.minByOrNull { it.startFrame }
    val trackColor = track.resolvedStudioColor()
    val auditionState = GuitarAuditionPolicy.trackState(track.roleId, auditionMode)
    val trackNameStyle = if (track.name.length > 34) MaterialTheme.typography.labelLarge else MaterialTheme.typography.bodyMedium
    val dragInProgress = draggingTrackId != null || draggingClipId != null
    val controlsEnabled = canEditClip && !dragInProgress

    Row(
        modifier = Modifier.fillMaxWidth().height(TrackLaneHeight),
        horizontalArrangement = Arrangement.spacedBy(TrackLaneGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            modifier = Modifier
                .width(TrackSidebarWidth)
                .fillMaxHeight()
                .alpha(if (draggingTrackId == track.id) 0.28f else 1f)
                .onGloballyPositioned { coordinates ->
                    trackOrigin = coordinates.positionInWindow()
                    trackMeasuredSize = coordinates.size
                }
                .pointerInput(track.id, canEditClip) {
                    if (canEditClip) detectDragGesturesAfterLongPress(
                        onDragStart = { touch -> onStartTrackDrag(currentTrackOrigin, touch, currentTrackSize) },
                        onDragCancel = onDragCancel,
                        onDragEnd = onDragEnd,
                        onDrag = { change, amount ->
                            change.consume()
                            onDrag(amount)
                        },
                    )
                }
                .clickable(enabled = !dragInProgress, onClick = onSelect),
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
                        if (auditionState != GuitarAuditionTrackState.UNAFFECTED) {
                            val included = auditionState == GuitarAuditionTrackState.INCLUDED
                            Surface(
                                shape = RoundedCornerShape(5.dp),
                                color = if (included) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.semantics { stateDescription = if (included) "Incluída na comparação" else "Oculta pela comparação" },
                            ) {
                                Text(
                                    if (included) "ATIVA" else "OCULTA",
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (included) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
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
                                    icon = Icons.Default.Edit,
                                    contentDescription = "Ações do áudio",
                                    enabled = controlsEnabled,
                                    onClick = { clipMenuExpanded = true },
                                )
                                DropdownMenu(expanded = clipMenuExpanded, onDismissRequest = { clipMenuExpanded = false }) {
                                    clips.sortedBy { it.startFrame }.forEach { clip ->
                                        val suffix = if (clips.size > 1) " · ${clip.name}" else ""
                                        ClipMenuItem(Icons.Default.ContentCopy, "Duplicar$suffix") { clipMenuExpanded = false; onDuplicate(clip.id) }
                                        ClipMenuItem(Icons.Default.CallSplit, "Dividir no cursor$suffix") { clipMenuExpanded = false; onSplit(clip.id) }
                                        if (clip.sourceChannelCount == 2) {
                                            ClipMenuItem(Icons.Default.CallSplit, "Separar estéreo em 2 pistas mono$suffix") { clipMenuExpanded = false; onSplitStereo(clip.id) }
                                        }
                                        ClipMenuItem(Icons.Default.Edit, "Fades…$suffix") { clipMenuExpanded = false; onOpenFades(clip.id) }
                                        ClipMenuItem(Icons.Default.CallSplit, "Crossfade com próximo$suffix") { clipMenuExpanded = false; onCrossfade(clip.id) }
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
                            enabled = controlsEnabled,
                            onClick = onSettings,
                        )
                    }
                }
            }
        }

        BoxWithConstraints(
            modifier = Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.surface.copy(alpha = 0.70f)),
        ) {
            if (clips.isEmpty() && livePeaks.isEmpty()) {
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
                        channelPeaks = waveformChannels[clip.id],
                        trackColor = trackColor,
                        trimming = activeTrimClipId == clip.id,
                        trimControls = trimControls?.takeIf { it.clipId == clip.id },
                        sampleRate = sampleRate,
                        onTrimStartFrameChanged = onTrimStartFrameChanged,
                        onTrimEndFrameChanged = onTrimEndFrameChanged,
                        canEdit = canEditClip,
                        dragging = draggingClipId == clip.id,
                        onStartDrag = { origin, touch, size -> onStartClipDrag(clip, trackColor, waveforms[clip.id], origin, touch, size) },
                        onDrag = onDrag,
                        onDragEnd = onDragEnd,
                        onDragCancel = onDragCancel,
                        modifier = Modifier.offset(x = x).width(clipWidth).fillMaxHeight().padding(vertical = 5.dp).align(Alignment.CenterStart),
                    )
                }
            }
            if (livePeaks.isNotEmpty()) {
                val startFraction = (liveStartFrame.toDouble() / projectEndFrame).coerceIn(0.0, 1.0)
                val endFraction = ((liveStartFrame + liveFrames).toDouble() / projectEndFrame).coerceIn(startFraction, 1.0)
                val x = maxWidth * startFraction.toFloat()
                val width = (maxWidth * (endFraction - startFraction).toFloat()).coerceAtLeast(24.dp).coerceAtMost((maxWidth - x).coerceAtLeast(1.dp))
                Surface(
                    modifier = Modifier.offset(x = x).width(width).fillMaxHeight().padding(vertical = 5.dp).align(Alignment.CenterStart).testTag("live-recording-waveform"),
                    shape = RoundedCornerShape(7.dp),
                    color = trackColor.copy(alpha = 0.24f),
                    border = BorderStroke(1.dp, StudioRecord),
                ) { WaveformMini(peaks = livePeaks, color = StudioRecord, modifier = Modifier.fillMaxSize().padding(4.dp)) }
            }
        }
    }
}

@Composable
private fun TimelineClipCard(
    clip: AudioClip,
    peaks: List<Float>?,
    channelPeaks: List<List<Float>>?,
    trackColor: Color,
    trimming: Boolean,
    trimControls: TrimControlState?,
    sampleRate: Int,
    onTrimStartFrameChanged: (Long) -> Unit,
    onTrimEndFrameChanged: (Long) -> Unit,
    canEdit: Boolean,
    dragging: Boolean,
    onStartDrag: (Offset, Offset, IntSize) -> Unit,
    onDrag: (Offset) -> Unit,
    onDragEnd: () -> Unit,
    onDragCancel: () -> Unit,
    modifier: Modifier,
) {
    var clipOrigin by remember(clip.id) { mutableStateOf(Offset.Zero) }
    var clipMeasuredSize by remember(clip.id) { mutableStateOf(IntSize.Zero) }
    val currentClipOrigin by rememberUpdatedState(clipOrigin)
    val currentClipSize by rememberUpdatedState(clipMeasuredSize)

    Surface(
        modifier = modifier
            .alpha(if (dragging) 0.22f else 1f)
            .onGloballyPositioned { coordinates ->
                clipOrigin = coordinates.positionInWindow()
                clipMeasuredSize = coordinates.size
            }
            .pointerInput(clip.id, canEdit, trimming) {
                if (canEdit && !trimming) detectDragGesturesAfterLongPress(
                    onDragStart = { touch -> onStartDrag(currentClipOrigin, touch, currentClipSize) },
                    onDragCancel = onDragCancel,
                    onDragEnd = onDragEnd,
                    onDrag = { change, amount ->
                        change.consume()
                        onDrag(amount)
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
                        channelPeaks = channelPeaks,
                        trackColor = trackColor,
                        sampleRate = sampleRate,
                        onStartChanged = onTrimStartFrameChanged,
                        onEndChanged = onTrimEndFrameChanged,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    if (channelPeaks?.size == 2) StereoWaveformMini(channelPeaks, clip.muted, trackColor, Modifier.fillMaxSize())
                    else WaveformMini(peaks = it, muted = clip.muted, color = trackColor, modifier = Modifier.fillMaxSize())
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
    channelPeaks: List<List<Float>>?,
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
                topLeft = Offset(size.width * startFraction, 0f),
                size = androidx.compose.ui.geometry.Size(size.width * (endFraction - startFraction), size.height),
            )
        }
        if (channelPeaks?.size == 2) StereoWaveformMini(channelPeaks, false, trackColor, Modifier.fillMaxSize())
        else WaveformMini(peaks = peaks, color = trackColor, modifier = Modifier.fillMaxSize())
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
    sampleRate: Int,
    takes: List<studio.guitarlab.core.model.RecordingTake>,
    levelAnalysis: studio.guitarlab.core.project.LevelAnalysis?,
    onActivateTake: (String) -> Unit,
    onAnalyzeLevel: () -> Unit,
    onApplyLevel: () -> Unit,
    onDismiss: () -> Unit,
    onSave: (String, Int) -> Unit,
    onDelete: () -> Unit,
) {
    val hasClips = clips.isNotEmpty()
    var name by remember(track.id, track.name) { mutableStateOf(track.name) }
    var colorIndex by remember(track.id, track.colorIndex) {
        mutableIntStateOf(if (track.colorIndex in StudioTrackPalette.indices) track.colorIndex else track.order % StudioTrackPalette.size)
    }
    val validName = name.trim().length in 1..24
    val scrollState = rememberScrollState()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("Configurar pista")
                Text(
                    "Identidade e informações da faixa",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().heightIn(max = 560.dp).verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                    val wide = maxWidth >= 520.dp
                    if (wide) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            TrackNameField(
                                name = name,
                                onNameChange = { if (it.length <= 24) name = it },
                                modifier = Modifier.weight(1.35f),
                            )
                            TrackRoleCard(track = track, modifier = Modifier.weight(0.85f))
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            TrackNameField(
                                name = name,
                                onNameChange = { if (it.length <= 24) name = it },
                                modifier = Modifier.fillMaxWidth(),
                            )
                            TrackRoleCard(track = track, modifier = Modifier.fillMaxWidth())
                        }
                    }
                }

                TrackColorSection(
                    colorIndex = colorIndex,
                    onColorChange = { colorIndex = it },
                    modifier = Modifier.fillMaxWidth(),
                )

                TrackSourceMetadata(
                    clips = clips,
                    sampleRate = sampleRate,
                    modifier = Modifier.fillMaxWidth(),
                )

                Surface(modifier=Modifier.fillMaxWidth(), shape=RoundedCornerShape(9.dp), color=MaterialTheme.colorScheme.surfaceVariant.copy(alpha=0.22f)) {
                    Column(Modifier.padding(10.dp), verticalArrangement=Arrangement.spacedBy(6.dp)) {
                        Text("Nível assistido", style=MaterialTheme.typography.titleSmall)
                        if (levelAnalysis == null) {
                            Text("Analisa o áudio real e sugere ganho com alvo RMS de −18 dBFS e pico máximo de −3 dBFS. Nada é aplicado automaticamente.", style=MaterialTheme.typography.bodySmall)
                            OutlinedButton(onClick=onAnalyzeLevel, enabled=hasClips) { Text("Analisar pista") }
                        } else {
                            Text("Pico ${"%.1f".format(levelAnalysis.peakDbfs)} dBFS · RMS ${"%.1f".format(levelAnalysis.rmsDbfs)} dBFS · ajuste ${"%+.1f".format(levelAnalysis.recommendedGainDb)} dB", style=MaterialTheme.typography.bodySmall)
                            Button(onClick=onApplyLevel, enabled=!levelAnalysis.silent) { Text("Aplicar sugestão") }
                        }
                    }
                }

                if (takes.isNotEmpty()) {
                    Surface(modifier=Modifier.fillMaxWidth(), shape=RoundedCornerShape(9.dp), color=MaterialTheme.colorScheme.surfaceVariant.copy(alpha=0.22f)) {
                        Column(Modifier.padding(10.dp), verticalArrangement=Arrangement.spacedBy(5.dp)) {
                            Text("Takes", style=MaterialTheme.typography.titleSmall)
                            takes.sortedByDescending { it.createdAtEpochMs }.forEach { take ->
                                if (take.active) Button(onClick={}, enabled=false, modifier=Modifier.fillMaxWidth()) { Text("Ativo · ${take.name}") }
                                else OutlinedButton(onClick={onActivateTake(take.id)}, modifier=Modifier.fillMaxWidth()) { Text("Usar ${take.name}") }
                            }
                        }
                    }
                }

                if (hasClips) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.26f),
                    ) {
                        Text(
                            "Para excluir esta pista, use primeiro “Limpar pista” no menu de áudio. A pista só pode ser excluída quando estiver vazia.",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        },
        dismissButton = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedButton(
                    onClick = onDelete,
                    enabled = !hasClips,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null)
                    Text("Excluir pista", modifier = Modifier.padding(start = 6.dp))
                }
                TextButton(onClick = onDismiss) { Text("Cancelar") }
            }
        },
        confirmButton = {
            Button(onClick = { onSave(name.trim(), colorIndex) }, enabled = validName) { Text("Salvar") }
        },
    )
}

@Composable
private fun TrackNameField(
    name: String,
    onNameChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = name,
        onValueChange = onNameChange,
        singleLine = true,
        label = { Text("Nome da pista") },
        supportingText = { Text("1–24 caracteres") },
        isError = name.trim().length !in 1..24,
        modifier = modifier,
    )
}

@Composable
private fun TrackRoleCard(track: AudioTrack, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.heightIn(min = 62.dp),
        shape = RoundedCornerShape(9.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.30f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.30f)),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                "Função",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                roleName(track.roleId),
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
            )
        }
    }
}

@Composable
private fun TrackColorSection(
    colorIndex: Int,
    onColorChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(9.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.22f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.26f)),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("Cor da pista", style = MaterialTheme.typography.titleSmall)
                Text(
                    "Selecione uma identidade visual",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val colorsPerRow = if (maxWidth >= 520.dp) 10 else 5
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    StudioTrackPalette.chunked(colorsPerRow).forEachIndexed { rowIndex, colors ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            colors.forEachIndexed { columnIndex, color ->
                                val index = rowIndex * colorsPerRow + columnIndex
                                Surface(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .semantics {
                                            contentDescription = "Cor da pista ${index + 1}"
                                            selected = colorIndex == index
                                            role = Role.RadioButton
                                        }
                                        .clickable { onColorChange(index) },
                                    shape = CircleShape,
                                    color = color,
                                    border = BorderStroke(
                                        if (colorIndex == index) 3.dp else 1.dp,
                                        if (colorIndex == index) MaterialTheme.colorScheme.onSurface else color.copy(alpha = 0.55f),
                                    ),
                                ) {}
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TrackSourceMetadata(
    clips: List<AudioClip>,
    sampleRate: Int,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Áudio fonte", style = MaterialTheme.typography.titleSmall)
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(9.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.30f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.30f)),
        ) {
            if (clips.isEmpty()) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Nenhum áudio nesta pista", style = MaterialTheme.typography.bodyMedium)
                    Text("Importe ou grave um take para exibir os metadados da fonte.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    clips.sortedBy { it.startFrame }.forEach { clip ->
                        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            MetadataLine("Arquivo", clip.name)
                            MetadataLine("Formato", clip.sourceFormat ?: "—")
                            MetadataLine("Sample rate", clip.sourceSampleRateHz?.let { "$it Hz" } ?: "—")
                            MetadataLine("Canais", clip.sourceChannelCount?.toString() ?: "—")
                            MetadataLine("Bit depth", clip.sourceBitsPerSample?.let { "$it-bit" } ?: "—")
                            MetadataLine("Encoding", clip.sourceEncoding ?: "—")
                            val durationFrames = clip.sourceTotalFrames ?: clip.lengthFrames
                            val durationRate = clip.sourceSampleRateHz ?: sampleRate
                            MetadataLine("Duração", formatPreciseTime(durationFrames, durationRate))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MetadataLine(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, modifier = Modifier.width(88.dp), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, maxLines = 2)
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


@Composable
private fun ClipFadeDialog(
    clip: AudioClip,
    sampleRateHz: Int,
    onDismiss: () -> Unit,
    onApply: (Long, Long) -> Unit,
) {
    fun framesToMs(frames: Long): Float = frames * 1000f / sampleRateHz.coerceAtLeast(1)
    fun msToFrames(ms: Float): Long = (ms * sampleRateHz / 1000f).toLong().coerceAtLeast(0L)
    val maxMs = (clip.lengthFrames * 1000f / sampleRateHz.coerceAtLeast(1)).coerceAtMost(5000f).coerceAtLeast(10f)
    var fadeInMs by remember(clip.id) { mutableStateOf(framesToMs(clip.fadeInFrames).coerceIn(0f, maxMs)) }
    var fadeOutMs by remember(clip.id) { mutableStateOf(framesToMs(clip.fadeOutFrames).coerceIn(0f, maxMs)) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Fades do clipe") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(clip.name, style = MaterialTheme.typography.bodyMedium)
                Text("Fade in · ${fadeInMs.toInt()} ms")
                androidx.compose.material3.Slider(value = fadeInMs, onValueChange = { fadeInMs = it }, valueRange = 0f..maxMs)
                Text("Fade out · ${fadeOutMs.toInt()} ms")
                androidx.compose.material3.Slider(value = fadeOutMs, onValueChange = { fadeOutMs = it }, valueRange = 0f..maxMs)
                Text("A edição é não destrutiva e usa a mesma curva na reprodução e na exportação.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        confirmButton = { Button(onClick = { onApply(msToFrames(fadeInMs), msToFrames(fadeOutMs)) }) { Text("Aplicar") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
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
