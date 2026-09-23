package studio.guitarlab.app.backup

import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals
import org.junit.Test

class DeletedProjectRetentionPolicyTest {
    @Test fun onlyExplicitTombstonesAtLeastTenDaysOldExpire() {
        val now = TimeUnit.DAYS.toMillis(20)
        val recent = DeletedProjectBackup("recent", "Recent", now - TimeUnit.DAYS.toMillis(9))
        val boundary = DeletedProjectBackup("boundary", "Boundary", now - TimeUnit.DAYS.toMillis(10))
        val old = DeletedProjectBackup("old", "Old", now - TimeUnit.DAYS.toMillis(11))

        assertEquals(listOf("boundary", "old"), DeletedProjectRetentionPolicy.expired(listOf(recent, boundary, old), now).map { it.projectId })
    }

    @Test fun anEmptyTombstoneCatalogNeverInfersDeletionFromCloudOrLocalAbsence() {
        assertEquals(emptyList(), DeletedProjectRetentionPolicy.expired(emptyList(), Long.MAX_VALUE))
    }
}
