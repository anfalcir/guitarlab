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
import kotlinx.coroutines.flow.Flow
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

class RemoteSeparationWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val id = runCatching {
            RemoteJobIdentity(
                requireNotNull(inputData.getString("jobId")),
                requireNotNull(inputData.getString("projectId")),
                requireNotNull(inputData.getString("sourceAssetId")),
                requireNotNull(inputData.getString("inputSha256")),
            )
        }.getOrElse { return Result.failure() }
        val app = GuitarLabFirebase.app(applicationContext)
        val auth = FirebaseAuth.getInstance(app)
        val store = FileRemoteJobStore(applicationContext)
        val repository = FileProjectRepository(applicationContext.filesDir)
        val mediaStore = ProjectManagedMediaStore(applicationContext.filesDir)
        val coordinator = RemoteSeparationCoordinator(
            store,
            FirebaseRemoteBackend(auth, FirebaseFirestore.getInstance(app), FirebaseFunctions.getInstance(app, "us-central1")),
            FirebaseResultTransport(applicationContext, auth, FirebaseStorage.getInstance(app)),
            RemoteManifestCodec::decode,
            ManagedStemSetPublisher(StemSetProjectPublisher(repository, mediaStore)),
        )
        return try {
            val current = store.load(id.jobId)
            val next = if (current == null) coordinator.start(id) else coordinator.reconcile(id)
            when (next.state) {
                RemoteJobState.IMPORTED -> prepareReferencesOrRetry(id, next, store, repository, mediaStore)
                RemoteJobState.CANCELLED -> Result.success()
                RemoteJobState.FAILED, RemoteJobState.EXPIRED -> Result.failure()
                else -> Result.retry()
            }
        } catch (error: Throwable) {
            if (RemoteFailurePolicy.shouldRetry(sanitize(error), runAttemptCount, 20)) Result.retry() else Result.failure()
        }
    }

    private fun prepareReferencesOrRetry(
        id: RemoteJobIdentity,
        importedJob: DurableRemoteJob,
        store: FileRemoteJobStore,
        repository: FileProjectRepository,
        mediaStore: ProjectManagedMediaStore,
    ): Result = try {
        PreparedReferenceService(repository, mediaStore, applicationContext.cacheDir).prepare(id.projectId)
        store.save(importedJob.copy(updatedAtMs = System.currentTimeMillis()))
        Result.success()
    } catch (error: Throwable) {
        markReferencePreparationError(id, repository)
        store.save(importedJob.copy(updatedAtMs = System.currentTimeMillis(), errorCode = "REFERENCE_PREPARATION_FAILED"))
        Result.failure()
    }

    private fun markReferencePreparationError(id: RemoteJobIdentity, repository: FileProjectRepository) {
        val project = repository.load(id.projectId) ?: return
        val preparation = project.preparation ?: return
        val sameGeneration = preparation.sourceAssetId == id.sourceAssetId && preparation.activeStemAssetIds.size == 6
        val referencesMissing = preparation.activeBackingAssetId == null || preparation.activeGuitarAssetId == null
        if (sameGeneration && referencesMissing) {
            repository.save(
                project.copy(
                    updatedAtEpochMs = System.currentTimeMillis(),
                    preparation = preparation.copy(status = PreparationStatus.ERROR),
                ),
            )
        }
    }

    private fun sanitize(error: Throwable) = error.message?.substringBefore(':')?.take(48)
        ?.replace(Regex("[^A-Z0-9_]"), "_") ?: "BACKEND_UNAVAILABLE"

}

class RemoteSeparationClient(private val context: Context) {
    fun enqueue(projectId: String): String {
        val repo = FileProjectRepository(context.filesDir)
        val project = requireNotNull(repo.load(projectId))
        val sourceId = requireNotNull(project.preparation?.sourceAssetId) { "Fonte gerenciada necessária." }
        val source = project.assets.single { it.assetId == sourceId }
        val jobId = UUID.randomUUID().toString()
        val id = RemoteJobIdentity(jobId, projectId, sourceId, source.sha256)
        FileRemoteJobStore(context).save(DurableRemoteJob(id, RemoteJobState.UPLOADING, System.currentTimeMillis()))
        repo.save(
            project.copy(
                updatedAtEpochMs = System.currentTimeMillis(),
                preparation = requireNotNull(project.preparation).copy(status = PreparationStatus.SEPARATING),
            ),
        )
        val request = OneTimeWorkRequestBuilder<RemoteSeparationWorker>()
            .setInputData(workDataOf("jobId" to id.jobId, "projectId" to id.projectId, "sourceAssetId" to id.sourceAssetId, "inputSha256" to id.inputSha256))
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .addTag("guitarlab-separation")
            .addTag("guitarlab-separation:$projectId")
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(workName(id.jobId), ExistingWorkPolicy.KEEP, request)
        return jobId
    }

    fun snapshot(jobId: String) = FileRemoteJobStore(context).load(jobId)
    fun snapshotProject(projectId: String) = FileRemoteJobStore(context).latestForProject(projectId)
    fun observeProject(projectId: String): Flow<DurableRemoteJob?> = FileRemoteJobStore(context).observeProject(projectId)

    fun cancel(projectId: String, jobId: String) {
        val store = FileRemoteJobStore(context)
        val job = store.load(jobId) ?: return
        store.save(job.copy(state = RemoteJobState.CANCEL_REQUESTED, updatedAtMs = System.currentTimeMillis()))
        val request = OneTimeWorkRequestBuilder<RemoteCancelWorker>()
            .setInputData(workDataOf("jobId" to jobId, "projectId" to projectId, "sourceAssetId" to job.identity.sourceAssetId, "inputSha256" to job.identity.inputSha256))
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()
        WorkManager.getInstance(context).cancelUniqueWork(workName(jobId))
        WorkManager.getInstance(context).enqueueUniqueWork(cancelWorkName(jobId), ExistingWorkPolicy.REPLACE, request)
    }

    fun closeProject(projectId: String) {
        val store = FileRemoteJobStore(context)
        store.active()
            .filter { it.identity.projectId == projectId && it.state !in TERMINAL_STATES }
            .forEach { cancel(projectId, it.identity.jobId) }
        store.active().filter { it.identity.projectId == projectId }.forEach {
            WorkManager.getInstance(context).cancelUniqueWork(workName(it.identity.jobId))
            WorkManager.getInstance(context).cancelUniqueWork(cancelWorkName(it.identity.jobId))
        }
        store.removeProject(projectId)
    }

    fun resumePending() {
        val store = FileRemoteJobStore(context)
        recoverOrphanedProjects(store)
        store.active()
            .filter { it.state !in TERMINAL_STATES }
            .forEach { job ->
                val id = job.identity
                if (job.state == RemoteJobState.CANCEL_REQUESTED) {
                    cancel(id.projectId, id.jobId)
                    return@forEach
                }
                val request = OneTimeWorkRequestBuilder<RemoteSeparationWorker>()
                    .setInputData(workDataOf("jobId" to id.jobId, "projectId" to id.projectId, "sourceAssetId" to id.sourceAssetId, "inputSha256" to id.inputSha256))
                    .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                    .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                    .build()
                WorkManager.getInstance(context).enqueueUniqueWork(workName(id.jobId), ExistingWorkPolicy.KEEP, request)
            }
    }

    private fun recoverOrphanedProjects(store: FileRemoteJobStore) {
        val repository = FileProjectRepository(context.filesDir)
        val activeProjectIds = store.active().filter { it.state !in TERMINAL_STATES }.map { it.identity.projectId }.toSet()
        repository.list().filter { project ->
            RemoteRecoveryPolicy.shouldRestoreSourceReady(project.preparation?.status, project.id in activeProjectIds)
        }.forEach { project ->
            repository.save(project.copy(
                updatedAtEpochMs = System.currentTimeMillis(),
                preparation = requireNotNull(project.preparation).copy(status = PreparationStatus.SOURCE_READY),
            ))
        }
    }

    private companion object {
        val TERMINAL_STATES = setOf(RemoteJobState.IMPORTED, RemoteJobState.CANCELLED, RemoteJobState.FAILED, RemoteJobState.EXPIRED)
        fun workName(jobId: String) = "guitarlab-separation:$jobId"
        fun cancelWorkName(jobId: String) = "guitarlab-separation-cancel:$jobId"
    }
}

class RemoteCancelWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val id = runCatching { RemoteJobIdentity(requireNotNull(inputData.getString("jobId")), requireNotNull(inputData.getString("projectId")), requireNotNull(inputData.getString("sourceAssetId")), requireNotNull(inputData.getString("inputSha256"))) }.getOrElse { return Result.failure() }
        return runCatching {
            val app = GuitarLabFirebase.app(applicationContext)
            val backend = FirebaseRemoteBackend(FirebaseAuth.getInstance(app), FirebaseFirestore.getInstance(app), FirebaseFunctions.getInstance(app, "us-central1"))
            backend.cancel(id)
            FileRemoteJobStore(applicationContext).save(DurableRemoteJob(id, RemoteJobState.CANCELLED, System.currentTimeMillis()))
            restoreSourceReady(id.projectId)
            Result.success()
        }.getOrElse { if (runAttemptCount < 20) Result.retry() else Result.failure() }
    }

    private fun restoreSourceReady(projectId: String) {
        val repository = FileProjectRepository(applicationContext.filesDir)
        val project = repository.load(projectId) ?: return
        val preparation = project.preparation ?: return
        if (preparation.status == PreparationStatus.SEPARATING) repository.save(project.copy(
            updatedAtEpochMs = System.currentTimeMillis(),
            preparation = preparation.copy(status = PreparationStatus.SOURCE_READY),
        ))
    }
}
