package studio.guitarlab.app.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import studio.guitarlab.core.project.RecordingSessionPhase
import studio.guitarlab.core.project.TransportPolicy
import studio.guitarlab.core.project.TimelineControlPolicy
import studio.guitarlab.platform.codec.android.MasterExportFormat

@Composable
fun StudioShellScreen(
    projectId: String,
    onBack: () -> Unit,
    onOptions: () -> Unit,
    viewModel: StudioViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val uiPreferences = remember(context) { StudioUiPreferencesStore(context) }
    var mixerPinned by rememberSaveable(projectId) { mutableStateOf(uiPreferences.mixerPinned()) }
    var mixerVisible by rememberSaveable(projectId) { mutableStateOf(uiPreferences.mixerPinned()) }
    var selectedTrackId by rememberSaveable(projectId) { mutableStateOf<String?>(null) }
    var exportDialogVisible by rememberSaveable(projectId) { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val recordPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        viewModel.onRecordPermissionResult(it)
    }
    val safeProjectName = state.project?.name
        ?.replace(Regex("[^A-Za-z0-9._ -]"), "_")
        ?.trim()
        ?.ifBlank { "GuitarLab" }
        ?: "GuitarLab"
    val projectLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri ->
        if (uri != null) viewModel.exportProjectPackage(uri)
    }
    val wavLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("audio/wav")) { uri ->
        if (uri != null) viewModel.exportMaster(uri, MasterExportFormat.WAV_FLOAT32)
    }
    val flacLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("audio/flac")) { uri ->
        if (uri != null) viewModel.exportMaster(uri, MasterExportFormat.FLAC)
    }
    val mp3Launcher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("audio/mpeg")) { uri ->
        if (uri != null) viewModel.exportMaster(uri, MasterExportFormat.MP3)
    }

    LaunchedEffect(projectId) { viewModel.load(projectId) }
    LaunchedEffect(state.project?.tracks) {
        val ids = state.project?.tracks?.map { it.id }.orEmpty()
        if (selectedTrackId !in ids) selectedTrackId = ids.firstOrNull()
    }
    val transientMessage = if (state.project != null) {
        state.error ?: if (!state.importing && state.recordingSession.phase == RecordingSessionPhase.IDLE) {
            state.clipStatus ?: state.importStatus ?: state.exportStatus
        } else null
    } else null
    LaunchedEffect(transientMessage) {
        transientMessage?.let { message ->
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarHostState.showSnackbar(message = message, duration = SnackbarDuration.Short)
            viewModel.dismissTransientMessage(message)
        }
    }

    val structuralControlsEnabled = !state.importing &&
        !state.editingClip &&
        !state.historyBusy &&
        !state.exporting &&
        state.trimControls == null &&
        TransportPolicy.timelineEditingEnabled(state.transport)
    val mixControlsEnabled = !state.importing && !state.editingClip && !state.historyBusy && !state.exporting && state.trimControls == null

    Box(
        Modifier
            .fillMaxSize()
            .semantics { testTagsAsResourceId = true }
            .testTag(if (state.project != null) "studio-loaded" else "studio-loading"),
    ) {
        Column(Modifier.fillMaxSize()) {
            val topBarProject = state.project
            val topBarEnd = topBarProject?.let(TimelineControlPolicy::projectEndFrame) ?: 0L
            val topBarRate = topBarProject?.sampleRate?.fixedHz
                ?: topBarProject?.clips?.firstNotNullOfOrNull { it.sourceSampleRateHz }
                ?: 48_000
            StudioTopBar(
                project = topBarProject,
                remainingText = formatFrameTime((topBarEnd - state.timelineControls.playheadFrame).coerceAtLeast(0L), topBarRate),
                totalText = formatFrameTime(topBarEnd, topBarRate),
                transport = {
                    TransportBar(
                        state = state.transport,
                        engineReady = state.transportEngineReady && state.trimControls == null,
                        recordEnabled = state.project != null && !state.importing && !state.editingClip && !state.historyBusy && !state.exporting && state.trimControls == null,
                        recordingPhase = state.recordingSession.phase,
                        canUndo = state.canUndo && !state.historyBusy,
                        canRedo = state.canRedo && !state.historyBusy,
                        onReturnToStart = viewModel::returnToStart,
                        onPlayStop = viewModel::togglePlayStop,
                        onRecord = {
                            if (context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                                viewModel.startRecording()
                            } else {
                                recordPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            }
                        },
                        onToggleLoop = viewModel::toggleLoop,
                        onUndo = viewModel::undo,
                        onRedo = viewModel::redo,
                    )
                },
                onMixer = { mixerVisible = true },
                onOptions = onOptions,
                onShare = { exportDialogVisible = true },
                onHome = onBack,
            )

            Box(Modifier.fillMaxWidth().weight(1f)) {
                StudioPlaceholderScreen(
                    viewModel = viewModel,
                    selectedTrackId = selectedTrackId,
                    onSelectTrack = { selectedTrackId = it },
                )
            }

            val project = state.project
            if (project != null && mixerVisible) {
                MixerDock(
                    tracks = project.tracks,
                    selectedTrackId = selectedTrackId,
                    pinned = mixerPinned,
                    mixControlsEnabled = mixControlsEnabled,
                    structuralControlsEnabled = structuralControlsEnabled,
                    masterGainDb = state.masterGainDb,
                    masterMeter = state.masterMeter,
                    trackMeters = state.trackMeters,
                    masterClipLatched = state.masterClipLatched,
                    trackClipLatched = state.trackClipLatched,
                    onSelectTrack = { selectedTrackId = it },
                    onPin = {
                        mixerPinned = true
                        mixerVisible = true
                        uiPreferences.setMixerPinned(true)
                    },
                    onClose = {
                        if (mixerPinned) {
                            mixerPinned = false
                            uiPreferences.setMixerPinned(false)
                        }
                        mixerVisible = false
                    },
                    onGainPreview = viewModel::previewTrackGainDb,
                    onGainCommit = viewModel::commitTrackGainDb,
                    onPanPreview = viewModel::previewTrackPan,
                    onPanCommit = viewModel::commitTrackPan,
                    onToggleMute = viewModel::toggleTrackMuted,
                    onToggleSolo = viewModel::toggleTrackSolo,
                    onToggleArm = viewModel::toggleTrackArmed,
                    onMasterGainPreview = viewModel::previewMasterGainDb,
                    onMasterGainCommit = viewModel::commitMasterGainDb,
                    onClearTrackClip = viewModel::clearTrackClipIndicator,
                    onClearMasterClip = viewModel::clearMasterClipIndicator,
                )
            }
        }
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 76.dp).widthIn(max = 560.dp),
        )
    }

    if (exportDialogVisible && state.project != null) {
        SaveAndExportDialog(
            projectName = state.project!!.name,
            busy = state.exporting,
            onDismiss = { if (!state.exporting) exportDialogVisible = false },
            onSaveProject = {
                exportDialogVisible = false
                projectLauncher.launch("$safeProjectName.guitarlab")
            },
            onWav = {
                exportDialogVisible = false
                wavLauncher.launch("$safeProjectName-master.wav")
            },
            onFlac = {
                exportDialogVisible = false
                flacLauncher.launch("$safeProjectName-master.flac")
            },
            onMp3 = {
                exportDialogVisible = false
                mp3Launcher.launch("$safeProjectName-master.mp3")
            },
        )
    }
}

@Composable
private fun StudioTopBar(
    project: GuitarProject?,
    remainingText: String,
    totalText: String,
    transport: @Composable () -> Unit,
    onMixer: () -> Unit,
    onOptions: () -> Unit,
    onShare: () -> Unit,
    onHome: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.background,
        tonalElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.widthIn(min = 180.dp, max = 320.dp),
                verticalArrangement = Arrangement.spacedBy(1.dp),
            ) {
                Text(
                    project?.name ?: "GuitarLab",
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 1,
                )
                project?.let {
                    Text(
                        "${it.tracks.size} ${if (it.tracks.size == 1) "pista" else "pistas"} · ${it.clips.size} ${if (it.clips.size == 1) "clipe" else "clipes"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) { transport() }

            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.30f),
                tonalElevation = 0.dp,
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Restante $remainingText", style = MaterialTheme.typography.labelMedium)
                    Text("·", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Total $totalText", style = MaterialTheme.typography.labelMedium)
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.CenterVertically) {
                AppIconButton(icon = Icons.Default.Equalizer, contentDescription = "Mixer", onClick = onMixer)
                AppIconButton(icon = Icons.Default.Tune, contentDescription = "Opções", onClick = onOptions, modifier = Modifier.testTag("studio-options"))
                AppIconButton(icon = Icons.Default.Share, contentDescription = "Salvar e exportar", enabled = project != null, onClick = onShare)
                AppIconButton(icon = Icons.Default.Home, contentDescription = "Início", onClick = onHome)
            }
        }
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

@Composable
fun SaveAndExportDialog(
    projectName: String,
    busy: Boolean,
    onDismiss: () -> Unit,
    onSaveProject: () -> Unit,
    onWav: () -> Unit,
    onFlac: () -> Unit,
    onMp3: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text("Salvar e exportar")
                Text(
                    projectName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Escolha entre preservar o projeto editável ou gerar um arquivo final de áudio.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Text("PROJETO EDITÁVEL", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
                ExportActionCard(
                    title = "Projeto GuitarLab",
                    badge = ".guitarlab",
                    detail = "Pacote portátil com pistas, clips, edições, fontes originais e proxies necessários.",
                    icon = { Icon(Icons.Default.Archive, contentDescription = null) },
                    enabled = !busy,
                    onClick = onSaveProject,
                )

                Text("MASTER FINAL", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
                ExportActionCard(
                    title = "WAV",
                    badge = "32-bit float",
                    detail = "Máxima qualidade para arquivo, edição ou masterização posterior.",
                    enabled = !busy,
                    onClick = onWav,
                )
                ExportActionCard(
                    title = "FLAC",
                    badge = "Lossless",
                    detail = "Compactação sem perdas com arquivo menor que WAV.",
                    enabled = !busy,
                    onClick = onFlac,
                )
                ExportActionCard(
                    title = "MP3",
                    badge = "320 kbps",
                    detail = "Versão prática e compatível para compartilhamento.",
                    enabled = !busy,
                    onClick = onMp3,
                )
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !busy) { Text("Fechar") }
        },
    )
}

@Composable
private fun ExportActionCard(
    title: String,
    badge: String,
    detail: String,
    enabled: Boolean,
    icon: (@Composable () -> Unit)? = null,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (enabled) 0.42f else 0.20f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.28f)),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            icon?.invoke()
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(title, style = MaterialTheme.typography.titleSmall)
                    Text(badge, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.secondary)
                }
                Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
