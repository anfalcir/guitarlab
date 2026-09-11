package studio.guitarlab.app.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

@Composable
fun AppIconButton(
    icon: ImageVector = Icons.Default.Info,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    tint: Color = LocalContentColor.current,
) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier,
    ) {
        Icon(imageVector = icon, contentDescription = contentDescription, tint = tint)
    }
}
