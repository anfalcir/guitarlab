package studio.guitarlab.platform.separation
import org.junit.Assert.*
import org.junit.Test
import studio.guitarlab.core.separation.*
import java.io.File
import java.nio.file.Files
import java.security.MessageDigest
import java.util.UUID
class ManagedStemSetPublisherTest {
 @Test fun atomicValidatedIdempotentPublish(){
  val root=Files.createTempDirectory("u4").toFile();val stage=File(root,"stage").apply{mkdirs()};fun h(b:ByteArray)=MessageDigest.getInstance("SHA-256").digest(b).joinToString(""){"%02x".format(it)}
  val rows=RemoteResultManifest.STEMS.map{n->val b=ByteArray(64){n.length.toByte()};File(stage,n+".wav").writeBytes(b);RemoteStem(n,"results/"+n+".wav",64,h(b))}
  val i=RemoteJobIdentity(UUID.randomUUID().toString(),"p1","s1","a".repeat(64))
  val stemJson=rows.joinToString(","){r->"{\"name\":\""+r.name+"\",\"path\":\""+r.path+"\",\"bytes\":"+r.bytes+",\"sha256\":\""+r.sha256+"\"}"}
  val json=("{\"schemaVersion\":1,\"jobId\":\""+i.jobId+"\",\"projectId\":\"p1\",\"inputSha256\":\""+i.inputSha256+"\",\"engine\":\"demucs.cpp\",\"engineRevision\":\"rc5\",\"model\":\"htdemucs_6s\",\"modelSha256\":\""+RemoteResultManifest.MODEL_SHA256+"\",\"sampleRate\":44100,\"channels\":2,\"frames\":44100,\"duration\":1.0,\"stems\":["+stemJson+"]}").toByteArray()
  val p=ManagedStemSetPublisher(File(root,"project").apply{mkdirs()});val a=p.publish(i,json,stage);val b=p.publish(i,json,stage);assertEquals(6,a.size);assertEquals(a,b);assertTrue(File(root,"project/media/stems/"+i.jobId+"/.complete").isFile)
 }
}
