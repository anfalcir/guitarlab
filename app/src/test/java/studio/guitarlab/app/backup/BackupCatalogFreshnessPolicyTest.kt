package studio.guitarlab.app.backup

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BackupCatalogFreshnessPolicyTest {
    @Test fun missingOrForcedCatalogAlwaysRefreshes() {
        assertTrue(BackupCatalogFreshnessPolicy.shouldRefresh(null, nowEpochMs = 10_000L, force = false))
        assertTrue(
            BackupCatalogFreshnessPolicy.shouldRefresh(
                snapshot = BackupCatalogSnapshot(emptyList(), emptyMap(), emptyMap(), emptyMap(), 9_900L),
                nowEpochMs = 10_000L,
                force = true,
            ),
        )
    }

    @Test fun recentDurableCatalogAvoidsImmediateRemoteReload() {
        val snapshot = BackupCatalogSnapshot(emptyList(), emptyMap(), emptyMap(), emptyMap(), refreshedAtEpochMs = 100_000L)
        assertFalse(
            BackupCatalogFreshnessPolicy.shouldRefresh(
                snapshot = snapshot,
                nowEpochMs = 100_000L + BackupCatalogFreshnessPolicy.DEFAULT_MAX_AGE_MS - 1L,
                force = false,
            ),
        )
    }

    @Test fun localRevisionDriftIsDetectableWithoutDiscardingCachedVersionHistory() {
        val snapshot = BackupCatalogSnapshot(
            versions = emptyList(),
            reconciliations = emptyMap(),
            remoteTips = emptyMap(),
            localRevisions = mapOf("project-a" to "revision-a"),
            refreshedAtEpochMs = 100_000L,
        )

        assertTrue(snapshot.localRevisions != mapOf("project-a" to "revision-b"))
        assertTrue(snapshot.localRevisions != mapOf("project-a" to "revision-a", "project-b" to "revision-b"))
    }

    @Test fun staleClockRollbackOrExpiredCatalogRefreshes() {
        val snapshot = BackupCatalogSnapshot(emptyList(), emptyMap(), emptyMap(), emptyMap(), refreshedAtEpochMs = 100_000L)
        assertTrue(
            BackupCatalogFreshnessPolicy.shouldRefresh(
                snapshot = snapshot,
                nowEpochMs = 100_000L + BackupCatalogFreshnessPolicy.DEFAULT_MAX_AGE_MS,
                force = false,
            ),
        )
        assertTrue(BackupCatalogFreshnessPolicy.shouldRefresh(snapshot, nowEpochMs = 99_999L, force = false))
    }
}
