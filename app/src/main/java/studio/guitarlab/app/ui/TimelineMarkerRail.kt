package studio.guitarlab.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
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
import studio.guitarlab.core.model.PunchRegion
import studio.guitarlab.core.model.TimelineMarker as ProjectTimelineMarker
import studio.guitarlab.core.model.TimelineSection

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
    sampleRateHz: Int,
    playheadFrame: Long,
    loopStartFrame: Long,
    loopEndFrame: Long,
    showLoopMarkers: Boolean,
    sections: List<TimelineSection>,
    markers: List<ProjectTimelineMarker>,
    punchRegion: PunchRegion?,
    enabled: Boolean,
    onPlayheadFrameChanged: (Long) -> Unit,
    onLoopStartFrameChanged: (Long) -> Unit,
    onLoopEndFrameChanged: (Long) -> Unit,
    onSectionClick: (String) -> Unit,
    onSectionRemove: (String) -> Unit,
    onMarkerRemove: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(
        modifier = modifier.height(62.dp).alpha(if (enabled) 1f else 0.52f),
    ) {
        val density = LocalDensity.current
        val widthPx = with(density) { maxWidth.toPx() }.coerceAtLeast(1f)
        TimelineAnnotations(
            sections = sections,
            markers = markers,
            punchRegion = punchRegion,
            projectEndFrame = projectEndFrame,
            widthPx = widthPx,
            onSectionClick = onSectionClick,
            onSectionRemove = onSectionRemove,
            onMarkerRemove = onMarkerRemove,
        )
        if (showLoopMarkers) {
            TimelineMarker(TimelineMarkerKind.LOOP_START, loopStartFrame, projectEndFrame, sampleRateHz, widthPx, enabled, onLoopStartFrameChanged)
            TimelineMarker(TimelineMarkerKind.LOOP_END, loopEndFrame, projectEndFrame, sampleRateHz, widthPx, enabled, onLoopEndFrameChanged)
        }
        TimelineMarker(TimelineMarkerKind.PLAYHEAD, playheadFrame, projectEndFrame, sampleRateHz, widthPx, enabled, onPlayheadFrameChanged)
    }
}

@Composable
private fun TimelineAnnotations(
    sections: List<TimelineSection>,
    markers: List<ProjectTimelineMarker>,
    punchRegion: PunchRegion?,
    projectEndFrame: Long,
    widthPx: Float,
    onSectionClick: (String) -> Unit,
    onSectionRemove: (String) -> Unit,
    onMarkerRemove: (String) -> Unit,
) {
    val density = LocalDensity.current
    val sectionColor = MaterialTheme.colorScheme.primary
    val markerColor = MaterialTheme.colorScheme.tertiary
    sections.forEach { section ->
        val start = TimelineControlPolicy.frameToFraction(section.startFrame, projectEndFrame) * widthPx
        val end = TimelineControlPolicy.frameToFraction(section.endFrame, projectEndFrame) * widthPx
        val width = (end - start).coerceAtLeast(with(density) { 34.dp.toPx() })
        Surface(
            modifier = Modifier
                .offset { IntOffset(start.roundToInt(), with(density) { 2.dp.roundToPx() }) }
                .width(with(density) { width.toDp() })
                .height(24.dp),
            shape = RoundedCornerShape(5.dp),
            color = sectionColor.copy(alpha = 0.16f),
            border = androidx.compose.foundation.BorderStroke(1.dp, sectionColor.copy(alpha = 0.48f)),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = section.name,
                    modifier = Modifier.weight(1f).clickable { onSectionClick(section.id) }.padding(start = 5.dp, top = 3.dp, bottom = 3.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = sectionColor,
                    maxLines = 1,
                )
                Text("×", modifier = Modifier.clickable { onSectionRemove(section.id) }.padding(horizontal = 5.dp, vertical = 3.dp), style = MaterialTheme.typography.labelSmall, color = sectionColor)
            }
        }
    }
    punchRegion?.let { punch ->
        val start = TimelineControlPolicy.frameToFraction(punch.startFrame, projectEndFrame) * widthPx
        val end = TimelineControlPolicy.frameToFraction(punch.endFrame, projectEndFrame) * widthPx
        Box(
            Modifier
                .offset { IntOffset(start.roundToInt(), with(density) { 27.dp.roundToPx() }) }
                .width(with(density) { (end - start).coerceAtLeast(1f).toDp() })
                .height(3.dp)
                .alpha(0.72f),
        ) { Canvas(Modifier.fillMaxHeight().width(with(density) { (end - start).coerceAtLeast(1f).toDp() })) { drawRect(StudioRecord) } }
    }
    markers.forEach { marker ->
        val x = TimelineControlPolicy.frameToFraction(marker.frame, projectEndFrame) * widthPx
        Column(
            modifier = Modifier.offset { IntOffset((x - with(density) { 5.dp.toPx() }).roundToInt(), with(density) { 25.dp.roundToPx() }) },
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Canvas(Modifier.size(10.dp, 7.dp)) {
                val path = Path().apply { moveTo(size.width / 2f, 0f); lineTo(size.width, size.height); lineTo(0f, size.height); close() }
                drawPath(path, markerColor)
            }
            Text("${marker.name} ×", modifier = Modifier.clickable { onMarkerRemove(marker.id) }, style = MaterialTheme.typography.labelSmall, color = markerColor, maxLines = 1)
        }
    }
}

@Composable
private fun TimelineMarker(
    kind: TimelineMarkerKind,
    frame: Long,
    projectEndFrame: Long,
    sampleRateHz: Int,
    widthPx: Float,
    enabled: Boolean,
    onFrameChanged: (Long) -> Unit,
) {
    val fraction = TimelineControlPolicy.frameToFraction(frame, projectEndFrame)
    val markerWidth = 86.dp
    val density = LocalDensity.current
    val markerWidthPx = with(density) { markerWidth.toPx() }
    val markerTop = when (kind) {
        TimelineMarkerKind.PLAYHEAD, TimelineMarkerKind.RECORD_HEAD -> 30.dp
        else -> 2.dp
    }
    val markerTopPx = with(density) { markerTop.roundToPx() }
    val x = (fraction * widthPx - markerWidthPx / 2f).coerceIn(-markerWidthPx / 2f, widthPx - markerWidthPx / 2f)
    val color = markerColor(kind)
    val label = markerLabel(kind)

    Box(
        modifier = Modifier.offset { IntOffset(x.roundToInt(), markerTopPx) }.width(markerWidth).height(32.dp),
        contentAlignment = Alignment.TopCenter,
    ) {
        Box(
            modifier = Modifier.width(markerWidth).height(32.dp).draggable(
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
                        text = "$label ${formatFrameTime(frame, sampleRateHz)}",
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
    }
}

private fun markerColor(kind: TimelineMarkerKind): Color = when (kind) {
    TimelineMarkerKind.PLAYHEAD -> StudioPlayhead
    TimelineMarkerKind.LOOP_START, TimelineMarkerKind.LOOP_END -> StudioLoop
    TimelineMarkerKind.TRIM_START, TimelineMarkerKind.TRIM_END -> StudioTrim
    TimelineMarkerKind.RECORD_HEAD -> StudioRecord
}

internal fun formatFrameTime(frame: Long, sampleRateHz: Int): String {
    val safeRate = sampleRateHz.coerceAtLeast(1)
    val totalSeconds = (frame.coerceAtLeast(0L) / safeRate).coerceAtLeast(0L)
    val hours = totalSeconds / 3600L
    val minutes = (totalSeconds % 3600L) / 60L
    val seconds = totalSeconds % 60L
    return if (hours > 0L) "%d:%02d:%02d".format(hours, minutes, seconds) else "%02d:%02d".format(minutes, seconds)
}

private fun markerLabel(kind: TimelineMarkerKind): String = when (kind) {
    TimelineMarkerKind.PLAYHEAD -> "P"
    TimelineMarkerKind.LOOP_START -> "L◀"
    TimelineMarkerKind.LOOP_END -> "L▶"
    TimelineMarkerKind.TRIM_START -> "T◀"
    TimelineMarkerKind.TRIM_END -> "T▶"
    TimelineMarkerKind.RECORD_HEAD -> "R"
}
