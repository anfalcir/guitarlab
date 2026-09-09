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
        tracks = listOf(
            AudioTrack(id = "t1", name = "Guitar", order = 0),
            AudioTrack(id = "t2", name = "Double", order = 1),
        ),
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
    fun moveBetweenTracksPreservesSourceAndTiming() {
        val original = project().clips.single()
        val moved = ProjectClipEditor.moveClipToTrack(project(), "c1", "t2", 103).clips.single()
        assertEquals("t2", moved.trackId)
        assertEquals(original.startFrame, moved.startFrame)
        assertEquals(original.sourceStartFrame, moved.sourceStartFrame)
        assertEquals(original.managedSourcePath, moved.managedSourcePath)
        assertEquals(original.sourceUri, moved.sourceUri)
    }

    @Test
    fun duplicateSharesManagedSourceWithoutDuplicatingIdentity() {
        val edited = ProjectClipEditor.duplicateClip(project(), "c1", "c2", 18_000, 104)
        val original = edited.clips.first { it.id == "c1" }
        val duplicate = edited.clips.first { it.id == "c2" }
        assertEquals(2, edited.clips.size)
        assertEquals(18_000, duplicate.startFrame)
        assertEquals(original.managedSourcePath, duplicate.managedSourcePath)
        assertEquals(original.sourceUri, duplicate.sourceUri)
        assertEquals(original.sourceStartFrame, duplicate.sourceStartFrame)
        assertEquals(original.lengthFrames, duplicate.lengthFrames)
    }

    @Test
    fun splitProducesContiguousTimelineAndSourceRanges() {
        val edited = ProjectClipEditor.splitClipAtTimelineFrame(project(), "c1", 13_000, "c2", 105)
        val left = edited.clips.first { it.id == "c1" }
        val right = edited.clips.first { it.id == "c2" }
        assertEquals(3_000, left.lengthFrames)
        assertEquals(13_000, right.startFrame)
        assertEquals(5_000, right.sourceStartFrame)
        assertEquals(5_000, right.lengthFrames)
        assertEquals(left.startFrame + left.lengthFrames, right.startFrame)
        assertEquals(left.sourceStartFrame + left.lengthFrames, right.sourceStartFrame)
        assertEquals(left.managedSourcePath, right.managedSourcePath)
    }

    @Test
    fun trimChangesMetadataOnlyAndRespectsImmutableSourceBounds() {
        val original = project().clips.single()
        val edited = ProjectClipEditor.trimClip(project(), "c1", 4_000, 4_000, 106)
        val clip = edited.clips.single()
        assertEquals(original.sourceUri, clip.sourceUri)
        assertEquals(original.managedSourcePath, clip.managedSourcePath)
        assertEquals(4_000, clip.sourceStartFrame)
        assertEquals(4_000, clip.lengthFrames)
        assertFailsWith<IllegalArgumentException> {
            ProjectClipEditor.trimClip(project(), "c1", 18_000, 4_000, 107)
        }
    }

    @Test
    fun timelineEdgeTrimMovesLeftEdgeAndSourceOffsetTogether() {
        val original = project().clips.single()
        val edited = ProjectClipEditor.trimClipToTimelineEdges(project(), "c1", 12_000, 17_000, 108)
        val clip = edited.clips.single()
        assertEquals(12_000, clip.startFrame)
        assertEquals(4_000, clip.sourceStartFrame)
        assertEquals(5_000, clip.lengthFrames)
        assertEquals(original.sourceUri, clip.sourceUri)
        assertEquals(original.managedSourcePath, clip.managedSourcePath)
    }

    @Test
    fun timelineEdgeTrimCanExtendOnlyInsideImmutableSource() {
        val extended = ProjectClipEditor.trimClipToTimelineEdges(project(), "c1", 8_000, 28_000, 109).clips.single()
        assertEquals(8_000, extended.startFrame)
        assertEquals(0, extended.sourceStartFrame)
        assertEquals(20_000, extended.lengthFrames)
        assertFailsWith<IllegalArgumentException> {
            ProjectClipEditor.trimClipToTimelineEdges(project(), "c1", 7_999, 18_000, 110)
        }
        assertFailsWith<IllegalArgumentException> {
            ProjectClipEditor.trimClipToTimelineEdges(project(), "c1", 10_000, 30_001, 111)
        }
    }

    @Test
    fun rejectsInvalidStructuralTargetsAndSplitBoundaries() {
        assertFailsWith<IllegalArgumentException> { ProjectClipEditor.moveClipToTrack(project(), "c1", "missing", 112) }
        assertFailsWith<IllegalArgumentException> { ProjectClipEditor.duplicateClip(project(), "c1", "c1", 0, 113) }
        assertFailsWith<IllegalArgumentException> { ProjectClipEditor.splitClipAtTimelineFrame(project(), "c1", 10_000, "c2", 114) }
        assertFailsWith<IllegalArgumentException> { ProjectClipEditor.splitClipAtTimelineFrame(project(), "c1", 18_000, "c2", 115) }
    }

    @Test
    fun rejectsMissingClipAndNegativeMove() {
        assertFailsWith<IllegalArgumentException> { ProjectClipEditor.removeClip(project(), "missing", 2) }
        assertFailsWith<IllegalArgumentException> { ProjectClipEditor.moveClip(project(), "c1", -1, 2) }
    }
}
