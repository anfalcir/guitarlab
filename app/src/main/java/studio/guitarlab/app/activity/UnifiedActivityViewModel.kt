package studio.guitarlab.app.activity

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import studio.guitarlab.core.project.UnifiedActivityPolicy
import studio.guitarlab.core.project.UnifiedOperationRecord

data class UnifiedActivityUiState(
    val records: List<UnifiedOperationRecord> = emptyList(),
    val active: UnifiedOperationRecord? = null,
    val historyCount: Int = 0,
    val cancellingOperationIds: Set<String> = emptySet(),
    val message: String? = null,
)

class UnifiedActivityViewModel(application: Application) : AndroidViewModel(application) {
    private val store = UnifiedActivityStore(application)
    private val cancellation = ActivityCancellationCoordinator(application)
    private val transient = MutableStateFlow(UnifiedActivityUiState())

    val state: StateFlow<UnifiedActivityUiState> = combine(store.records, transient) { records, ui ->
        val ordered = UnifiedActivityPolicy.ordered(records)
        ui.copy(
            records = ordered,
            active = ordered.firstOrNull { it.state.isActive },
            historyCount = ordered.count(UnifiedActivityPolicy::isHistorical),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UnifiedActivityUiState())

    fun clearHistory() {
        store.clearHistory()
    }

    fun cancel(record: UnifiedOperationRecord) {
        if (!record.state.isActive || record.operationId in transient.value.cancellingOperationIds) return
        transient.update {
            it.copy(
                cancellingOperationIds = it.cancellingOperationIds + record.operationId,
                message = null,
            )
        }
        viewModelScope.launch {
            val message = runCatching { cancellation.cancel(record).message }
                .getOrElse { error -> error.message ?: "Não foi possível cancelar a atividade." }
            transient.update {
                it.copy(
                    cancellingOperationIds = it.cancellingOperationIds - record.operationId,
                    message = message,
                )
            }
        }
    }

    fun clearMessage() {
        transient.update { it.copy(message = null) }
    }
}
