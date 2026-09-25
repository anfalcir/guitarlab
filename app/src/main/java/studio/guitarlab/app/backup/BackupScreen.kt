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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import studio.guitarlab.app.ui.AppTransientFeedbackHost
import studio.guitarlab.app.ui.TransientFeedbackKind
import studio.guitarlab.app.ui.ProductEmptyState
import studio.guitarlab.app.ui.ProductSectionCard
import studio.guitarlab.core.project.BackupVersionDescriptor
import studio.guitarlab.core.project.DriveReconciliation

@Composable
fun BackupScreen(
    projectId: String? = null,
    onBack: () -> Unit,
    onProjectsChanged: () -> Unit,
    viewModel: BackupViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val authorizationLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) viewModel.completeDriveConnection(result.data)
        else viewModel.authorizationCancelled()
    }
    Box(Modifier.fillMaxSize()) {
        BackupScreenContent(
            state = state,
            projectId = projectId,
            onBack = onBack,
            onConnectDrive = {
                viewModel.beginDriveConnection { pendingIntent ->
                    authorizationLauncher.launch(
                        IntentSenderRequest.Builder(pendingIntent.intentSender).build(),
                    )
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
            onKeepLocal = viewModel::keepLocalVersion,
            onUseCloud = { viewModel.useCloudVersion(it, onProjectsChanged) },
            onRestoreVersion = { viewModel.restoreVersion(it, onProjectsChanged) },
            onRestoreAll = { viewModel.restoreLatestAll(onProjectsChanged) },
            onDeleteCloudProject = viewModel::deleteCloudProject,
        )
        AppTransientFeedbackHost(
            message = state.error ?: state.message,
            kind = if (state.error != null) TransientFeedbackKind.ERROR else TransientFeedbackKind.ASYNC_COMPLETION,
            onConsumed = viewModel::clearMessage,
            modifier = Modifier.align(Alignment.TopCenter),
            fallback = "Não foi possível concluir a operação de backup.",
        )
        if (state.busy) {
            BackupBusyFeedback(state.busyLabel)
        }
    }
}

@Composable
internal fun BackupBusyFeedback(label: String?) {
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.scrim.copy(alpha = 0.28f)) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Surface(
                modifier = Modifier.fillMaxWidth(0.72f).testTag("backup-busy-feedback"),
                shape = MaterialTheme.shapes.large,
                tonalElevation = 6.dp,
            ) {
                Column(
                    Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    CircularProgressIndicator()
                    Text(
                        label ?: "Concluindo operação…",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        "O GuitarLab está verificando os dados e atualizará o histórico ao terminar.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
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
    onKeepLocal: (String, BackupVersionDescriptor) -> Unit,
    onUseCloud: (BackupVersionDescriptor) -> Unit,
    onRestoreVersion: (BackupVersionDescriptor) -> Unit,
    onRestoreAll: () -> Unit,
    onDeleteCloudProject: (String) -> Unit = {},
) {
    var restoreVersion by remember { mutableStateOf<BackupVersionDescriptor?>(null) }
    var keepLocalVersion by remember { mutableStateOf<BackupVersionDescriptor?>(null) }
    var useCloudVersion by remember { mutableStateOf<BackupVersionDescriptor?>(null) }
    var confirmRestoreAll by remember { mutableStateOf(false) }
    var confirmDisconnect by remember { mutableStateOf(false) }
    var selectedProjectId by remember { mutableStateOf<String?>(null) }
    var confirmCloudDeleteId by remember { mutableStateOf<String?>(null) }
    val configured = state.settings.driveConnected
    val currentProject = projectId?.let { id ->
        state.localProjects.firstOrNull { it.id == id }
    }
    val currentReconciliation = currentProject?.let { state.reconciliations[it.id] }
    val currentRemoteTips = currentProject?.let { state.remoteTips[it.id].orEmpty() }.orEmpty()

    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            AppIconButton(icon = Icons.Default.ArrowBack, contentDescription = "Voltar", onClick = onBack)
            Text("Backup e restauração", modifier = Modifier.weight(1f), style = MaterialTheme.typography.headlineSmall)
            AppIconButton(
                icon = Icons.Default.Refresh,
                contentDescription = "Atualizar catálogo de backups",
                onClick = onRefresh,
                enabled = !state.busy && !state.catalogRefreshing,
            )
        }
        when {
            state.catalogRefreshing && state.versions.isNotEmpty() -> {
                Row(
                    Modifier.fillMaxWidth().testTag("backup-catalog-background-refresh"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    CircularProgressIndicator()
                    Text("Atualizando histórico em segundo plano…", style = MaterialTheme.typography.bodySmall)
                }
            }
            state.catalogUpdatedAtEpochMs != null -> {
                val updated = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
                    .format(Date(state.catalogUpdatedAtEpochMs))
                Text(
                    "Histórico atualizado em $updated${if (state.catalogFromCache) " · cache local" else ""}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.testTag("backup-catalog-freshness"),
                )
            }
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

            if (
                currentProject != null &&
                    currentReconciliation in setOf(
                        DriveReconciliation.CONFLICT,
                        DriveReconciliation.DOWNLOAD_REMOTE,
                    )
            ) {
                item {
                    val trueConflict = currentReconciliation == DriveReconciliation.CONFLICT
                    BackupSection(
                        if (trueConflict) "Conflito de backup" else "Atualização disponível na nuvem",
                        if (trueConflict) {
                            "Há alterações locais e na nuvem. Escolha explicitamente qual estado preservar."
                        } else {
                            "A nuvem avançou desde a última revisão confirmada deste projeto."
                        },
                    ) {
                        if (currentRemoteTips.isEmpty()) {
                            Text(
                                "Atualize o catálogo para carregar as versões disponíveis.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        } else {
                            currentRemoteTips.forEach { remote ->
                                val date = DateFormat.getDateTimeInstance(
                                    DateFormat.MEDIUM,
                                    DateFormat.SHORT,
                                ).format(Date(remote.backupCreatedAtEpochMs))
                                Text(
                                    "Versão da nuvem · $date",
                                    style = MaterialTheme.typography.titleSmall,
                                )
                                if (trueConflict && currentRemoteTips.size == 1) {
                                    OutlinedButton(
                                        onClick = { keepLocalVersion = remote },
                                        enabled = !state.busy,
                                        modifier = Modifier.fillMaxWidth()
                                            .testTag("conflict-keep-local"),
                                    ) {
                                        Text("Manter versão local")
                                    }
                                }
                                if (currentRemoteTips.size == 1) {
                                    Button(
                                        onClick = { useCloudVersion = remote },
                                        enabled = !state.busy,
                                        modifier = Modifier.fillMaxWidth()
                                            .testTag("conflict-use-cloud-${remote.remoteId}"),
                                    ) {
                                        Text("Usar versão da nuvem")
                                    }
                                }
                                OutlinedButton(
                                    onClick = { restoreVersion = remote },
                                    enabled = !state.busy,
                                    modifier = Modifier.fillMaxWidth()
                                        .testTag("conflict-import-copy-${remote.remoteId}"),
                                ) {
                                    Text("Importar versão da nuvem como cópia")
                                }
                                if (trueConflict && currentRemoteTips.size > 1) {
                                    Text(
                                        "Existem versões remotas concorrentes. Para preservar tudo, importe a versão desejada como cópia antes de decidir substituir o projeto.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
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

            if (state.loading && state.versions.isEmpty()) {
                item { Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() } }
            } else if (!configured) {
                item {
                    ProductEmptyState(
                        title = "Backup indisponível",
                        body = "Conecte o Google Drive para carregar o catálogo de backups.",
                        modifier = Modifier.testTag("backup-catalog-disconnected"),
                    )
                }
            } else if (state.versions.isEmpty()) {
                item {
                    ProductEmptyState(
                        title = "Nenhum backup disponível",
                        body = "Quando um backup for concluído, a versão aparecerá aqui.",
                        modifier = Modifier.testTag("backup-catalog-empty"),
                    )
                }
            } else {
                val grouped = state.versions.groupBy { it.projectId }.values
                    .sortedBy { versions -> versions.first().projectName.lowercase() }
                items(grouped, key = { it.first().projectId }) { versions ->
                    val latest = versions.maxBy { it.backupCreatedAtEpochMs }
                    BackupProjectRow(
                        latest = latest,
                        versionCount = versions.size,
                        deleted = latest.projectId in state.deletedProjects,
                        cloudOnly = state.localProjects.none { it.id == latest.projectId },
                        enabled = !state.busy,
                        onOpen = { selectedProjectId = latest.projectId },
                    )
                }
            }
        }
    }

    keepLocalVersion?.let { version ->
        AlertDialog(
            onDismissRequest = { keepLocalVersion = null },
            title = { Text("Manter versão local?") },
            text = {
                Text(
                    "A versão local será preservada e publicada como a próxima revisão na nuvem. A versão remota atual continuará no histórico."
                )
            },
            dismissButton = {
                TextButton(onClick = { keepLocalVersion = null }) {
                    Text("Cancelar")
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        keepLocalVersion = null
                        currentProject?.let { onKeepLocal(it.id, version) }
                    },
                    modifier = Modifier.testTag("confirm-keep-local"),
                ) {
                    Text("Manter local")
                }
            },
        )
    }
    useCloudVersion?.let { version ->
        AlertDialog(
            onDismissRequest = { useCloudVersion = null },
            title = { Text("Usar versão da nuvem?") },
            text = {
                Text(
                    "O projeto local atual será substituído somente depois que a versão da nuvem for baixada e validada por completo. Para preservar as alterações locais, cancele e importe a nuvem como cópia."
                )
            },
            dismissButton = {
                TextButton(onClick = { useCloudVersion = null }) {
                    Text("Cancelar")
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        useCloudVersion = null
                        onUseCloud(version)
                    },
                    modifier = Modifier.testTag("confirm-use-cloud"),
                ) {
                    Text("Usar nuvem")
                }
            },
        )
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
    selectedProjectId?.let { selectedId ->
        val versions = state.versions.filter { it.projectId == selectedId }
            .sortedByDescending { it.backupCreatedAtEpochMs }
        if (versions.isNotEmpty()) {
            val deleted = selectedId in state.deletedProjects
            AlertDialog(
                onDismissRequest = { selectedProjectId = null },
                title = { Text(versions.first().projectName) },
                text = {
                    Column(
                        Modifier.fillMaxWidth().heightIn(max = 520.dp).verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text("ID: $selectedId", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (deleted) Text("Excluído do aparelho · remoção automática após 10 dias", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelMedium)
                        versions.forEach { version ->
                            BackupVersionRow(version, enabled = !state.busy, onRestore = { selectedProjectId = null; restoreVersion = version })
                        }
                        if (deleted) {
                            OutlinedButton(onClick = { selectedProjectId = null; confirmCloudDeleteId = selectedId }, enabled = !state.busy) {
                                Text("Excluir definitivamente da nuvem")
                            }
                        }
                    }
                },
                confirmButton = { TextButton(onClick = { selectedProjectId = null }) { Text("Fechar") } },
            )
        }
    }
    confirmCloudDeleteId?.let { selectedId ->
        AlertDialog(
            onDismissRequest = { confirmCloudDeleteId = null },
            title = { Text("Excluir backups da nuvem?") },
            text = { Text("Todas as versões deste projeto serão removidas definitivamente. Esta ação não pode ser desfeita.") },
            dismissButton = { TextButton(onClick = { confirmCloudDeleteId = null }) { Text("Cancelar") } },
            confirmButton = { TextButton(onClick = { confirmCloudDeleteId = null; onDeleteCloudProject(selectedId) }) { Text("Excluir definitivamente") } },
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
private fun BackupSection(
    title: String,
    subtitle: String?,
    content: @Composable () -> Unit,
) {
    ProductSectionCard(title = title, subtitle = subtitle) {
        content()
    }
}

@Composable
private fun ToggleRow(
    label: String,
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        Switch(checked = checked, onCheckedChange = onCheckedChange, enabled = enabled)
    }
}

@Composable
private fun <T> ChoiceRow(
    label: String,
    value: String,
    options: List<Pair<String, T>>,
    enabled: Boolean,
    onSelect: (T) -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        Box {
            OutlinedButton(onClick = { open = true }, enabled = enabled) { Text(value) }
            DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
                options.forEach { (optionLabel, optionValue) ->
                    DropdownMenuItem(
                        text = { Text(optionLabel) },
                        onClick = {
                            open = false
                            onSelect(optionValue)
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun BackupProjectRow(
    latest: BackupVersionDescriptor,
    versionCount: Int,
    deleted: Boolean,
    cloudOnly: Boolean,
    enabled: Boolean,
    onOpen: () -> Unit,
) {
    val date = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(latest.backupCreatedAtEpochMs))
    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp), border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(latest.projectName, style = MaterialTheme.typography.titleSmall)
                Text("ID: ${latest.projectId}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("$versionCount ${if (versionCount == 1) "versão" else "versões"} · mais recente em $date", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (deleted) Text("EXCLUÍDO", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                else if (cloudOnly) Text("SOMENTE NA NUVEM", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.tertiary)
            }
            OutlinedButton(onClick = onOpen, enabled = enabled) { Text("Ver versões") }
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
