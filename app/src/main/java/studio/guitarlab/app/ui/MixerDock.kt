package studio.guitarlab.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import kotlin.math.log10
import studio.guitarlab.core.audio.MeterBallisticsState
import studio.guitarlab.core.model.AudioTrack

@Composable
fun MixerDock(
    tracks: List<AudioTrack>,
    selectedTrackId: String?,
    pinned: Boolean,
    editingEnabled: Boolean,
    masterGainDb: Float,
    masterMeter: MeterBallisticsState,
    trackMeters: Map<String, MeterBallisticsState>,
    onSelectTrack: (String) -> Unit,
    onTogglePinned: () -> Unit,
    onClose: () -> Unit,
    onGainChanged: (String, Float) -> Unit,
    onPanChanged: (String, Float) -> Unit,
    onToggleMute: (String) -> Unit,
    onToggleSolo: (String) -> Unit,
    onMasterGainChanged: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth().height(260.dp),
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
                        if (editingEnabled) "Track + master mix" else "Locked during transport",
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
                        editingEnabled = editingEnabled,
                        meter = trackMeters[track.id] ?: MeterBallisticsState(),
                        onSelect = { onSelectTrack(track.id) },
                        onGainChanged = { onGainChanged(track.id, it) },
                        onPanChanged = { onPanChanged(track.id, it) },
                        onToggleMute = { onToggleMute(track.id) },
                        onToggleSolo = { onToggleSolo(track.id) },
                    )
                }
                MasterStrip(
                    gainDb = masterGainDb,
                    meter = masterMeter,
                    editingEnabled = editingEnabled,
                    onGainChanged = onMasterGainChanged,
                )
            }
        }
    }
}

@Composable
private fun MixerTrackStrip(
    track: AudioTrack,
    selected: Boolean,
    editingEnabled: Boolean,
    meter: MeterBallisticsState,
    onSelect: () -> Unit,
    onGainChanged: (Float) -> Unit,
    onPanChanged: (Float) -> Unit,
    onToggleMute: () -> Unit,
    onToggleSolo: () -> Unit,
) {
    var gainDraft by remember(track.id, track.gainDb) { mutableFloatStateOf(track.gainDb) }
    var panDraft by remember(track.id, track.pan) { mutableFloatStateOf(track.pan) }
    val borderColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
    Surface(
        modifier = Modifier.width(176.dp).fillMaxHeight().clip(RoundedCornerShape(12.dp)).clickable(onClick = onSelect),
        shape = RoundedCornerShape(12.dp),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.26f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.34f),
        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor),
    ) {
        Column(
            Modifier.fillMaxHeight().padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(track.name, modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelLarge, maxLines = 1)
                Text(if (track.armed) "R" else "", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.error)
            }

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(onClick = onToggleMute, enabled = editingEnabled) { Text(if (track.muted) "M✓" else "M") }
                TextButton(onClick = onToggleSolo, enabled = editingEnabled) { Text(if (track.solo) "S✓" else "S") }
            }

            MeterRow("PK", meter.peak, meter.heldPeak)
            MeterRow("RMS", meter.rms)

            Text("${gainDraft.formatDb()} dB", style = MaterialTheme.typography.labelMedium)
            Slider(
                value = gainDraft,
                onValueChange = { gainDraft = it },
                onValueChangeFinished = { onGainChanged(gainDraft) },
                enabled = editingEnabled,
                valueRange = -60f..12f,
            )

            Text("Pan ${panDraft.formatPan()}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Slider(
                value = panDraft,
                onValueChange = { panDraft = it },
                onValueChangeFinished = { onPanChanged(panDraft) },
                enabled = editingEnabled,
                valueRange = -1f..1f,
            )
        }
    }
}

@Composable
private fun MasterStrip(
    gainDb: Float,
    meter: MeterBallisticsState,
    editingEnabled: Boolean,
    onGainChanged: (Float) -> Unit,
) {
    var gainDraft by remember(gainDb) { mutableFloatStateOf(gainDb) }
    Surface(
        modifier = Modifier.width(192.dp).fillMaxHeight(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.32f),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.secondary),
    ) {
        Column(
            Modifier.fillMaxHeight().padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text("MASTER", style = MaterialTheme.typography.labelLarge)
            Text("Routing in Options", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

            MeterRow("PK", meter.peak, meter.heldPeak)
            MeterRow("RMS", meter.rms)

            Text("${gainDraft.formatDb()} dB", style = MaterialTheme.typography.labelMedium)
            Slider(
                value = gainDraft,
                onValueChange = { gainDraft = it },
                onValueChangeFinished = { onGainChanged(gainDraft) },
                enabled = editingEnabled,
                valueRange = -60f..12f,
            )

            Text(
                if (meter.heldPeak > 1f || meter.peak > 1f) "CLIP" else "Main output",
                style = MaterialTheme.typography.labelMedium,
                color = if (meter.heldPeak > 1f || meter.peak > 1f) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun MeterRow(label: String, value: Float, heldPeak: Float? = null) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(label, modifier = Modifier.width(28.dp), style = MaterialTheme.typography.labelSmall)
        BoxWithConstraints(
            Modifier.weight(1f).height(9.dp).background(MaterialTheme.colorScheme.background.copy(alpha = 0.62f), RoundedCornerShape(5.dp)),
        ) {
            Box(
                Modifier.fillMaxWidth(value.coerceIn(0f, 1f)).fillMaxHeight().background(MaterialTheme.colorScheme.primary, RoundedCornerShape(5.dp)),
            )
            heldPeak?.takeIf { it > 0f }?.let { held ->
                val x = (maxWidth * held.coerceIn(0f, 1f)).coerceAtMost(maxWidth - 2.dp)
                Box(
                    Modifier.offset(x = x).width(2.dp).fillMaxHeight().background(MaterialTheme.colorScheme.onSurface),
                )
            }
        }
        Text(value.formatDbfs(), modifier = Modifier.width(42.dp), style = MaterialTheme.typography.labelSmall)
    }
}

private fun Float.formatDb(): String = if (this >= 0f) "+%.1f".format(this) else "%.1f".format(this)
private fun Float.formatPan(): String = when {
    this < -0.02f -> "L${(kotlin.math.abs(this) * 100).toInt()}"
    this > 0.02f -> "R${(this * 100).toInt()}"
    else -> "C"
}
private fun Float.formatDbfs(): String {
    if (this <= 0.000001f) return "-60"
    return "%.0f".format((20.0 * log10(this.toDouble())).coerceAtLeast(-60.0))
}
