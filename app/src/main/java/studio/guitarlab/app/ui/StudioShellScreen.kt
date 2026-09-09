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
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import studio.guitarlab.core.model.GuitarProject
import studio.guitarlab.core.project.TransportPolicy

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
    val recordPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        viewModel.onRecordPermissionResult(it)
    }

    LaunchedEffect(projectId) { viewModel.load(projectId) }
    LaunchedEffect(state.project?.tracks) {
        val ids = state.project?.tracks?.map { it.id }.orEmpty()
        if (selectedTrackId !in ids) selectedTrackId = ids.firstOrNull()
    }

    val structuralControlsEnabled = !state.importing &&
        !state.editingClip &&
        !state.historyBusy &&
        state.trimControls == null &&
        TransportPolicy.timelineEditingEnabled(state.transport)
    val mixControlsEnabled = !state.importing && !state.editingClip && !state.historyBusy && state.trimControls == null

    Column(Modifier.fillMaxSize()) {
        StudioTopBar(
            project = state.project,
            transport = {
                TransportBar(
                    state = state.transport,
                    engineReady = state.transportEngineReady && state.trimControls == null,
                    recordEnabled = state.project != null && !state.importing && !state.editingClip && !state.historyBusy && state.trimControls == null,
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
}

@Composable
private fun StudioTopBar(
    project: GuitarProject?,
    transport: @Composable () -> Unit,
    onMixer: () -> Unit,
    onOptions: () -> Unit,
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
                Text(project?.name ?: "GuitarLab", style = MaterialTheme.typography.titleLarge, maxLines = 1)
                project?.let {
                    Text(
                        "${it.tracks.size} ${if (it.tracks.size == 1) "pista" else "pistas"} · ${it.clips.size} ${if (it.clips.size == 1) "clipe" else "clipes"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                transport()
            }

            Row(horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.CenterVertically) {
                AppIconButton(icon = Icons.Default.Equalizer, contentDescription = "Mixer", onClick = onMixer)
                AppIconButton(icon = Icons.Default.Tune, contentDescription = "Opções", onClick = onOptions)
                AppIconButton(icon = Icons.Default.Home, contentDescription = "Início", onClick = onHome)
            }
        }
    }
}
