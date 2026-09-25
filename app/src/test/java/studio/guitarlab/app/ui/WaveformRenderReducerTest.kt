package studio.guitarlab.app.ui

import kotlin.test.Test
import kotlin.test.assertEquals

class WaveformRenderReducerTest {
    @Test
    fun preservesMaximumTransientPerVisibleColumn() {
        val reduced = WaveformRenderReducer.maxPerColumn(
            peaks = listOf(0.1f, 0.8f, 0.2f, 0.3f, 1f, 0.4f, 0.2f, 0.6f),
            columns = 4,
        )
        assertEquals(listOf(0.8f, 0.3f, 1f, 0.6f), reduced)
    }

    @Test
    fun neverExpandsAlreadyScreenSizedEnvelope() {
        val peaks = listOf(0.1f, 0.5f, 0.2f)
        assertEquals(peaks, WaveformRenderReducer.maxPerColumn(peaks, 20))
        assertEquals(emptyList(), WaveformRenderReducer.maxPerColumn(peaks, 0))
    }
}
