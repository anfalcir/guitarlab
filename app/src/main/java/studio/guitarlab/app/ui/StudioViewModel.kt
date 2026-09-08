package studio.guitarlab.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import studio.guitarlab.core.model.GuitarProject
import studio.guitarlab.core.project.FileProjectRepository

data class StudioUiState(
    val loading: Boolean = true,
    val project: GuitarProject? = null,
    val error: String? = null,
)

class StudioViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = FileProjectRepository(application.filesDir)
    private val _state = MutableStateFlow(StudioUiState())
    val state: StateFlow<StudioUiState> = _state.asStateFlow()

    fun load(projectId: String) {
        if (_state.value.project?.id == projectId && !_state.value.loading) return
        viewModelScope.launch {
            _state.value = StudioUiState(loading = true)
            runCatching {
                withContext(Dispatchers.IO) { repository.load(projectId) }
                    ?: error("Project not found: $projectId")
            }.onSuccess { project ->
                _state.value = StudioUiState(loading = false, project = project)
            }.onFailure { error ->
                _state.value = StudioUiState(
                    loading = false,
                    error = error.message ?: "Unable to load project.",
                )
            }
        }
    }
}
