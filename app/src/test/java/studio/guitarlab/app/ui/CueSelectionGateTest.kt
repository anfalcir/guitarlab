package studio.guitarlab.app.ui

import org.junit.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CueSelectionGateTest {
    @Test fun onlyLatestSelectionMayCommit() {
        val gate = CueSelectionGate()
        val first = gate.begin()
        val second = gate.begin()
        assertFalse(gate.accepts(first))
        assertTrue(gate.accepts(second))
    }
    @Test fun disablingOrChangingMainInvalidatesPendingResult() {
        val gate = CueSelectionGate()
        val pending = gate.begin()
        gate.invalidate()
        assertFalse(gate.accepts(pending))
    }
}
