package studio.guitarlab.core.audio

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CueRouteSafetyPolicyTest {
    @Test
    fun cueAdmissionRequiresTwoExplicitDistinctResolvedEndpoints() {
        assertEquals(
            CueRouteBlockReason.NOT_REQUESTED,
            CueRouteSafetyPolicy.admit(false, true, true, 1, 2).blockReason,
        )
        assertEquals(
            CueRouteBlockReason.MAIN_NOT_EXPLICIT,
            CueRouteSafetyPolicy.admit(true, false, false, null, 2).blockReason,
        )
        assertEquals(
            CueRouteBlockReason.MAIN_NOT_ACCEPTED,
            CueRouteSafetyPolicy.admit(true, true, false, 1, 2).blockReason,
        )
        assertEquals(
            CueRouteBlockReason.CUE_UNAVAILABLE,
            CueRouteSafetyPolicy.admit(true, true, true, 1, null).blockReason,
        )
        assertEquals(
            CueRouteBlockReason.SAME_ENDPOINT,
            CueRouteSafetyPolicy.admit(true, true, true, 7, 7).blockReason,
        )
        assertTrue(CueRouteSafetyPolicy.admit(true, true, true, 7, 9).allowed)
    }

    @Test
    fun effectiveRoutesMustRemainExactlyDistinct() {
        assertTrue(CueRouteSafetyPolicy.routedPairMatches(10, 20, 10, 20))
        assertFalse(CueRouteSafetyPolicy.routedPairMatches(10, 20, 20, 20))
        assertFalse(CueRouteSafetyPolicy.routedPairMatches(10, 20, 10, null))
        assertFalse(CueRouteSafetyPolicy.routedPairMatches(10, 20, 10, 21))
    }

    @Test
    fun driftGuardStaysWithinMusicallyTightWindowAtCommonRates() {
        assertEquals(750L, CueRouteSafetyPolicy.driftLimitFrames(48_000))
        assertEquals(689L, CueRouteSafetyPolicy.driftLimitFrames(44_100))
        assertFalse(CueRouteSafetyPolicy.driftExceeded(100_000, 100_750, 48_000))
        assertTrue(CueRouteSafetyPolicy.driftExceeded(100_000, 100_751, 48_000))
        assertEquals(2_880L, CueRouteSafetyPolicy.communicationDriftLimitFrames(48_000))
        assertFalse(CueRouteSafetyPolicy.driftExceeded(
            100_000, 102_880, 48_000, CueRouteSafetyPolicy.communicationDriftLimitFrames(48_000),
        ))
    }

    @Test
    fun initialPresentationOffsetIsBoundedAndOverflowSafe() {
        val limit = CueRouteSafetyPolicy.DEFAULT_INITIAL_OFFSET_LIMIT_NS
        assertTrue(CueRouteSafetyPolicy.initialOffsetWithinLimit(1_000_000_000L, 1_000_000_000L + limit))
        assertTrue(CueRouteSafetyPolicy.initialOffsetWithinLimit(1_000_000_000L + limit, 1_000_000_000L))
        assertFalse(CueRouteSafetyPolicy.initialOffsetWithinLimit(1_000_000_000L, 1_000_000_000L + limit + 1L))
        assertFalse(CueRouteSafetyPolicy.initialOffsetWithinLimit(Long.MIN_VALUE, Long.MAX_VALUE))
        assertTrue(CueRouteSafetyPolicy.initialOffsetWithinLimit(
            1_000_000_000L,
            1_000_000_000L + 30_000_000L,
            CueRouteSafetyPolicy.COMMUNICATION_INITIAL_OFFSET_LIMIT_NS,
        ))
    }

}
