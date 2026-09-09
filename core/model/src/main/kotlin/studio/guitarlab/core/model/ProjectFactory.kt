package studio.guitarlab.core.model

import java.util.UUID

class ProjectFactory(
    private val idGenerator: () -> String = { UUID.randomUUID().toString() },
    private val clock: () -> Long = { System.currentTimeMillis() }
) {
    fun create(name: String, template: ProjectTemplate): GuitarProject {
        require(name.isNotBlank()) { "O nome do projeto não pode ficar vazio." }
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
        val backingGroup = TrackGroup(idGenerator(), "Base", order = 0)
        val referenceGroup = TrackGroup(idGenerator(), "Guitarras de referência", order = 1)
        val myGuitarsGroup = TrackGroup(idGenerator(), "Minhas guitarras", order = 2)

        val tracks = listOf(
            AudioTrack(id = idGenerator(), name = "Base", groupId = backingGroup.id, roleId = BuiltInRoles.BACKING, roleSource = RoleSource.AUTO, channelLayout = ChannelLayout.STEREO, pan = 0f, order = 0, colorIndex = 0),
            AudioTrack(id = idGenerator(), name = "Guitarra Ref. E", groupId = referenceGroup.id, roleId = BuiltInRoles.REFERENCE_GUITAR_L, roleSource = RoleSource.AUTO, channelLayout = ChannelLayout.MONO, pan = -1f, order = 1, colorIndex = 1),
            AudioTrack(id = idGenerator(), name = "Guitarra Ref. D", groupId = referenceGroup.id, roleId = BuiltInRoles.REFERENCE_GUITAR_R, roleSource = RoleSource.AUTO, channelLayout = ChannelLayout.MONO, pan = 1f, order = 2, colorIndex = 2),
            AudioTrack(id = idGenerator(), name = "Minha Guitarra E", groupId = myGuitarsGroup.id, roleId = BuiltInRoles.RECORDED_GUITAR_L, roleSource = RoleSource.AUTO, channelLayout = ChannelLayout.MONO, pan = -1f, order = 3, colorIndex = 5),
            AudioTrack(id = idGenerator(), name = "Minha Guitarra D", groupId = myGuitarsGroup.id, roleId = BuiltInRoles.RECORDED_GUITAR_R, roleSource = RoleSource.AUTO, channelLayout = ChannelLayout.MONO, pan = 1f, order = 4, colorIndex = 7)
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
