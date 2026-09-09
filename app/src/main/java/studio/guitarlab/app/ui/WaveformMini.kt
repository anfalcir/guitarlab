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

@Composable
fun WaveformMini(
    peaks: List<Float>,
    modifier: Modifier = Modifier,
    muted: Boolean = false,
    color: Color = MaterialTheme.colorScheme.primary,
) {
    val waveformColor = if (muted) MaterialTheme.colorScheme.onSurfaceVariant else color
    Canvas(modifier.fillMaxWidth().height(18.dp)) {
        if (peaks.isEmpty()) return@Canvas
        val centerY = size.height / 2f
        val step = size.width / peaks.size.coerceAtLeast(1)
        peaks.forEachIndexed { index, rawPeak ->
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
