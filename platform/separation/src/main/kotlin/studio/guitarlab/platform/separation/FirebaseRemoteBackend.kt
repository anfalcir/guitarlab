package studio.guitarlab.platform.separation
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.tasks.await
import studio.guitarlab.core.separation.*
class FirebaseRemoteBackend(private val auth:FirebaseAuth,private val db:FirebaseFirestore,private val functions:FirebaseFunctions):RemoteSeparationBackend {
 override suspend fun enqueue(i:RemoteJobIdentity,inputPath:String){user();functions.getHttpsCallable("enqueueRemoteSeparation").call(mapOf("jobId" to i.jobId,"projectId" to i.projectId,"inputSha256" to i.inputSha256,"inputPath" to inputPath)).await()}
 override suspend fun cancel(i:RemoteJobIdentity){user();functions.getHttpsCallable("cancelRemoteSeparation").call(mapOf("jobId" to i.jobId)).await()}
 override suspend fun acknowledge(i:RemoteJobIdentity,resultManifestSha256:String){user();require(resultManifestSha256.matches(RemoteJobIdentity.SHA));functions.getHttpsCallable("acknowledgeRemoteImport").call(mapOf("jobId" to i.jobId,"projectId" to i.projectId,"resultManifestSha256" to resultManifestSha256)).await()}
 override suspend fun status(i:RemoteJobIdentity):DurableRemoteJob?{val uid=user();val d=db.document("users/"+uid+"/jobs/"+i.jobId).get().await();if(!d.exists())return null;require(d.getString("projectId")==i.projectId&&d.getString("inputSha256")==i.inputSha256){"remote job ownership mismatch"};return DurableRemoteJob(i,RemoteJobState.valueOf(requireNotNull(d.getString("state"))),d.getTimestamp("updatedAt")?.toDate()?.time?:System.currentTimeMillis(),d.getString("resultManifestSha256"),d.getString("errorCode"))}
 private fun user()=requireNotNull(auth.currentUser?.uid){"AUTH_REQUIRED"}
}
