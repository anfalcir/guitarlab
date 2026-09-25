package studio.guitarlab.core.project

import studio.guitarlab.core.model.AudioClip
import studio.guitarlab.core.model.GuitarProject
import studio.guitarlab.core.model.RecordingTake

data class StereoSeparationIds(
    val leftClipId: String,
    val rightClipId: String,
    val leftTakeId: String,
    val rightTakeId: String,
)

/**
 * Transactionally replaces one stereo clip with synchronized mono L/R derivatives.
 *
 * The generated proxy frame count is authoritative for the editing domain. Recording-take
 * lineage is split into one valid take per destination track rather than leaving a dangling
 * canonical clip or a single lineage spanning two tracks.
 */
object StereoSeparationProjectPolicy {
    fun separate(
        project: GuitarProject,
        sourceClipId: String,
        leftTrackId: String,
        rightTrackId: String,
        leftProxyPath: String,
        rightProxyPath: String,
        splitTotalFrames: Long,
        splitSampleRateHz: Int,
        ids: StereoSeparationIds,
        nowEpochMs: Long,
    ): GuitarProject {
        require(leftTrackId != rightTrackId) { "Stereo separation requires two distinct destination tracks." }
        require(splitTotalFrames > 0L) { "Stereo separation produced no audio frames." }
        require(splitSampleRateHz > 0) { "Stereo separation produced an invalid sample rate." }
        require(project.tracks.any { it.id == leftTrackId }) { "Left destination track does not exist." }
        require(project.tracks.any { it.id == rightTrackId }) { "Right destination track does not exist." }

        val source = project.clips.firstOrNull { it.id == sourceClipId }
            ?: error("Stereo source clip not found: $sourceClipId")
        require(source.sourceChannelCount == 2) { "Stereo separation requires a two-channel clip." }
        require(source.sourceStartFrame <= splitTotalFrames && source.lengthFrames <= splitTotalFrames - source.sourceStartFrame) {
            "Stereo proxy is shorter than the visible source window."
        }
        require(ids.leftClipId != ids.rightClipId) { "Stereo clip ids must be distinct." }
        require(project.clips.none { it.id == ids.leftClipId || it.id == ids.rightClipId }) {
            "Stereo separation clip id already exists."
        }

        val sourceTake = source.takeId?.let { takeId ->
            requireNotNull(project.takes.firstOrNull { it.id == takeId }) {
                "Stereo source references a missing take: $takeId"
            }.also { take ->
                require(take.trackId == source.trackId) {
                    "Stereo source take lineage is inconsistent."
                }
                val lineage = project.clips.filter { it.takeId == takeId }
                require(lineage.size == 1 && lineage.single().id == source.id && take.clipId == source.id) {
                    "Separe canais apenas antes de dividir temporalmente uma take."
                }
            }
        }

        if (sourceTake != null) {
            require(ids.leftTakeId != ids.rightTakeId) { "Stereo take ids must be distinct." }
            require(project.takes.none {
                it.id != sourceTake.id && (it.id == ids.leftTakeId || it.id == ids.rightTakeId)
            }) { "Stereo separation take id already exists." }
        }

        val leftClip = monoClip(
            source = source,
            id = ids.leftClipId,
            trackId = leftTrackId,
            proxyPath = leftProxyPath,
            suffix = "L",
            takeId = sourceTake?.let { ids.leftTakeId },
            splitTotalFrames = splitTotalFrames,
            splitSampleRateHz = splitSampleRateHz,
        )
        val rightClip = monoClip(
            source = source,
            id = ids.rightClipId,
            trackId = rightTrackId,
            proxyPath = rightProxyPath,
            suffix = "R",
            takeId = sourceTake?.let { ids.rightTakeId },
            splitTotalFrames = splitTotalFrames,
            splitSampleRateHz = splitSampleRateHz,
        )

        var takes = project.takes
        if (sourceTake != null) {
            takes = takes.filterNot { it.id == sourceTake.id }
            if (sourceTake.active) {
                takes = takes.map { take ->
                    if (take.trackId == leftTrackId || take.trackId == rightTrackId) take.copy(active = false) else take
                }
            }
            takes = takes + channelTake(sourceTake, ids.leftTakeId, leftTrackId, leftClip.id, "L") +
                channelTake(sourceTake, ids.rightTakeId, rightTrackId, rightClip.id, "R")
        }

        val replaced = project.copy(
            clips = project.clips.filterNot { it.id == source.id } + leftClip + rightClip,
            takes = takes,
            updatedAtEpochMs = nowEpochMs,
        )
        return TakeManagementPolicy.normalizeAll(replaced)
    }

    private fun monoClip(
        source: AudioClip,
        id: String,
        trackId: String,
        proxyPath: String,
        suffix: String,
        takeId: String?,
        splitTotalFrames: Long,
        splitSampleRateHz: Int,
    ): AudioClip = source.copy(
        id = id,
        trackId = trackId,
        name = "${source.name} · $suffix",
        managedEditProxyPath = proxyPath,
        sourceChannelCount = 1,
        sourceBitsPerSample = 32,
        sourceEncoding = "IEEE_FLOAT · canal $suffix derivado",
        editingSampleRateHz = splitSampleRateHz,
        editingTotalFrames = splitTotalFrames,
        takeId = takeId,
    )

    private fun channelTake(
        source: RecordingTake,
        id: String,
        trackId: String,
        clipId: String,
        suffix: String,
    ): RecordingTake = source.copy(
        id = id,
        trackId = trackId,
        clipId = clipId,
        name = channelTakeName(source.name, suffix),
    )

    private fun channelTakeName(name: String, suffix: String): String {
        val marker = " · $suffix"
        return name.trim().ifBlank { "Take" }
            .take((TakeManagementPolicy.MAX_NAME_LENGTH - marker.length).coerceAtLeast(1))
            .trimEnd() + marker
    }
}
