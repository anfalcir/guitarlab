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
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID
import studio.guitarlab.app.activity.ActivityCancellationRegistry
import studio.guitarlab.app.activity.UnifiedActivityStore
import studio.guitarlab.core.model.GuitarProject
import studio.guitarlab.core.project.BackupRetentionPolicy
import studio.guitarlab.core.project.BackupRunReport
import studio.guitarlab.core.project.BackupVersionDescriptor
import studio.guitarlab.core.project.DriveConflictAction
import studio.guitarlab.core.project.DriveReconciliation
import studio.guitarlab.core.project.FileProjectRepository
import studio.guitarlab.core.project.ProjectBackupAttempt
import studio.guitarlab.core.project.UnifiedOperationKind
import studio.guitarlab.core.project.UnifiedOperationState

data class BackupUiState(
    val loading: Boolean = true,
    val busy: Boolean = false,
    val busyLabel: String? = null,
    val settings: BackupSettingsSnapshot = BackupSettingsSnapshot(),
    val localProjects: List<GuitarProject> = emptyList(),
    val versions: List<BackupVersionDescriptor> = emptyList(),
    val deletedProjects: Map<String, DeletedProjectBackup> = emptyMap(),
    val reconciliations: Map<String, DriveReconciliation> = emptyMap(),
    val remoteTips: Map<String, List<BackupVersionDescriptor>> = emptyMap(),
    val authorizationRequired: Boolean = false,
    val catalogRefreshing: Boolean = false,
    val catalogUpdatedAtEpochMs: Long? = null,
    val catalogFromCache: Boolean = false,
    val message: String? = null,
    val error: String? = null,
)

class BackupViewModel(application: Application) : AndroidViewModel(application) {
    private val settingsStore = BackupSettingsStore(application)
    private val repository = FileProjectRepository(application.filesDir)
    private val authorization = GoogleDriveAuthorization(application)
    private val activityStore = UnifiedActivityStore(application)
    private val confirmedRevisions = ConfirmedRevisionStore(application)
    private val unifiedDrive = UnifiedDriveProductionService(application)
    private val deletedProjectStore = DeletedProjectBackupStore(application)
    private val catalogCache = BackupCatalogCacheStore(application)
    private var refreshJob: Job? = null
    private val _state = MutableStateFlow(BackupUiState())
    val state: StateFlow<BackupUiState> = _state.asStateFlow()
    init {
        refreshInternal(forceRemote = false)
        observeAutomaticBackupCompletion()
    }

    /** User-requested refresh always verifies Drive; re-entry may reuse a recent durable catalog. */
    fun refresh() = refreshInternal(forceRemote = true)

    private fun refreshInternal(forceRemote: Boolean) {
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            val settings = settingsStore.snapshot()
            val projects = withContext(Dispatchers.IO) { repository.list() }
            val cached = catalogCache.load(settings)
            val shouldRefreshRemote = settings.driveConnected &&
                BackupCatalogFreshnessPolicy.shouldRefresh(cached, System.currentTimeMillis(), forceRemote)

            _state.update { current ->
                current.copy(
                    loading = cached == null && shouldRefreshRemote,
                    catalogRefreshing = shouldRefreshRemote,
                    settings = settings,
                    localProjects = projects,
                    versions = cached?.versions ?: if (settings.driveConnected) current.versions else emptyList(),
                    deletedProjects = deletedProjectStore.all(),
                    reconciliations = cached?.reconciliations ?: if (settings.driveConnected) current.reconciliations else emptyMap(),
                    remoteTips = cached?.remoteTips ?: if (settings.driveConnected) current.remoteTips else emptyMap(),
                    catalogUpdatedAtEpochMs = cached?.refreshedAtEpochMs ?: current.catalogUpdatedAtEpochMs,
                    catalogFromCache = cached != null,
                    authorizationRequired = false,
                    error = null,
                )
            }

            if (!settings.driveConnected || !shouldRefreshRemote) {
                _state.update { it.copy(loading = false, catalogRefreshing = false) }
                return@launch
            }

            try {
                val remoteCatalog = unifiedDrive.loadCatalog(projects, retention(settings))
                val refreshedAt = System.currentTimeMillis()
                val snapshot = BackupCatalogSnapshot(
                    versions = remoteCatalog.versions,
                    reconciliations = remoteCatalog.reconciliations,
                    remoteTips = remoteCatalog.remoteTips,
                    refreshedAtEpochMs = refreshedAt,
                )
                catalogCache.save(settings, snapshot)
                _state.update {
                    it.copy(
                        loading = false,
                        catalogRefreshing = false,
                        settings = settingsStore.snapshot(),
                        localProjects = projects,
                        versions = snapshot.versions,
                        deletedProjects = deletedProjectStore.all(),
                        reconciliations = snapshot.reconciliations,
                        remoteTips = snapshot.remoteTips,
                        catalogUpdatedAtEpochMs = refreshedAt,
                        catalogFromCache = false,
                        authorizationRequired = false,
                        error = null,
                    )
                }
            } catch (_: DriveAuthorizationRequiredException) {
                _state.update {
                    it.copy(
                        loading = false,
                        catalogRefreshing = false,
                        authorizationRequired = true,
                        error = if (cached == null) "Reconecte o Google Drive para atualizar o histórico de backups."
                        else "A autorização do Google Drive expirou. O último histórico salvo continua disponível.",
                    )
                }
            } catch (error: Throwable) {
                if (error is CancellationException) throw error
                _state.update {
                    it.copy(
                        loading = false,
                        catalogRefreshing = false,
                        error = if (cached == null) {
                            error.message ?: "Não foi possível ler os backups do Google Drive."
                        } else {
                            "Não foi possível atualizar o Google Drive agora. O último histórico salvo continua disponível."
                        },
                    )
                }
            }
        }
    }

    fun beginDriveConnection(onResolution: (PendingIntent) -> Unit) {
        beginDriveAuthorization(onResolution)
    }

    private fun beginDriveAuthorization(
        onResolution: (PendingIntent) -> Unit,
    ) {
        if (_state.value.busy) return
        _state.update { it.copy(busy = true, busyLabel = "Conectando ao Google Drive…", error = null, message = null) }
        viewModelScope.launch {
            try {
                when (val result = authorization.request()) {
                    is DriveAuthorizationState.Authorized ->
                        finishDriveConnection()
                    is DriveAuthorizationState.NeedsUserConsent -> {
                        _state.update { it.copy(busy = false, busyLabel = null) }
                        onResolution(result.pendingIntent)
                    }
                }
            } catch (error: Throwable) {
                if (error is CancellationException) {
                    _state.update {
                        it.copy(
                            busy = false,
                            busyLabel = null,
                            message = "Operação cancelada.",
                            error = null,
                        )
                    }
                    throw error
                }
                fail(error)
            }
        }
    }

    fun completeDriveConnection(data: Intent?) {
        if (_state.value.busy) return
        _state.update { it.copy(busy = true, busyLabel = "Validando a conexão…", error = null, message = null) }
        viewModelScope.launch {
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
        _state.update {
            it.copy(
                busy = false,
                busyLabel = null,
                error = "A conexão com o Google Drive foi cancelada.",
            )
        }
    }

    fun disconnectDrive() {
        if (_state.value.busy) return
        _state.update { it.copy(busy = true, busyLabel = "Desconectando…", error = null, message = null) }
        viewModelScope.launch {
            try {
                authorization.revoke()
                unifiedDrive.clearAllLocalState()
                confirmedRevisions.clearAll()
                catalogCache.clear()
                settingsStore.clearDriveConnection()
                BackupScheduler.sync(getApplication())
                _state.update {
                    it.copy(
                        busy = false,
                        busyLabel = null,
                        settings = settingsStore.snapshot(),
                        versions = emptyList(),
                        reconciliations = emptyMap(),
                        remoteTips = emptyMap(),
                        catalogRefreshing = false,
                        catalogUpdatedAtEpochMs = null,
                        catalogFromCache = false,
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

    fun setAutomaticEnabled(value: Boolean) = updateSettings { settingsStore.setAutomaticEnabled(value) }
    fun setCadence(value: BackupCadence) = updateSettings { settingsStore.setCadence(value) }
    fun setUnmeteredOnly(value: Boolean) = updateSettings { settingsStore.setUnmeteredOnly(value) }
    fun setChargingOnly(value: Boolean) = updateSettings { settingsStore.setChargingOnly(value) }
    fun setRetentionDays(value: Int?) = updateSettings { settingsStore.setRetentionDays(value) }
    fun setMaximumVersions(value: Int) = updateSettings { settingsStore.setMaximumVersions(value) }

    fun backupAllNow() = runBackup(null, "Backup total") { service ->
        val settings = settingsStore.snapshot()
        cleanupExpiredDeletedProjects(service)
        service.backupAll(retentionPolicy = retention(settings))
    }

    fun backupProjectNow(projectId: String) = runBackup(projectId, "Backup do projeto") { service ->
        val settings = settingsStore.snapshot()
        service.backupProject(projectId, retentionPolicy = retention(settings))
    }

    fun keepLocalVersion(
        projectId: String,
        remoteVersion: BackupVersionDescriptor,
    ) = runBackup(projectId, "Manter versão local") { service ->
        val settings = settingsStore.snapshot()
        service.keepLocal(
            projectId = projectId,
            remoteVersion = remoteVersion,
            retentionPolicy = retention(settings),
        )
    }

    fun useCloudVersion(
        version: BackupVersionDescriptor,
        onProjectsChanged: () -> Unit,
    ) = runBusy(null) {
        val operationId = UUID.randomUUID().toString()
        activityStore.record(
            operationId,
            version.projectId,
            UnifiedOperationKind.RESTORE,
            UnifiedOperationState.RUNNING,
            null,
            "Aplicando versão da nuvem",
        )
        registerCurrentActivityOperation(operationId)
        requireDriveConnected()
        try {
            val restored = BackupOperationLock.withLock {
                unifiedDrive.restoreVersion(version, DriveConflictAction.USE_DRIVE)
            }
            settingsStore.recordSuccess("Versão da nuvem aplicada: ${restored.name}")
            activityStore.record(
                operationId,
                version.projectId,
                UnifiedOperationKind.RESTORE,
                UnifiedOperationState.SUCCEEDED,
                100,
                "Versão da nuvem aplicada ao projeto",
            )
            withContext(Dispatchers.Main) { onProjectsChanged() }
            _state.update {
                it.copy(message = "Versão da nuvem aplicada com segurança.")
            }
        } catch (error: Throwable) {
            if (error is CancellationException) throw error
            activityStore.record(
                operationId,
                version.projectId,
                UnifiedOperationKind.RESTORE,
                UnifiedOperationState.FAILED,
                null,
                "Não foi possível aplicar a versão da nuvem",
                error.message,
            )
            throw error
        }
    }

    fun restoreVersion(version: BackupVersionDescriptor, onProjectsChanged: () -> Unit) = runBusy(null) {
        val operationId = UUID.randomUUID().toString()
        activityStore.record(operationId, version.projectId, UnifiedOperationKind.RESTORE, UnifiedOperationState.RUNNING, null, "Restaurando uma versão do projeto")
        registerCurrentActivityOperation(operationId)
        requireDriveConnected()
        try {
            val explicitlyDeleted = deletedProjectStore.all().containsKey(version.projectId)
            val restored = BackupOperationLock.withLock {
                unifiedDrive.restoreVersion(
                    version,
                    if (explicitlyDeleted) DriveConflictAction.USE_DRIVE else DriveConflictAction.IMPORT_AS_COPY,
                ).also { if (explicitlyDeleted) deletedProjectStore.remove(version.projectId) }
            }
            settingsStore.recordSuccess("Projeto restaurado: ${restored.name}")
            val success = if (explicitlyDeleted) "Projeto restaurado" else "Projeto restaurado como uma nova cópia local"
            activityStore.record(operationId, version.projectId, UnifiedOperationKind.RESTORE, UnifiedOperationState.SUCCEEDED, 100, success)
            withContext(Dispatchers.Main) { onProjectsChanged() }
            _state.update { it.copy(message = "$success.") }
        } catch (error: Throwable) {
            if (error is CancellationException) throw error
            activityStore.record(operationId, version.projectId, UnifiedOperationKind.RESTORE, UnifiedOperationState.FAILED, null, "Não foi possível restaurar o projeto", error.message)
            throw error
        }
    }

    fun deleteCloudProject(projectId: String) = runBusy(null, "Excluindo backups da nuvem…") {
        requireDriveConnected()
        BackupOperationLock.withLock { unifiedDrive.deleteProjectBackups(projectId) }
        deletedProjectStore.remove(projectId)
        _state.update { it.copy(message = "Backups do projeto removidos da nuvem.") }
    }

    fun restoreLatestAll(onProjectsChanged: () -> Unit) = runBusy(null) {
        val operationId = UUID.randomUUID().toString()
        activityStore.record(operationId, null, UnifiedOperationKind.RESTORE, UnifiedOperationState.RUNNING, null, "Restaurando os projetos mais recentes")
        registerCurrentActivityOperation(operationId)
        requireDriveConnected()
        try {
            val results = BackupOperationLock.withLock { unifiedDrive.restoreLatestAll() }
            val ok = results.count { it.succeeded }
            val failed = results.size - ok
            if (ok > 0) withContext(Dispatchers.Main) { onProjectsChanged() }
            val summary = "Restauração total: ${countLabel(ok, "projeto restaurado", "projetos restaurados")}${if (failed > 0) ", ${countLabel(failed, "falha", "falhas")}" else ""}."
            if (failed == 0) {
                settingsStore.recordSuccess(summary)
                activityStore.record(operationId, null, UnifiedOperationKind.RESTORE, UnifiedOperationState.SUCCEEDED, 100, summary)
                _state.update { it.copy(message = summary) }
            } else {
                val detail = results.firstOrNull { !it.succeeded }?.error ?: "Não foi possível restaurar todos os projetos."
                settingsStore.recordPartial(summary, detail)
                activityStore.record(operationId, null, UnifiedOperationKind.RESTORE, UnifiedOperationState.FAILED, null, "Restauração parcial", detail)
                _state.update { it.copy(error = detail, message = summary) }
            }
        } catch (error: Throwable) {
            if (error is CancellationException) throw error
            activityStore.record(operationId, null, UnifiedOperationKind.RESTORE, UnifiedOperationState.FAILED, null, "Não foi possível restaurar os projetos", error.message)
            throw error
        }
    }

    fun clearMessage() = _state.update { it.copy(message = null, error = null) }

    private suspend fun finishDriveConnection() {
        val label = unifiedDrive.probeReadWriteDelete()
        settingsStore.setDriveConnected(label)
        BackupScheduler.sync(getApplication())
        _state.update {
            it.copy(
                busy = false,
                busyLabel = null,
                settings = settingsStore.snapshot(),
                authorizationRequired = false,
                message = "Google Drive conectado e validado.",
            )
        }
        refresh()
    }

    private fun runBackup(
        projectId: String?,
        label: String,
        action: suspend (UnifiedDriveProductionService) -> BackupRunReport,
    ) {
        if (_state.value.busy) return
        val automaticBackupActive = activityStore.snapshot().any {
            it.operationId == "automatic-backup" &&
                it.state.isActive &&
                System.currentTimeMillis() - it.updatedAtEpochMs < ACTIVE_OPERATION_STALE_MS
        }
        if (automaticBackupActive) {
            _state.update {
                it.copy(message = "Já existe um backup automático em andamento. O catálogo será atualizado quando ele terminar.")
            }
            return
        }
        BackupScheduler.cancelCoalesced(getApplication())
        runBusy(successMessage = null, busyLabel = "$label em andamento…") {
            val operationId = UUID.randomUUID().toString()
            activityStore.record(operationId, projectId, UnifiedOperationKind.BACKUP, UnifiedOperationState.RUNNING, null, label)
            registerCurrentActivityOperation(operationId)
            try {
                requireDriveConnected()
                val report = BackupOperationLock.withLock { action(unifiedDrive) }
                val summary = report.userSummary(label)
                val failureDetail = report.userFailureDetail()
                if (failureDetail == null) {
                    settingsStore.recordSuccess(summary)
                    activityStore.record(operationId, projectId, UnifiedOperationKind.BACKUP, UnifiedOperationState.SUCCEEDED, 100, summary)
                    _state.update { it.copy(message = summary) }
                } else {
                    settingsStore.recordPartial(summary, failureDetail)
                    activityStore.record(operationId, projectId, UnifiedOperationKind.BACKUP, UnifiedOperationState.FAILED, null, "Backup parcial", failureDetail)
                    _state.update { it.copy(message = summary, error = failureDetail) }
                }
            } catch (error: Throwable) {
                if (error is CancellationException) throw error
                activityStore.record(operationId, projectId, UnifiedOperationKind.BACKUP, UnifiedOperationState.FAILED, null, "Não foi possível concluir o backup", error.message)
                throw error
            }
        }
    }

    private fun requireDriveConnected() {
        require(settingsStore.snapshot().driveConnected) { "Conecte primeiro o Google Drive." }
    }

    private fun retention(settings: BackupSettingsSnapshot) = BackupRetentionPolicy(settings.retentionDays, settings.maximumVersions)

    private suspend fun cleanupExpiredDeletedProjects(service: UnifiedDriveProductionService) {
        DeletedProjectRetentionPolicy.expired(deletedProjectStore.all().values, System.currentTimeMillis()).forEach { tombstone ->
            service.deleteProjectBackups(tombstone.projectId)
            deletedProjectStore.remove(tombstone.projectId)
        }
    }

    private fun updateSettings(change: () -> Unit) {
        change()
        BackupScheduler.sync(getApplication())
        _state.update { it.copy(settings = settingsStore.snapshot()) }
    }

    private fun runBusy(
        successMessage: String?,
        busyLabel: String = "Concluindo operação…",
        action: suspend () -> Unit,
    ) {
        if (_state.value.busy) return
        _state.update { it.copy(busy = true, busyLabel = busyLabel, error = null, message = null) }
        viewModelScope.launch {
            try {
                action()
                _state.update { it.copy(busy = false, busyLabel = null, message = it.message ?: successMessage) }
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
                busyLabel = null,
                authorizationRequired = error is DriveAuthorizationRequiredException || it.authorizationRequired,
                error = message,
            )
        }
    }

    private fun observeAutomaticBackupCompletion() {
        viewModelScope.launch {
            activityStore.records
                .map { records ->
                    records.firstOrNull {
                        it.operationId == "automatic-backup" && !it.state.isActive
                    }?.updatedAtEpochMs
                }
                .distinctUntilChanged()
                .drop(1)
                .collect { completedAt ->
                    if (completedAt != null && settingsStore.snapshot().driveConnected) refresh()
                }
        }
    }

    private suspend fun registerCurrentActivityOperation(operationId: String) {
        val job = kotlinx.coroutines.currentCoroutineContext()[Job] ?: return
        ActivityCancellationRegistry.register(operationId, job)
    }

    private companion object {
        const val ACTIVE_OPERATION_STALE_MS = 30L * 60L * 1_000L
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
