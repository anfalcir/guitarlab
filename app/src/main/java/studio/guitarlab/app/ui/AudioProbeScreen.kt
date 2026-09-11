package studio.guitarlab.app.ui

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlin.math.roundToInt
import studio.guitarlab.core.audio.AudioDeviceDescriptor
import studio.guitarlab.core.audio.AudioProbeOperation
import studio.guitarlab.core.audio.AudioProbeResult
import studio.guitarlab.core.audio.PcmEncoding

@Composable
fun AudioProbeScreen(
    onBack: () -> Unit,
    viewModel: AudioProbeViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        viewModel.onPermissionStateChanged(granted)
    }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(28.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text("Diagnóstico de áudio", style = MaterialTheme.typography.headlineMedium)
                Text("Dispositivos, rotas e estabilidade", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            AppIconButton(icon = Icons.Default.ArrowBack, contentDescription = "Voltar", enabled = state.running == null, onClick = onBack)
        }

        Surface(shape = RoundedCornerShape(16.dp), tonalElevation = 1.dp, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Segurança", style = MaterialTheme.typography.titleMedium)
                Text("Os testes de saída usam um sinal de baixo nível. Comece com o volume do fone ou Master baixo.")
                if (!state.permissionGranted) {
                    Text("Os testes de entrada precisam de permissão para gravar áudio.", color = MaterialTheme.colorScheme.error)
                    Button(onClick = { permissionLauncher.launch(Manifest.permission.RECORD_AUDIO) }) {
                        Text("Permitir gravação de áudio")
                    }
                }
            }
        }

        DeviceSelector("Dispositivo de entrada", state.inputs, state.selectedInputKey, viewModel::selectInput)
        DeviceSelector("Dispositivo de saída", state.outputs, state.selectedOutputKey, viewModel::selectOutput)

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(onClick = viewModel::runPlayback, enabled = state.running == null && state.outputs.isNotEmpty()) { Text("Testar saída") }
            Button(onClick = viewModel::runRecord, enabled = state.running == null && state.permissionGranted && state.inputs.isNotEmpty()) { Text("Testar entrada") }
            Button(
                onClick = viewModel::runDuplex,
                enabled = state.running == null && state.permissionGranted && state.inputs.isNotEmpty() && state.outputs.isNotEmpty(),
            ) { Text("Testar duplex") }
            OutlinedButton(onClick = viewModel::stop, enabled = state.running != null && !state.stopping) { Text(if (state.stopping) "Parando…" else "Parar") }
        }

        state.running?.let { running ->
            Text(
                if (state.stopping) "Parando teste de ${running.label()}…" else "Executando teste de ${running.label()}…",
                color = MaterialTheme.colorScheme.primary,
            )
        }
        state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        state.lastResult?.let { ProbeResultCard(it) }

        Surface(shape = RoundedCornerShape(16.dp), tonalElevation = 1.dp, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Eventos de dispositivos", style = MaterialTheme.typography.titleMedium)
                if (state.eventLog.isEmpty()) Text("Nenhum evento ainda.")
                state.eventLog.takeLast(12).forEach { Text(it, style = MaterialTheme.typography.bodySmall) }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(onClick = viewModel::refresh, enabled = state.running == null) {
                        Icon(Icons.Default.Refresh, contentDescription = null)
                        Text("Atualizar", modifier = Modifier.padding(start = 6.dp))
                    }
                    OutlinedButton(
                        onClick = {
                            val clipboard = context.getSystemService(ClipboardManager::class.java)
                            clipboard?.setPrimaryClip(ClipData.newPlainText("Diagnóstico de áudio GuitarLab", viewModel.diagnosticsReport()))
                        },
                        enabled = state.devices.isNotEmpty(),
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null)
                        Text("Copiar relatório", modifier = Modifier.padding(start = 6.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun DeviceSelector(
    title: String,
    devices: List<AudioDeviceDescriptor>,
    selectedKey: String?,
    onSelect: (String?) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val selected = devices.firstOrNull { it.key == selectedKey }

    Surface(shape = RoundedCornerShape(16.dp), tonalElevation = 1.dp, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            OutlinedButton(onClick = { expanded = true }, enabled = devices.isNotEmpty()) {
                Text(selected?.let { "${it.name} • ${it.typeLabel}" } ?: "Nenhum dispositivo compatível")
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                devices.forEach { device ->
                    DropdownMenuItem(
                        text = { Text("${device.name} • ${device.typeLabel}") },
                        onClick = {
                            expanded = false
                            onSelect(device.key)
                        },
                    )
                }
            }
            selected?.let { DeviceDetails(it) }
        }
    }
}

@Composable
private fun DeviceDetails(device: AudioDeviceDescriptor) {
    val rates = device.sampleRatesHz.takeIf { it.isNotEmpty() }?.joinToString { "$it Hz" } ?: "negociado pelo sistema"
    val channels = device.channelCounts.takeIf { it.isNotEmpty() }?.joinToString() ?: "negociado pelo sistema"
    val encodings = device.encodings.takeIf { it.isNotEmpty() }?.joinToString { it.label() } ?: "negociado pelo sistema"
    Text("Conexão: ${device.transport}", style = MaterialTheme.typography.bodySmall)
    Text("Taxas: $rates", style = MaterialTheme.typography.bodySmall)
    Text("Canais: $channels", style = MaterialTheme.typography.bodySmall)
    Text("PCM: $encodings", style = MaterialTheme.typography.bodySmall)
}

@Composable
private fun ProbeResultCard(result: AudioProbeResult) {
    Surface(shape = RoundedCornerShape(16.dp), tonalElevation = 2.dp, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                "${result.operation.label()} • ${when { result.stopped -> "PARADO"; result.success -> "APROVADO"; else -> "FALHA" }}",
                style = MaterialTheme.typography.titleMedium,
                color = if (result.success) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
            )
            Text(result.message)
            Text("Tempo: ${result.elapsedMs} ms", style = MaterialTheme.typography.bodySmall)
            result.inputConfig?.let {
                Text("Entrada: ${it.sampleRateHz} Hz • ${it.channelCount} canal(is) • ${it.encoding.label()} • ${it.bufferFrames} frames", style = MaterialTheme.typography.bodySmall)
                Text("Rota de entrada: solicitada ${it.deviceKey ?: "automática"} • ativa ${it.routedDeviceKey ?: "desconhecida"}", style = MaterialTheme.typography.bodySmall)
            }
            result.outputConfig?.let {
                Text("Saída: ${it.sampleRateHz} Hz • ${it.channelCount} canal(is) • ${it.encoding.label()} • ${it.bufferFrames} frames", style = MaterialTheme.typography.bodySmall)
                Text("Rota de saída: solicitada ${it.deviceKey ?: "automática"} • ativa ${it.routedDeviceKey ?: "desconhecida"}", style = MaterialTheme.typography.bodySmall)
            }
            result.inputStats?.let {
                Text("Capturado: ${it.frames} frames • pico ${(it.peak * 100).roundToInt()}% • RMS ${(it.rms * 100).roundToInt()}%", style = MaterialTheme.typography.bodySmall)
            }
            if (result.outputFrames > 0) Text("Frames de saída: ${result.outputFrames}", style = MaterialTheme.typography.bodySmall)
            Text("Underruns de saída: ${result.outputUnderruns}", style = MaterialTheme.typography.bodySmall)
            result.warnings.forEach { Text("Aviso: $it", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
        }
    }
}

private fun AudioProbeOperation.label(): String = when (this) {
    AudioProbeOperation.PLAYBACK -> "saída"
    AudioProbeOperation.RECORD -> "entrada"
    AudioProbeOperation.DUPLEX -> "duplex"
}

private fun PcmEncoding.label(): String = when (this) {
    PcmEncoding.FLOAT_32 -> "float 32 bits"
    PcmEncoding.PCM_16 -> "PCM 16 bits"
}
