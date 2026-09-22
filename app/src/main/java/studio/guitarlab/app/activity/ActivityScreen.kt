package studio.guitarlab.app.activity

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.text.DateFormat
import java.util.Date
import studio.guitarlab.app.ui.AppIconButton
import studio.guitarlab.core.project.UnifiedOperationKind
import studio.guitarlab.core.project.UnifiedOperationRecord
import studio.guitarlab.core.project.UnifiedOperationState

@Composable
fun ActivityScreen(
    onBack: () -> Unit,
    viewModel: UnifiedActivityViewModel,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    Column(
        Modifier.fillMaxSize().padding(horizontal = 22.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            AppIconButton(Icons.Default.ArrowBack, "Voltar", onBack)
            Text("Atividade", Modifier.weight(1f).padding(start = 10.dp), style = MaterialTheme.typography.headlineSmall)
            Icon(Icons.Default.Info, contentDescription = "Atividade reúne operações do aplicativo")
        }
        Text(
            "Acompanhe fontes, separação, referências, exportações e backup no mesmo histórico.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (state.records.isEmpty()) {
            ActivityEmptyState(Modifier.fillMaxWidth().weight(1f))
        } else {
            LazyColumn(
                Modifier.fillMaxWidth().weight(1f).testTag("activity-screen"),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(state.records, key = { it.operationId }) { record -> ActivityRecordCard(record) }
            }
        }
    }
}

@Composable
private fun ActivityEmptyState(modifier: Modifier) {
    Box(modifier, contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Default.Sync, contentDescription = null)
            Text("Nenhuma atividade recente", style = MaterialTheme.typography.titleMedium)
            Text("Quando uma operação começar, ela aparecerá aqui.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ActivityRecordCard(record: UnifiedOperationRecord) {
    val active = record.state.isActive
    Card(Modifier.fillMaxWidth().testTag("activity-record-${record.operationId}")) {
        Row(Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
            Icon(
                when {
                    active -> Icons.Default.Refresh
                    record.state == UnifiedOperationState.SUCCEEDED -> Icons.Default.CheckCircle
                    record.state == UnifiedOperationState.FAILED -> Icons.Default.Error
                    else -> Icons.Default.Info
                },
                contentDescription = null,
                tint = when {
                    record.state == UnifiedOperationState.FAILED -> MaterialTheme.colorScheme.error
                    record.state == UnifiedOperationState.SUCCEEDED -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(activityKindLabel(record.kind), style = MaterialTheme.typography.titleSmall)
                Text(record.summary, style = MaterialTheme.typography.bodyMedium)
                record.projectId?.let { Text("Projeto $it", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                record.progressPercent?.let { progress ->
                    if (active) {
                        androidx.compose.material3.LinearProgressIndicator(
                            progress = { progress / 100f },
                            Modifier.fillMaxWidth().padding(top = 4.dp),
                        )
                    }
                }
                Text(
                    DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(record.updatedAtEpochMs)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private fun activityKindLabel(kind: UnifiedOperationKind): String = when (kind) {
    UnifiedOperationKind.SOURCE_ACQUISITION -> "Fonte"
    UnifiedOperationKind.SEPARATION -> "Separação"
    UnifiedOperationKind.REFERENCE_PREPARATION -> "Referências"
    UnifiedOperationKind.EXPORT -> "Exportação"
    UnifiedOperationKind.BACKUP -> "Backup"
    UnifiedOperationKind.RESTORE -> "Restauração"
}
