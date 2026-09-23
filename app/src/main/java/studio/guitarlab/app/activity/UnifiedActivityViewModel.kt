package studio.guitarlab.app.activity

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import studio.guitarlab.core.project.UnifiedActivityPolicy
import studio.guitarlab.core.project.UnifiedOperationRecord

data class UnifiedActivityUiState(
    val records: List<UnifiedOperationRecord> = emptyList(),
    val active: UnifiedOperationRecord? = null,
    val historyCount: Int = 0,
)

class UnifiedActivityViewModel(application: Application) : AndroidViewModel(application) {
    private val store = UnifiedActivityStore(application)
    val state: StateFlow<UnifiedActivityUiState> = store.records
        .map { records ->
            val ordered = UnifiedActivityPolicy.ordered(records)
            UnifiedActivityUiState(
                records = ordered,
                active = ordered.firstOrNull { it.state.isActive },
                historyCount = ordered.count(UnifiedActivityPolicy::isHistorical),
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UnifiedActivityUiState())

    fun clearHistory() {
        store.clearHistory()
    }
}
