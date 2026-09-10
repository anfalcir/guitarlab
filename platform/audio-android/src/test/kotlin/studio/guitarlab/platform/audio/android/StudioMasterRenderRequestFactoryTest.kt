package studio.guitarlab.platform.audio.android

import java.io.File
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import studio.guitarlab.core.model.AudioClip
import studio.guitarlab.core.model.AudioTrack
import studio.guitarlab.core.model.GuitarProject
import studio.guitarlab.core.model.ProjectTemplate

class StudioMasterRenderRequestFactoryTest {
    @Test fun canonicalRequestAppliesSoloMuteAndPrefersEditingProxy() {
        val project = project(
            tracks = listOf(
                AudioTrack("solo", "Solo", order = 0, solo = true, gainDb = -2f, pan = -.5f),
                AudioTrack("other", "Other", order = 1),
            ),
            clips = listOf(
                clip("kept", "solo", proxy = "proxy.wav", source = "source.wav"),
                clip("clip-muted", "solo", muted = true),
                clip("track-not-solo", "other"),
            ),
        )
        val resolved = mutableListOf<String>()

        val request = StudioMasterRenderRequestFactory.create(project, 48_000) {
            resolved += it
            File(it)
        }

        assertEquals(listOf("proxy.wav"), resolved)
        assertEquals(listOf("proxy"), request.clips.map { it.file.nameWithoutExtension })
        assertEquals(listOf("solo"), request.clips.map { it.trackId })
        assertEquals(listOf("solo"), request.trackMixes.map { it.trackId })
        assertEquals(-2f, request.trackMixes.single().gainDb)
        assertEquals(-.5f, request.trackMixes.single().pan)
        assertEquals(110L, request.projectEndFrame)
    }

    @Test fun rejectsUnconvertedClipIdenticallyForEveryCaller() {
        val project = project(
            tracks = listOf(AudioTrack("t", "Track", order = 0)),
            clips = listOf(clip("wrong-rate", "t", editingRate = 44_100)),
        )

        assertFailsWith<IllegalArgumentException> {
            StudioMasterRenderRequestFactory.create(project, 48_000) { File(it) }
        }
    }

    @Test fun rejectsProjectWithoutAudibleManagedMedia() {
        val project = project(
            tracks = listOf(AudioTrack("t", "Track", order = 0, muted = true)),
            clips = listOf(clip("silent", "t")),
        )
        assertFailsWith<IllegalArgumentException> {
            StudioMasterRenderRequestFactory.create(project, 48_000) { File(it) }
        }
    }

    private fun project(tracks: List<AudioTrack>, clips: List<AudioClip>) = GuitarProject(
        id = "project", name = "Project", template = ProjectTemplate.BLANK,
        createdAtEpochMs = 1, updatedAtEpochMs = 1, tracks = tracks, clips = clips,
        masterGainDb = 1.5f,
    )

    private fun clip(
        id: String,
        trackId: String,
        muted: Boolean = false,
        source: String = "$id.wav",
        proxy: String? = null,
        editingRate: Int = 48_000,
    ) = AudioClip(
        id = id, trackId = trackId, name = id, sourceUri = "",
        startFrame = 10, lengthFrames = 100, muted = muted,
        managedSourcePath = source, managedEditProxyPath = proxy,
        sourceSampleRateHz = 48_000, editingSampleRateHz = editingRate,
    )
}
