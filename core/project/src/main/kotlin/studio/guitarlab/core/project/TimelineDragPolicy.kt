package studio.guitarlab.core.project

import kotlin.math.abs

/** Pure geometry/policy used by the Compose drag coordinator. */
object TimelineDragPolicy {
    data class LaneBounds(
        val trackId: String,
        val topPx: Float,
        val bottomPx: Float,
        val order: Int,
    ) {
        init {
            require(trackId.isNotBlank()) { "trackId must not be blank" }
            require(bottomPx >= topPx) { "lane bottom must be >= top" }
        }

        val centerPx: Float get() = (topPx + bottomPx) / 2f
        val heightPx: Float get() = bottomPx - topPx
    }

    enum class EdgeDirection { NONE, UP, DOWN }

    data class AutoScroll(
        val direction: EdgeDirection,
        /** Signed pixels per frame/tick. Negative = up, positive = down. */
        val deltaPx: Float,
    )

    /**
     * Reorder semantics are expressed as a final list index, matching ProjectTrackEditor.reorderTrack.
     * The pointer is compared with real lane centers, so variable heights and scrolled layouts work.
     */
    fun trackTargetIndex(
        sourceTrackId: String,
        pointerYPx: Float,
        visibleLanes: List<LaneBounds>,
        orderedTrackIds: List<String>,
    ): Int {
        require(sourceTrackId in orderedTrackIds) { "source track not found" }
        if (orderedTrackIds.size <= 1 || visibleLanes.isEmpty()) return orderedTrackIds.indexOf(sourceTrackId)

        val lanes = visibleLanes.sortedBy { it.topPx }
        val first = lanes.first()
        val last = lanes.last()

        val visibleTargetOrder = when {
            pointerYPx <= first.centerPx -> first.order
            pointerYPx >= last.centerPx -> last.order
            else -> lanes.firstOrNull { pointerYPx < it.centerPx }?.order ?: last.order
        }.coerceIn(0, orderedTrackIds.lastIndex)

        return visibleTargetOrder
    }

    /** Target lane for a clip migration. Uses lane bodies rather than insertion gaps. */
    fun clipTargetTrackId(pointerYPx: Float, visibleLanes: List<LaneBounds>): String? {
        if (visibleLanes.isEmpty()) return null
        val lanes = visibleLanes.sortedBy { it.topPx }
        return lanes.firstOrNull { pointerYPx in it.topPx..it.bottomPx }?.trackId
            ?: lanes.minByOrNull { abs(pointerYPx - it.centerPx) }?.trackId
    }

    /** Y position for the reorder insertion indicator, expressed in workspace coordinates. */
    fun insertionIndicatorYPx(
        targetIndex: Int,
        visibleLanes: List<LaneBounds>,
        orderedTrackIds: List<String>,
    ): Float? {
        if (orderedTrackIds.isEmpty() || visibleLanes.isEmpty()) return null
        val safeIndex = targetIndex.coerceIn(0, orderedTrackIds.lastIndex)
        val targetId = orderedTrackIds[safeIndex]
        val target = visibleLanes.firstOrNull { it.trackId == targetId }
        if (target != null) return target.topPx

        val lanes = visibleLanes.sortedBy { it.order }
        val before = lanes.lastOrNull { it.order < safeIndex }
        val after = lanes.firstOrNull { it.order > safeIndex }
        return when {
            before != null && after != null -> (before.bottomPx + after.topPx) / 2f
            before != null -> before.bottomPx
            after != null -> after.topPx
            else -> null
        }
    }

    /**
     * Continuous autoscroll policy. speed grows linearly toward the edge and is clamped.
     * Caller runs this policy in a frame/coroutine loop while drag is active.
     */
    fun autoScroll(
        pointerYPx: Float,
        viewportTopPx: Float,
        viewportBottomPx: Float,
        edgeZonePx: Float,
        maxStepPx: Float,
        canScrollBackward: Boolean,
        canScrollForward: Boolean,
    ): AutoScroll {
        require(viewportBottomPx >= viewportTopPx) { "invalid viewport" }
        require(edgeZonePx > 0f) { "edgeZonePx must be positive" }
        require(maxStepPx >= 0f) { "maxStepPx must be non-negative" }

        val topDistance = pointerYPx - viewportTopPx
        if (topDistance < edgeZonePx && canScrollBackward) {
            val intensity = ((edgeZonePx - topDistance) / edgeZonePx).coerceIn(0f, 1f)
            return AutoScroll(EdgeDirection.UP, -maxStepPx * intensity)
        }

        val bottomDistance = viewportBottomPx - pointerYPx
        if (bottomDistance < edgeZonePx && canScrollForward) {
            val intensity = ((edgeZonePx - bottomDistance) / edgeZonePx).coerceIn(0f, 1f)
            return AutoScroll(EdgeDirection.DOWN, maxStepPx * intensity)
        }

        return AutoScroll(EdgeDirection.NONE, 0f)
    }
}
