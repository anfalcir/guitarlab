package studio.guitarlab.core.project

import studio.guitarlab.core.model.GuitarProject
import studio.guitarlab.core.model.RecordingTake

/** Non-destructive take metadata and lineage policy. Audio bytes are never deleted here. */
object TakeManagementPolicy {
    const val MAX_NAME_LENGTH = 48
    const val MAX_NOTE_LENGTH = 160

    fun orderedForTrack(project: GuitarProject, trackId: String): List<RecordingTake> =
        project.takes.filter { it.trackId == trackId }.sortedWith(
            compareByDescending<RecordingTake> { it.active }
                .thenByDescending { it.favorite }
                .thenByDescending { it.createdAtEpochMs }
                .thenBy { it.id }
        )

    fun rename(project: GuitarProject, takeId: String, name: String, nowEpochMs: Long): GuitarProject {
        val clean = name.trim()
        require(clean.length in 1..MAX_NAME_LENGTH) { "O nome do take deve ter entre 1 e $MAX_NAME_LENGTH caracteres." }
        require(project.takes.any { it.id == takeId }) { "Take não encontrado: $takeId" }
        return project.copy(
            takes = project.takes.map { if (it.id == takeId) it.copy(name = clean) else it },
            updatedAtEpochMs = nowEpochMs,
        )
    }

    fun setNote(project: GuitarProject, takeId: String, note: String, nowEpochMs: Long): GuitarProject {
        val clean = note.trim()
        require(clean.length <= MAX_NOTE_LENGTH) { "A nota do take deve ter no máximo $MAX_NOTE_LENGTH caracteres." }
        require(project.takes.any { it.id == takeId }) { "Take não encontrado: $takeId" }
        return project.copy(
            takes = project.takes.map { if (it.id == takeId) it.copy(note = clean) else it },
            updatedAtEpochMs = nowEpochMs,
        )
    }

    fun setFavorite(project: GuitarProject, takeId: String, favorite: Boolean, nowEpochMs: Long): GuitarProject {
        require(project.takes.any { it.id == takeId }) { "Take não encontrado: $takeId" }
        return project.copy(
            takes = project.takes.map { if (it.id == takeId) it.copy(favorite = favorite) else it },
            updatedAtEpochMs = nowEpochMs,
        )
    }

    fun activate(project: GuitarProject, takeId: String, nowEpochMs: Long): GuitarProject =
        ActiveTakePolicy.activate(project, takeId, nowEpochMs)

    /**
     * Applies a persistent, non-destructive synchronization correction to every clip in one take
     * lineage. Positive values advance the take; negative values delay it. The operation applies
     * only the delta from the previously stored correction, so repeated edits never accumulate
     * drift. Source offsets/lengths are untouched. If advancing would cross timeline zero, the
     * operation fails closed rather than silently trimming audio.
     */
    fun setFineAdjustmentFrames(
        project: GuitarProject,
        takeId: String,
        newFineAdjustmentFrames: Long,
        nowEpochMs: Long,
    ): GuitarProject {
        val take = project.takes.firstOrNull { it.id == takeId } ?: error("Take não encontrado: $takeId")
        val lineage = project.clips.filter { it.takeId == takeId }
        require(lineage.isNotEmpty()) { "A take não possui clipes para sincronizar." }
        val delta = try {
            Math.subtractExact(newFineAdjustmentFrames, take.fineAdjustmentFrames)
        } catch (_: ArithmeticException) {
            error("O ajuste solicitado excede o intervalo suportado.")
        }
        if (delta == 0L) return project

        val shiftedById = lineage.associate { clip ->
            val newStart = if (delta > 0L) {
                require(clip.startFrame >= delta) {
                    "Não é possível antecipar esta take além do início da timeline sem cortar áudio."
                }
                clip.startFrame - delta
            } else {
                val delay = try { Math.negateExact(delta) } catch (_: ArithmeticException) {
                    error("O ajuste solicitado excede o intervalo suportado.")
                }
                try { Math.addExact(clip.startFrame, delay) } catch (_: ArithmeticException) {
                    error("O ajuste solicitado excede o limite da timeline.")
                }
            }
            clip.id to clip.copy(startFrame = newStart)
        }
        return project.copy(
            clips = project.clips.map { shiftedById[it.id] ?: it },
            takes = project.takes.map {
                if (it.id == takeId) it.copy(fineAdjustmentFrames = newFineAdjustmentFrames) else it
            },
            updatedAtEpochMs = nowEpochMs,
        )
    }

    /**
     * Removes all split/moved clip descendants carrying this takeId. A deleted active take gets a
     * deterministic fallback. Media files are deliberately retained so Undo/Redo/history/shared
     * references can never be broken by this metadata operation.
     */
    fun delete(project: GuitarProject, takeId: String, nowEpochMs: Long): GuitarProject {
        val selected = project.takes.firstOrNull { it.id == takeId } ?: error("Take não encontrado: $takeId")
        val remainingTrackTakes = project.takes.filter { it.trackId == selected.trackId && it.id != takeId }
        val fallbackId = if (selected.active || remainingTrackTakes.none { it.active }) {
            fallbackCandidate(remainingTrackTakes)?.id
        } else {
            remainingTrackTakes.singleOrNull { it.active }?.id ?: fallbackCandidate(remainingTrackTakes)?.id
        }
        return project.copy(
            takes = project.takes.filterNot { it.id == takeId }.map { take ->
                if (take.trackId == selected.trackId) take.copy(active = take.id == fallbackId) else take
            },
            clips = project.clips.filterNot { it.takeId == takeId },
            updatedAtEpochMs = nowEpochMs,
        )
    }

    fun normalizeAll(project: GuitarProject): GuitarProject {
        var normalized = project
        project.takes.map { it.trackId }.distinct().forEach { trackId ->
            normalized = normalizeTrack(normalized, trackId, project.updatedAtEpochMs)
        }
        return normalized
    }

    /** Repairs legacy zero/multi-active state deterministically without changing unrelated tracks. */
    fun normalizeTrack(project: GuitarProject, trackId: String, nowEpochMs: Long = project.updatedAtEpochMs): GuitarProject {
        val takes = project.takes.filter { it.trackId == trackId }
        if (takes.isEmpty()) return project
        val chosen = takes.singleOrNull { it.active } ?: fallbackCandidate(takes) ?: return project
        val normalized = project.takes.map { take ->
            if (take.trackId == trackId) take.copy(active = take.id == chosen.id) else take
        }
        return if (normalized == project.takes) project else project.copy(takes = normalized, updatedAtEpochMs = nowEpochMs)
    }

    private fun fallbackCandidate(takes: List<RecordingTake>): RecordingTake? =
        takes.sortedWith(
            compareByDescending<RecordingTake> { it.favorite }
                .thenByDescending { it.createdAtEpochMs }
                .thenBy { it.id }
        ).firstOrNull()
}
