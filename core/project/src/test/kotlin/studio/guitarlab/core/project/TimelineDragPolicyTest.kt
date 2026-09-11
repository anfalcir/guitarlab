package studio.guitarlab.core.project

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TimelineDragPolicyTest {
    private val ids = listOf("a", "b", "c", "d")
    private val lanes = listOf(
        TimelineDragPolicy.LaneBounds("a", 0f, 90f, 0),
        TimelineDragPolicy.LaneBounds("b", 100f, 190f, 1),
        TimelineDragPolicy.LaneBounds("c", 200f, 290f, 2),
        TimelineDragPolicy.LaneBounds("d", 300f, 390f, 3),
    )

    @Test fun `track target follows real lane bounds upward and downward`() {
        assertEquals(0, TimelineDragPolicy.trackTargetIndex("c", 15f, lanes, ids))
        assertEquals(3, TimelineDragPolicy.trackTargetIndex("a", 370f, lanes, ids))
    }

    @Test fun `track target reaches first and last position`() {
        assertEquals(0, TimelineDragPolicy.trackTargetIndex("b", -50f, lanes, ids))
        assertEquals(3, TimelineDragPolicy.trackTargetIndex("b", 900f, lanes, ids))
    }

    @Test fun `target after scroll uses current measured bounds not original drag delta`() {
        val scrolled = lanes.map { it.copy(topPx = it.topPx - 150f, bottomPx = it.bottomPx - 150f) }
        assertEquals(2, TimelineDragPolicy.trackTargetIndex("a", 80f, scrolled, ids))
    }

    @Test fun `clip target uses lane under pointer`() {
        assertEquals("c", TimelineDragPolicy.clipTargetTrackId(250f, lanes))
        assertEquals("a", TimelineDragPolicy.clipTargetTrackId(-20f, lanes))
        assertEquals("d", TimelineDragPolicy.clipTargetTrackId(450f, lanes))
    }

    @Test fun `autoscroll is proportional clamped and directional`() {
        val nearTop = TimelineDragPolicy.autoScroll(5f, 0f, 400f, 80f, 24f, true, true)
        val deeperTop = TimelineDragPolicy.autoScroll(40f, 0f, 400f, 80f, 24f, true, true)
        val nearBottom = TimelineDragPolicy.autoScroll(395f, 0f, 400f, 80f, 24f, true, true)
        assertEquals(TimelineDragPolicy.EdgeDirection.UP, nearTop.direction)
        assertEquals(TimelineDragPolicy.EdgeDirection.DOWN, nearBottom.direction)
        assertTrue(nearTop.deltaPx < deeperTop.deltaPx)
        assertTrue(nearTop.deltaPx >= -24f)
        assertTrue(nearBottom.deltaPx <= 24f)
    }

    @Test fun `autoscroll stops outside edge zone or at content limits`() {
        assertEquals(0f, TimelineDragPolicy.autoScroll(200f, 0f, 400f, 80f, 24f, true, true).deltaPx)
        assertEquals(0f, TimelineDragPolicy.autoScroll(5f, 0f, 400f, 80f, 24f, false, true).deltaPx)
        assertEquals(0f, TimelineDragPolicy.autoScroll(395f, 0f, 400f, 80f, 24f, true, false).deltaPx)
    }

    @Test fun `insertion indicator resolves visible and virtual positions`() {
        assertEquals(200f, TimelineDragPolicy.insertionIndicatorYPx(2, lanes, ids))
        val partial = lanes.filter { it.trackId == "b" || it.trackId == "c" }
        assertEquals(100f, TimelineDragPolicy.insertionIndicatorYPx(0, partial, ids))
        assertEquals(290f, TimelineDragPolicy.insertionIndicatorYPx(3, partial, ids))
    }
}
