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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

@Composable
fun SettingsScreen(
    projectId: String? = null,
    onBack: () -> Unit,
    onAudioDiagnostics: () -> Unit,
    onCodecDiagnostics: () -> Unit,
) {
    val context = LocalContext.current
    val routingStore = remember(context) { StudioAudioRoutingStore(context) }
    var inputChoices by remember { mutableStateOf(routingStore.inputChoices()) }
    var outputChoices by remember { mutableStateOf(routingStore.outputChoices()) }
    var selectedInput by remember { mutableStateOf(routingStore.selectedInputSignature()) }
    var selectedOutput by remember { mutableStateOf(routingStore.selectedOutputSignature()) }

    fun refreshAudioDevices() {
        inputChoices = routingStore.inputChoices()
        outputChoices = routingStore.outputChoices()
        if (selectedInput != null && inputChoices.none { it.signature == selectedInput }) selectedInput = null
        if (selectedOutput != null && outputChoices.none { it.signature == selectedOutput }) selectedOutput = null
    }

    Column(Modifier.fillMaxSize().padding(horizontal = 22.dp, vertical = 16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("Opções", style = MaterialTheme.typography.headlineMedium)
                Text(
                    if (projectId == null) "Preferências do aplicativo" else "Áudio, projeto e ferramentas do Studio",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            AppIconButton(icon = Icons.Default.ArrowBack, contentDescription = "Voltar", onClick = onBack)
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                OptionSection(
                    title = "Áudio",
                    subtitle = "Escolha a entrada de gravação e a saída principal do Studio.",
                ) {
                    AudioDeviceSelector(
                        title = "Entrada de gravação",
                        selectedSignature = selectedInput,
                        choices = inputChoices,
                        onSelect = { signature ->
                            selectedInput = signature
                            routingStore.selectInput(signature)
                        },
                    )
                    AudioDeviceSelector(
                        title = "Saída principal",
                        selectedSignature = selectedOutput,
                        choices = outputChoices,
                        onSelect = { signature ->
                            selectedOutput = signature
                            routingStore.selectOutput(signature)
                        },
                    )
                    OptionRow("Taxa de amostragem", "Automática", "Usa a configuração do projeto quando definida")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = ::refreshAudioDevices) {
                            Icon(Icons.Default.Refresh, contentDescription = null)
                            Text("Atualizar", modifier = Modifier.padding(start = 6.dp))
                        }
                        Button(onClick = onAudioDiagnostics) { Text("Diagnóstico de áudio") }
                    }
                }
            }

            item {
                OptionSection(
                    title = "Exportação",
                    subtitle = "As opções de exportação ficam centralizadas aqui.",
                ) {
                    OptionRow("Mix final", "Em breve", "Formato, qualidade, taxa de amostragem e intervalo")
                    OptionRow("Pistas separadas", "Em breve", "Exportação individual das pistas")
                    OptionRow("Pacote do projeto", "Em breve", "Projeto portátil com mídias")
                    OutlinedButton(onClick = { }, enabled = false) { Text("Exportar projeto") }
                }
            }

            item {
                OptionSection(
                    title = "Projeto e Studio",
                    subtitle = "Preferências que não precisam ocupar a área de edição.",
                ) {
                    OptionRow("Salvamento automático", "Ativo", "Alterações do projeto são salvas automaticamente")
                    OptionRow("Mixer", "Flutuante ou fixo", "O estado fixado é mantido ao navegar pelo aplicativo")
                    OptionRow("Novo projeto", "Perguntar sempre", "Projeto vazio ou template de guitarra")
                }
            }

            item {
                OptionSection(
                    title = "Importação",
                    subtitle = "Formatos de áudio disponíveis no Studio.",
                ) {
                    OptionRow("Importar áudio", "WAV", "Outros formatos serão habilitados conforme suporte completo")
                    Button(onClick = onCodecDiagnostics) { Text("Diagnóstico de arquivos") }
                }
            }

            item {
                OptionSection(
                    title = "Ferramentas avançadas",
                    subtitle = "Diagnósticos técnicos para suporte e solução de problemas.",
                ) {
                    TextButton(onClick = onAudioDiagnostics) { Text("Diagnóstico de dispositivos de áudio") }
                    TextButton(onClick = onCodecDiagnostics) { Text("Diagnóstico de codecs e arquivos") }
                }
            }
        }
    }
}

@Composable
private fun AudioDeviceSelector(
    title: String,
    selectedSignature: String?,
    choices: List<StudioAudioDeviceChoice>,
    onSelect: (String?) -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    val selectedLabel = choices.firstOrNull { it.signature == selectedSignature }?.label ?: "Automático"
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
                Text(
                    if (selectedSignature == null) "O Android escolhe a rota ativa" else "Dispositivo preferido",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Box(Modifier.padding(start = 12.dp)) {
                OutlinedButton(onClick = { menuOpen = true }) { Text(selectedLabel, maxLines = 1) }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(
                        text = { Text("Automático") },
                        onClick = {
                            menuOpen = false
                            onSelect(null)
                        },
                    )
                    choices.forEach { choice ->
                        DropdownMenuItem(
                            text = { Text(choice.label) },
                            onClick = {
                                menuOpen = false
                                onSelect(choice.signature)
                            },
                        )
                    }
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
                Text(value, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.secondary)
            }
        }
    }
}
