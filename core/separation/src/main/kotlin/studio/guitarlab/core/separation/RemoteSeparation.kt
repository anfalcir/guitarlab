package studio.guitarlab.core.separation

enum class RemoteJobState { UPLOADING, READY, QUEUED, RUNNING, COMPLETED, IMPORTING, IMPORTED, CANCEL_REQUESTED, CANCELLED, FAILED, EXPIRED }
data class RemoteJobIdentity(val jobId:String,val projectId:String,val sourceAssetId:String,val inputSha256:String) {
 init { require(jobId.matches(UUID)); require(projectId.isNotBlank()); require(sourceAssetId.isNotBlank()); require(inputSha256.matches(SHA)) }
 companion object { val UUID=Regex("[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}",RegexOption.IGNORE_CASE); val SHA=Regex("[a-f0-9]{64}") }
}
data class RemoteStem(val name:String,val path:String,val bytes:Long,val sha256:String)
data class RemoteResultManifest(val jobId:String,val projectId:String,val inputSha256:String,val engine:String,val engineRevision:String,val model:String,val modelSha256:String,val sampleRate:Int,val channels:Int,val frames:Long,val durationSeconds:Double,val stems:List<RemoteStem>) {
 fun validateFor(i:RemoteJobIdentity) {
  require(jobId==i.jobId && projectId==i.projectId && inputSha256==i.inputSha256){"remote result ownership mismatch"}
  require(engine=="demucs.cpp" && model=="htdemucs_6s" && modelSha256==MODEL_SHA256)
  require(sampleRate==44100 && channels==2 && frames>0 && durationSeconds>0)
  require(stems.map{it.name}==STEMS && stems.map{it.name}.distinct().size==6)
  require(stems.all{it.bytes>44 && it.sha256.matches(RemoteJobIdentity.SHA)})
 }
 companion object { const val MODEL_SHA256="09704f4ceae204e56e77d5eefd6ac71d7275be81fd507e6913371d59abcee856"; val STEMS=listOf("drums","bass","other","vocals","guitar","piano") }
}
object RemoteStateMachine {
 fun accepts(c:RemoteJobState?,n:RemoteJobState):Boolean {
  if(c==null||c==n)return true
  if(c in setOf(RemoteJobState.IMPORTED,RemoteJobState.CANCELLED,RemoteJobState.FAILED,RemoteJobState.EXPIRED))return false
  if(c==RemoteJobState.CANCEL_REQUESTED)return n in setOf(RemoteJobState.CANCELLED,RemoteJobState.COMPLETED,RemoteJobState.FAILED,RemoteJobState.EXPIRED)
  return n in when(c){
   RemoteJobState.UPLOADING->setOf(RemoteJobState.READY,RemoteJobState.CANCEL_REQUESTED,RemoteJobState.FAILED,RemoteJobState.EXPIRED)
   RemoteJobState.READY->setOf(RemoteJobState.QUEUED,RemoteJobState.CANCEL_REQUESTED,RemoteJobState.FAILED,RemoteJobState.EXPIRED)
   RemoteJobState.QUEUED->setOf(RemoteJobState.RUNNING,RemoteJobState.CANCEL_REQUESTED,RemoteJobState.CANCELLED,RemoteJobState.FAILED,RemoteJobState.EXPIRED)
   RemoteJobState.RUNNING->setOf(RemoteJobState.COMPLETED,RemoteJobState.CANCEL_REQUESTED,RemoteJobState.CANCELLED,RemoteJobState.FAILED,RemoteJobState.EXPIRED)
   RemoteJobState.COMPLETED->setOf(RemoteJobState.IMPORTING,RemoteJobState.CANCEL_REQUESTED,RemoteJobState.EXPIRED)
   RemoteJobState.IMPORTING->setOf(RemoteJobState.COMPLETED,RemoteJobState.IMPORTED,RemoteJobState.FAILED,RemoteJobState.EXPIRED)
   else->emptySet()
  }
 }
}
data class DurableRemoteJob(val identity:RemoteJobIdentity,val state:RemoteJobState,val updatedAtMs:Long,val resultManifestSha256:String?=null,val errorCode:String?=null)
interface RemoteJobStore { fun load(jobId:String):DurableRemoteJob?; fun save(job:DurableRemoteJob):DurableRemoteJob }
interface RemoteSeparationBackend { suspend fun enqueue(identity:RemoteJobIdentity,inputPath:String); suspend fun status(identity:RemoteJobIdentity):DurableRemoteJob?; suspend fun cancel(identity:RemoteJobIdentity); suspend fun acknowledge(identity:RemoteJobIdentity,resultManifestSha256:String) }
object RemoteFailurePolicy {
 private val terminal=setOf("AUTH_REQUIRED","APP_CHECK_REJECTED","QUOTA_EXCEEDED","REMOTE_JOB_NOT_FOUND","CONFIG_INVALID","INPUT_MISSING","INPUT_HASH_MISMATCH","RESULT_INVALID")
 fun shouldRetry(code:String,attempt:Int,maxAttempts:Int=5)=attempt<maxAttempts && code !in terminal
}
