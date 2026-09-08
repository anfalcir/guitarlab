package studio.guitarlab.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onAudioDiagnostics: () -> Unit
) {
    Column(Modifier.fillMaxSize().padding(28.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Settings", style = MaterialTheme.typography.headlineMedium)
        Text("Audio", style = MaterialTheme.typography.titleMedium)
        Text("Device: Auto\nSample rate: Auto / follow device\nInternal processing: 32-bit float")
        Button(onClick = onAudioDiagnostics) { Text("Audio Diagnostics") }
        Text("Projects", style = MaterialTheme.typography.titleMedium)
        Text("Autosave: On\nDefault new project: Ask every time")
        OutlinedButton(onClick = onBack) { Text("Back") }
    }
}
