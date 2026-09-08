package studio.guitarlab.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import studio.guitarlab.core.model.AudioClip
import studio.guitarlab.core.model.AudioTrack
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
            .padding(horizontal = 24.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("Studio", style = MaterialTheme.typography.headlineMedium)
                Text(
                    state.project?.name ?: "Project ${projectId.take(8)}…",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            OutlinedButton(onClick = onBack) { Text("Home") }
        }

        when {
            state.loading -> CircularProgressIndicator()
            state.error != null -> InlineStatus(
                title = "Project could not be opened",
                text = state.error.orEmpty(),
            )
            state.project != null -> ProjectWorkspace(project = state.project!!)
        }
    }
}

@Composable
private fun ProjectWorkspace(project: GuitarProject) {
    val sampleRateText = project.sampleRate.fixedHz?.let { "$it Hz" } ?: "Auto"

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        ProjectFact("Template", project.template.name.lowercase().replaceFirstChar { it.uppercase() })
        ProjectFact("Tracks", project.tracks.size.toString())
        ProjectFact("Clips", project.clips.size.toString())
        ProjectFact("Sample rate", sampleRateText)
    }

    TimelinePreview(project)

    Text("Track structure", style = MaterialTheme.typography.titleLarge)

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

    InlineStatus(
        title = "M4 timeline foundation",
        text = "The project now persists non-destructive clip placement metadata. Import, transport and waveform rendering will be unlocked in separate validated checkpoints.",
    )
}

@Composable
private fun TimelinePreview(project: GuitarProject) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Timeline", style = MaterialTheme.typography.titleLarge)
            Text(
                "00:00   00:15   00:30   00:45   01:00",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.28f),
        ) {
            Column(Modifier.padding(vertical = 8.dp)) {
                project.tracks.sortedBy { it.order }.forEach { track ->
                    TimelineTrackRow(track, project.clips.filter { it.trackId == track.id })
                }
                if (project.tracks.isEmpty()) {
                    Text(
                        "No tracks yet",
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun TimelineTrackRow(track: AudioTrack, clips: List<AudioClip>) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            track.name,
            modifier = Modifier.weight(0.28f),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Box(
            modifier = Modifier
                .weight(0.72f)
                .height(34.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.78f)),
        ) {
            if (clips.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxSize().padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    clips.sortedBy { it.startFrame }.take(3).forEach { clip ->
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                        ) {
                            Text(
                                clip.name,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProjectFact(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun GroupLane(group: TrackGroup, tracks: List<Pair<String, String>>) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            group.name,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (tracks.isEmpty()) {
            Text("No tracks", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            tracks.forEach { (name, role) -> TrackLane(name, role) }
        }
    }
}

@Composable
private fun TrackLane(name: String, role: String) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 13.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(name, style = MaterialTheme.typography.bodyLarge)
            Text(role, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun InlineStatus(title: String, text: String) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private fun roleName(roleId: String?): String {
    if (roleId == null) return "No role"
    return BuiltInRoles.definitions.firstOrNull { it.id == roleId }?.name ?: roleId
}
