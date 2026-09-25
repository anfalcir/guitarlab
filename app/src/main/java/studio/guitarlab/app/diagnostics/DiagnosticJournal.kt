package studio.guitarlab.app.diagnostics

import android.content.Context
import android.os.SystemClock
import java.io.File
import java.time.Instant
import org.json.JSONObject
import studio.guitarlab.core.project.UnifiedOperationRecord

data class DiagnosticEvent(
    val timestampEpochMs: Long,
    val eventType: String,
    val projectId: String?,
    val operationId: String?,
    val state: String?,
    val summary: String,
    val technicalDetail: String?,
)

class DiagnosticJournal(context: Context) {
    private val root = File(context.applicationContext.filesDir, "diagnostics").apply { mkdirs() }
    private val file = File(root, "events.jsonl")
    private val lock = Any()

    fun appendActivity(record: UnifiedOperationRecord) {
        append(
            eventType = "activity.${record.kind.name.lowercase()}",
            projectId = record.projectId,
            operationId = record.operationId,
            state = record.state.name,
            summary = record.summary,
            technicalDetail = record.technicalDetail,
            timestampEpochMs = record.updatedAtEpochMs,
        )
    }

    fun append(
        eventType: String,
        projectId: String? = null,
        operationId: String? = null,
        state: String? = null,
        summary: String,
        technicalDetail: String? = null,
        timestampEpochMs: Long = System.currentTimeMillis(),
    ) = synchronized(lock) {
        root.mkdirs()
        val event = JSONObject()
            .put("schemaVersion", 1)
            .put("timestampEpochMs", timestampEpochMs)
            .put("timestampUtc", Instant.ofEpochMilli(timestampEpochMs).toString())
            .put("elapsedRealtimeMs", SystemClock.elapsedRealtime())
            .put("eventType", sanitizeScalar(eventType))
            .put("projectId", projectId?.let(::sanitizeScalar))
            .put("operationId", operationId?.let(::sanitizeScalar))
            .put("state", state?.let(::sanitizeScalar))
            .put("summary", sanitizeText(summary))
            .put("technicalDetail", technicalDetail?.let(::sanitizeText))
        file.appendText(event.toString() + "\n", Charsets.UTF_8)
        pruneLocked(timestampEpochMs)
    }

    fun readEvents(): List<DiagnosticEvent> = synchronized(lock) {
        if (!file.isFile) return@synchronized emptyList()
        file.useLines { lines ->
            lines.mapNotNull { line ->
                runCatching {
                    val o = JSONObject(line)
                    DiagnosticEvent(
                        timestampEpochMs = o.getLong("timestampEpochMs"),
                        eventType = o.getString("eventType"),
                        projectId = o.optString("projectId").takeIf { it.isNotBlank() && it != "null" },
                        operationId = o.optString("operationId").takeIf { it.isNotBlank() && it != "null" },
                        state = o.optString("state").takeIf { it.isNotBlank() && it != "null" },
                        summary = o.optString("summary"),
                        technicalDetail = o.optString("technicalDetail").takeIf { it.isNotBlank() && it != "null" },
                    )
                }.getOrNull()
            }.toList()
        }
    }

    fun rawSanitizedJsonl(): String = synchronized(lock) {
        if (!file.isFile) "" else file.readLines(Charsets.UTF_8)
            .mapNotNull { line -> runCatching { JSONObject(line).toString() }.getOrNull() }
            .joinToString(separator = "\n", postfix = if (file.length() > 0) "\n" else "")
    }

    fun clear() = synchronized(lock) {
        if (file.exists()) file.writeText("")
    }

    fun sizeBytes(): Long = synchronized(lock) { file.takeIf(File::isFile)?.length() ?: 0L }

    private fun pruneLocked(nowEpochMs: Long) {
        if (!file.isFile) return
        val cutoff = nowEpochMs - RETENTION_MS
        var kept = file.readLines(Charsets.UTF_8)
            .filter { line ->
                runCatching { JSONObject(line).optLong("timestampEpochMs", Long.MAX_VALUE) >= cutoff }
                    .getOrDefault(false)
            }

        var bytes = kept.sumOf { it.toByteArray(Charsets.UTF_8).size + 1 }
        if (bytes > MAX_BYTES) {
            val reversed = mutableListOf<String>()
            bytes = 0
            for (line in kept.asReversed()) {
                val lineBytes = line.toByteArray(Charsets.UTF_8).size + 1
                if (bytes + lineBytes > MAX_BYTES) break
                reversed += line
                bytes += lineBytes
            }
            kept = reversed.asReversed()
        }
        val rendered = kept.joinToString(separator = "\n", postfix = if (kept.isEmpty()) "" else "\n")
        file.writeText(rendered, Charsets.UTF_8)
    }

    private fun sanitizeScalar(value: String): String =
        sanitizeText(value).replace("\n", " ").take(MAX_SCALAR_CHARS)

    private fun sanitizeText(value: String): String {
        var sanitized = value
        SENSITIVE_PATTERNS.forEach { pattern -> sanitized = sanitized.replace(pattern, "[REDACTED]") }
        return sanitized.take(MAX_TEXT_CHARS)
    }

    companion object {
        const val MAX_BYTES = 8 * 1024 * 1024
        const val RETENTION_MS = 14L * 24L * 60L * 60L * 1000L
        private const val MAX_TEXT_CHARS = 8_192
        private const val MAX_SCALAR_CHARS = 512
        private val SENSITIVE_PATTERNS = listOf(
            Regex("""(?i)Bearer\s+[A-Za-z0-9._~+/=-]+"""),
            Regex("""(?i)(access[_-]?token|refresh[_-]?token|id[_-]?token|app[_-]?check|password|authorization|credential)\s*[:=]\s*[^\s,;]+"""),
            Regex("""(?i)https?://[^\s"']+[?&](?:X-Goog-Signature|upload_id|session|token)=[^\s"']+"""),
            Regex("""AIza[0-9A-Za-z_-]{30,}"""),
        )
    }
}
