package studio.guitarlab.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import studio.guitarlab.core.model.AssetRole
import studio.guitarlab.core.model.GuitarProject
import studio.guitarlab.core.model.ManagedAsset

@Composable
fun ProjectMediaDialog(
    project: GuitarProject,
    referenceBindingDiffers: Boolean,
    referenceDecisionPending: Boolean,
    onKeepCurrent: () -> Unit,
    onApplyUpdate: () -> Unit,
    onDismiss: () -> Unit,
) {
    val boundIds = project.referenceBindings.mapTo(mutableSetOf()) { it.assetId }
    val activePreparationIds = buildSet {
        project.preparation?.sourceAssetId?.let(::add)
        project.preparation?.activeStemAssetIds?.values?.let(::addAll)
        project.preparation?.activeBackingAssetId?.let(::add)
        project.preparation?.activeGuitarAssetId?.let(::add)
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("project-media-dialog"),
        title = { Text("Mídia do projeto") },
        text = {
            Column(
                Modifier.fillMaxWidth().heightIn(max = 520.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    "Arquivos gerenciados pelo GuitarLab. Atualizar referências não adiciona stems à timeline nem altera suas gravações.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (referenceBindingDiffers) {
                    Column(
                        Modifier.fillMaxWidth().testTag("project-media-reference-update"),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text("Uma nova versão preparada está disponível.", style = MaterialTheme.typography.titleSmall)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            if (referenceDecisionPending) {
                                OutlinedButton(onClick = onKeepCurrent) { Text("Manter atual") }
                            }
                            Button(onClick = onApplyUpdate) { Text("Usar nova versão") }
                        }
                    }
                    HorizontalDivider()
                }
                MediaGroup("Fonte", project.assets.filter { it.role == AssetRole.SOURCE_ORIGINAL }, activePreparationIds, boundIds)
                MediaGroup("Stems preparados", project.assets.filter { it.role.name.startsWith("STEM_") }, activePreparationIds, boundIds)
                MediaGroup("Referências", project.assets.filter { it.role in setOf(AssetRole.REFERENCE_BACKING, AssetRole.REFERENCE_GUITAR) }, activePreparationIds, boundIds)
                MediaGroup("Gravações", project.assets.filter { it.role == AssetRole.RECORDING_TAKE }, activePreparationIds, boundIds)
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Fechar") } },
    )
}

@Composable
private fun MediaGroup(
    title: String,
    assets: List<ManagedAsset>,
    activeIds: Set<String>,
    boundIds: Set<String>,
) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("$title · ${assets.size}", style = MaterialTheme.typography.titleSmall)
        if (assets.isEmpty()) {
            Text("Nenhum arquivo", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            assets.sortedByDescending { it.createdAtEpochMs }.forEach { asset ->
                val status = when {
                    asset.assetId in boundIds -> "Em uso no Studio"
                    asset.assetId in activeIds -> "Versão preparada ativa"
                    else -> "Versão anterior"
                }
                Column(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                    Text(asset.relativePath.substringAfterLast('/'), style = MaterialTheme.typography.bodyMedium)
                    Text("$status · ${asset.format.uppercase()} · ${formatBytes(asset.byteSize)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

private fun formatBytes(bytes: Long): String = when {
    bytes >= 1024L * 1024L -> "%.1f MB".format(bytes / (1024.0 * 1024.0))
    bytes >= 1024L -> "%.1f KB".format(bytes / 1024.0)
    else -> "$bytes B"
}
