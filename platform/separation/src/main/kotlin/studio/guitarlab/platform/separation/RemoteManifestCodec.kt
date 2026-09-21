package studio.guitarlab.platform.separation
import org.json.JSONObject
import studio.guitarlab.core.separation.*
object RemoteManifestCodec {
 fun decode(bytes:ByteArray):RemoteResultManifest {
  require(bytes.size<=65536);val o=JSONObject(bytes.toString(Charsets.UTF_8));require(o.getInt("schemaVersion")==1);val a=o.getJSONArray("stems")
  val stems=(0 until a.length()).map{val s=a.getJSONObject(it);RemoteStem(s.getString("name"),s.getString("path"),s.getLong("bytes"),s.getString("sha256"))}
  return RemoteResultManifest(o.getString("jobId"),o.getString("projectId"),o.getString("inputSha256"),o.getString("engine"),o.getString("engineRevision"),o.getString("model"),o.getString("modelSha256"),o.getInt("sampleRate"),o.getInt("channels"),o.getLong("frames"),o.getDouble("duration"),stems)
 }
}
