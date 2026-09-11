package studio.guitarlab.core.project

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import studio.guitarlab.core.model.AudioClip
import studio.guitarlab.core.model.AudioTrack
import studio.guitarlab.core.model.GuitarProject
import studio.guitarlab.core.model.ProjectTemplate

class ProjectMediaCompatibilityTest {
    private val codec = ProjectCodec()

    @Test
    fun legacyClipWithoutEditProxyDecodesWithNullProxy() {
        val legacy = """
            {
              "schemaVersion": 1,
              "id": "legacy-media",
              "name": "Legacy media project",
              "template": "BLANK",
              "createdAtEpochMs": 1,
              "updatedAtEpochMs": 2,
              "tracks": [
                {
                  "id": "track-1",
                  "name": "Guitar",
                  "order": 0
                }
              ],
              "clips": [
                {
                  "id": "clip-1",
                  "trackId": "track-1",
                  "name": "take.wav",
                  "sourceUri": "content://legacy/take.wav",
                  "startFrame": 0,
                  "lengthFrames": 48000,
                  "managedSourcePath": "media/source/take.wav"
                }
              ]
            }
        """.trimIndent()

        val decoded = codec.decode(legacy)
        val clip = decoded.clips.single()

        assertEquals("media/source/take.wav", clip.managedSourcePath)
        assertNull(clip.managedEditProxyPath)
    }

    @Test
    fun managedSourceAndEditProxyRoundTripIndependently() {
        val project = GuitarProject(
            id = "project-media",
            name = "Media project",
            template = ProjectTemplate.BLANK,
            createdAtEpochMs = 1,
            updatedAtEpochMs = 2,
            tracks = listOf(
                AudioTrack(
                    id = "track-1",
                    name = "Guitar",
                    order = 0,
                ),
            ),
            clips = listOf(
                AudioClip(
                    id = "clip-1",
                    trackId = "track-1",
                    name = "source.flac",
                    sourceUri = "content://source/source.flac",
                    startFrame = 0,
                    lengthFrames = 48000,
                    managedSourcePath = "media/source/source.flac",
                    managedEditProxyPath = "media/proxy/source.wav",
                    sourceFormat = "FLAC",
                    sourceSampleRateHz = 48000,
                    sourceChannelCount = 2,
                    sourceBitsPerSample = 24,
                    sourceEncoding = "PCM",
                    sourceTotalFrames = 48000,
                ),
            ),
        )

        val decoded = codec.decode(codec.encode(project))
        val clip = decoded.clips.single()

        assertEquals("media/source/source.flac", clip.managedSourcePath)
        assertEquals("media/proxy/source.wav", clip.managedEditProxyPath)
        assertEquals("FLAC", clip.sourceFormat)
        assertEquals(24, clip.sourceBitsPerSample)
    }
}
