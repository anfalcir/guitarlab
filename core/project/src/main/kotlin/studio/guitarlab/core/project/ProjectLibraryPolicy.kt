package studio.guitarlab.core.project

import java.text.Normalizer
import java.util.Locale
import studio.guitarlab.core.model.GuitarProject
import studio.guitarlab.core.model.ProjectTemplate
import studio.guitarlab.core.model.SampleRateMode

enum class ProjectTemplateFilter { ALL, GUITAR, BLANK }
enum class ProjectContentFilter { ALL, WITH_RECORDINGS, WITH_AUDIO, EMPTY }
enum class ProjectSampleRateFilter(val hz: Int?) {
    ALL(null),
    AUTO(null),
    HZ_44100(44_100),
    HZ_48000(48_000),
    HZ_88200(88_200),
    HZ_96000(96_000),
}

enum class ProjectSortOrder {
    UPDATED_DESC,
    UPDATED_ASC,
    NAME_ASC,
    NAME_DESC,
    CREATED_DESC,
    CREATED_ASC,
}

data class ProjectLibraryQuery(
    val searchText: String = "",
    val template: ProjectTemplateFilter = ProjectTemplateFilter.ALL,
    val content: ProjectContentFilter = ProjectContentFilter.ALL,
    val sampleRate: ProjectSampleRateFilter = ProjectSampleRateFilter.ALL,
    val sortOrder: ProjectSortOrder = ProjectSortOrder.UPDATED_DESC,
) {
    val activeFilterCount: Int
        get() = listOf(
            template != ProjectTemplateFilter.ALL,
            content != ProjectContentFilter.ALL,
            sampleRate != ProjectSampleRateFilter.ALL,
        ).count { it }

    val hasSearchOrFilters: Boolean
        get() = searchText.isNotBlank() || activeFilterCount > 0

    fun clearFilters(): ProjectLibraryQuery = copy(
        template = ProjectTemplateFilter.ALL,
        content = ProjectContentFilter.ALL,
        sampleRate = ProjectSampleRateFilter.ALL,
    )

    fun clearSearchAndFilters(): ProjectLibraryQuery = clearFilters().copy(searchText = "")
}

/**
 * Immutable in-memory index for the Home project library. The normalized name is calculated once
 * when the repository snapshot changes, so typing in search never performs repository I/O and does
 * not repeatedly normalize every project name.
 */
class ProjectLibraryIndex internal constructor(
    private val entries: List<ProjectLibraryEntry>,
) {
    val size: Int get() = entries.size

    fun select(query: ProjectLibraryQuery): List<GuitarProject> {
        val normalizedSearch = ProjectLibraryPolicy.normalizeForSearch(query.searchText.trim())
        return entries.asSequence()
            .filter { normalizedSearch.isEmpty() || it.normalizedName.contains(normalizedSearch) }
            .filter { ProjectLibraryPolicy.matchesTemplate(it.project, query.template) }
            .filter { ProjectLibraryPolicy.matchesContent(it.project, query.content) }
            .filter { ProjectLibraryPolicy.matchesSampleRate(it.project, query.sampleRate) }
            .sortedWith(ProjectLibraryPolicy.comparator(query.sortOrder))
            .map { it.project }
            .toList()
    }
}

internal data class ProjectLibraryEntry(
    val project: GuitarProject,
    val normalizedName: String,
)

/** Pure Home project-library query policy. */
object ProjectLibraryPolicy {
    fun index(projects: List<GuitarProject>): ProjectLibraryIndex = ProjectLibraryIndex(
        projects.map { ProjectLibraryEntry(it, normalizeForSearch(it.name)) },
    )

    /** Convenience path for tests/callers that do not retain an index. */
    fun apply(projects: List<GuitarProject>, query: ProjectLibraryQuery): List<GuitarProject> =
        index(projects).select(query)

    fun normalizeForSearch(value: String): String = Normalizer
        .normalize(value, Normalizer.Form.NFD)
        .replace(COMBINING_MARKS, "")
        .lowercase(Locale.ROOT)
        .trim()

    internal fun matchesTemplate(project: GuitarProject, filter: ProjectTemplateFilter): Boolean = when (filter) {
        ProjectTemplateFilter.ALL -> true
        ProjectTemplateFilter.GUITAR -> project.template == ProjectTemplate.GUITAR
        ProjectTemplateFilter.BLANK -> project.template == ProjectTemplate.BLANK
    }

    internal fun matchesContent(project: GuitarProject, filter: ProjectContentFilter): Boolean = when (filter) {
        ProjectContentFilter.ALL -> true
        ProjectContentFilter.WITH_RECORDINGS -> project.takes.isNotEmpty() || project.clips.any { it.takeId != null }
        ProjectContentFilter.WITH_AUDIO -> project.clips.isNotEmpty()
        ProjectContentFilter.EMPTY -> project.clips.isEmpty()
    }

    internal fun matchesSampleRate(project: GuitarProject, filter: ProjectSampleRateFilter): Boolean = when (filter) {
        ProjectSampleRateFilter.ALL -> true
        ProjectSampleRateFilter.AUTO -> project.sampleRate.mode == SampleRateMode.AUTO
        else -> project.sampleRate.mode == SampleRateMode.FIXED && project.sampleRate.fixedHz == filter.hz
    }

    internal fun comparator(order: ProjectSortOrder): Comparator<ProjectLibraryEntry> {
        val stableName = compareBy<ProjectLibraryEntry>({ it.normalizedName }, { it.project.name }, { it.project.id })
        return when (order) {
            ProjectSortOrder.UPDATED_DESC -> compareByDescending<ProjectLibraryEntry> { it.project.updatedAtEpochMs }
                .then(stableName)
            ProjectSortOrder.UPDATED_ASC -> compareBy<ProjectLibraryEntry> { it.project.updatedAtEpochMs }
                .then(stableName)
            ProjectSortOrder.NAME_ASC -> stableName
            ProjectSortOrder.NAME_DESC -> Comparator { first, second ->
                val normalizedCompare = second.normalizedName.compareTo(first.normalizedName)
                if (normalizedCompare != 0) normalizedCompare
                else {
                    val exactCompare = second.project.name.compareTo(first.project.name)
                    if (exactCompare != 0) exactCompare else first.project.id.compareTo(second.project.id)
                }
            }
            ProjectSortOrder.CREATED_DESC -> compareByDescending<ProjectLibraryEntry> { it.project.createdAtEpochMs }
                .then(stableName)
            ProjectSortOrder.CREATED_ASC -> compareBy<ProjectLibraryEntry> { it.project.createdAtEpochMs }
                .then(stableName)
        }
    }

    private val COMBINING_MARKS = Regex("\\p{M}+")
}
