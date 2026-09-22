package studio.guitarlab.app.backup

import java.nio.file.Files
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.Test
import studio.guitarlab.core.model.ProjectValidator

class U8mRealDriveAcceptanceTest {
    @Test fun campaignNamespaceIsFailClosed() {
        assertTrue(
            U8mRealDriveAcceptanceContract.isCampaignProjectId(
                "u8m-real-drive-1234567890abcdef1234567890abcdef",
            ),
        )
        assertFalse(U8mRealDriveAcceptanceContract.isCampaignProjectId("real-project"))
        assertFalse(U8mRealDriveAcceptanceContract.isCampaignProjectId("u8m-real-drive-short"))
    }

    @Test fun fixtureIsValidPathAwareAndDeduplicatesDuplicateContent() {
        val root = Files.createTempDirectory("u8m-fixture").toFile()
        try {
            val projectId = "u8m-real-drive-1234567890abcdef1234567890abcdef"
            val created = U8mRealDriveFixture.create(
                root, projectId, "campaign", 10L,
            )
            assertTrue(ProjectValidator.validate(created.project).isEmpty())
            assertEquals(3, created.uniqueMediaHashes.size)
            assertEquals(4, created.project.assets.size)
            assertTrue(U8mRealDriveFixture.verifyFiles(root, created.project))

            val second = U8mRealDriveFixture.addSecondTake(
                root, created.project, "campaign", 20L,
            )
            assertTrue(ProjectValidator.validate(second.project).isEmpty())
            assertEquals(2, second.project.takes.size)
            assertEquals(1, second.project.takes.count { it.active })
            assertTrue(second.newTakeHash !in created.uniqueMediaHashes)
        } finally {
            root.deleteRecursively()
        }
    }

    @Test fun reportContainsEvidenceButNoCredentialFields() {
        val report = U8mRealDriveAcceptanceReport(
            campaignId = "c",
            projectId = "u8m-real-drive-1234567890abcdef1234567890abcdef",
            passed = false,
            phase = "oauth",
            error = "authorization required",
        )
        val json = report.toSanitizedJson()
        assertTrue(json.contains("\"campaignId\""))
        assertFalse(json.contains("access_token", ignoreCase = true))
        assertFalse(json.contains("refresh_token", ignoreCase = true))
        assertFalse(json.contains("Authorization: Bearer", ignoreCase = true))
    }
}
