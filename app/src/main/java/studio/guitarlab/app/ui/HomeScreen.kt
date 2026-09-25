package studio.guitarlab.app.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import studio.guitarlab.app.activity.UnifiedActivityViewModel
import studio.guitarlab.app.backup.BackupSettingsStore
import studio.guitarlab.app.backup.ConfirmedRevisionStore
import java.text.DateFormat
import java.util.Date
import studio.guitarlab.core.model.GuitarProject
import studio.guitarlab.core.project.BackupRevisionIdentity
import studio.guitarlab.core.project.ProjectContentFilter
import studio.guitarlab.core.project.ProjectLibraryQuery
import studio.guitarlab.core.project.ProjectSampleRateFilter
import studio.guitarlab.core.project.ProjectSortOrder
import studio.guitarlab.core.project.ProjectTemplateFilter
import studio.guitarlab.core.project.ProjectSyncState
import studio.guitarlab.core.project.ProjectSyncStatePolicy
import studio.guitarlab.core.project.UnifiedOperationKind
import studio.guitarlab.core.project.UnifiedOperationRecord

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onNewProject: () -> Unit,
    onOpenProject: (String) -> Unit,
    onSettings: () -> Unit,
    onActivity: () -> Unit = {},
    onBackupProject: (String) -> Unit = {},
    onPrepareProject: (String) -> Unit = {},
    onExportWorkspace: (String) -> Unit = {},
    activityViewModel: UnifiedActivityViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val activityState by activityViewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val backupConnected = BackupSettingsStore(context).snapshot().driveConnected
    val confirmedRevisions = remember(context) { ConfirmedRevisionStore(context) }
    var renameProject by remember { mutableStateOf<GuitarProject?>(null) }
    var deleteProject by remember { mutableStateOf<GuitarProject?>(null) }
    var deleteHasActiveOperations by remember { mutableStateOf(false) }
    var helpDialogVisible by remember { mutableStateOf(false) }
    val projectListState = rememberLazyListState()
    val projectPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> if (uri != null) viewModel.importProject(uri, onOpenProject) }

    LaunchedEffect(state.libraryQuery) {
        if (state.projects.isNotEmpty()) projectListState.scrollToItem(0)
    }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 18.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                    Text("GuitarLab Studio", style = MaterialTheme.typography.headlineLarge)
                    Text("Pratique · grave · compare", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.CenterVertically) {
                    AppIconButton(
                        icon = Icons.Default.Sync,
                        contentDescription = "Atividade",
                        onClick = onActivity,
                        modifier = Modifier.testTag("home-activity"),
                    )
                    AppIconButton(
                        icon = Icons.Default.Info,
                        contentDescription = "Ajuda",
                        onClick = { helpDialogVisible = true },
                        modifier = Modifier.testTag("home-help"),
                    )
                    AppIconButton(
                        icon = Icons.Default.Tune,
                        contentDescription = "Opções",
                        onClick = onSettings,
                        modifier = Modifier.testTag("home-options"),
                    )
                }
            }
            Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp), color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.34f), tonalElevation = 0.dp) {
                BoxWithConstraints(Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 20.dp)) {
                    val compactHome = maxWidth < 600.dp
                    if (compactHome) {
                        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                Text("Seu espaço para tocar e evoluir", style = MaterialTheme.typography.headlineSmall)
                                Text("Abra um projeto e trabalhe direto na música, com timeline, mixer e edição no mesmo fluxo.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                OutlinedButton(onClick = { projectPicker.launch(arrayOf("application/octet-stream", "application/zip", "*/*")) }, enabled = !state.loading && !state.exportBusy) { Icon(Icons.Default.FolderOpen, null); Text("Importar projeto", Modifier.padding(start = 6.dp)) }
                                Button(onClick = onNewProject, enabled = !state.loading && !state.exportBusy) { Icon(Icons.Default.Add, null); Text("Novo projeto", Modifier.padding(start = 6.dp)) }
                            }
                        }
                    } else {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                Text("Seu espaço para tocar e evoluir", style = MaterialTheme.typography.headlineSmall)
                                Text("Abra um projeto e trabalhe direto na música, com timeline, mixer e edição no mesmo fluxo.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Row(Modifier.padding(start = 20.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                OutlinedButton(onClick = { projectPicker.launch(arrayOf("application/octet-stream", "application/zip", "*/*")) }, enabled = !state.loading && !state.exportBusy) { Icon(Icons.Default.FolderOpen, null); Text("Importar projeto", Modifier.padding(start = 6.dp)) }
                                Button(onClick = onNewProject, enabled = !state.loading && !state.exportBusy) { Icon(Icons.Default.Add, null); Text("Novo projeto", Modifier.padding(start = 6.dp)) }
                            }
                        }
                    }
                }
            }
            activityState.active?.let { active ->
                CompactActivityPanel(active, onClick = onActivity)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                    Text("Projetos", style = MaterialTheme.typography.titleLarge)
                    Text("Encontre e organize sua biblioteca", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (state.totalProjects > 0) {
                    val countText = if (state.projects.size == state.totalProjects) {
                        "${state.totalProjects} no total"
                    } else {
                        "${state.projects.size} de ${state.totalProjects} projetos"
                    }
                    Text(countText, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.testTag("home-project-count"))
                }
            }
            ProjectLibraryControls(
                query = state.libraryQuery,
                enabled = !state.loading && !state.exportBusy,
                onSearchChange = viewModel::updateProjectSearch,
                onTemplateFilter = viewModel::setProjectTemplateFilter,
                onContentFilter = viewModel::setProjectContentFilter,
                onSampleRateFilter = viewModel::setProjectSampleRateFilter,
                onSortOrder = viewModel::setProjectSortOrder,
                onClearFilters = viewModel::clearProjectFilters,
            )
            when {
                state.loading -> Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                state.totalProjects == 0 -> EmptyProjectsState(onNewProject, Modifier.weight(1f))
                state.projects.isEmpty() -> FilteredProjectsEmptyState(
                    query = state.libraryQuery,
                    onClear = viewModel::clearProjectSearchAndFilters,
                    modifier = Modifier.weight(1f),
                )
                else -> LazyColumn(
                    modifier = Modifier.weight(1f).testTag("home-projects"),
                    state = projectListState,
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(state.projects, key = { it.id }) { project ->
                        ProjectRow(
                            project = project,
                            onOpen = { onOpenProject(project.id) },
                            onPrepare = { onPrepareProject(project.id) },
                            onStudio = { onOpenProject(project.id) },
                            onExportWorkspace = { onExportWorkspace(project.id) },
                            onRename = { renameProject = project },
                            onBackup = { onBackupProject(project.id) },
                            onDuplicate = { viewModel.duplicateProject(project) },
                            onDelete = {
                                deleteHasActiveOperations = viewModel.hasActiveProjectOperations(project.id)
                                deleteProject = project
                            },
                            syncLabel = projectSyncLabel(
                                project,
                                backupConnected,
                                confirmedRevisions.confirmedRevision(project.id),
                                activityState.records,
                            ),
                        )
                    }
                }
            }
        }
        if (state.exportBusy) Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.scrim.copy(alpha = 0.35f)) { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() } }
    }

    if (helpDialogVisible) {
        GuitarLabUserGuideDialog(onDismiss = { helpDialogVisible = false })
    }

    renameProject?.let { project -> RenameProjectDialog(project.name, { renameProject = null }) { name -> viewModel.renameProject(project.id, name); renameProject = null } }
    deleteProject?.let { project ->
        ProjectDeleteConfirmationDialog(
            projectName = project.name,
            hasActiveOperations = deleteHasActiveOperations,
            onDismiss = { deleteProject = null },
            onConfirm = {
                viewModel.deleteProject(project.id)
                deleteProject = null
            },
        )
    }
}

@Composable
private fun CompactActivityPanel(
    record: studio.guitarlab.core.project.UnifiedOperationRecord,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().testTag("home-active-operation"),
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Atividade em andamento", style = MaterialTheme.typography.labelLarge)
                Text("Ver atividade", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            }
            Text(record.summary, style = MaterialTheme.typography.bodyMedium)
            record.progressPercent?.let { progress ->
                androidx.compose.material3.LinearProgressIndicator(progress = { progress / 100f }, Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
fun ProjectDeleteConfirmationDialog(
    projectName: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    hasActiveOperations: Boolean = false,
) {
    AlertDialog(
        modifier = Modifier.testTag("home-delete-confirmation"),
        onDismissRequest = onDismiss,
        title = { Text("Excluir projeto?") },
        text = {
            Text(
                if (hasActiveOperations) {
                    "O projeto “$projectName” e seus arquivos gerenciados serão excluídos. As operações em andamento serão canceladas e desvinculadas antes da exclusão. Esta ação não pode ser desfeita."
                } else {
                    "O projeto “$projectName” e seus arquivos gerenciados serão excluídos. Esta ação não pode ser desfeita."
                },
            )
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("home-cancel-delete"),
            ) { Text("Cancelar") }
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                modifier = Modifier.testTag("home-confirm-delete"),
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
            ) { Text("Excluir") }
        },
    )
}

@Composable
fun ProjectLibraryControls(
    query: ProjectLibraryQuery,
    enabled: Boolean,
    onSearchChange: (String) -> Unit,
    onTemplateFilter: (ProjectTemplateFilter) -> Unit,
    onContentFilter: (ProjectContentFilter) -> Unit,
    onSampleRateFilter: (ProjectSampleRateFilter) -> Unit,
    onSortOrder: (ProjectSortOrder) -> Unit,
    onClearFilters: () -> Unit,
) {
    var filterMenuOpen by remember { mutableStateOf(false) }
    var sortMenuOpen by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = query.searchText,
                onValueChange = onSearchChange,
                modifier = Modifier.weight(1f).testTag("home-project-search"),
                enabled = enabled,
                singleLine = true,
                label = { Text("Pesquisar projetos") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = if (query.searchText.isNotBlank()) {
                    {
                        IconButton(onClick = { onSearchChange("") }, enabled = enabled) {
                            Icon(Icons.Default.Close, contentDescription = "Limpar pesquisa")
                        }
                    }
                } else null,
            )
            Box {
                AppIconButton(
                    icon = Icons.Default.FilterList,
                    contentDescription = if (query.activeFilterCount == 0) "Filtrar projetos" else "Filtrar projetos, ${query.activeFilterCount} filtros ativos",
                    onClick = { filterMenuOpen = true },
                    modifier = Modifier.testTag("home-project-filter"),
                    enabled = enabled,
                )
                ProjectFilterMenu(
                    expanded = filterMenuOpen,
                    query = query,
                    onDismiss = { filterMenuOpen = false },
                    onTemplateFilter = onTemplateFilter,
                    onContentFilter = onContentFilter,
                    onSampleRateFilter = onSampleRateFilter,
                )
            }
            Box {
                AppIconButton(
                    icon = Icons.Default.Sort,
                    contentDescription = "Ordenar projetos: ${sortOrderLabel(query.sortOrder)}",
                    onClick = { sortMenuOpen = true },
                    modifier = Modifier.testTag("home-project-sort"),
                    enabled = enabled,
                )
                ProjectSortMenu(
                    expanded = sortMenuOpen,
                    selected = query.sortOrder,
                    onDismiss = { sortMenuOpen = false },
                    onSortOrder = onSortOrder,
                )
            }
        }

        if (query.activeFilterCount > 0) {
            Surface(
                modifier = Modifier.fillMaxWidth().testTag("home-active-filters"),
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.28f),
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(start = 12.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Filtros: ${activeFilterSummary(query)}",
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    TextButton(onClick = onClearFilters, enabled = enabled, modifier = Modifier.testTag("home-clear-filters")) {
                        Text("Limpar filtros")
                    }
                }
            }
        }
    }
}

@Composable
private fun ProjectFilterMenu(
    expanded: Boolean,
    query: ProjectLibraryQuery,
    onDismiss: () -> Unit,
    onTemplateFilter: (ProjectTemplateFilter) -> Unit,
    onContentFilter: (ProjectContentFilter) -> Unit,
    onSampleRateFilter: (ProjectSampleRateFilter) -> Unit,
) {
    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
        MenuSectionTitle("Template")
        ProjectTemplateFilter.entries.forEach { option ->
            FilterMenuItem(templateFilterLabel(option), option == query.template) { onTemplateFilter(option) }
        }
        HorizontalDivider()
        MenuSectionTitle("Conteúdo")
        ProjectContentFilter.entries.forEach { option ->
            FilterMenuItem(contentFilterLabel(option), option == query.content) { onContentFilter(option) }
        }
        HorizontalDivider()
        MenuSectionTitle("Sample rate")
        ProjectSampleRateFilter.entries.forEach { option ->
            FilterMenuItem(sampleRateFilterLabel(option), option == query.sampleRate) { onSampleRateFilter(option) }
        }
    }
}

@Composable
private fun ProjectSortMenu(
    expanded: Boolean,
    selected: ProjectSortOrder,
    onDismiss: () -> Unit,
    onSortOrder: (ProjectSortOrder) -> Unit,
) {
    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
        MenuSectionTitle("Ordenar por")
        ProjectSortOrder.entries.forEach { option ->
            DropdownMenuItem(
                text = { Text(sortOrderLabel(option)) },
                onClick = { onSortOrder(option); onDismiss() },
                trailingIcon = if (option == selected) ({ Icon(Icons.Default.Check, contentDescription = null) }) else null,
            )
        }
    }
}

@Composable
private fun FilterMenuItem(label: String, selected: Boolean, onClick: () -> Unit) {
    DropdownMenuItem(
        text = { Text(label) },
        onClick = onClick,
        trailingIcon = if (selected) ({ Icon(Icons.Default.Check, contentDescription = null) }) else null,
    )
}

@Composable
private fun MenuSectionTitle(label: String) {
    Text(
        text = label,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
    )
}

private fun activeFilterSummary(query: ProjectLibraryQuery): String = buildList {
    if (query.template != ProjectTemplateFilter.ALL) add(templateFilterLabel(query.template))
    if (query.content != ProjectContentFilter.ALL) add(contentFilterLabel(query.content))
    if (query.sampleRate != ProjectSampleRateFilter.ALL) add(sampleRateFilterLabel(query.sampleRate))
}.joinToString(" · ")

private fun templateFilterLabel(filter: ProjectTemplateFilter): String = when (filter) {
    ProjectTemplateFilter.ALL -> "Todos os templates"
    ProjectTemplateFilter.GUITAR -> "Guitarra"
    ProjectTemplateFilter.BLANK -> "Vazio"
}

private fun contentFilterLabel(filter: ProjectContentFilter): String = when (filter) {
    ProjectContentFilter.ALL -> "Todo conteúdo"
    ProjectContentFilter.WITH_RECORDINGS -> "Com gravações"
    ProjectContentFilter.WITH_AUDIO -> "Com áudio/clipes"
    ProjectContentFilter.EMPTY -> "Sem clipes"
}

private fun sampleRateFilterLabel(filter: ProjectSampleRateFilter): String = when (filter) {
    ProjectSampleRateFilter.ALL -> "Todos os sample rates"
    ProjectSampleRateFilter.AUTO -> "Auto"
    ProjectSampleRateFilter.HZ_44100 -> "44,1 kHz"
    ProjectSampleRateFilter.HZ_48000 -> "48 kHz"
    ProjectSampleRateFilter.HZ_88200 -> "88,2 kHz"
    ProjectSampleRateFilter.HZ_96000 -> "96 kHz"
}

private fun sortOrderLabel(order: ProjectSortOrder): String = when (order) {
    ProjectSortOrder.UPDATED_DESC -> "Modificados recentemente"
    ProjectSortOrder.UPDATED_ASC -> "Modificados há mais tempo"
    ProjectSortOrder.NAME_ASC -> "Nome A–Z"
    ProjectSortOrder.NAME_DESC -> "Nome Z–A"
    ProjectSortOrder.CREATED_DESC -> "Criados recentemente"
    ProjectSortOrder.CREATED_ASC -> "Criados há mais tempo"
}

@Composable
private fun EmptyProjectsState(onNewProject: () -> Unit, modifier: Modifier = Modifier) {
    ProductEmptyState(
        title = "Nenhum projeto ainda",
        body = "Crie seu primeiro projeto para preparar uma música ou começar diretamente no Studio.",
        modifier = modifier.testTag("home-empty"),
        action = {
            Button(onClick = onNewProject) { Text("Criar primeiro projeto") }
        },
    )
}

@Composable
private fun FilteredProjectsEmptyState(
    query: ProjectLibraryQuery,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val detail = if (query.searchText.isNotBlank()) {
        "A pesquisa ou os filtros atuais não encontraram projetos."
    } else {
        "Os filtros atuais não encontraram projetos."
    }
    ProductEmptyState(
        title = "Nenhum projeto encontrado",
        body = detail,
        modifier = modifier.testTag("home-filtered-empty"),
        action = {
            Button(
                onClick = onClear,
                modifier = Modifier.testTag("home-clear-search-filters"),
            ) {
                Text("Limpar pesquisa e filtros")
            }
        },
    )
}

@Composable
private fun ProjectRow(
    project: GuitarProject,
    onOpen: () -> Unit,
    onPrepare: () -> Unit,
    onStudio: () -> Unit,
    onExportWorkspace: () -> Unit,
    onRename: () -> Unit,
    onBackup: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
    syncLabel: String,
) {
    var menuOpen by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(10.dp)
    Surface(Modifier.fillMaxWidth(), shape = shape, color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.34f), border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.55f))) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Row(
                Modifier.weight(1f).clip(shape).clickable(onClick = onOpen).padding(start = 16.dp, top = 14.dp, bottom = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.primaryContainer) { Icon(Icons.Default.MusicNote, null, Modifier.padding(12.dp)) }
                Column(Modifier.weight(1f).padding(start = 14.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(project.name, style = MaterialTheme.typography.titleMedium)
                    val modified = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(project.updatedAtEpochMs))
                    Text("$modified  ·  ${project.tracks.size} ${if (project.tracks.size == 1) "pista" else "pistas"}  ·  ${project.clips.size} ${if (project.clips.size == 1) "clipe" else "clipes"}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        UnifiedProjectShellPolicy.cardStatus(project).label,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.testTag("project-status-${project.id}"),
                    )
                    Text(
                        syncLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.testTag("project-sync-${project.id}"),
                    )
                }
            }
            Box(Modifier.padding(horizontal = 8.dp)) {
                AppIconButton(icon = Icons.Default.MoreVert, contentDescription = "Mais ações de ${project.name}", onClick = { menuOpen = true })
                DropdownMenu(menuOpen, { menuOpen = false }) {
                    DropdownMenuItem({ Text("Preparar") }, { menuOpen = false; onPrepare() }, leadingIcon = { Icon(Icons.Default.Tune, null) })
                    DropdownMenuItem({ Text("Abrir Studio") }, { menuOpen = false; onStudio() }, leadingIcon = { Icon(Icons.Default.MusicNote, null) })
                    DropdownMenuItem({ Text("Exportar") }, { menuOpen = false; onExportWorkspace() }, modifier = Modifier.testTag("project-export-${project.id}"), leadingIcon = { Icon(Icons.Default.Share, null) })
                    HorizontalDivider()
                    DropdownMenuItem({ Text("Renomear") }, { menuOpen = false; onRename() }, leadingIcon = { Icon(Icons.Default.Edit, null) })
                    DropdownMenuItem({ Text("Backup deste projeto") }, { menuOpen = false; onBackup() }, leadingIcon = { Icon(Icons.Default.CloudUpload, null) })
                    DropdownMenuItem({ Text("Duplicar") }, { menuOpen = false; onDuplicate() }, leadingIcon = { Icon(Icons.Default.ContentCopy, null) })
                    DropdownMenuItem({ Text("Excluir") }, { menuOpen = false; onDelete() }, leadingIcon = { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) })
                }
            }
        }
    }
}

internal fun projectSyncLabel(
    project: GuitarProject,
    driveConnected: Boolean,
    confirmedRevisionId: String?,
    records: List<UnifiedOperationRecord>,
): String {
    val projectOperation = records
        .filter { it.projectId == project.id && it.kind == UnifiedOperationKind.BACKUP }
        .maxByOrNull { it.updatedAtEpochMs }
    val globalActiveOperation = records
        .filter { it.projectId == null && it.kind == UnifiedOperationKind.BACKUP && it.state.isActive }
        .maxByOrNull { it.updatedAtEpochMs }
    val operation = listOfNotNull(projectOperation, globalActiveOperation).maxByOrNull { it.updatedAtEpochMs }
    val syncState = ProjectSyncStatePolicy.derive(
        driveConnected = driveConnected,
        localRevisionId = BackupRevisionIdentity.forProject(project),
        confirmedRevisionId = confirmedRevisionId,
        backupOperation = operation,
    )
    return when (syncState) {
        ProjectSyncState.NOT_CONNECTED -> "Nuvem desconectada · somente local"
        ProjectSyncState.LOCAL_ONLY -> "Somente local"
        ProjectSyncState.PENDING -> "Backup pendente"
        ProjectSyncState.SYNCING -> "Sincronizando…"
        ProjectSyncState.SYNCED -> "Sincronizado"
        ProjectSyncState.ERROR -> "Backup com erro"
        ProjectSyncState.CONFLICT -> "Conflito de backup"
    }
}
