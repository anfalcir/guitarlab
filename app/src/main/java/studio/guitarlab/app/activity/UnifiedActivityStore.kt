package studio.guitarlab.app.activity

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import studio.guitarlab.app.diagnostics.DiagnosticJournal
import studio.guitarlab.core.project.UnifiedActivityPolicy
import studio.guitarlab.core.project.UnifiedOperationKind
import studio.guitarlab.core.project.UnifiedOperationRecord
import studio.guitarlab.core.project.UnifiedOperationState

/**
 * The durable, provider-neutral activity source for the Android layer.
 *
 * Workers write snapshots and every UI surface observes the same preference-backed stream. The
 * preference is deliberately small and bounded: finished history is useful for diagnostics, but
 * it must never grow with every export or retry forever.
 */
class UnifiedActivityStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
    private val journal = DiagnosticJournal(context.applicationContext)
    private val lock = Any()
    private val _records = MutableStateFlow(read())
    val records: StateFlow<List<UnifiedOperationRecord>> = _records.asStateFlow()

    private val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == RECORDS_KEY) _records.value = read()
    }

    init {
        preferences.registerOnSharedPreferenceChangeListener(listener)
    }

    fun snapshot(): List<UnifiedOperationRecord> = _records.value

    fun upsert(record: UnifiedOperationRecord) {
        synchronized(lock) {
            val next = (_records.value.filterNot { it.operationId == record.operationId } + record)
                .sortedByDescending { it.updatedAtEpochMs }
                .let(::bounded)
            write(next)
            _records.value = next
            journal.appendActivity(record)
        }
    }

    fun record(
        operationId: String,
        projectId: String?,
        kind: UnifiedOperationKind,
        state: UnifiedOperationState,
        progressPercent: Int?,
        summary: String,
        technicalDetail: String? = null,
        updatedAtEpochMs: Long = System.currentTimeMillis(),
    ) = upsert(
        UnifiedOperationRecord(
            operationId = operationId,
            projectId = projectId,
            kind = kind,
            state = state,
            progressPercent = progressPercent,
            updatedAtEpochMs = updatedAtEpochMs,
            summary = summary,
            technicalDetail = technicalDetail,
        ),
    )

    fun remove(operationId: String) {
        synchronized(lock) {
            val next = _records.value.filterNot { it.operationId == operationId }
            write(next)
            _records.value = next
        }
    }

    fun cancelActive(
        operationId: String,
        summary: String = "Operação cancelada pelo usuário",
        technicalDetail: String? = "USER_CANCELLED_FROM_ACTIVITY",
        nowEpochMs: Long = System.currentTimeMillis(),
    ): Boolean {
        val current = _records.value.firstOrNull { it.operationId == operationId } ?: return false
        if (!current.state.isActive) return false
        upsert(
            current.copy(
                state = UnifiedOperationState.CANCELLED,
                progressPercent = null,
                updatedAtEpochMs = nowEpochMs,
                summary = summary,
                technicalDetail = technicalDetail,
            ),
        )
        return true
    }

    fun clearHistory(): Int = synchronized(lock) {
        val before = _records.value
        val next = UnifiedActivityPolicy.clearHistory(before).let(::bounded)
        write(next)
        _records.value = next
        before.size - next.size
    }

    fun terminalizeProject(projectId: String, nowEpochMs: Long = System.currentTimeMillis()) {
        synchronized(lock) {
            val next = UnifiedActivityPolicy.terminalizeProject(_records.value, projectId, nowEpochMs).let(::bounded)
            write(next)
            _records.value = next
        }
    }

    fun reconcileMissingProjects(existingProjectIds: Set<String>, nowEpochMs: Long = System.currentTimeMillis()) {
        synchronized(lock) {
            val next = UnifiedActivityPolicy.terminalizeMissingProjects(_records.value, existingProjectIds, nowEpochMs).let(::bounded)
            if (next != _records.value) {
                write(next)
                _records.value = next
            }
        }
    }

    private fun read(): List<UnifiedOperationRecord> = synchronized(lock) {
        runCatching {
            val array = JSONArray(preferences.getString(RECORDS_KEY, "[]"))
            buildList {
                for (index in 0 until array.length()) {
                    val item = array.optJSONObject(index) ?: continue
                    runCatching {
                        UnifiedOperationRecord(
                            operationId = item.getString("operationId"),
                            projectId = item.optString("projectId").ifBlank { null },
                            kind = UnifiedOperationKind.valueOf(item.getString("kind")),
                            state = UnifiedOperationState.valueOf(item.getString("state")),
                            progressPercent = if (item.isNull("progressPercent")) null else item.optInt("progressPercent"),
                            updatedAtEpochMs = item.getLong("updatedAtEpochMs"),
                            summary = item.getString("summary"),
                            technicalDetail = item.optString("technicalDetail").ifBlank { null },
                        )
                    }.onSuccess(::add)
                }
            }.let(::bounded)
        }.getOrDefault(emptyList())
    }

    private fun write(records: List<UnifiedOperationRecord>) {
        val array = JSONArray()
        records.forEach { record ->
            array.put(
                JSONObject()
                    .put("operationId", record.operationId)
                    .put("projectId", record.projectId)
                    .put("kind", record.kind.name)
                    .put("state", record.state.name)
                    .put("progressPercent", record.progressPercent)
                    .put("updatedAtEpochMs", record.updatedAtEpochMs)
                    .put("summary", record.summary)
                    .put("technicalDetail", record.technicalDetail),
            )
        }
        preferences.edit().putString(RECORDS_KEY, array.toString()).apply()
    }

    private fun bounded(records: List<UnifiedOperationRecord>): List<UnifiedOperationRecord> {
        val active = records.filter { it.state.isActive }
        val history = records.filterNot { it.state.isActive }.take(MAX_HISTORY)
        return (active + history).distinctBy { it.operationId }
            .sortedWith(compareByDescending<UnifiedOperationRecord> { it.state.isActive }.thenByDescending { it.updatedAtEpochMs })
    }

    private companion object {
        const val PREFERENCES = "guitarlab_unified_activity"
        const val RECORDS_KEY = "records"
        const val MAX_HISTORY = 100
    }
}
