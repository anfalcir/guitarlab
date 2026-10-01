package studio.guitarlab.platform.audio.android

import org.junit.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RecordingInputRoutePolicyTest {
    @Test fun explicitInputIsFailClosed() {
        assertTrue(RecordingInputRoutePolicy.accepts(true,31,31))
        assertFalse(RecordingInputRoutePolicy.accepts(true,31,null))
        assertFalse(RecordingInputRoutePolicy.accepts(true,31,7))
    }
    @Test fun automaticInputMayUseResolvedRoute() {
        assertTrue(RecordingInputRoutePolicy.accepts(false,null,null))
        assertTrue(RecordingInputRoutePolicy.accepts(false,null,7))
    }
    @Test
    fun requiredMonitorOutputIsFailClosed() {
        assertTrue(RecordingMonitorRoutePolicy.accepts(true, 41, 41))
        assertFalse(RecordingMonitorRoutePolicy.accepts(true, 41, null))
        assertFalse(RecordingMonitorRoutePolicy.accepts(true, null, 41))
        assertFalse(RecordingMonitorRoutePolicy.accepts(true, 41, 42))
        assertTrue(RecordingMonitorRoutePolicy.accepts(false, null, null))
    }

}
