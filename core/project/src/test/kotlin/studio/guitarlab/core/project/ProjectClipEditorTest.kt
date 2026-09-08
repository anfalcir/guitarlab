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
                sourceUri = "managed://media/source/take.wav",
                managedSourcePath = "media/source/take.wav",
                startFrame = 10_000,
                sourceStartFrame = 2_000,
                lengthFrames = 8_000,
                sourceTotalFrames = 20_000,
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
    fun trimChangesMetadataOnlyAndRespectsImmutableSourceBounds() {
        val original = project().clips.single()
        val edited = ProjectClipEditor.trimClip(project(), "c1", 4_000, 4_000, 103)
        val clip = edited.clips.single()
        assertEquals(original.sourceUri, clip.sourceUri)
        assertEquals(original.managedSourcePath, clip.managedSourcePath)
        assertEquals(4_000, clip.sourceStartFrame)
        assertEquals(4_000, clip.lengthFrames)
        assertFailsWith<IllegalArgumentException> {
            ProjectClipEditor.trimClip(project(), "c1", 18_000, 4_000, 104)
        }
    }

    @Test
    fun timelineEdgeTrimMovesLeftEdgeAndSourceOffsetTogether() {
        val original = project().clips.single()
        val edited = ProjectClipEditor.trimClipToTimelineEdges(project(), "c1", 12_000, 17_000, 105)
        val clip = edited.clips.single()
        assertEquals(12_000, clip.startFrame)
        assertEquals(4_000, clip.sourceStartFrame)
        assertEquals(5_000, clip.lengthFrames)
        assertEquals(original.sourceUri, clip.sourceUri)
        assertEquals(original.managedSourcePath, clip.managedSourcePath)
    }

    @Test
    fun timelineEdgeTrimCanExtendOnlyInsideImmutableSource() {
        val extended = ProjectClipEditor.trimClipToTimelineEdges(project(), "c1", 8_000, 28_000, 106).clips.single()
        assertEquals(8_000, extended.startFrame)
        assertEquals(0, extended.sourceStartFrame)
        assertEquals(20_000, extended.lengthFrames)
        assertFailsWith<IllegalArgumentException> {
            ProjectClipEditor.trimClipToTimelineEdges(project(), "c1", 7_999, 18_000, 107)
        }
        assertFailsWith<IllegalArgumentException> {
            ProjectClipEditor.trimClipToTimelineEdges(project(), "c1", 10_000, 30_001, 108)
        }
    }

    @Test
    fun rejectsMissingClipAndNegativeMove() {
        assertFailsWith<IllegalArgumentException> { ProjectClipEditor.removeClip(project(), "missing", 2) }
        assertFailsWith<IllegalArgumentException> { ProjectClipEditor.moveClip(project(), "c1", -1, 2) }
    }
}
