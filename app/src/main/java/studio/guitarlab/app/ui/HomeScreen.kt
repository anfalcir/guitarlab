package studio.guitarlab.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import studio.guitarlab.core.model.GuitarProject
import java.text.DateFormat
import java.util.Date

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onNewProject: () -> Unit,
    onOpenProject: (String) -> Unit,
    onSettings: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 28.dp, vertical = 22.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text("GuitarLab Studio", style = MaterialTheme.typography.headlineMedium)
                Text("Practice • Record • Compare • Mix", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            OutlinedButton(onClick = onSettings) { Text("Settings") }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = onNewProject) { Text("+ New Project") }
            OutlinedButton(onClick = { }, enabled = false) { Text("Import Project") }
        }

        Text("Recent Projects", style = MaterialTheme.typography.titleMedium)

        when {
            state.loading -> Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            state.projects.isEmpty() -> EmptyProjectsCard(onNewProject)
            else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(state.projects, key = { it.id }) { project ->
                    ProjectCard(
                        project = project,
                        onOpen = { onOpenProject(project.id) },
                        onDuplicate = { viewModel.duplicateProject(project) },
                        onDelete = { viewModel.deleteProject(project.id) }
                    )
                }
            }
        }

        state.error?.let {
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun EmptyProjectsCard(onNewProject: () -> Unit) {
    Surface(shape = RoundedCornerShape(18.dp), tonalElevation = 2.dp, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("No projects yet", style = MaterialTheme.typography.titleMedium)
            Text("Start blank or use the Guitar Template to be ready for backing, reference guitars and double-tracked recording.")
            Spacer(Modifier.height(4.dp))
            Button(onClick = onNewProject) { Text("Create first project") }
        }
    }
}

@Composable
private fun ProjectCard(
    project: GuitarProject,
    onOpen: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit
) {
    var menuOpen by remember { mutableStateOf(false) }
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onOpen),
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 1.dp
    ) {
        Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(project.name, style = MaterialTheme.typography.titleMedium)
                val modified = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
                    .format(Date(project.updatedAtEpochMs))
                Text("$modified • ${project.tracks.size} tracks", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Box {
                Text(
                    text = "•••",
                    modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(10.dp)).clickable { menuOpen = true }.padding(horizontal = 12.dp, vertical = 8.dp)
                )
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(text = { Text("Duplicate") }, onClick = { menuOpen = false; onDuplicate() })
                    DropdownMenuItem(text = { Text("Delete") }, onClick = { menuOpen = false; onDelete() })
                }
            }
        }
    }
}
