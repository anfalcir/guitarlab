package studio.guitarlab.platform.separation

import android.content.Context
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import org.json.JSONObject
import studio.guitarlab.core.separation.DurableRemoteJob
import studio.guitarlab.core.separation.RemoteJobIdentity
import studio.guitarlab.core.separation.RemoteJobState
import studio.guitarlab.core.separation.RemoteJobStore
import studio.guitarlab.core.separation.RemoteStateMachine

class FileRemoteJobStore(context: Context) : RemoteJobStore {
    private val dir = File(context.filesDir, "state/remote-separation").apply { mkdirs() }

    override fun load(jobId: String): DurableRemoteJob? = synchronized(this) {
        runCatching { decode(File(dir, "$jobId.json").readText()) }.getOrNull()
    }

    override fun save(job: DurableRemoteJob): DurableRemoteJob = synchronized(this) {
        val old = load(job.identity.jobId)
        if (old != null && !RemoteStateMachine.accepts(old.state, job.state)) return old
        val target = File(dir, "${job.identity.jobId}.json")
        val tmp = File(dir, "${target.name}.tmp")
        tmp.writeText(encode(job))
        runCatching {
            Files.move(tmp.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
        }.getOrElse {
            Files.move(tmp.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING)
        }
        changes.tryEmit(job.identity.projectId)
        job
    }

    override fun active(): List<DurableRemoteJob> = synchronized(this) {
        dir.listFiles { f -> f.isFile && f.extension == "json" }.orEmpty()
            .mapNotNull { runCatching { decode(it.readText()) }.getOrNull() }
    }

    fun latestForProject(projectId: String): DurableRemoteJob? =
        RemoteRecoveryPolicy.selectLatestForProject(active(), projectId)

    /** Local durable-job observation. Remote reconciliation remains owned by WorkManager. */
    fun observeProject(projectId: String): Flow<DurableRemoteJob?> = changes
        .filter { it == projectId }
        .map { latestForProject(projectId) }
        .onStart { emit(latestForProject(projectId)) }
        .distinctUntilChanged()

    fun removeProject(projectId: String): Int = synchronized(this) {
        val files = dir.listFiles { f -> f.isFile && f.extension == "json" }.orEmpty()
        var removed = 0
        files.forEach { file ->
            val job = runCatching { decode(file.readText()) }.getOrNull()
            if (job?.identity?.projectId == projectId && file.delete()) removed++
        }
        changes.tryEmit(projectId)
        removed
    }

    private fun encode(j: DurableRemoteJob) = JSONObject()
        .put("jobId", j.identity.jobId)
        .put("projectId", j.identity.projectId)
        .put("sourceAssetId", j.identity.sourceAssetId)
        .put("inputSha256", j.identity.inputSha256)
        .put("state", j.state.name)
        .put("updatedAtMs", j.updatedAtMs)
        .apply {
            j.resultManifestSha256?.let { put("resultManifestSha256", it) }
            j.errorCode?.let { put("errorCode", it) }
        }
        .toString()

    private fun decode(s: String): DurableRemoteJob {
        val o = JSONObject(s)
        val i = RemoteJobIdentity(
            o.getString("jobId"),
            o.getString("projectId"),
            o.getString("sourceAssetId"),
            o.getString("inputSha256"),
        )
        return DurableRemoteJob(
            i,
            RemoteJobState.valueOf(o.getString("state")),
            o.getLong("updatedAtMs"),
            o.optString("resultManifestSha256").takeIf { it.isNotBlank() },
            o.optString("errorCode").takeIf { it.isNotBlank() },
        )
    }

    private companion object {
        val changes = MutableSharedFlow<String>(extraBufferCapacity = 64)
    }
}
