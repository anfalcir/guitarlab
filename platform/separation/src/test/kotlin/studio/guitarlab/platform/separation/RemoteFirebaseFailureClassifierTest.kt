package studio.guitarlab.platform.separation

import com.google.firebase.storage.StorageException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RemoteFirebaseFailureClassifierTest {
    @Test fun disabledAnonymousAuthIsTerminalAndActionable() {
        val failure = RemoteFirebaseFailureClassifier.classifyAuth("ERROR_OPERATION_NOT_ALLOWED")
        assertEquals("AUTH_PROVIDER_DISABLED", failure.code)
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
}
