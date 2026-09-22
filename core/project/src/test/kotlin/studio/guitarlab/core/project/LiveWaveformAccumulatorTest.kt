package studio.guitarlab.core.project

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LiveWaveformAccumulatorTest {
    @Test fun `stays bounded preserves transient and exact frame coverage`() {
        val waveform = LiveWaveformAccumulator(8)
        var frames = 0L
        repeat(200) { index ->
            frames += if (index % 3 == 0) 64L else 257L
            waveform.append(frames, if (index == 41) 1f else 0.1f)
        }
        val points = waveform.snapshot()
        assertTrue(points.size <= 8)
        assertEquals(1f, points.maxOf { it.peak })
        assertEquals(frames, waveform.coveredFrames())
        assertEquals(0L, points.first().startFrame)
        assertEquals(frames, points.last().endFrameExclusive)
        points.zipWithNext().forEach { (left, right) -> assertEquals(left.endFrameExclusive, right.startFrame) }
    }

    @Test fun `callback cadence does not define display density and duplicate frames are ignored`() {
        val waveform = LiveWaveformAccumulator(8)
        waveform.append(128L, 0.2f)
        waveform.append(1024L, 0.8f)
        val beforeDuplicate = waveform.snapshot()
        waveform.append(1024L, 1f)

        assertEquals(beforeDuplicate, waveform.snapshot())
        assertEquals(1024L, waveform.coveredFrames())
        assertEquals(0L, waveform.snapshot().first().startFrame)
        assertEquals(1024L, waveform.snapshot().last().endFrameExclusive)
        waveform.snapshot().zipWithNext().forEach { (left, right) ->
            assertEquals(left.endFrameExclusive, right.startFrame)
        }
    }

    @Test fun `rebinned history and future capture keep one uniform temporal resolution`() {
        val waveform = LiveWaveformAccumulator(8)
        var frames = 0L
        repeat(80) { index ->
            frames += if (index % 5 == 0) 96L else 128L
            waveform.append(frames, if (index == 7 || index == 55) 0.95f else 0.2f)
        }
        val afterRebin = waveform.snapshot()
        val fullDurations = afterRebin.dropLast(1).map { it.endFrameExclusive - it.startFrame }.distinct()
        assertTrue(fullDurations.size <= 1, "Historical buckets must have one resolution: $fullDurations")

        repeat(20) {
            frames += 64L
            waveform.append(frames, 0.3f)
        }
        val afterFutureCapture = waveform.snapshot()
        val futureFullDurations = afterFutureCapture.dropLast(1).map { it.endFrameExclusive - it.startFrame }.distinct()
        assertTrue(futureFullDurations.size <= 1, "New capture must use the same rebinned resolution: $futureFullDurations")
        assertTrue(afterFutureCapture.size <= 8)
        assertEquals(frames, afterFutureCapture.last().endFrameExclusive)
        assertEquals(0.95f, afterFutureCapture.maxOf { it.peak })
    }

    @Test fun `clamps input and clears frame clock and bucket resolution`() {
        val waveform = LiveWaveformAccumulator(8)
        waveform.append(32L, 4f)
        assertEquals(1f, waveform.snapshot().single().peak)
        waveform.clear()
        assertTrue(waveform.snapshot().isEmpty())
        assertEquals(0L, waveform.coveredFrames())
        waveform.append(16L, 0.5f)
        assertEquals(0L, waveform.snapshot().single().startFrame)
        assertEquals(16L, waveform.snapshot().single().endFrameExclusive)
    }
}
