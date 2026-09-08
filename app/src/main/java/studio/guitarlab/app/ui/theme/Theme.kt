package studio.guitarlab.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val GuitarLabColors = darkColorScheme(
    primary = StudioAmber,
    onPrimary = Graphite950,
    primaryContainer = StudioAmberDark,
    onPrimaryContainer = StudioText,
    background = Graphite950,
    onBackground = StudioText,
    surface = Graphite900,
    onSurface = StudioText,
    surfaceVariant = Graphite800,
    onSurfaceVariant = StudioMuted,
    outline = Graphite700
)

@Composable
fun GuitarLabTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = GuitarLabColors,
        content = content
    )
}
