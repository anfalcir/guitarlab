package studio.guitarlab.app.backup

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import java.text.DateFormat
import java.util.Date
import studio.guitarlab.app.ui.AppIconButton
import studio.guitarlab.core.project.BackupVersionDescriptor

@Composable
fun BackupScreen(
    projectId: String? = null,
    onBack: () -> Unit,
    onProjectsChanged: () -> Unit,
    viewModel: BackupViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val authorizationLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) viewModel.completeDriveConnection(result.data)
        else viewModel.authorizationCancelled()
    }
    LaunchedEffect(state.message, state.error) {
        val message = state.error ?: state.message
        if (message != null) {
            snackbar.showSnackbar(message)
            viewModel.clearMessage()
        }
    }
    Box(Modifier.fillMaxSize()) {
        BackupScreenContent(
            state = state,
            projectId = projectId,
            onBack = onBack,
            onConnectDrive = {
                viewModel.beginDriveConnection { pendingIntent ->
                    authorizationLauncher.launch(IntentSenderRequest.Builder(pendingIntent.intentSender).build())
                }
            },
            onDisconnectDrive = viewModel::disconnectDrive,
            onRefresh = viewModel::refresh,
            onAutomaticEnabled = viewModel::setAutomaticEnabled,
            onCadence = viewModel::setCadence,
            onUnmeteredOnly = viewModel::setUnmeteredOnly,
            onChargingOnly = viewModel::setChargingOnly,
            onRetentionDays = viewModel::setRetentionDays,
            onMaximumVersions = viewModel::setMaximumVersions,
            onBackupAll = viewModel::backupAllNow,
            onBackupProject = viewModel::backupProjectNow,
            onRestoreVersion = { viewModel.restoreVersion(it, onProjectsChanged) },
            onRestoreAll = { viewModel.restoreLatestAll(onProjectsChanged) },
        )
        SnackbarHost(snackbar, Modifier.align(Alignment.TopCenter).padding(top = 12.dp))
        if (state.busy) {
            Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.scrim.copy(alpha = 0.28f)) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            }
        }
    }
}

@Composable
fun BackupScreenContent(
    state: BackupUiState,
    projectId: String?,
    onBack: () -> Unit,
    onConnectDrive: () -> Unit,
    onDisconnectDrive: () -> Unit,
    onRefresh: () -> Unit,
    onAutomaticEnabled: (Boolean) -> Unit,
    onCadence: (BackupCadence) -> Unit,
    onUnmeteredOnly: (Boolean) -> Unit,
    onChargingOnly: (Boolean) -> Unit,
    onRetentionDays: (Int?) -> Unit,
    onMaximumVersions: (Int) -> Unit,
    onBackupAll: () -> Unit,
    onBackupProject: (String) -> Unit,
    onRestoreVersion: (BackupVersionDescriptor) -> Unit,
    onRestoreAll: () -> Unit,
) {
    var restoreVersion by remember { mutableStateOf<BackupVersionDescriptor?>(null) }
    var confirmRestoreAll by remember { mutableStateOf(false) }
    var confirmDisconnect by remember { mutableStateOf(false) }
    val configured = state.settings.driveConnected
    val currentProject = projectId?.let { id -> state.localProjects.firstOrNull { it.id == id } }

    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            AppIconButton(icon = Icons.Default.ArrowBack, contentDescription = "Voltar", onClick = onBack)
            Text("Backup e restauração", modifier = Modifier.weight(1f), style = MaterialTheme.typography.headlineSmall)
            AppIconButton(icon = Icons.Default.Refresh, contentDescription = "Atualizar catálogo de backups", onClick = onRefresh, enabled = !state.busy)
        }

        LazyColumn(Modifier.fillMaxSize().testTag("backup-screen"), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                BackupSection("Google Drive", "Os backups são enviados diretamente pelo GuitarLab usando Drive API v3 e acesso limitado aos arquivos do app.") {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Icon(Icons.Default.Cloud, null)
                            Column {
                                Text(state.settings.driveAccountLabel ?: "Google Drive não conectado", style = MaterialTheme.typography.titleSmall)
                                Text(
                                    when {
                                        state.authorizationRequired -> "Reconecte para renovar a autorização"
                                        configured -> "Drive conectado e validado"
                                        else -> "Conecte sua Conta Google para ativar o backup"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (state.authorizationRequired) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        OutlinedButton(onClick = onConnectDrive, enabled = !state.busy, modifier = Modifier.testTag("backup-connect-drive")) {
                            Text(if (configured && !state.authorizationRequired) "Reconectar" else "Conectar")
                        }
                    }
                    if (configured) {
                        TextButton(onClick = { confirmDisconnect = true }, enabled = !state.busy) { Text("Desconectar Google Drive") }
                    }
                }
            }

            item {
                BackupSection("Backup automático", "Salva somente quando houver alterações e evita cópias repetidas da mesma versão.") {
                    ToggleRow("Backup automático", state.settings.automaticEnabled, configured && !state.busy, onAutomaticEnabled)
                    ChoiceRow("Frequência", state.settings.cadence.label, BackupCadence.entries.map { it.label to it }, !state.busy) { onCadence(it) }
                    ToggleRow("Somente rede não tarifada / Wi‑Fi", state.settings.unmeteredOnly, !state.busy, onUnmeteredOnly)
                    ToggleRow("Somente enquanto carregando", state.settings.chargingOnly, !state.busy, onChargingOnly)
                }
            }

            item {
                BackupSection("Histórico", "Defina quantas versões de cada projeto devem permanecer disponíveis.") {
                    val retentionOptions = listOf(
                        "7 dias" to 7, "30 dias" to 30, "60 dias" to 60, "90 dias" to 90,
                        "180 dias" to 180, "1 ano" to 365, "Sem limite de idade" to null,
                    )
                    val retentionLabel = retentionOptions.firstOrNull { it.second == state.settings.retentionDays }?.first ?: "90 dias"
                    ChoiceRow("Idade máxima", retentionLabel, retentionOptions, !state.busy) { onRetentionDays(it) }
                    val maximumOptions = listOf("1 versão" to 1, "3 versões" to 3, "5 versões" to 5, "10 versões" to 10)
                    val maximumLabel = if (state.settings.maximumVersions == 1) "1 versão" else "${state.settings.maximumVersions} versões"
                    ChoiceRow("Máximo de versões por projeto", maximumLabel, maximumOptions, !state.busy) { onMaximumVersions(it) }
                    Text("A versão mais recente de cada projeto é sempre preservada. Versões repetidas da mesma revisão são removidas automaticamente.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            item {
                BackupSection("Ações", "Se o projeto já estiver atualizado no backup, nenhuma cópia duplicada será criada.") {
                    if (currentProject != null) {
                        Button(
                            onClick = { onBackupProject(currentProject.id) },
                            enabled = configured && !state.busy,
                            modifier = Modifier.fillMaxWidth().testTag("backup-current-project"),
                        ) {
                            Icon(Icons.Default.CloudUpload, null)
                            Text("Backup de “${currentProject.name}” agora", Modifier.padding(start = 8.dp))
                        }
                    }
                    Button(onClick = onBackupAll, enabled = configured && state.localProjects.isNotEmpty() && !state.busy, modifier = Modifier.fillMaxWidth().testTag("backup-all-now")) {
                        Icon(Icons.Default.CloudDone, null)
                        Text("Backup total agora", Modifier.padding(start = 8.dp))
                    }
                    OutlinedButton(onClick = { confirmRestoreAll = true }, enabled = configured && state.versions.isNotEmpty() && !state.busy, modifier = Modifier.fillMaxWidth().testTag("restore-all")) {
                        Icon(Icons.Default.Restore, null)
                        Text("Restaurar versão mais recente de cada projeto", Modifier.padding(start = 8.dp))
                    }
                }
            }

            item {
                BackupSection("Estado", null) {
                    Text("Última execução: ${state.settings.lastRunLabel()}", style = MaterialTheme.typography.bodyMedium)
                    state.settings.lastRunSummary?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    state.settings.lastError?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error) }
                }
            }

            item {
                Text("Versões disponíveis", style = MaterialTheme.typography.titleLarge)
                Text("Restaurar cria uma nova cópia local; nenhum projeto existente é sobrescrito.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            if (state.loading) {
                item { Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() } }
            } else if (!configured) {
                item { Text("Conecte o Google Drive para carregar o catálogo de backups.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            } else if (state.versions.isEmpty()) {
                item { Text("Nenhum backup disponível no Google Drive.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            } else {
                items(state.versions, key = { it.remoteId }) { version ->
                    BackupVersionRow(version, enabled = !state.busy, onRestore = { restoreVersion = version })
                }
            }
        }
    }

    restoreVersion?.let { version ->
        AlertDialog(
            onDismissRequest = { restoreVersion = null },
            title = { Text("Restaurar projeto?") },
            text = { Text("Será criada uma nova cópia local de “${version.projectName}”. O projeto atual não será sobrescrito.") },
            dismissButton = { TextButton(onClick = { restoreVersion = null }) { Text("Cancelar") } },
            confirmButton = { TextButton(onClick = { restoreVersion = null; onRestoreVersion(version) }, modifier = Modifier.testTag("confirm-restore-version")) { Text("Restaurar") } },
        )
    }
    if (confirmRestoreAll) {
        AlertDialog(
            onDismissRequest = { confirmRestoreAll = false },
            title = { Text("Restauração total?") },
            text = { Text("A versão mais recente de cada projeto será restaurada como uma nova cópia local. Projetos existentes não serão substituídos.") },
            dismissButton = { TextButton(onClick = { confirmRestoreAll = false }) { Text("Cancelar") } },
            confirmButton = { TextButton(onClick = { confirmRestoreAll = false; onRestoreAll() }, modifier = Modifier.testTag("confirm-restore-all")) { Text("Restaurar tudo") } },
        )
    }
    if (confirmDisconnect) {
        AlertDialog(
            onDismissRequest = { confirmDisconnect = false },
            title = { Text("Desconectar Google Drive?") },
            text = { Text("O GuitarLab revogará o acesso concedido. Os backups já gravados permanecerão intactos no seu Drive.") },
            dismissButton = { TextButton(onClick = { confirmDisconnect = false }) { Text("Cancelar") } },
            confirmButton = { TextButton(onClick = { confirmDisconnect = false; onDisconnectDrive() }) { Text("Desconectar") } },
        )
    }
}

@Composable
private fun BackupSection(title: String, subtitle: String?, content: @Composable () -> Unit) {
    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.28f)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            subtitle?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            content()
        }
    }
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, enabled: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        Switch(checked = checked, onCheckedChange = onCheckedChange, enabled = enabled)
    }
}

@Composable
private fun <T> ChoiceRow(label: String, value: String, options: List<Pair<String, T>>, enabled: Boolean, onSelect: (T) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        Box {
            OutlinedButton(onClick = { open = true }, enabled = enabled) { Text(value) }
            DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
                options.forEach { (optionLabel, optionValue) ->
                    DropdownMenuItem(text = { Text(optionLabel) }, onClick = { open = false; onSelect(optionValue) })
                }
            }
        }
    }
}

@Composable
private fun BackupVersionRow(version: BackupVersionDescriptor, enabled: Boolean, onRestore: () -> Unit) {
    val date = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(version.backupCreatedAtEpochMs))
    val size = if (version.sizeBytes < 1024 * 1024) "${version.sizeBytes / 1024} KB" else String.format("%.1f MB", version.sizeBytes / 1024.0 / 1024.0)
    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp), border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(version.projectName, style = MaterialTheme.typography.titleSmall)
                Text("$date · $size", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            OutlinedButton(onClick = onRestore, enabled = enabled, modifier = Modifier.testTag("restore-version-${version.remoteId}")) { Text("Restaurar") }
        }
    }
}
