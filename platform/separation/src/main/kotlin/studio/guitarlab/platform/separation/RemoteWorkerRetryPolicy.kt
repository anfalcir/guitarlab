package studio.guitarlab.platform.separation

internal object RemoteWorkerRetryPolicy {
    const val MAX_FAILURE_ATTEMPTS = 6

    fun nextAttempt(previousErrorCode: String?, failure: RemoteFailure): Int {
        val prefix = "RETRY:${failure.stage?.name ?: "UNKNOWN"}:${failure.code}:ATTEMPT_"
        val previous = previousErrorCode
            ?.takeIf { it.startsWith(prefix) }
            ?.substringAfterLast("ATTEMPT_")
            ?.toIntOrNull()
            ?: 0
        return previous + 1
    }

    fun shouldRetry(failure: RemoteFailure, attempt: Int, maxAttempts: Int = MAX_FAILURE_ATTEMPTS): Boolean =
        failure.retryable && attempt <= maxAttempts

    fun durableCode(failure: RemoteFailure, attempt: Int): String =
        "RETRY:${failure.stage?.name ?: "UNKNOWN"}:${failure.code}:ATTEMPT_$attempt"
}
