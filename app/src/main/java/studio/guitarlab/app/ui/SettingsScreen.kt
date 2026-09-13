package studio.guitarlab.app.ui

import android.Manifest
import android.content.pm.PackageManager
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import studio.guitarlab.core.audio.MonitoringMode
import studio.guitarlab.core.codec.AudioImportFormatPolicy

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
    var monitoringMode by remember { mutableStateOf(routingStore.monitoringMode()) }
    val latencyStore = remember(context) { StudioLatencyCalibrationStore(context) }
    val latencyEngine = remember(context) { AndroidLatencyCalibrationEngine(context) }
    val scope = rememberCoroutineScope()
    var calibrating by remember { mutableStateOf(false) }
    var calibrationStatus by remember { mutableStateOf<String?>(null) }
    var calibrationProgress by remember { mutableStateOf(0 to 0) }
    var pendingCalibration by remember { mutableStateOf(false) }

    fun runLatencyCalibration() {
        if (calibrating) return
        if (selectedInput.isNullOrBlank() || selectedOutput.isNullOrBlank()) {
            calibrationStatus = "Selecione explicitamente a entrada e a saída antes de calibrar."
            return
        }
        calibrating = true
        calibrationStatus = "Preparando medição de loopback…"
        scope.launch {
            runCatching {
                latencyEngine.calibrate(
                    sampleRateHz = 48_000,
                    inputDevice = routingStore.resolveSelectedInputDevice(),
                    outputDevice = routingStore.resolveSelectedOutputDevice(),
                    onProgress = { current, total -> calibrationProgress = current to total },
                )
            }.onSuccess { result ->
                latencyStore.save(selectedInput, selectedOutput, result)
                calibrationStatus = if (result.accepted) "Calibração válida · ${result.describe()}" else "Medição instável · ${result.describe()} · repita antes de usar compensação"
            }.onFailure { error ->
                calibrationStatus = error.message ?: "Não foi possível medir a latência."
            }
            calibrating = false
        }
    }

    val latencyPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted && pendingCalibration) runLatencyCalibration()
        if (!granted) calibrationStatus = "Permissão de microfone necessária para calibrar."
        pendingCalibration = false
    }

    LaunchedEffect(Unit) {
        inputChoices = routingStore.inputChoices()
        outputChoices = routingStore.outputChoices()
    }

    fun refreshAudioDevices() {
        inputChoices = routingStore.inputChoices()
        outputChoices = routingStore.outputChoices()
        if (selectedInput != null && inputChoices.none { it.signature == selectedInput }) selectedInput = null
        if (selectedOutput != null && outputChoices.none { it.signature == selectedOutput }) selectedOutput = null
    }

    val routeHealth = routingStore.routeHealth()

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
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.24f),
                    ) {
                        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text("Rota efetiva", style = MaterialTheme.typography.titleSmall)
                            Text("Entrada: ${routeHealth.effectiveInput?.let(::audioCapabilities) ?: if (selectedInput == null) "Automática" else "Selecionada, mas indisponível"}", style = MaterialTheme.typography.bodySmall)
                            Text("Saída: ${routeHealth.effectiveOutput?.let(::audioCapabilities) ?: if (selectedOutput == null) "Automática" else "Selecionada, mas indisponível"}", style = MaterialTheme.typography.bodySmall)
                            if (routeHealth.mk300Detected) Text("MK-300 detectada", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                            if (!routeHealth.selectedInputAvailable) Text("A gravação será bloqueada: não haverá fallback silencioso para o microfone.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                        }
                    }
                    OptionRow("Taxa de amostragem", "Automática", "Durante a gravação o Studio respeita a taxa já estabelecida pelo projeto")
                    OptionRow(
                        "Compensação de latência",
                        latencyStore.find(selectedInput, selectedOutput, 48_000)?.takeIf { it.accepted }?.describe() ?: "Não calibrada",
                        "M6 usa medição round-trip por loopback, vinculada à combinação entrada/saída. Uma calibração instável nunca é aplicada automaticamente.",
                    )
                    Text(
                        "Para calibrar, conecte ou ative um retorno físico da saída para a entrada. O GuitarLab executa várias medições, estima jitter/drift e só aceita resultados estáveis.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    calibrationStatus?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                    if (calibrating) {
                        Text("Medição ${calibrationProgress.first}/${calibrationProgress.second.coerceAtLeast(1)}…", style = MaterialTheme.typography.bodySmall)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            enabled = !calibrating,
                            onClick = {
                                if (context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                                    runLatencyCalibration()
                                } else {
                                    pendingCalibration = true
                                    latencyPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                }
                            },
                        ) { Text(if (calibrating) "Medindo…" else "Calibrar latência") }
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
                    title = "Projeto e Studio",
                    subtitle = "Preferências que não precisam ocupar a área de edição.",
                ) {
                    OptionRow("Salvamento automático", "Ativo", "Alterações do projeto são salvas automaticamente")
                    OptionRow("Mixer", "Flutuante ou fixo", "O estado fixado é mantido ao navegar pelo aplicativo")
                    OptionRow("Novo projeto", "Perguntar sempre", "Projeto vazio ou template de guitarra")
                    if (projectId != null) {
                        Text(
                            "Para salvar uma cópia portátil do projeto ou exportar o master, use o botão Compartilhar na barra superior do Studio.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
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

private fun audioCapabilities(choice: StudioAudioDeviceChoice): String = buildString {
    append(choice.label)
    if (choice.channelCounts.isNotEmpty()) append(" · ${choice.channelCounts.joinToString("/")} canais")
    if (choice.sampleRates.isNotEmpty()) append(" · ${choice.sampleRates.joinToString("/")} Hz")
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
                        MonitoringMode.OFF -> "Não envia a entrada de volta para a saída pelo app; nunca controla o conteúdo gravado"
                        MonitoringMode.AUTO -> "Evita retorno duplicado em interfaces USB; nunca mistura backing no arquivo gravado"
                        MonitoringMode.ON -> "Força o retorno da entrada pela saída, sem misturar backing no arquivo gravado"
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
