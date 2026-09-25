package studio.guitarlab.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlin.math.ceil
import kotlin.math.floor

internal object WaveformRenderReducer {
    /**
     * Keeps the maximum peak that contributes to each visible column. This makes rendering cost
     * proportional to screen pixels rather than cache resolution while preserving transients.
     */
    fun maxPerColumn(peaks: List<Float>, columns: Int): List<Float> {
        if (peaks.isEmpty() || columns <= 0) return emptyList()
        if (peaks.size <= columns) return peaks
        val outputColumns = columns.coerceAtMost(peaks.size)
        return List(outputColumns) { column ->
            val start = floor(column.toDouble() * peaks.size / outputColumns).toInt()
            val endExclusive = ceil((column + 1).toDouble() * peaks.size / outputColumns).toInt()
                .coerceAtMost(peaks.size)
                .coerceAtLeast(start + 1)
            var peak = 0f
            for (index in start until endExclusive) peak = maxOf(peak, peaks[index])
            peak
        }
    }
}

@Composable
fun WaveformMini(
    peaks: List<Float>,
    modifier: Modifier = Modifier,
    muted: Boolean = false,
    color: Color = MaterialTheme.colorScheme.primary,
) {
    val waveformColor = if (muted) MaterialTheme.colorScheme.onSurfaceVariant else color
    Canvas(modifier.fillMaxWidth().height(18.dp)) {
        if (peaks.isEmpty() || size.width <= 0f) return@Canvas
        val columns = size.width.toInt().coerceAtLeast(1)
        val visiblePeaks = WaveformRenderReducer.maxPerColumn(peaks, columns)
        if (visiblePeaks.isEmpty()) return@Canvas
        val centerY = size.height / 2f
        val step = size.width / visiblePeaks.size
        visiblePeaks.forEachIndexed { index, rawPeak ->
            val amplitude = rawPeak.coerceIn(0f, 1f) * centerY
            val x = (index + 0.5f) * step
            drawLine(
                color = waveformColor,
                start = Offset(x, centerY - amplitude),
                end = Offset(x, centerY + amplitude),
                strokeWidth = step.coerceAtMost(2f).coerceAtLeast(1f),
            )
        }
    }
}
