package studio.guitarlab.platform.separation
import kotlinx.serialization.json.*
import studio.guitarlab.core.separation.*
object RemoteManifestCodec {
 fun decode(bytes:ByteArray):RemoteResultManifest {
  require(bytes.size in 2..65536);val o=Json.parseToJsonElement(bytes.toString(Charsets.UTF_8)).jsonObject;require(o.reqInt("schemaVersion")==1);val a=o.req("stems").jsonArray
  val stems=a.map{val s=it.jsonObject;RemoteStem(s.reqString("name"),s.reqString("path"),s.reqLong("bytes"),s.reqString("sha256"))}
  return RemoteResultManifest(o.reqString("jobId"),o.reqString("projectId"),o.reqString("inputSha256"),o.reqString("engine"),o.reqString("engineRevision"),o.reqString("model"),o.reqString("modelSha256"),o.reqInt("sampleRate"),o.reqInt("channels"),o.reqLong("frames"),o.req("duration").jsonPrimitive.double,stems)
 }
 private fun JsonObject.req(k:String)=requireNotNull(get(k)){"missing $k"}
 private fun JsonObject.reqString(k:String)=req(k).jsonPrimitive.content
 private fun JsonObject.reqInt(k:String)=req(k).jsonPrimitive.int
 private fun JsonObject.reqLong(k:String)=req(k).jsonPrimitive.long
}
