package studio.guitarlab.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun SettingsScreen(
    projectId: String? = null,
    onBack: () -> Unit,
    onAudioDiagnostics: () -> Unit,
    onCodecDiagnostics: () -> Unit,
) {
    Column(Modifier.fillMaxSize().padding(horizontal = 22.dp, vertical = 16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("Options", style = MaterialTheme.typography.headlineMedium)
                Text(
                    if (projectId == null) "App setup and tools" else "Studio setup, project commands and tools",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            OutlinedButton(onClick = onBack) { Text("Back") }
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                OptionSection(
                    title = "Audio I/O",
                    subtitle = "One place for recording input, monitoring and main output routing.",
                ) {
                    OptionRow("Recording input", "Auto / selected audio interface", "Global for the Studio; not configured per track")
                    OptionRow("Main output", "Auto / selected output device", "Revalidated when USB devices reconnect")
                    OptionRow("Project sample rate", "Auto / project setting", "32-bit float internal processing")
                    Button(onClick = onAudioDiagnostics) { Text("Audio diagnostics") }
                }
            }

            item {
                OptionSection(
                    title = "Export",
                    subtitle = "Export commands live here instead of occupying the timeline.",
                ) {
                    OptionRow("Mix export", "WAV / FLAC / compressed targets", "Format, bit depth, sample rate and range will be chosen here")
                    OptionRow("Export tracks", "Planned", "Individual track export will use the same centralized workflow")
                    OptionRow("Project package", "Planned", "Portable GuitarLab project/media package")
                    OutlinedButton(onClick = { }, enabled = false) { Text("Export project") }
                    Text(
                        "Export remains disabled until the export engine is implemented and software-gated; the Studio will not expose a fake command.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            item {
                OptionSection(
                    title = "Project & Studio",
                    subtitle = "Low-frequency commands and preferences stay out of the editing canvas.",
                ) {
                    OptionRow("Autosave", "On", "Project edits are persisted automatically")
                    OptionRow("Mixer dock", "Show / hide / pin", "The mixer can be temporary or fixed at the bottom of the Studio")
                    OptionRow("New project behavior", "Ask every time", "Blank or Guitar template")
                }
            }

            item {
                OptionSection(
                    title = "Import & codecs",
                    subtitle = "Codec capability and technical validation tools.",
                ) {
                    OptionRow("Import", "WAV vertical slice", "Other V1 formats remain tracked by the codec support matrix")
                    Button(onClick = onCodecDiagnostics) { Text("Codec diagnostics") }
                }
            }

            item {
                OptionSection(
                    title = "Advanced / diagnostics",
                    subtitle = "Engineering tools stay available without polluting normal Studio workflows.",
                ) {
                    TextButton(onClick = onAudioDiagnostics) { Text("Open M2 audio diagnostics") }
                    TextButton(onClick = onCodecDiagnostics) { Text("Open M3 codec diagnostics") }
                }
            }
        }
    }
}

@Composable
private fun OptionSection(
    title: String,
    subtitle: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.30f),
        tonalElevation = 0.dp,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            content()
        }
    }
}

@Composable
private fun OptionRow(title: String, value: String, detail: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, style = MaterialTheme.typography.bodyMedium)
                Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Box(Modifier.padding(start = 12.dp)) {
                Text(value, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}
