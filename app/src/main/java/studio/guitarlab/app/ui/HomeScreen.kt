package studio.guitarlab.app.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.text.DateFormat
import java.util.Date
import studio.guitarlab.core.model.GuitarProject
import studio.guitarlab.platform.codec.android.MasterExportFormat

@Composable
fun HomeScreen(viewModel: HomeViewModel, onNewProject: () -> Unit, onOpenProject: (String) -> Unit, onSettings: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var renameProject by remember { mutableStateOf<GuitarProject?>(null) }
    var exportProject by remember { mutableStateOf<GuitarProject?>(null) }
    var pendingProjectId by remember { mutableStateOf<String?>(null) }
    val snackbar = remember { SnackbarHostState() }
    val projectPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> if (uri != null) viewModel.importProject(uri, onOpenProject) }
    val projectLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri -> pendingProjectId?.let { id -> if (uri != null) viewModel.saveProjectPackage(id, uri) }; pendingProjectId = null }
    val wavLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("audio/wav")) { uri -> pendingProjectId?.let { id -> if (uri != null) viewModel.exportMaster(id, uri, MasterExportFormat.WAV_FLOAT32) }; pendingProjectId = null }
    val flacLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("audio/flac")) { uri -> pendingProjectId?.let { id -> if (uri != null) viewModel.exportMaster(id, uri, MasterExportFormat.FLAC) }; pendingProjectId = null }
    val mp3Launcher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("audio/mpeg")) { uri -> pendingProjectId?.let { id -> if (uri != null) viewModel.exportMaster(id, uri, MasterExportFormat.MP3) }; pendingProjectId = null }

    LaunchedEffect(state.message, state.error) {
        val message = state.error ?: state.message
        if (message != null) { snackbar.showSnackbar(message); viewModel.clearMessage() }
    }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 18.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                    Text("GuitarLab", style = MaterialTheme.typography.headlineLarge)
                    Text("Pratique · grave · compare", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                AppIconButton(icon = Icons.Default.Tune, contentDescription = "Opções", onClick = onSettings)
            }
            Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp), color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.34f), tonalElevation = 0.dp) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 20.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text("Seu espaço para tocar e evoluir", style = MaterialTheme.typography.headlineSmall)
                        Text("Abra um projeto e trabalhe direto na música, com timeline, mixer e edição no mesmo fluxo.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Row(Modifier.padding(start = 20.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedButton(onClick = { projectPicker.launch(arrayOf("application/octet-stream", "application/zip", "*/*")) }, enabled = !state.loading && !state.exportBusy) { Icon(Icons.Default.FolderOpen, null); Text("Abrir projeto", Modifier.padding(start = 6.dp)) }
                        Button(onClick = onNewProject, enabled = !state.loading && !state.exportBusy) { Icon(Icons.Default.Add, null); Text("Novo projeto", Modifier.padding(start = 6.dp)) }
                    }
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(verticalArrangement = Arrangement.spacedBy(1.dp)) { Text("Projetos recentes", style = MaterialTheme.typography.titleLarge); Text("Continue de onde parou", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                if (state.projects.isNotEmpty()) Text("${state.projects.size} no total", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            when {
                state.loading -> Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                state.projects.isEmpty() -> EmptyProjectsState(onNewProject, Modifier.weight(1f))
                else -> LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(state.projects, key = { it.id }) { project ->
                        ProjectRow(project, { onOpenProject(project.id) }, { renameProject = project }, { exportProject = project }, { viewModel.duplicateProject(project) }, { viewModel.deleteProject(project.id) })
                    }
                }
            }
        }
        SnackbarHost(snackbar, Modifier.align(Alignment.TopCenter).padding(top = 12.dp))
        if (state.exportBusy) Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.scrim.copy(alpha = 0.35f)) { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() } }
    }

    renameProject?.let { project -> RenameProjectDialog(project.name, { renameProject = null }) { name -> viewModel.renameProject(project.id, name); renameProject = null } }
    exportProject?.let { project ->
        val safe = project.name.replace(Regex("[^A-Za-z0-9._ -]"), "_").trim().ifBlank { "GuitarLab" }
        SaveAndExportDialog(project.name, state.exportBusy, { if (!state.exportBusy) exportProject = null },
            onSaveProject = { exportProject = null; pendingProjectId = project.id; projectLauncher.launch("$safe.guitarlab") },
            onWav = { exportProject = null; pendingProjectId = project.id; wavLauncher.launch("$safe-master.wav") },
            onFlac = { exportProject = null; pendingProjectId = project.id; flacLauncher.launch("$safe-master.flac") },
            onMp3 = { exportProject = null; pendingProjectId = project.id; mp3Launcher.launch("$safe-master.mp3") })
    }
}

@Composable private fun EmptyProjectsState(onNewProject: () -> Unit, modifier: Modifier = Modifier) {
    Surface(modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.26f)) {
        Column(Modifier.fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { Text("Nenhum projeto ainda", style = MaterialTheme.typography.titleLarge); Text("Crie um projeto vazio ou abra um arquivo .guitarlab salvo anteriormente.", color = MaterialTheme.colorScheme.onSurfaceVariant); Button(onClick = onNewProject) { Text("Criar primeiro projeto") } }
    }
}

@Composable private fun ProjectRow(project: GuitarProject, onOpen: () -> Unit, onRename: () -> Unit, onExport: () -> Unit, onDuplicate: () -> Unit, onDelete: () -> Unit) {
    var menuOpen by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(16.dp)
    Surface(Modifier.fillMaxWidth().clip(shape).clickable(onClick = onOpen), shape = shape, color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.34f), border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.55f))) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.primaryContainer) { Icon(Icons.Default.MusicNote, null, Modifier.padding(12.dp)) }
            Column(Modifier.weight(1f).padding(start = 14.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(project.name, style = MaterialTheme.typography.titleMedium)
                val modified = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(project.updatedAtEpochMs))
                Text("$modified  ·  ${project.tracks.size} ${if (project.tracks.size == 1) "pista" else "pistas"}  ·  ${project.clips.size} ${if (project.clips.size == 1) "clipe" else "clipes"}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Box {
                AppIconButton(icon = Icons.Default.MoreVert, contentDescription = "Mais ações", onClick = { menuOpen = true })
                DropdownMenu(menuOpen, { menuOpen = false }) {
                    DropdownMenuItem({ Text("Renomear") }, { menuOpen = false; onRename() }, leadingIcon = { Icon(Icons.Default.Edit, null) })
                    DropdownMenuItem({ Text("Salvar e exportar") }, { menuOpen = false; onExport() }, leadingIcon = { Icon(Icons.Default.Share, null) })
                    DropdownMenuItem({ Text("Duplicar") }, { menuOpen = false; onDuplicate() }, leadingIcon = { Icon(Icons.Default.ContentCopy, null) })
                    DropdownMenuItem({ Text("Excluir") }, { menuOpen = false; onDelete() }, leadingIcon = { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) })
                }
            }
        }
    }
}
