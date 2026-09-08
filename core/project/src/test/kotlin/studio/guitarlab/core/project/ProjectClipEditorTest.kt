package studio.guitarlab.core.project

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import studio.guitarlab.core.model.AudioClip
import studio.guitarlab.core.model.AudioTrack
import studio.guitarlab.core.model.GuitarProject
import studio.guitarlab.core.model.ProjectTemplate

class ProjectClipEditorTest {
    private fun project(): GuitarProject = GuitarProject(
        id = "p1",
        name = "Project",
        template = ProjectTemplate.BLANK,
        createdAtEpochMs = 1,
        updatedAtEpochMs = 1,
        tracks = listOf(AudioTrack(id = "t1", name = "Guitar", order = 0)),
        clips = listOf(
            AudioClip(
                id = "c1",
                trackId = "t1",
                name = "Take",
                sourceUri = "content://audio/take.wav",
                startFrame = 0,
                lengthFrames = 48_000,
            )
        ),
    )

    @Test
    fun removeClipUpdatesProjectAtomically() {
        val edited = ProjectClipEditor.removeClip(project(), "c1", 100)
        assertEquals(emptyList(), edited.clips)
        assertEquals(100, edited.updatedAtEpochMs)
    }

    @Test
    fun muteAndMovePreserveClipIdentity() {
        val muted = ProjectClipEditor.setClipMuted(project(), "c1", true, 101)
        assertEquals(true, muted.clips.single().muted)
        val moved = ProjectClipEditor.moveClip(muted, "c1", 24_000, 102)
        assertEquals("c1", moved.clips.single().id)
        assertEquals(24_000, moved.clips.single().startFrame)
        assertEquals(102, moved.updatedAtEpochMs)
    }

    @Test
    fun rejectsMissingClipAndNegativeMove() {
        assertFailsWith<IllegalArgumentException> { ProjectClipEditor.removeClip(project(), "missing", 2) }
        assertFailsWith<IllegalArgumentException> { ProjectClipEditor.moveClip(project(), "c1", -1, 2) }
    }
}
