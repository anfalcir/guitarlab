package studio.guitarlab.core.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TrackOutputRoutingPolicyTest {
    @Test
    fun routingMatrixKeepsMainCueAndBothSemanticsDistinct() {
        assertTrue(TrackOutputRoutingPolicy.sendsToMain(TrackOutputRoute.MAIN))
        assertFalse(TrackOutputRoutingPolicy.sendsToCue(TrackOutputRoute.MAIN))

        assertFalse(TrackOutputRoutingPolicy.sendsToMain(TrackOutputRoute.CUE))
        assertTrue(TrackOutputRoutingPolicy.sendsToCue(TrackOutputRoute.CUE))

        assertTrue(TrackOutputRoutingPolicy.sendsToMain(TrackOutputRoute.MAIN_AND_CUE))
        assertTrue(TrackOutputRoutingPolicy.sendsToCue(TrackOutputRoute.MAIN_AND_CUE))
    }

    @Test
    fun headphoneToggleIsExclusiveAndDeterministic() {
        assertEquals(TrackOutputRoute.CUE, TrackOutputRoutingPolicy.toggleExclusiveCue(TrackOutputRoute.MAIN))
        assertEquals(TrackOutputRoute.MAIN, TrackOutputRoutingPolicy.toggleExclusiveCue(TrackOutputRoute.CUE))
        assertEquals(TrackOutputRoute.MAIN, TrackOutputRoutingPolicy.toggleExclusiveCue(TrackOutputRoute.MAIN_AND_CUE))
    }
    @Test
    fun cueSinkOpensOnlyWhenPlaybackContentBelongsToCueRoute() {
        val routes = mapOf(
            "backing" to TrackOutputRoute.MAIN,
            "reference" to TrackOutputRoute.CUE,
            "empty-cue" to TrackOutputRoute.CUE,
        )

        assertFalse(TrackOutputRoutingPolicy.playbackNeedsCue(routes, listOf("backing")))
        assertTrue(TrackOutputRoutingPolicy.playbackNeedsCue(routes, listOf("backing", "reference")))
        assertFalse(TrackOutputRoutingPolicy.playbackNeedsCue(routes, emptyList()))
        assertFalse(TrackOutputRoutingPolicy.playbackNeedsCue(routes, listOf("missing")))
    }

}
