package studio.guitarlab.core.project

import kotlin.test.Test
import kotlin.test.assertEquals

class RecordingStartPolicyTest {
    @Test
    fun recordAlwaysUsesThreeSecondCountdown() {
        assertEquals(3, RecordingStartPolicy.START_DELAY_SECONDS)
        assertEquals(listOf(3, 2, 1), RecordingStartPolicy.countdownSequence())
    }
}
