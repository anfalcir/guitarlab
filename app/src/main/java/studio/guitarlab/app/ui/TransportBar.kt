package studio.guitarlab.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Stop
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

@Composable
fun TransportBar(
    state: TransportState,
    engineReady: Boolean,
    onReturnToStart: () -> Unit,
    onPlayStop: () -> Unit,
    onRecord: () -> Unit,
    onToggleLoop: () -> Unit,
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
                enabled = markerEditingEnabled,
                onClick = onReturnToStart,
            )
            AppIconButton(
                icon = if (state.mode == TransportMode.STOPPED) Icons.Default.PlayArrow else Icons.Default.Stop,
                contentDescription = if (state.mode == TransportMode.STOPPED) "Reproduzir" else "Parar",
                enabled = engineReady || state.mode == TransportMode.PLAYING,
                onClick = onPlayStop,
            )
            AppIconButton(
                icon = Icons.Default.FiberManualRecord,
                contentDescription = "Gravar",
                enabled = false,
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
        }
    }
}
