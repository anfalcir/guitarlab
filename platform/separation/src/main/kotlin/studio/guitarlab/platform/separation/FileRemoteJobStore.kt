package studio.guitarlab.platform.separation
import android.content.Context
import org.json.JSONObject
import studio.guitarlab.core.separation.*
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
class FileRemoteJobStore(context:Context):RemoteJobStore {
 private val dir=File(context.filesDir,"state/remote-separation").apply{mkdirs()}
 override fun load(jobId:String):DurableRemoteJob?=synchronized(this){runCatching{decode(File(dir,jobId+".json").readText())}.getOrNull()}
 override fun save(job:DurableRemoteJob):DurableRemoteJob=synchronized(this){
  val old=load(job.identity.jobId);if(old!=null&&!RemoteStateMachine.accepts(old.state,job.state))return old
  val target=File(dir,job.identity.jobId+".json");val tmp=File(dir,target.name+".tmp");tmp.writeText(encode(job))
  runCatching{Files.move(tmp.toPath(),target.toPath(),StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE)}.getOrElse{Files.move(tmp.toPath(),target.toPath(),StandardCopyOption.REPLACE_EXISTING)}
  job
 }
 private fun encode(j:DurableRemoteJob)=JSONObject().put("jobId",j.identity.jobId).put("projectId",j.identity.projectId).put("sourceAssetId",j.identity.sourceAssetId).put("inputSha256",j.identity.inputSha256).put("state",j.state.name).put("updatedAtMs",j.updatedAtMs).apply{j.resultManifestSha256?.let{put("resultManifestSha256",it)};j.errorCode?.let{put("errorCode",it)}}.toString()
 private fun decode(s:String):DurableRemoteJob{val o=JSONObject(s);val i=RemoteJobIdentity(o.getString("jobId"),o.getString("projectId"),o.getString("sourceAssetId"),o.getString("inputSha256"));return DurableRemoteJob(i,RemoteJobState.valueOf(o.getString("state")),o.getLong("updatedAtMs"),o.optString("resultManifestSha256").takeIf{it.isNotBlank()},o.optString("errorCode").takeIf{it.isNotBlank()})}
}
