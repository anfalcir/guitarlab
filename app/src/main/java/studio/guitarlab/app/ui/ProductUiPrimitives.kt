package studio.guitarlab.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp

/**
 * Shared major section surface for non-Studio product screens.
 *
 * Keeps Settings, Backup and later Activity/Prepare surfaces on the same
 * 10dp maximum panel geometry and header/content hierarchy defined by
 * docs/UI_VISUAL_SYSTEM.md.
 */
@Composable
fun ProductSectionCard(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.18f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.28f)),
        tonalElevation = 0.dp,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                subtitle?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.24f))
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                content = content,
            )
        }
    }
}

enum class ProductStatusTone {
    NEUTRAL,
    ACTIVE,
    SUCCESS,
    ERROR,
}

@Composable
fun ProductStatusChip(
    label: String,
    tone: ProductStatusTone,
    modifier: Modifier = Modifier,
) {
    val containerColor = when (tone) {
        ProductStatusTone.NEUTRAL -> MaterialTheme.colorScheme.surfaceVariant
        ProductStatusTone.ACTIVE -> MaterialTheme.colorScheme.secondaryContainer
        ProductStatusTone.SUCCESS -> MaterialTheme.colorScheme.primaryContainer
        ProductStatusTone.ERROR -> MaterialTheme.colorScheme.errorContainer
    }
    val contentColor = when (tone) {
        ProductStatusTone.NEUTRAL -> MaterialTheme.colorScheme.onSurfaceVariant
        ProductStatusTone.ACTIVE -> MaterialTheme.colorScheme.onSecondaryContainer
        ProductStatusTone.SUCCESS -> MaterialTheme.colorScheme.onPrimaryContainer
        ProductStatusTone.ERROR -> MaterialTheme.colorScheme.onErrorContainer
    }
    Surface(
        modifier = modifier.semantics { stateDescription = label },
        shape = MaterialTheme.shapes.small,
        color = containerColor,
        border = BorderStroke(1.dp, contentColor.copy(alpha = 0.38f)),
        tonalElevation = 0.dp,
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
            style = MaterialTheme.typography.labelMedium,
            color = contentColor,
        )
    }
}

