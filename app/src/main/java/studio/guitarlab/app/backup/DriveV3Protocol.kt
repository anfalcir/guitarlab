package studio.guitarlab.app.backup

import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.concurrent.ThreadLocalRandom
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put

internal data class DriveFileResource(
    val id: String,
    val name: String? = null,
    val size: Long? = null,
    val sha256Checksum: String? = null,
    val createdTime: String? = null,
    val modifiedTime: String? = null,
    val trashed: Boolean? = null,
    val appProperties: Map<String, String> = emptyMap(),
)

internal data class DriveFilePage(
    val files: List<DriveFileResource>,
    val nextPageToken: String? = null,
)

internal object DriveV3Json {
    private val json = Json { ignoreUnknownKeys = true }

    fun parseFile(raw: String): DriveFileResource {
        val obj = json.parseToJsonElement(raw).jsonObject
        return file(obj)
    }

    fun parsePage(raw: String): DriveFilePage {
        val obj = json.parseToJsonElement(raw).jsonObject
        val nextPageToken = obj["nextPageToken"]?.jsonPrimitive?.contentOrNull
        require(nextPageToken == null || nextPageToken.isNotBlank()) {
            "Resposta paginada do Drive contém nextPageToken vazio."
        }
        return DriveFilePage(
            files = obj["files"]?.jsonArray?.map { file(it.jsonObject) }.orEmpty(),
            nextPageToken = nextPageToken,
        )
    }

    fun metadata(
        name: String,
        mimeType: String,
        parents: List<String> = emptyList(),
        appProperties: Map<String, String> = emptyMap(),
    ): String = buildJsonObject {
        put("name", name)
        put("mimeType", mimeType)
        if (parents.isNotEmpty()) put("parents", buildJsonArray { parents.forEach { add(JsonPrimitive(it)) } })
        if (appProperties.isNotEmpty()) put("appProperties", mapJson(appProperties))
    }.toString()

    fun appProperties(properties: Map<String, String>): String = buildJsonObject {
        put("appProperties", mapJson(properties))
    }.toString()

    private fun mapJson(values: Map<String, String>): JsonObject = buildJsonObject {
        values.forEach { (key, value) -> put(key, value) }
    }

    private fun file(obj: JsonObject): DriveFileResource = DriveFileResource(
        id = obj["id"]?.jsonPrimitive?.contentOrNull ?: error("Resposta do Drive sem fileId."),
        name = obj["name"]?.jsonPrimitive?.contentOrNull,
        size = obj["size"]?.jsonPrimitive?.longOrNull,
        sha256Checksum = obj["sha256Checksum"]?.jsonPrimitive?.contentOrNull,
        createdTime = obj["createdTime"]?.jsonPrimitive?.contentOrNull,
        modifiedTime = obj["modifiedTime"]?.jsonPrimitive?.contentOrNull,
        trashed = obj["trashed"]?.jsonPrimitive?.booleanOrNull,
        appProperties = obj["appProperties"]?.jsonObject?.mapNotNull { (key, value) ->
            value.jsonPrimitive.contentOrNull?.let { key to it }
        }?.toMap().orEmpty(),
    )
}

internal object DriveRetryPolicy {
    const val MAX_ATTEMPTS = 5
    private const val MAX_DELAY_MS = 16_000L

    fun retryableStatus(code: Int, body: String = ""): Boolean = when (code) {
        429, 500, 502, 503, 504 -> true
        403 -> body.contains("rateLimitExceeded", ignoreCase = true) ||
            body.contains("userRateLimitExceeded", ignoreCase = true)
        else -> false
    }

    fun delayMs(attempt: Int, jitterMs: Long = 0L): Long {
        val base = (1_000L shl attempt.coerceIn(0, 4)).coerceAtMost(MAX_DELAY_MS)
        return base + jitterMs.coerceIn(0L, 500L)
    }
}

internal data class DriveHttpResponse(
    val code: Int,
    val body: String,
    val headers: Map<String, List<String>>,
) {
    fun header(name: String): String? = headers.entries.firstOrNull { it.key.equals(name, true) }?.value?.firstOrNull()
    val successful: Boolean get() = code in 200..299
}

internal interface DriveV3Api {
    suspend fun get(url: String): DriveHttpResponse
    suspend fun postJson(url: String, body: String, extraHeaders: Map<String, String> = emptyMap()): DriveHttpResponse
    suspend fun patchJson(url: String, body: String): DriveHttpResponse
    suspend fun delete(url: String): DriveHttpResponse
    suspend fun uploadStatus(sessionUrl: String, totalBytes: Long): DriveHttpResponse
    suspend fun uploadChunk(sessionUrl: String, file: File, start: Long, length: Int, totalBytes: Long): DriveHttpResponse
    suspend fun download(url: String, destination: File)
}

internal interface DriveTransportObserver {
    fun onRetry(operation: String) = Unit
    fun onAuthorizationRefresh() = Unit
}

internal object NoopDriveTransportObserver : DriveTransportObserver

internal class DriveV3HttpClient(
    private val tokenProvider: DriveAccessTokenProvider,
    private val retryDelay: suspend (Long) -> Unit = { delay(it) },
    private val connectionFactory: (String) -> HttpURLConnection = {
        URL(it).openConnection() as HttpURLConnection
    },
    private val observer: DriveTransportObserver = NoopDriveTransportObserver,
) : DriveV3Api {
    override suspend fun get(url: String): DriveHttpResponse = ordinaryRequest("GET", url)

    override suspend fun postJson(url: String, body: String, extraHeaders: Map<String, String>): DriveHttpResponse =
        ordinaryRequest("POST", url, body.toByteArray(Charsets.UTF_8), "application/json; charset=UTF-8", extraHeaders)

    override suspend fun patchJson(url: String, body: String): DriveHttpResponse = ordinaryRequest(
        method = "POST",
        url = url,
        body = body.toByteArray(Charsets.UTF_8),
        contentType = "application/json; charset=UTF-8",
        extraHeaders = mapOf("X-HTTP-Method-Override" to "PATCH"),
    )

    override suspend fun delete(url: String): DriveHttpResponse = ordinaryRequest("DELETE", url)

    /** Resumable session calls intentionally do not blindly replay chunks after 5xx/network errors. */
    override suspend fun uploadStatus(sessionUrl: String, totalBytes: Long): DriveHttpResponse = rawAuthorizedRequest(
        method = "PUT",
        url = sessionUrl,
        body = ByteArray(0),
        contentType = null,
        extraHeaders = mapOf("Content-Range" to "bytes */$totalBytes"),
        retry401 = true,
    )

    override suspend fun uploadChunk(
        sessionUrl: String,
        file: File,
        start: Long,
        length: Int,
        totalBytes: Long,
    ): DriveHttpResponse = withContext(Dispatchers.IO) {
        val token = tokenProvider.accessToken()
        val connection = open(sessionUrl, "PUT", token).apply {
            setRequestProperty("Content-Type", "application/octet-stream")
            setRequestProperty("Content-Length", length.toString())
            setRequestProperty("Content-Range", "bytes $start-${start + length - 1}/$totalBytes")
            doOutput = true
            setFixedLengthStreamingMode(length)
        }
        try {
            file.inputStream().buffered().use { input ->
                var skipped = 0L
                while (skipped < start) {
                    val amount = input.skip(start - skipped)
                    if (amount <= 0L) error("Não foi possível posicionar o pacote para retomar o upload.")
                    skipped += amount
                }
                connection.outputStream.buffered().use { output ->
                    var remaining = length
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    while (remaining > 0) {
                        val read = input.read(buffer, 0, minOf(buffer.size, remaining))
                        if (read < 0) error("Pacote local terminou antes do tamanho esperado.")
                        output.write(buffer, 0, read)
                        remaining -= read
                    }
                }
            }
            response(connection).also { response ->
                if (response.code == 401) {
                    tokenProvider.invalidateRejectedToken()
                    observer.onAuthorizationRefresh()
                }
            }
        } finally {
            connection.disconnect()
        }
    }

    override suspend fun download(url: String, destination: File) = withContext(Dispatchers.IO) {
        var attempt = 0
        var refreshed401 = false
        while (true) {
            destination.delete()
            try {
                val token = tokenProvider.accessToken()
                val connection = open(url, "GET", token)
                try {
                    val code = connection.responseCode
                    if (code == 401) {
                        if (!refreshed401) {
                            tokenProvider.invalidateRejectedToken()
                            observer.onAuthorizationRefresh()
                            refreshed401 = true
                            continue
                        }
                        throw DriveAuthorizationRequiredException()
                    }
                    if (code in 200..299) {
                        connection.inputStream.buffered().use { input ->
                            destination.outputStream().buffered().use { output ->
                                input.copyTo(output)
                            }
                        }
                        return@withContext
                    }
                    val body = readBody(connection, code)
                    if (
                        DriveRetryPolicy.retryableStatus(code, body) &&
                            attempt < DriveRetryPolicy.MAX_ATTEMPTS - 1
                    ) {
                        observer.onRetry("download-http-$code")
                        retryDelay(backoff(attempt++))
                        continue
                    }
                    throw DriveApiException(code, body)
                } finally {
                    connection.disconnect()
                }
            } catch (error: IOException) {
                destination.delete()
                if (error is DriveApiException || attempt >= DriveRetryPolicy.MAX_ATTEMPTS - 1) {
                    throw error
                }
                observer.onRetry("download-io")
                retryDelay(backoff(attempt++))
            }
        }
    }

    private suspend fun ordinaryRequest(
        method: String,
        url: String,
        body: ByteArray? = null,
        contentType: String? = null,
        extraHeaders: Map<String, String> = emptyMap(),
    ): DriveHttpResponse {
        var attempt = 0
        while (true) {
            try {
                val response = rawAuthorizedRequest(method, url, body, contentType, extraHeaders, retry401 = true)
                if (response.successful) return response
                if (DriveRetryPolicy.retryableStatus(response.code, response.body) && attempt < DriveRetryPolicy.MAX_ATTEMPTS - 1) {
                    observer.onRetry("request-http-${response.code}")
                    retryDelay(backoff(attempt++))
                    continue
                }
                return response
            } catch (error: IOException) {
                if (attempt >= DriveRetryPolicy.MAX_ATTEMPTS - 1) throw error
                observer.onRetry("request-io")
                retryDelay(backoff(attempt++))
            }
        }
    }

    private suspend fun rawAuthorizedRequest(
        method: String,
        url: String,
        body: ByteArray? = null,
        contentType: String? = null,
        extraHeaders: Map<String, String> = emptyMap(),
        retry401: Boolean,
    ): DriveHttpResponse = withContext(Dispatchers.IO) {
        var refreshed = false
        var completed: DriveHttpResponse? = null
        while (completed == null) {
            val token = tokenProvider.accessToken()
            val connection = open(url, method, token)
            try {
                contentType?.let { connection.setRequestProperty("Content-Type", it) }
                extraHeaders.forEach(connection::setRequestProperty)
                if (body != null) {
                    connection.doOutput = true
                    connection.setFixedLengthStreamingMode(body.size)
                    connection.outputStream.use { it.write(body) }
                }
                val result = response(connection)
                if (result.code == 401 && retry401) {
                    if (!refreshed) {
                        tokenProvider.invalidateRejectedToken()
                        observer.onAuthorizationRefresh()
                        refreshed = true
                    } else {
                        throw DriveAuthorizationRequiredException()
                    }
                } else {
                    completed = result
                }
            } finally {
                connection.disconnect()
            }
        }
        checkNotNull(completed)
    }

    private fun open(url: String, method: String, token: String): HttpURLConnection =
        connectionFactory(url).apply {
            requestMethod = method
            connectTimeout = 20_000
            readTimeout = 60_000
            useCaches = false
            setRequestProperty("Authorization", "Bearer $token")
            setRequestProperty("Accept", "application/json")
        }

    private fun response(connection: HttpURLConnection): DriveHttpResponse {
        val code = connection.responseCode
        val headers = connection.headerFields.entries
            .mapNotNull { (key, values) -> key?.let { it to values } }
            .toMap()
        return DriveHttpResponse(code, readBody(connection, code), headers)
    }

    private fun readBody(connection: HttpURLConnection, code: Int): String {
        val stream = if (code in 200..399) connection.inputStream else connection.errorStream
        return stream?.bufferedReader()?.use { it.readText() }.orEmpty()
    }

    private fun backoff(attempt: Int): Long = DriveRetryPolicy.delayMs(
        attempt,
        ThreadLocalRandom.current().nextLong(0L, 501L),
    )
}

internal fun isTransientDriveFailure(error: Throwable): Boolean = when (error) {
    is DriveAuthorizationRequiredException -> false
    is DriveApiException ->
        DriveRetryPolicy.retryableStatus(error.statusCode, error.responseBody)
    is IOException -> true
    else -> false
}

internal class DriveApiException(val statusCode: Int, val responseBody: String) : IOException(
    when (statusCode) {
        401 -> "A autorização do Google Drive expirou. Reconecte sua conta."
        403 -> "O Google Drive recusou a operação. Verifique a autorização ou a cota da API."
        404 -> "O item de backup não existe mais no Google Drive."
        429 -> "O Google Drive aplicou um limite temporário. O GuitarLab tentará novamente."
        in 500..599 -> "O Google Drive está temporariamente indisponível."
        else -> "Falha na API Google Drive (HTTP $statusCode)."
    },
)

internal fun requireSuccessful(response: DriveHttpResponse, operation: String): DriveHttpResponse {
    if (!response.successful) throw DriveApiException(response.code, response.body.ifBlank { operation })
    return response
}

internal fun encodeQuery(value: String): String = URLEncoder.encode(value, StandardCharsets.UTF_8.name())
