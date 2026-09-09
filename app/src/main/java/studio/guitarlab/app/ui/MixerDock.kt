package studio.guitarlab.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import studio.guitarlab.core.model.AudioTrack

@Composable
fun MixerDock(
    tracks: List<AudioTrack>,
    selectedTrackId: String?,
    pinned: Boolean,
    onSelectTrack: (String) -> Unit,
    onTogglePinned: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth().height(218.dp),
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Mixer", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Track controls",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(onClick = onTogglePinned) { Text(if (pinned) "Unpin" else "Pin") }
                    TextButton(onClick = onClose, enabled = !pinned) { Text("✕") }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().weight(1f).horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                tracks.sortedBy { it.order }.forEach { track ->
                    MixerTrackStrip(
                        track = track,
                        selected = track.id == selectedTrackId,
                        onSelect = { onSelectTrack(track.id) },
                    )
                }
                MasterStrip()
            }
        }
    }
}

@Composable
private fun MixerTrackStrip(track: AudioTrack, selected: Boolean, onSelect: () -> Unit) {
    val borderColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
    Surface(
        modifier = Modifier.width(136.dp).fillMaxHeight().clip(RoundedCornerShape(12.dp)).clickable(onClick = onSelect),
        shape = RoundedCornerShape(12.dp),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.26f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.34f),
        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor),
    ) {
        Column(
            Modifier.fillMaxHeight().padding(10.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(track.name, style = MaterialTheme.typography.labelLarge, maxLines = 1)
                Text(
                    "${if (track.muted) "M" else "·"}  ${if (track.solo) "S" else "·"}  ${if (track.armed) "R" else "·"}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Box(
                Modifier.fillMaxWidth().height(52.dp).background(MaterialTheme.colorScheme.background.copy(alpha = 0.58f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Text("${track.gainDb.formatDb()} dB", style = MaterialTheme.typography.titleSmall)
            }
            Text(
                "Pan ${track.pan.formatPan()}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun MasterStrip() {
    Surface(
        modifier = Modifier.width(152.dp).fillMaxHeight(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.32f),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.secondary),
    ) {
        Column(
            Modifier.fillMaxHeight().padding(10.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text("MASTER", style = MaterialTheme.typography.labelLarge)
                Text("Output routing in Options", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Box(
                Modifier.fillMaxWidth().height(52.dp).background(MaterialTheme.colorScheme.background.copy(alpha = 0.58f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center,
            ) { Text("0.0 dB", style = MaterialTheme.typography.titleSmall) }
            Text("Main output", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun Float.formatDb(): String = if (this >= 0f) "+%.1f".format(this) else "%.1f".format(this)
private fun Float.formatPan(): String = when {
    this < -0.02f -> "L${(kotlin.math.abs(this) * 100).toInt()}"
    this > 0.02f -> "R${(this * 100).toInt()}"
    else -> "C"
}
