package studio.guitarlab.platform.separation

import com.google.firebase.storage.StorageException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RemoteFirebaseFailureClassifierTest {
    @Test fun disabledAuthProviderIsTerminalAndActionable() {
        val failure = RemoteFirebaseFailureClassifier.classifyAuth("ERROR_OPERATION_NOT_ALLOWED")
        assertEquals("AUTH_PROVIDER_DISABLED", failure.code)
        assertFalse(failure.retryable)
        assertEquals(RemotePipelineStage.AUTHENTICATING, failure.stage)
    }

    @Test fun missingStableSessionIsTerminalAndNeverRetries() {
        val failure = RemoteFirebaseFailureClassifier.classify(
            RemotePipelineException(RemotePipelineStage.AUTHENTICATING, RemoteAuthenticationRequiredException()),
        )
        assertEquals("AUTH_REQUIRED", failure.code)
        assertFalse(failure.retryable)
        assertEquals(RemotePipelineStage.AUTHENTICATING, failure.stage)
    }

    @Test fun authNetworkFailureRetries() {
        val failure = RemoteFirebaseFailureClassifier.classifyAuth("ERROR_NETWORK_REQUEST_FAILED")
        assertEquals("NETWORK_UNAVAILABLE", failure.code)
        assertTrue(failure.retryable)
    }

    @Test fun firestorePermissionDeniedNeverLoops() {
        val failure = RemoteFirebaseFailureClassifier.classifyFirestore("PERMISSION_DENIED")
        assertEquals("FIRESTORE_PERMISSION_DENIED", failure.code)
        assertFalse(failure.retryable)
    }

    @Test fun functionsUnavailableRetriesButPermissionDeniedDoesNot() {
        assertTrue(RemoteFirebaseFailureClassifier.classifyFunctions("UNAVAILABLE").retryable)
        assertFalse(RemoteFirebaseFailureClassifier.classifyFunctions("PERMISSION_DENIED").retryable)
    }

    @Test fun storageAuthAndRetryLimitAreTerminal() {
        assertFalse(RemoteFirebaseFailureClassifier.classifyStorage(StorageException.ERROR_NOT_AUTHENTICATED).retryable)
        assertFalse(RemoteFirebaseFailureClassifier.classifyStorage(StorageException.ERROR_NOT_AUTHORIZED).retryable)
        assertFalse(RemoteFirebaseFailureClassifier.classifyStorage(StorageException.ERROR_RETRY_LIMIT_EXCEEDED).retryable)
    }

    @Test fun durableRetryCodeCarriesStageAndAttempt() {
        val failure = RemoteFailure("NETWORK_UNAVAILABLE", true, RemotePipelineStage.UPLOADING)
        assertEquals("RETRY:UPLOADING:NETWORK_UNAVAILABLE:ATTEMPT_3", failure.durableCode(2))
    }

    @Test fun storageRetryLimitExceededIsRetryableForResultDownload() {
        val failure = RemoteFirebaseFailureClassifier.classifyStorage(
            com.google.firebase.storage.StorageException.ERROR_RETRY_LIMIT_EXCEEDED,
            RemotePipelineStage.DOWNLOADING_RESULTS,
        )
        assertEquals("STORAGE_RETRY_LIMIT_EXCEEDED", failure.code)
        assertTrue(failure.retryable)
        assertEquals(RemotePipelineStage.DOWNLOADING_RESULTS, failure.stage)
    }

    @Test fun resultValidationFailureIsTerminalButRemainsAnImportProblem() {
        val failure = RemoteFirebaseFailureClassifier.classify(
            RemotePipelineException(
                RemotePipelineStage.DOWNLOADING_RESULTS,
                RemoteResultValidationException("bad stem"),
            ),
        )
        assertEquals("RESULT_INVALID", failure.code)
        assertFalse(failure.retryable)
        assertEquals(RemotePipelineStage.DOWNLOADING_RESULTS, failure.stage)
    }

    @Test fun memoryPressureIsTypedInsteadOfMasqueradingAsRemoteExpiration() {
        val failure = RemoteFirebaseFailureClassifier.classify(
            RemotePipelineException(
                RemotePipelineStage.DOWNLOADING_RESULTS,
                OutOfMemoryError("simulated"),
            ),
        )
        assertEquals("CLIENT_MEMORY_PRESSURE", failure.code)
        assertFalse(failure.retryable)
        assertEquals(RemotePipelineStage.DOWNLOADING_RESULTS, failure.stage)
    }

}
