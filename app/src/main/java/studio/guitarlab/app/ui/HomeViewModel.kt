package studio.guitarlab.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import java.util.UUID
import kotlinx.coroutines.Dispatchers
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

data class HomeUiState(
    val loading: Boolean = true,
    val projects: List<GuitarProject> = emptyList(),
    val error: String? = null,
)

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = FileProjectRepository(application.filesDir)
    private val factory = ProjectFactory()

    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            runCatching { withContext(Dispatchers.IO) { repository.list() } }
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
                refresh()
                onCreated(project.id)
            }.onFailure { error ->
                _state.update { it.copy(error = error.message ?: "Não foi possível criar o projeto.") }
            }
        }
    }

    fun deleteProject(projectId: String) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { repository.delete(projectId) }
            refresh()
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
