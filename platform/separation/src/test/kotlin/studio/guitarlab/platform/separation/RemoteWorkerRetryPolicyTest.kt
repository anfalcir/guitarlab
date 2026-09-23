package studio.guitarlab.platform.separation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RemoteWorkerRetryPolicyTest {
    @Test fun pollingAttemptsDoNotConsumeFailureBudget() {
        val failure = RemoteFailure("NETWORK_IO", true, RemotePipelineStage.DOWNLOADING_RESULTS)
        assertEquals(1, RemoteWorkerRetryPolicy.nextAttempt(null, failure))
        assertEquals(
            1,
            RemoteWorkerRetryPolicy.nextAttempt("REMOTE_STATE:RUNNING:POLL_99", failure),
        )
    }

    @Test fun sameFailureStageAdvancesDurableAttemptCounter() {
        val failure = RemoteFailure("NETWORK_IO", true, RemotePipelineStage.DOWNLOADING_RESULTS)
        assertEquals(
            3,
            RemoteWorkerRetryPolicy.nextAttempt(
                "RETRY:DOWNLOADING_RESULTS:NETWORK_IO:ATTEMPT_2",
                failure,
            ),
        )
    }

    @Test fun differentFailureSignatureGetsFreshBudget() {
        val failure = RemoteFailure("STORAGE_RETRY_LIMIT_EXCEEDED", true, RemotePipelineStage.DOWNLOADING_RESULTS)
        assertEquals(
            1,
            RemoteWorkerRetryPolicy.nextAttempt(
                "RETRY:UPLOADING:NETWORK_IO:ATTEMPT_4",
                failure,
            ),
        )
    }

    @Test fun retryBudgetIsBounded() {
        val failure = RemoteFailure("NETWORK_IO", true, RemotePipelineStage.DOWNLOADING_RESULTS)
        assertTrue(RemoteWorkerRetryPolicy.shouldRetry(failure, 6))
        assertFalse(RemoteWorkerRetryPolicy.shouldRetry(failure, 7))
        assertFalse(RemoteWorkerRetryPolicy.shouldRetry(failure.copy(retryable = false), 1))
    }
}
