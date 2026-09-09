package studio.guitarlab.app.ui

import androidx.compose.ui.graphics.Color
import studio.guitarlab.core.model.AudioTrack

val StudioTrackPalette: List<Color> = listOf(
    Color(0xFF58C4B6),
    Color(0xFF4EA8DE),
    Color(0xFF7B8CFF),
    Color(0xFF9B7EDE),
    Color(0xFFC77DFF),
    Color(0xFFE56B8A),
    Color(0xFFF28482),
    Color(0xFFF6AE2D),
    Color(0xFFD9A441),
    Color(0xFF90BE6D),
    Color(0xFF43AA8B),
    Color(0xFF577590),
    Color(0xFF5FA8D3),
    Color(0xFFB8C480),
    Color(0xFFE07A5F),
    Color(0xFFA3BE8C),
    Color(0xFFB48EAD),
    Color(0xFF8EC5FC),
    Color(0xFF74C69D),
    Color(0xFFADB5BD),
)

fun AudioTrack.resolvedStudioColor(): Color {
    val index = if (colorIndex in StudioTrackPalette.indices) colorIndex else order
    val normalized = ((index % StudioTrackPalette.size) + StudioTrackPalette.size) % StudioTrackPalette.size
    return StudioTrackPalette[normalized]
}
