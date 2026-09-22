package studio.guitarlab.core.project

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import studio.guitarlab.core.model.AudioClip
import studio.guitarlab.core.model.GuitarProject
import studio.guitarlab.core.model.ProjectTemplate
import studio.guitarlab.core.model.RecordingTake
import studio.guitarlab.core.model.SampleRateConfig
import studio.guitarlab.core.model.SampleRateMode

class ProjectLibraryPolicyTest {
    @Test
    fun searchIsCaseAndAccentInsensitive() {
        val projects = listOf(
            project("a", "Canção Árvore"),
            project("b", "METAL Worship"),
            project("c", "Outro"),
        )

        assertEquals(listOf("a"), ids(ProjectLibraryPolicy.apply(projects, ProjectLibraryQuery(searchText = "ARVORE"))))
        assertEquals(listOf("b"), ids(ProjectLibraryPolicy.apply(projects, ProjectLibraryQuery(searchText = "metal worship"))))
    }

    @Test
    fun templateContentAndSampleRateFiltersCompose() {
        val matching = project(
            id = "match",
            name = "Guitar 44",
            template = ProjectTemplate.GUITAR,
            sampleRate = SampleRateConfig(SampleRateMode.FIXED, 44_100),
            recorded = true,
        )
        val projects = listOf(
            matching,
            project("wrong-template", "Blank 44", ProjectTemplate.BLANK, SampleRateConfig(SampleRateMode.FIXED, 44_100), recorded = true),
            project("wrong-rate", "Guitar 48", ProjectTemplate.GUITAR, SampleRateConfig(SampleRateMode.FIXED, 48_000), recorded = true),
            project("no-recording", "Guitar empty", ProjectTemplate.GUITAR, SampleRateConfig(SampleRateMode.FIXED, 44_100)),
        )

        val result = ProjectLibraryPolicy.apply(
            projects,
            ProjectLibraryQuery(
                template = ProjectTemplateFilter.GUITAR,
                content = ProjectContentFilter.WITH_RECORDINGS,
                sampleRate = ProjectSampleRateFilter.HZ_44100,
            ),
        )
        assertEquals(listOf("match"), ids(result))
    }

    @Test
    fun contentFiltersDistinguishAudioRecordingAndEmptyProjects() {
        val recorded = project("recorded", "Recorded", recorded = true)
        val imported = project("audio", "Audio", withAudio = true)
        val empty = project("empty", "Empty")
        val projects = listOf(recorded, imported, empty)

        assertEquals(listOf("recorded"), ids(ProjectLibraryPolicy.apply(projects, ProjectLibraryQuery(content = ProjectContentFilter.WITH_RECORDINGS))))
        assertEquals(setOf("recorded", "audio"), ids(ProjectLibraryPolicy.apply(projects, ProjectLibraryQuery(content = ProjectContentFilter.WITH_AUDIO))).toSet())
        assertEquals(listOf("empty"), ids(ProjectLibraryPolicy.apply(projects, ProjectLibraryQuery(content = ProjectContentFilter.EMPTY))))
    }

    @Test
    fun clipTakeIdCountsAsRecordingEvenWhenLegacyTakeListIsMissing() {
        val project = project("legacy", "Legacy", clipTakeIdOnly = true)
        assertEquals(listOf("legacy"), ids(ProjectLibraryPolicy.apply(listOf(project), ProjectLibraryQuery(content = ProjectContentFilter.WITH_RECORDINGS))))
    }

    @Test
    fun sampleRateFiltersHandleAutoAndAllSupportedFixedRates() {
        val projects = listOf(
            project("auto", "Auto"),
            project("44", "44", sampleRate = SampleRateConfig(SampleRateMode.FIXED, 44_100)),
            project("48", "48", sampleRate = SampleRateConfig(SampleRateMode.FIXED, 48_000)),
            project("88", "88", sampleRate = SampleRateConfig(SampleRateMode.FIXED, 88_200)),
            project("96", "96", sampleRate = SampleRateConfig(SampleRateMode.FIXED, 96_000)),
        )
        assertEquals(listOf("auto"), ids(ProjectLibraryPolicy.apply(projects, ProjectLibraryQuery(sampleRate = ProjectSampleRateFilter.AUTO))))
        assertEquals(listOf("44"), ids(ProjectLibraryPolicy.apply(projects, ProjectLibraryQuery(sampleRate = ProjectSampleRateFilter.HZ_44100))))
        assertEquals(listOf("48"), ids(ProjectLibraryPolicy.apply(projects, ProjectLibraryQuery(sampleRate = ProjectSampleRateFilter.HZ_48000))))
        assertEquals(listOf("88"), ids(ProjectLibraryPolicy.apply(projects, ProjectLibraryQuery(sampleRate = ProjectSampleRateFilter.HZ_88200))))
        assertEquals(listOf("96"), ids(ProjectLibraryPolicy.apply(projects, ProjectLibraryQuery(sampleRate = ProjectSampleRateFilter.HZ_96000))))
    }

    @Test
    fun allSortOrdersAreDeterministic() {
        val projects = listOf(
            project("z", "Beta", created = 30, updated = 10),
            project("b", "Álpha", created = 20, updated = 30),
            project("a", "Alpha", created = 10, updated = 20),
        )
        assertEquals(listOf("b", "a", "z"), ids(apply(projects, ProjectSortOrder.UPDATED_DESC)))
        assertEquals(listOf("z", "a", "b"), ids(apply(projects, ProjectSortOrder.UPDATED_ASC)))
        assertEquals(listOf("a", "b", "z"), ids(apply(projects, ProjectSortOrder.NAME_ASC)))
        assertEquals(listOf("z", "b", "a"), ids(apply(projects, ProjectSortOrder.NAME_DESC)))
        assertEquals(listOf("z", "b", "a"), ids(apply(projects, ProjectSortOrder.CREATED_DESC)))
        assertEquals(listOf("a", "b", "z"), ids(apply(projects, ProjectSortOrder.CREATED_ASC)))
    }

    @Test
    fun timeSortUsesStableNameAndIdTieBreakers() {
        val projects = listOf(
            project("2", "Same", created = 1, updated = 5),
            project("1", "Same", created = 1, updated = 5),
            project("3", "Alpha", created = 1, updated = 5),
        )
        assertEquals(listOf("3", "1", "2"), ids(apply(projects, ProjectSortOrder.UPDATED_DESC)))
    }

    @Test
    fun clearingSearchAndFiltersKeepsChosenSort() {
        val query = ProjectLibraryQuery(
            searchText = "riff",
            template = ProjectTemplateFilter.GUITAR,
            content = ProjectContentFilter.WITH_AUDIO,
            sampleRate = ProjectSampleRateFilter.HZ_48000,
            sortOrder = ProjectSortOrder.NAME_ASC,
        )
        val cleared = query.clearSearchAndFilters()
        assertFalse(cleared.hasSearchOrFilters)
        assertEquals(0, cleared.activeFilterCount)
        assertEquals(ProjectSortOrder.NAME_ASC, cleared.sortOrder)
    }

    @Test
    fun activeFilterMetadataIgnoresSearchAndSort() {
        val searchOnly = ProjectLibraryQuery(searchText = "abc", sortOrder = ProjectSortOrder.NAME_DESC)
        assertEquals(0, searchOnly.activeFilterCount)
        assertTrue(searchOnly.hasSearchOrFilters)
        val filtered = searchOnly.copy(template = ProjectTemplateFilter.BLANK, content = ProjectContentFilter.EMPTY)
        assertEquals(2, filtered.activeFilterCount)
    }

    private fun apply(projects: List<GuitarProject>, order: ProjectSortOrder) =
        ProjectLibraryPolicy.apply(projects, ProjectLibraryQuery(sortOrder = order))

    private fun ids(projects: List<GuitarProject>) = projects.map { it.id }

    private fun project(
        id: String,
        name: String,
        template: ProjectTemplate = ProjectTemplate.BLANK,
        sampleRate: SampleRateConfig = SampleRateConfig(),
        withAudio: Boolean = false,
        recorded: Boolean = false,
        clipTakeIdOnly: Boolean = false,
        created: Long = 1,
        updated: Long = created,
    ): GuitarProject {
        val hasClip = withAudio || recorded || clipTakeIdOnly
        val takeId = if (recorded || clipTakeIdOnly) "take-$id" else null
        val clip = if (hasClip) AudioClip(
            id = "clip-$id",
            trackId = "track-$id",
            name = "Clip",
            sourceUri = "managed://clip.wav",
            startFrame = 0,
            lengthFrames = 10,
            takeId = takeId,
        ) else null
        val take = if (recorded) RecordingTake(
            id = requireNotNull(takeId),
            trackId = "track-$id",
            clipId = "clip-$id",
            name = "Take",
            createdAtEpochMs = created,
        ) else null
        return GuitarProject(
            id = id,
            name = name,
            template = template,
            createdAtEpochMs = created,
            updatedAtEpochMs = updated,
            sampleRate = sampleRate,
            clips = listOfNotNull(clip),
            takes = listOfNotNull(take),
        )
    }
}
