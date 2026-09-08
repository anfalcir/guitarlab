package studio.guitarlab.core.model

object RoleResolver {
    fun applyAutomaticRole(track: AudioTrack, roleId: String, channelLayout: ChannelLayout? = null, pan: Float? = null): AudioTrack {
        if (track.roleSource == RoleSource.USER) return track
        return track.copy(roleId = roleId, roleSource = RoleSource.AUTO, channelLayout = channelLayout ?: track.channelLayout, pan = pan ?: track.pan)
    }

    fun applyUserRole(track: AudioTrack, roleId: String?, channelLayout: ChannelLayout? = null, pan: Float? = null): AudioTrack = track.copy(
        roleId = roleId,
        roleSource = if (roleId == null) RoleSource.NONE else RoleSource.USER,
        channelLayout = channelLayout ?: track.channelLayout,
        pan = pan ?: track.pan
    )
}
