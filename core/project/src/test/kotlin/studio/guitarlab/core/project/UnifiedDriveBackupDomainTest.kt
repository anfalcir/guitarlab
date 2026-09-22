package studio.guitarlab.core.project

import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import org.junit.Test

class UnifiedDriveBackupDomainTest {
    private val source = asset('a', 100)
    private val take = asset('b', 50)

    @Test fun canonicalManifestIsIndependentFromAssetEnumerationOrder() {
        val first = manifest(listOf(source, take))
        val second = manifest(listOf(take, source))
        assertEquals(first.canonicalBytes().decodeToString(), second.canonicalBytes().decodeToString())
        assertEquals(first.manifestSha256, second.manifestSha256)
    }

    @Test fun malformedAndDuplicateAssetIdentityFailsClosed() {
        assertFailsWith<IllegalArgumentException> { DriveAssetObject("A".repeat(64), 1) }
        assertFailsWith<IllegalArgumentException> { DriveAssetObject("a".repeat(64), 0) }
        assertFailsWith<IllegalArgumentException> { manifest(listOf(source, source)) }
    }

    @Test fun completeSnapshotMapsStateAndPathsDeterministically() {
        val state = asset('e', 25)
        val first = DriveProjectRevisionManifest(
            projectId = "project-1",
            revisionId = "r1",
            baseRevisionId = null,
            createdAtEpochMs = 10,
            canonicalProjectStateSha256 = "f".repeat(64),
            assets = listOf(state, source, take),
            projectStateAsset = state,
            fileEntries = listOf(
                DriveProjectFileEntry("media/take.wav", take),
                DriveProjectFileEntry("media/source.wav", source),
            ),
        )
        val second = first.copy(
            assets = listOf(take, state, source),
            fileEntries = first.fileEntries.reversed(),
        )
        assertTrue(first.isCompleteProjectSnapshot)
        assertEquals(first.canonicalBytes().decodeToString(), second.canonicalBytes().decodeToString())
        assertEquals(first.manifestSha256, second.manifestSha256)
        val parsed = DriveProjectRevisionManifest.parseCanonical(first.canonicalBytes())
        assertEquals(first.canonicalBytes().decodeToString(), parsed.canonicalBytes().decodeToString())
        assertEquals(listOf("media/source.wav", "media/take.wav"), parsed.fileEntries.map { it.relativePath })
        val stateOnly = first.copy(assets = listOf(state), fileEntries = emptyList())
        assertEquals(stateOnly, DriveProjectRevisionManifest.parseCanonical(stateOnly.canonicalBytes()))
        assertFailsWith<IllegalArgumentException> {
            first.copy(fileEntries = listOf(DriveProjectFileEntry("../escape", source)))
        }
        assertFailsWith<IllegalArgumentException> {
            first.copy(assets = listOf(state, source))
        }
    }

    @Test fun metadataOnlyRevisionUploadsNoMedia() {
        val changedMetadata = manifest(listOf(source, take), revision = "r2", state = 'd')
        assertTrue(DriveUploadPlanner.missingAssets(changedMetadata, setOf(source.sha256, take.sha256)).isEmpty())
    }

    @Test fun oneNewTakeUploadsOnlyThatAsset() {
        val nextTake = asset('c', 75)
        val missing = DriveUploadPlanner.missingAssets(manifest(listOf(source, take, nextTake)), setOf(source.sha256, take.sha256))
        assertEquals(listOf(nextTake), missing)
    }

    @Test fun reconciliationNeverUsesNewestTimestampAsConflictResolution() {
        assertEquals(DriveReconciliation.NO_OP, DriveConflictResolver.resolve("r1", "r1", "r1"))
        assertEquals(DriveReconciliation.UPLOAD_LOCAL, DriveConflictResolver.resolve("r2", "r1", "r1"))
        assertEquals(DriveReconciliation.DOWNLOAD_REMOTE, DriveConflictResolver.resolve("r1", "r1", "r2"))
        assertEquals(DriveReconciliation.CONFLICT, DriveConflictResolver.resolve("local", "base", "remote"))
        assertEquals(DriveReconciliation.LOCAL_ONLY, DriveConflictResolver.resolve("r1", null, null))
        assertEquals(DriveReconciliation.REMOTE_ONLY, DriveConflictResolver.resolve(null, null, "r1"))
    }

    @Test fun syncIsTrueOnlyAfterPublishedHeadIsReadBackAndMatches() {
        DriveCommitStage.entries.dropLast(1).forEach { stage ->
            assertFalse(DriveCommitProgress("r2", if (stage == DriveCommitStage.HEAD_PUBLISHED) "r2" else null, stage).isServerConfirmed)
        }
        assertFalse(DriveCommitProgress("r2", "r1", DriveCommitStage.HEAD_VERIFIED).isServerConfirmed)
        assertTrue(DriveCommitProgress("r2", "r2", DriveCommitStage.HEAD_VERIFIED).isServerConfirmed)
    }

    @Test fun gcProtectsEveryRetainedOrPendingOrRecentAsset() {
        val reachable = source
        val pending = take
        val oldOrphan = asset('c', 20)
        val recentOrphan = asset('d', 20)
        val candidates = listOf(
            DriveGcCandidate(reachable, 1), DriveGcCandidate(pending, 1),
            DriveGcCandidate(oldOrphan, 1), DriveGcCandidate(recentOrphan, 950),
        )
        val deleted = DriveGarbageCollectionPlanner.deletions(
            candidates, listOf(manifest(listOf(reachable))), setOf(pending.sha256), nowEpochMs = 1_000, gracePeriodMs = 100,
        )
        assertEquals(listOf(oldOrphan.sha256), deleted.map { it.asset.sha256 })
    }

    private fun asset(char: Char, size: Long) = DriveAssetObject(char.toString().repeat(64), size)

    private fun manifest(
        assets: List<DriveAssetObject>,
        revision: String = "r1",
        state: Char = 'c',
    ) = DriveProjectRevisionManifest(
        projectId = "project-1", revisionId = revision, baseRevisionId = null,
        createdAtEpochMs = 10, canonicalProjectStateSha256 = state.toString().repeat(64), assets = assets,
    )
}
