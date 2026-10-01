package studio.guitarlab.core.project

import kotlin.test.Test
import kotlin.test.assertEquals
import studio.guitarlab.core.model.AudioClip
import studio.guitarlab.core.model.AudioTrack
import studio.guitarlab.core.model.ChannelLayout
import studio.guitarlab.core.model.GuitarProject
import studio.guitarlab.core.model.RecordingTake
import studio.guitarlab.core.model.ProjectTemplate
import studio.guitarlab.core.model.TrackOutputRoute

class ProjectCodecCompatibilityTest {
    private val codec = ProjectCodec()

    @Test
    fun legacyProjectWithoutMasterGainDefaultsToUnity() {
        val legacy = """
            {
              "schemaVersion": 1,
              "id": "legacy",
              "name": "Legacy project",
              "template": "BLANK",
              "createdAtEpochMs": 1,
              "updatedAtEpochMs": 1
            }
        """.trimIndent()

        assertEquals(0f, codec.decode(legacy).masterGainDb)
    }

    @Test
    fun masterGainRoundTrips() {
        val project = GuitarProject(
            id = "project",
            name = "Mix",
            template = ProjectTemplate.BLANK,
            createdAtEpochMs = 1,
            updatedAtEpochMs = 2,
            masterGainDb = -3.5f,
        )

        assertEquals(-3.5f, codec.decode(codec.encode(project)).masterGainDb)
    }
    @Test
    fun perTakeFineAdjustmentRoundTripsWithProject() {
        val track = AudioTrack(id = "track", name = "Guitar", channelLayout = ChannelLayout.MONO, order = 0)
        val clip = AudioClip(
            id = "clip",
            trackId = track.id,
            name = "Take",
            sourceUri = "managed://media/source/take.wav",
            managedSourcePath = "media/source/take.wav",
            startFrame = 12_000L,
            lengthFrames = 48_000L,
            sourceSampleRateHz = 48_000,
            sourceChannelCount = 1,
            takeId = "take",
        )
        val project = GuitarProject(
            id = "project-take-sync",
            name = "Take sync",
            template = ProjectTemplate.GUITAR,
            createdAtEpochMs = 1,
            updatedAtEpochMs = 2,
            tracks = listOf(track),
            clips = listOf(clip),
            takes = listOf(RecordingTake("take", track.id, clip.id, "Take", 2L, true, fineAdjustmentFrames = 1_200L)),
        )

        val restored = codec.decode(codec.encode(project))
        assertEquals(1_200L, restored.takes.single().fineAdjustmentFrames)
        assertEquals(12_000L, restored.clips.single().startFrame)
    }

    @Test
    fun legacyTrackWithoutOutputRouteDefaultsToMain() {
        val legacy = """
            {
              "schemaVersion": 2,
              "id": "legacy-route",
              "name": "Legacy route",
              "template": "BLANK",
              "createdAtEpochMs": 1,
              "updatedAtEpochMs": 1,
              "tracks": [
                {
                  "id": "track",
                  "name": "Reference",
                  "order": 0
                }
              ]
            }
        """.trimIndent()

        assertEquals(TrackOutputRoute.MAIN, codec.decode(legacy).tracks.single().outputRoute)
    }

    @Test
    fun cueOutputRouteRoundTripsWithoutChangingSchemaCompatibility() {
        val project = GuitarProject(
            id = "cue-route",
            name = "Cue route",
            template = ProjectTemplate.GUITAR,
            createdAtEpochMs = 1,
            updatedAtEpochMs = 2,
            tracks = listOf(
                AudioTrack(
                    id = "reference",
                    name = "Reference",
                    outputRoute = TrackOutputRoute.CUE,
                    order = 0,
                )
            ),
        )

        val restored = codec.decode(codec.encode(project))
        assertEquals(TrackOutputRoute.CUE, restored.tracks.single().outputRoute)
    }

}
