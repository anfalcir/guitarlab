package studio.guitarlab.app.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class ExportEntryPointPolicyTest {
    @Test
    fun everyExportEntryPointResolvesToCanonicalWorkspace() {
        assertEquals(AppScreen.Export("project-1"), ExportEntryPointPolicy.destination("project-1"))
    }
}
