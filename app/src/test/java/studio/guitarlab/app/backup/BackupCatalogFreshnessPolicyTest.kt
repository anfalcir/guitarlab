package studio.guitarlab.app.backup

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BackupCatalogFreshnessPolicyTest {
    @Test fun missingOrForcedCatalogAlwaysRefreshes() {
        assertTrue(BackupCatalogFreshnessPolicy.shouldRefresh(null, nowEpochMs = 10_000L, force = false))
        assertTrue(
            BackupCatalogFreshnessPolicy.shouldRefresh(
                snapshot = BackupCatalogSnapshot(emptyList(), emptyMap(), emptyMap(), 9_900L),
                nowEpochMs = 10_000L,
                force = true,
            ),
        )
    }

    @Test fun recentDurableCatalogAvoidsImmediateRemoteReload() {
        val snapshot = BackupCatalogSnapshot(emptyList(), emptyMap(), emptyMap(), refreshedAtEpochMs = 100_000L)
        assertFalse(
            BackupCatalogFreshnessPolicy.shouldRefresh(
                snapshot = snapshot,
                nowEpochMs = 100_000L + BackupCatalogFreshnessPolicy.DEFAULT_MAX_AGE_MS - 1L,
                force = false,
            ),
        )
    }

    @Test fun staleClockRollbackOrExpiredCatalogRefreshes() {
        val snapshot = BackupCatalogSnapshot(emptyList(), emptyMap(), emptyMap(), refreshedAtEpochMs = 100_000L)
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
