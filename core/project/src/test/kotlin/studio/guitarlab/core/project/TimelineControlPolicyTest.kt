package studio.guitarlab.core.project

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import studio.guitarlab.core.model.AudioClip
import studio.guitarlab.core.model.AudioTrack
import studio.guitarlab.core.model.GuitarProject
import studio.guitarlab.core.model.ProjectTemplate

class TimelineControlPolicyTest {
    private fun project(): GuitarProject = GuitarProject(
        id = "p1",
        name = "Timeline",
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
                startFrame = 12_000,
                lengthFrames = 48_000,
            )
        ),
    )

    @Test
    fun projectEndAndPlayheadAreBounded() {
        val end = TimelineControlPolicy.projectEndFrame(project())
        assertEquals(60_000, end)
        val moved = TimelineControlPolicy.movePlayhead(TimelineControlState(), 90_000, end)
        assertEquals(end, moved.playheadFrame)
    }

    @Test
    fun loopMarkersCannotCross() {
        val end = 48_000L
        val initial = TimelineControlState(loopStartFrame = 12_000, loopEndFrame = 36_000)
        val startMoved = TimelineControlPolicy.moveLoopStart(initial, 40_000, end)
        assertEquals(35_999, startMoved.loopStartFrame)
        val endMoved = TimelineControlPolicy.moveLoopEnd(initial, 1_000, end)
        assertEquals(12_001, endMoved.loopEndFrame)
    }

    @Test
    fun frameFractionMappingIsStableAndClamped() {
        assertEquals(0.5f, TimelineControlPolicy.frameToFraction(24_000, 48_000))
        assertEquals(24_000, TimelineControlPolicy.fractionToFrame(0.5f, 48_000))
        assertEquals(0, TimelineControlPolicy.fractionToFrame(-1f, 48_000))
        assertEquals(48_000, TimelineControlPolicy.fractionToFrame(2f, 48_000))
        assertTrue(TimelineControlPolicy.normalizedForProject(TimelineControlState(), 48_000).loopEndFrame > 0)
    }
}
