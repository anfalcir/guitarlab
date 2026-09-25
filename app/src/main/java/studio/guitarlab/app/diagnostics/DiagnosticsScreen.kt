package studio.guitarlab.app.diagnostics

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import studio.guitarlab.app.BuildConfig
import studio.guitarlab.app.backup.BackupSettingsStore
import studio.guitarlab.app.ui.AppIconButton
import studio.guitarlab.app.ui.AppTransientFeedbackHost
import studio.guitarlab.app.ui.TransientFeedbackKind
import studio.guitarlab.app.ui.StudioAudioRoutingStore
import studio.guitarlab.core.project.FileProjectRepository
import studio.guitarlab.core.project.PreparedReferenceBindingPolicy
import studio.guitarlab.platform.separation.FileRemoteJobStore

@Composable
fun DiagnosticsScreen(
    projectId: String?,
    onBack: () -> Unit,
    onAudioDiagnostics: () -> Unit,
    onCodecDiagnostics: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val journal = remember(context) { DiagnosticJournal(context) }
    var events by remember { mutableStateOf(journal.readEvents()) }
    var status by remember { mutableStateOf<String?>(null) }
    var statusKind by remember { mutableStateOf(TransientFeedbackKind.ASYNC_COMPLETION) }
    var exporting by remember { mutableStateOf(false) }

    val project = remember(projectId) { projectId?.let { FileProjectRepository(context.filesDir).load(it) } }
    val routing = remember(context) { StudioAudioRoutingStore(context) }
    val routeHealth = routing.routeHealth()
    val backup = remember(context) { BackupSettingsStore(context).snapshot() }
    val jobs = remember(projectId) {
        FileRemoteJobStore(context).active()
            .filter { projectId == null || it.identity.projectId == projectId }
            .sortedByDescending { it.updatedAtMs }
            .take(12)
    }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/zip"),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        exporting = true
        status = null
        scope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(uri, "w")?.use { output ->
                        DiagnosticBundleExporter(context).export(projectId, output)
                    } ?: error("O Android não abriu o arquivo de destino.")
                }
            }.onSuccess { result ->
                statusKind = TransientFeedbackKind.ASYNC_COMPLETION
                status = "Pacote exportado com ${result.entryNames.size} arquivos de diagnóstico. Nenhum áudio foi incluído."
            }.onFailure { error ->
                statusKind = TransientFeedbackKind.ERROR
                status = error.message ?: "Não foi possível exportar o pacote de diagnóstico."
            }
            exporting = false
        }
    }

    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier.fillMaxSize().padding(horizontal = 22.dp, vertical = 16.dp).testTag("diagnostics-screen"),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text("Diagnóstico", style = MaterialTheme.typography.headlineMedium)
                Text(
                    "Estado técnico consolidado e pacote de suporte",
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
                DiagnosticSection("Aplicativo e dispositivo") {
                    DiagnosticLine("Versão", "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
                    DiagnosticLine("Android", "${android.os.Build.VERSION.RELEASE} · API ${android.os.Build.VERSION.SDK_INT}")
                    DiagnosticLine("Dispositivo", "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}")
                    DiagnosticLine("Armazenamento livre", "${context.filesDir.usableSpace / (1024 * 1024)} MiB")
                }
            }

            item {
                DiagnosticSection("Áudio e USB") {
                    DiagnosticLine("Entrada selecionada", routing.selectedInputDiagnosticIdentity() ?: "Automática")
                    DiagnosticLine("Saída selecionada", routing.selectedOutputDiagnosticIdentity() ?: "Automática")
                    DiagnosticLine("Entrada disponível", if (routeHealth.selectedInputAvailable) "Sim" else "Não")
                    DiagnosticLine("Saída disponível", if (routeHealth.selectedOutputAvailable) "Sim" else "Não")
                    DiagnosticLine("USB detectado", if (routeHealth.usbDeviceDetected) "Sim" else "Não")
                    DiagnosticLine("Monitoramento", routing.monitoringMode().name)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = onAudioDiagnostics) { Text("Áudio e dispositivos") }
                        OutlinedButton(onClick = onCodecDiagnostics) { Text("Codecs e arquivos") }
                    }
                }
            }

            item {
                DiagnosticSection("Separação em nuvem") {
                    if (jobs.isEmpty()) {
                        Text("Nenhum job local recente.", style = MaterialTheme.typography.bodySmall)
                    } else {
                        jobs.forEach { job ->
                            DiagnosticLine(
                                job.identity.jobId.take(12),
                                "${job.state.name} · manifest ${job.resultManifestSha256?.take(12) ?: "—"}",
                            )
                            job.errorCode?.let { Text(it.take(180), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error) }
                        }
                    }
                }
            }

            item {
                DiagnosticSection("Projeto atual") {
                    if (project == null) {
                        Text("Abra Diagnóstico a partir de um projeto para ver o grafo local.", style = MaterialTheme.typography.bodySmall)
                    } else {
                        DiagnosticLine("Projeto", project.name)
                        DiagnosticLine("Assets", project.assets.size.toString())
                        DiagnosticLine("Clipes", project.clips.size.toString())
                        DiagnosticLine("Bindings", project.referenceBindings.size.toString())
                        DiagnosticLine(
                            "Referências",
                            if (PreparedReferenceBindingPolicy.bindingDiffersFromDesired(project)) "Reparo local disponível" else "Consistentes",
                        )
                    }
                }
            }

            item {
                DiagnosticSection("Backup e nuvem") {
                    DiagnosticLine("Google Drive", if (backup.driveConnected) "Conectado" else "Não conectado")
                    DiagnosticLine("Backup automático", if (backup.automaticEnabled) backup.cadence.label else "Desativado")
                    DiagnosticLine("Último sucesso", backup.lastSuccessEpochMs?.toString() ?: "—")
                    backup.lastError?.let { Text(it.take(240), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error) }
                }
            }

            item {
                DiagnosticSection("Registro de eventos") {
                    DiagnosticLine("Eventos legíveis", events.size.toString())
                    DiagnosticLine("Armazenamento", "${journal.sizeBytes() / 1024} KiB · limite 8 MiB / 14 dias")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            enabled = !exporting,
                            onClick = {
                                val timestamp = System.currentTimeMillis()
                                launcher.launch("GuitarLab-Diagnostics-$timestamp.zip")
                            },
                            modifier = Modifier.testTag("diagnostics-export"),
                        ) { Text(if (exporting) "Exportando…" else "Exportar pacote de diagnóstico") }
                        OutlinedButton(
                            enabled = events.isNotEmpty(),
                            onClick = {
                                journal.clear()
                                events = emptyList()
                                status = null
                            },
                            modifier = Modifier.testTag("diagnostics-clear-journal"),
                        ) { Text("Limpar registro") }
                    }
                    Text(
                        "O pacote padrão contém apenas metadados e logs sanitizados. Áudio das músicas não é incluído.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            if (events.isNotEmpty()) {
                item { Text("Eventos recentes", style = MaterialTheme.typography.titleMedium) }
                items(events.takeLast(20).asReversed()) { event ->
                    Surface(Modifier.fillMaxWidth(), tonalElevation = 1.dp, shape = MaterialTheme.shapes.small) {
                        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(event.eventType, style = MaterialTheme.typography.labelLarge)
                            Text("${event.state ?: "—"} · ${event.summary}", style = MaterialTheme.typography.bodySmall)
                            Text(event.timestampEpochMs.toString(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

        }
        AppTransientFeedbackHost(
            message = status,
            kind = statusKind,
            onConsumed = { status = null },
            modifier = Modifier.align(Alignment.TopCenter),
            fallback = "Não foi possível exportar o pacote de diagnóstico.",
        )
    }
}

@Composable
private fun DiagnosticSection(title: String, content: @Composable () -> Unit) {
    Surface(Modifier.fillMaxWidth(), tonalElevation = 1.dp, shape = MaterialTheme.shapes.medium) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            content()
        }
    }
}

@Composable
private fun DiagnosticLine(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodySmall)
        Text(value, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
