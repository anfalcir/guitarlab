package studio.guitarlab.core.project

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import studio.guitarlab.core.model.GuitarProject
import studio.guitarlab.core.model.ProjectTemplate

class ProjectHistoryTest {
    private fun project(name: String, updated: Long = 1): GuitarProject = GuitarProject(
        id = "p1",
        name = name,
        template = ProjectTemplate.BLANK,
        createdAtEpochMs = 1,
        updatedAtEpochMs = updated,
    )

    @Test
    fun undoRedoRoundTripUsesMetadataSnapshots() {
        val history = ProjectHistory(capacity = 4)
        val a = project("A", 1)
        val b = project("B", 2)
        val c = project("C", 3)
        history.record(a, b)
        history.record(b, c)
        assertTrue(history.canUndo)
        assertEquals("B", history.undo(c)?.name)
        assertEquals("A", history.undo(b)?.name)
        assertFalse(history.canUndo)
        assertTrue(history.canRedo)
        assertEquals("B", history.redo(a)?.name)
        assertEquals("C", history.redo(b)?.name)
    }

    @Test
    fun newEditClearsRedoAndCapacityDropsOldestSnapshot() {
        val history = ProjectHistory(capacity = 2)
        val a = project("A", 1)
        val b = project("B", 2)
        val c = project("C", 3)
        val d = project("D", 4)
        history.record(a, b)
        history.record(b, c)
        history.record(c, d)
        assertEquals(2, history.undoDepth)
        assertEquals("C", history.undo(d)?.name)
        history.record(c, project("E", 5))
        assertFalse(history.canRedo)
    }

    @Test
    fun equalSnapshotsDoNotCreateHistoryEntry() {
        val history = ProjectHistory()
        val a = project("A")
        history.record(a, a)
        assertFalse(history.canUndo)
        assertNull(history.undo(a))
    }
}
