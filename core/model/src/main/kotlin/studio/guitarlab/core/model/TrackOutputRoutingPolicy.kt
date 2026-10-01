package studio.guitarlab.core.model

/**
 * Pure routing policy shared by UI state and the Android playback engine.
 *
 * MAIN is the normal monitor/master bus. CUE is an explicit secondary bus and is never an
 * implicit fallback target. MAIN_AND_CUE is retained as an engine capability even though the
 * first mixer affordance toggles between exclusive MAIN and exclusive CUE.
 */
object TrackOutputRoutingPolicy {
    fun sendsToMain(route: TrackOutputRoute): Boolean =
        route == TrackOutputRoute.MAIN || route == TrackOutputRoute.MAIN_AND_CUE

    fun sendsToCue(route: TrackOutputRoute): Boolean =
        route == TrackOutputRoute.CUE || route == TrackOutputRoute.MAIN_AND_CUE

    fun toggleExclusiveCue(route: TrackOutputRoute): TrackOutputRoute =
        if (route == TrackOutputRoute.MAIN) TrackOutputRoute.CUE else TrackOutputRoute.MAIN
}
