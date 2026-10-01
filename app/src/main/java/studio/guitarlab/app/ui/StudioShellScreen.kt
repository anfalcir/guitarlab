package studio.guitarlab.app.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import studio.guitarlab.core.model.GuitarProject
import studio.guitarlab.core.project.PracticeRecordingMode
import studio.guitarlab.core.project.PreparedReferenceBindingPolicy
import studio.guitarlab.core.project.ExternalControlCommandDispatcher
import studio.guitarlab.core.project.ExternalControlCommandHandlers
import studio.guitarlab.core.project.RecordingSessionPhase
import studio.guitarlab.core.project.TransportPolicy

@Composable
fun StudioShellScreen(
    projectId: String,
    shellProject: GuitarProject,
    navigationEntry: Long = 0L,
    onBack: () -> Unit,
    onPrepare: () -> Unit,
    onOptions: () -> Unit,
    onExport: () -> Unit,
    viewModel: StudioViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    ExternalControlHub.initialize(context)
    val uiPreferences = remember(context) { StudioUiPreferencesStore(context) }
    var mixerVisible by rememberSaveable { mutableStateOf(uiPreferences.mixerVisible()) }
    var selectedTrackId by rememberSaveable(projectId) { mutableStateOf<String?>(null) }
    var helpDialogVisible by rememberSaveable(projectId) { mutableStateOf(false) }
    var allLevelsDialogVisible by rememberSaveable(projectId) { mutableStateOf(false) }
    var projectMediaVisible by rememberSaveable(projectId) { mutableStateOf(false) }
    var pendingRecordMode by remember(projectId) { mutableStateOf(PracticeRecordingMode.CURRENT_PLAYHEAD) }
    val recordPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) viewModel.startRecording(pendingRecordMode) else viewModel.onRecordPermissionResult(false)
    }

    fun requestRecording(mode: PracticeRecordingMode) {
        pendingRecordMode = mode
        if (context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            viewModel.startRecording(mode)
        } else {
            recordPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    fun handleRecordIntent() {
        // External control and the on-screen REC button share this exact path.
        val liveState = viewModel.state.value
        when (liveState.recordingSession.phase) {
            RecordingSessionPhase.IDLE -> {
                if (liveState.transport.loopEnabled) viewModel.showLoopRecordingChoice()
                else requestRecording(PracticeRecordingMode.CURRENT_PLAYHEAD)
            }
            RecordingSessionPhase.COUNTDOWN,
            RecordingSessionPhase.CAPTURING,
            RecordingSessionPhase.FINALIZING -> viewModel.startRecording()
        }
    }

    LaunchedEffect(projectId, navigationEntry) { viewModel.load(projectId) }
    LaunchedEffect(projectId) {
        ExternalControlHub.actions.collect { action ->
            ExternalControlCommandDispatcher.dispatch(
                action = action,
                handlers = ExternalControlCommandHandlers(
                    playStop = viewModel::togglePlayStop,
                    recordToggle = ::handleRecordIntent,
                    returnToStart = viewModel::returnToStart,
                    loopToggle = viewModel::toggleLoop,
                    undo = viewModel::undo,
                    redo = viewModel::redo,
                ),
            )
        }
    }
    DisposableEffect(projectId) { onDispose { viewModel.onStudioHidden() } }
    LaunchedEffect(state.project?.tracks) {
        val ids = state.project?.tracks?.map { it.id }.orEmpty()
        if (selectedTrackId !in ids) selectedTrackId = ids.firstOrNull()
    }
    val rawTransientMessage = if (state.project != null) {
        state.error ?: state.transientNotice
    } else null
    val transientKind = if (state.error != null) TransientFeedbackKind.ERROR else state.transientNoticeKind

    val structuralControlsEnabled = !state.importing &&
        !state.editingClip &&
        !state.historyBusy &&
        state.trimControls == null &&
        TransportPolicy.timelineEditingEnabled(state.transport)
    val mixControlsEnabled = !state.importing && !state.editingClip && !state.historyBusy && state.trimControls == null

    Box(
        Modifier
            .fillMaxSize()
            .semantics { testTagsAsResourceId = true }
            .testTag(if (state.project != null) "studio-loaded" else "studio-loading"),
    ) {
        ProjectShellScaffold(
            project = state.project ?: shellProject,
            currentWorkspace = ProjectWorkspace.STUDIO,
            onProjects = onBack,
            onPrepare = onPrepare,
            onStudio = {},
            onExport = onExport,
            onSettings = onOptions,
            trailingActions = {
                AppIconButton(
                    icon = Icons.Default.FolderOpen,
                    contentDescription = "Mídia do projeto",
                    onClick = { projectMediaVisible = true },
                    modifier = Modifier.testTag("studio-project-media"),
                )
                AppIconButton(
                    icon = Icons.Default.Equalizer,
                    contentDescription = if (mixerVisible) "Ocultar Mixer" else "Mostrar Mixer",
                    onClick = {
                        val nextVisible = !mixerVisible
                        mixerVisible = nextVisible
                        uiPreferences.setMixerVisible(nextVisible)
                    },
                    tint = if (mixerVisible) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.testTag("studio-mixer-toggle"),
                )
                AppIconButton(
                    icon = Icons.Default.Info,
                    contentDescription = "Ajuda",
                    onClick = { helpDialogVisible = true },
                    modifier = Modifier.testTag("studio-help"),
                )
            },
            secondaryBar = {
                Surface(modifier = Modifier.fillMaxWidth(), tonalElevation = 0.dp) {
                    Box(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp)) {
                        Box(modifier = Modifier.align(Alignment.Center).testTag("studio-navigation")) {
                            TransportBar(
                                state = state.transport,
                                engineReady = state.transportEngineReady && state.trimControls == null,
                                recordEnabled = state.project != null && !state.importing && !state.editingClip && !state.historyBusy && state.trimControls == null,
                                recordingPhase = state.recordingSession.phase,
                                canUndo = state.canUndo && !state.historyBusy,
                                canRedo = state.canRedo && !state.historyBusy,
                                onReturnToStart = viewModel::returnToStart,
                                onPlayStop = viewModel::togglePlayStop,
                                onRecord = ::handleRecordIntent,
                                onToggleLoop = viewModel::toggleLoop,
                                onUndo = viewModel::undo,
                                onRedo = viewModel::redo,
                            )
                        }
                    }
                }
            },
        ) {
            Column(Modifier.fillMaxSize()) {
            if (state.preparedReferenceUpdateAvailable) {
                Surface(
                    modifier = Modifier.fillMaxWidth().testTag("studio-prepared-reference-update"),
                    tonalElevation = 3.dp,
                    color = MaterialTheme.colorScheme.secondaryContainer,
                ) {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("Base/referência precisam ser sincronizadas com o Studio", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                        OutlinedButton(onClick = viewModel::keepCurrentPreparedReferences, modifier = Modifier.testTag("studio-keep-prepared-reference")) { Text("Manter atual") }
                        Button(onClick = viewModel::applyPreparedReferenceUpdate, modifier = Modifier.testTag("studio-update-prepared-reference")) { Text("Atualizar Studio") }
                    }
                }
            }

            Box(Modifier.fillMaxWidth().weight(1f)) {
                StudioPlaceholderScreen(
                    viewModel = viewModel,
                    selectedTrackId = selectedTrackId,
                    onSelectTrack = { selectedTrackId = it },
                    showPracticeControls = !mixerVisible,
                    onOpenLevelAnalysis = { allLevelsDialogVisible = true },
                )
            }

            val project = state.project
            if (project != null && mixerVisible) {
                MixerDock(
                    tracks = project.tracks,
                    auditionMode = state.guitarAuditionMode,
                    selectedTrackId = selectedTrackId,
                    mixControlsEnabled = mixControlsEnabled,
                    structuralControlsEnabled = structuralControlsEnabled,
                    masterGainDb = state.masterGainDb,
                    masterMeter = state.masterMeter,
                    trackMeters = state.trackMeters,
                    masterClipLatched = state.masterClipLatched,
                    trackClipLatched = state.trackClipLatched,
                    onSelectTrack = { selectedTrackId = it },
                    onGainPreview = viewModel::previewTrackGainDb,
                    onGainCommit = viewModel::commitTrackGainDb,
                    onPanPreview = viewModel::previewTrackPan,
                    onPanCommit = viewModel::commitTrackPan,
                    onToggleMute = viewModel::toggleTrackMuted,
                    onToggleSolo = viewModel::toggleTrackSolo,
                    onToggleCue = viewModel::toggleTrackCue,
                    onToggleArm = viewModel::toggleTrackArmed,
                    onMasterGainPreview = viewModel::previewMasterGainDb,
                    onMasterGainCommit = viewModel::commitMasterGainDb,
                    onClearTrackClip = viewModel::clearTrackClipIndicator,
                    onClearMasterClip = viewModel::clearMasterClipIndicator,
                    headerContent = {
                        PracticeControls(
                            project = project,
                            auditionMode = state.guitarAuditionMode,
                            suggestions = state.sectionSuggestions.size,
                            enabled = structuralControlsEnabled,
                            loopEnabled = state.transport.loopEnabled,
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
                            onOpenLevelAnalysis = { allLevelsDialogVisible = true },
                            docked = true,
                        )
                    },
                )
            }
        }
        }
        AppTransientFeedbackHost(
            message = rawTransientMessage,
            kind = transientKind,
            onConsumed = { rawTransientMessage?.let(viewModel::dismissTransientMessage) },
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 64.dp),
            fallback = "Não foi possível concluir a operação de áudio.",
        )
    }

    val levelProject = state.project
    if (allLevelsDialogVisible && levelProject != null) {
        AllTracksLevelDialog(
            project = levelProject,
            analyses = state.trackLevelAnalysis,
            busyTrackIds = state.trackLevelAnalysisBusy,
            onAnalyzeTrack = viewModel::analyzeTrackLevel,
            onAnalyzeAll = viewModel::analyzeAllTrackLevels,
            onApplyTrack = viewModel::applyTrackLevelSuggestion,
            onApplyAll = viewModel::applyAllTrackLevelSuggestions,
            onDismiss = { allLevelsDialogVisible = false },
        )
    }

    if (state.loopRecordingChoiceVisible) {
        AlertDialog(
            onDismissRequest = viewModel::dismissLoopRecordingChoice,
            title = { Text("Gravar com o loop ativo") },
            text = {
                Text(
                    "Escolha como iniciar esta gravação. “Desde o início” desativa o loop e grava a partir de 00:00. “Somente o loop” usa o trecho marcado como punch, com o pre-roll/post-roll já previstos pelo GuitarLab.",
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.dismissLoopRecordingChoice()
                        requestRecording(PracticeRecordingMode.LOOP_PUNCH)
                    },
                ) { Text("Somente o loop") }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = viewModel::dismissLoopRecordingChoice) { Text("Cancelar") }
                    OutlinedButton(
                        onClick = {
                            viewModel.dismissLoopRecordingChoice()
                            requestRecording(PracticeRecordingMode.FROM_PROJECT_START)
                        },
                    ) { Text("Desde o início") }
                }
            },
        )
    }

    if (helpDialogVisible) {
        StudioUserGuideDialog(onDismiss = { helpDialogVisible = false })
    }

    val mediaProject = state.project
    if (projectMediaVisible && mediaProject != null) {
        ProjectMediaDialog(
            project = mediaProject,
            referenceBindingDiffers = PreparedReferenceBindingPolicy.bindingDiffersFromDesired(mediaProject),
            referenceDecisionPending = state.preparedReferenceUpdateAvailable,
            onKeepCurrent = {
                viewModel.keepCurrentPreparedReferences()
                projectMediaVisible = false
            },
            onApplyUpdate = {
                viewModel.applyPreparedReferenceUpdate()
                projectMediaVisible = false
            },
            onDismiss = { projectMediaVisible = false },
        )
    }


}

@Composable
fun RenameProjectDialog(
    currentName: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var value by remember(currentName) { mutableStateOf(currentName) }
    val normalized = value.trim().replace(Regex("\\s+"), " ")
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Renomear projeto") },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = { if (it.length <= 80) value = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Nome do projeto") },
                supportingText = { Text("${value.length}/80") },
                singleLine = true,
            )
        },
        confirmButton = {
            Button(
                enabled = normalized.isNotBlank() && normalized != currentName,
                onClick = { onConfirm(normalized) },
            ) { Text("Renomear") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}
