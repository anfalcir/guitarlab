package studio.guitarlab.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import studio.guitarlab.core.model.BuiltInRoles
import studio.guitarlab.core.model.GuitarProject
import studio.guitarlab.core.model.TrackGroup

@Composable
fun StudioPlaceholderScreen(
    projectId: String,
    onBack: () -> Unit,
    viewModel: StudioViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsState()
    LaunchedEffect(projectId) { viewModel.load(projectId) }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(28.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text("Studio", style = MaterialTheme.typography.headlineMedium)
                Text(
                    state.project?.name ?: "Project ${projectId.take(8)}…",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            OutlinedButton(onClick = onBack) { Text("Home") }
        }

        when {
            state.loading -> CircularProgressIndicator()
            state.error != null -> StatusCard(
                title = "Project could not be opened",
                text = state.error.orEmpty(),
            )
            state.project != null -> ProjectWorkspace(project = state.project!!)
        }
    }
}

@Composable
private fun ProjectWorkspace(project: GuitarProject) {
    StatusCard(
        title = "M4 project workspace foundation",
        text = "Project persistence, template structure and track lanes are now rendered from the saved project model. Audio clips and transport remain gated until the next M4 checkpoints pass their software and tablet tests.",
    )

    Surface(shape = RoundedCornerShape(18.dp), tonalElevation = 1.dp, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(project.name, style = MaterialTheme.typography.titleLarge)
            Text("Template: ${project.template}")
            Text("Tracks: ${project.tracks.size} • Groups: ${project.groups.size}")
            val sampleRateText = project.sampleRate.fixedHz?.let { "$it Hz" } ?: "Auto"
            Text("Project sample rate: $sampleRateText")
        }
    }

    val groupedTrackIds = mutableSetOf<String>()
    project.groups.sortedBy { it.order }.forEach { group ->
        val tracks = project.tracks.filter { it.groupId == group.id }.sortedBy { it.order }
        groupedTrackIds += tracks.map { it.id }
        GroupLane(group, tracks.map { it.name to roleName(it.roleId) })
    }

    val ungrouped = project.tracks.filter { it.id !in groupedTrackIds }.sortedBy { it.order }
    if (ungrouped.isNotEmpty()) {
        GroupLane(
            group = TrackGroup(id = "ungrouped", name = "Ungrouped", order = Int.MAX_VALUE),
            tracks = ungrouped.map { it.name to roleName(it.roleId) },
        )
    }
}

@Composable
private fun GroupLane(group: TrackGroup, tracks: List<Pair<String, String>>) {
    Surface(shape = RoundedCornerShape(16.dp), tonalElevation = 1.dp, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(group.name, style = MaterialTheme.typography.titleMedium)
            if (tracks.isEmpty()) {
                Text("No tracks", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                tracks.forEach { (name, role) ->
                    Surface(shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                        Row(
                            Modifier.fillMaxWidth().padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(name)
                            Text(role, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusCard(title: String, text: String) {
    Surface(shape = RoundedCornerShape(18.dp), tonalElevation = 1.dp, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            Text(text)
        }
    }
}

private fun roleName(roleId: String?): String {
    if (roleId == null) return "No role"
    return BuiltInRoles.definitions.firstOrNull { it.id == roleId }?.name ?: roleId
}
