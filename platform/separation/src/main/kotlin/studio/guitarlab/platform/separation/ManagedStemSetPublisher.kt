package studio.guitarlab.platform.separation
import studio.guitarlab.core.separation.*
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.security.MessageDigest
data class PublishedStem(val role:String,val relativePath:String,val bytes:Long,val sha256:String)
class ManagedStemSetPublisher(private val projectDir:File) {
 fun publish(i:RemoteJobIdentity,manifestBytes:ByteArray,staging:File):List<PublishedStem>{
  val m=RemoteManifestCodec.decode(manifestBytes);m.validateFor(i);val dest=File(projectDir,"media/stems/"+i.jobId);val marker=File(dest,".complete")
  if(marker.isFile)return verified(dest,m)
  val tmp=File(projectDir,"media/stems/."+i.jobId+".publishing");tmp.deleteRecursively();tmp.mkdirs()
  try{m.stems.forEach{e->val src=File(staging,e.name+".wav");require(valid(src,e)){"invalid stem "+e.name};Files.copy(src.toPath(),File(tmp,e.name+".wav").toPath(),StandardCopyOption.REPLACE_EXISTING)}
   require(!dest.exists());dest.parentFile?.mkdirs();runCatching{Files.move(tmp.toPath(),dest.toPath(),StandardCopyOption.ATOMIC_MOVE)}.getOrElse{Files.move(tmp.toPath(),dest.toPath())};marker.writeText(hash(manifestBytes));return verified(dest,m)
  }catch(t:Throwable){tmp.deleteRecursively();throw t}
 }
 private fun verified(dir:File,m:RemoteResultManifest)=m.stems.map{e->val f=File(dir,e.name+".wav");require(valid(f,e));PublishedStem("STEM_"+e.name.uppercase(),"media/stems/"+m.jobId+"/"+e.name+".wav",e.bytes,e.sha256)}
 private fun valid(f:File,e:RemoteStem)=f.isFile&&f.length()==e.bytes&&fileHash(f)==e.sha256
 private fun fileHash(f:File):String{val d=MessageDigest.getInstance("SHA-256");f.inputStream().use{ins->val b=ByteArray(65536);while(true){val n=ins.read(b);if(n<0)break;d.update(b,0,n)}};return d.digest().joinToString(""){"%02x".format(it)}}
 private fun hash(b:ByteArray)=MessageDigest.getInstance("SHA-256").digest(b).joinToString(""){"%02x".format(it)}
}
