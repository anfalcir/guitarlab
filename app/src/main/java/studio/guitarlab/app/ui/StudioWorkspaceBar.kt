package studio.guitarlab.app.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import studio.guitarlab.core.project.GuitarAuditionMode

/** Fixed slots; only the available viewport, never operation state, changes layout. */
enum class StudioActionPanel(val label: String) {
    COMPARISON("Comparação"), TIMELINE("Timeline"),
}

@Composable
fun StudioWorkspaceBar(
    auditionMode: GuitarAuditionMode,
    mixerVisible: Boolean,
    mixerMinimal: Boolean,
    onToggleMixerMode: () -> Unit,
    panelContent: @Composable (StudioActionPanel) -> Unit,
    transportContent: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    var openPanel by rememberSaveable { mutableStateOf<StudioActionPanel?>(null) }
    Surface(modifier.fillMaxWidth().testTag("studio-workspace-bar")) {
        BoxWithConstraints(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp)) {
            val viewportWidth = maxWidth
            // At tablet widths the transport has its own centered slot. Small screens scroll
            // the SAME row; controls never wrap or disappear when recording/comparing/changing Mixer mode.
            // Keep viewport and content as separate layout nodes: the scroll modifier's
            // semantic bounds describe its viewport, not the width of the row it scrolls.
            Box(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
                .testTag("studio-workspace-viewport")) {
                Row(
                    Modifier.width(viewportWidth.coerceAtLeast(664.dp)).testTag("studio-workspace-row"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Row(Modifier.width(104.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        StudioActionPanel.entries.forEach { panel ->
                            Box {
                                val active = panel == StudioActionPanel.COMPARISON && auditionMode != GuitarAuditionMode.MIXER
                                AppIconButton(
                                    icon = when (panel) {
                                        StudioActionPanel.COMPARISON -> Icons.Default.CompareArrows
                                        StudioActionPanel.TIMELINE -> Icons.Default.Timeline
                                    },
                                    contentDescription = panel.label,
                                    tint = if (active) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    onClick = { openPanel = if (openPanel == panel) null else panel },
                                    modifier = Modifier.testTag("studio-action-${panel.name.lowercase()}").semantics {
                                        stateDescription = if (panel == StudioActionPanel.COMPARISON) {
                                            when (auditionMode) {
                                                GuitarAuditionMode.MIXER -> "Desativada"
                                                GuitarAuditionMode.REFERENCE -> "Referência"
                                                GuitarAuditionMode.MY_GUITAR -> "Minha"
                                                GuitarAuditionMode.BOTH -> "Ambas"
                                            }
                                        } else if (openPanel == panel) "Aberto" else "Fechado"
                                    },
                                )
                                if (active) {
                                    Box(Modifier.align(Alignment.BottomEnd).size(16.dp), contentAlignment = Alignment.Center) {
                                        Text(
                                            when (auditionMode) {
                                                GuitarAuditionMode.REFERENCE -> "R"
                                                GuitarAuditionMode.MY_GUITAR -> "M"
                                                GuitarAuditionMode.BOTH -> "2"
                                                GuitarAuditionMode.MIXER -> ""
                                            },
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.secondary,
                                        )
                                    }
                                }
                                DropdownMenu(
                                    expanded = openPanel == panel,
                                    onDismissRequest = { openPanel = null },
                                    modifier = Modifier.width(viewportWidth.coerceAtMost(if (panel == StudioActionPanel.COMPARISON) 240.dp else 320.dp))
                                        .testTag("studio-panel-${panel.name.lowercase()}"),
                                ) { panelContent(panel) }
                            }
                        }
                    }
                    Box(Modifier.weight(1f), contentAlignment = Alignment.Center) { transportContent() }
                    Box(Modifier.width(104.dp), contentAlignment = Alignment.CenterEnd) {
                        AppIconButton(
                            icon = if (mixerMinimal) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = if (mixerMinimal) "Mostrar Mixer completo" else "Mostrar Mixer mínimo",
                            enabled = mixerVisible,
                            onClick = onToggleMixerMode,
                            modifier = Modifier.testTag("studio-mixer-mode").semantics {
                                stateDescription = if (mixerMinimal) "Mínimo" else "Completo"
                            },
                        )
                    }
                }
            }
        }
    }
}
