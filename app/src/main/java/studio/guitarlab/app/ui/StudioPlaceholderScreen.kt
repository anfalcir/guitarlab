package studio.guitarlab.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun StudioPlaceholderScreen(projectId: String, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(28.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text("Studio", style = MaterialTheme.typography.headlineMedium)
                Text("Project ${projectId.take(8)}…", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            OutlinedButton(onClick = onBack) { Text("Home") }
        }

        Surface(shape = RoundedCornerShape(18.dp), tonalElevation = 1.dp, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("M2 hardware gate in progress", style = MaterialTheme.typography.titleLarge)
                Text("Project lifecycle and Guitar Template are active. USB input/output diagnostics are now available under Settings → Audio Diagnostics. Timeline/audio production features remain gated until the real-device M2 probe passes.")
            }
        }
    }
}
