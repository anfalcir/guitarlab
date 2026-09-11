package studio.guitarlab.core.project

import kotlin.test.Test
import kotlin.test.assertEquals
import studio.guitarlab.core.model.ProjectTemplate

class ProjectTrackColorCompatibilityTest {
    @Test
    fun legacyProjectWithoutTrackColorLoadsWithDefault() {
        val legacy = """
            {
              "schemaVersion": 1,
              "id": "legacy-project",
              "name": "Legacy",
              "template": "BLANK",
              "createdAtEpochMs": 1,
              "updatedAtEpochMs": 1,
              "tracks": [
                {
                  "id": "track-1",
                  "name": "Track",
                  "order": 0
                }
              ]
            }
        """.trimIndent()

        val project = ProjectCodec().decode(legacy)

        assertEquals(ProjectTemplate.BLANK, project.template)
        assertEquals(-1, project.tracks.single().colorIndex)
    }

    @Test
    fun explicitTrackColorRoundTrips() {
        val project = ProjectCodec().decode(
            """
                {
                  "schemaVersion": 1,
                  "id": "colored-project",
                  "name": "Colored",
                  "template": "BLANK",
                  "createdAtEpochMs": 1,
                  "updatedAtEpochMs": 1,
                  "tracks": [
                    {
                      "id": "track-1",
                      "name": "Track",
                      "order": 0,
                      "colorIndex": 17
                    }
                  ]
                }
            """.trimIndent()
        )

        val restored = ProjectCodec().decode(ProjectCodec().encode(project))

        assertEquals(17, restored.tracks.single().colorIndex)
    }
}
