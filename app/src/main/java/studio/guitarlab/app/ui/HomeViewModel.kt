package studio.guitarlab.app.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import studio.guitarlab.app.activity.UnifiedActivityStore
import studio.guitarlab.app.backup.BackupScheduler
import studio.guitarlab.app.backup.ConfirmedRevisionStore
import studio.guitarlab.app.backup.UnifiedDriveProductionService
import studio.guitarlab.core.model.GuitarProject
import studio.guitarlab.core.model.ProjectFactory
import studio.guitarlab.core.model.ProjectTemplate
import studio.guitarlab.core.project.FileProjectRepository
import studio.guitarlab.core.project.ProjectBundleReader
import studio.guitarlab.core.project.ProjectContentFilter
import studio.guitarlab.core.project.ProjectLibraryIndex
import studio.guitarlab.core.project.ProjectLibraryPolicy
import studio.guitarlab.core.project.ProjectLibraryQuery
import studio.guitarlab.core.project.ProjectManagedMediaStore
import studio.guitarlab.core.project.PreparedReferenceService
import studio.guitarlab.core.project.ProjectRecordingMediaStore
import studio.guitarlab.core.project.ProjectSampleRateFilter
import studio.guitarlab.core.project.ProjectSortOrder
import studio.guitarlab.core.project.ProjectTemplateFilter
import studio.guitarlab.platform.codec.android.MasterExportFormat
import studio.guitarlab.core.source.RankedSourceCandidate
import studio.guitarlab.core.source.SourceSearchRequest
import studio.guitarlab.platform.source.android.SourceAcquisitionClient
import studio.guitarlab.platform.source.android.SourceOperationSnapshot
import studio.guitarlab.platform.source.android.SourceOperationState
import studio.guitarlab.core.separation.DurableRemoteJob
import studio.guitarlab.core.separation.RemoteJobState
import studio.guitarlab.platform.separation.RemoteSeparationClient
import studio.guitarlab.core.project.UnifiedOperationKind
import studio.guitarlab.core.project.UnifiedOperationState

data class HomeUiState(
    val loading: Boolean = true,
    val projects: List<GuitarProject> = emptyList(),
    val projectsById: Map<String, GuitarProject> = emptyMap(),
    val totalProjects: Int = 0,
    val libraryQuery: ProjectLibraryQuery = ProjectLibraryQuery(),
    val error: String? = null,
    val exportBusy: Boolean = false,
    val exportOperationLabel: String? = null,
    val message: String? = null,
    val sourceCandidatesByProject: Map<String, List<RankedSourceCandidate>> = emptyMap(),
    val sourceWarningsByProject: Map<String, List<String>> = emptyMap(),
    val sourceSearchBusyProjects: Set<String> = emptySet(),
    val sourceOperationsByProject: Map<String, SourceOperationSnapshot> = emptyMap(),
    val separationJobsByProject: Map<String, DurableRemoteJob> = emptyMap(),
    val preparedReferenceBusyProjects: Set<String> = emptySet(),
    val sourceReplacementProjects: Set<String> = emptySet(),
)

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = FileProjectRepository(application.filesDir)
    private val bundleReader = ProjectBundleReader(application.filesDir)
    private val recordingMediaStore = ProjectRecordingMediaStore(application.filesDir)
    private val factory = ProjectFactory()
    private val exportService = ProjectExportService(application)
    private val sourceAcquisition = SourceAcquisitionClient(application)
    private val separation = RemoteSeparationClient(application)
    private val preparedReferences = PreparedReferenceService(repository, ProjectManagedMediaStore(application.filesDir), application.cacheDir)
    private val activityStore = UnifiedActivityStore(application)
    private val confirmedRevisions = ConfirmedRevisionStore(application)
    private val unifiedDrive = UnifiedDriveProductionService(application)
    private val sourceSearchJobs = mutableMapOf<String, Job>()
    private val sourceSearchOperationIds = mutableMapOf<String, String>()
    private val preparedReferenceJobs = mutableMapOf<String, Job>()
    private val prepareObservationJobs = mutableMapOf<String, Job>()
    private val handledSourceSuccessOperations = mutableSetOf<String>()
    private val handledSeparationCompletions = mutableSetOf<String>()
    private var refreshJob: Job? = null
    private var exportJob: Job? = null
    private var projectLibraryIndex: ProjectLibraryIndex = ProjectLibraryPolicy.index(emptyList())

    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { runCatching { bundleReader.cleanupInterruptedImports() } }
            refresh()
        }
    }

    fun refresh() {
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            runCatching {
                withContext(Dispatchers.IO) {
                    repository.list().also { projects ->
                        projects.forEach { project -> runCatching { recordingMediaStore.repairInterrupted(project.id) } }
                    }
                }
            }
                .onSuccess { projects ->
                    projectLibraryIndex = ProjectLibraryPolicy.index(projects)
                    _state.update { current ->
                        current.copy(
                            loading = false,
                            projects = projectLibraryIndex.select(current.libraryQuery),
                            projectsById = projects.associateBy { it.id },
                            totalProjects = projectLibraryIndex.size,
                            error = null,
                        )
                    }
                }
                .onFailure { error ->
                    _state.update { current ->
                        current.copy(loading = false, error = error.message ?: "Não foi possível carregar os projetos.")
                    }
                }
        }
    }

    fun updateProjectSearch(text: String) = updateLibraryQuery { it.copy(searchText = text) }

    fun setProjectTemplateFilter(filter: ProjectTemplateFilter) = updateLibraryQuery { it.copy(template = filter) }

    fun setProjectContentFilter(filter: ProjectContentFilter) = updateLibraryQuery { it.copy(content = filter) }

    fun setProjectSampleRateFilter(filter: ProjectSampleRateFilter) = updateLibraryQuery { it.copy(sampleRate = filter) }

    fun setProjectSortOrder(order: ProjectSortOrder) = updateLibraryQuery { it.copy(sortOrder = order) }

    fun clearProjectFilters() = updateLibraryQuery { it.clearFilters() }

    fun clearProjectSearchAndFilters() = updateLibraryQuery { it.clearSearchAndFilters() }

    private fun updateLibraryQuery(transform: (ProjectLibraryQuery) -> ProjectLibraryQuery) {
        _state.update { current ->
            val nextQuery = transform(current.libraryQuery)
            current.copy(
                libraryQuery = nextQuery,
                projects = projectLibraryIndex.select(nextQuery),
                totalProjects = projectLibraryIndex.size,
            )
        }
    }


    fun startPrepareObservation(projectId: String) {
        if (prepareObservationJobs[projectId]?.isActive == true) return
        refreshSourceOperation(projectId)
        refreshSeparation(projectId)
        prepareObservationJobs[projectId] = viewModelScope.launch {
            launch {
                sourceAcquisition.observe(projectId).collect { snapshot ->
                    applySourceOperationSnapshot(projectId, snapshot)
                }
            }
            launch {
                separation.observeProject(projectId).collect { job ->
                    applySeparationSnapshot(projectId, job)
                }
            }
        }
    }

    fun stopPrepareObservation(projectId: String) {
        prepareObservationJobs.remove(projectId)?.cancel()
    }

    private fun applySourceOperationSnapshot(projectId: String, snapshot: SourceOperationSnapshot?) {
        _state.update { current ->
            current.copy(
                sourceOperationsByProject = if (snapshot == null) {
                    current.sourceOperationsByProject - projectId
                } else {
                    current.sourceOperationsByProject + (projectId to snapshot)
                },
            )
        }
        snapshot?.let { current ->
            val state = when (current.state) {
                SourceOperationState.RUNNING -> UnifiedOperationState.RUNNING
                SourceOperationState.RETRYING -> UnifiedOperationState.RETRYING
                SourceOperationState.SUCCESS -> UnifiedOperationState.SUCCEEDED
                SourceOperationState.ERROR -> UnifiedOperationState.FAILED
                SourceOperationState.CANCELLED -> UnifiedOperationState.CANCELLED
                SourceOperationState.IDLE -> return@let
            }
            activityStore.record(
                operationId = current.operationId,
                projectId = projectId,
                kind = UnifiedOperationKind.SOURCE_ACQUISITION,
                state = state,
                progressPercent = current.progress,
                summary = sourceActivitySummary(current.state),
                technicalDetail = current.message,
                updatedAtEpochMs = current.updatedAtEpochMs,
            )
        }
        if (snapshot?.state == SourceOperationState.SUCCESS && handledSourceSuccessOperations.add(snapshot.operationId)) {
            _state.update { current -> current.copy(sourceReplacementProjects = current.sourceReplacementProjects - projectId) }
            BackupScheduler.enqueueCoalesced(getApplication())
            refresh()
        }
    }

    private fun applySeparationSnapshot(projectId: String, job: DurableRemoteJob?) {
        _state.update { current ->
            current.copy(
                separationJobsByProject = if (job == null) {
                    current.separationJobsByProject - projectId
                } else {
                    current.separationJobsByProject + (projectId to job)
                },
            )
        }
        job?.let { current ->
            val state = when (current.state) {
                RemoteJobState.IMPORTED -> UnifiedOperationState.SUCCEEDED
                RemoteJobState.CANCELLED -> UnifiedOperationState.CANCELLED
                RemoteJobState.FAILED, RemoteJobState.EXPIRED -> UnifiedOperationState.FAILED
                RemoteJobState.QUEUED, RemoteJobState.READY -> UnifiedOperationState.QUEUED
                else -> UnifiedOperationState.RUNNING
            }
            activityStore.record(
                operationId = current.identity.jobId,
                projectId = projectId,
                kind = UnifiedOperationKind.SEPARATION,
                state = state,
                progressPercent = null,
                summary = separationActivitySummary(current.state),
                technicalDetail = current.errorCode,
                updatedAtEpochMs = current.updatedAtMs,
            )
        }
        if (job != null && job.state in TERMINAL_SEPARATION_STATES) {
            val completionKey = "${job.identity.jobId}:${job.updatedAtMs}:${job.errorCode.orEmpty()}"
            if (handledSeparationCompletions.add(completionKey)) refresh()
        }
    }

    fun createProject(name: String, template: ProjectTemplate, onCreated: (String) -> Unit) {
        viewModelScope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    val project = factory.create(name, template)
                    repository.save(project)
                }
            }.onSuccess { project ->
                BackupScheduler.enqueueCoalesced(getApplication())
                onCreated(project.id)
                refresh()
            }.onFailure { error ->
                _state.update { it.copy(error = error.message ?: "Não foi possível criar o projeto.") }
            }
        }
    }

    fun importProject(uri: Uri, onImported: (String) -> Unit) {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            runCatching {
                withContext(Dispatchers.IO) {
                    val input = getApplication<Application>().contentResolver.openInputStream(uri)
                        ?: error("O Android não conseguiu abrir o arquivo GuitarLab.")
                    input.use { bundleReader.read(it) }
                }
            }.onSuccess { project ->
                BackupScheduler.enqueueCoalesced(getApplication())
                onImported(project.id)
                refresh()
            }.onFailure { error ->
                _state.update { current ->
                    current.copy(
                        loading = false,
                        error = error.message ?: "Não foi possível restaurar o projeto GuitarLab.",
                    )
                }
            }
        }
    }

    fun deleteProject(projectId: String) {
        viewModelScope.launch {
            val hadActiveOperations = hasActiveProjectOperations(projectId)
            stopPrepareObservation(projectId)
            sourceSearchJobs.remove(projectId)?.cancelAndJoin()
            preparedReferenceJobs.remove(projectId)?.cancelAndJoin()
            sourceAcquisition.closeProject(projectId)
            separation.closeProject(projectId)
            runCatching { withContext(Dispatchers.IO) { repository.delete(projectId) } }
                .onSuccess {
                    _state.update { current ->
                        current.copy(
                            sourceCandidatesByProject = current.sourceCandidatesByProject - projectId,
                            sourceWarningsByProject = current.sourceWarningsByProject - projectId,
                            sourceSearchBusyProjects = current.sourceSearchBusyProjects - projectId,
                            sourceOperationsByProject = current.sourceOperationsByProject - projectId,
                            separationJobsByProject = current.separationJobsByProject - projectId,
                            preparedReferenceBusyProjects = current.preparedReferenceBusyProjects - projectId,
                            sourceReplacementProjects = current.sourceReplacementProjects - projectId,
                            message = if (hadActiveOperations) "Projeto excluído. As operações em andamento foram canceladas e desvinculadas." else "Projeto excluído.",
                        )
                    }
                    unifiedDrive.clearProjectLocalState(projectId)
                    confirmedRevisions.clearProject(projectId)
                    BackupScheduler.enqueueCoalesced(getApplication())
                    refresh()
                }
                .onFailure { error -> _state.update { it.copy(error = error.message ?: "Não foi possível excluir o projeto.") } }
        }
    }

    fun hasActiveProjectOperations(projectId: String): Boolean {
        val source = sourceAcquisition.snapshot(projectId)
        val sourceActive = source?.state in setOf(SourceOperationState.RUNNING, SourceOperationState.RETRYING)
        val separationState = separation.snapshotProject(projectId)?.state
        val separationActive = separationState != null && separationState !in TERMINAL_SEPARATION_STATES
        return sourceActive || separationActive || sourceSearchJobs[projectId]?.isActive == true || preparedReferenceJobs[projectId]?.isActive == true
    }

    fun beginSourceReplacement(projectId: String) {
        val project = _state.value.projectsById[projectId]
        if (project?.preparation?.sourceAssetId == null) return
        sourceSearchJobs.remove(projectId)?.cancel()
        sourceAcquisition.cancel(projectId)
        separation.snapshotProject(projectId)?.takeIf { it.state !in TERMINAL_SEPARATION_STATES }?.let { separation.cancel(projectId, it.identity.jobId) }
        preparedReferenceJobs.remove(projectId)?.cancel()
        _state.update { current ->
            current.copy(
                sourceReplacementProjects = current.sourceReplacementProjects + projectId,
                sourceCandidatesByProject = current.sourceCandidatesByProject - projectId,
                sourceWarningsByProject = current.sourceWarningsByProject - projectId,
                sourceSearchBusyProjects = current.sourceSearchBusyProjects - projectId,
                preparedReferenceBusyProjects = current.preparedReferenceBusyProjects - projectId,
                message = "Escolha a nova fonte. A fonte atual e o conteúdo do Studio permanecem preservados até a substituição ser validada.",
                error = null,
            )
        }
        refreshSourceOperation(projectId)
        refreshSeparation(projectId)
    }

    fun cancelSourceReplacement(projectId: String) {
        sourceAcquisition.cancel(projectId)
        _state.update { current ->
            current.copy(
                sourceReplacementProjects = current.sourceReplacementProjects - projectId,
                message = "Substituição cancelada. A fonte atual foi mantida.",
                error = null,
            )
        }
        refreshSourceOperation(projectId)
    }

    private fun sourceMutationAllowed(projectId: String): Boolean {
        val existingSource = _state.value.projectsById[projectId]?.preparation?.sourceAssetId
        if (existingSource != null && projectId !in _state.value.sourceReplacementProjects) {
            _state.update { it.copy(error = "Use ‘Trocar fonte’ antes de escolher outra fonte para este projeto.") }
            return false
        }
        return true
    }

    fun renameProject(projectId: String, name: String) {
        val normalized = name.trim().replace(Regex("\\s+"), " ")
        if (normalized.isBlank() || normalized.length > 80) {
            _state.update { it.copy(error = "O nome do projeto deve ter entre 1 e 80 caracteres.") }
            return
        }
        viewModelScope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    val project = repository.load(projectId) ?: error("Projeto não encontrado.")
                    repository.save(project.copy(name = normalized, updatedAtEpochMs = System.currentTimeMillis()))
                }
            }.onSuccess {
                val active = hasActiveProjectOperations(projectId)
                BackupScheduler.enqueueCoalesced(getApplication())
                _state.update { it.copy(message = if (active) "Projeto renomeado. As operações em andamento continuam vinculadas ao mesmo projeto." else "Projeto renomeado.") }
                refresh()
            }.onFailure { error -> _state.update { it.copy(error = error.message ?: "Não foi possível renomear o projeto.") } }
        }
    }

    fun searchSources(projectId: String, artist: String, song: String) {
        if (song.isBlank()) {
            _state.update { it.copy(error = "Informe o nome da música.") }
            return
        }
        sourceSearchJobs.remove(projectId)?.cancel()
        sourceSearchOperationIds.remove(projectId)?.let { replacedId ->
            activityStore.record(
                replacedId,
                projectId,
                UnifiedOperationKind.SOURCE_ACQUISITION,
                UnifiedOperationState.CANCELLED,
                null,
                "Pesquisa de fontes substituída",
            )
        }
        val operationId = UUID.randomUUID().toString()
        sourceSearchOperationIds[projectId] = operationId
        activityStore.record(
            operationId,
            projectId,
            UnifiedOperationKind.SOURCE_ACQUISITION,
            UnifiedOperationState.RUNNING,
            null,
            "Pesquisando fontes para a música",
        )
        sourceSearchJobs[projectId] = viewModelScope.launch {
            _state.update { current ->
                current.copy(
                    sourceSearchBusyProjects = current.sourceSearchBusyProjects + projectId,
                    sourceWarningsByProject = current.sourceWarningsByProject - projectId,
                    error = null,
                )
            }
            try {
                val result = withTimeout(SOURCE_SEARCH_TIMEOUT_MS) {
                    sourceAcquisition.search(SourceSearchRequest(artist = artist, song = song))
                }
                val providerFailure = result.candidates.isEmpty() && result.warnings.isNotEmpty()
                val emptyMessage = when {
                    result.candidates.isNotEmpty() -> null
                    providerFailure -> null
                    else -> "Nenhuma fonte automática compatível foi encontrada."
                }
                val providerFailureMessage = if (providerFailure) {
                    "A pesquisa não pôde consultar as fontes automáticas. Verifique a conexão e tente novamente."
                } else {
                    null
                }
                _state.update { current ->
                    current.copy(
                        sourceCandidatesByProject = current.sourceCandidatesByProject + (projectId to result.candidates),
                        sourceWarningsByProject = current.sourceWarningsByProject + (projectId to result.warnings),
                        message = emptyMessage,
                        error = providerFailureMessage,
                    )
                }
                activityStore.record(
                    operationId,
                    projectId,
                    UnifiedOperationKind.SOURCE_ACQUISITION,
                    if (providerFailure) UnifiedOperationState.FAILED else UnifiedOperationState.SUCCEEDED,
                    if (providerFailure) null else 100,
                    when {
                        providerFailure -> "Pesquisa de fontes indisponível"
                        result.candidates.isEmpty() -> "Pesquisa concluída sem fontes compatíveis"
                        result.candidates.size == 1 -> "1 fonte encontrada"
                        else -> "${result.candidates.size} fontes encontradas"
                    },
                    result.warnings.joinToString("\n").ifBlank { null },
                )
            } catch (_: TimeoutCancellationException) {
                val message = "A pesquisa demorou mais que o esperado. Verifique a conexão e tente novamente."
                _state.update { it.copy(error = message) }
                activityStore.record(operationId, projectId, UnifiedOperationKind.SOURCE_ACQUISITION, UnifiedOperationState.FAILED, null, "Pesquisa de fontes expirou", message)
            } catch (error: CancellationException) {
                activityStore.record(operationId, projectId, UnifiedOperationKind.SOURCE_ACQUISITION, UnifiedOperationState.CANCELLED, null, "Pesquisa de fontes cancelada")
                throw error
            } catch (error: Throwable) {
                val message = "Não foi possível pesquisar fontes. Verifique a conexão e tente novamente."
                _state.update { it.copy(error = message) }
                activityStore.record(
                    operationId,
                    projectId,
                    UnifiedOperationKind.SOURCE_ACQUISITION,
                    UnifiedOperationState.FAILED,
                    null,
                    "Não foi possível pesquisar fontes",
                    error.message,
                )
            } finally {
                if (sourceSearchOperationIds[projectId] == operationId) {
                    sourceSearchOperationIds.remove(projectId)
                    sourceSearchJobs.remove(projectId)
                    _state.update { current ->
                        current.copy(sourceSearchBusyProjects = current.sourceSearchBusyProjects - projectId)
                    }
                }
            }
        }
    }

    fun importSource(projectId: String, uri: Uri) {
        if (!sourceMutationAllowed(projectId)) return
        viewModelScope.launch {
            _state.update { it.copy(error = null, message = null) }
            runCatching { withContext(Dispatchers.IO) { sourceAcquisition.importLocal(projectId, uri) } }
                .onSuccess {
                    BackupScheduler.enqueueCoalesced(getApplication())
                    _state.update { current -> current.copy(
                        sourceReplacementProjects = current.sourceReplacementProjects - projectId,
                        message = "Fonte validada. Uma nova preparação foi iniciada sem alterar gravações ou edições do Studio.",
                    ) }
                    refreshSourceOperation(projectId)
                    refresh()
                }
                .onFailure { error ->
                    if (error is kotlinx.coroutines.CancellationException) return@onFailure
                    _state.update { it.copy(error = error.message ?: "Não foi possível importar a fonte.") }
                    refreshSourceOperation(projectId)
                }
        }
    }

    fun acquireSource(projectId: String, candidate: RankedSourceCandidate) {
        if (!sourceMutationAllowed(projectId)) return
        runCatching { sourceAcquisition.enqueueOnline(projectId, candidate) }
            .onSuccess { operationId ->
                handledSourceSuccessOperations.remove(operationId)
                refreshSourceOperation(projectId)
                _state.update { it.copy(message = "Aquisição iniciada em segundo plano.", error = null) }
            }
            .onFailure { error -> _state.update { it.copy(error = error.message ?: "Não foi possível iniciar a aquisição.") } }
    }

    fun cancelSourceAcquisition(projectId: String) {
        sourceAcquisition.cancel(projectId)
        refreshSourceOperation(projectId)
    }

    fun refreshSourceOperation(projectId: String) {
        applySourceOperationSnapshot(projectId, sourceAcquisition.snapshot(projectId))
    }

    fun startSeparation(projectId: String) = runCatching { separation.enqueue(projectId) }
        .onSuccess { _state.update { it.copy(message = "Separação Demucs iniciada em segundo plano.", error = null) }; refreshSeparation(projectId); refresh() }
        .onFailure { error -> _state.update { it.copy(error = error.message ?: "Não foi possível iniciar a separação.") } }

    fun refreshSeparation(projectId: String) {
        applySeparationSnapshot(projectId, separation.snapshotProject(projectId))
    }

    fun cancelSeparation(projectId:String){separation.snapshotProject(projectId)?.let{separation.cancel(projectId,it.identity.jobId)};refreshSeparation(projectId)}

    fun prepareReferences(projectId: String) {
        if (preparedReferenceJobs[projectId]?.isActive == true) return
        val activityId = referenceActivityId(projectId)
        activityStore.record(
            operationId = activityId,
            projectId = projectId,
            kind = UnifiedOperationKind.REFERENCE_PREPARATION,
            state = UnifiedOperationState.RUNNING,
            progressPercent = 10,
            summary = "Preparando referências para o Studio",
        )
        val job = viewModelScope.launch {
            _state.update { it.copy(preparedReferenceBusyProjects = it.preparedReferenceBusyProjects + projectId, error = null, message = null) }
            try {
                val result = withContext(Dispatchers.IO) { preparedReferences.prepare(projectId) }
                BackupScheduler.enqueueCoalesced(getApplication())
                _state.update { it.copy(
                    message = if (result.reusedExisting) "Referências preparadas já estavam atualizadas." else "Base e referência preparadas para o Studio.",
                ) }
                activityStore.record(
                    operationId = activityId,
                    projectId = projectId,
                    kind = UnifiedOperationKind.REFERENCE_PREPARATION,
                    state = UnifiedOperationState.SUCCEEDED,
                    progressPercent = 100,
                    summary = "Referências prontas para o Studio",
                )
                refresh()
            } catch (_: CancellationException) {
                activityStore.record(
                    operationId = activityId,
                    projectId = projectId,
                    kind = UnifiedOperationKind.REFERENCE_PREPARATION,
                    state = UnifiedOperationState.CANCELLED,
                    progressPercent = null,
                    summary = "Preparação de referências cancelada",
                )
                _state.update { it.copy(message = "Preparação de referências cancelada.") }
            } catch (error: Throwable) {
                activityStore.record(
                    operationId = activityId,
                    projectId = projectId,
                    kind = UnifiedOperationKind.REFERENCE_PREPARATION,
                    state = UnifiedOperationState.FAILED,
                    progressPercent = null,
                    summary = "Não foi possível preparar as referências",
                    technicalDetail = error.message,
                )
                _state.update { it.copy(error = error.message ?: "Não foi possível preparar a base e a guitarra de referência.") }
            } finally {
                preparedReferenceJobs.remove(projectId)
                _state.update { it.copy(preparedReferenceBusyProjects = it.preparedReferenceBusyProjects - projectId) }
            }
        }
        preparedReferenceJobs[projectId] = job
    }

    fun saveProjectPackage(projectId: String, uri: Uri) = launchExport(projectId, "Projeto GuitarLab") {
        exportService.saveProject(projectId, uri)
        "Projeto GuitarLab salvo com sucesso."
    }

    fun exportStudyReference(projectId: String, kind: StudyExportKind, uri: Uri, format: MasterExportFormat) =
        launchExport(projectId, "${kind.label} ${format.name}") {
            val result = exportService.exportStudyReference(projectId, kind, uri, format)
            val detail = when (result.strategy) {
                StudyExportStrategy.DIRECT_CANONICAL_WAV -> "WAV publicado diretamente, sem conversão."
                StudyExportStrategy.SINGLE_ENCODE -> "${format.name} gerado uma única vez a partir da referência canônica."
            }
            "${kind.label} exportado com sucesso. $detail"
        }

    fun exportMaster(projectId: String, uri: Uri, format: MasterExportFormat) =
        launchExport(projectId, "Master ${format.name}") {
            exportService.exportMaster(projectId, uri, format)
            "Master ${format.name} exportado com sucesso."
        }

    fun cancelExport() { exportJob?.cancel() }

    fun clearMessage() { _state.update { it.copy(message = null, error = null) } }

    private fun launchExport(projectId: String, label: String, action: suspend () -> String) {
        if (_state.value.exportBusy) return
        val operationId = UUID.randomUUID().toString()
        activityStore.record(
            operationId = operationId,
            projectId = projectId,
            kind = UnifiedOperationKind.EXPORT,
            state = UnifiedOperationState.RUNNING,
            progressPercent = null,
            summary = "Exportando $label",
        )
        exportJob = viewModelScope.launch {
            _state.update { it.copy(exportBusy = true, exportOperationLabel = label, error = null, message = null) }
            try {
                val success = action()
                activityStore.record(operationId, projectId, UnifiedOperationKind.EXPORT, UnifiedOperationState.SUCCEEDED, 100, success)
                _state.update { it.copy(exportBusy = false, exportOperationLabel = null, message = success) }
            } catch (_: CancellationException) {
                activityStore.record(operationId, projectId, UnifiedOperationKind.EXPORT, UnifiedOperationState.CANCELLED, null, "Exportação cancelada")
                _state.update { it.copy(exportBusy = false, exportOperationLabel = null, message = "Exportação cancelada.") }
            } catch (error: Throwable) {
                activityStore.record(operationId, projectId, UnifiedOperationKind.EXPORT, UnifiedOperationState.FAILED, null, "Não foi possível concluir a exportação", error.message)
                _state.update { it.copy(exportBusy = false, exportOperationLabel = null, error = error.message ?: "Não foi possível exportar o projeto.") }
            } finally {
                exportJob = null
            }
        }
    }

    fun duplicateProject(project: GuitarProject) {
        viewModelScope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    repository.duplicate(
                        projectId = project.id,
                        newName = "${project.name} - Cópia",
                        newProjectId = UUID.randomUUID().toString(),
                        nowEpochMs = System.currentTimeMillis(),
                    )
                }
            }.onSuccess {
                BackupScheduler.enqueueCoalesced(getApplication())
                _state.update { it.copy(message = "Cópia criada com o conteúdo durável do projeto, sem herdar operações em andamento.") }
                refresh()
            }.onFailure { error -> _state.update { it.copy(error = error.message ?: "Não foi possível duplicar o projeto.") } }
        }
    }

    private companion object {
        const val SOURCE_SEARCH_TIMEOUT_MS = 60_000L
        fun referenceActivityId(projectId: String) = "reference-preparation:$projectId"

        fun sourceActivitySummary(state: SourceOperationState): String = when (state) {
            SourceOperationState.RUNNING, SourceOperationState.RETRYING -> "Adquirindo fonte"
            SourceOperationState.SUCCESS -> "Fonte pronta para separação"
            SourceOperationState.ERROR -> "Não foi possível adquirir a fonte"
            SourceOperationState.CANCELLED -> "Aquisição de fonte cancelada"
            SourceOperationState.IDLE -> "Aquisição de fonte aguardando"
        }

        fun separationActivitySummary(state: RemoteJobState): String = when (state) {
            RemoteJobState.IMPORTED -> "Separação e referências concluídas"
            RemoteJobState.FAILED, RemoteJobState.EXPIRED -> "Não foi possível concluir a separação"
            RemoteJobState.CANCEL_REQUESTED -> "Cancelamento da separação solicitado"
            RemoteJobState.CANCELLED -> "Separação cancelada"
            RemoteJobState.QUEUED, RemoteJobState.READY -> "Separação aguardando processamento"
            else -> "Separando fonte"
        }

        val TERMINAL_SEPARATION_STATES = setOf(RemoteJobState.IMPORTED, RemoteJobState.CANCELLED, RemoteJobState.FAILED, RemoteJobState.EXPIRED)
    }
}
