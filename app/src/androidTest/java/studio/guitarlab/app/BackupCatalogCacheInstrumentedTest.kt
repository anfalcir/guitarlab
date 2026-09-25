package studio.guitarlab.app

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlin.test.assertEquals
import kotlin.test.assertNull
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import studio.guitarlab.app.backup.BackupCatalogCacheStore
import studio.guitarlab.app.backup.BackupCatalogSnapshot
import studio.guitarlab.app.backup.BackupSettingsSnapshot
import studio.guitarlab.core.project.BackupVersionDescriptor
import studio.guitarlab.core.project.DriveReconciliation

@RunWith(AndroidJUnit4::class)
class BackupCatalogCacheInstrumentedTest {
    private val context get() = ApplicationProvider.getApplicationContext<android.content.Context>()
    private val store get() = BackupCatalogCacheStore(context)

    @Before fun clearBefore() = store.clear()
    @After fun clearAfter() = store.clear()

    @Test fun durableCatalogRoundTripsAndRejectsDifferentAccountOrRetention() {
        val settings = BackupSettingsSnapshot(
            driveConnected = true,
            driveAccountLabel = "Conta A",
            retentionDays = 90,
            maximumVersions = 3,
        )
        val version = BackupVersionDescriptor(
            remoteId = "u8:aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
            projectId = "project-a",
            projectName = "Projeto A",
            projectUpdatedAtEpochMs = 10L,
            backupCreatedAtEpochMs = 20L,
            sizeBytes = 30L,
            sha256 = "a".repeat(64),
            revisionId = "revision-a",
            formatVersion = 3,
        )
        val snapshot = BackupCatalogSnapshot(
            versions = listOf(version),
            reconciliations = mapOf("project-a" to DriveReconciliation.NO_OP),
            remoteTips = emptyMap(),
            localRevisions = mapOf("project-a" to "revision-a"),
            refreshedAtEpochMs = 40L,
        )

        store.save(settings, snapshot)

        assertEquals(snapshot, store.load(settings))
        assertNull(store.load(settings.copy(driveAccountLabel = "Conta B")))
        assertNull(store.load(settings.copy(retentionDays = 30)))
        assertNull(store.load(settings.copy(maximumVersions = 5)))
    }

    @Test fun disconnectedDriveNeverExposesPersistedCatalog() {
        val connected = BackupSettingsSnapshot(driveConnected = true, driveAccountLabel = "Conta A")
        store.save(
            connected,
            BackupCatalogSnapshot(
                versions = emptyList(),
                reconciliations = emptyMap(),
                remoteTips = emptyMap(),
                localRevisions = emptyMap(),
                refreshedAtEpochMs = 1L,
            ),
        )

        assertNull(store.load(connected.copy(driveConnected = false)))
    }
}
