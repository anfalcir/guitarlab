package studio.guitarlab.core.project

import studio.guitarlab.core.model.AudioTrack
import studio.guitarlab.core.model.GuitarProject

object ProjectTrackEditor {
    /** Reorders the complete track lane while preserving stable track IDs and every clip-to-track reference. */
    fun reorderTrack(project: GuitarProject, trackId: String, targetIndex: Int, nowEpochMs: Long): GuitarProject {
        val ordered = project.tracks.sortedWith(compareBy<AudioTrack> { it.order }.thenBy { it.id }).toMutableList()
        require(targetIndex in ordered.indices) { "Target track index is out of bounds." }
        val sourceIndex = ordered.indexOfFirst { it.id == trackId }
        require(sourceIndex >= 0) { "Track '$trackId' not found." }

        val moved = ordered.removeAt(sourceIndex)
        ordered.add(targetIndex, moved)
        val normalized = ordered.mapIndexed { index, track -> track.copy(order = index) }
        return project.copy(tracks = normalized, updatedAtEpochMs = nowEpochMs)
    }
}
