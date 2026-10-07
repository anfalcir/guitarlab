package studio.guitarlab.platform.audio.android

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CueNonBlockingWriteQueueTest {
    @Test
    fun partialWritesAreRetainedAndDrainedWithoutDroppingSamples() {
        val queue = CueNonBlockingWriteQueue(maxPendingSamples = 32, maxWritesPerDrain = 2)
        assertTrue(queue.enqueue(FloatArray(12) { it.toFloat() }, 12))
        var calls = 0
        val first = queue.drain { _, _, remaining ->
            calls += 1
            if (calls == 1) 4 else 0
        }
        assertFalse(first.fatalError)
        assertEquals(8, first.pendingSamples)

        val second = queue.drain { _, _, remaining -> remaining }
        assertFalse(second.fatalError)
        assertEquals(0, second.pendingSamples)
        assertEquals(0, queue.pendingSamples)
    }

    @Test
    fun boundedBacklogRejectsGrowthWithoutBlockingMain() {
        val queue = CueNonBlockingWriteQueue(maxPendingSamples = 8)
        assertTrue(queue.enqueue(FloatArray(8), 8))
        assertFalse(queue.enqueue(FloatArray(2), 2))
        assertEquals(8, queue.pendingSamples)
    }

    @Test
    fun negativeWriteIsFatalButZeroWriteIsTransient() {
        val queue = CueNonBlockingWriteQueue(maxPendingSamples = 16)
        queue.enqueue(FloatArray(8), 8)
        assertFalse(queue.drain { _, _, _ -> 0 }.fatalError)
        assertEquals(8, queue.pendingSamples)
        assertTrue(queue.drain { _, _, _ -> -3 }.fatalError)
        assertEquals(8, queue.pendingSamples)
    }

    @Test
    fun clearDropsPendingSamplesForSeekOrRouteLoss() {
        val queue = CueNonBlockingWriteQueue(maxPendingSamples = 16)
        queue.enqueue(FloatArray(8), 8)
        queue.clear()
        assertEquals(0, queue.pendingSamples)
    }
}
