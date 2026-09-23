package studio.guitarlab.app.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.BoxWithConstraintsScope
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
import androidx.compose.foundation.layout.wrapContentWidth
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
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay
import studio.guitarlab.app.ui.theme.StudioComparisonActive
import studio.guitarlab.app.ui.theme.StudioComparisonHidden
import studio.guitarlab.app.ui.theme.StudioLoop
import studio.guitarlab.app.ui.theme.StudioPlayhead
import studio.guitarlab.app.ui.theme.StudioRecord
import studio.guitarlab.app.ui.theme.StudioTrim
import studio.guitarlab.core.model.AudioClip
import studio.guitarlab.core.model.AudioTrack
import studio.guitarlab.core.model.BuiltInRoles
import studio.guitarlab.core.model.GuitarProject
import studio.guitarlab.core.model.TrackRoleDefinition
import studio.guitarlab.core.project.RecordingSessionPhase
import studio.guitarlab.core.project.ActiveTakePolicy
import studio.guitarlab.core.project.LiveWaveformPoint
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
import studio.guitarlab.core.project.TrackRoleAssignmentPolicy

private val TrackSidebarWidth = 224.dp
private val TrackLaneGap = 6.dp
private val TrackLaneHeight = 96.dp
private val ClipTrashWidth = 168.dp
private val ClipTrashHeight = 68.dp
private val ClipTrashMargin = 16.dp

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
    val deleteTargetHovered: Boolean = false,
    val visibleLanes: List<TimelineDragPolicy.LaneBounds> = emptyList(),
)

/**
 * Ephemeral layout measurements used only by drag gestures.
 *
 * These values deliberately are not Compose snapshot state. Updating geometry from
 * onGloballyPositioned must not invalidate composition, otherwise measurement can create
 * a layout -> state write -> recomposition feedback loop and keep Compose permanently busy.
 */
private class DragLayoutGeometry(
    var origin: Offset = Offset.Zero,
    var size: IntSize = IntSize.Zero,
)

@Composable
fun StudioPlaceholderScreen(
    viewModel: StudioViewModel = viewModel(),
    selectedTrackId: String? = null,
    onSelectTrack: (String) -> Unit = {},
    showPracticeControls: Boolean = true,
    onOpenLevelAnalysis: () -> Unit = {},
) {
    val state by viewModel.state.collectAsState()
    var pendingTrackId by remember { mutableStateOf<String?>(null) }
    var settingsTrackId by remember { mutableStateOf<String?>(null) }
    var focusTakeManagement by remember { mutableStateOf(false) }
    var fadeClipId by remember { mutableStateOf<String?>(null) }
    var pendingDiscardRecoveryId by remember { mutableStateOf<String?>(null) }
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
                liveRecordingWaveform = state.liveRecordingWaveform,
                recordingTrackId = state.recordingSession.targetTrackId,
                recordingStartFrame = state.recordingSession.timelineStartFrame,
                recordingFrames = state.recordingSession.framesCaptured,
                recordingPeak = state.recordingPeak,
                recordingRms = state.recordingRms,
                auditionMode = state.guitarAuditionMode,
                sectionSuggestions = state.sectionSuggestions,
                showPracticeControls = showPracticeControls,
                selectedTrackId = selectedTrackId,
                onSelectTrack = onSelectTrack,
                onOpenTrackSettings = { settingsTrackId = it },
                onOpenTakeManagement = {
                    focusTakeManagement = true
                    settingsTrackId = it
                },
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
                onClearSections = viewModel::clearSections,
                onLoopSection = viewModel::loopSection,
                onRemoveMarker = viewModel::removeMarker,
                onRemoveSection = viewModel::removeSection,
                onOpenLevelAnalysis = onOpenLevelAnalysis,
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
            focusTakeManagement = focusTakeManagement,
            levelAnalysis = state.trackLevelAnalysis[settingsTrack.id],
            availableRoles = TrackRoleAssignmentPolicy.availableForTrack(project, settingsTrack.id),
            onActivateTake = viewModel::activateTake,
            onAuditionTake = viewModel::auditionTake,
            onUpdateTakeMetadata = viewModel::updateTakeMetadata,
            onToggleTakeFavorite = viewModel::toggleTakeFavorite,
            onDeleteTake = viewModel::deleteTake,
            onAnalyzeLevel = { viewModel.analyzeTrackLevel(settingsTrack.id) },
            onApplyLevel = { viewModel.applyTrackLevelSuggestion(settingsTrack.id) },
            onClearTrack = { viewModel.clearTrackContents(settingsTrack.id) },
            onDismiss = { settingsTrackId = null; focusTakeManagement = false },
            onSave = { name, colorIndex, roleId ->
                viewModel.updateTrackConfiguration(settingsTrack.id, name, colorIndex, roleId)
                settingsTrackId = null
                focusTakeManagement = false
            },
            onDelete = {
                viewModel.deleteTrack(settingsTrack.id)
                settingsTrackId = null
                focusTakeManagement = false
            },
        )
    }

    state.newTrackRolePromptId?.let { trackId ->
        val promptProject = state.project
        val promptTrack = promptProject?.tracks?.firstOrNull { it.id == trackId }
        if (promptProject != null && promptTrack != null) {
            val suggestions = TrackRoleAssignmentPolicy.availableSuggestionsForNewTrack(promptProject, trackId)
            if (suggestions.isNotEmpty()) {
                NewTrackRolePromptDialog(
                    trackName = promptTrack.name,
                    suggestions = suggestions,
                    onChoose = { roleId -> viewModel.assignNewTrackRole(trackId, roleId) },
                    onKeepGeneric = viewModel::dismissNewTrackRolePrompt,
                )
            } else {
                LaunchedEffect(trackId) { viewModel.dismissNewTrackRolePrompt() }
            }
        }
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

    state.recoveryItems.firstOrNull()?.let { recovery ->
        val duration = recovery.approximateDurationSeconds?.let { seconds ->
            val total = seconds.toLong().coerceAtLeast(0L)
            "%d:%02d".format(total / 60L, total % 60L)
        } ?: "duração desconhecida"
        AlertDialog(
            onDismissRequest = { if (!state.recoveryBusy) viewModel.postponeInterruptedRecording(recovery.transactionId) },
            title = { Text("Gravação interrompida encontrada") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("O GuitarLab preservou áudio capturado antes da interrupção.")
                    Text("${recovery.trackName ?: "Pista original não disponível"} • $duration")
                    recovery.diagnosticReason?.let { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    if (!recovery.recoverable) {
                        Text("O áudio será mantido sem alterações até você decidir o que fazer.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (recovery.safePlayable) {
                        TextButton(enabled = !state.recoveryBusy, onClick = { viewModel.previewInterruptedRecording(recovery.transactionId) }) { Text("Ouvir") }
                    }
                    Button(
                        enabled = recovery.recoverable && !state.recoveryBusy,
                        onClick = { viewModel.recoverInterruptedRecording(recovery.transactionId) },
                    ) { Text(if (state.recoveryBusy) "Recuperando…" else "Recuperar") }
                }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(enabled = !state.recoveryBusy, onClick = { pendingDiscardRecoveryId = recovery.transactionId }) { Text("Descartar") }
                    TextButton(enabled = !state.recoveryBusy, onClick = { viewModel.postponeInterruptedRecording(recovery.transactionId) }) { Text("Depois") }
                }
            },
        )
    }

    pendingDiscardRecoveryId?.let { transactionId ->
        AlertDialog(
            onDismissRequest = { pendingDiscardRecoveryId = null },
            title = { Text("Descartar gravação preservada?") },
            text = { Text("Esta ação apaga definitivamente o áudio interrompido que ainda não foi publicado no projeto.") },
            confirmButton = {
                Button(onClick = {
                    viewModel.discardInterruptedRecording(transactionId)
                    pendingDiscardRecoveryId = null
                }) { Text("Descartar") }
            },
            dismissButton = { TextButton(onClick = { pendingDiscardRecoveryId = null }) { Text("Cancelar") } },
        )
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
                    Text(if (prompt.hasGuitarPair) "Distribuir L/R" else "Separar em 2 canais mono")
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::keepStereoImport) { Text("Manter estéreo") }
            },
        )
    }
}

private val AutoSectionsSlotWidth = 124.dp
private val AutoSectionsDiscardWidth = 36.dp
private val PracticeControlsWideBreakpoint = 920.dp
private val DockedPracticeAdaptiveSingleRowBreakpoint = 720.dp
private const val ComparisonPracticeWeight = 0.34f
private const val AdjustmentsPracticeWeight = 0.16f
private const val TimelinePracticeWeight = 0.50f
private val TimelineRulerHeight = 20.dp

@Composable
internal fun PracticeControls(
    project: GuitarProject,
    auditionMode: GuitarAuditionMode,
    suggestions: Int,
    enabled: Boolean,
    loopEnabled: Boolean,
    onAuditionMode: (GuitarAuditionMode) -> Unit,
    onAddMarker: () -> Unit,
    onAddSection: () -> Unit,
    onSuggestSections: () -> Unit,
    onAcceptSections: () -> Unit,
    onDiscardSections: () -> Unit,
    onClearSections: () -> Unit,
    onLoopSection: (String) -> Unit,
    onRemoveMarker: (String) -> Unit,
    onRemoveSection: (String) -> Unit,
    onOpenLevelAnalysis: () -> Unit = {},
    compact: Boolean = false,
    docked: Boolean = false,
    modifier: Modifier = Modifier,
) {
    var confirmClearSections by remember { mutableStateOf(false) }
    val buttonShape = RoundedCornerShape(if (docked) 4.dp else 6.dp)

    val comparisonContent: @Composable RowScope.() -> Unit = {
        GuitarAuditionMode.entries.forEach { mode ->
            val label = when(mode) { GuitarAuditionMode.MIXER -> "Desativado"; GuitarAuditionMode.REFERENCE -> "Referência"; GuitarAuditionMode.MY_GUITAR -> "Minha"; GuitarAuditionMode.BOTH -> "Ambas" }
            if (mode == auditionMode) {
                Button(
                    onClick = { onAuditionMode(mode) },
                    shape = buttonShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary,
                        contentColor = MaterialTheme.colorScheme.onSecondary,
                    ),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                    modifier = Modifier.semantics { selected = true },
                ) { Text(label) }
            } else {
                OutlinedButton(
                    onClick = { onAuditionMode(mode) },
                    shape = buttonShape,
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.14f),
                        contentColor = MaterialTheme.colorScheme.secondary,
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.55f)),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                ) { Text(label) }
            }
        }
    }
    val adjustmentsContent: @Composable RowScope.() -> Unit = {
        OutlinedButton(
            onClick = onOpenLevelAnalysis,
            enabled = enabled,
            shape = buttonShape,
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                contentColor = MaterialTheme.colorScheme.primary,
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.58f)),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
            modifier = Modifier.testTag("open-all-level-analysis"),
        ) { Text("Níveis") }
    }
    val timelineContent: @Composable RowScope.() -> Unit = {
        OutlinedButton(
            onClick = onAddMarker,
            enabled = enabled,
            shape = buttonShape,
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.14f),
                contentColor = MaterialTheme.colorScheme.tertiary,
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.52f)),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
        ) { Text("+ Marcador") }
        OutlinedButton(
            onClick = onAddSection,
            enabled = enabled && loopEnabled,
            shape = buttonShape,
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.14f),
                contentColor = MaterialTheme.colorScheme.tertiary,
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.52f)),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
        ) { Text("Criar seção do loop") }
        AutoSectionsSlot(
            previewVisible = suggestions > 0,
            enabled = enabled,
            onDetect = onSuggestSections,
            onApply = onAcceptSections,
            onDiscard = onDiscardSections,
            buttonShape = buttonShape,
        )
        OutlinedButton(
            onClick = { confirmClearSections = true },
            enabled = enabled && (project.sections.isNotEmpty() || suggestions > 0),
            shape = buttonShape,
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.14f),
                contentColor = MaterialTheme.colorScheme.tertiary,
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.52f)),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
        ) { Text("Limpar seções") }
    }

    if (docked) {
        SegmentedPracticeBar(
            modifier = modifier.fillMaxWidth(),
            comparisonContent = comparisonContent,
            adjustmentsContent = adjustmentsContent,
            timelineContent = timelineContent,
        )
    } else {
        BoxWithConstraints(modifier.fillMaxWidth()) {
            when {
                compact -> {
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("Comparação", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        comparisonContent()
                        Text("•", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.outline)
                        Text("Ajustes", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        adjustmentsContent()
                        Text("•", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.outline)
                        Text("Timeline", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        timelineContent()
                    }
                }
                maxWidth >= PracticeControlsWideBreakpoint -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        PracticeControlGroup("Comparação", Modifier.weight(ComparisonPracticeWeight), comparisonContent)
                        PracticeControlGroup("Ajustes", Modifier.weight(AdjustmentsPracticeWeight), adjustmentsContent)
                        PracticeControlGroup("Timeline", Modifier.weight(TimelinePracticeWeight), timelineContent)
                    }
                }
                else -> {
                    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        PracticeControlGroup("Comparação", Modifier.fillMaxWidth(), comparisonContent, horizontalScroll = true)
                        PracticeControlGroup("Ajustes", Modifier.fillMaxWidth(), adjustmentsContent, horizontalScroll = true)
                        PracticeControlGroup("Timeline", Modifier.fillMaxWidth(), timelineContent, horizontalScroll = true)
                    }
                }
            }
        }
    }

    if (confirmClearSections) {
        AlertDialog(
            onDismissRequest = { confirmClearSections = false },
            title = { Text("Limpar todas as seções?") },
            text = { Text("As seções salvas e qualquer prévia de detecção serão removidas. Marcadores, clipes e o intervalo de loop não serão alterados.") },
            confirmButton = {
                Button(
                    onClick = {
                        confirmClearSections = false
                        onClearSections()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    shape = buttonShape,
                ) { Text("Limpar seções") }
            },
            dismissButton = { TextButton(onClick = { confirmClearSections = false }) { Text("Cancelar") } },
        )
    }
}

@Composable
private fun SegmentedPracticeBar(
    modifier: Modifier = Modifier,
    comparisonContent: @Composable RowScope.() -> Unit,
    adjustmentsContent: @Composable RowScope.() -> Unit,
    timelineContent: @Composable RowScope.() -> Unit,
) {
    Surface(
        modifier = modifier.testTag("mixer-practice-segmented-bar"),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.58f)),
    ) {
        BoxWithConstraints(Modifier.fillMaxWidth().padding(3.dp)) {
            if (maxWidth >= DockedPracticeAdaptiveSingleRowBreakpoint) {
                // Hardware-style groups stay visually independent. Comparison and Adjustments
                // measure to their real content; Timeline owns the flexible remainder/overflow.
                Row(
                    Modifier.fillMaxWidth().height(48.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    SegmentedPracticeSection(
                        title = "Comparação",
                        accent = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.wrapContentWidth().testTag("practice-comparison-segment"),
                        content = comparisonContent,
                    )
                    SegmentedPracticeSection(
                        title = "Ajustes",
                        accent = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.wrapContentWidth().testTag("practice-adjustments-segment"),
                        centerContent = true,
                        content = adjustmentsContent,
                    )
                    SegmentedPracticeSection(
                        title = "Timeline",
                        accent = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.weight(1f).testTag("practice-timeline-segment"),
                        horizontalScroll = true,
                        content = timelineContent,
                    )
                }
            } else {
                // Narrow viewports keep every semantic group discoverable. Each long action strip
                // scrolls internally while the title/header stays fixed and visually identifiable.
                Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    SegmentedPracticeSection(
                        title = "Comparação",
                        accent = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("practice-comparison-segment"),
                        horizontalScroll = true,
                        content = comparisonContent,
                    )
                    SegmentedPracticeSection(
                        title = "Ajustes",
                        accent = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("practice-adjustments-segment"),
                        centerContent = true,
                        content = adjustmentsContent,
                    )
                    SegmentedPracticeSection(
                        title = "Timeline",
                        accent = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("practice-timeline-segment"),
                        horizontalScroll = true,
                        content = timelineContent,
                    )
                }
            }
        }
    }
}

@Composable
private fun SegmentedPracticeSection(
    title: String,
    accent: Color,
    modifier: Modifier = Modifier,
    centerContent: Boolean = false,
    horizontalScroll: Boolean = false,
    content: @Composable RowScope.() -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxHeight(),
        shape = RoundedCornerShape(6.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.34f),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.42f)),
        tonalElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier.fillMaxHeight(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(horizontal = 4.dp)
                    .testTag("practice-${title.lowercase()}-title"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Box(
                    Modifier
                        .width(3.dp)
                        .height(20.dp)
                        .background(accent, RoundedCornerShape(2.dp)),
                )
                Text(
                    title,
                    style = MaterialTheme.typography.labelMedium,
                    color = accent,
                )
            }
            Box(
                Modifier
                    .width(1.dp)
                    .height(30.dp)
                    .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.50f)),
            )
            val actionsModifier = Modifier
                .fillMaxHeight()
                .padding(horizontal = if (centerContent) 10.dp else 6.dp, vertical = 4.dp)
            Row(
                modifier = if (horizontalScroll) actionsModifier.horizontalScroll(rememberScrollState()) else actionsModifier,
                horizontalArrangement = if (centerContent) {
                    Arrangement.spacedBy(5.dp, Alignment.CenterHorizontally)
                } else {
                    Arrangement.spacedBy(5.dp)
                },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                content()
            }
        }
    }
}


@Composable
internal fun AutoSectionsSlot(
    previewVisible: Boolean,
    enabled: Boolean,
    onDetect: () -> Unit,
    onApply: () -> Unit,
    onDiscard: () -> Unit,
    buttonShape: RoundedCornerShape = RoundedCornerShape(6.dp),
) {
    Box(Modifier.width(AutoSectionsSlotWidth).testTag("auto-sections-slot")) {
        if (previewVisible) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Button(
                    onClick = onApply,
                    enabled = enabled,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.tertiary,
                        contentColor = MaterialTheme.colorScheme.onTertiary,
                    ),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    shape = buttonShape,
                    modifier = Modifier.weight(1f).testTag("auto-sections-apply"),
                ) { Text("Aplicar") }
                TextButton(
                    onClick = onDiscard,
                    enabled = enabled,
                    contentPadding = PaddingValues(0.dp),
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier
                        .width(AutoSectionsDiscardWidth)
                        .semantics { contentDescription = "Descartar prévia de seções" }
                        .testTag("auto-sections-discard"),
                ) { Text("X", style = MaterialTheme.typography.titleMedium) }
            }
        } else {
            OutlinedButton(
                onClick = onDetect,
                enabled = enabled,
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.14f),
                    contentColor = MaterialTheme.colorScheme.tertiary,
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.62f)),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                shape = buttonShape,
                modifier = Modifier.fillMaxWidth().testTag("auto-sections-detect"),
            ) { Text("Auto seções") }
        }
    }
}

@Composable
private fun PracticeControlGroup(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
    horizontalScroll: Boolean = false,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.34f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
    ) {
        val rowModifier = if (horizontalScroll) {
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 8.dp, vertical = 4.dp)
        } else {
            Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp)
        }
        Row(
            modifier = rowModifier,
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
    liveRecordingWaveform: List<LiveWaveformPoint>,
    recordingTrackId: String?,
    recordingStartFrame: Long,
    recordingFrames: Long,
    recordingPeak: Float,
    recordingRms: Float,
    auditionMode: GuitarAuditionMode,
    sectionSuggestions: List<studio.guitarlab.core.project.SectionBoundarySuggestion>,
    showPracticeControls: Boolean,
    selectedTrackId: String?,
    onSelectTrack: (String) -> Unit,
    onOpenTrackSettings: (String) -> Unit,
    onOpenTakeManagement: (String) -> Unit,
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
    onClearSections: () -> Unit,
    onLoopSection: (String) -> Unit,
    onRemoveMarker: (String) -> Unit,
    onRemoveSection: (String) -> Unit,
    onOpenLevelAnalysis: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val baseProjectEndFrame = TimelineControlPolicy.projectEndFrame(project)
    val trimClip = trimControls?.let { state -> project.clips.firstOrNull { it.id == state.clipId } }
    val liveEndFrame = if (recordingPhase == RecordingSessionPhase.CAPTURING) recordingStartFrame + recordingFrames else 0L
    val projectEndFrame = maxOf(baseProjectEndFrame, trimClip?.let(TrimControlPolicy::maximumEndFrame) ?: 0L, liveEndFrame).coerceAtLeast(1L)
    val timelineEditingEnabled = TransportPolicy.timelineEditingEnabled(transport)
    val trimActive = trimControls != null
    val clipEditingEnabled = !importing && !editingClip && timelineEditingEnabled && !trimActive
    var pendingDeleteClipId by remember(project.id) { mutableStateOf<String?>(null) }
    val pendingDeleteClip = pendingDeleteClipId?.let { id -> project.clips.firstOrNull { it.id == id } }

    Box(modifier = modifier) {
        Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Surface(
            modifier = Modifier.fillMaxWidth().weight(1f),
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.20f),
            tonalElevation = 0.dp,
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(0.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(TrackLaneGap),
                    ) {
                        Surface(
                            modifier = Modifier.width(TrackSidebarWidth).height(TimelineMarkerRailHeight),
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
                            sectionSuggestions = sectionSuggestions,
                            markers = project.markers,
                            enabled = timelineEditingEnabled,
                            playheadDragEnabled = recordingPhase == RecordingSessionPhase.IDLE,
                            onPlayheadFrameChanged = onPlayheadFrameChanged,
                            onLoopStartFrameChanged = onLoopStartFrameChanged,
                            onLoopEndFrameChanged = onLoopEndFrameChanged,
                            onSectionClick = onLoopSection,
                            onSectionRemove = onRemoveSection,
                            onMarkerRemove = onRemoveMarker,
                            modifier = Modifier.weight(1f).testTag("timeline-marker-rail"),
                        )
                    }

                    TimelineRuler(project, projectEndFrame, trimControls)
                }

                val orderedTracks = project.tracks.sortedBy { it.order }
                val orderedIds = orderedTracks.map { it.id }
                val trackListState = rememberLazyListState()
                var dragState by remember { mutableStateOf<WorkspaceDragState?>(null) }
                val workspaceGeometry = remember { DragLayoutGeometry() }
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

                fun clipDeleteBounds(): TimelineDragPolicy.DropBounds {
                    val trashWidthPx = with(density) { ClipTrashWidth.toPx() }
                    val trashHeightPx = with(density) { ClipTrashHeight.toPx() }
                    val marginPx = with(density) { ClipTrashMargin.toPx() }
                    val right = (workspaceGeometry.size.width.toFloat() - marginPx).coerceAtLeast(0f)
                    val bottom = (workspaceGeometry.size.height.toFloat() - marginPx).coerceAtLeast(0f)
                    return TimelineDragPolicy.DropBounds(
                        leftPx = (right - trashWidthPx).coerceAtLeast(0f),
                        topPx = (bottom - trashHeightPx).coerceAtLeast(0f),
                        rightPx = right,
                        bottomPx = bottom,
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
                            visibleLanes = lanes,
                        )
                        WorkspaceDragKind.CLIP -> {
                            val targetTrackId = TimelineDragPolicy.clipTargetTrackId(state.pointer.y, lanes)
                            val intent = TimelineDragPolicy.clipDropIntent(
                                sourceTrackId = state.sourceTrackId,
                                targetTrackId = targetTrackId,
                                pointerXPx = state.pointer.x,
                                pointerYPx = state.pointer.y,
                                deleteBounds = clipDeleteBounds(),
                            )
                            state.copy(
                                targetTrackId = targetTrackId,
                                targetIndex = null,
                                deleteTargetHovered = intent == TimelineDragPolicy.ClipDropIntent.Delete,
                                visibleLanes = lanes,
                            )
                        }
                    }
                }

                fun beginTrackDrag(track: AudioTrack, trackIndex: Int, itemOriginWindow: Offset, touch: Offset, itemSize: IntSize) {
                    if (!clipEditingEnabled) return
                    val origin = itemOriginWindow - workspaceGeometry.origin
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
                    val origin = itemOriginWindow - workspaceGeometry.origin
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
                            when (val intent = TimelineDragPolicy.clipDropIntent(
                                sourceTrackId = current.sourceTrackId,
                                targetTrackId = current.targetTrackId,
                                pointerXPx = current.pointer.x,
                                pointerYPx = current.pointer.y,
                                deleteBounds = clipDeleteBounds(),
                            )) {
                                TimelineDragPolicy.ClipDropIntent.Delete -> pendingDeleteClipId = current.itemId
                                is TimelineDragPolicy.ClipDropIntent.Move -> onMoveClipToTrack(current.itemId, intent.targetTrackId)
                                TimelineDragPolicy.ClipDropIntent.NoOp -> Unit
                            }
                        }
                    }
                }

                LaunchedEffect(dragState?.kind, dragState?.itemId) {
                    while (dragState != null) {
                        val current = dragState ?: break
                        val auto = if (current.kind == WorkspaceDragKind.CLIP && current.deleteTargetHovered) {
                            TimelineDragPolicy.AutoScroll(TimelineDragPolicy.EdgeDirection.NONE, 0f)
                        } else {
                            TimelineDragPolicy.autoScroll(
                                pointerYPx = current.pointer.y,
                                viewportTopPx = 0f,
                                viewportBottomPx = workspaceGeometry.size.height.toFloat(),
                                edgeZonePx = edgeZonePx,
                                maxStepPx = maxScrollStepPx,
                                canScrollBackward = trackListState.canScrollBackward,
                                canScrollForward = trackListState.canScrollForward,
                            )
                        }
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
                            workspaceGeometry.origin = coordinates.positionInWindow()
                            workspaceGeometry.size = coordinates.size
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
                                hasTakes = project.takes.any { it.trackId == track.id },
                                liveWaveform = if (recordingTrackId == track.id && recordingPhase == RecordingSessionPhase.CAPTURING) liveRecordingWaveform else emptyList(),
                                liveStartFrame = recordingStartFrame,
                                liveFrames = recordingFrames,
                                livePeak = if (recordingTrackId == track.id && recordingPhase == RecordingSessionPhase.CAPTURING) recordingPeak else 0f,
                                liveRms = if (recordingTrackId == track.id && recordingPhase == RecordingSessionPhase.CAPTURING) recordingRms else 0f,
                                waveforms = waveforms,
                                waveformChannels = waveformChannels,
                                projectEndFrame = projectEndFrame,
                                selected = track.id == selectedTrackId,
                                canImport = clipEditingEnabled && dragState == null,
                                canEditClip = clipEditingEnabled,
                                activeTrimClipId = trimControls?.clipId,
                                trimControls = trimControls,
                                onTrimStartFrameChanged = onTrimStartFrameChanged,
                                onTrimEndFrameChanged = onTrimEndFrameChanged,
                                onSelect = { onSelectTrack(track.id) },
                                onSettings = { onOpenTrackSettings(track.id) },
                                onManageTakes = { onOpenTakeManagement(track.id) },
                                onImportWav = { onImportWav(track.id) },
                                onBeginTrim = onBeginTrim,
                                onRemove = { pendingDeleteClipId = it },
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

                    dragState?.let { activeDrag ->
                        DragOverlay(
                            drag = activeDrag,
                            orderedTracks = orderedTracks,
                            modifier = Modifier.fillMaxSize().zIndex(20f),
                        )
                    }
                }
            }
        }

        if (showPracticeControls) {
            PracticeControls(
                project = project,
                auditionMode = auditionMode,
                suggestions = sectionSuggestions.size,
                enabled = timelineEditingEnabled && !trimActive,
                loopEnabled = transport.loopEnabled,
                onAuditionMode = onAuditionMode,
                onAddMarker = onAddMarker,
                onAddSection = onAddSection,
                onSuggestSections = onSuggestSections,
                onAcceptSections = onAcceptSections,
                onDiscardSections = onDiscardSections,
                onClearSections = onClearSections,
                onLoopSection = onLoopSection,
                onRemoveMarker = onRemoveMarker,
                onRemoveSection = onRemoveSection,
                onOpenLevelAnalysis = onOpenLevelAnalysis,
            )
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

        pendingDeleteClip?.let { clip ->
            AlertDialog(
                onDismissRequest = { pendingDeleteClipId = null },
                title = { Text("Excluir clipe?") },
                text = { Text("O trecho ‘${clip.name}’ será removido da timeline. O arquivo de origem compartilhado por outros trechos será preservado.") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val id = clip.id
                            pendingDeleteClipId = null
                            onRemoveClip(id)
                        },
                    ) { Text("Excluir", color = MaterialTheme.colorScheme.error) }
                },
                dismissButton = { TextButton(onClick = { pendingDeleteClipId = null }) { Text("Cancelar") } },
            )
        }

        if (recordingPhase == RecordingSessionPhase.COUNTDOWN) {
            RecordingCountdownOverlay(
                seconds = countdownSeconds,
                modifier = Modifier.fillMaxSize().zIndex(50f),
            )
        }
    }
}

@Composable
internal fun RecordingCountdownOverlay(seconds: Int, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.testTag("recording-countdown-overlay"),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            modifier = Modifier
                .size(216.dp)
                .semantics { contentDescription = "Contagem para gravação: ${seconds.coerceAtLeast(1)}" }
                .testTag("recording-countdown-disc"),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.78f),
            tonalElevation = 10.dp,
        ) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = seconds.coerceAtLeast(1).toString(),
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontSize = 132.sp,
                        lineHeight = 132.sp,
                    ),
                    color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.94f),
                )
            }
        }
    }
}

@Composable
private fun DragOverlay(
    drag: WorkspaceDragState,
    orderedTracks: List<AudioTrack>,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val visibleLanes = drag.visibleLanes
    val ghostX = drag.pointer.x - drag.grabOffset.x
    val ghostY = drag.pointer.y - drag.grabOffset.y
    val orderedIds = orderedTracks.map { it.id }

    Box(modifier = modifier) {
        if (drag.kind == WorkspaceDragKind.CLIP) {
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(ClipTrashMargin)
                    .size(width = ClipTrashWidth, height = ClipTrashHeight)
                    .testTag("clip-trash-target"),
                shape = RoundedCornerShape(8.dp),
                color = if (drag.deleteTargetHovered) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
                border = BorderStroke(
                    2.dp,
                    if (drag.deleteTargetHovered) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline.copy(alpha = 0.65f),
                ),
                tonalElevation = 12.dp,
            ) {
                Row(
                    Modifier.fillMaxSize().padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                    Text(
                        if (drag.deleteTargetHovered) "Solte para excluir" else "Arraste aqui para excluir",
                        style = MaterialTheme.typography.labelLarge,
                        color = if (drag.deleteTargetHovered) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }

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
                    shape = RoundedCornerShape(4.dp),
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
        } else if (!drag.deleteTargetHovered) {
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
                    shape = RoundedCornerShape(4.dp),
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
            shape = RoundedCornerShape(8.dp),
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
private fun TimelineRuler(
    project: GuitarProject,
    projectEndFrame: Long,
    trimControls: TrimControlState?,
) {
    val sampleRate = project.sampleRate.fixedHz
        ?: project.clips.firstOrNull { it.sourceSampleRateHz != null }?.sourceSampleRateHz
        ?: 48_000
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(TimelineRulerHeight)
            .zIndex(10f)
            .padding(start = TrackSidebarWidth + TrackLaneGap)
            .testTag("timeline-time-ruler"),
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom,
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

        if (trimControls != null) {
            val startFraction = TimelineControlPolicy.frameToFraction(trimControls.startFrame, projectEndFrame)
            val endFraction = TimelineControlPolicy.frameToFraction(trimControls.endFrame, projectEndFrame)
            TrimRulerMarker(
                fraction = startFraction,
                contentDescription = "Marcador T1 do corte",
                tag = "trim-ruler-start",
            )
            TrimRulerMarker(
                fraction = endFraction,
                contentDescription = "Marcador T2 do corte",
                tag = "trim-ruler-end",
            )
        }
    }
}

@Composable
private fun BoxWithConstraintsScope.TrimRulerMarker(
    fraction: Float,
    contentDescription: String,
    tag: String,
) {
    val markerX = maxWidth * fraction.coerceIn(0f, 1f)
    Box(
        modifier = Modifier
            .offset(x = (markerX - 1.dp).coerceIn(0.dp, (maxWidth - 2.dp).coerceAtLeast(0.dp)))
            .width(2.dp)
            .height(10.dp)
            .background(StudioTrim)
            .zIndex(8f)
            .semantics { this.contentDescription = contentDescription }
            .testTag(tag),
    )
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
    hasTakes: Boolean,
    liveWaveform: List<LiveWaveformPoint>,
    liveStartFrame: Long,
    liveFrames: Long,
    livePeak: Float,
    liveRms: Float,
    waveforms: Map<String, List<Float>>,
    waveformChannels: Map<String, List<List<Float>>>,
    projectEndFrame: Long,
    selected: Boolean,
    canImport: Boolean,
    canEditClip: Boolean,
    activeTrimClipId: String?,
    trimControls: TrimControlState?,
    onTrimStartFrameChanged: (Long) -> Unit,
    onTrimEndFrameChanged: (Long) -> Unit,
    onSelect: () -> Unit,
    onSettings: () -> Unit,
    onManageTakes: () -> Unit,
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
    val trackGeometry = remember(track.id) { DragLayoutGeometry() }
    var clipMenuExpanded by remember(track.id) { mutableStateOf(false) }
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
                    trackGeometry.origin = coordinates.positionInWindow()
                    trackGeometry.size = coordinates.size
                }
                .pointerInput(track.id, canEditClip) {
                    if (canEditClip) detectDragGesturesAfterLongPress(
                        onDragStart = { touch -> onStartTrackDrag(trackGeometry.origin, touch, trackGeometry.size) },
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
            color = when {
                track.armed -> StudioRecord.copy(alpha = if (selected) 0.16f else 0.10f)
                selected -> trackColor.copy(alpha = 0.11f)
                else -> MaterialTheme.colorScheme.surface.copy(alpha = 0.86f)
            },
            border = BorderStroke(
                if (track.armed || selected) 1.5.dp else 1.dp,
                when {
                    track.armed -> StudioRecord
                    selected -> trackColor
                    else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.38f)
                },
            ),
        ) {
            Row(modifier = Modifier.fillMaxSize()) {
                Box(Modifier.width(if (track.armed) 6.dp else 4.dp).fillMaxHeight().background(if (track.armed) StudioRecord else trackColor))
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
                            val comparisonColor = if (included) StudioComparisonActive else StudioComparisonHidden
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = comparisonColor.copy(alpha = 0.18f),
                                border = BorderStroke(1.dp, comparisonColor.copy(alpha = 0.94f)),
                                modifier = Modifier.semantics { stateDescription = if (included) "Incluída na comparação" else "Oculta pela comparação" },
                            ) {
                                Text(
                                    if (included) "ATIVA" else "OCULTA",
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = comparisonColor,
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
                                    if (hasTakes) {
                                        ClipMenuItem(Icons.Default.Edit, "Gerenciar takes e sincronização…") {
                                            clipMenuExpanded = false
                                            onManageTakes()
                                        }
                                    }
                                    clips.sortedBy { it.startFrame }.forEach { clip ->
                                        val suffix = if (clips.size > 1) " · ${clip.name}" else ""
                                        ClipMenuItem(Icons.Default.ContentCopy, "Duplicar$suffix") { clipMenuExpanded = false; onDuplicate(clip.id) }
                                        ClipMenuItem(Icons.Default.CallSplit, "Dividir no cursor$suffix") { clipMenuExpanded = false; onSplit(clip.id) }
                                        if (clip.sourceChannelCount == 2) {
                                            ClipMenuItem(Icons.Default.CallSplit, "Separar estéreo em 2 pistas mono$suffix") { clipMenuExpanded = false; onSplitStereo(clip.id) }
                                        }
                                        ClipMenuItem(Icons.Default.Edit, "Fades…$suffix") { clipMenuExpanded = false; onOpenFades(clip.id) }
                                        ClipMenuItem(Icons.Default.CallSplit, "Crossfade com próximo$suffix") { clipMenuExpanded = false; onCrossfade(clip.id) }
                                        ClipMenuItem(Icons.Default.ContentCut, "Cortar$suffix") {
                                            clipMenuExpanded = false
                                            onBeginTrim(clip.id)
                                        }
                                        ClipMenuItem(Icons.Default.Delete, "Excluir clipe$suffix", destructive = true) { clipMenuExpanded = false; onRemove(clip.id) }
                                    }
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
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .clip(RoundedCornerShape(8.dp))
                .background(if (track.armed) StudioRecord.copy(alpha = 0.055f) else MaterialTheme.colorScheme.surface.copy(alpha = 0.70f))
                .border(
                    width = if (track.armed) 1.5.dp else 0.dp,
                    color = if (track.armed) StudioRecord.copy(alpha = 0.82f) else Color.Transparent,
                    shape = RoundedCornerShape(8.dp),
                ),
        ) {
            // Keep the lane-level selection target as a sibling behind clip content. An ancestor
            // clickable merges descendant semantics in Compose and would hide TrimHandle nodes
            // from accessibility/testing while trim mode is active.
            Box(
                Modifier
                    .matchParentSize()
                    .testTag("track-waveform-area-${track.id}")
                    .semantics {
                        role = Role.Button
                        this.selected = selected
                        contentDescription = "Área de áudio da pista ${track.name}"
                    }
                    .clickable(enabled = !dragInProgress, role = Role.Button, onClick = onSelect),
            )

            if (clips.isEmpty() && liveWaveform.isEmpty()) {
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
                        onTrimStartFrameChanged = onTrimStartFrameChanged,
                        onTrimEndFrameChanged = onTrimEndFrameChanged,
                        canEdit = canEditClip,
                        dragging = draggingClipId == clip.id,
                        onSelect = onSelect,
                        onStartDrag = { origin, touch, size -> onStartClipDrag(clip, trackColor, waveforms[clip.id], origin, touch, size) },
                        onDrag = onDrag,
                        onDragEnd = onDragEnd,
                        onDragCancel = onDragCancel,
                        modifier = Modifier.offset(x = x).width(clipWidth).fillMaxHeight().padding(vertical = 5.dp).align(Alignment.CenterStart),
                    )
                }
            }
            if (liveWaveform.isNotEmpty()) {
                val startFraction = (liveStartFrame.toDouble() / projectEndFrame).coerceIn(0.0, 1.0)
                val endFraction = ((liveStartFrame + liveFrames).toDouble() / projectEndFrame).coerceIn(startFraction, 1.0)
                val x = maxWidth * startFraction.toFloat()
                val width = (maxWidth * (endFraction - startFraction).toFloat()).coerceAtLeast(24.dp).coerceAtMost((maxWidth - x).coerceAtLeast(1.dp))
                Surface(
                    modifier = Modifier
                        .offset(x = x)
                        .width(width)
                        .fillMaxHeight()
                        .padding(vertical = 5.dp)
                        .align(Alignment.CenterStart)
                        .clickable(role = Role.Button, onClick = onSelect)
                        .testTag("live-recording-waveform"),
                    shape = RoundedCornerShape(6.dp),
                    color = trackColor.copy(alpha = 0.24f),
                    border = BorderStroke(1.dp, StudioRecord),
                 ) {
                    LiveRecordingWaveformMini(
                        points = liveWaveform,
                        totalFrames = liveFrames,
                        color = StudioRecord,
                        modifier = Modifier.fillMaxSize().padding(4.dp),
                    )
                }
                RecordingLevelOverlay(
                    peak = livePeak,
                    rms = liveRms,
                    modifier = Modifier.align(Alignment.TopEnd).padding(top = 8.dp, end = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun RecordingLevelOverlay(
    peak: Float,
    rms: Float,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.testTag("recording-level-overlay"),
        shape = RoundedCornerShape(4.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
        border = BorderStroke(1.dp, StudioRecord.copy(alpha = 0.72f)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("PK ${formatDbFs(peak)}", style = MaterialTheme.typography.labelSmall, color = StudioRecord)
            Text("RMS ${formatDbFs(rms)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

private fun formatDbFs(value: Float): String {
    val safe = value.coerceAtLeast(0f)
    if (safe <= 0.000001f) return "−∞ dB"
    val db = 20.0 * kotlin.math.log10(safe.toDouble())
    return String.format(java.util.Locale.US, "%.1f dB", db)
}

@Composable
private fun LiveRecordingWaveformMini(
    points: List<LiveWaveformPoint>,
    totalFrames: Long,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier) {
        if (points.isEmpty() || totalFrames <= 0L) return@Canvas
        val centerY = size.height / 2f
        points.forEach { point ->
            val startFraction = (point.startFrame.toDouble() / totalFrames.toDouble()).toFloat().coerceIn(0f, 1f)
            val endFraction = (point.endFrameExclusive.toDouble() / totalFrames.toDouble()).toFloat().coerceIn(startFraction, 1f)
            val startX = size.width * startFraction
            val endX = size.width * endFraction
            val amplitude = point.peak.coerceIn(0f, 1f) * centerY
            val spanWidth = (endX - startX).coerceAtLeast(1f)
            val spanHeight = (amplitude * 2f).coerceAtLeast(1f)
            drawRect(
                color = color,
                topLeft = Offset(startX, centerY - spanHeight / 2f),
                size = androidx.compose.ui.geometry.Size(spanWidth, spanHeight),
            )
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
    onTrimStartFrameChanged: (Long) -> Unit,
    onTrimEndFrameChanged: (Long) -> Unit,
    canEdit: Boolean,
    dragging: Boolean,
    onSelect: () -> Unit,
    onStartDrag: (Offset, Offset, IntSize) -> Unit,
    onDrag: (Offset) -> Unit,
    onDragEnd: () -> Unit,
    onDragCancel: () -> Unit,
    modifier: Modifier,
) {
    val clipGeometry = remember(clip.id) { DragLayoutGeometry() }

    Surface(
        modifier = modifier
            .testTag("timeline-clip-${clip.id}")
            .alpha(if (dragging) 0.22f else 1f)
            .onGloballyPositioned { coordinates ->
                clipGeometry.origin = coordinates.positionInWindow()
                clipGeometry.size = coordinates.size
            }
            .pointerInput(clip.id, canEdit, trimming) {
                if (canEdit && !trimming) detectDragGesturesAfterLongPress(
                    onDragStart = { touch -> onStartDrag(clipGeometry.origin, touch, clipGeometry.size) },
                    onDragCancel = onDragCancel,
                    onDragEnd = onDragEnd,
                    onDrag = { change, amount ->
                        change.consume()
                        onDrag(amount)
                    },
                )
            }
            // Do not install clickable semantics at all while trimming. A disabled clickable still
            // merges descendants and would make the two trim handles disappear from the merged
            // semantics tree even though they remain visually rendered.
            .then(
                if (!dragging && !trimming) Modifier.clickable(role = Role.Button, onClick = onSelect)
                else Modifier,
            ),
        shape = RoundedCornerShape(6.dp),
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
    onStartChanged: (Long) -> Unit,
    onEndChanged: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val startFraction = ((trim.startFrame - clip.startFrame).toFloat() / clip.lengthFrames.coerceAtLeast(1L)).coerceIn(0f, 1f)
    val endFraction = ((trim.endFrame - clip.startFrame).toFloat() / clip.lengthFrames.coerceAtLeast(1L)).coerceIn(startFraction, 1f)
    BoxWithConstraints(modifier) {
        Canvas(Modifier.fillMaxSize()) {
            drawRect(
                color = StudioTrim.copy(alpha = 0.24f),
                topLeft = Offset(size.width * startFraction, 0f),
                size = androidx.compose.ui.geometry.Size(size.width * (endFraction - startFraction), size.height),
            )
        }
        if (channelPeaks?.size == 2) StereoWaveformMini(channelPeaks, false, trackColor, Modifier.fillMaxSize())
        else WaveformMini(peaks = peaks, color = trackColor, modifier = Modifier.fillMaxSize())
        TrimHandle(
            clip = clip,
            frame = trim.startFrame,
            tag = "trim-start-handle",
            label = "Marcador de início do corte",
            onFrameChanged = { onStartChanged(it.coerceAtMost(trim.endFrame - 1L)) },
            modifier = Modifier.fillMaxHeight(),
        )
        TrimHandle(
            clip = clip,
            frame = trim.endFrame,
            tag = "trim-end-handle",
            label = "Marcador de fim do corte",
            onFrameChanged = { onEndChanged(it.coerceAtLeast(trim.startFrame + 1L)) },
            modifier = Modifier.fillMaxHeight(),
        )
    }
}

@Composable
private fun BoxWithConstraintsScope.TrimHandle(
    clip: AudioClip,
    frame: Long,
    tag: String,
    label: String,
    onFrameChanged: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val widthPx = constraints.maxWidth.toFloat().coerceAtLeast(1f)
    val fraction = TrimControlPolicy.visibleFractionForFrame(clip, frame)
    val handleWidth = 48.dp
    val handleX = (maxWidth * fraction - handleWidth / 2).coerceIn(0.dp, (maxWidth - handleWidth).coerceAtLeast(0.dp))
    val currentFrame by rememberUpdatedState(frame)
    val latestOnFrameChanged by rememberUpdatedState(onFrameChanged)

    Box(
        modifier = modifier
            .offset(x = handleX)
            .width(handleWidth)
            .zIndex(6f)
            .semantics { contentDescription = label }
            .testTag(tag)
            .pointerInput(clip.id, tag, widthPx) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    val currentX = TrimControlPolicy.visibleFractionForFrame(clip, currentFrame) * widthPx
                    latestOnFrameChanged(TrimControlPolicy.visibleFrameAtPointerX(clip, currentX + dragAmount.x, widthPx))
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .width(3.dp)
                .fillMaxHeight()
                .background(StudioTrim),
        )
        Surface(
            modifier = Modifier.size(width = 18.dp, height = 38.dp),
            shape = RoundedCornerShape(6.dp),
            color = StudioTrim,
            border = BorderStroke(2.dp, Color.White.copy(alpha = 0.88f)),
            tonalElevation = 6.dp,
        ) {}
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
    focusTakeManagement: Boolean,
    levelAnalysis: studio.guitarlab.core.project.LevelAnalysis?,
    availableRoles: List<TrackRoleDefinition>,
    onActivateTake: (String) -> Unit,
    onAuditionTake: (String) -> Unit,
    onUpdateTakeMetadata: (String, String, String, Double) -> Unit,
    onToggleTakeFavorite: (String) -> Unit,
    onDeleteTake: (String) -> Unit,
    onAnalyzeLevel: () -> Unit,
    onApplyLevel: () -> Unit,
    onClearTrack: () -> Unit,
    onDismiss: () -> Unit,
    onSave: (String, Int, String?) -> Unit,
    onDelete: () -> Unit,
) {
    val hasClips = clips.isNotEmpty()
    var name by remember(track.id, track.name) { mutableStateOf(track.name) }
    var colorIndex by remember(track.id, track.colorIndex) {
        mutableIntStateOf(if (track.colorIndex in StudioTrackPalette.indices) track.colorIndex else track.order % StudioTrackPalette.size)
    }
    var selectedRoleId by remember(track.id, track.roleId) { mutableStateOf(track.roleId) }
    val validName = name.trim().length in 1..24
    val scrollState = rememberScrollState()
    var confirmClearTrack by remember(track.id) { mutableStateOf(false) }
    var editingTakeId by remember(track.id) { mutableStateOf<String?>(null) }
    var deleteTakeId by remember(track.id) { mutableStateOf<String?>(null) }

    LaunchedEffect(track.id, focusTakeManagement, takes.size) {
        if (focusTakeManagement && takes.isNotEmpty()) {
            delay(120L)
            scrollState.animateScrollTo(scrollState.maxValue)
        }
    }

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
                            TrackRoleEditor(
                                selectedRoleId = selectedRoleId,
                                availableRoles = availableRoles,
                                onRoleSelected = { selectedRoleId = it },
                                modifier = Modifier.weight(0.85f),
                            )
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            TrackNameField(
                                name = name,
                                onNameChange = { if (it.length <= 24) name = it },
                                modifier = Modifier.fillMaxWidth(),
                            )
                            TrackRoleEditor(
                                selectedRoleId = selectedRoleId,
                                availableRoles = availableRoles,
                                onRoleSelected = { selectedRoleId = it },
                                modifier = Modifier.fillMaxWidth(),
                            )
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

                Surface(modifier=Modifier.fillMaxWidth(), shape=RoundedCornerShape(8.dp), color=MaterialTheme.colorScheme.surfaceVariant.copy(alpha=0.22f)) {
                    Column(Modifier.padding(10.dp), verticalArrangement=Arrangement.spacedBy(6.dp)) {
                        Text("Nível assistido", style=MaterialTheme.typography.titleSmall)
                        if (levelAnalysis == null) {
                            Text("Analisa o nível efetivo da pista, incluindo os ganhos atuais da pista e dos clipes, com alvo RMS de −18 dBFS e pico máximo de −3 dBFS. Nada é aplicado automaticamente.", style=MaterialTheme.typography.bodySmall)
                            OutlinedButton(onClick=onAnalyzeLevel, enabled=hasClips) { Text("Analisar pista") }
                        } else {
                            Text("Pico efetivo ${"%.1f".format(levelAnalysis.peakDbfs)} dBFS · RMS ${"%.1f".format(levelAnalysis.rmsDbfs)} dBFS · ajuste ${"%+.1f".format(levelAnalysis.recommendedGainDb)} dB", style=MaterialTheme.typography.bodySmall)
                            if (kotlin.math.abs(levelAnalysis.recommendedGainDb) < 0.1f && !levelAnalysis.silent) {
                                Text("Nível já está dentro do alvo.", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
                                OutlinedButton(onClick=onAnalyzeLevel) { Text("Analisar novamente") }
                            } else {
                                Button(onClick=onApplyLevel, enabled=!levelAnalysis.silent) { Text("Aplicar sugestão") }
                            }
                        }
                    }
                }

                if (takes.isNotEmpty()) {
                    Surface(modifier=Modifier.fillMaxWidth(), shape=RoundedCornerShape(8.dp), color=MaterialTheme.colorScheme.surfaceVariant.copy(alpha=0.22f)) {
                        Column(Modifier.padding(10.dp), verticalArrangement=Arrangement.spacedBy(8.dp)) {
                            Text("Gerenciar takes", style=MaterialTheme.typography.titleSmall)
                            Text("Escolha o take ativo, ouça rapidamente e organize nome, nota e favorito sem alterar o áudio original.", style=MaterialTheme.typography.bodySmall, color=MaterialTheme.colorScheme.onSurfaceVariant)
                            takes.sortedWith(
                                compareByDescending<studio.guitarlab.core.model.RecordingTake> { it.active }
                                    .thenByDescending { it.favorite }
                                    .thenByDescending { it.createdAtEpochMs }
                                    .thenBy { it.id }
                            ).forEach { take ->
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.62f),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.22f)),
                                ) {
                                    Column(Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                buildString {
                                                    if (take.favorite) append("★ ")
                                                    if (take.active) append("Ativo · ")
                                                    append(take.name)
                                                },
                                                style = MaterialTheme.typography.labelLarge,
                                                modifier = Modifier.weight(1f),
                                            )
                                            TextButton(onClick = { onToggleTakeFavorite(take.id) }) { Text(if (take.favorite) "Desfavoritar" else "Favoritar") }
                                        }
                                        if (take.note.isNotBlank()) Text(take.note, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                            if (!take.active) OutlinedButton(onClick = { onActivateTake(take.id) }) { Text("Usar") }
                                            OutlinedButton(onClick = { onAuditionTake(take.id) }) { Text("Ouvir") }
                                            TextButton(onClick = { editingTakeId = take.id }) { Text("Editar") }
                                            TextButton(onClick = { deleteTakeId = take.id }, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) { Text("Excluir") }
                                        }
                                    }
                                }
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
                        Column(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Text(
                                "Limpar toda a pista remove todos os clipes/takes desta pista, mas preserva a pista, sua função, cor e mixagem.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            if (confirmClearTrack) {
                                Text("Confirma remover todo o conteúdo desta pista?", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.error)
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Button(
                                        onClick = { confirmClearTrack = false; onClearTrack() },
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                    ) { Text("Limpar toda a pista") }
                                    TextButton(onClick = { confirmClearTrack = false }) { Text("Cancelar") }
                                }
                            } else {
                                OutlinedButton(
                                    onClick = { confirmClearTrack = true },
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = null)
                                    Text("Limpar toda a pista", modifier = Modifier.padding(start = 6.dp))
                                }
                            }
                        }
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
            Button(onClick = { onSave(name.trim(), colorIndex, selectedRoleId) }, enabled = validName) { Text("Salvar") }
        },
    )

    editingTakeId?.let { id ->
        val take = takes.firstOrNull { it.id == id }
        if (take == null) {
            LaunchedEffect(id) { editingTakeId = null }
        } else {
            var takeName by remember(id, take.name) { mutableStateOf(take.name) }
            var takeNote by remember(id, take.note) { mutableStateOf(take.note) }
            var takeFineMs by remember(id, take.fineAdjustmentFrames, sampleRate) {
                mutableStateOf(studio.guitarlab.core.audio.LatencyFineAdjustmentPolicy.framesToMilliseconds(take.fineAdjustmentFrames, sampleRate))
            }
            fun adjustTakeFine(deltaMs: Double) {
                val bounded = (takeFineMs + deltaMs).coerceIn(
                    -studio.guitarlab.core.audio.LatencyFineAdjustmentPolicy.MAX_ABS_MILLISECONDS,
                    studio.guitarlab.core.audio.LatencyFineAdjustmentPolicy.MAX_ABS_MILLISECONDS,
                )
                takeFineMs = bounded
            }
            AlertDialog(
                onDismissRequest = { editingTakeId = null },
                title = { Text("Editar take") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = takeName,
                            onValueChange = { if (it.length <= studio.guitarlab.core.project.TakeManagementPolicy.MAX_NAME_LENGTH) takeName = it },
                            label = { Text("Nome") },
                            singleLine = true,
                            supportingText = { Text("1–${studio.guitarlab.core.project.TakeManagementPolicy.MAX_NAME_LENGTH} caracteres") },
                            isError = takeName.trim().isEmpty(),
                        )
                        OutlinedTextField(
                            value = takeNote,
                            onValueChange = { if (it.length <= studio.guitarlab.core.project.TakeManagementPolicy.MAX_NOTE_LENGTH) takeNote = it },
                            label = { Text("Nota curta") },
                            minLines = 2,
                            maxLines = 4,
                            supportingText = { Text("Até ${studio.guitarlab.core.project.TakeManagementPolicy.MAX_NOTE_LENGTH} caracteres") },
                        )
                        Text("Sincronização desta take", style = MaterialTheme.typography.titleSmall)
                        Text(
                            "Ajuste persistente somente desta take. Positivo antecipa; negativo atrasa. Splits da mesma take acompanham juntos e o áudio original não é alterado.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            String.format(java.util.Locale.US, "%+.1f ms", takeFineMs),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            OutlinedButton(onClick = { adjustTakeFine(-25.0) }) { Text("−25 ms") }
                            OutlinedButton(onClick = { adjustTakeFine(-5.0) }) { Text("−5 ms") }
                            OutlinedButton(onClick = { adjustTakeFine(-1.0) }) { Text("−1 ms") }
                            TextButton(onClick = { takeFineMs = 0.0 }) { Text("Zerar") }
                            OutlinedButton(onClick = { adjustTakeFine(1.0) }) { Text("+1 ms") }
                            OutlinedButton(onClick = { adjustTakeFine(5.0) }) { Text("+5 ms") }
                            OutlinedButton(onClick = { adjustTakeFine(25.0) }) { Text("+25 ms") }
                        }
                        Text(
                            "O ajuste global de Configurações vale apenas para novas gravações. Este valor pertence à take atual e é salvo no projeto.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                confirmButton = {
                    Button(
                        enabled = takeName.trim().isNotEmpty(),
                        onClick = {
                            onUpdateTakeMetadata(id, takeName.trim(), takeNote.trim(), takeFineMs)
                            editingTakeId = null
                        },
                    ) { Text("Salvar") }
                },
                dismissButton = { TextButton(onClick = { editingTakeId = null }) { Text("Cancelar") } },
            )
        }
    }

    deleteTakeId?.let { id ->
        val take = takes.firstOrNull { it.id == id }
        if (take != null) {
            AlertDialog(
                onDismissRequest = { deleteTakeId = null },
                title = { Text("Excluir ${take.name}?") },
                text = { Text("Os clipes deste take serão removidos da sessão. O GuitarLab mantém a operação reversível pelo histórico e não apaga mídia compartilhada automaticamente.") },
                confirmButton = {
                    Button(
                        onClick = { onDeleteTake(id); deleteTakeId = null },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    ) { Text("Excluir take") }
                },
                dismissButton = { TextButton(onClick = { deleteTakeId = null }) { Text("Cancelar") } },
            )
        } else {
            LaunchedEffect(id) { deleteTakeId = null }
        }
    }
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
private fun TrackRoleEditor(
    selectedRoleId: String?,
    availableRoles: List<TrackRoleDefinition>,
    onRoleSelected: (String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedName = availableRoles.firstOrNull { it.id == selectedRoleId }?.name
        ?: BuiltInRoles.definitions.firstOrNull { it.id == selectedRoleId }?.name
        ?: "Sem função"

    Surface(
        modifier = modifier.heightIn(min = 62.dp),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.30f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.30f)),
    ) {
        Box {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = true }
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Função", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(selectedName, style = MaterialTheme.typography.bodyMedium, maxLines = 2)
                }
                Text("Alterar", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                DropdownMenuItem(
                    text = { Text("Sem função") },
                    onClick = {
                        expanded = false
                        onRoleSelected(null)
                    },
                )
                availableRoles.forEach { role ->
                    DropdownMenuItem(
                        text = { Text(role.name) },
                        onClick = {
                            expanded = false
                            onRoleSelected(role.id)
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun NewTrackRolePromptDialog(
    trackName: String,
    suggestions: List<TrackRoleDefinition>,
    onChoose: (String) -> Unit,
    onKeepGeneric: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onKeepGeneric,
        title = { Text("Definir função da nova pista?") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("“$trackName” foi criada. Há funções importantes ainda disponíveis neste projeto:")
                suggestions.forEach { role ->
                    OutlinedButton(
                        onClick = { onChoose(role.id) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(role.name)
                    }
                }
                Text(
                    "A função ajuda o GuitarLab a organizar comparação e gravação. Você também pode alterá-la depois em Configurar pista.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onKeepGeneric) { Text("Agora não") } },
    )
}

@Composable
private fun TrackColorSection(
    colorIndex: Int,
    onColorChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
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
                                        .clip(RoundedCornerShape(8.dp))
                                        .semantics {
                                            contentDescription = "Cor da pista ${index + 1}"
                                            selected = colorIndex == index
                                            role = Role.RadioButton
                                        }
                                        .clickable { onColorChange(index) },
                                    shape = RoundedCornerShape(8.dp),
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
            shape = RoundedCornerShape(8.dp),
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
                            MetadataLine("Taxa de amostragem", clip.sourceSampleRateHz?.let { "$it Hz" } ?: "—")
                            MetadataLine("Canais", clip.sourceChannelCount?.toString() ?: "—")
                            MetadataLine("Profundidade de bits", clip.sourceBitsPerSample?.let { "$it bits" } ?: "—")
                            MetadataLine("Codificação", clip.sourceEncoding ?: "—")
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
                Text("Fade-in · ${fadeInMs.toInt()} ms")
                androidx.compose.material3.Slider(value = fadeInMs, onValueChange = { fadeInMs = it }, valueRange = 0f..maxMs)
                Text("Fade-out · ${fadeOutMs.toInt()} ms")
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
