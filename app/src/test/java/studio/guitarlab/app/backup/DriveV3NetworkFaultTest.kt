package studio.guitarlab.app.backup

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import java.nio.file.Files
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking
import org.junit.Test

class DriveV3NetworkFaultTest {
    @Test fun ordinaryRequestRefreshesRejectedTokenExactlyOnce() = runBlocking {
        val tokenProvider = FakeTokenProvider()
        val factory = ScriptedConnectionFactory(
            Step(code = 401, body = "expired"),
            Step(code = 200, body = "ok"),
        )
        val client = client(tokenProvider, factory)

        val response = client.get("https://example.test/files")

        assertEquals(200, response.code)
        assertEquals("ok", response.body)
        assertEquals(1, tokenProvider.invalidations)
        assertEquals(2, factory.openCount)
    }

    @Test fun retryableHttpMatrixConvergesWithoutChangingPermanentFailures() = runBlocking {
        listOf(429, 500, 502, 503, 504).forEach { status ->
            val delays = mutableListOf<Long>()
            val factory = ScriptedConnectionFactory(
                Step(code = status, body = "transient"),
                Step(code = 200, body = "ok"),
            )
            val client = DriveV3HttpClient(
                tokenProvider = FakeTokenProvider(),
                retryDelay = { delays += it },
                connectionFactory = factory::open,
            )

            assertEquals(200, client.get("https://example.test/$status").code)
            assertEquals(2, factory.openCount, "HTTP $status should retry once")
            assertEquals(1, delays.size, "HTTP $status should back off once")
        }

        val permanent = ScriptedConnectionFactory(
            Step(code = 403, body = "insufficientPermissions"),
        )
        val permanentClient = client(FakeTokenProvider(), permanent)
        val denied = permanentClient.get("https://example.test/403")
        assertEquals(403, denied.code)
        assertEquals(1, permanent.openCount)
    }

    @Test fun quota403RetriesButPermission403DoesNot() = runBlocking {
        val delays = mutableListOf<Long>()
        val factory = ScriptedConnectionFactory(
            Step(code = 403, body = "userRateLimitExceeded"),
            Step(code = 200, body = "ok"),
        )
        val client = DriveV3HttpClient(
            tokenProvider = FakeTokenProvider(),
            retryDelay = { delays += it },
            connectionFactory = factory::open,
        )

        assertEquals(200, client.get("https://example.test/quota").code)
        assertEquals(2, factory.openCount)
        assertEquals(1, delays.size)
    }

    @Test fun timeoutAndOfflineIoAreBoundedAndRetryable() = runBlocking {
        val timeoutFactory = ScriptedConnectionFactory(
            Step(responseError = SocketTimeoutException("timeout")),
            Step(code = 200, body = "ok"),
        )
        val delays = mutableListOf<Long>()
        val timeoutClient = DriveV3HttpClient(
            tokenProvider = FakeTokenProvider(),
            retryDelay = { delays += it },
            connectionFactory = timeoutFactory::open,
        )
        assertEquals(200, timeoutClient.get("https://example.test/timeout").code)
        assertEquals(2, timeoutFactory.openCount)
        assertEquals(1, delays.size)

        val offlineFactory = ScriptedConnectionFactory(
            *Array(DriveRetryPolicy.MAX_ATTEMPTS) {
                Step(responseError = IOException("offline"))
            },
        )
        val offlineClient = client(FakeTokenProvider(), offlineFactory)
        assertFailsWith<IOException> {
            offlineClient.get("https://example.test/offline")
        }
        assertEquals(DriveRetryPolicy.MAX_ATTEMPTS, offlineFactory.openCount)
    }

    @Test fun interruptedDownloadDeletesPartialBytesBeforeRetry() = runBlocking {
        val first = "partial".toByteArray()
        val second = "complete-payload".toByteArray()
        val factory = ScriptedConnectionFactory(
            Step(code = 200, input = ThrowAfterInputStream(first, 3)),
            Step(code = 200, input = ByteArrayInputStream(second)),
        )
        val delays = mutableListOf<Long>()
        val client = DriveV3HttpClient(
            tokenProvider = FakeTokenProvider(),
            retryDelay = { delays += it },
            connectionFactory = factory::open,
        )
        val destination = Files.createTempFile("u8l-download", ".bin").toFile().apply {
            writeText("stale-local-bytes")
        }

        client.download("https://example.test/download", destination)

        assertEquals(second.toList(), destination.readBytes().toList())
        assertEquals(2, factory.openCount)
        assertEquals(1, delays.size)
    }

    @Test fun failedDownloadNeverLeavesPartialDestination() = runBlocking {
        val factory = ScriptedConnectionFactory(
            *Array(DriveRetryPolicy.MAX_ATTEMPTS) {
                Step(
                    code = 200,
                    input = ThrowAfterInputStream("partial".toByteArray(), 2),
                )
            },
        )
        val destination = Files.createTempFile("u8l-failed-download", ".bin").toFile()
        val client = client(FakeTokenProvider(), factory)

        assertFailsWith<IOException> {
            client.download("https://example.test/download", destination)
        }
        assertTrue(!destination.exists())
        assertEquals(DriveRetryPolicy.MAX_ATTEMPTS, factory.openCount)
    }

    @Test fun chunk401InvalidatesTokenButDoesNotBlindlyReplayChunk() = runBlocking {
        val tokenProvider = FakeTokenProvider()
        val factory = ScriptedConnectionFactory(Step(code = 401, body = "expired"))
        val client = client(tokenProvider, factory)
        val payload = Files.createTempFile("u8l-upload", ".bin").toFile().apply {
            writeText("payload")
        }

        val response = client.uploadChunk(
            sessionUrl = "https://example.test/session",
            file = payload,
            start = 0,
            length = payload.length().toInt(),
            totalBytes = payload.length(),
        )

        assertEquals(401, response.code)
        assertEquals(1, tokenProvider.invalidations)
        assertEquals(1, factory.openCount)
    }

    @Test fun malformedDriveJsonAndBlankPaginationTokenFailClosed() {
        assertFailsWith<IllegalStateException> {
            DriveV3Json.parseFile("""{"name":"missing-id"}""")
        }
        assertFailsWith<IllegalArgumentException> {
            DriveV3Json.parsePage("""{"files":[],"nextPageToken":""}""")
        }
        assertFailsWith<Throwable> {
            DriveV3Json.parsePage("""{"files": [""")
        }
    }

    private fun client(
        tokenProvider: FakeTokenProvider,
        factory: ScriptedConnectionFactory,
    ) = DriveV3HttpClient(
        tokenProvider = tokenProvider,
        retryDelay = {},
        connectionFactory = factory::open,
    )

    private class FakeTokenProvider : DriveAccessTokenProvider {
        var invalidations = 0
        var tokenRequests = 0

        override suspend fun accessToken(): String {
            tokenRequests++
            return if (invalidations == 0) "token-old" else "token-refreshed"
        }

        override suspend fun invalidateRejectedToken() {
            invalidations++
        }
    }

    private data class Step(
        val code: Int = 200,
        val body: String = "",
        val headers: Map<String, List<String>> = emptyMap(),
        val responseError: IOException? = null,
        val input: InputStream? = null,
    )

    private class ScriptedConnectionFactory(
        vararg steps: Step,
    ) {
        private val queue = ArrayDeque(steps.toList())
        var openCount = 0
            private set

        fun open(url: String): HttpURLConnection {
            openCount++
            val step = queue.removeFirstOrNull()
                ?: error("No scripted connection left for $url")
            return ScriptedConnection(URL(url), step)
        }
    }

    private class ScriptedConnection(
        url: URL,
        private val step: Step,
    ) : HttpURLConnection(url) {
        private val requestBody = ByteArrayOutputStream()

        override fun connect() = Unit
        override fun disconnect() = Unit
        override fun usingProxy(): Boolean = false

        override fun getResponseCode(): Int {
            step.responseError?.let { throw it }
            return step.code
        }

        override fun getInputStream(): InputStream =
            step.input ?: ByteArrayInputStream(step.body.toByteArray())

        override fun getErrorStream(): InputStream? =
            if (step.code >= 400) ByteArrayInputStream(step.body.toByteArray()) else null

        override fun getOutputStream() = requestBody

        override fun getHeaderFields(): Map<String, List<String>> = step.headers
    }

    private class ThrowAfterInputStream(
        private val bytes: ByteArray,
        private val throwAfterBytes: Int,
    ) : InputStream() {
        private var index = 0

        override fun read(): Int {
            if (index >= throwAfterBytes) throw IOException("connection dropped")
            if (index >= bytes.size) return -1
            return bytes[index++].toInt() and 0xff
        }

        override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
            if (index >= throwAfterBytes) throw IOException("connection dropped")
            if (index >= bytes.size) return -1
            val allowed = minOf(length, throwAfterBytes - index, bytes.size - index)
            if (allowed <= 0) throw IOException("connection dropped")
            bytes.copyInto(buffer, offset, index, index + allowed)
            index += allowed
            return allowed
        }
    }
}
