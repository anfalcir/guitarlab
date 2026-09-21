package studio.guitarlab.platform.separation

import android.content.Context
import android.net.Uri
import androidx.work.*
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import java.io.File
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.tasks.await
import studio.guitarlab.core.model.PreparationStatus
import studio.guitarlab.core.project.*
import studio.guitarlab.core.separation.*

internal object GuitarLabFirebase {
    fun app(context: Context): FirebaseApp = FirebaseApp.getApps(context).firstOrNull { it.name == NAME }
        ?: FirebaseApp.initializeApp(context, FirebaseOptions.Builder()
            .setApplicationId("1:119299736855:android:0ca2542a90f42ad7526352")
            .setProjectId("gbwapp-ef048")
            .setStorageBucket("gbwapp-ef048.firebasestorage.app")
            .setApiKey("AIzaSyAQPXzWCX6sUNv2UkI1t5yEH-7Vvry130A")
            .build(), NAME)
    const val NAME = "guitarlab-separation"
}

internal class FirebaseResultTransport(private val context:Context,private val auth:FirebaseAuth,private val storage:FirebaseStorage):RemoteResultTransport {
    private suspend fun uid():String { auth.currentUser?.uid?.let{return it};return requireNotNull(auth.signInAnonymously().await().user?.uid){"AUTH_REQUIRED"} }
    override suspend fun uploadSource(identity:RemoteJobIdentity):String {
        val project=requireNotNull(FileProjectRepository(context.filesDir).load(identity.projectId));val source=project.assets.singleOrNull{it.assetId==identity.sourceAssetId}?:error("INPUT_MISSING");require(source.sha256==identity.inputSha256){"INPUT_HASH_MISMATCH"}
        val file=ProjectManagedMediaStore(context.filesDir).resolve(identity.projectId,source.relativePath);val path="remote/v1/users/${uid()}/jobs/${identity.jobId}/input/source.${source.format.lowercase()}"
        val metadata=StorageMetadata.Builder().setContentType("audio/${source.format.lowercase()}").setCustomMetadata("sha256",identity.inputSha256).setCustomMetadata("projectId",identity.projectId).setCustomMetadata("jobId",identity.jobId).build()
        storage.reference.child(path).putFile(Uri.fromFile(file),metadata).await();return path
    }
    override suspend fun downloadManifest(identity:RemoteJobIdentity)=storage.reference.child(prefix(identity)+"/result-manifest.json").getBytes(65536).await()
    override suspend fun downloadStem(identity:RemoteJobIdentity,stem:RemoteStem):ByteArray { require(stem.path.startsWith(prefix(identity)+"/")&&!stem.path.contains(".."));return storage.reference.child(stem.path).getBytes(stem.bytes).await() }
    override suspend fun cleanup(identity:RemoteJobIdentity) { runCatching{storage.reference.child(prefix(identity)).listAll().await().items.forEach{runCatching{it.delete().await()}}} }
    private suspend fun prefix(identity:RemoteJobIdentity)="remote/v1/users/${uid()}/jobs/${identity.jobId}/output"
}

class RemoteSeparationWorker(context:Context,params:WorkerParameters):CoroutineWorker(context,params) {
    override suspend fun doWork():Result {
        val id=runCatching{RemoteJobIdentity(requireNotNull(inputData.getString("jobId")),requireNotNull(inputData.getString("projectId")),requireNotNull(inputData.getString("sourceAssetId")),requireNotNull(inputData.getString("inputSha256")))}.getOrElse{return Result.failure()}
        val app=GuitarLabFirebase.app(applicationContext);val auth=FirebaseAuth.getInstance(app);val store=FileRemoteJobStore(applicationContext);val coordinator=RemoteSeparationCoordinator(store,FirebaseRemoteBackend(auth,FirebaseFirestore.getInstance(app),FirebaseFunctions.getInstance(app,"us-central1")),FirebaseResultTransport(applicationContext,auth,FirebaseStorage.getInstance(app)),RemoteManifestCodec::decode,ManagedStemSetPublisher(StemSetProjectPublisher(FileProjectRepository(applicationContext.filesDir),ProjectManagedMediaStore(applicationContext.filesDir))))
        return try { val current=store.load(id.jobId);val next=if(current==null)coordinator.start(id) else coordinator.reconcile(id);when(next.state){RemoteJobState.IMPORTED,RemoteJobState.CANCELLED->Result.success();RemoteJobState.FAILED,RemoteJobState.EXPIRED->Result.failure();else->Result.retry()} } catch(e:Throwable){if(RemoteFailurePolicy.shouldRetry(sanitize(e),runAttemptCount,20))Result.retry() else Result.failure()}
    }
    private fun sanitize(e:Throwable)=e.message?.substringBefore(':')?.take(48)?.replace(Regex("[^A-Z0-9_]"),"_")?:"BACKEND_UNAVAILABLE"
}

class RemoteSeparationClient(private val context:Context) {
    fun enqueue(projectId:String):String {
        val repo=FileProjectRepository(context.filesDir);val project=requireNotNull(repo.load(projectId));val sourceId=requireNotNull(project.preparation?.sourceAssetId){"Fonte gerenciada necessária."};val source=project.assets.single{it.assetId==sourceId};val jobId=UUID.randomUUID().toString();val id=RemoteJobIdentity(jobId,projectId,sourceId,source.sha256)
        repo.save(project.copy(updatedAtEpochMs=System.currentTimeMillis(),preparation=requireNotNull(project.preparation).copy(status=PreparationStatus.SEPARATING)))
        val request=OneTimeWorkRequestBuilder<RemoteSeparationWorker>().setInputData(workDataOf("jobId" to id.jobId,"projectId" to id.projectId,"sourceAssetId" to id.sourceAssetId,"inputSha256" to id.inputSha256)).setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()).setBackoffCriteria(BackoffPolicy.EXPONENTIAL,30,TimeUnit.SECONDS).addTag("guitarlab-separation").addTag("guitarlab-separation:$projectId").build()
        WorkManager.getInstance(context).enqueueUniqueWork("guitarlab-separation:$projectId",ExistingWorkPolicy.KEEP,request);return jobId
    }
    fun snapshot(jobId:String)=FileRemoteJobStore(context).load(jobId)
    fun snapshotProject(projectId:String)=FileRemoteJobStore(context).active().filter{it.identity.projectId==projectId}.maxByOrNull{it.updatedAtMs}
    fun cancel(projectId:String,jobId:String){val store=FileRemoteJobStore(context);val job=store.load(jobId)?:return;store.save(job.copy(state=RemoteJobState.CANCEL_REQUESTED,updatedAtMs=System.currentTimeMillis()));val request=OneTimeWorkRequestBuilder<RemoteCancelWorker>().setInputData(workDataOf("jobId" to jobId,"projectId" to projectId,"sourceAssetId" to job.identity.sourceAssetId,"inputSha256" to job.identity.inputSha256)).setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()).build();WorkManager.getInstance(context).enqueueUniqueWork("guitarlab-separation-cancel:$jobId",ExistingWorkPolicy.KEEP,request)}
    fun resumePending(){FileRemoteJobStore(context).active().filter{it.state !in setOf(RemoteJobState.IMPORTED,RemoteJobState.CANCELLED,RemoteJobState.FAILED,RemoteJobState.EXPIRED)}.forEach{job->val id=job.identity;val request=OneTimeWorkRequestBuilder<RemoteSeparationWorker>().setInputData(workDataOf("jobId" to id.jobId,"projectId" to id.projectId,"sourceAssetId" to id.sourceAssetId,"inputSha256" to id.inputSha256)).setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()).setBackoffCriteria(BackoffPolicy.EXPONENTIAL,30,TimeUnit.SECONDS).build();WorkManager.getInstance(context).enqueueUniqueWork("guitarlab-separation:${id.projectId}",ExistingWorkPolicy.KEEP,request)}}
}

class RemoteCancelWorker(context:Context,params:WorkerParameters):CoroutineWorker(context,params){override suspend fun doWork():Result{val id=runCatching{RemoteJobIdentity(requireNotNull(inputData.getString("jobId")),requireNotNull(inputData.getString("projectId")),requireNotNull(inputData.getString("sourceAssetId")),requireNotNull(inputData.getString("inputSha256")))}.getOrElse{return Result.failure()};return runCatching{val app=GuitarLabFirebase.app(applicationContext);val auth=FirebaseAuth.getInstance(app);FirebaseRemoteBackend(auth,FirebaseFirestore.getInstance(app),FirebaseFunctions.getInstance(app,"us-central1")).cancel(id);Result.success()}.getOrElse{Result.retry()}}}
