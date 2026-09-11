package studio.guitarlab.core.project

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import studio.guitarlab.core.model.GuitarProject
import studio.guitarlab.core.model.ProjectTemplate
import studio.guitarlab.core.model.AudioClip
import studio.guitarlab.core.model.AudioTrack

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

    @Test fun longMixedEditSequenceUndoRedoRestoresExactSnapshotsAndBranchesCorrectly() {
        val history = ProjectHistory(capacity = 16)
        val initial = project("Initial").copy(
            tracks = listOf(AudioTrack("t1", "Track", order = 0), AudioTrack("t2", "Other", order = 1)),
            clips = listOf(AudioClip("c1", "t1", "Take", "managed://media/source/a.wav", 0, lengthFrames = 100, sourceTotalFrames = 200)),
        )
        val states = mutableListOf(initial)
        fun edit(next: GuitarProject) { history.record(states.last(), next); states += next }
        edit(ProjectClipEditor.trimClip(states.last(), "c1", 10, 90, 2))
        edit(ProjectClipEditor.moveClip(states.last(), "c1", 50, 3))
        edit(ProjectClipEditor.splitClipAtTimelineFrame(states.last(), "c1", 100, "c2", 4))
        edit(ProjectClipEditor.setClipFades(states.last(), "c1", 10, 20, 5))
        edit(states.last().copy(name = "Renamed", updatedAtEpochMs = 6))
        var cursor = states.last()
        for (index in states.lastIndex - 1 downTo 0) {
            cursor = history.undo(cursor)!!
            assertEquals(states[index], cursor)
        }
        for (index in 1..states.lastIndex) {
            cursor = history.redo(cursor)!!
            assertEquals(states[index], cursor)
        }
        cursor = history.undo(cursor)!!
        val branched = cursor.copy(name = "Branch", updatedAtEpochMs = 7)
        history.record(cursor, branched)
        assertFalse(history.canRedo)
    }
}
