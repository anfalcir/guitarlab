package studio.guitarlab.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import studio.guitarlab.core.project.TransportPolicy

@Composable
fun StudioShellScreen(
    projectId: String,
    onBack: () -> Unit,
    onOptions: () -> Unit,
    viewModel: StudioViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsState()
    var mixerVisible by rememberSaveable(projectId) { mutableStateOf(false) }
    var mixerPinned by rememberSaveable(projectId) { mutableStateOf(false) }
    var selectedTrackId by rememberSaveable(projectId) { mutableStateOf<String?>(null) }

    LaunchedEffect(projectId) { viewModel.load(projectId) }
    LaunchedEffect(state.project?.tracks) {
        val ids = state.project?.tracks?.map { it.id }.orEmpty()
        if (selectedTrackId !in ids) selectedTrackId = ids.firstOrNull()
    }

    val editingEnabled = !state.importing &&
        !state.editingClip &&
        state.trimControls == null &&
        TransportPolicy.timelineEditingEnabled(state.transport)

    Column(Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxWidth().weight(1f)) {
            StudioPlaceholderScreen(
                projectId = projectId,
                onBack = onBack,
                viewModel = viewModel,
                selectedTrackId = selectedTrackId,
                onSelectTrack = { selectedTrackId = it },
                onShowMixer = { trackId ->
                    selectedTrackId = trackId
                    mixerVisible = true
                },
            )
            Surface(
                modifier = Modifier.align(Alignment.TopEnd).padding(top = 8.dp, end = 118.dp),
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
                tonalElevation = 2.dp,
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = { mixerVisible = true }) { Text("Mixer") }
                    TextButton(onClick = onOptions) { Text("Options") }
                }
            }
        }

        val project = state.project
        if (project != null && mixerVisible) {
            MixerDock(
                tracks = project.tracks,
                selectedTrackId = selectedTrackId,
                pinned = mixerPinned,
                editingEnabled = editingEnabled,
                masterGainDb = state.masterGainDb,
                masterMeter = state.masterMeter,
                trackMeters = state.trackMeters,
                onSelectTrack = { selectedTrackId = it },
                onTogglePinned = { mixerPinned = !mixerPinned },
                onClose = { if (!mixerPinned) mixerVisible = false },
                onGainChanged = viewModel::setTrackGainDb,
                onPanChanged = viewModel::setTrackPan,
                onToggleMute = viewModel::toggleTrackMuted,
                onToggleSolo = viewModel::toggleTrackSolo,
                onMasterGainChanged = viewModel::setMasterGainDb,
            )
        }
    }
}
