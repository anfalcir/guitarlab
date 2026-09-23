package studio.guitarlab.core.project

import java.io.InputStream
import java.security.MessageDigest
import java.util.UUID
import studio.guitarlab.core.model.*

data class ValidatedStem(val name:String,val byteCount:Long,val sha256:String,val sampleRate:Int,val channels:Int,val frames:Long,val openStream:()->InputStream)
data class StemSetPublicationRequest(val projectId:String,val jobId:String,val sourceAssetId:String,val sourceSha256:String,val manifestSha256:String,val engine:String,val model:String,val modelSha256:String,val stems:List<ValidatedStem>)

class StemSetProjectPublisher(private val repository:ProjectRepository,private val mediaStore:ProjectManagedMediaStore,private val nowMs:()->Long=System::currentTimeMillis,private val idFactory:()->String={UUID.randomUUID().toString()}) {
 fun publish(r:StemSetPublicationRequest):Boolean {
  require(r.jobId.isNotBlank()&&r.manifestSha256.matches(Regex("[a-f0-9]{64}")));require(r.stems.map{it.name}.toSet()==ROLES.keys&&r.stems.size==ROLES.size)
  r.stems.forEach{require(it.byteCount>44&&it.sha256.matches(Regex("[a-f0-9]{64}"))&&it.sampleRate==44100&&it.channels==2&&it.frames>0)}
  val before=requireNotNull(repository.load(r.projectId));val source=before.assets.singleOrNull{it.assetId==r.sourceAssetId}?:error("source asset missing");require(source.sha256==r.sourceSha256&&before.preparation?.sourceAssetId==r.sourceAssetId){"source ownership mismatch"}
  val prior=before.assets.count{it.provenance?.parameters?.get("jobId")==r.jobId};if(prior==ROLES.size)return true;require(prior==0){"partial prior publication"}
  mediaStore.discardAbandonedStemSet(r.projectId,r.jobId)
  val ingested=mutableListOf<Pair<ValidatedStem,ManagedMediaAsset>>()
  try {
   r.stems.sortedBy{it.name}.forEach{stem->\n    val managed=stem.openStream().use{input->mediaStore.ingestStem(r.projectId,"${r.jobId}-${stem.name}.wav",input)}\n    ingested+=stem to managed\n    require(managed.byteCount==stem.byteCount&&sha256(managed.file)==stem.sha256){"managed stem integrity mismatch: ${stem.name}"}\n   }
   val current=requireNotNull(repository.load(r.projectId));require(current.preparation?.sourceAssetId==r.sourceAssetId&&current.assets.any{it.assetId==r.sourceAssetId&&it.sha256==r.sourceSha256}){"source changed before commit"}
   val assets=ingested.map{(s,m)->ManagedAsset(idFactory(),requireNotNull(ROLES[s.name]),m.relativePath,s.sha256,m.byteCount,"wav",s.sampleRate,s.channels,s.frames,nowMs(),AssetClassification.AUTHORITATIVE,AssetLifecycle.MANAGED,AssetProvenance("REMOTE_SEPARATION",listOf(r.sourceAssetId),listOf(r.sourceSha256),r.engine,r.model,mapOf("jobId" to r.jobId,"manifestSha256" to r.manifestSha256,"modelSha256" to r.modelSha256),1,"REMOTE"))}
   repository.save(current.copy(updatedAtEpochMs=nowMs(),assets=current.assets+assets,preparation=requireNotNull(current.preparation).copy(status=PreparationStatus.READY,activeStemAssetIds=assets.associate{it.role to it.assetId})))
   return false
  } catch(t:Throwable){ingested.forEach{(_,m)->runCatching{mediaStore.discardUncommitted(r.projectId,m.relativePath)}};throw t}
 }
 private fun sha256(file:java.io.File):String{\n  val digest=MessageDigest.getInstance("SHA-256")\n  file.inputStream().use{input->val buffer=ByteArray(DEFAULT_BUFFER_SIZE);while(true){val read=input.read(buffer);if(read<0)break;if(read>0)digest.update(buffer,0,read)}}\n  return digest.digest().joinToString(""){"%02x".format(it)}\n }
 companion object { val ROLES=mapOf("drums" to AssetRole.STEM_DRUMS,"bass" to AssetRole.STEM_BASS,"other" to AssetRole.STEM_OTHER,"vocals" to AssetRole.STEM_VOCALS,"guitar" to AssetRole.STEM_GUITAR,"piano" to AssetRole.STEM_PIANO) }
}
