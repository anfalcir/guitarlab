package studio.guitarlab.app.activity

import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job

/**
 * Process-local bridge between Activity and live coroutine jobs.
 *
 * Durable/background providers are cancelled separately. This registry only gives Activity a way
 * to stop work that is still owned by an in-process ViewModel.
 */
object ActivityCancellationRegistry {
    private val cancellers = ConcurrentHashMap<String, () -> Unit>()

    fun register(operationId: String, job: Job) {
        require(operationId.isNotBlank())
        cancellers[operationId] = {
            job.cancel(CancellationException("Cancelled from Activity"))
        }
        job.invokeOnCompletion {
            cancellers.remove(operationId)
        }
    }

    fun unregister(operationId: String) {
        cancellers.remove(operationId)
    }

    fun cancel(operationId: String): Boolean =
        cancellers.remove(operationId)?.let { cancel ->
            cancel()
            true
        } ?: false
}
