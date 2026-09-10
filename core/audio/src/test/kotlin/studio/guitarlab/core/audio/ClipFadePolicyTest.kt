package studio.guitarlab.core.audio

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ClipFadePolicyTest {
    @Test fun fadeInAndOutAreBoundedAndDeterministic() {
        assertEquals(0f, ClipFadePolicy.gain(0, 100, 10, 10))
        assertTrue(ClipFadePolicy.gain(5, 100, 10, 10) in 0.49f..0.51f)
        assertEquals(1f, ClipFadePolicy.gain(50, 100, 10, 10))
        assertTrue(ClipFadePolicy.gain(95, 100, 10, 10) < 0.5f)
    }
}
