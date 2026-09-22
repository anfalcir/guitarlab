package studio.guitarlab.core.project

import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import studio.guitarlab.core.model.AudioClip
import studio.guitarlab.core.model.AudioTrack
import studio.guitarlab.core.model.ChannelLayout
import studio.guitarlab.core.model.GuitarProject
import studio.guitarlab.core.model.ProjectTemplate
import studio.guitarlab.core.model.RecordingTake
import studio.guitarlab.core.model.SampleRateConfig
import studio.guitarlab.core.model.SampleRateMode

/** H32 release-quality boundary: GuitarLab projects are songs, guaranteed through 10 minutes. */
class SongSessionQualityGateTest {
    @Test
    fun waveformRemainsBoundedAndFrameAccurateAcrossSongMatrix() {
        val rates = listOf(44_100, 48_000, 88_200, 96_000)
        val minutes = listOf(1, 3, 5, 10)
        for (rate in rates) for (durationMinutes in minutes) {
            val totalFrames = Math.multiplyExact(rate.toLong(), durationMinutes.toLong() * 60L)
            val callbacks = if (durationMinutes == 10) 60_000 else 12_000
            val waveform = LiveWaveformAccumulator(maxPoints = 512)
            var captured = 0L
            repeat(callbacks) { index ->
                val remaining = totalFrames - captured
                if (remaining <= 0L) return@repeat
                val nominal = (totalFrames / callbacks).coerceAtLeast(1L)
                val jittered = (nominal + ((index % 7) - 3) * (nominal / 8)).coerceAtLeast(1L)
                captured += jittered.coerceAtMost(remaining)
                waveform.append(captured, if (index % 997 == 0) 1f else 0.21f)
            }
            if (captured < totalFrames) waveform.append(totalFrames, .21f)
            val points = waveform.snapshot()
            assertTrue(points.isNotEmpty(), "$durationMinutes min @ $rate Hz")
            assertTrue(points.size <= 512, "$durationMinutes min @ $rate Hz grew to ${points.size} points")
            assertEquals(totalFrames, waveform.coveredFrames())
            assertEquals(0L, points.first().startFrame)
            assertEquals(totalFrames, points.last().endFrameExclusive)
            points.zipWithNext().forEach { (left, right) -> assertEquals(left.endFrameExclusive, right.startFrame) }
            assertTrue(points.all { it.startFrame >= 0L && it.endFrameExclusive > it.startFrame && it.peak.isFinite() })
        }
    }

    @Test
    fun tenMinuteTakePlacementSurvivesSaveAndReopenExactly() {
        val root = Files.createTempDirectory("guitarlab-h32-song").toFile()
        try {
            val rate = 96_000
            val frames = rate.toLong() * 10L * 60L
            val track = AudioTrack(id = "track", name = "My Guitar", channelLayout = ChannelLayout.MONO, order = 0)
            val clip = AudioClip(
                id = "take-10m", trackId = track.id, name = "10 min", sourceUri = "managed://media/source/take.wav",
                managedSourcePath = "media/source/take.wav", startFrame = 1_234L, sourceStartFrame = 321L,
                lengthFrames = frames - 321L, sourceTotalFrames = frames, sourceSampleRateHz = rate,
                sourceChannelCount = 1, editingSampleRateHz = rate, editingTotalFrames = frames, takeId = "take-10m",
            )
            val project = GuitarProject(
                id = "song", name = "Song", template = ProjectTemplate.GUITAR, createdAtEpochMs = 1L, updatedAtEpochMs = 2L,
                sampleRate = SampleRateConfig(mode = SampleRateMode.FIXED, fixedHz = rate), tracks = listOf(track), clips = listOf(clip),
                takes = listOf(RecordingTake("take-10m", track.id, clip.id, "Take 10 min", 2L, true)),
            )
            // Repository validation must not depend on media byte availability; media resolution is a playback concern.
            val repository = FileProjectRepository(root)
            repository.save(project)
            val reopened = requireNotNull(repository.load(project.id))
            val reopenedClip = reopened.clips.single()
            assertEquals(clip.startFrame, reopenedClip.startFrame)
            assertEquals(clip.sourceStartFrame, reopenedClip.sourceStartFrame)
            assertEquals(clip.lengthFrames, reopenedClip.lengthFrames)
            assertEquals(frames, reopenedClip.sourceTotalFrames)
            assertEquals(1, reopened.takes.count { it.active })
        } finally {
            root.deleteRecursively()
        }
    }
}
