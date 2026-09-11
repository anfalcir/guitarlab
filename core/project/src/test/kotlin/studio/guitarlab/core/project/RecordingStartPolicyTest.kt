package studio.guitarlab.core.project

import kotlin.test.Test
import kotlin.test.assertEquals

class RecordingStartPolicyTest {
    @Test
    fun recordAlwaysUsesFiveSecondCountdown() {
        assertEquals(5, RecordingStartPolicy.START_DELAY_SECONDS)
        assertEquals(listOf(5, 4, 3, 2, 1), RecordingStartPolicy.countdownSequence())
    }
}
