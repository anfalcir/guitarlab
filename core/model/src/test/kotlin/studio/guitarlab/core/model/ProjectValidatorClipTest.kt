package studio.guitarlab.core.model

import kotlin.test.Test
import kotlin.test.assertTrue

class ProjectValidatorClipTest {
    private fun baseProject(clips: List<AudioClip>): GuitarProject = GuitarProject(
        id = "project-1",
        name = "Test",
        template = ProjectTemplate.BLANK,
        createdAtEpochMs = 1,
        updatedAtEpochMs = 1,
        tracks = listOf(AudioTrack(id = "track-1", name = "Guitar", order = 0)),
        clips = clips,
    )

    @Test
    fun validClipPassesValidation() {
        val issues = ProjectValidator.validate(
            baseProject(
                listOf(
                    AudioClip(
                        id = "clip-1",
                        trackId = "track-1",
                        name = "Take 1",
                        sourceUri = "content://audio/take1.wav",
                        startFrame = 0,
                        lengthFrames = 48_000,
                        sourceFormat = "WAV",
                        sourceSampleRateHz = 48_000,
                        sourceChannelCount = 2,
                        sourceBitsPerSample = 24,
                        sourceEncoding = "PCM_S24_LE",
                    )
                )
            )
        )
        assertTrue(issues.none { it.code.startsWith("clip.") })
    }

    @Test
    fun clipRejectsMissingTrackAndInvalidFrameBounds() {
        val issues = ProjectValidator.validate(
            baseProject(
                listOf(
                    AudioClip(
                        id = "clip-1",
                        trackId = "missing",
                        name = "Bad clip",
                        sourceUri = "content://audio/bad.wav",
                        startFrame = -1,
                        sourceStartFrame = -2,
                        lengthFrames = 0,
                    )
                )
            )
        )
        val codes = issues.map { it.code }.toSet()
        assertTrue("clip.track.missing" in codes)
        assertTrue("clip.start.negative" in codes)
        assertTrue("clip.source-start.negative" in codes)
        assertTrue("clip.length.invalid" in codes)
    }

    @Test
    fun clipRejectsInvalidKnownSourceMetadata() {
        val issues = ProjectValidator.validate(
            baseProject(
                listOf(
                    AudioClip(
                        id = "clip-1",
                        trackId = "track-1",
                        name = "Bad metadata",
                        sourceUri = "content://audio/bad.wav",
                        startFrame = 0,
                        lengthFrames = 100,
                        sourceSampleRateHz = 0,
                        sourceChannelCount = 0,
                        sourceBitsPerSample = -1,
                    )
                )
            )
        )
        val codes = issues.map { it.code }.toSet()
        assertTrue("clip.source-rate.invalid" in codes)
        assertTrue("clip.source-channels.invalid" in codes)
        assertTrue("clip.source-bits.invalid" in codes)
    }

    @Test
    fun duplicateClipIdsAreRejected() {
        val clip = AudioClip(
            id = "clip-1",
            trackId = "track-1",
            name = "Take",
            sourceUri = "content://audio/take.wav",
            startFrame = 0,
            lengthFrames = 100,
        )
        val issues = ProjectValidator.validate(baseProject(listOf(clip, clip.copy(name = "Take 2"))))
        assertTrue(issues.any { it.code == "clip.id.duplicate" })
    }
}
