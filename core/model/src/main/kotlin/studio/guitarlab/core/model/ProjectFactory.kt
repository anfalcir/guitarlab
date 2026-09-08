package studio.guitarlab.core.model

import java.util.UUID

class ProjectFactory(
    private val idGenerator: () -> String = { UUID.randomUUID().toString() },
    private val clock: () -> Long = { System.currentTimeMillis() }
) {
    fun create(name: String, template: ProjectTemplate): GuitarProject {
        require(name.isNotBlank()) { "Project name must not be blank." }
        val now = clock()
        return when (template) {
            ProjectTemplate.BLANK -> GuitarProject(
                id = idGenerator(),
                name = name.trim(),
                template = template,
                createdAtEpochMs = now,
                updatedAtEpochMs = now
            )

            ProjectTemplate.GUITAR -> createGuitarTemplate(name.trim(), now)
        }
    }

    private fun createGuitarTemplate(name: String, now: Long): GuitarProject {
        val projectId = idGenerator()
        val backingGroup = TrackGroup(idGenerator(), "Backing", order = 0)
        val referenceGroup = TrackGroup(idGenerator(), "Reference Guitars", order = 1)
        val myGuitarsGroup = TrackGroup(idGenerator(), "My Guitars", order = 2)

        val tracks = listOf(
            AudioTrack(id = idGenerator(), name = "Backing Track", groupId = backingGroup.id, roleId = BuiltInRoles.BACKING, roleSource = RoleSource.AUTO, channelLayout = ChannelLayout.STEREO, pan = 0f, order = 0),
            AudioTrack(id = idGenerator(), name = "Guitar L", groupId = referenceGroup.id, roleId = BuiltInRoles.REFERENCE_GUITAR_L, roleSource = RoleSource.AUTO, channelLayout = ChannelLayout.MONO, pan = -1f, order = 1),
            AudioTrack(id = idGenerator(), name = "Guitar R", groupId = referenceGroup.id, roleId = BuiltInRoles.REFERENCE_GUITAR_R, roleSource = RoleSource.AUTO, channelLayout = ChannelLayout.MONO, pan = 1f, order = 2),
            AudioTrack(id = idGenerator(), name = "My Guitar L", groupId = myGuitarsGroup.id, roleId = BuiltInRoles.RECORDED_GUITAR_L, roleSource = RoleSource.AUTO, channelLayout = ChannelLayout.MONO, pan = -1f, order = 3),
            AudioTrack(id = idGenerator(), name = "My Guitar R", groupId = myGuitarsGroup.id, roleId = BuiltInRoles.RECORDED_GUITAR_R, roleSource = RoleSource.AUTO, channelLayout = ChannelLayout.MONO, pan = 1f, order = 4)
        )

        return GuitarProject(
            id = projectId,
            name = name,
            template = ProjectTemplate.GUITAR,
            createdAtEpochMs = now,
            updatedAtEpochMs = now,
            groups = listOf(backingGroup, referenceGroup, myGuitarsGroup),
            tracks = tracks
        )
    }
}
