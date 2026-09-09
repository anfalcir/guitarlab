package studio.guitarlab.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.log10
import studio.guitarlab.app.ui.theme.StudioMute
import studio.guitarlab.app.ui.theme.StudioRecord
import studio.guitarlab.app.ui.theme.StudioSolo
import studio.guitarlab.core.audio.MeterBallisticsState
import studio.guitarlab.core.model.AudioTrack

@Composable
fun MixerDock(
    tracks: List<AudioTrack>,
    selectedTrackId: String?,
    pinned: Boolean,
    mixControlsEnabled: Boolean,
    structuralControlsEnabled: Boolean,
    masterGainDb: Float,
    masterMeter: MeterBallisticsState,
    trackMeters: Map<String, MeterBallisticsState>,
    masterClipLatched: Boolean,
    trackClipLatched: Set<String>,
    onSelectTrack: (String) -> Unit,
    onPin: () -> Unit,
    onClose: () -> Unit,
    onGainPreview: (String, Float) -> Unit,
    onGainCommit: (String, Float) -> Unit,
    onPanPreview: (String, Float) -> Unit,
    onPanCommit: (String, Float) -> Unit,
    onToggleMute: (String) -> Unit,
    onToggleSolo: (String) -> Unit,
    onToggleArm: (String) -> Unit,
    onMasterGainPreview: (Float) -> Unit,
    onMasterGainCommit: (Float) -> Unit,
    onClearTrackClip: (String) -> Unit,
    onClearMasterClip: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth().height(246.dp),
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp,
    ) {
        Column(Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("Mixer", style = MaterialTheme.typography.titleMedium)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (!pinned) {
                        AppIconButton(
                            icon = Icons.Default.PushPin,
                            contentDescription = "Fixar mixer",
                            onClick = onPin,
                        )
                    }
                    AppIconButton(
                        icon = Icons.Default.Close,
                        contentDescription = if (pinned) "Fechar e desafixar mixer" else "Fechar mixer",
                        onClick = onClose,
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().weight(1f),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(
                    modifier = Modifier.weight(1f).fillMaxHeight().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    tracks.sortedBy { it.order }.forEach { track ->
                        MixerTrackStrip(
                            track = track,
                            selected = track.id == selectedTrackId,
                            mixControlsEnabled = mixControlsEnabled,
                            structuralControlsEnabled = structuralControlsEnabled,
                            meter = trackMeters[track.id] ?: MeterBallisticsState(),
                            clipLatched = track.id in trackClipLatched,
                            onSelect = { onSelectTrack(track.id) },
                            onGainPreview = { onGainPreview(track.id, it) },
                            onGainCommit = { onGainCommit(track.id, it) },
                            onPanPreview = { onPanPreview(track.id, it) },
                            onPanCommit = { onPanCommit(track.id, it) },
                            onToggleMute = { onToggleMute(track.id) },
                            onToggleSolo = { onToggleSolo(track.id) },
                            onToggleArm = { onToggleArm(track.id) },
                            onClearClip = { onClearTrackClip(track.id) },
                        )
                    }
                }

                MasterStrip(
                    gainDb = masterGainDb,
                    meter = masterMeter,
                    clipLatched = masterClipLatched,
                    enabled = mixControlsEnabled,
                    onGainPreview = onMasterGainPreview,
                    onGainCommit = onMasterGainCommit,
                    onClearClip = onClearMasterClip,
                )
            }
        }
    }
}

@Composable
private fun MixerTrackStrip(
    track: AudioTrack,
    selected: Boolean,
    mixControlsEnabled: Boolean,
    structuralControlsEnabled: Boolean,
    meter: MeterBallisticsState,
    clipLatched: Boolean,
    onSelect: () -> Unit,
    onGainPreview: (Float) -> Unit,
    onGainCommit: (Float) -> Unit,
    onPanPreview: (Float) -> Unit,
    onPanCommit: (Float) -> Unit,
    onToggleMute: () -> Unit,
    onToggleSolo: () -> Unit,
    onToggleArm: () -> Unit,
    onClearClip: () -> Unit,
) {
    var gainDraft by remember(track.id, track.gainDb) { mutableFloatStateOf(track.gainDb) }
    var panDraft by remember(track.id, track.pan) { mutableFloatStateOf(track.pan) }
    val accent = track.resolvedStudioColor()
    val borderColor = if (selected) accent else MaterialTheme.colorScheme.outline.copy(alpha = 0.55f)

    Surface(
        modifier = Modifier.width(184.dp).fillMaxHeight().clip(RoundedCornerShape(12.dp)).clickable(onClick = onSelect),
        shape = RoundedCornerShape(12.dp),
        color = if (selected) accent.copy(alpha = 0.10f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.28f),
        border = BorderStroke(if (selected) 1.5.dp else 1.dp, borderColor),
    ) {
        Column(
            Modifier.fillMaxHeight().padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(7.dp).background(accent, RoundedCornerShape(4.dp)))
                Text(
                    track.name,
                    modifier = Modifier.weight(1f).padding(start = 6.dp),
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 1,
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                MixerStateButton("M", track.muted, StudioMute, structuralControlsEnabled, onToggleMute)
                MixerStateButton("S", track.solo, StudioSolo, structuralControlsEnabled, onToggleSolo)
                MixerStateButton("R", track.armed, StudioRecord, structuralControlsEnabled, onToggleArm)
            }

            MeterRow("PK", meter.peak, accent, meter.heldPeak, clipLatched, onClearClip)
            MeterRow("RMS", meter.rms, accent)

            Text("${gainDraft.formatDb()} dB", style = MaterialTheme.typography.labelMedium)
            MixerSlider(
                value = gainDraft,
                valueRange = -60f..12f,
                neutralValue = 0f,
                snapThreshold = 0.8f,
                accent = accent,
                enabled = mixControlsEnabled,
                onValueChange = { value ->
                    gainDraft = value
                    onGainPreview(value)
                },
                onValueChangeFinished = { onGainCommit(gainDraft) },
                contentDescription = "Volume da pista ${track.name}",
            )

            Text("Pan ${panDraft.formatPan()}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            MixerSlider(
                value = panDraft,
                valueRange = -1f..1f,
                neutralValue = 0f,
                snapThreshold = 0.035f,
                accent = accent,
                enabled = mixControlsEnabled,
                onValueChange = { value ->
                    panDraft = value
                    onPanPreview(value)
                },
                onValueChangeFinished = { onPanCommit(panDraft) },
                contentDescription = "Pan da pista ${track.name}",
            )
        }
    }
}

@Composable
private fun MasterStrip(
    gainDb: Float,
    meter: MeterBallisticsState,
    clipLatched: Boolean,
    enabled: Boolean,
    onGainPreview: (Float) -> Unit,
    onGainCommit: (Float) -> Unit,
    onClearClip: () -> Unit,
) {
    var gainDraft by remember(gainDb) { mutableFloatStateOf(gainDb) }
    val accent = MaterialTheme.colorScheme.secondary
    Surface(
        modifier = Modifier.width(198.dp).fillMaxHeight(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.30f),
        border = BorderStroke(1.5.dp, accent.copy(alpha = 0.78f)),
    ) {
        Column(
            Modifier.fillMaxHeight().padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Text("MASTER", style = MaterialTheme.typography.labelLarge)
            Text("Saída principal", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

            MeterRow("PK", meter.peak, accent, meter.heldPeak, clipLatched, onClearClip)
            MeterRow("RMS", meter.rms, accent)

            Spacer(Modifier.height(2.dp))
            Text("${gainDraft.formatDb()} dB", style = MaterialTheme.typography.labelMedium)
            MixerSlider(
                value = gainDraft,
                valueRange = -60f..12f,
                neutralValue = 0f,
                snapThreshold = 0.8f,
                accent = accent,
                enabled = enabled,
                onValueChange = { value ->
                    gainDraft = value
                    onGainPreview(value)
                },
                onValueChangeFinished = { onGainCommit(gainDraft) },
                contentDescription = "Volume Master",
            )
        }
    }
}

@Composable
private fun MixerStateButton(
    label: String,
    active: Boolean,
    activeColor: Color,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val inactive = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.58f)
    val border = if (active) activeColor else MaterialTheme.colorScheme.outline.copy(alpha = 0.55f)
    val foreground = if (active) activeColor else inactive
    Surface(
        modifier = Modifier.size(width = 38.dp, height = 30.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable(enabled = enabled, onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        color = if (active) activeColor.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface.copy(alpha = 0.44f),
        border = BorderStroke(if (active) 1.5.dp else 1.dp, border),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(label, style = MaterialTheme.typography.labelLarge, color = foreground.copy(alpha = if (enabled) 1f else 0.55f))
        }
    }
}

@Composable
private fun MixerSlider(
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    neutralValue: Float,
    snapThreshold: Float,
    accent: Color,
    enabled: Boolean,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit,
    contentDescription: String,
) {
    val neutralFraction = ((neutralValue - valueRange.start) / (valueRange.endInclusive - valueRange.start)).coerceIn(0f, 1f)
    BoxWithConstraints(modifier = Modifier.fillMaxWidth().height(30.dp)) {
        Slider(
            value = value,
            onValueChange = { raw ->
                val snapped = if (abs(raw - neutralValue) <= snapThreshold) neutralValue else raw
                onValueChange(snapped.coerceIn(valueRange.start, valueRange.endInclusive))
            },
            onValueChangeFinished = onValueChangeFinished,
            valueRange = valueRange,
            enabled = enabled,
            colors = SliderDefaults.colors(
                thumbColor = accent,
                activeTrackColor = accent.copy(alpha = 0.88f),
                inactiveTrackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.34f),
                disabledThumbColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.42f),
                disabledActiveTrackColor = accent.copy(alpha = 0.30f),
                disabledInactiveTrackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.20f),
            ),
            modifier = Modifier.fillMaxWidth().height(30.dp),
        )
        Box(
            Modifier
                .align(Alignment.CenterStart)
                .offset(x = (maxWidth * neutralFraction) - 1.dp)
                .width(2.dp)
                .height(18.dp)
                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f), RoundedCornerShape(1.dp)),
        )
    }
}

@Composable
private fun MeterRow(
    label: String,
    value: Float,
    accent: Color,
    heldPeak: Float? = null,
    clipLatched: Boolean = false,
    onClearClip: (() -> Unit)? = null,
) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(label, modifier = Modifier.width(28.dp), style = MaterialTheme.typography.labelSmall)
        BoxWithConstraints(
            Modifier.weight(1f).height(8.dp).background(MaterialTheme.colorScheme.background.copy(alpha = 0.78f), RoundedCornerShape(4.dp)),
        ) {
            Box(
                Modifier.fillMaxWidth(value.coerceIn(0f, 1f)).fillMaxHeight().background(accent, RoundedCornerShape(4.dp)),
            )
            heldPeak?.takeIf { it > 0f }?.let { held ->
                val x = (maxWidth * held.coerceIn(0f, 1f)).coerceAtMost(maxWidth - 2.dp)
                Box(
                    Modifier.offset(x = x).width(2.dp).fillMaxHeight().background(MaterialTheme.colorScheme.onSurface),
                )
            }
        }
        if (clipLatched && onClearClip != null) {
            Surface(
                modifier = Modifier.clip(RoundedCornerShape(5.dp)).clickable(onClick = onClearClip),
                shape = RoundedCornerShape(5.dp),
                color = MaterialTheme.colorScheme.error.copy(alpha = 0.16f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
            ) {
                Text(
                    "CLIP",
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        } else {
            Text(value.formatDbfs(), modifier = Modifier.width(38.dp), style = MaterialTheme.typography.labelSmall)
        }
    }
}

private fun Float.formatDb(): String = if (this >= 0f) "+%.1f".format(this) else "%.1f".format(this)
private fun Float.formatPan(): String = when {
    this < -0.02f -> "E${(abs(this) * 100).toInt()}"
    this > 0.02f -> "D${(this * 100).toInt()}"
    else -> "C"
}
private fun Float.formatDbfs(): String {
    if (this <= 0.000001f) return "-60"
    return "%.0f".format((20.0 * log10(this.toDouble())).coerceAtLeast(-60.0))
}
