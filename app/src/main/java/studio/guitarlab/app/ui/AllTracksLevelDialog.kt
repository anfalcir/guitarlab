package studio.guitarlab.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import studio.guitarlab.core.model.AudioTrack
import studio.guitarlab.core.model.GuitarProject
import studio.guitarlab.core.project.ActiveTakePolicy
import studio.guitarlab.core.project.LevelAnalysis

@Composable
internal fun AllTracksLevelDialog(
    project: GuitarProject,
    analyses: Map<String, LevelAnalysis>,
    busyTrackIds: Set<String>,
    onAnalyzeTrack: (String) -> Unit,
    onAnalyzeAll: () -> Unit,
    onApplyTrack: (String) -> Unit,
    onApplyAll: () -> Unit,
    onDismiss: () -> Unit,
) {
    val tracks = project.tracks.sortedBy { it.order }
    val audibleCounts = tracks.associate { track ->
        track.id to ActiveTakePolicy.audibleClips(project).count { it.trackId == track.id && !it.muted }
    }
    val actionable = tracks.count { track -> analyses[track.id]?.isActionableLevelSuggestion() == true }
    val anyBusy = busyTrackIds.isNotEmpty()

    AlertDialog(
        modifier = Modifier.testTag("all-levels-dialog"),
        onDismissRequest = { if (!anyBusy) onDismiss() },
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("Níveis das pistas")
                Text(
                    "Análise assistida global",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Analise o nível efetivo de todas as pistas audíveis. Você pode aplicar cada sugestão separadamente ou aplicar todas em uma única edição, preservando um único passo de Undo.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedButton(
                        onClick = onAnalyzeAll,
                        enabled = !anyBusy && audibleCounts.values.any { it > 0 },
                        modifier = Modifier.weight(1f).testTag("analyze-all-levels"),
                        shape = RoundedCornerShape(6.dp),
                    ) { Text(if (anyBusy) "Analisando…" else "Analisar todas") }
                    Button(
                        onClick = onApplyAll,
                        enabled = !anyBusy && actionable > 0,
                        modifier = Modifier.weight(1f).testTag("apply-all-levels"),
                        shape = RoundedCornerShape(6.dp),
                    ) { Text("Aplicar sugestões ($actionable)") }
                }
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 430.dp).testTag("all-levels-list"),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(tracks, key = { it.id }) { track ->
                        LevelTrackRow(
                            track = track,
                            audibleClipCount = audibleCounts[track.id] ?: 0,
                            analysis = analyses[track.id],
                            busy = track.id in busyTrackIds,
                            globalBusy = anyBusy,
                            onAnalyze = { onAnalyzeTrack(track.id) },
                            onApply = { onApplyTrack(track.id) },
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss, enabled = !anyBusy) { Text("Fechar") }
        },
    )
}

@Composable
private fun LevelTrackRow(
    track: AudioTrack,
    audibleClipCount: Int,
    analysis: LevelAnalysis?,
    busy: Boolean,
    globalBusy: Boolean,
    onAnalyze: () -> Unit,
    onApply: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth().testTag("all-level-row-${track.id}"),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.24f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.22f)),
    ) {
        Column(Modifier.padding(horizontal = 10.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(track.name, style = MaterialTheme.typography.labelLarge, maxLines = 1)
                    Text(
                        "Ganho atual ${track.gainDb.formatSignedDb()} · $audibleClipCount ${if (audibleClipCount == 1) "clipe audível" else "clipes audíveis"}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (busy) CircularProgressIndicator(modifier = Modifier.padding(start = 8.dp), strokeWidth = 2.dp)
            }

            val status = when {
                audibleClipCount == 0 -> "Sem áudio audível para análise."
                busy -> "Analisando…"
                analysis == null -> "Ainda não analisada."
                analysis.silent -> "Silêncio detectado."
                else -> "PK ${"%.1f".format(analysis.peakDbfs)} dBFS · RMS ${"%.1f".format(analysis.rmsDbfs)} dBFS · ajuste ${analysis.recommendedGainDb.formatSignedDb()}"
            }
            Text(status, style = MaterialTheme.typography.bodySmall)

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(
                    onClick = onAnalyze,
                    enabled = audibleClipCount > 0 && !globalBusy,
                    shape = RoundedCornerShape(6.dp),
                ) { Text(if (analysis == null) "Analisar" else "Reanalisar") }
                if (analysis?.isActionableLevelSuggestion() == true) {
                    Button(onClick = onApply, enabled = !globalBusy, shape = RoundedCornerShape(6.dp)) { Text("Aplicar") }
                } else if (analysis != null && !analysis.silent) {
                    Text("Dentro do alvo", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
                }
            }
        }
    }
}

private fun LevelAnalysis.isActionableLevelSuggestion(): Boolean = !silent && abs(recommendedGainDb) >= 0.1f
private fun Float.formatSignedDb(): String = if (this >= 0f) "+%.1f dB".format(this) else "%.1f dB".format(this)
