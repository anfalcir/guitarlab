package studio.guitarlab.app.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
    var pendingTrackId by remember { mutableStateOf<String?>(null) }
    val wavPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        val trackId = pendingTrackId
        pendingTrackId = null
        if (uri != null && trackId != null) viewModel.importWav(trackId, uri)
    }

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
            state.error != null && state.project == null -> InlineStatus(
                title = "Project could not be opened",
                text = state.error.orEmpty(),
            )
            state.project != null -> ProjectWorkspace(
                project = state.project!!,
                waveforms = state.waveforms,
                importing = state.importing,
                editingClip = state.editingClip,
                importStatus = state.importStatus,
                clipStatus = state.clipStatus,
                error = state.error,
                onImportWav = { trackId ->
                    if (!state.importing && !state.editingClip) {
                        pendingTrackId = trackId
                        wavPicker.launch(arrayOf("audio/wav", "audio/x-wav", "audio/wave", "application/octet-stream"))
                    }
                },
                onToggleClipMuted = viewModel::toggleClipMuted,
                onRemoveClip = viewModel::removeClip,
            )
        }
    }
}

@Composable
private fun ProjectWorkspace(
    project: GuitarProject,
    waveforms: Map<String, List<Float>>,
    importing: Boolean,
    editingClip: Boolean,
    importStatus: String?,
    clipStatus: String?,
    error: String?,
    onImportWav: (String) -> Unit,
    onToggleClipMuted: (String) -> Unit,
    onRemoveClip: (String) -> Unit,
) {
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

    TimelinePreview(project, importing || editingClip, onImportWav)

    importStatus?.let { InlineStatus("Import", it) }
    clipStatus?.let { InlineStatus("Clip", it) }
    error?.let { InlineStatus("Operation failed", it) }

    if (project.clips.isNotEmpty()) {
        ClipManager(
            project = project,
            waveforms = waveforms,
            busy = importing || editingClip,
            onToggleMuted = onToggleClipMuted,
            onRemove = onRemoveClip,
        )
    }

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
        title = "M4 managed media + waveform checkpoint",
        text = "Imports are copied into immutable project-managed source storage. Edits remain metadata-only, while waveform envelopes are cached as disposable derived data and rendered without modifying either source copy.",
    )
}

@Composable
private fun TimelinePreview(
    project: GuitarProject,
    busy: Boolean,
    onImportWav: (String) -> Unit,
) {
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
                    TimelineTrackRow(
                        track = track,
                        clips = project.clips.filter { it.trackId == track.id },
                        busy = busy,
                        onImportWav = { onImportWav(track.id) },
                    )
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
private fun TimelineTrackRow(
    track: AudioTrack,
    clips: List<AudioClip>,
    busy: Boolean,
    onImportWav: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            track.name,
            modifier = Modifier.weight(0.24f),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Box(
            modifier = Modifier
                .weight(0.62f)
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
                            color = if (clip.muted) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.primaryContainer,
                        ) {
                            Text(
                                if (clip.muted) "Muted • ${clip.name}" else clip.name,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.bodySmall,
                                color = if (clip.muted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                        }
                    }
                }
            }
        }
        TextButton(
            onClick = onImportWav,
            enabled = !busy,
            modifier = Modifier.weight(0.14f),
        ) {
            Text(if (busy) "…" else "+ WAV")
        }
    }
}

@Composable
private fun ClipManager(
    project: GuitarProject,
    waveforms: Map<String, List<Float>>,
    busy: Boolean,
    onToggleMuted: (String) -> Unit,
    onRemove: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Clips", style = MaterialTheme.typography.titleLarge)
        project.clips.sortedWith(compareBy<AudioClip> { it.trackId }.thenBy { it.startFrame }).forEach { clip ->
            val trackName = project.tracks.firstOrNull { it.id == clip.trackId }?.name ?: "Missing track"
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.34f),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(clip.name, style = MaterialTheme.typography.bodyLarge)
                        Text(
                            "$trackName • ${clip.sourceFormat ?: "audio"} • ${clip.sourceSampleRateHz?.let { "$it Hz" } ?: "rate n/a"} • ${clip.sourceChannelCount?.let { "${it}ch" } ?: "channels n/a"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            if (clip.managedSourcePath != null) "Managed source • original protected" else "Legacy external reference",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        waveforms[clip.id]?.let { WaveformMini(peaks = it, muted = clip.muted) }
                    }
                    TextButton(onClick = { onToggleMuted(clip.id) }, enabled = !busy) {
                        Text(if (clip.muted) "Unmute" else "Mute")
                    }
                    TextButton(onClick = { onRemove(clip.id) }, enabled = !busy) { Text("Remove") }
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
