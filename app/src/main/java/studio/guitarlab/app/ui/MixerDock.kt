package studio.guitarlab.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.log10
import studio.guitarlab.app.ui.theme.StudioComparisonActive
import studio.guitarlab.app.ui.theme.StudioComparisonHidden
import studio.guitarlab.app.ui.theme.StudioMute
import studio.guitarlab.app.ui.theme.StudioRecord
import studio.guitarlab.app.ui.theme.StudioSolo
import studio.guitarlab.core.audio.MeterBallisticsState
import studio.guitarlab.core.model.AudioTrack
import studio.guitarlab.core.model.TrackOutputRoute
import studio.guitarlab.core.project.GuitarAuditionMode
import studio.guitarlab.core.project.GuitarAuditionPolicy
import studio.guitarlab.core.project.GuitarAuditionTrackState

@Composable
fun MixerDock(
    tracks: List<AudioTrack>,
    auditionMode: GuitarAuditionMode = GuitarAuditionMode.MIXER,
    selectedTrackId: String?,
    mixControlsEnabled: Boolean,
    structuralControlsEnabled: Boolean,
    masterGainDb: Float,
    masterMeter: MeterBallisticsState,
    trackMeters: Map<String, MeterBallisticsState>,
    masterClipLatched: Boolean,
    trackClipLatched: Set<String>,
    onSelectTrack: (String) -> Unit,
    onGainPreview: (String, Float) -> Unit,
    onGainCommit: (String, Float) -> Unit,
    onPanPreview: (String, Float) -> Unit,
    onPanCommit: (String, Float) -> Unit,
    onToggleMute: (String) -> Unit,
    onToggleSolo: (String) -> Unit,
    onToggleCue: (String) -> Unit,
    onToggleArm: (String) -> Unit,
    onMasterGainPreview: (Float) -> Unit,
    onMasterGainCommit: (Float) -> Unit,
    onClearTrackClip: (String) -> Unit,
    onClearMasterClip: () -> Unit,
    minimal: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val fontScale = LocalDensity.current.fontScale.coerceAtLeast(1f)
    val dockHeight = (if (minimal) 172.dp else 252.dp) + ((if (minimal) 48.dp else 96.dp) * (fontScale - 1f))
    Surface(
        modifier = modifier.fillMaxWidth().height(dockHeight).testTag("mixer-dock"),
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp,
    ) {
        Column(Modifier.fillMaxSize().padding(horizontal = 8.dp, vertical = 0.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().weight(1f),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                LazyRow(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .testTag("mixer-track-scroll"),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    userScrollEnabled = true,
                ) {
                    items(tracks.sortedBy { it.order }, key = { it.id }) { track ->
                        MixerTrackStrip(
                            track = track,
                            minimal = minimal,
                            auditionMode = auditionMode,
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
                            onToggleCue = { onToggleCue(track.id) },
                            onToggleArm = { onToggleArm(track.id) },
                            onClearClip = { onClearTrackClip(track.id) },
                        )
                    }
                }

                MasterStrip(
                    minimal = minimal,
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
    minimal: Boolean = false,
    auditionMode: GuitarAuditionMode,
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
    onToggleCue: () -> Unit,
    onToggleArm: () -> Unit,
    onClearClip: () -> Unit,
) {
    var gainDraft by remember(track.id, track.gainDb) { mutableFloatStateOf(track.gainDb) }
    var panDraft by remember(track.id, track.pan) { mutableFloatStateOf(track.pan) }
    var detailsVisible by remember(track.id) { mutableStateOf(false) }
    val accent = track.resolvedStudioColor()
    val auditionState = GuitarAuditionPolicy.trackState(track.roleId, auditionMode)
    val borderColor = if (selected) accent else MaterialTheme.colorScheme.outline.copy(alpha = 0.55f)

    Surface(
        modifier = Modifier
            .width(200.dp + 80.dp * (LocalDensity.current.fontScale.coerceAtLeast(1f) - 1f))
            .fillMaxHeight()
            .testTag("mixer-track-strip-${track.id}")
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onSelect),
        shape = RoundedCornerShape(8.dp),
        color = if (selected) accent.copy(alpha = 0.10f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.28f),
        border = BorderStroke(if (selected) 1.5.dp else 1.dp, borderColor),
    ) {
        Column(
            Modifier.fillMaxHeight().padding(horizontal = 4.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp),
        ) {
            Row(Modifier.fillMaxWidth().height(48.dp * LocalDensity.current.fontScale.coerceAtLeast(1f))
                .clickable { onSelect(); if (minimal) detailsVisible = true }
                .semantics { contentDescription = if (minimal) "Detalhes da pista ${track.name}" else "Selecionar pista ${track.name}" }, verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(7.dp).background(accent, RoundedCornerShape(4.dp)))
                Text(
                    track.name,
                    modifier = Modifier.weight(1f).padding(start = 6.dp),
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (minimal) {
                    Text(track.pan.formatPan(), Modifier.padding(horizontal = 4.dp), style = MaterialTheme.typography.labelSmall)
                }
                if (auditionState != GuitarAuditionTrackState.UNAFFECTED) {
                    val included = auditionState == GuitarAuditionTrackState.INCLUDED
                    Text(if (included) "●" else "○", Modifier.semantics {
                        stateDescription = if (included) "Incluída na comparação" else "Oculta pela comparação"
                    }, color = if (included) StudioComparisonActive else StudioComparisonHidden)
                }
                if (minimal && clipLatched) {
                    Text("!", color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.semantics { stateDescription = "Clipping da pista ${track.name}" })
                } else if (clipLatched) {
                    MixerClipButton(
                        contentDescription = "Limpar clipping da pista ${track.name}",
                        onClick = onClearClip,
                    )
                }

            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                MixerStateButton(
                    label = "M",
                    active = track.muted,
                    activeColor = StudioMute,
                    enabled = mixControlsEnabled,
                    contentDescription = "Mute da pista ${track.name}",
                    onClick = onToggleMute,
                )
                MixerStateButton(
                    label = "S",
                    active = track.solo,
                    activeColor = StudioSolo,
                    enabled = mixControlsEnabled,
                    contentDescription = "Solo da pista ${track.name}",
                    onClick = onToggleSolo,
                )
                MixerCueButton(
                    active = track.outputRoute != TrackOutputRoute.MAIN,
                    activeColor = MaterialTheme.colorScheme.tertiary,
                    enabled = structuralControlsEnabled,
                    contentDescription = "Saída CUE da pista ${track.name}",
                    onClick = onToggleCue,
                )
                MixerArmButton(
                    active = track.armed,
                    enabled = structuralControlsEnabled,
                    contentDescription = "Gravação da pista ${track.name}",
                    onClick = onToggleArm,
                )
            }

            if (minimal) {
                CompactMeter(meter, accent)
            } else {
                MeterPair(
                    meter = meter,
                    accent = accent,
                    modifier = Modifier.fillMaxWidth().testTag("mixer-track-meters-${track.id}"),
                )
            }

            LabeledVolumeSlider(
                value = gainDraft,
                accent = accent,
                enabled = mixControlsEnabled,
                onValueChange = { value ->
                    gainDraft = value
                    onGainPreview(value)
                },
                onValueChangeFinished = { onGainCommit(gainDraft) },
                contentDescription = "Volume da pista ${track.name}",
            )

            if (!minimal) BipolarPanSlider(
                value = panDraft,
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
    if (detailsVisible && minimal) {
        AlertDialog(
            modifier = Modifier.testTag("mixer-track-details"),
            onDismissRequest = { detailsVisible = false },
            title = { Text("Controles da pista") },
            text = {
                Box(Modifier.height(252.dp + 96.dp * (LocalDensity.current.fontScale.coerceAtLeast(1f) - 1f))) {
                    MixerTrackStrip(
                        track = track, auditionMode = auditionMode, selected = selected,
                        mixControlsEnabled = mixControlsEnabled, structuralControlsEnabled = structuralControlsEnabled,
                        meter = meter, clipLatched = clipLatched, onSelect = onSelect,
                        onGainPreview = onGainPreview, onGainCommit = onGainCommit,
                        onPanPreview = onPanPreview, onPanCommit = onPanCommit,
                        onToggleMute = onToggleMute, onToggleSolo = onToggleSolo,
                        onToggleCue = onToggleCue, onToggleArm = onToggleArm, onClearClip = onClearClip,
                    )
                }
            },
            confirmButton = { TextButton(onClick = { detailsVisible = false }) { Text("Fechar") } },
        )
    }

}

@Composable
private fun MasterStrip(
    minimal: Boolean,
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
        modifier = Modifier.width(144.dp + 80.dp * (LocalDensity.current.fontScale.coerceAtLeast(1f) - 1f)).fillMaxHeight().testTag("mixer-master-strip"),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.30f),
        border = BorderStroke(1.5.dp, accent.copy(alpha = 0.78f)),
    ) {
        Column(
            Modifier.fillMaxHeight().padding(horizontal = 8.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(Modifier.fillMaxWidth().height(48.dp * LocalDensity.current.fontScale.coerceAtLeast(1f)), verticalAlignment = Alignment.CenterVertically) {
                Text("MASTER", Modifier.weight(1f), style = MaterialTheme.typography.labelLarge)
                if (clipLatched) {
                    MixerClipButton(
                        contentDescription = "Limpar clipping do master",
                        onClick = onClearClip,
                    )
                }
            }
            if (minimal) {
                CompactMeter(meter, accent)
            } else {
                MeterPair(
                    meter = meter,
                    accent = accent,
                    modifier = Modifier.fillMaxWidth().testTag("mixer-master-meters"),
                )
            }

            MasterVolumeSlider(
                value = gainDraft,
                accent = accent,
                enabled = enabled,
                onValueChange = { value ->
                    gainDraft = value
                    onGainPreview(value)
                },
                onValueChangeFinished = { onGainCommit(gainDraft) },
                contentDescription = "Volume do master",
            )
        }
    }
}

@Composable
private fun MixerClipButton(
    contentDescription: String,
    onClick: () -> Unit,
) {
    Box(
        Modifier.size(48.dp).clickable(role = Role.Button, onClick = onClick)
            .semantics { this.contentDescription = contentDescription },
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            modifier = Modifier.size(width = 38.dp, height = 24.dp),
            shape = RoundedCornerShape(4.dp),
            color = MaterialTheme.colorScheme.error.copy(alpha = 0.16f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text("CLIP", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun MixerStateButton(
    label: String,
    active: Boolean,
    activeColor: Color,
    enabled: Boolean,
    contentDescription: String,
    onClick: () -> Unit,
) {
    val inactive = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.58f)
    val border = if (active) activeColor else MaterialTheme.colorScheme.outline.copy(alpha = 0.55f)
    val foreground = if (active) activeColor else inactive
    Box(
        modifier = Modifier
            .size(48.dp)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .semantics {
                this.contentDescription = contentDescription
                stateDescription = if (active) "Ativado" else "Desativado"
            },
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            modifier = Modifier.size(46.dp).testTag("mixer-button-face-$contentDescription"),
            shape = RoundedCornerShape(8.dp),
            color = if (active) activeColor.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface.copy(alpha = 0.44f),
            border = BorderStroke(if (active) 1.5.dp else 1.dp, border),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(label, style = MaterialTheme.typography.labelLarge, color = foreground.copy(alpha = if (enabled) 1f else 0.55f))
            }
        }
    }
}

@Composable
private fun MixerCueButton(
    active: Boolean,
    activeColor: Color,
    enabled: Boolean,
    contentDescription: String,
    onClick: () -> Unit,
) {
    val inactive = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.58f)
    val border = if (active) activeColor else MaterialTheme.colorScheme.outline.copy(alpha = 0.55f)
    val foreground = if (active) activeColor else inactive
    Box(
        modifier = Modifier
            .size(48.dp)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .semantics {
                this.contentDescription = contentDescription
                stateDescription = if (active) "Ativado" else "Desativado"
            },
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            modifier = Modifier.size(46.dp).testTag("mixer-button-face-$contentDescription"),
            shape = RoundedCornerShape(8.dp),
            color = if (active) activeColor.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface.copy(alpha = 0.44f),
            border = BorderStroke(if (active) 1.5.dp else 1.dp, border),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.Headphones,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = foreground.copy(alpha = if (enabled) 1f else 0.55f),
                )
            }
        }
    }
}

@Composable
private fun MixerArmButton(
    active: Boolean,
    enabled: Boolean,
    contentDescription: String,
    onClick: () -> Unit,
) {
    val vivid = StudioRecord
    val dim = vivid.copy(alpha = if (enabled) 0.27f else 0.16f)
    Box(
        modifier = Modifier
            .size(48.dp)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .semantics {
                this.contentDescription = contentDescription
                stateDescription = if (active) "Armada" else "Desarmada"
            },
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            modifier = Modifier.size(46.dp).testTag("mixer-button-face-$contentDescription"),
            shape = RoundedCornerShape(8.dp),
            color = if (active) vivid.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surface.copy(alpha = 0.44f),
            border = BorderStroke(if (active) 1.5.dp else 1.dp, if (active) vivid else vivid.copy(alpha = 0.18f)),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.FiberManualRecord,
                    contentDescription = null,
                    modifier = Modifier.size(15.dp),
                    tint = if (active) vivid else dim,
                )
            }
        }
    }
}

@Composable
private fun LabeledVolumeSlider(
    value: Float, accent: Color, enabled: Boolean,
    onValueChange: (Float) -> Unit, onValueChangeFinished: () -> Unit,
    contentDescription: String, label: String = "VOL",
) {
    DenseMixerSlider(label, "${value.formatDb()} dB", value, -60f..12f, accent, enabled,
        onValueChange = { raw -> onValueChange((if (abs(raw) <= 0.8f) 0f else raw).coerceIn(-60f, 12f)) },
        onValueChangeFinished = onValueChangeFinished, contentDescription = contentDescription)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MasterVolumeSlider(
    value: Float,
    accent: Color,
    enabled: Boolean,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit,
    contentDescription: String,
) {
    val tint = if (enabled) accent else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
    Column(
        Modifier.fillMaxWidth().height(68.dp).testTag("mixer-master-volume"),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Slider(
            value = value,
            onValueChange = { raw ->
                onValueChange((if (abs(raw) <= 0.8f) 0f else raw).coerceIn(-60f, 12f))
            },
            onValueChangeFinished = onValueChangeFinished,
            valueRange = -60f..12f,
            enabled = enabled,
            modifier = Modifier.fillMaxWidth().height(48.dp).padding(horizontal = 10.dp)
                .testTag("mixer-master-volume-slider")
                .semantics { this.contentDescription = contentDescription },
            thumb = {
                Box(Modifier.size(width = 6.dp, height = 48.dp), contentAlignment = Alignment.Center) {
                    Box(Modifier.size(width = 6.dp, height = 20.dp).background(tint, RoundedCornerShape(3.dp)))
                }
            },
            track = {
                BoxWithConstraints(
                    Modifier.fillMaxWidth().height(4.dp)
                        .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(2.dp)),
                ) {
                    val fraction = ((value + 60f) / 72f).coerceIn(0f, 1f)
                    Box(
                        Modifier.fillMaxWidth(fraction).fillMaxHeight()
                            .background(tint, RoundedCornerShape(2.dp)),
                    )
                }
            },
        )
        Text(
            "VOL ${value.formatDb()} dB",
            modifier = Modifier.testTag("mixer-master-volume-readout"),
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
        )
    }
}

@Composable
private fun BipolarPanSlider(
    value: Float, accent: Color, enabled: Boolean,
    onValueChange: (Float) -> Unit, onValueChangeFinished: () -> Unit, contentDescription: String,
) {
    DenseMixerSlider("PAN", value.formatPan(), value, -1f..1f, accent, enabled,
        onValueChange = { raw -> onValueChange((if (abs(raw) <= 0.035f) 0f else raw).coerceIn(-1f, 1f)) },
        onValueChangeFinished = onValueChangeFinished, contentDescription = contentDescription, bipolar = true)
}

/** Keep Material Slider gesture/keyboard/accessibility behavior; only replace its bulky drawing. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DenseMixerSlider(
    label: String, valueLabel: String, value: Float, range: ClosedFloatingPointRange<Float>,
    accent: Color, enabled: Boolean, onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit, contentDescription: String, bipolar: Boolean = false,
) {
    Row(Modifier.fillMaxWidth().height(48.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.width(54.dp * LocalDensity.current.fontScale.coerceAtLeast(1f))) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(valueLabel, style = MaterialTheme.typography.labelSmall, maxLines = 1)
        }
        val tint = if (enabled) accent else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
        Slider(
            value = value, onValueChange = onValueChange, onValueChangeFinished = onValueChangeFinished,
            valueRange = range, enabled = enabled,
            // Material Slider expands horizontal semantics by 10dp per side. Reserve that
            // inside the channel so its accessible target remains reachable at either scroll end.
            modifier = Modifier.weight(1f).height(48.dp).padding(horizontal = 10.dp)
                .semantics { this.contentDescription = contentDescription },
            thumb = {
                // Slider measures its interactive height from the thumb layout. Keep that 48dp
                // while centering the original compact 20dp drawing inside it.
                Box(Modifier.size(width = 6.dp, height = 48.dp), contentAlignment = Alignment.Center) {
                    Box(Modifier.size(width = 6.dp, height = 20.dp).background(tint, RoundedCornerShape(3.dp)))
                }
            },
            track = {
                BoxWithConstraints(Modifier.fillMaxWidth().height(4.dp).background(MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(2.dp))) {
                    val fraction = ((value - range.start) / (range.endInclusive - range.start)).coerceIn(0f, 1f)
                    val start = if (bipolar) minOf(0.5f, fraction) else 0f
                    val width = if (bipolar) abs(fraction - 0.5f) else fraction
                    Box(Modifier.offset(x = maxWidth * start).width(maxWidth * width).fillMaxHeight().background(tint, RoundedCornerShape(2.dp)))
                    if (bipolar) Box(Modifier.offset(x = maxWidth / 2 - 1.dp).width(2.dp).height(4.dp).background(MaterialTheme.colorScheme.onSurface))
                }
            },
        )
    }
}

@Composable
private fun CompactMeter(meter: MeterBallisticsState, accent: Color) {
    Row(Modifier.fillMaxWidth().height(16.dp).semantics {
        contentDescription = "Peak ${meter.peak.formatDbfs()} dBFS, RMS ${meter.rms.formatDbfs()} dBFS"
    }, verticalAlignment = Alignment.CenterVertically) {
        BoxWithConstraints(Modifier.fillMaxWidth().height(4.dp).background(MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))) {
            Box(Modifier.fillMaxWidth(meter.peak.coerceIn(0f, 1f)).fillMaxHeight().background(accent))
            val heldX = (maxWidth * meter.heldPeak.coerceIn(0f, 1f)).coerceAtMost(maxWidth - 2.dp)
            Box(Modifier.offset(x = heldX).width(2.dp).fillMaxHeight().background(MaterialTheme.colorScheme.onSurface))
        }
    }
}

@Composable
private fun MeterPair(
    meter: MeterBallisticsState,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier.fillMaxWidth().height(48.dp * LocalDensity.current.fontScale.coerceAtLeast(1f)),
        verticalArrangement = Arrangement.Center,
    ) {
        MeterRow("PK", meter.peak, accent, meter.heldPeak)
        MeterRow("RMS", meter.rms, accent)
    }
}

@Composable
private fun MeterRow(label: String, value: Float, accent: Color, heldPeak: Float? = null) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(label, modifier = Modifier.width(28.dp * LocalDensity.current.fontScale.coerceAtLeast(1f)), style = MaterialTheme.typography.labelSmall)
        BoxWithConstraints(
            Modifier.weight(1f).height(8.dp).background(MaterialTheme.colorScheme.background.copy(alpha = 0.78f), RoundedCornerShape(4.dp)),
        ) {
            Box(Modifier.fillMaxWidth(value.coerceIn(0f, 1f)).fillMaxHeight().background(accent, RoundedCornerShape(4.dp)))
            heldPeak?.takeIf { it > 0f }?.let { held ->
                val x = (maxWidth * held.coerceIn(0f, 1f)).coerceAtMost(maxWidth - 2.dp)
                Box(Modifier.offset(x = x).width(2.dp).fillMaxHeight().background(MaterialTheme.colorScheme.onSurface))
            }
        }
        Text(value.formatDbfs(), modifier = Modifier.width(28.dp * LocalDensity.current.fontScale.coerceAtLeast(1f)), style = MaterialTheme.typography.labelSmall)
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
