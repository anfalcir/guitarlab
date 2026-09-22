package studio.guitarlab.platform.source.android

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import org.json.JSONObject

enum class SourceOperationState { IDLE, RUNNING, RETRYING, SUCCESS, ERROR, CANCELLED }

data class SourceOperationSnapshot(
    val projectId: String,
    val operationId: String,
    val state: SourceOperationState,
    val progress: Int,
    val message: String,
    val updatedAtEpochMs: Long,
)

/** Durable project-scoped operation ownership and Activity-Center-ready progress state. */
class SourceOperationStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun begin(projectId: String, operationId: String, message: String) {
        require(projectId.isNotBlank() && operationId.isNotBlank())
        write(SourceOperationSnapshot(projectId, operationId, SourceOperationState.RUNNING, 0, message, System.currentTimeMillis()))
    }

    fun update(projectId: String, operationId: String, state: SourceOperationState, progress: Int, message: String) {
        if (!isCurrent(projectId, operationId)) return
        write(SourceOperationSnapshot(projectId, operationId, state, progress.coerceIn(0, 100), message, System.currentTimeMillis()))
    }

    fun snapshot(projectId: String): SourceOperationSnapshot? {
        val raw = prefs.getString(key(projectId), null) ?: return null
        return runCatching {
            val json = JSONObject(raw)
            SourceOperationSnapshot(
                projectId = projectId,
                operationId = json.getString("operationId"),
                state = SourceOperationState.valueOf(json.getString("state")),
                progress = json.optInt("progress", 0).coerceIn(0, 100),
                message = json.optString("message"),
                updatedAtEpochMs = json.optLong("updatedAtEpochMs", 0L),
            )
        }.getOrNull()
    }

    /**
     * Observable view of the durable project operation. The worker remains the owner of retries and
     * reconciliation; UI collectors only receive committed local state changes and never poll the backend.
     */
    fun observe(projectId: String): Flow<SourceOperationSnapshot?> = callbackFlow {
        val watchedKey = key(projectId)
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, changedKey ->
            if (changedKey == watchedKey) trySend(snapshot(projectId))
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        trySend(snapshot(projectId))
        awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }.distinctUntilChanged()

    fun isCurrent(projectId: String, operationId: String): Boolean = snapshot(projectId)?.operationId == operationId

    fun markCancelled(projectId: String) {
        val current = snapshot(projectId) ?: return
        update(projectId, current.operationId, SourceOperationState.CANCELLED, current.progress, "Aquisição cancelada.")
    }

    fun clear(projectId: String) {
        prefs.edit().remove(key(projectId)).apply()
    }

    private fun write(snapshot: SourceOperationSnapshot) {
        val json = JSONObject()
            .put("operationId", snapshot.operationId)
            .put("state", snapshot.state.name)
            .put("progress", snapshot.progress)
            .put("message", snapshot.message)
            .put("updatedAtEpochMs", snapshot.updatedAtEpochMs)
        prefs.edit().putString(key(snapshot.projectId), json.toString()).apply()
    }

    private fun key(projectId: String) = "project:$projectId"

    private companion object { const val PREFS = "guitarlab_source_operations" }
}
