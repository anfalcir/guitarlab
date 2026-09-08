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
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import studio.guitarlab.core.audio.AudioDeviceDescriptor
import studio.guitarlab.core.audio.AudioProbeResult
import studio.guitarlab.core.audio.PcmEncoding
import kotlin.math.roundToInt

@Composable
fun AudioProbeScreen(
    onBack: () -> Unit,
    viewModel: AudioProbeViewModel = viewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        viewModel.onPermissionStateChanged(granted)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(28.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text("Audio Diagnostics", style = MaterialTheme.typography.headlineMedium)
                Text("M2 USB Audio Probe", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            OutlinedButton(onClick = onBack, enabled = state.running == null) { Text("Back") }
        }

        Surface(shape = RoundedCornerShape(16.dp), tonalElevation = 1.dp, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Safety", style = MaterialTheme.typography.titleMedium)
                Text("Playback tests use a low-level diagnostic sine tone. Start with headphone/master volume low and raise only if needed.")
                if (!state.permissionGranted) {
                    Text("Input tests require Android audio-record permission.", color = MaterialTheme.colorScheme.error)
                    Button(onClick = { permissionLauncher.launch(Manifest.permission.RECORD_AUDIO) }) {
                        Text("Grant audio-record permission")
                    }
                }
            }
        }

        DeviceSelector(
            title = "Input device",
            devices = state.inputs,
            selectedKey = state.selectedInputKey,
            onSelect = viewModel::selectInput
        )
        DeviceSelector(
            title = "Output device",
            devices = state.outputs,
            selectedKey = state.selectedOutputKey,
            onSelect = viewModel::selectOutput
        )

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(onClick = viewModel::runPlayback, enabled = state.running == null && state.outputs.isNotEmpty()) { Text("Play test") }
            Button(onClick = viewModel::runRecord, enabled = state.running == null && state.permissionGranted && state.inputs.isNotEmpty()) { Text("Record test") }
            Button(
                onClick = viewModel::runDuplex,
                enabled = state.running == null && state.permissionGranted && state.inputs.isNotEmpty() && state.outputs.isNotEmpty()
            ) { Text("Duplex test") }
            OutlinedButton(onClick = viewModel::stop, enabled = state.running != null && !state.stopping) { Text(if (state.stopping) "Stopping…" else "Stop") }
        }

        if (state.running != null) {
            Text(
                if (state.stopping) "Stopping ${state.running!!.name.lowercase()} test…" else "Running ${state.running!!.name.lowercase()} test…",
                color = MaterialTheme.colorScheme.primary
            )
        }
        state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        state.lastResult?.let { ProbeResultCard(it) }

        Surface(shape = RoundedCornerShape(16.dp), tonalElevation = 1.dp, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Device event log", style = MaterialTheme.typography.titleMedium)
                if (state.eventLog.isEmpty()) Text("No events yet.")
                state.eventLog.takeLast(12).forEach { Text(it, style = MaterialTheme.typography.bodySmall) }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(onClick = viewModel::refresh, enabled = state.running == null) { Text("Refresh devices") }
                    OutlinedButton(
                        onClick = {
                            val clipboard = context.getSystemService(ClipboardManager::class.java)
                            clipboard?.setPrimaryClip(
                                ClipData.newPlainText("GuitarLab Audio Diagnostics", viewModel.diagnosticsReport())
                            )
                        },
                        enabled = state.devices.isNotEmpty()
                    ) { Text("Copy diagnostics") }
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
    onSelect: (String?) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selected = devices.firstOrNull { it.key == selectedKey }

    Surface(shape = RoundedCornerShape(16.dp), tonalElevation = 1.dp, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            OutlinedButton(onClick = { expanded = true }, enabled = devices.isNotEmpty()) {
                Text(selected?.let { "${it.name} • ${it.typeLabel}" } ?: "No compatible device")
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                devices.forEach { device ->
                    DropdownMenuItem(
                        text = { Text("${device.name} • ${device.typeLabel}") },
                        onClick = {
                            expanded = false
                            onSelect(device.key)
                        }
                    )
                }
            }
            selected?.let { DeviceDetails(it) }
        }
    }
}

@Composable
private fun DeviceDetails(device: AudioDeviceDescriptor) {
    val rates = device.sampleRatesHz.takeIf { it.isNotEmpty() }?.joinToString { "$it Hz" } ?: "system negotiated"
    val channels = device.channelCounts.takeIf { it.isNotEmpty() }?.joinToString() ?: "system negotiated"
    val encodings = device.encodings.takeIf { it.isNotEmpty() }?.joinToString { it.label() } ?: "system negotiated"
    Text("Transport: ${device.transport}", style = MaterialTheme.typography.bodySmall)
    Text("Rates: $rates", style = MaterialTheme.typography.bodySmall)
    Text("Channels: $channels", style = MaterialTheme.typography.bodySmall)
    Text("PCM: $encodings", style = MaterialTheme.typography.bodySmall)
}

@Composable
private fun ProbeResultCard(result: AudioProbeResult) {
    Surface(shape = RoundedCornerShape(16.dp), tonalElevation = 2.dp, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                "${result.operation} • ${when { result.stopped -> "STOPPED"; result.success -> "PASS"; else -> "FAIL" }}",
                style = MaterialTheme.typography.titleMedium,
                color = if (result.success) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
            )
            Text(result.message)
            Text("Elapsed: ${result.elapsedMs} ms", style = MaterialTheme.typography.bodySmall)
            result.inputConfig?.let {
                Text("Input: ${it.sampleRateHz} Hz • ${it.channelCount} ch • ${it.encoding.label()} • ${it.bufferFrames} frames", style = MaterialTheme.typography.bodySmall)
                Text("Input route: requested ${it.deviceKey ?: "auto"} • actual ${it.routedDeviceKey ?: "unknown"} • source ${it.inputSourceLabel ?: "unknown"}", style = MaterialTheme.typography.bodySmall)
            }
            result.outputConfig?.let {
                Text("Output: ${it.sampleRateHz} Hz • ${it.channelCount} ch • ${it.encoding.label()} • ${it.bufferFrames} frames", style = MaterialTheme.typography.bodySmall)
                Text("Output route: requested ${it.deviceKey ?: "auto"} • actual ${it.routedDeviceKey ?: "unknown"} • low-latency requested ${it.requestedLowLatency}", style = MaterialTheme.typography.bodySmall)
            }
            result.inputStats?.let {
                Text(
                    "Captured: ${it.frames} frames • peak ${(it.peak * 100).roundToInt()}% • RMS ${(it.rms * 100).roundToInt()}%",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            if (result.outputFrames > 0) Text("Output frames: ${result.outputFrames}", style = MaterialTheme.typography.bodySmall)
            Text("Output underruns: ${result.outputUnderruns}", style = MaterialTheme.typography.bodySmall)
            result.warnings.forEach { Text("Warning: $it", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
        }
    }
}

private fun PcmEncoding.label(): String = when (this) {
    PcmEncoding.FLOAT_32 -> "32-bit float"
    PcmEncoding.PCM_16 -> "16-bit PCM"
}
