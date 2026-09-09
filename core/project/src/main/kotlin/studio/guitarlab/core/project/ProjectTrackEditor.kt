package studio.guitarlab.core.project

import studio.guitarlab.core.model.AudioTrack
import studio.guitarlab.core.model.GuitarProject

object ProjectTrackEditor {
    /** Clears timeline content while preserving the track identity, role, color and mix state. */
    fun clearTrackContents(project: GuitarProject, trackId: String, nowEpochMs: Long): GuitarProject {
        require(project.tracks.any { it.id == trackId }) { "Track '$trackId' not found." }
        return project.copy(
            clips = project.clips.filterNot { it.trackId == trackId },
            updatedAtEpochMs = nowEpochMs,
        )
    }

    /** Reorders the complete track lane while preserving stable track IDs and every clip-to-track reference. */
    fun reorderTrack(project: GuitarProject, trackId: String, targetIndex: Int, nowEpochMs: Long): GuitarProject {
        val ordered = project.tracks.sortedWith(compareBy<AudioTrack> { it.order }.thenBy { it.id }).toMutableList()
        require(targetIndex in ordered.indices) { "Target track index is out of bounds." }
        val sourceIndex = ordered.indexOfFirst { it.id == trackId }
        require(sourceIndex >= 0) { "Track '$trackId' not found." }
        if (sourceIndex == targetIndex) return project

        val moved = ordered.removeAt(sourceIndex)
        ordered.add(targetIndex, moved)
        val normalized = ordered.mapIndexed { index, track -> track.copy(order = index) }
        return project.copy(tracks = normalized, updatedAtEpochMs = nowEpochMs)
    }
}
