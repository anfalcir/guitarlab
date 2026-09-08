package studio.guitarlab.core.project

import studio.guitarlab.core.model.GuitarProject

object ProjectClipEditor {
    fun removeClip(project: GuitarProject, clipId: String, nowEpochMs: Long): GuitarProject {
        require(project.clips.any { it.id == clipId }) { "Clip '$clipId' not found." }
        return project.copy(
            clips = project.clips.filterNot { it.id == clipId },
            updatedAtEpochMs = nowEpochMs,
        )
    }

    fun setClipMuted(project: GuitarProject, clipId: String, muted: Boolean, nowEpochMs: Long): GuitarProject {
        require(project.clips.any { it.id == clipId }) { "Clip '$clipId' not found." }
        return project.copy(
            clips = project.clips.map { clip -> if (clip.id == clipId) clip.copy(muted = muted) else clip },
            updatedAtEpochMs = nowEpochMs,
        )
    }

    fun moveClip(project: GuitarProject, clipId: String, startFrame: Long, nowEpochMs: Long): GuitarProject {
        require(startFrame >= 0) { "Clip start frame must be non-negative." }
        require(project.clips.any { it.id == clipId }) { "Clip '$clipId' not found." }
        return project.copy(
            clips = project.clips.map { clip -> if (clip.id == clipId) clip.copy(startFrame = startFrame) else clip },
            updatedAtEpochMs = nowEpochMs,
        )
    }
}
