package studio.guitarlab.app.diagnostics

import android.content.Context
import android.os.Build
import java.io.File
import java.io.OutputStream
import java.security.MessageDigest
import java.time.Instant
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import org.json.JSONArray
import org.json.JSONObject
import studio.guitarlab.app.BuildConfig
import studio.guitarlab.app.activity.UnifiedActivityStore
import studio.guitarlab.app.backup.BackupSettingsStore
import studio.guitarlab.app.ui.StudioAudioRoutingStore
import studio.guitarlab.core.project.FileProjectRepository
import studio.guitarlab.core.project.PreparedReferenceBindingPolicy
import studio.guitarlab.platform.separation.FileRemoteJobStore

data class DiagnosticBundleResult(
    val entryNames: List<String>,
    val sha256ByEntry: Map<String, String>,
)

class DiagnosticBundleExporter(private val context: Context) {
    fun export(projectId: String?, output: OutputStream): DiagnosticBundleResult {
        val entries = linkedMapOf<String, ByteArray>()
        val project = projectId?.let { FileProjectRepository(context.filesDir).load(it) }
        val activity = UnifiedActivityStore(context).snapshot()
        val journal = DiagnosticJournal(context)
        val routing = StudioAudioRoutingStore(context)
        val backup = BackupSettingsStore(context).snapshot()
        val jobs = FileRemoteJobStore(context).active()
            .filter { projectId == null || it.identity.projectId == projectId }
            .sortedByDescending { it.updatedAtMs }

        entries["README.txt"] = buildString {
            appendLine("GuitarLab diagnostic package")
            appendLine("Generated: ${Instant.now()}")
            appendLine("Contains metadata and sanitized logs only; song audio is excluded.")
            appendLine("Authentication tokens, passwords and resumable-session URLs are not included.")
            appendLine("Project scope: ${projectId ?: "application"}")
        }.toByteArray()

        entries["app.json"] = JSONObject()
            .put("versionName", BuildConfig.VERSION_NAME)
            .put("versionCode", BuildConfig.VERSION_CODE)
            .put("packageName", BuildConfig.APPLICATION_ID)
            .put("buildType", BuildConfig.BUILD_TYPE)
            .toString(2).toByteArray()

        entries["device.json"] = JSONObject()
            .put("manufacturer", Build.MANUFACTURER)
            .put("model", Build.MODEL)
            .put("device", Build.DEVICE)
            .put("androidRelease", Build.VERSION.RELEASE)
            .put("sdkInt", Build.VERSION.SDK_INT)
            .put("supportedAbis", JSONArray(Build.SUPPORTED_ABIS.toList()))
            .put("filesFreeBytes", context.filesDir.usableSpace)
            .put("filesTotalBytes", context.filesDir.totalSpace)
            .toString(2).toByteArray()

        entries["events.jsonl"] = journal.rawSanitizedJsonl().toByteArray()

        val routeHealth = routing.routeHealth()
        entries["audio-route.json"] = JSONObject()
            .put("selectedInput", routing.selectedInputDiagnosticIdentity())
            .put("selectedOutput", routing.selectedOutputDiagnosticIdentity())
            .put("selectedCueOutput", routing.selectedCueOutputDiagnosticIdentity())
            .put("monitoringMode", routing.monitoringMode().name)
            .put("selectedInputAvailable", routeHealth.selectedInputAvailable)
            .put("selectedOutputAvailable", routeHealth.selectedOutputAvailable)
            .put("selectedCueOutputAvailable", routeHealth.selectedCueOutputAvailable)
            .put("cueOutputDistinctFromMain", routeHealth.cueOutputDistinctFromMain)
            .put("usbDeviceDetected", routeHealth.usbDeviceDetected)
            .put("effectiveInput", routeHealth.effectiveInput?.let { "${it.transportFamily}:${it.label}" })
            .put("effectiveOutput", routeHealth.effectiveOutput?.let { "${it.transportFamily}:${it.label}" })
            .put("effectiveCueOutput", routeHealth.effectiveCueOutput?.let { "${it.transportFamily}:${it.label}" })
            .toString(2).toByteArray()

        entries["activity.json"] = JSONArray().also { array ->
            activity.forEach { record ->
                array.put(
                    JSONObject()
                        .put("operationId", record.operationId)
                        .put("projectId", record.projectId)
                        .put("kind", record.kind.name)
                        .put("state", record.state.name)
                        .put("progressPercent", record.progressPercent)
                        .put("updatedAtEpochMs", record.updatedAtEpochMs)
                        .put("summary", record.summary)
                        .put("technicalDetail", sanitize(record.technicalDetail)),
                )
            }
        }.toString(2).toByteArray()

        if (project != null) {
            val preparation = project.preparation
            val activeAssetIds = buildSet {
                preparation?.sourceAssetId?.let(::add)
                preparation?.activeStemAssetIds?.values?.let(::addAll)
                preparation?.activeBackingAssetId?.let(::add)
                preparation?.activeGuitarAssetId?.let(::add)
            }
            entries["project/summary.json"] = JSONObject()
                .put("projectId", project.id)
                .put("name", project.name)
                .put("schemaVersion", project.schemaVersion)
                .put("assetCount", project.assets.size)
                .put("clipCount", project.clips.size)
                .put("trackCount", project.tracks.size)
                .put("takeCount", project.takes.size)
                .put("preparationStatus", preparation?.status?.name)
                .put("referenceRepairAvailable", PreparedReferenceBindingPolicy.bindingDiffersFromDesired(project))
                .put("updatedAtEpochMs", project.updatedAtEpochMs)
                .toString(2).toByteArray()

            entries["project/active-assets.json"] = JSONArray().also { array ->
                project.assets.filter { it.assetId in activeAssetIds }.sortedBy { it.role.name }.forEach { asset ->
                    array.put(
                        JSONObject()
                            .put("assetId", asset.assetId)
                            .put("role", asset.role.name)
                            .put("format", asset.format)
                            .put("byteSize", asset.byteSize)
                            .put("sha256", asset.sha256)
                            .put("sampleRateHz", asset.sampleRateHz)
                            .put("channelCount", asset.channelCount)
                            .put("frameCount", asset.frameCount)
                            .put("provenanceKind", asset.provenance?.kind),
                    )
                }
            }.toString(2).toByteArray()

            entries["project/reference-bindings.json"] = JSONArray().also { array ->
                project.referenceBindings.sortedBy { it.kind.name }.forEach { binding ->
                    array.put(
                        JSONObject()
                            .put("bindingId", binding.bindingId)
                            .put("kind", binding.kind.name)
                            .put("trackId", binding.trackId)
                            .put("assetId", binding.assetId)
                            .put("createdAtEpochMs", binding.createdAtEpochMs),
                    )
                }
            }.toString(2).toByteArray()
        }

        entries["separation/jobs.json"] = JSONArray().also { array ->
            jobs.forEach { job ->
                array.put(
                    JSONObject()
                        .put("jobId", job.identity.jobId)
                        .put("projectId", job.identity.projectId)
                        .put("sourceAssetId", job.identity.sourceAssetId)
                        .put("inputSha256", job.identity.inputSha256)
                        .put("state", job.state.name)
                        .put("updatedAtMs", job.updatedAtMs)
                        .put("resultManifestSha256", job.resultManifestSha256)
                        .put("errorCode", sanitize(job.errorCode)),
                )
            }
        }.toString(2).toByteArray()

        val manifestRoot = File(context.filesDir, "diagnostics/manifests")
        val allowedJobIds = jobs.mapTo(mutableSetOf()) { it.identity.jobId }
        manifestRoot.listFiles { file -> file.isFile && file.extension == "json" }.orEmpty()
            .filter { projectId == null || it.nameWithoutExtension in allowedJobIds }
            .sortedBy { it.name }
            .forEach { file ->
                val sanitized = runCatching { sanitizeJson(JSONObject(file.readText())) as? JSONObject }.getOrNull()
                if (sanitized != null) entries["separation/manifests/${file.name}"] = sanitized.toString(2).toByteArray()
            }

        entries["backup/status.json"] = JSONObject()
            .put("driveConnected", backup.driveConnected)
            .put("driveAccountConfigured", !backup.driveAccountLabel.isNullOrBlank())
            .put("automaticEnabled", backup.automaticEnabled)
            .put("cadence", backup.cadence.name)
            .put("lastRunEpochMs", backup.lastRunEpochMs)
            .put("lastSuccessEpochMs", backup.lastSuccessEpochMs)
            .put("lastRunSummary", sanitize(backup.lastRunSummary))
            .put("lastError", sanitize(backup.lastError))
            .toString(2).toByteArray()

        val checksums = entries.entries.associate { (name, bytes) -> name to sha256(bytes) }
        entries["integrity/SHA256SUMS.txt"] = checksums.entries
            .sortedBy { it.key }
            .joinToString(separator = "\n", postfix = "\n") { (name, hash) -> "$hash  $name" }
            .toByteArray()

        ZipOutputStream(output.buffered()).use { zip ->
            entries.entries.sortedBy { it.key }.forEach { (name, bytes) ->
                val entry = ZipEntry(name).apply { time = 0L }
                zip.putNextEntry(entry)
                zip.write(bytes)
                zip.closeEntry()
            }
        }
        return DiagnosticBundleResult(entries.keys.sorted(), checksums)
    }

    private fun sanitize(value: String?): String? {
        if (value == null) return null
        var result: String = value
        SENSITIVE_PATTERNS.forEach { pattern -> result = result.replace(pattern, "[REDACTED]") }
        return result.take(8_192)
    }

    private fun sanitizeJson(value: Any?): Any? = when (value) {
        is JSONObject -> JSONObject().also { output ->
            val keys = value.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                if (SENSITIVE_KEYS.any { it in key.lowercase() }) continue
                output.put(key, sanitizeJson(value.opt(key)))
            }
        }
        is JSONArray -> JSONArray().also { output ->
            for (i in 0 until value.length()) output.put(sanitizeJson(value.opt(i)))
        }
        is String -> sanitize(value)
        JSONObject.NULL, null -> JSONObject.NULL
        else -> value
    }

    private fun sha256(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }

    private companion object {
        val SENSITIVE_KEYS = listOf(
            "password", "authorization", "accesstoken", "refreshtoken", "idtoken",
            "appcheck", "credential", "serviceaccount", "resumablesession", "sessionurl",
        )
        val SENSITIVE_PATTERNS = listOf(
            Regex("""(?i)Bearer\s+[A-Za-z0-9._~+/=-]+"""),
            Regex("""(?i)(access[_-]?token|refresh[_-]?token|id[_-]?token|app[_-]?check|password|authorization|credential)\s*[:=]\s*[^\s,;]+"""),
            Regex("""AIza[0-9A-Za-z_-]{30,}"""),
        )
    }
}
