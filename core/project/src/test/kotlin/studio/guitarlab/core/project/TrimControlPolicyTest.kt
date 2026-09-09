package studio.guitarlab.core.project

import kotlin.test.Test
import kotlin.test.assertEquals
import studio.guitarlab.core.model.AudioClip

class TrimControlPolicyTest {
    private val clip = AudioClip(
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

    @Test
    fun derivesCurrentVisibleEdges() {
        assertEquals(TrimControlState("c1", 12_800, 15_200), TrimControlPolicy.fromClip(clip))
    }

    @Test
    fun startCannotExposeBeforeSourceZeroOrCrossEnd() {
        val state = TrimControlPolicy.fromClip(clip)
        assertEquals(8_000, TrimControlPolicy.moveStart(state, 0, clip).startFrame)
        assertEquals(15_199, TrimControlPolicy.moveStart(state, 99_999, clip).startFrame)
    }

    @Test
    fun endCannotCrossStartOrExposePastSourceEnd() {
        val state = TrimControlPolicy.fromClip(clip)
        assertEquals(12_801, TrimControlPolicy.moveEnd(state, 0, clip).endFrame)
        assertEquals(28_000, TrimControlPolicy.moveEnd(state, 99_999, clip).endFrame)
    }
}
