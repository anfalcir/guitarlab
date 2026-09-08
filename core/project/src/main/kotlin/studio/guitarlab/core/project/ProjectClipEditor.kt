package studio.guitarlab.core.project

import studio.guitarlab.core.model.GuitarProject

object ProjectClipEditor {
    fun removeClip(project: GuitarProject, clipId: String, nowEpochMs: Long): GuitarProject {
        require(project.clips.any { it.id == clipId }) { "Clip '$clipId' not found." }
        return project.copy(clips = project.clips.filterNot { it.id == clipId }, updatedAtEpochMs = nowEpochMs)
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

    /** Non-destructive trim: source media is never rewritten. */
    fun trimClip(
        project: GuitarProject,
        clipId: String,
        sourceStartFrame: Long,
        lengthFrames: Long,
        nowEpochMs: Long,
    ): GuitarProject {
        require(sourceStartFrame >= 0) { "Clip source start frame must be non-negative." }
        require(lengthFrames > 0) { "Clip length must be positive." }
        val target = project.clips.firstOrNull { it.id == clipId } ?: error("Clip '$clipId' not found.")
        target.sourceTotalFrames?.let { total ->
            require(sourceStartFrame + lengthFrames <= total) { "Trim exceeds immutable source bounds." }
        }
        return project.copy(
            clips = project.clips.map { clip ->
                if (clip.id == clipId) clip.copy(sourceStartFrame = sourceStartFrame, lengthFrames = lengthFrames) else clip
            },
            updatedAtEpochMs = nowEpochMs,
        )
    }

    /**
     * DAW-style edge trim expressed in timeline frames.
     * Moving the left edge also moves sourceStartFrame by the same delta so audiovisual timing stays stable.
     * The immutable managed source path/URI is intentionally untouched.
     */
    fun trimClipToTimelineEdges(
        project: GuitarProject,
        clipId: String,
        timelineStartFrame: Long,
        timelineEndFrame: Long,
        nowEpochMs: Long,
    ): GuitarProject {
        require(timelineStartFrame >= 0) { "Trim start must be non-negative." }
        require(timelineEndFrame > timelineStartFrame) { "Trim end must be after trim start." }
        val target = project.clips.firstOrNull { it.id == clipId } ?: error("Clip '$clipId' not found.")
        val leftDelta = timelineStartFrame - target.startFrame
        val newSourceStart = target.sourceStartFrame + leftDelta
        val newLength = timelineEndFrame - timelineStartFrame
        require(newSourceStart >= 0) { "Trim would expose audio before immutable source frame zero." }
        target.sourceTotalFrames?.let { total ->
            require(newSourceStart + newLength <= total) { "Trim exceeds immutable source bounds." }
        }
        return project.copy(
            clips = project.clips.map { clip ->
                if (clip.id == clipId) {
                    clip.copy(
                        startFrame = timelineStartFrame,
                        sourceStartFrame = newSourceStart,
                        lengthFrames = newLength,
                    )
                } else clip
            },
            updatedAtEpochMs = nowEpochMs,
        )
    }
}
