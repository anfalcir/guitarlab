package studio.guitarlab.app.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import studio.guitarlab.core.model.GuitarProject
import studio.guitarlab.core.model.ProjectFactory
import studio.guitarlab.core.model.ProjectTemplate
import studio.guitarlab.core.project.FileProjectRepository
import studio.guitarlab.core.project.ProjectBundleReader
import studio.guitarlab.core.project.ProjectRecordingMediaStore
import studio.guitarlab.platform.codec.android.MasterExportFormat

data class HomeUiState(
    val loading: Boolean = true,
    val projects: List<GuitarProject> = emptyList(),
    val error: String? = null,
    val exportBusy: Boolean = false,
    val message: String? = null,
)

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = FileProjectRepository(application.filesDir)
    private val bundleReader = ProjectBundleReader(application.filesDir)
    private val recordingMediaStore = ProjectRecordingMediaStore(application.filesDir)
    private val factory = ProjectFactory()
    private val exportService = ProjectExportService(application)
    private var refreshJob: Job? = null

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
                .onSuccess { projects -> _state.value = HomeUiState(loading = false, projects = projects) }
                .onFailure { error -> _state.value = HomeUiState(loading = false, error = error.message ?: "Não foi possível carregar os projetos.") }
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
            withContext(Dispatchers.IO) { repository.delete(projectId) }
            refresh()
        }
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
            }.onSuccess { refresh() }
                .onFailure { error -> _state.update { it.copy(error = error.message ?: "Não foi possível renomear o projeto.") } }
        }
    }

    fun saveProjectPackage(projectId: String, uri: Uri) = launchExport("Projeto GuitarLab salvo com sucesso") {
        exportService.saveProject(projectId, uri)
    }

    fun exportMaster(projectId: String, uri: Uri, format: MasterExportFormat) = launchExport("Master ${format.name} exportado com sucesso") {
        exportService.exportMaster(projectId, uri, format)
    }

    fun clearMessage() { _state.update { it.copy(message = null, error = null) } }

    private fun launchExport(success: String, action: suspend () -> Unit) {
        if (_state.value.exportBusy) return
        viewModelScope.launch {
            _state.update { it.copy(exportBusy = true, error = null, message = null) }
            runCatching { action() }
                .onSuccess { _state.update { it.copy(exportBusy = false, message = success) } }
                .onFailure { error -> _state.update { it.copy(exportBusy = false, error = error.message ?: "Não foi possível exportar o projeto.") } }
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
            }.onSuccess { refresh() }
                .onFailure { error -> _state.update { it.copy(error = error.message ?: "Não foi possível duplicar o projeto.") } }
        }
    }
}
