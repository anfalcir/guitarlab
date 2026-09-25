package studio.guitarlab.platform.separation

import android.content.Context
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import org.json.JSONArray
import org.json.JSONObject
import studio.guitarlab.core.separation.RemoteJobIdentity

/**
 * Durable, sanitized provenance retained after remote ACK/purge.
 * Audio is never copied here.
 */
internal class AcceptedRemoteManifestStore(context: Context) {
    private val root = File(context.filesDir, "diagnostics/manifests").apply { mkdirs() }

    fun persist(identity: RemoteJobIdentity, bytes: ByteArray) {
        require(bytes.size in 2..65_536) { "RESULT_INVALID" }
        val parsed = JSONObject(bytes.toString(Charsets.UTF_8))
        val sanitized = requireNotNull(sanitize(parsed) as? JSONObject) { "RESULT_INVALID" }
        sanitized.put("_diagnosticJobId", identity.jobId)
        sanitized.put("_diagnosticProjectId", identity.projectId)
        sanitized.put("_acceptedAtEpochMs", System.currentTimeMillis())

        val target = File(root, "${identity.jobId}.json")
        val partial = File(root, ".${identity.jobId}.json.part")
        partial.writeText(sanitized.toString(2), Charsets.UTF_8)
        try {
            Files.move(
                partial.toPath(),
                target.toPath(),
                StandardCopyOption.REPLACE_EXISTING,
                StandardCopyOption.ATOMIC_MOVE,
            )
        } catch (_: Exception) {
            Files.move(partial.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING)
        }
        prune()
    }

    private fun sanitize(value: Any?): Any? = when (value) {
        is JSONObject -> JSONObject().also { output ->
            val names = value.keys()
            while (names.hasNext()) {
                val key = names.next()
                if (isSensitiveKey(key)) continue
                output.put(key, sanitize(value.opt(key)))
            }
        }
        is JSONArray -> JSONArray().also { output ->
            for (index in 0 until value.length()) output.put(sanitize(value.opt(index)))
        }
        JSONObject.NULL, null -> JSONObject.NULL
        is String -> sanitizeString(value)
        else -> value
    }

    private fun isSensitiveKey(key: String): Boolean {
        val normalized = key.lowercase()
        return listOf(
            "password", "authorization", "accesstoken", "refreshtoken",
            "idtoken", "appcheck", "credential", "serviceaccount",
            "resumablesession", "sessionurl",
        ).any { it in normalized }
    }

    private fun sanitizeString(value: String): String {
        var result = value
        SENSITIVE_PATTERNS.forEach { regex -> result = result.replace(regex, "[REDACTED]") }
        return result.take(MAX_STRING_CHARS)
    }

    private fun prune() {
        val now = System.currentTimeMillis()
        root.listFiles { file -> file.isFile && file.extension == "json" }.orEmpty()
            .forEach { file ->
                if (now - file.lastModified() > RETENTION_MS) file.delete()
            }
        val ordered = root.listFiles { file -> file.isFile && file.extension == "json" }.orEmpty()
            .sortedByDescending { it.lastModified() }
        ordered.drop(MAX_FILES).forEach(File::delete)
    }

    private companion object {
        const val MAX_FILES = 40
        const val MAX_STRING_CHARS = 8_192
        const val RETENTION_MS = 14L * 24L * 60L * 60L * 1000L
        val SENSITIVE_PATTERNS = listOf(
            Regex("""(?i)Bearer\s+[A-Za-z0-9._~+/=-]+"""),
            Regex("""(?i)(token|password|authorization)\s*[:=]\s*[^\s,;]+"""),
            Regex("""(?i)https?://[^\s"']+[?&](?:X-Goog-Signature|upload_id|session)=[^\s"']+"""),
        )
    }
}
