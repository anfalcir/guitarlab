package studio.guitarlab.core.project

import kotlin.test.Test
import kotlin.test.assertEquals
import studio.guitarlab.core.model.GuitarProject
import studio.guitarlab.core.model.ProjectTemplate

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
}
