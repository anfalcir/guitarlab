package studio.guitarlab.core.project

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import studio.guitarlab.core.model.AudioClip
import studio.guitarlab.core.model.AudioTrack
import studio.guitarlab.core.model.GuitarProject
import studio.guitarlab.core.model.ProjectTemplate
import studio.guitarlab.core.model.SampleRateConfig
import studio.guitarlab.core.model.SampleRateMode

class RecordingProjectPolicyTest {
    @Test
    fun `recording target requires exactly one armed track`() {
        val none = project(tracks = listOf(track("a", armed = false), track("b", armed = false)))
        val noneError = runCatching { RecordingTargetPolicy.resolve(none) }.exceptionOrNull()
        assertTrue(noneError?.message?.contains("Arme uma pista") == true)

        val many = project(tracks = listOf(track("a", armed = true), track("b", armed = true)))
        val manyError = runCatching { RecordingTargetPolicy.resolve(many) }.exceptionOrNull()
        assertTrue(manyError?.message?.contains("somente uma pista") == true)
    }

    @Test
    fun `recording target uses fixed project rate when configured`() {
        val project = project(
            tracks = listOf(track("guitar", armed = true)),
            sampleRate = SampleRateConfig(mode = SampleRateMode.FIXED, fixedHz = 48_000),
        )
        val target = RecordingTargetPolicy.resolve(project)
        assertEquals("guitar", target.trackId)
        assertEquals(48_000, target.preferredSampleRateHz)
    }

    @Test
    fun `recording target derives existing source rate in auto mode`() {
        val project = project(
            tracks = listOf(track("guitar", armed = true)),
            clips = listOf(sourceClip(trackId = "guitar", sampleRateHz = 44_100)),
        )
        assertEquals(44_100, RecordingTargetPolicy.resolve(project).preferredSampleRateHz)
    }

    @Test
    fun `recorded take integration is metadata only and keeps timeline start`() {
        val original = project(tracks = listOf(track("guitar", armed = true)))
        val saved = RecordedTakeProjectIntegrator.integrate(
            project = original,
            targetTrackId = "guitar",
            take = RecordedTakeMetadata(
                clipId = "take-1",
                displayName = "Take 1",
                managedRelativePath = "media/source/take-1.wav",
                timelineStartFrame = 12_345L,
                sampleRateHz = 48_000,
                channelCount = 1,
                framesCaptured = 96_000L,
            ),
            nowEpochMs = 999L,
        )

        assertEquals(1, saved.clips.size)
        val clip = saved.clips.single()
        assertEquals("guitar", clip.trackId)
        assertEquals(12_345L, clip.startFrame)
        assertEquals(96_000L, clip.lengthFrames)
        assertEquals("managed://media/source/take-1.wav", clip.sourceUri)
        assertEquals("media/source/take-1.wav", clip.managedSourcePath)
        assertEquals("WAV", clip.sourceFormat)
        assertEquals(32, clip.sourceBitsPerSample)
        assertEquals("FLOAT32_LE", clip.sourceEncoding)
        assertEquals(999L, saved.updatedAtEpochMs)
    }

    @Test
    fun `recorded take rejects sample rate mismatch`() {
        val fixed = project(
            tracks = listOf(track("guitar", armed = true)),
            sampleRate = SampleRateConfig(mode = SampleRateMode.FIXED, fixedHz = 48_000),
        )
        val error = runCatching {
            RecordedTakeProjectIntegrator.integrate(
                project = fixed,
                targetTrackId = "guitar",
                take = RecordedTakeMetadata(
                    clipId = "take-1",
                    displayName = "Take 1",
                    managedRelativePath = "media/source/take-1.wav",
                    timelineStartFrame = 0L,
                    sampleRateHz = 44_100,
                    channelCount = 1,
                    framesCaptured = 100L,
                ),
                nowEpochMs = 1L,
            )
        }.exceptionOrNull()
        assertTrue(error?.message?.contains("48000") == true)
    }

    private fun project(
        tracks: List<AudioTrack>,
        clips: List<AudioClip> = emptyList(),
        sampleRate: SampleRateConfig = SampleRateConfig(),
    ) = GuitarProject(
        id = "project",
        name = "Projeto",
        template = ProjectTemplate.BLANK,
        createdAtEpochMs = 1L,
        updatedAtEpochMs = 1L,
        sampleRate = sampleRate,
        tracks = tracks,
        clips = clips,
    )

    private fun track(id: String, armed: Boolean) = AudioTrack(
        id = id,
        name = id,
        armed = armed,
        order = 0,
    )

    private fun sourceClip(trackId: String, sampleRateHz: Int) = AudioClip(
        id = "source",
        trackId = trackId,
        name = "source.wav",
        sourceUri = "managed://media/source/source.wav",
        managedSourcePath = "media/source/source.wav",
        startFrame = 0L,
        lengthFrames = 1_000L,
        sourceSampleRateHz = sampleRateHz,
        sourceChannelCount = 1,
        sourceBitsPerSample = 32,
        sourceEncoding = "FLOAT32_LE",
        sourceFormat = "WAV",
        sourceTotalFrames = 1_000L,
    )
}
