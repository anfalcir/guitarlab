package studio.guitarlab.core.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ProjectFactoryTest {
    private var nextId = 0
    private val factory = ProjectFactory(idGenerator = { "id-${nextId++}" }, clock = { 1234L })

    @Test fun blankProjectStartsEmpty() {
        val project = factory.create("Blank", ProjectTemplate.BLANK)
        assertTrue(project.groups.isEmpty()); assertTrue(project.tracks.isEmpty()); assertTrue(ProjectValidator.validate(project).isEmpty())
    }
    @Test fun guitarTemplateHasThreeGroupsAndFiveTracks() {
        val project = factory.create("Practice", ProjectTemplate.GUITAR)
        assertEquals(listOf("Backing", "Reference Guitars", "My Guitars"), project.groups.sortedBy { it.order }.map { it.name })
        assertEquals(listOf("Backing Track", "Guitar L", "Guitar R", "My Guitar L", "My Guitar R"), project.tracks.sortedBy { it.order }.map { it.name })
        assertTrue(ProjectValidator.validate(project).isEmpty())
    }
    @Test fun doubleTrackingDefaultsAreMonoAndHardPanned() {
        val project = factory.create("Practice", ProjectTemplate.GUITAR)
        val left = project.tracks.first { it.name == "My Guitar L" }; val right = project.tracks.first { it.name == "My Guitar R" }
        assertEquals(ChannelLayout.MONO, left.channelLayout); assertEquals(ChannelLayout.MONO, right.channelLayout); assertEquals(-1f, left.pan); assertEquals(1f, right.pan)
    }
}
