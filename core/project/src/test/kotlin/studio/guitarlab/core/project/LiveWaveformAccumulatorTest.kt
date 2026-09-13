package studio.guitarlab.core.project

import kotlin.test.*

class LiveWaveformAccumulatorTest {
    @Test fun staysBoundedAndPreservesPeak() {
        val waveform = LiveWaveformAccumulator(8)
        repeat(200) { waveform.append(if (it == 41) 1f else 0.1f) }
        assertTrue(waveform.snapshot().size <= 8)
        assertEquals(1f, waveform.snapshot().max())
    }
    @Test fun clampsInputAndClears() {
        val waveform = LiveWaveformAccumulator(8)
        assertEquals(listOf(1f), waveform.append(4f))
        waveform.clear()
        assertTrue(waveform.snapshot().isEmpty())
    }
}
