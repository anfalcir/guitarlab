package studio.guitarlab.core.model

import kotlin.test.Test
import kotlin.test.assertEquals

class RoleResolverTest {
    @Test fun automaticAssignmentNeverOverridesUserRole() {
        val initial = AudioTrack(id = "t", name = "Lead", order = 0)
        val user = RoleResolver.applyUserRole(initial, BuiltInRoles.GUITAR)
        val autoAttempt = RoleResolver.applyAutomaticRole(user, BuiltInRoles.BACKING, ChannelLayout.STEREO)
        assertEquals(BuiltInRoles.GUITAR, autoAttempt.roleId); assertEquals(RoleSource.USER, autoAttempt.roleSource); assertEquals(ChannelLayout.MONO, autoAttempt.channelLayout)
    }
}
