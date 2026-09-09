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

    /** Moves clip ownership between lanes without touching or copying its immutable source media. */
    fun moveClipToTrack(project: GuitarProject, clipId: String, targetTrackId: String, nowEpochMs: Long): GuitarProject {
        require(project.tracks.any { it.id == targetTrackId }) { "Target track '$targetTrackId' not found." }
        val target = project.clips.firstOrNull { it.id == clipId } ?: error("Clip '$clipId' not found.")
        if (target.trackId == targetTrackId) return project
        return project.copy(
            clips = project.clips.map { clip -> if (clip.id == clipId) clip.copy(trackId = targetTrackId) else clip },
            updatedAtEpochMs = nowEpochMs,
        )
    }

    fun duplicateClip(
        project: GuitarProject,
        clipId: String,
        newClipId: String,
        startFrame: Long,
        nowEpochMs: Long,
        targetTrackId: String? = null,
    ): GuitarProject {
        require(newClipId.isNotBlank()) { "New clip id must not be blank." }
        require(project.clips.none { it.id == newClipId }) { "Clip '$newClipId' already exists." }
        require(startFrame >= 0) { "Clip start frame must be non-negative." }
        val source = project.clips.firstOrNull { it.id == clipId } ?: error("Clip '$clipId' not found.")
        val destinationTrackId = targetTrackId ?: source.trackId
        require(project.tracks.any { it.id == destinationTrackId }) { "Target track '$destinationTrackId' not found." }
        val duplicate = source.copy(id = newClipId, trackId = destinationTrackId, startFrame = startFrame)
        return project.copy(clips = project.clips + duplicate, updatedAtEpochMs = nowEpochMs)
    }

    fun splitClipAtTimelineFrame(
        project: GuitarProject,
        clipId: String,
        splitFrame: Long,
        newRightClipId: String,
        nowEpochMs: Long,
    ): GuitarProject {
        require(newRightClipId.isNotBlank()) { "New clip id must not be blank." }
        require(project.clips.none { it.id == newRightClipId }) { "Clip '$newRightClipId' already exists." }
        val source = project.clips.firstOrNull { it.id == clipId } ?: error("Clip '$clipId' not found.")
        require(source.startFrame <= Long.MAX_VALUE - source.lengthFrames) { "Clip timeline range overflows." }
        val endFrame = source.startFrame + source.lengthFrames
        require(splitFrame > source.startFrame && splitFrame < endFrame) { "Split frame must be inside the clip." }
        val leftLength = splitFrame - source.startFrame
        val rightLength = endFrame - splitFrame
        val left = source.copy(lengthFrames = leftLength)
        val right = source.copy(
            id = newRightClipId,
            startFrame = splitFrame,
            sourceStartFrame = source.sourceStartFrame + leftLength,
            lengthFrames = rightLength,
        )
        return project.copy(
            clips = project.clips.flatMap { clip -> if (clip.id == clipId) listOf(left, right) else listOf(clip) },
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
                if (clip.id == clipId) clip.copy(
                    startFrame = timelineStartFrame,
                    sourceStartFrame = newSourceStart,
                    lengthFrames = newLength,
                ) else clip
            },
            updatedAtEpochMs = nowEpochMs,
        )
    }
}
