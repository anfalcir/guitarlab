package studio.guitarlab.core.project

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import studio.guitarlab.core.model.AudioClip
import studio.guitarlab.core.model.AudioTrack
import studio.guitarlab.core.model.GuitarProject
import studio.guitarlab.core.model.ProjectTemplate

class ProjectTrackEditorTest {
    private fun project(): GuitarProject = GuitarProject(
        id = "p1",
        name = "Project",
        template = ProjectTemplate.BLANK,
        createdAtEpochMs = 1,
        updatedAtEpochMs = 1,
        tracks = listOf(
            AudioTrack(id = "a", name = "A", order = 0),
            AudioTrack(id = "b", name = "B", order = 1),
            AudioTrack(id = "c", name = "C", order = 2),
        ),
        clips = listOf(
            AudioClip(id = "clip-b", trackId = "b", name = "Take", sourceUri = "managed://media/source/take.wav", startFrame = 0, lengthFrames = 100),
        ),
    )

    @Test
    fun reorderMovesWholeTrackIdentityAndNormalizesOrder() {
        val edited = ProjectTrackEditor.reorderTrack(project(), "c", 0, 10)
        assertEquals(listOf("c", "a", "b"), edited.tracks.sortedBy { it.order }.map { it.id })
        assertEquals(listOf(0, 1, 2), edited.tracks.sortedBy { it.order }.map { it.order })
        assertEquals("b", edited.clips.single().trackId)
        assertEquals(10, edited.updatedAtEpochMs)
    }

    @Test
    fun reorderToEndPreservesClipReferences() {
        val edited = ProjectTrackEditor.reorderTrack(project(), "a", 2, 11)
        assertEquals(listOf("b", "c", "a"), edited.tracks.sortedBy { it.order }.map { it.id })
        assertEquals("b", edited.clips.single().trackId)
    }

    @Test
    fun clearTrackContentsPreservesTrackAndOtherLanes() {
        val withOtherClip = project().copy(
            clips = project().clips + AudioClip(id = "clip-a", trackId = "a", name = "Other", sourceUri = "managed://other.wav", startFrame = 0, lengthFrames = 20),
        )
        val edited = ProjectTrackEditor.clearTrackContents(withOtherClip, "b", 14)
        assertEquals(listOf("a", "b", "c"), edited.tracks.sortedBy { it.order }.map { it.id })
        assertEquals(listOf("clip-a"), edited.clips.map { it.id })
        assertEquals(14, edited.updatedAtEpochMs)
    }

    @Test
    fun rejectsUnknownTrackAndOutOfBoundsTarget() {
        assertFailsWith<IllegalArgumentException> { ProjectTrackEditor.reorderTrack(project(), "missing", 0, 12) }
        assertFailsWith<IllegalArgumentException> { ProjectTrackEditor.reorderTrack(project(), "a", 3, 13) }
        assertFailsWith<IllegalArgumentException> { ProjectTrackEditor.clearTrackContents(project(), "missing", 14) }
    }
}
