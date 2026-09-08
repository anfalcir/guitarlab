package studio.guitarlab.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import kotlin.math.roundToLong
import studio.guitarlab.app.ui.theme.StudioLoop
import studio.guitarlab.app.ui.theme.StudioPlayhead
import studio.guitarlab.app.ui.theme.StudioRecord
import studio.guitarlab.app.ui.theme.StudioTrim
import studio.guitarlab.core.project.TimelineControlPolicy

enum class TimelineMarkerKind {
    PLAYHEAD,
    LOOP_START,
    LOOP_END,
    TRIM_START,
    TRIM_END,
    RECORD_HEAD,
}

@Composable
fun TimelineMarkerRail(
    projectEndFrame: Long,
    playheadFrame: Long,
    loopStartFrame: Long,
    loopEndFrame: Long,
    enabled: Boolean,
    onPlayheadFrameChanged: (Long) -> Unit,
    onLoopStartFrameChanged: (Long) -> Unit,
    onLoopEndFrameChanged: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.Start) {
        Row(modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)) {
            MarkerLegend("Playhead", StudioPlayhead)
            MarkerLegend("Loop in/out", StudioLoop)
            if (!enabled) {
                Text(
                    "Locked while transport is active",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(104.dp)
                .alpha(if (enabled) 1f else 0.48f),
        ) {
            val density = LocalDensity.current
            val widthPx = with(density) { maxWidth.toPx() }.coerceAtLeast(1f)
            TimelineMarker(TimelineMarkerKind.LOOP_START, loopStartFrame, projectEndFrame, widthPx, enabled, onLoopStartFrameChanged)
            TimelineMarker(TimelineMarkerKind.LOOP_END, loopEndFrame, projectEndFrame, widthPx, enabled, onLoopEndFrameChanged)
            TimelineMarker(TimelineMarkerKind.PLAYHEAD, playheadFrame, projectEndFrame, widthPx, enabled, onPlayheadFrameChanged)
        }
    }
}

@Composable
private fun TimelineMarker(
    kind: TimelineMarkerKind,
    frame: Long,
    projectEndFrame: Long,
    widthPx: Float,
    enabled: Boolean,
    onFrameChanged: (Long) -> Unit,
) {
    val fraction = TimelineControlPolicy.frameToFraction(frame, projectEndFrame)
    val markerWidth = 48.dp
    val density = LocalDensity.current
    val markerWidthPx = with(density) { markerWidth.toPx() }
    val x = (fraction * widthPx - markerWidthPx / 2f)
        .coerceIn(-markerWidthPx / 2f, widthPx - markerWidthPx / 2f)
    val color = markerColor(kind)
    val label = markerLabel(kind)

    Box(
        modifier = Modifier
            .offset { IntOffset(x.roundToInt(), 0) }
            .width(markerWidth)
            .height(104.dp),
        contentAlignment = Alignment.TopCenter,
    ) {
        Box(
            modifier = Modifier
                .width(markerWidth)
                .height(40.dp)
                .draggable(
                    enabled = enabled,
                    orientation = Orientation.Horizontal,
                    state = rememberDraggableState { deltaPx ->
                        val deltaFrames = (deltaPx / widthPx * projectEndFrame.coerceAtLeast(1L)).roundToLong()
                        onFrameChanged((frame + deltaFrames).coerceIn(0L, projectEndFrame.coerceAtLeast(1L)))
                    },
                ),
            contentAlignment = Alignment.TopCenter,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Surface(shape = RoundedCornerShape(7.dp), color = color, tonalElevation = 0.dp) {
                    Text(
                        text = label,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White,
                    )
                }
                Canvas(Modifier.size(14.dp, 8.dp)) {
                    val path = Path().apply {
                        moveTo(0f, 0f)
                        lineTo(size.width, 0f)
                        lineTo(size.width / 2f, size.height)
                        close()
                    }
                    drawPath(path, color)
                }
            }
        }
        Canvas(Modifier.padding(top = 39.dp).width(2.dp).height(65.dp)) {
            drawLine(
                color = color.copy(alpha = 0.82f),
                start = Offset(size.width / 2f, 0f),
                end = Offset(size.width / 2f, size.height),
                strokeWidth = size.width,
            )
        }
    }
}

@Composable
private fun MarkerLegend(text: String, color: Color) {
    Row(modifier = Modifier.padding(end = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        Surface(modifier = Modifier.size(8.dp), shape = RoundedCornerShape(50), color = color) {}
        Text(
            text = text,
            modifier = Modifier.padding(start = 6.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun markerColor(kind: TimelineMarkerKind): Color = when (kind) {
    TimelineMarkerKind.PLAYHEAD -> StudioPlayhead
    TimelineMarkerKind.LOOP_START, TimelineMarkerKind.LOOP_END -> StudioLoop
    TimelineMarkerKind.TRIM_START, TimelineMarkerKind.TRIM_END -> StudioTrim
    TimelineMarkerKind.RECORD_HEAD -> StudioRecord
}

private fun markerLabel(kind: TimelineMarkerKind): String = when (kind) {
    TimelineMarkerKind.PLAYHEAD -> "P"
    TimelineMarkerKind.LOOP_START -> "L◀"
    TimelineMarkerKind.LOOP_END -> "L▶"
    TimelineMarkerKind.TRIM_START -> "T◀"
    TimelineMarkerKind.TRIM_END -> "T▶"
    TimelineMarkerKind.RECORD_HEAD -> "R"
}
