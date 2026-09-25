package studio.guitarlab.core.project

import studio.guitarlab.core.model.AudioClip
import studio.guitarlab.core.model.BuiltInRoles
import studio.guitarlab.core.model.GuitarProject
import studio.guitarlab.core.model.RecordingTake

/**
 * Recovers take metadata emitted by older unified GuitarLab recording builds before RecordingTake
 * became durable project metadata.
 *
 * Recovery is deliberately narrow: only project-managed WAV files created by the canonical
 * recording-media filename contract (<clipId>-take-<epoch>.wav) on a recorded-guitar role qualify.
 * Imported clips, arbitrary WAV files and non-recorded roles are never promoted heuristically.
 */
object LegacyRecordingTakeRecoveryPolicy {
    private val recordedRoles = setOf(
        BuiltInRoles.RECORDED_GUITAR,
        BuiltInRoles.RECORDED_GUITAR_L,
        BuiltInRoles.RECORDED_GUITAR_R,
    )
    private val recordingName = Regex("""^(.+)-take-(\d{10,})\.wav$""", RegexOption.IGNORE_CASE)

    fun recover(project: GuitarProject): GuitarProject {
        val tracksById = project.tracks.associateBy { it.id }
        val existingTakeIds = project.takes.mapTo(mutableSetOf()) { it.id }
        val recoveredByClipId = linkedMapOf<String, RecordingTake>()

        project.clips.forEach { clip ->
            if (clip.takeId != null) return@forEach
            val track = tracksById[clip.trackId] ?: return@forEach
            if (track.roleId !in recordedRoles) return@forEach
            val createdAt = recordingTimestamp(clip) ?: return@forEach
            val takeId = clip.id.takeUnless(existingTakeIds::contains) ?: return@forEach
            existingTakeIds += takeId
            recoveredByClipId[clip.id] = RecordingTake(
                id = takeId,
                trackId = clip.trackId,
                clipId = clip.id,
                name = clip.name.trim().ifBlank { "Take" }.take(TakeManagementPolicy.MAX_NAME_LENGTH),
                createdAtEpochMs = createdAt,
                active = false,
            )
        }

        if (recoveredByClipId.isEmpty()) return project

        val recovered = project.copy(
            clips = project.clips.map { clip ->
                recoveredByClipId[clip.id]?.let { clip.copy(takeId = it.id) } ?: clip
            },
            takes = project.takes + recoveredByClipId.values,
        )
        return TakeManagementPolicy.normalizeAll(recovered)
    }

    private fun recordingTimestamp(clip: AudioClip): Long? {
        val path = clip.managedSourcePath ?: return null
        if (!path.startsWith("media/source/")) return null
        val fileName = path.substringAfterLast('/')
        val match = recordingName.matchEntire(fileName) ?: return null
        if (match.groupValues[1] != clip.id) return null
        if (clip.sourceUri != "managed://$path") return null
        if (!clip.sourceFormat.equals("WAV", ignoreCase = true)) return null
        return match.groupValues[2].toLongOrNull()
    }
}
