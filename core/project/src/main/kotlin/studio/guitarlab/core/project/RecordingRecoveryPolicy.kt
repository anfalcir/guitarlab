package studio.guitarlab.core.project

import java.io.File
import kotlinx.serialization.Serializable
import studio.guitarlab.core.model.GuitarProject

@Serializable
data class RecordingRecoveryMarker(
    val schemaVersion: Int = 1,
    val projectId: String,
    val transactionId: String,
    val suggestedFinalName: String,
    val targetTrackId: String? = null,
    val requestedTimelineStartFrame: Long? = null,
    val finalTimelineStartFrame: Long? = null,
    val finalSourceStartFrame: Long? = null,
    val finalLengthFrames: Long? = null,
    val sampleRateHz: Int? = null,
    val channelCount: Int? = null,
    val framesCaptured: Long? = null,
    val createdAtEpochMs: Long,
)

enum class RecordingRecoveryMediaState {
    TEMPORARY_REPAIRED,
    TEMPORARY_UNSAFE,
    FINALIZED_UNPUBLISHED,
}

data class RecordingRecoveryCandidate(
    val projectId: String,
    val transactionId: String,
    val mediaFile: File,
    val finalFile: File?,
    val relativePath: String?,
    val marker: RecordingRecoveryMarker?,
    val state: RecordingRecoveryMediaState,
    val sampleRateHz: Int?,
    val channelCount: Int?,
    val frames: Long?,
    val safePlayable: Boolean,
    val diagnosticReason: String? = null,
) {
    val approximateDurationSeconds: Double?
        get() = if (sampleRateHz != null && sampleRateHz > 0 && frames != null) frames.toDouble() / sampleRateHz else null
}

enum class RecordingRecoveryPublicationDecision {
    RECOVERABLE,
    ALREADY_PUBLISHED,
    TARGET_TRACK_MISSING,
    UNSAFE_PAYLOAD,
}

object RecordingRecoveryPolicy {
    fun publicationDecision(project: GuitarProject, candidate: RecordingRecoveryCandidate): RecordingRecoveryPublicationDecision {
        if (project.id != candidate.projectId) return RecordingRecoveryPublicationDecision.UNSAFE_PAYLOAD
        if (project.clips.any { it.id == candidate.transactionId } || project.takes.any { it.id == candidate.transactionId }) {
            return RecordingRecoveryPublicationDecision.ALREADY_PUBLISHED
        }
        if (!candidate.safePlayable || candidate.sampleRateHz == null || candidate.channelCount == null || candidate.frames == null) {
            return RecordingRecoveryPublicationDecision.UNSAFE_PAYLOAD
        }
        val targetTrack = candidate.marker?.targetTrackId
        if (targetTrack.isNullOrBlank() || project.tracks.none { it.id == targetTrack }) {
            return RecordingRecoveryPublicationDecision.TARGET_TRACK_MISSING
        }
        return RecordingRecoveryPublicationDecision.RECOVERABLE
    }

    fun timelineStart(candidate: RecordingRecoveryCandidate): Long =
        candidate.marker?.finalTimelineStartFrame
            ?: candidate.marker?.requestedTimelineStartFrame
            ?: 0L

    fun sourceStart(candidate: RecordingRecoveryCandidate): Long =
        candidate.marker?.finalSourceStartFrame?.coerceAtLeast(0L) ?: 0L

    fun lengthFrames(candidate: RecordingRecoveryCandidate): Long {
        val frames = requireNotNull(candidate.frames) { "Recovery candidate has no validated frame count." }
        val sourceStart = sourceStart(candidate).coerceAtMost(frames - 1L)
        val requested = candidate.marker?.finalLengthFrames
        return requested?.coerceAtMost(frames - sourceStart)?.coerceAtLeast(1L) ?: (frames - sourceStart)
    }
}
