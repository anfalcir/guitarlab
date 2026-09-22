package studio.guitarlab.app.backup

import android.app.Application
import android.app.PendingIntent
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import java.text.DateFormat
import java.util.Date
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import studio.guitarlab.core.model.GuitarProject
import studio.guitarlab.core.project.BackupRetentionPolicy
import studio.guitarlab.core.project.BackupRunReport
import studio.guitarlab.core.project.BackupVersionDescriptor
import studio.guitarlab.core.project.FileProjectRepository
import studio.guitarlab.core.project.ProjectBackupAttempt
import studio.guitarlab.core.project.ProjectBackupCoordinator

data class BackupUiState(
    val loading: Boolean = true,
    val busy: Boolean = false,
    val settings: BackupSettingsSnapshot = BackupSettingsSnapshot(),
    val localProjects: List<GuitarProject> = emptyList(),
    val versions: List<BackupVersionDescriptor> = emptyList(),
    val authorizationRequired: Boolean = false,
    val message: String? = null,
    val error: String? = null,
)

class BackupViewModel(application: Application) : AndroidViewModel(application) {
    private val settingsStore = BackupSettingsStore(application)
    private val repository = FileProjectRepository(application.filesDir)
    private val authorization = GoogleDriveAuthorization(application)
    private val _state = MutableStateFlow(BackupUiState())
    val state: StateFlow<BackupUiState> = _state.asStateFlow()

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            val settings = settingsStore.snapshot()
            val projects = withContext(Dispatchers.IO) { repository.list() }
            var authRequired = false
            val versions = if (settings.driveConnected) {
                try {
                    DriveV3BackupRemoteStore(getApplication()).listCommittedVersions()
                } catch (_: DriveAuthorizationRequiredException) {
                    authRequired = true
                    emptyList()
                } catch (error: Throwable) {
                    if (error is CancellationException) throw error
                    _state.update { it.copy(error = error.message ?: "Não foi possível ler os backups do Google Drive.") }
                    emptyList()
                }
            } else emptyList()
            _state.update { current ->
                current.copy(
                    loading = false,
                    settings = settingsStore.snapshot(),
                    localProjects = projects,
                    versions = versions,
                    authorizationRequired = authRequired,
                )
            }
        }
    }

    fun beginDriveConnection(onResolution: (PendingIntent) -> Unit) {
        if (_state.value.busy) return
        viewModelScope.launch {
            _state.update { it.copy(busy = true, error = null, message = null) }
            try {
                when (val result = authorization.request()) {
                    is DriveAuthorizationState.Authorized -> finishDriveConnection()
                    is DriveAuthorizationState.NeedsUserConsent -> {
                        _state.update { it.copy(busy = false) }
                        onResolution(result.pendingIntent)
                    }
                }
            } catch (error: Throwable) {
                if (error is CancellationException) throw error
                fail(error)
            }
        }
    }

    fun completeDriveConnection(data: Intent?) {
        if (_state.value.busy) return
        viewModelScope.launch {
            _state.update { it.copy(busy = true, error = null, message = null) }
            try {
                authorization.tokenFromResult(data) // validates the user-granted result
                finishDriveConnection()
            } catch (error: Throwable) {
                if (error is CancellationException) throw error
                fail(error)
            }
        }
    }

    fun authorizationCancelled() {
        _state.update { it.copy(busy = false, error = "A conexão com o Google Drive foi cancelada.") }
    }

    fun disconnectDrive() {
        if (_state.value.busy) return
        viewModelScope.launch {
            _state.update { it.copy(busy = true, error = null, message = null) }
            try {
                authorization.revoke()
                DriveBackupStateStore(getApplication()).clearAll()
                settingsStore.clearDriveConnection()
                BackupScheduler.sync(getApplication())
                _state.update {
                    it.copy(
                        busy = false,
                        settings = settingsStore.snapshot(),
                        versions = emptyList(),
                        authorizationRequired = false,
                        message = "Google Drive desconectado. Os backups existentes não foram apagados.",
                    )
                }
            } catch (error: Throwable) {
                if (error is CancellationException) throw error
                fail(error)
            }
        }
    }

    fun migrateLegacySaf() = runBusy(null) {
        val settings = settingsStore.snapshot()
        val uri = settings.treeUri ?: error("Não há destino SAF antigo para migrar.")
        val saf = SafBackupRemoteStore(getApplication(), uri)
        require(saf.hasPersistedReadWritePermission()) {
            "A permissão da pasta antiga foi revogada. Os backups antigos continuam intactos, mas não podem ser migrados automaticamente."
        }
        val drive = driveRemoteOrError()
        val report = BackupOperationLock.withLock {
            LegacySafBackupMigrator(getApplication()).migrate(saf, drive)
        }
        if (report.failed == 0) {
            SafBackupRemoteStore.releasePersistedPermission(getApplication(), uri)
            settingsStore.clearLegacyFolder()
            val summary = "Migração concluída: ${countLabel(report.migrated, "versão copiada", "versões copiadas")}, ${countLabel(report.skippedExisting, "já existente", "já existentes")}."
            settingsStore.recordSuccess(summary)
            _state.update { it.copy(message = summary) }
        } else {
            val detail = report.errors.joinToString("\n").take(3000)
            val summary = "Migração SAF parcial: ${report.migrated} copiadas, ${report.skippedExisting} já existentes, ${report.failed} falhas. A pasta antiga foi preservada."
            settingsStore.recordPartial(summary, detail)
            _state.update { it.copy(message = summary, error = detail) }
        }
    }

    fun setAutomaticEnabled(value: Boolean) = updateSettings { settingsStore.setAutomaticEnabled(value) }
    fun setCadence(value: BackupCadence) = updateSettings { settingsStore.setCadence(value) }
    fun setUnmeteredOnly(value: Boolean) = updateSettings { settingsStore.setUnmeteredOnly(value) }
    fun setChargingOnly(value: Boolean) = updateSettings { settingsStore.setChargingOnly(value) }
    fun setRetentionDays(value: Int?) = updateSettings { settingsStore.setRetentionDays(value) }
    fun setMaximumVersions(value: Int) = updateSettings { settingsStore.setMaximumVersions(value) }

    fun backupAllNow() = runBackup("Backup total") { coordinator ->
        val settings = settingsStore.snapshot()
        coordinator.backupAll(force = false, retentionPolicy = retention(settings))
    }

    fun backupProjectNow(projectId: String) = runBackup("Backup do projeto") { coordinator ->
        val settings = settingsStore.snapshot()
        coordinator.backupProject(projectId, force = false, retentionPolicy = retention(settings))
    }

    fun restoreVersion(version: BackupVersionDescriptor, onProjectsChanged: () -> Unit) = runBusy(null) {
        val coordinator = coordinatorOrError()
        val result = BackupOperationLock.withLock { coordinator.restoreVersion(version) }
        if (!result.succeeded) error(result.error ?: "Não foi possível restaurar o backup.")
        settingsStore.recordSuccess("Projeto restaurado: ${result.restoredProject?.name.orEmpty()}")
        withContext(Dispatchers.Main) { onProjectsChanged() }
        _state.update { it.copy(message = "Projeto restaurado como uma nova cópia local.") }
    }

    fun restoreLatestAll(onProjectsChanged: () -> Unit) = runBusy(null) {
        val coordinator = coordinatorOrError()
        val results = BackupOperationLock.withLock { coordinator.restoreLatestAll() }
        val ok = results.count { it.succeeded }
        val failed = results.size - ok
        if (ok > 0) withContext(Dispatchers.Main) { onProjectsChanged() }
        val summary = "Restauração total: ${countLabel(ok, "projeto restaurado", "projetos restaurados")}${if (failed > 0) ", ${countLabel(failed, "falha", "falhas")}" else ""}."
        if (failed == 0) {
            settingsStore.recordSuccess(summary)
            _state.update { it.copy(message = summary) }
        } else {
            val detail = results.firstOrNull { !it.succeeded }?.error ?: "Não foi possível restaurar todos os projetos."
            settingsStore.recordPartial(summary, detail)
            _state.update { it.copy(error = detail, message = summary) }
        }
    }

    fun clearMessage() = _state.update { it.copy(message = null, error = null) }

    private suspend fun finishDriveConnection() {
        val label = DriveV3BackupRemoteStore(getApplication()).probeReadWriteDelete()
        settingsStore.setDriveConnected(label)
        BackupScheduler.sync(getApplication())
        _state.update {
            it.copy(
                busy = false,
                settings = settingsStore.snapshot(),
                authorizationRequired = false,
                message = "Google Drive conectado e validado.",
            )
        }
        refresh()
    }

    private fun runBackup(label: String, action: suspend (ProjectBackupCoordinator) -> BackupRunReport) = runBusy(null) {
        val coordinator = coordinatorOrError()
        val report = BackupOperationLock.withLock { action(coordinator) }
        val summary = report.userSummary(label)
        val failureDetail = report.userFailureDetail()
        if (failureDetail == null) {
            settingsStore.recordSuccess(summary)
            _state.update { it.copy(message = summary) }
        } else {
            settingsStore.recordPartial(summary, failureDetail)
            _state.update { it.copy(message = summary, error = failureDetail) }
        }
    }

    private fun driveRemoteOrError(): DriveV3BackupRemoteStore {
        require(settingsStore.snapshot().driveConnected) { "Conecte primeiro o Google Drive." }
        return DriveV3BackupRemoteStore(getApplication())
    }

    private fun coordinatorOrError(): ProjectBackupCoordinator = ProjectBackupCoordinator(
        getApplication<Application>().filesDir,
        driveRemoteOrError(),
    )

    private fun retention(settings: BackupSettingsSnapshot) = BackupRetentionPolicy(settings.retentionDays, settings.maximumVersions)

    private fun updateSettings(change: () -> Unit) {
        change()
        BackupScheduler.sync(getApplication())
        _state.update { it.copy(settings = settingsStore.snapshot()) }
    }

    private fun runBusy(successMessage: String?, action: suspend () -> Unit) {
        if (_state.value.busy) return
        viewModelScope.launch {
            _state.update { it.copy(busy = true, error = null, message = null) }
            try {
                action()
                _state.update { it.copy(busy = false, message = it.message ?: successMessage) }
                refresh()
            } catch (error: Throwable) {
                if (error is CancellationException) throw error
                fail(error)
            }
        }
    }

    private fun fail(error: Throwable) {
        val message = error.message ?: "Não foi possível concluir a operação de backup."
        settingsStore.recordError(message)
        _state.update {
            it.copy(
                busy = false,
                authorizationRequired = error is DriveAuthorizationRequiredException || it.authorizationRequired,
                error = message,
            )
        }
    }
}

internal fun BackupRunReport.userSummary(label: String): String = buildString {
    append(label)
    append(": ")
    append(countLabel(committedCount, "projeto salvo", "projetos salvos"))
    append(", ")
    append(countLabel(skippedCount, "já estava atualizado", "já estavam atualizados"))
    append(", ")
    append(countLabel(deletedVersions, "versão antiga removida", "versões antigas removidas"))
    if (failedCount > 0) {
        append(", ")
        append(countLabel(failedCount, "falha", "falhas"))
    }
    append('.')
}

internal fun BackupRunReport.userFailureDetail(): String? {
    val failures = attempts.filter { it.status == ProjectBackupAttempt.Status.FAILED }
    if (failures.isEmpty()) return null
    return failures.joinToString("\n") { attempt ->
        "Não foi possível salvar “${attempt.projectName}”: ${friendlyBackupError(attempt.error)}"
    }
}

internal fun friendlyBackupError(raw: String?): String {
    val message = raw.orEmpty().ifBlank { "erro inesperado no armazenamento." }
    return when {
        message.contains("SHA-256", ignoreCase = true) -> "a verificação de integridade do arquivo falhou."
        message.contains("tamanho", ignoreCase = true) -> "o arquivo enviado não pôde ser verificado por completo."
        message.contains("commit", ignoreCase = true) -> "o backup não pôde ser confirmado no armazenamento."
        else -> message.replaceFirstChar { if (it.isUpperCase()) it.lowercase() else it.toString() }
    }
}

internal fun countLabel(count: Int, singular: String, plural: String): String = "$count ${if (count == 1) singular else plural}"

fun BackupSettingsSnapshot.lastRunLabel(): String = lastRunEpochMs?.let {
    DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(it))
} ?: "Nenhuma execução ainda"
