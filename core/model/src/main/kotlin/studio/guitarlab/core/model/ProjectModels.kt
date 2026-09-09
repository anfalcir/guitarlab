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
        TrackRoleDefinition(BACKING, "Base", true, ChannelLayout.STEREO),
        TrackRoleDefinition(REFERENCE_GUITAR, "Guitarra de referência", true, ChannelLayout.MONO),
        TrackRoleDefinition(REFERENCE_GUITAR_L, "Guitarra de referência E", true, ChannelLayout.MONO, -1f),
        TrackRoleDefinition(REFERENCE_GUITAR_R, "Guitarra de referência D", true, ChannelLayout.MONO, 1f),
        TrackRoleDefinition(RECORDED_GUITAR, "Guitarra gravada", true, ChannelLayout.MONO),
        TrackRoleDefinition(RECORDED_GUITAR_L, "Guitarra gravada E", true, ChannelLayout.MONO, -1f),
        TrackRoleDefinition(RECORDED_GUITAR_R, "Guitarra gravada D", true, ChannelLayout.MONO, 1f),
        TrackRoleDefinition(GUITAR, "Guitarra", true, ChannelLayout.MONO),
        TrackRoleDefinition(BASS, "Baixo", true, ChannelLayout.MONO),
        TrackRoleDefinition(DRUMS, "Bateria", true, ChannelLayout.STEREO),
        TrackRoleDefinition(VOCALS, "Voz", true, ChannelLayout.MONO),
        TrackRoleDefinition(CLICK, "Metrônomo", true, ChannelLayout.MONO),
        TrackRoleDefinition(GENERIC, "Genérica", true, ChannelLayout.MONO)
    )
}

@Serializable data class TrackGroup(val id: String, val name: String, val collapsed: Boolean = false, val order: Int)
@Serializable data class AudioTrack(
    val id: String,
    val name: String,
    val groupId: String? = null,
    val roleId: String? = null,
    val roleSource: RoleSource = RoleSource.NONE,
    val channelLayout: ChannelLayout = ChannelLayout.MONO,
    val pan: Float = 0f,
    val gainDb: Float = 0f,
    val muted: Boolean = false,
    val solo: Boolean = false,
    val armed: Boolean = false,
    val order: Int,
    val colorIndex: Int = -1,
)

/**
 * Imported media is copied into project-managed source storage. Neither the external original nor
 * the managed source copy is rewritten by ordinary editing. Trim/move/gain/mute stay as metadata;
 * waveform/proxy/render outputs are separate derived files. sourceUri remains for compatibility
 * with older projects that referenced Android documents directly.
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
    val managedSourcePath: String? = null,
    val managedEditProxyPath: String? = null,
    val originUri: String? = null,
    val sourceFormat: String? = null,
    val sourceSampleRateHz: Int? = null,
    val sourceChannelCount: Int? = null,
    val sourceBitsPerSample: Int? = null,
    val sourceEncoding: String? = null,
    val sourceTotalFrames: Long? = null,
)

@Serializable data class GuitarProject(
    val schemaVersion: Int = CURRENT_PROJECT_SCHEMA_VERSION,
    val id: String,
    val name: String,
    val template: ProjectTemplate,
    val createdAtEpochMs: Long,
    val updatedAtEpochMs: Long,
    val sampleRate: SampleRateConfig = SampleRateConfig(),
    val masterGainDb: Float = 0f,
    val groups: List<TrackGroup> = emptyList(),
    val tracks: List<AudioTrack> = emptyList(),
    val clips: List<AudioClip> = emptyList(),
    val customRoles: List<TrackRoleDefinition> = emptyList(),
)
