package studio.guitarlab.core.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ProjectFactoryTest {
    private var nextId = 0
    private val factory = ProjectFactory(idGenerator = { "id-${nextId++}" }, clock = { 1234L })

    @Test fun blankProjectStartsEmpty() {
        val project = factory.create("Vazio", ProjectTemplate.BLANK)
        assertTrue(project.groups.isEmpty())
        assertTrue(project.tracks.isEmpty())
        assertTrue(ProjectValidator.validate(project).isEmpty())
    }

    @Test fun guitarTemplateHasThreeGroupsAndFiveTracks() {
        val project = factory.create("Estudo", ProjectTemplate.GUITAR)
        assertEquals(listOf("Base", "Guitarras de referência", "Minhas guitarras"), project.groups.sortedBy { it.order }.map { it.name })
        assertEquals(listOf("Base", "Guitarra de referência E", "Guitarra de referência D", "Minha guitarra E", "Minha guitarra D"), project.tracks.sortedBy { it.order }.map { it.name })
        assertTrue(ProjectValidator.validate(project).isEmpty())
    }

    @Test fun doubleTrackingDefaultsAreMonoAndHardPanned() {
        val project = factory.create("Estudo", ProjectTemplate.GUITAR)
        val left = project.tracks.first { it.name == "Minha guitarra E" }
        val right = project.tracks.first { it.name == "Minha guitarra D" }
        assertEquals(ChannelLayout.MONO, left.channelLayout)
        assertEquals(ChannelLayout.MONO, right.channelLayout)
        assertEquals(-1f, left.pan)
        assertEquals(1f, right.pan)
    }
}
