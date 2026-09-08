package studio.guitarlab.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
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
    val editingEnabled = TransportPolicy.timelineEditingEnabled(state)
    Surface(modifier = modifier, tonalElevation = 0.dp, color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.28f)) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TransportButton("|◀", "Return to timeline start", editingEnabled, onReturnToStart)
            TransportButton(
                if (state.mode == TransportMode.STOPPED) "▶" else "■",
                if (state.mode == TransportMode.STOPPED) "Play" else "Stop",
                engineReady || state.mode == TransportMode.PLAYING,
                onPlayStop,
            )
            // Recording belongs to M5 and must not become enabled merely because M4 playback is ready.
            TransportButton("●", "Record", false, onRecord, StudioRecord)
            TransportButton("↻", "Loop", editingEnabled, onToggleLoop, if (state.loopEnabled) StudioLoop else null)
        }
    }
}

@Composable
private fun TransportButton(
    symbol: String,
    description: String,
    enabled: Boolean,
    onClick: () -> Unit,
    accent: androidx.compose.ui.graphics.Color? = null,
) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.semantics { contentDescription = description },
    ) {
        Text(
            symbol,
            style = MaterialTheme.typography.titleLarge,
            color = accent ?: MaterialTheme.colorScheme.onSurface,
        )
    }
}
