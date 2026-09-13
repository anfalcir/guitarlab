package studio.guitarlab.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Redo
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import studio.guitarlab.app.ui.theme.StudioLoop
import studio.guitarlab.app.ui.theme.StudioRecord
import studio.guitarlab.core.project.TransportMode
import studio.guitarlab.core.project.TransportPolicy
import studio.guitarlab.core.project.TransportState
import studio.guitarlab.core.project.RecordingSessionPhase

@Composable
fun TransportBar(
    state: TransportState,
    engineReady: Boolean,
    recordEnabled: Boolean,
    recordingPhase: RecordingSessionPhase,
    canUndo: Boolean,
    canRedo: Boolean,
    onReturnToStart: () -> Unit,
    onPlayStop: () -> Unit,
    onRecord: () -> Unit,
    onToggleLoop: () -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val markerEditingEnabled = TransportPolicy.timelineEditingEnabled(state)
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.38f),
        tonalElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
            horizontalArrangement = Arrangement.spacedBy(1.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AppIconButton(
                icon = Icons.Default.SkipPrevious,
                contentDescription = "Voltar ao início",
                enabled = state.mode != TransportMode.RECORDING && recordingPhase == RecordingSessionPhase.IDLE,
                onClick = onReturnToStart,
            )
            AppIconButton(
                icon = if (state.mode == TransportMode.STOPPED) Icons.Default.PlayArrow else Icons.Default.Stop,
                contentDescription = if (state.mode == TransportMode.STOPPED) "Reproduzir" else "Parar",
                enabled = TransportPolicy.playStopEnabled(state, engineReady) && recordingPhase == RecordingSessionPhase.IDLE,
                onClick = onPlayStop,
            )
            AppIconButton(
                icon = Icons.Default.FiberManualRecord,
                contentDescription = when (recordingPhase) {
                    RecordingSessionPhase.COUNTDOWN -> "Cancelar contagem da gravação"
                    RecordingSessionPhase.CAPTURING -> "Parar gravação"
                    RecordingSessionPhase.FINALIZING -> "Finalizando gravação"
                    RecordingSessionPhase.IDLE -> "Gravar"
                },
                enabled = recordEnabled && recordingPhase != RecordingSessionPhase.FINALIZING,
                tint = StudioRecord,
                onClick = onRecord,
            )
            AppIconButton(
                icon = Icons.Default.Repeat,
                contentDescription = if (state.loopEnabled) "Desativar loop" else "Ativar loop",
                enabled = markerEditingEnabled,
                tint = if (state.loopEnabled) StudioLoop else MaterialTheme.colorScheme.onSurface,
                onClick = onToggleLoop,
            )
            AppIconButton(
                icon = Icons.Default.Undo,
                contentDescription = "Desfazer",
                enabled = markerEditingEnabled && canUndo,
                onClick = onUndo,
            )
            AppIconButton(
                icon = Icons.Default.Redo,
                contentDescription = "Refazer",
                enabled = markerEditingEnabled && canRedo,
                onClick = onRedo,
            )
        }
    }
}
