package studio.guitarlab.app.backup

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import kotlin.test.assertFailsWith
import kotlinx.coroutines.runBlocking
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

    @Test fun ordinaryRequestsRetryEverySupportedTransientHttpClass() = runBlocking {
        val cases = listOf(
            429 to "",
            500 to "",
            502 to "",
            503 to "",
            504 to "",
            403 to """{"error":{"errors":[{"reason":"rateLimitExceeded"}]}}""",
            403 to """{"error":{"errors":[{"reason":"userRateLimitExceeded"}]}}""",
        )
        cases.forEach { (status, body) ->
            val delays = mutableListOf<Long>()
            val connections = ArrayDeque<HttpURLConnection>().apply {
                add(FakeConnection(status, body))
                add(FakeConnection(200, "{}"))
            }
            val client = DriveV3HttpClient(
                tokenProvider = FakeTokenProvider(),
                retryDelay = { delays += it },
                connectionFactory = { connections.removeFirst() },
            )

            assertEquals(200, client.get("https://drive.test/files").code)
            assertEquals(1, delays.size, "HTTP $status must retry exactly once before success")
            assertTrue(connections.isEmpty())
        }
    }

    @Test fun permanent403DoesNotRetry() = runBlocking {
        val delays = mutableListOf<Long>()
        val connections = ArrayDeque<HttpURLConnection>().apply {
            add(FakeConnection(403, """{"error":{"errors":[{"reason":"insufficientPermissions"}]}}"""))
            add(FakeConnection(200, "{}"))
        }
        val client = DriveV3HttpClient(
            tokenProvider = FakeTokenProvider(),
            retryDelay = { delays += it },
            connectionFactory = { connections.removeFirst() },
        )

        assertEquals(403, client.get("https://drive.test/files").code)
        assertTrue(delays.isEmpty())
        assertEquals(1, connections.size)
    }

    @Test fun offlineAndTimeoutFailuresRetryWithoutBeingClassifiedAsCorruption() = runBlocking {
        listOf(
            IOException("offline"),
            SocketTimeoutException("timeout"),
        ).forEach { failure ->
            val delays = mutableListOf<Long>()
            val connections = ArrayDeque<HttpURLConnection>().apply {
                add(FakeConnection(0, failure = failure))
                add(FakeConnection(200, "{}"))
            }
            val client = DriveV3HttpClient(
                tokenProvider = FakeTokenProvider(),
                retryDelay = { delays += it },
                connectionFactory = { connections.removeFirst() },
            )

            assertEquals(200, client.get("https://drive.test/files").code)
            assertEquals(1, delays.size)
        }
    }

    @Test fun transientRetryBudgetIsBounded() = runBlocking {
        val delays = mutableListOf<Long>()
        val connections = ArrayDeque<HttpURLConnection>().apply {
            repeat(DriveRetryPolicy.MAX_ATTEMPTS) {
                add(FakeConnection(503, "temporarily unavailable"))
            }
        }
        val client = DriveV3HttpClient(
            tokenProvider = FakeTokenProvider(),
            retryDelay = { delays += it },
            connectionFactory = { connections.removeFirst() },
        )

        assertEquals(503, client.get("https://drive.test/files").code)
        assertEquals(DriveRetryPolicy.MAX_ATTEMPTS - 1, delays.size)
        assertTrue(connections.isEmpty())
    }

    @Test fun single401InvalidatesRejectedTokenAndRetriesWithFreshAuthorization() = runBlocking {
        val tokens = FakeTokenProvider()
        val connections = ArrayDeque<HttpURLConnection>().apply {
            add(FakeConnection(401, "expired"))
            add(FakeConnection(200, "{}"))
        }
        val client = DriveV3HttpClient(
            tokenProvider = tokens,
            retryDelay = {},
            connectionFactory = { connections.removeFirst() },
        )

        assertEquals(200, client.get("https://drive.test/files").code)
        assertEquals(1, tokens.invalidations)
        assertEquals(listOf("token-0", "token-1"), tokens.issuedTokens)
    }

    @Test fun repeated401FailsAsAuthorizationRequiredInsteadOfGenericNetworkRetry() = runBlocking {
        val tokens = FakeTokenProvider()
        val connections = ArrayDeque<HttpURLConnection>().apply {
            add(FakeConnection(401, "expired"))
            add(FakeConnection(401, "still expired"))
        }
        val client = DriveV3HttpClient(
            tokenProvider = tokens,
            retryDelay = { error("401 must not enter transient backoff") },
            connectionFactory = { connections.removeFirst() },
        )

        assertFailsWith<DriveAuthorizationRequiredException> {
            client.get("https://drive.test/files")
        }
        assertEquals(1, tokens.invalidations)
        assertTrue(connections.isEmpty())
    }

    @Test fun downloadRetriesAfterMidStreamFailureWithoutKeepingPartialBytes() = runBlocking {
        val delays = mutableListOf<Long>()
        val connections = ArrayDeque<HttpURLConnection>().apply {
            add(
                FakeConnection(
                    200,
                    "partial-corrupt",
                    inputFailureAfterBytes = 3,
                ),
            )
            add(FakeConnection(200, "complete"))
        }
        val destination = java.nio.file.Files.createTempFile("drive-download", ".bin")
            .toFile()
            .apply { writeText("stale") }
        val client = DriveV3HttpClient(
            tokenProvider = FakeTokenProvider(),
            retryDelay = { delays += it },
            connectionFactory = { connections.removeFirst() },
        )

        client.download("https://drive.test/files/object?alt=media", destination)

        assertEquals("complete", destination.readText())
        assertEquals(1, delays.size)
        assertTrue(connections.isEmpty())
    }

    @Test fun repeated401DuringDownloadAlsoRequiresUserAuthorization() = runBlocking {
        val tokens = FakeTokenProvider()
        val connections = ArrayDeque<HttpURLConnection>().apply {
            add(FakeConnection(401, "expired"))
            add(FakeConnection(401, "still expired"))
        }
        val destination = java.nio.file.Files.createTempFile("drive-download-auth", ".bin").toFile()
        val client = DriveV3HttpClient(
            tokenProvider = tokens,
            retryDelay = { error("401 must not use network backoff") },
            connectionFactory = { connections.removeFirst() },
        )

        assertFailsWith<DriveAuthorizationRequiredException> {
            client.download("https://drive.test/files/object?alt=media", destination)
        }
        assertEquals(1, tokens.invalidations)
        assertTrue(!destination.exists() || destination.length() == 0L)
    }

    @Test fun malformedDriveJsonFailsClosed() {
        assertFailsWith<Throwable> {
            DriveV3Json.parseFile("""{"name":"missing-id"}""")
        }
        assertFailsWith<Throwable> {
            DriveV3Json.parsePage("""{"files":[{"id":}]}""")
        }
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

    private class FakeTokenProvider : DriveAccessTokenProvider {
        var invalidations = 0
        val issuedTokens = mutableListOf<String>()

        override suspend fun accessToken(): String =
            "token-$invalidations".also(issuedTokens::add)

        override suspend fun invalidateRejectedToken() {
            invalidations++
        }
    }

    private class FakeConnection(
        private val status: Int,
        private val responseBody: String = "",
        private val failure: IOException? = null,
        private val inputFailureAfterBytes: Int? = null,
    ) : HttpURLConnection(URL("https://drive.test")) {
        override fun connect() = Unit
        override fun disconnect() = Unit
        override fun usingProxy(): Boolean = false

        override fun getResponseCode(): Int {
            failure?.let { throw it }
            return status
        }

        override fun getInputStream(): java.io.InputStream {
            val bytes = responseBody.toByteArray(Charsets.UTF_8)
            val failAt = inputFailureAfterBytes
                ?: return ByteArrayInputStream(bytes)
            return object : java.io.InputStream() {
                private var index = 0

                override fun read(): Int {
                    if (index >= failAt) throw IOException("stream interrupted")
                    if (index >= bytes.size) return -1
                    return bytes[index++].toInt() and 0xff
                }
            }
        }

        override fun getErrorStream() =
            ByteArrayInputStream(responseBody.toByteArray(Charsets.UTF_8))

        override fun getOutputStream() = ByteArrayOutputStream()

        override fun getHeaderFields(): Map<String, List<String>> = emptyMap()
    }

}
