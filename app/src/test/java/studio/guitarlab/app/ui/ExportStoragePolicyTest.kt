package studio.guitarlab.app.ui

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ExportStoragePolicyTest {
    @Test fun singleEncodedStudyRequiresOneTempBudgetPlusReserve() {
        assertEquals(18L * 1024L * 1024L, ExportStoragePolicy.requiredTemporaryBytes(10L * 1024L * 1024L, false))
    }

    @Test fun encodedMasterRequiresRenderAndEncodedSiblingBudget() {
        assertEquals(28L * 1024L * 1024L, ExportStoragePolicy.requiredTemporaryBytes(10L * 1024L * 1024L, true))
    }

    @Test fun lowStorageFailsClosedBeforeExportStarts() {
        assertFailsWith<IllegalArgumentException> { ExportStoragePolicy.requireAvailable(100, 101) }
    }
}
