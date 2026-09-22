package studio.guitarlab.app.backup

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DriveV3ProtocolTest {
    @Test fun parsesDriveFileAndCommittedMetadata() {
        val file = DriveV3Json.parseFile(
            """{"id":"f1","name":"P.guitarlab","size":"123","sha256Checksum":"abc","appProperties":{"projectId":"p1","revisionId":"r_1_aaaaaaaaaaaaaaaaaaaaaaaa"}}""",
        )
        assertEquals("f1", file.id)
        assertEquals(123L, file.size)
        assertEquals("abc", file.sha256Checksum)
        assertEquals("p1", file.appProperties["projectId"])
    }

    @Test fun parsesPaginatedFileList() {
        val page = DriveV3Json.parsePage(
            """{"nextPageToken":"n2","files":[{"id":"a"},{"id":"b"}]}""",
        )
        assertEquals("n2", page.nextPageToken)
        assertEquals(listOf("a", "b"), page.files.map { it.id })
    }

    @Test fun retryPolicyIsBoundedAndOnlyRetriesTransientClasses() {
        assertTrue(DriveRetryPolicy.retryableStatus(429))
        assertTrue(DriveRetryPolicy.retryableStatus(503))
        assertTrue(DriveRetryPolicy.retryableStatus(403, "userRateLimitExceeded"))
        assertFalse(DriveRetryPolicy.retryableStatus(400))
        assertFalse(DriveRetryPolicy.retryableStatus(403, "insufficientPermissions"))
        assertEquals(1_000L, DriveRetryPolicy.delayMs(0))
        assertEquals(16_000L, DriveRetryPolicy.delayMs(9))
    }

    @Test fun metadataJsonEscapesUserVisibleNames() {
        val encoded = DriveV3Json.metadata(
            name = "A \"quoted\" project",
            mimeType = "application/octet-stream",
            parents = listOf("root"),
            appProperties = mapOf("projectId" to "p1"),
        )
        val parsed = Json.parseToJsonElement(encoded).jsonObject
        assertEquals("A \"quoted\" project", parsed["name"]?.jsonPrimitive?.content)
    }
}
