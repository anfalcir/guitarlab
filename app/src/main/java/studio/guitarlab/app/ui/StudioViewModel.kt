package studio.guitarlab.app.ui

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import studio.guitarlab.core.codec.WavMetadataReader
import studio.guitarlab.core.model.AudioClip
import studio.guitarlab.core.model.GuitarProject
import studio.guitarlab.core.project.FileProjectRepository
import studio.guitarlab.platform.codec.android.AndroidAudioDocumentSourceFactory

data class StudioUiState(
    val loading: Boolean = true,
    val importing: Boolean = false,
    val project: GuitarProject? = null,
    val error: String? = null,
    val importStatus: String? = null,
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

    fun importWav(trackId: String, uri: Uri) {
        val current = _state.value.project ?: return
        if (current.tracks.none { it.id == trackId }) {
            _state.value = _state.value.copy(error = "The selected destination track no longer exists.")
            return
        }

        viewModelScope.launch {
            _state.value = _state.value.copy(importing = true, error = null, importStatus = "Validating WAV…")
            runCatching {
                withContext(Dispatchers.IO) {
                    val context = getApplication<Application>()
                    runCatching {
                        context.contentResolver.takePersistableUriPermission(
                            uri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION,
                        )
                    }

                    val metadata = AndroidAudioDocumentSourceFactory.open(context, uri).use { source ->
                        WavMetadataReader().read(source)
                    }
                    require(metadata.totalFrames > 0) { "Selected WAV contains no complete audio frames." }

                    val name = queryDisplayName(uri)
                        ?.takeIf { it.isNotBlank() }
                        ?: "Imported WAV"
                    val clip = AudioClip(
                        id = UUID.randomUUID().toString(),
                        trackId = trackId,
                        name = name,
                        sourceUri = uri.toString(),
                        startFrame = 0,
                        sourceStartFrame = 0,
                        lengthFrames = metadata.totalFrames,
                        sourceFormat = metadata.fileFormat.name,
                        sourceSampleRateHz = metadata.sampleRateHz,
                        sourceChannelCount = metadata.channelCount,
                        sourceBitsPerSample = metadata.bitsPerSample,
                        sourceEncoding = metadata.sampleEncoding.name,
                    )
                    repository.save(
                        current.copy(
                            clips = current.clips + clip,
                            updatedAtEpochMs = System.currentTimeMillis(),
                        )
                    ) to metadata
                }
            }.onSuccess { (saved, metadata) ->
                _state.value = StudioUiState(
                    loading = false,
                    project = saved,
                    importStatus = "Imported ${metadata.channelCount}ch • ${metadata.sampleRateHz} Hz • ${metadata.totalFrames} frames",
                )
            }.onFailure { error ->
                _state.value = _state.value.copy(
                    importing = false,
                    error = error.message ?: "The WAV could not be imported.",
                    importStatus = null,
                )
            }
        }
    }

    private fun queryDisplayName(uri: Uri): String? {
        val resolver = getApplication<Application>().contentResolver
        return runCatching {
            resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                if (!cursor.moveToFirst()) return@use null
                val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index >= 0) cursor.getString(index) else null
            }
        }.getOrNull()
    }
}
