package studio.guitarlab.platform.separation

import java.util.UUID
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import studio.guitarlab.core.model.PreparationStatus
import studio.guitarlab.core.separation.DurableRemoteJob
import studio.guitarlab.core.separation.RemoteJobIdentity
import studio.guitarlab.core.separation.RemoteJobState
import studio.guitarlab.core.separation.RemoteSourceGeneration

class RemoteRecoveryPolicyTest {
    private fun job(state: RemoteJobState, updatedAtMs: Long, projectId: String = "project") =
        DurableRemoteJob(
            RemoteJobIdentity(UUID.randomUUID().toString(), projectId, "source", "a".repeat(64)),
            state,
            updatedAtMs,
        )

    @Test fun projectWithoutLocalJobRecoversAfterRestart() {
        assertTrue(RemoteRecoveryPolicy.shouldRestoreSourceReady(PreparationStatus.SEPARATING, hasActiveJob = false))
        assertFalse(RemoteRecoveryPolicy.shouldRestoreSourceReady(PreparationStatus.SEPARATING, hasActiveJob = true))
    }

    @Test fun terminalRecoveryRequiresSameSourceAndNoNewerActiveGeneration() {
        assertTrue(
            RemoteRecoveryPolicy.shouldRestoreAfterTerminalJob(
                PreparationStatus.SEPARATING, "source", "source", hasOtherActiveJob = false,
            ),
        )
        assertFalse(
            RemoteRecoveryPolicy.shouldRestoreAfterTerminalJob(
                PreparationStatus.SEPARATING, "new-source", "source", hasOtherActiveJob = false,
            ),
        )
        assertFalse(
            RemoteRecoveryPolicy.shouldRestoreAfterTerminalJob(
                PreparationStatus.SEPARATING, "source", "source", hasOtherActiveJob = true,
            ),
        )
        assertFalse(
            RemoteRecoveryPolicy.shouldRestoreAfterTerminalJob(
                PreparationStatus.READY, "source", "source", hasOtherActiveJob = false,
            ),
        )
    }

    @Test fun activeGenerationWinsLatestSelectionEvenIfOldTerminalFinishesLater() {
        val active = job(RemoteJobState.QUEUED, 10)
        val lateOldTerminal = job(RemoteJobState.CANCELLED, 20)
        assertEquals(
            active.identity.jobId,
            RemoteRecoveryPolicy.selectLatestForProject(listOf(lateOldTerminal, active), "project")?.identity?.jobId,
        )
    }

    @Test fun importFailureRemainsUnresolvedAndWinsOverOlderTerminalHistory() {
        val recoverable = job(RemoteJobState.IMPORT_FAILED, 30)
        val oldTerminal = job(RemoteJobState.CANCELLED, 40)
        assertTrue(RemoteRecoveryPolicy.isUnresolved(RemoteJobState.IMPORT_FAILED))
        assertEquals(
            recoverable.identity.jobId,
            RemoteRecoveryPolicy.selectLatestForProject(listOf(oldTerminal, recoverable), "project")?.identity?.jobId,
        )
    }

    @Test fun latestTerminalIsUsedWhenNoActiveGenerationExists() {
        val older = job(RemoteJobState.FAILED, 10)
        val newer = job(RemoteJobState.CANCELLED, 20)
        assertEquals(
            newer.identity.jobId,
            RemoteRecoveryPolicy.selectLatestForProject(listOf(older, newer), "project")?.identity?.jobId,
        )
    }

    @Test fun onlyLatestExpiredJobWithCompletedManifestIsEligibleForLegacyImportRepair() {
        val recoverable = job(RemoteJobState.EXPIRED, 30).copy(resultManifestSha256 = "b".repeat(64))
        val olderWithoutManifest = job(RemoteJobState.EXPIRED, 10)
        assertEquals(
            recoverable.identity.jobId,
            RemoteRecoveryPolicy.legacyImportRecoveryCandidate(
                listOf(olderWithoutManifest, recoverable),
                "project",
            )?.identity?.jobId,
        )
        assertEquals(
            null,
            RemoteRecoveryPolicy.legacyImportRecoveryCandidate(
                listOf(recoverable, job(RemoteJobState.CANCELLED, 40)),
                "project",
            ),
        )
    }

    @Test fun sameGenerationRequiresProjectSourceAndHash() {
        val current = job(RemoteJobState.FAILED, 1)
        val generation = RemoteSourceGeneration(
            current.identity.projectId,
            current.identity.sourceAssetId,
            current.identity.inputSha256,
        )
        assertTrue(RemoteRecoveryPolicy.sameGeneration(current, generation))
        assertFalse(RemoteRecoveryPolicy.sameGeneration(current, generation.copy(sourceAssetId = "different-source")))
        assertFalse(RemoteRecoveryPolicy.sameGeneration(current, generation.copy(inputSha256 = "b".repeat(64))))
    }

    @Test fun cancellationRetriesAreBoundedAndTerminalize() {
        assertEquals(
            CancellationFailureAction.RETRY,
            RemoteRecoveryPolicy.cancellationFailureAction("BACKEND_UNAVAILABLE", 0, 2),
        )
        assertEquals(
            CancellationFailureAction.RETRY,
            RemoteRecoveryPolicy.cancellationFailureAction("BACKEND_UNAVAILABLE", 1, 2),
        )
        assertEquals(
            CancellationFailureAction.TERMINAL_FAILURE,
            RemoteRecoveryPolicy.cancellationFailureAction("BACKEND_UNAVAILABLE", 2, 2),
        )
        assertEquals(
            CancellationFailureAction.TERMINAL_FAILURE,
            RemoteRecoveryPolicy.cancellationFailureAction("AUTH_REQUIRED", 0, 8),
        )
    }
}
