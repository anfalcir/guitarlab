package studio.guitarlab.app.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import studio.guitarlab.core.audio.MonitoringMode
import studio.guitarlab.core.codec.AudioImportFormatPolicy
import studio.guitarlab.platform.codec.android.MasterExportFormat

@Composable
fun SettingsScreen(
    projectId: String? = null,
    onBack: () -> Unit,
    onAudioDiagnostics: () -> Unit,
    onCodecDiagnostics: () -> Unit,
    viewModel: StudioViewModel = viewModel(),
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsState()
    LaunchedEffect(projectId) { projectId?.let(viewModel::load) }
    val projectName = state.project?.name?.replace(Regex("[^A-Za-z0-9._ -]"), "_")?.trim()?.ifBlank { "GuitarLab" } ?: "GuitarLab"
    val projectLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri ->
        if (uri != null) viewModel.exportProjectPackage(uri)
    }
    val wavLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("audio/wav")) { uri ->
        if (uri != null) viewModel.exportMaster(uri, MasterExportFormat.WAV_FLOAT32)
    }
    val flacLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("audio/flac")) { uri ->
        if (uri != null) viewModel.exportMaster(uri, MasterExportFormat.FLAC)
    }
    val mp3Launcher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("audio/mpeg")) { uri ->
        if (uri != null) viewModel.exportMaster(uri, MasterExportFormat.MP3)
    }
    val routingStore = remember(context) { StudioAudioRoutingStore(context) }
    var inputChoices by remember { mutableStateOf(routingStore.inputChoices()) }
    var outputChoices by remember { mutableStateOf(routingStore.outputChoices()) }
    var selectedInput by remember { mutableStateOf(routingStore.selectedInputSignature()) }
    var selectedOutput by remember { mutableStateOf(routingStore.selectedOutputSignature()) }
    var monitoringMode by remember { mutableStateOf(routingStore.monitoringMode()) }

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
                    subtitle = "Entrada, saída e monitoramento usados pelo Studio.",
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
                    MonitoringSelector(
                        mode = monitoringMode,
                        onSelect = { mode ->
                            monitoringMode = mode
                            routingStore.selectMonitoringMode(mode)
                        },
                    )
                    OptionRow("Taxa de amostragem", "Automática", "Durante a gravação o Studio respeita a taxa já estabelecida pelo projeto")
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
                    title = "Salvar e exportar",
                    subtitle = "Projeto editável e master final no mesmo fluxo de saída.",
                ) {
                    OptionRow("Projeto GuitarLab", ".guitarlab", "Pacote portátil com project.json, fontes originais e proxies necessários")
                    OutlinedButton(
                        onClick = { projectLauncher.launch("$projectName.guitarlab") },
                        enabled = projectId != null && state.project != null && !state.exporting,
                    ) { Text("Salvar cópia do projeto") }
                    OptionRow("WAV master", "32-bit float", "Máxima qualidade para arquivo, edição ou masterização posterior")
                    OptionRow("FLAC master", "Lossless", "Compactação sem perdas; encoder do dispositivo é validado no momento da exportação")
                    OptionRow("MP3 master", "320 kbps", "Arquivo prático para compartilhamento")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { wavLauncher.launch("$projectName-master.wav") }, enabled = state.project != null && !state.exporting) { Text("WAV") }
                        OutlinedButton(onClick = { flacLauncher.launch("$projectName-master.flac") }, enabled = state.project != null && !state.exporting) { Text("FLAC") }
                        OutlinedButton(onClick = { mp3Launcher.launch("$projectName-master.mp3") }, enabled = state.project != null && !state.exporting) { Text("MP3") }
                    }
                    state.exportStatus?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
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
                    subtitle = "A fonte original é preservada; formatos não-WAV recebem proxy PCM de edição regenerável.",
                ) {
                    OptionRow("Importar áudio", AudioImportFormatPolicy.supportedExtensionsDescription, "WAV/FLAC/AIFF/MP3/AAC-M4A/OGG/Opus")
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
private fun MonitoringSelector(mode: MonitoringMode, onSelect: (MonitoringMode) -> Unit) {
    var menuOpen by remember { mutableStateOf(false) }
    val label = when (mode) {
        MonitoringMode.OFF -> "Desligado"
        MonitoringMode.AUTO -> "Automático"
        MonitoringMode.ON -> "Ligado"
    }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("Monitoramento de entrada", style = MaterialTheme.typography.bodyMedium)
                Text(
                    when (mode) {
                        MonitoringMode.OFF -> "Não envia a entrada de volta para a saída pelo app"
                        MonitoringMode.AUTO -> "Evita retorno duplicado em interfaces USB e ativa somente em rotas seguras"
                        MonitoringMode.ON -> "Força o retorno da entrada pela saída principal"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Box(Modifier.padding(start = 12.dp)) {
                OutlinedButton(onClick = { menuOpen = true }) { Text(label) }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    MonitoringMode.entries.forEach { candidate ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    when (candidate) {
                                        MonitoringMode.OFF -> "Desligado"
                                        MonitoringMode.AUTO -> "Automático"
                                        MonitoringMode.ON -> "Ligado"
                                    }
                                )
                            },
                            onClick = {
                                menuOpen = false
                                onSelect(candidate)
                            },
                        )
                    }
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
                    DropdownMenuItem(text = { Text("Automático") }, onClick = { menuOpen = false; onSelect(null) })
                    choices.forEach { choice ->
                        DropdownMenuItem(text = { Text(choice.label) }, onClick = { menuOpen = false; onSelect(choice.signature) })
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
