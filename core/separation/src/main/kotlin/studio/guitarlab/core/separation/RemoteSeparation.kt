package studio.guitarlab.core.separation

enum class RemoteJobState { UPLOADING, READY, QUEUED, RUNNING, COMPLETED, IMPORTING, IMPORT_FAILED, IMPORTED, CANCEL_REQUESTED, CANCELLED, FAILED, EXPIRED }
data class RemoteJobIdentity(val jobId:String,val projectId:String,val sourceAssetId:String,val inputSha256:String) {
 init { require(jobId.matches(UUID)); require(projectId.isNotBlank()); require(sourceAssetId.isNotBlank()); require(inputSha256.matches(SHA)) }
 companion object { val UUID=Regex("[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}",RegexOption.IGNORE_CASE); val SHA=Regex("[a-f0-9]{64}") }
}
data class RemoteStem(val name:String,val path:String,val bytes:Long,val sha256:String)
data class RemoteReference(
 val name:String,
 val role:String,
 val path:String,
 val bytes:Long,
 val sha256:String,
 val sampleRate:Int,
 val channels:Int,
 val frames:Long,
 val encoding:String,
)
data class RemoteReferenceRecipe(
 val version:String,
 val targetPeakDbfs:Double,
 val sharedGainDb:Double,
 val backingStems:List<String>,
 val guitarStem:String,
)
data class RemoteResultManifest(
 val jobId:String,
 val projectId:String,
 val inputSha256:String,
 val engine:String,
 val engineRevision:String,
 val model:String,
 val modelSha256:String,
 val sampleRate:Int,
 val channels:Int,
 val frames:Long,
 val durationSeconds:Double,
 val stems:List<RemoteStem> = emptyList(),
 val schemaVersion:Int = 1,
 val deliverables:List<RemoteReference> = emptyList(),
 val referenceRecipe:RemoteReferenceRecipe? = null,
) {
 fun validateFor(i:RemoteJobIdentity) {
  require(jobId==i.jobId && projectId==i.projectId && inputSha256==i.inputSha256){"remote result ownership mismatch"}
  require(engine=="demucs.cpp" && model=="htdemucs_6s" && modelSha256==MODEL_SHA256)
  require(sampleRate==44100 && channels==2 && frames>0 && durationSeconds>0)
  when(schemaVersion) {
   1 -> {
    require(stems.map{it.name}.toSet()==STEMS.toSet() && stems.size==STEMS.size) { "invalid stem set" }
    require(stems.map{it.name}.distinct().size==STEMS.size) { "duplicate stem" }
    require(stems.all{it.bytes>44 && it.sha256.matches(RemoteJobIdentity.SHA)})
    require(stems.all{safePath(it.path)}) { "unsafe stem path" }
   }
   2 -> {
    require(stems.isEmpty()) { "v2 result must not expose intermediate stems" }
    require(deliverables.size==2 && deliverables.map{it.name}.toSet()==DELIVERABLES.keys) { "invalid prepared reference set" }
    require(deliverables.map{it.name}.distinct().size==2) { "duplicate prepared reference" }
    deliverables.forEach { artifact ->
     require(DELIVERABLES[artifact.name]==artifact.role) { "prepared reference role mismatch" }
     require(artifact.bytes>44 && artifact.sha256.matches(RemoteJobIdentity.SHA))
     require(artifact.sampleRate==sampleRate && artifact.channels==channels && artifact.frames==frames)
     require(artifact.encoding=="FLOAT32_LE")
     require(safePath(artifact.path) && artifact.path.contains("/output/prepared/")) { "unsafe prepared reference path" }
    }
    val recipe=requireNotNull(referenceRecipe) { "missing reference recipe" }
    require(recipe.version=="prepared-reference-v2")
    require(kotlin.math.abs(recipe.targetPeakDbfs-(-1.0))<0.000001 && recipe.sharedGainDb<=0.000001)
    require(recipe.backingStems.toSet()==BACKING_STEMS.toSet() && recipe.backingStems.size==BACKING_STEMS.size)
    require(recipe.guitarStem=="guitar")
   }
   else -> error("unsupported remote result schema: $schemaVersion")
  }
 }
 private fun safePath(path:String)=path.startsWith("remote/v1/")&&!path.contains("..")
 companion object {
  const val MODEL_SHA256="09704f4ceae204e56e77d5eefd6ac71d7275be81fd507e6913371d59abcee856"
  val STEMS=listOf("drums","bass","other","vocals","guitar","piano")
  val BACKING_STEMS=listOf("drums","bass","other","vocals","piano")
  val DELIVERABLES=mapOf("backing" to "REFERENCE_BACKING","guitar" to "REFERENCE_GUITAR")
 }
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
   RemoteJobState.COMPLETED->setOf(RemoteJobState.IMPORTING,RemoteJobState.IMPORT_FAILED,RemoteJobState.CANCEL_REQUESTED,RemoteJobState.EXPIRED)
   RemoteJobState.IMPORTING->setOf(RemoteJobState.COMPLETED,RemoteJobState.IMPORT_FAILED,RemoteJobState.IMPORTED,RemoteJobState.FAILED,RemoteJobState.EXPIRED)
   RemoteJobState.IMPORT_FAILED->setOf(RemoteJobState.COMPLETED,RemoteJobState.IMPORTING,RemoteJobState.CANCEL_REQUESTED,RemoteJobState.FAILED,RemoteJobState.EXPIRED)
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
  RemoteJobState.RUNNING->RemoteJobState.EXPIRED
  RemoteJobState.COMPLETED,RemoteJobState.IMPORTING,RemoteJobState.IMPORT_FAILED->RemoteJobState.IMPORT_FAILED
  else->null
 }
 fun failureTerminalState(state:RemoteJobState)=when(state){
  RemoteJobState.COMPLETED,RemoteJobState.IMPORTING,RemoteJobState.IMPORT_FAILED->RemoteJobState.IMPORT_FAILED
  else->RemoteJobState.FAILED
 }
}
interface RemoteJobStore { fun load(jobId:String):DurableRemoteJob?; fun save(job:DurableRemoteJob):DurableRemoteJob; fun active():List<DurableRemoteJob> = emptyList() }
interface RemoteSeparationBackend { suspend fun enqueue(identity:RemoteJobIdentity,inputPath:String); suspend fun status(identity:RemoteJobIdentity):DurableRemoteJob?; suspend fun cancel(identity:RemoteJobIdentity); suspend fun acknowledge(identity:RemoteJobIdentity,resultManifestSha256:String) }
interface RemoteStemPayload {
 val name:String
 val byteCount:Long
 fun openStream():java.io.InputStream
}
interface RemoteResultTransport {
 suspend fun uploadSource(identity:RemoteJobIdentity):String
 suspend fun downloadManifest(identity:RemoteJobIdentity):ByteArray
 suspend fun downloadStem(identity:RemoteJobIdentity,stem:RemoteStem):RemoteStemPayload
 suspend fun downloadReference(identity:RemoteJobIdentity,reference:RemoteReference):RemoteStemPayload =
  throw UnsupportedOperationException("prepared reference transport is not implemented")
 suspend fun cleanup(identity:RemoteJobIdentity)
}
interface RemoteStemPublisher {
 fun publish(identity:RemoteJobIdentity,manifest:RemoteResultManifest,manifestSha256:String,stems:Map<String,RemoteStemPayload>):Boolean
 fun publishReferences(identity:RemoteJobIdentity,manifest:RemoteResultManifest,manifestSha256:String,references:Map<String,RemoteStemPayload>):Boolean =
  throw UnsupportedOperationException("prepared reference publisher is not implemented")
}

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
   if(local.state in setOf(RemoteJobState.COMPLETED,RemoteJobState.IMPORTING,RemoteJobState.IMPORT_FAILED) && local.resultManifestSha256!=null){
    return importCompleted(identity,local,false)
   }
   RemoteMissingPolicy.terminalState(local.state)?.let { terminal ->
    return store.save(local.copy(state=terminal,updatedAtMs=nowMs(),errorCode=RemoteMissingPolicy.ERROR_REMOTE_JOB_NOT_FOUND))
   }
   return local
  }
  val accepted=store.save(remote)
  if(accepted.state!=RemoteJobState.COMPLETED&&accepted.state!=RemoteJobState.IMPORTING)return accepted
  return importCompleted(identity,accepted,true)
 }
 private suspend fun importCompleted(identity:RemoteJobIdentity,accepted:DurableRemoteJob,acknowledgeRemote:Boolean):DurableRemoteJob {
  store.save(accepted.copy(state=RemoteJobState.IMPORTING,updatedAtMs=nowMs(),errorCode=null))
  val manifestBytes=transport.downloadManifest(identity);val manifestSha=sha256(manifestBytes);accepted.resultManifestSha256?.let{require(it==manifestSha){"manifest checksum mismatch"}}
  val manifest=decodeManifest(manifestBytes);manifest.validateFor(identity)
  if(manifest.schemaVersion==1) {
   val stems=linkedMapOf<String,RemoteStemPayload>()
   manifest.stems.forEach { stem -> stems[stem.name]=transport.downloadStem(identity,stem) }
   require(stems.size==RemoteResultManifest.STEMS.size)
   publisher.publish(identity,manifest,manifestSha,stems)
  } else {
   val references=linkedMapOf<String,RemoteStemPayload>()
   manifest.deliverables.forEach { artifact -> references[artifact.name]=transport.downloadReference(identity,artifact) }
   require(references.size==RemoteResultManifest.DELIVERABLES.size)
   publisher.publishReferences(identity,manifest,manifestSha,references)
  }
  if(acknowledgeRemote) backend.acknowledge(identity,manifestSha)
  transport.cleanup(identity)
  return store.save(accepted.copy(state=RemoteJobState.IMPORTED,updatedAtMs=nowMs(),resultManifestSha256=manifestSha,errorCode=null))
 }
 suspend fun cancel(identity:RemoteJobIdentity):DurableRemoteJob { val current=requireNotNull(store.load(identity.jobId));require(current.identity==identity);if(current.state in setOf(RemoteJobState.IMPORTED,RemoteJobState.CANCELLED,RemoteJobState.FAILED,RemoteJobState.EXPIRED))return current;val requesting=store.save(current.copy(state=RemoteJobState.CANCEL_REQUESTED,updatedAtMs=nowMs()));backend.cancel(identity);return requesting }
 private fun sha256(bytes:ByteArray)=java.security.MessageDigest.getInstance("SHA-256").digest(bytes).joinToString(""){"%02x".format(it)}
}
object RemoteFailurePolicy {
 private val terminal=setOf("AUTH_REQUIRED","APP_CHECK_REJECTED","QUOTA_EXCEEDED","REMOTE_JOB_NOT_FOUND","CONFIG_INVALID","INPUT_MISSING","INPUT_HASH_MISMATCH","RESULT_INVALID")
 fun shouldRetry(code:String,attempt:Int,maxAttempts:Int=5)=attempt<maxAttempts && code !in terminal
}
