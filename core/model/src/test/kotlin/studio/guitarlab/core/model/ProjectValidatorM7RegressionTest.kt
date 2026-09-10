package studio.guitarlab.core.model

import kotlin.test.Test
import kotlin.test.assertTrue

class ProjectValidatorM7RegressionTest {
    private fun project(clip: AudioClip) = GuitarProject(
        id = "p", name = "P", template = ProjectTemplate.BLANK,
        createdAtEpochMs = 1, updatedAtEpochMs = 1,
        tracks = listOf(AudioTrack("t", "T", order = 0)), clips = listOf(clip),
    )

    private fun clip() = AudioClip(
        id = "c", trackId = "t", name = "C", sourceUri = "managed://media/source/a.wav",
        startFrame = 0, sourceStartFrame = 0, lengthFrames = 100,
        sourceTotalFrames = 100, editingSampleRateHz = 48_000, editingTotalFrames = 100,
    )

    @Test fun rejectsTimelineAndSourceOverflow() {
        assertTrue(ProjectValidator.validate(project(clip().copy(startFrame = Long.MAX_VALUE))).any { it.code == "clip.timeline.overflow" })
        assertTrue(ProjectValidator.validate(project(clip().copy(sourceStartFrame = Long.MAX_VALUE))).any { it.code == "clip.source.overflow" })
    }

    @Test fun rejectsInvalidEditingDomainAndFadeBounds() {
        val issues = ProjectValidator.validate(project(clip().copy(
            editingSampleRateHz = 0, editingTotalFrames = -1, fadeInFrames = 101, fadeOutFrames = -1,
        ))).map { it.code }.toSet()
        assertTrue("clip.editing-rate.invalid" in issues)
        assertTrue("clip.editing-total.invalid" in issues)
        assertTrue("clip.fade.bounds" in issues)
    }

    @Test fun editingProxyDomainIsCanonicalTrimBound() {
        val issues = ProjectValidator.validate(project(clip().copy(
            sourceTotalFrames = 1_000, editingTotalFrames = 100, sourceStartFrame = 80, lengthFrames = 30,
        )))
        assertTrue(issues.any { it.code == "clip.trim.bounds" })
    }
}
