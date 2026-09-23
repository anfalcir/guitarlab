package studio.guitarlab.platform.separation

import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.functions.FirebaseFunctionsException
import com.google.firebase.storage.StorageException
import java.io.IOException

internal enum class RemotePipelineStage {
    AUTHENTICATING,
    CHECKING_REMOTE,
    UPLOADING,
    ENQUEUEING,
    DOWNLOADING_RESULTS,
    ACKNOWLEDGING,
    CANCELLING,
}

internal class RemotePipelineException(
    val stage: RemotePipelineStage,
    cause: Throwable,
) : RuntimeException(cause)

internal data class RemoteFailure(
    val code: String,
    val retryable: Boolean,
    val stage: RemotePipelineStage?,
) {
    fun durableCode(attempt: Int): String =
        if (retryable) "RETRY:${stage?.name ?: "UNKNOWN"}:$code:ATTEMPT_${attempt + 1}"
        else "TERMINAL:${stage?.name ?: "UNKNOWN"}:$code"
}

internal object RemoteFirebaseFailureClassifier {
    fun classify(error: Throwable): RemoteFailure {
        val chain = generateSequence(error) { it.cause }.toList()
        val stage = chain.filterIsInstance<RemotePipelineException>().firstOrNull()?.stage

        if (chain.any { it is RemoteAuthenticationRequiredException }) {
            return RemoteFailure("AUTH_REQUIRED", false, stage ?: RemotePipelineStage.AUTHENTICATING)
        }
        chain.filterIsInstance<FirebaseAuthException>().firstOrNull()?.let {
            return classifyAuth(it.errorCode, stage ?: RemotePipelineStage.AUTHENTICATING)
        }
        chain.filterIsInstance<FirebaseFirestoreException>().firstOrNull()?.let {
            return classifyFirestore(it.code.name, stage ?: RemotePipelineStage.CHECKING_REMOTE)
        }
        chain.filterIsInstance<FirebaseFunctionsException>().firstOrNull()?.let {
            return classifyFunctions(it.code.name, stage ?: RemotePipelineStage.ENQUEUEING)
        }
        chain.filterIsInstance<StorageException>().firstOrNull()?.let {
            return classifyStorage(it.errorCode, stage ?: RemotePipelineStage.UPLOADING)
        }
        if (chain.any { it is FirebaseNetworkException }) return RemoteFailure("NETWORK_UNAVAILABLE", true, stage)
        if (chain.any { it is IOException }) return RemoteFailure("NETWORK_IO", true, stage)

        val root = chain.last()
        return RemoteFailure("UNEXPECTED_${root.javaClass.simpleName.uppercase().take(40)}", false, stage)
    }

    internal fun classifyAuth(code: String, stage: RemotePipelineStage? = RemotePipelineStage.AUTHENTICATING): RemoteFailure {
        val normalized = code.removePrefix("ERROR_").uppercase()
        val retryable = normalized in setOf("NETWORK_REQUEST_FAILED", "INTERNAL_ERROR")
        val durable = when (normalized) {
            "OPERATION_NOT_ALLOWED" -> "AUTH_PROVIDER_DISABLED"
            "INVALID_API_KEY", "APP_NOT_AUTHORIZED", "INVALID_CREDENTIAL" -> "AUTH_CONFIG_INVALID"
            "TOO_MANY_REQUESTS" -> "AUTH_QUOTA_EXCEEDED"
            "NETWORK_REQUEST_FAILED" -> "NETWORK_UNAVAILABLE"
            else -> "AUTH_$normalized"
        }
        return RemoteFailure(durable, retryable, stage)
    }

    internal fun classifyFirestore(code: String, stage: RemotePipelineStage? = RemotePipelineStage.CHECKING_REMOTE): RemoteFailure {
        val normalized = code.uppercase()
        val retryable = normalized in setOf("UNAVAILABLE", "DEADLINE_EXCEEDED", "ABORTED", "INTERNAL")
        return RemoteFailure("FIRESTORE_$normalized", retryable, stage)
    }

    internal fun classifyFunctions(code: String, stage: RemotePipelineStage? = RemotePipelineStage.ENQUEUEING): RemoteFailure {
        val normalized = code.uppercase()
        val retryable = normalized in setOf("UNAVAILABLE", "DEADLINE_EXCEEDED", "ABORTED", "INTERNAL")
        return RemoteFailure("FUNCTIONS_$normalized", retryable, stage)
    }

    internal fun classifyStorage(errorCode: Int, stage: RemotePipelineStage? = RemotePipelineStage.UPLOADING): RemoteFailure {
        return when (errorCode) {
            StorageException.ERROR_NOT_AUTHENTICATED -> RemoteFailure("STORAGE_NOT_AUTHENTICATED", false, stage)
            StorageException.ERROR_NOT_AUTHORIZED -> RemoteFailure("STORAGE_NOT_AUTHORIZED", false, stage)
            StorageException.ERROR_BUCKET_NOT_FOUND -> RemoteFailure("STORAGE_BUCKET_NOT_FOUND", false, stage)
            StorageException.ERROR_PROJECT_NOT_FOUND -> RemoteFailure("STORAGE_PROJECT_NOT_FOUND", false, stage)
            StorageException.ERROR_QUOTA_EXCEEDED -> RemoteFailure("STORAGE_QUOTA_EXCEEDED", false, stage)
            StorageException.ERROR_RETRY_LIMIT_EXCEEDED -> RemoteFailure("STORAGE_RETRY_LIMIT_EXCEEDED", false, stage)
            StorageException.ERROR_CANCELED -> RemoteFailure("STORAGE_CANCELLED", false, stage)
            else -> RemoteFailure("STORAGE_UNKNOWN", true, stage)
        }
    }
}
