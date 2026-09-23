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
  require(stems.map{it.name}.toSet()==STEMS.toSet() && stems.size==STEMS.size) { "invalid stem set" }
  require(stems.map{it.name}.distinct().size==STEMS.size) { "duplicate stem" }
  require(stems.all{it.bytes>44 && it.sha256.matches(RemoteJobIdentity.SHA)})
  require(stems.all{it.path.startsWith("remote/v1/") && !it.path.contains("..")}) { "unsafe stem path" }
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
object RemoteMissingPolicy {
 const val ERROR_REMOTE_JOB_NOT_FOUND="REMOTE_JOB_NOT_FOUND"
 fun shouldReplay(state:RemoteJobState)=state in setOf(RemoteJobState.UPLOADING,RemoteJobState.READY,RemoteJobState.QUEUED)
 fun terminalState(state:RemoteJobState):RemoteJobState?=when(state){
  RemoteJobState.CANCEL_REQUESTED->RemoteJobState.CANCELLED
  RemoteJobState.RUNNING,RemoteJobState.COMPLETED,RemoteJobState.IMPORTING->RemoteJobState.EXPIRED
  else->null
 }
 fun failureTerminalState(state:RemoteJobState)=when(state){
  RemoteJobState.COMPLETED,RemoteJobState.IMPORTING->RemoteJobState.EXPIRED
  else->RemoteJobState.FAILED
 }
}
interface RemoteJobStore { fun load(jobId:String):DurableRemoteJob?; fun save(job:DurableRemoteJob):DurableRemoteJob; fun active():List<DurableRemoteJob> = emptyList() }
interface RemoteSeparationBackend { suspend fun enqueue(identity:RemoteJobIdentity,inputPath:String); suspend fun status(identity:RemoteJobIdentity):DurableRemoteJob?; suspend fun cancel(identity:RemoteJobIdentity); suspend fun acknowledge(identity:RemoteJobIdentity,resultManifestSha256:String) }
interface RemoteResultTransport { suspend fun uploadSource(identity:RemoteJobIdentity):String; suspend fun downloadManifest(identity:RemoteJobIdentity):ByteArray; suspend fun downloadStem(identity:RemoteJobIdentity,stem:RemoteStem):ByteArray; suspend fun cleanup(identity:RemoteJobIdentity) }
interface RemoteStemPublisher { fun publish(identity:RemoteJobIdentity,manifest:RemoteResultManifest,manifestSha256:String,stems:Map<String,ByteArray>):Boolean }

class RemoteSeparationCoordinator(private val store:RemoteJobStore,private val backend:RemoteSeparationBackend,private val transport:RemoteResultTransport,private val decodeManifest:(ByteArray)->RemoteResultManifest,private val publisher:RemoteStemPublisher,private val nowMs:()->Long=System::currentTimeMillis) {
 suspend fun start(identity:RemoteJobIdentity):DurableRemoteJob {
  store.load(identity.jobId)?.let{require(it.identity==identity);return it}
  val initial=store.save(DurableRemoteJob(identity,RemoteJobState.UPLOADING,nowMs()))
  val path=transport.uploadSource(identity);store.save(initial.copy(state=RemoteJobState.READY,updatedAtMs=nowMs()));backend.enqueue(identity,path)
  return store.save(initial.copy(state=RemoteJobState.QUEUED,updatedAtMs=nowMs()))
 }
 suspend fun reconcile(identity:RemoteJobIdentity):DurableRemoteJob {
  val local=requireNotNull(store.load(identity.jobId)){"local job missing"};require(local.identity==identity){"local job ownership mismatch"}
  val remote=backend.status(identity)
  if(remote==null){
   if(RemoteMissingPolicy.shouldReplay(local.state)){
    val path=transport.uploadSource(identity)
    store.save(local.copy(state=RemoteJobState.READY,updatedAtMs=nowMs()))
    backend.enqueue(identity,path)
    return store.save(local.copy(state=RemoteJobState.QUEUED,updatedAtMs=nowMs(),errorCode=null))
   }
   RemoteMissingPolicy.terminalState(local.state)?.let { terminal ->
    return store.save(local.copy(state=terminal,updatedAtMs=nowMs(),errorCode=RemoteMissingPolicy.ERROR_REMOTE_JOB_NOT_FOUND))
   }
   return local
  }
  val accepted=store.save(remote)
  if(accepted.state!=RemoteJobState.COMPLETED&&accepted.state!=RemoteJobState.IMPORTING)return accepted
  store.save(accepted.copy(state=RemoteJobState.IMPORTING,updatedAtMs=nowMs()))
  val manifestBytes=transport.downloadManifest(identity);val manifestSha=sha256(manifestBytes);accepted.resultManifestSha256?.let{require(it==manifestSha){"manifest checksum mismatch"}}
  val manifest=decodeManifest(manifestBytes);manifest.validateFor(identity);val stems=manifest.stems.associate{it.name to transport.downloadStem(identity,it)};require(stems.size==RemoteResultManifest.STEMS.size)
  publisher.publish(identity,manifest,manifestSha,stems)
  backend.acknowledge(identity,manifestSha);transport.cleanup(identity)
  return store.save(accepted.copy(state=RemoteJobState.IMPORTED,updatedAtMs=nowMs(),resultManifestSha256=manifestSha,errorCode=null))
 }
 suspend fun cancel(identity:RemoteJobIdentity):DurableRemoteJob { val current=requireNotNull(store.load(identity.jobId));require(current.identity==identity);if(current.state in setOf(RemoteJobState.IMPORTED,RemoteJobState.CANCELLED,RemoteJobState.FAILED,RemoteJobState.EXPIRED))return current;val requesting=store.save(current.copy(state=RemoteJobState.CANCEL_REQUESTED,updatedAtMs=nowMs()));backend.cancel(identity);return requesting }
 private fun sha256(bytes:ByteArray)=java.security.MessageDigest.getInstance("SHA-256").digest(bytes).joinToString(""){"%02x".format(it)}
}
object RemoteFailurePolicy {
 private val terminal=setOf("AUTH_REQUIRED","APP_CHECK_REJECTED","QUOTA_EXCEEDED","REMOTE_JOB_NOT_FOUND","CONFIG_INVALID","INPUT_MISSING","INPUT_HASH_MISMATCH","RESULT_INVALID")
 fun shouldRetry(code:String,attempt:Int,maxAttempts:Int=5)=attempt<maxAttempts && code !in terminal
}
