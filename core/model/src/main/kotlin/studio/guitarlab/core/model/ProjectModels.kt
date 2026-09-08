package studio.guitarlab.core.model

import kotlinx.serialization.Serializable

const val CURRENT_PROJECT_SCHEMA_VERSION: Int = 1

@Serializable enum class ProjectTemplate { BLANK, GUITAR }
@Serializable enum class RoleSource { NONE, AUTO, USER }
@Serializable enum class ChannelLayout { MONO, STEREO }
@Serializable enum class SampleRateMode { AUTO, FIXED }

@Serializable data class SampleRateConfig(val mode: SampleRateMode = SampleRateMode.AUTO, val fixedHz: Int? = null)
@Serializable data class TrackRoleDefinition(val id: String, val name: String, val builtIn: Boolean, val defaultChannelLayout: ChannelLayout, val defaultPan: Float = 0f)

object BuiltInRoles {
    const val BACKING = "builtin.backing"
    const val REFERENCE_GUITAR = "builtin.reference-guitar"
    const val REFERENCE_GUITAR_L = "builtin.reference-guitar-l"
    const val REFERENCE_GUITAR_R = "builtin.reference-guitar-r"
    const val RECORDED_GUITAR = "builtin.recorded-guitar"
    const val RECORDED_GUITAR_L = "builtin.recorded-guitar-l"
    const val RECORDED_GUITAR_R = "builtin.recorded-guitar-r"
    const val GUITAR = "builtin.guitar"
    const val BASS = "builtin.bass"
    const val DRUMS = "builtin.drums"
    const val VOCALS = "builtin.vocals"
    const val CLICK = "builtin.click"
    const val GENERIC = "builtin.generic"

    val definitions = listOf(
        TrackRoleDefinition(BACKING, "Backing Track", true, ChannelLayout.STEREO),
        TrackRoleDefinition(REFERENCE_GUITAR, "Reference Guitar", true, ChannelLayout.MONO),
        TrackRoleDefinition(REFERENCE_GUITAR_L, "Reference Guitar L", true, ChannelLayout.MONO, -1f),
        TrackRoleDefinition(REFERENCE_GUITAR_R, "Reference Guitar R", true, ChannelLayout.MONO, 1f),
        TrackRoleDefinition(RECORDED_GUITAR, "Recorded Guitar", true, ChannelLayout.MONO),
        TrackRoleDefinition(RECORDED_GUITAR_L, "Recorded Guitar L", true, ChannelLayout.MONO, -1f),
        TrackRoleDefinition(RECORDED_GUITAR_R, "Recorded Guitar R", true, ChannelLayout.MONO, 1f),
        TrackRoleDefinition(GUITAR, "Guitar", true, ChannelLayout.MONO),
        TrackRoleDefinition(BASS, "Bass", true, ChannelLayout.MONO),
        TrackRoleDefinition(DRUMS, "Drums", true, ChannelLayout.STEREO),
        TrackRoleDefinition(VOCALS, "Vocals", true, ChannelLayout.MONO),
        TrackRoleDefinition(CLICK, "Click", true, ChannelLayout.MONO),
        TrackRoleDefinition(GENERIC, "Generic", true, ChannelLayout.MONO)
    )
}

@Serializable data class TrackGroup(val id: String, val name: String, val collapsed: Boolean = false, val order: Int)
@Serializable data class AudioTrack(val id: String, val name: String, val groupId: String? = null, val roleId: String? = null, val roleSource: RoleSource = RoleSource.NONE, val channelLayout: ChannelLayout = ChannelLayout.MONO, val pan: Float = 0f, val gainDb: Float = 0f, val muted: Boolean = false, val solo: Boolean = false, val armed: Boolean = false, val order: Int)

/**
 * Timeline placement metadata. Audio bytes stay outside the project JSON; sourceUri is a stable
 * document/app URI reference and sourceStartFrame allows non-destructive trims later.
 * Technical source fields are optional for backward-compatible schema evolution and let later
 * transport/resampling checkpoints reason about imported media without re-probing on every load.
 */
@Serializable data class AudioClip(
    val id: String,
    val trackId: String,
    val name: String,
    val sourceUri: String,
    val startFrame: Long,
    val sourceStartFrame: Long = 0,
    val lengthFrames: Long,
    val gainDb: Float = 0f,
    val muted: Boolean = false,
    val sourceFormat: String? = null,
    val sourceSampleRateHz: Int? = null,
    val sourceChannelCount: Int? = null,
    val sourceBitsPerSample: Int? = null,
    val sourceEncoding: String? = null,
)

@Serializable data class GuitarProject(
    val schemaVersion: Int = CURRENT_PROJECT_SCHEMA_VERSION,
    val id: String,
    val name: String,
    val template: ProjectTemplate,
    val createdAtEpochMs: Long,
    val updatedAtEpochMs: Long,
    val sampleRate: SampleRateConfig = SampleRateConfig(),
    val groups: List<TrackGroup> = emptyList(),
    val tracks: List<AudioTrack> = emptyList(),
    val clips: List<AudioClip> = emptyList(),
    val customRoles: List<TrackRoleDefinition> = emptyList(),
)
