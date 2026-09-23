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
import kotlinx.coroutines.CancellationException
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
        val id = remoteIdentityFrom(inputData) ?: return Result.failure()
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
                RemoteJobState.CANCELLED -> {
                    restoreSourceReadyAfterTerminal(id, store, repository)
                    Result.success()
                }
                RemoteJobState.FAILED, RemoteJobState.EXPIRED -> {
                    restoreSourceReadyAfterTerminal(id, store, repository)
                    Result.failure()
                }
                else -> Result.retry()
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Throwable) {
            val code = sanitizeRemoteFailure(error)
            if (RemoteFailurePolicy.shouldRetry(code, runAttemptCount, MAX_WORKER_ATTEMPTS)) {
                Result.retry()
            } else {
                terminalizeExhaustedWorker(id, code, store, repository)
                Result.failure()
            }
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

    private companion object {
        const val MAX_WORKER_ATTEMPTS = 20
    }
}

class RemoteSeparationClient(private val context: Context) {
    fun enqueue(projectId: String): String {
        val repo = FileProjectRepository(context.filesDir)
        val store = FileRemoteJobStore(context)
        val existing = store.active().firstOrNull {
            it.identity.projectId == projectId && it.state !in RemoteRecoveryPolicy.terminalStates
        }
        require(existing == null) { "Já existe uma separação em andamento para este projeto." }

        val project = requireNotNull(repo.load(projectId))
        val sourceId = requireNotNull(project.preparation?.sourceAssetId) { "Fonte gerenciada necessária." }
        val source = project.assets.single { it.assetId == sourceId }
        val jobId = UUID.randomUUID().toString()
        val id = RemoteJobIdentity(jobId, projectId, sourceId, source.sha256)
        store.save(DurableRemoteJob(id, RemoteJobState.UPLOADING, System.currentTimeMillis()))
        repo.save(
            project.copy(
                updatedAtEpochMs = System.currentTimeMillis(),
                preparation = requireNotNull(project.preparation).copy(status = PreparationStatus.SEPARATING),
            ),
        )
        WorkManager.getInstance(context).enqueueUniqueWork(
            workName(id.jobId),
            ExistingWorkPolicy.KEEP,
            separationRequest(id),
        )
        return jobId
    }

    fun snapshot(jobId: String) = FileRemoteJobStore(context).load(jobId)
    fun snapshotProject(projectId: String) = FileRemoteJobStore(context).latestForProject(projectId)
    fun observeProject(projectId: String): Flow<DurableRemoteJob?> = FileRemoteJobStore(context).observeProject(projectId)

    fun cancel(projectId: String, jobId: String) {
        val store = FileRemoteJobStore(context)
        val job = store.load(jobId) ?: return
        if (job.state in RemoteRecoveryPolicy.terminalStates) return
        val requesting = store.save(job.copy(state = RemoteJobState.CANCEL_REQUESTED, updatedAtMs = System.currentTimeMillis()))
        scheduleCancel(requesting)
    }

    fun closeProject(projectId: String) {
        val store = FileRemoteJobStore(context)
        store.active()
            .filter { it.identity.projectId == projectId && it.state !in RemoteRecoveryPolicy.terminalStates }
            .forEach { cancel(projectId, it.identity.jobId) }
        store.active().filter { it.identity.projectId == projectId }.forEach {
            WorkManager.getInstance(context).cancelUniqueWork(workName(it.identity.jobId))
            WorkManager.getInstance(context).cancelUniqueWork(cancelWorkName(it.identity.jobId))
        }
        store.removeProject(projectId)
    }

    fun resumePending() {
        val store = FileRemoteJobStore(context)
        recoverProjectsWithoutLocalJob(store)
        store.active()
            .filter { it.state !in RemoteRecoveryPolicy.terminalStates }
            .forEach { job ->
                if (job.state == RemoteJobState.CANCEL_REQUESTED) {
                    scheduleCancel(job)
                } else {
                    WorkManager.getInstance(context).enqueueUniqueWork(
                        workName(job.identity.jobId),
                        ExistingWorkPolicy.KEEP,
                        separationRequest(job.identity),
                    )
                }
            }
    }

    private fun scheduleCancel(job: DurableRemoteJob) {
        val id = job.identity
        WorkManager.getInstance(context).cancelUniqueWork(workName(id.jobId))
        WorkManager.getInstance(context).enqueueUniqueWork(
            cancelWorkName(id.jobId),
            ExistingWorkPolicy.REPLACE,
            cancelRequest(id),
        )
    }

    private fun recoverProjectsWithoutLocalJob(store: FileRemoteJobStore) {
        val repository = FileProjectRepository(context.filesDir)
        val activeProjectIds = store.active()
            .filter { it.state !in RemoteRecoveryPolicy.terminalStates }
            .map { it.identity.projectId }
            .toSet()
        repository.list()
            .filter { project ->
                RemoteRecoveryPolicy.shouldRestoreSourceReady(
                    project.preparation?.status,
                    project.id in activeProjectIds,
                )
            }
            .forEach { project ->
                repository.save(
                    project.copy(
                        updatedAtEpochMs = System.currentTimeMillis(),
                        preparation = requireNotNull(project.preparation).copy(status = PreparationStatus.SOURCE_READY),
                    ),
                )
            }
    }

    private fun separationRequest(id: RemoteJobIdentity) =
        OneTimeWorkRequestBuilder<RemoteSeparationWorker>()
            .setInputData(remoteWorkData(id))
            .setConstraints(networkConstraints())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .addTag("guitarlab-separation")
            .addTag("guitarlab-separation:${id.projectId}")
            .build()

    private fun cancelRequest(id: RemoteJobIdentity) =
        OneTimeWorkRequestBuilder<RemoteCancelWorker>()
            .setInputData(remoteWorkData(id))
            .setConstraints(networkConstraints())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .addTag("guitarlab-separation-cancel")
            .addTag("guitarlab-separation-cancel:${id.projectId}")
            .build()

    private companion object {
        fun workName(jobId: String) = "guitarlab-separation:$jobId"
        fun cancelWorkName(jobId: String) = "guitarlab-separation-cancel:$jobId"
    }
}

class RemoteCancelWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val id = remoteIdentityFrom(inputData) ?: return Result.failure()
        val app = GuitarLabFirebase.app(applicationContext)
        val backend = FirebaseRemoteBackend(
            FirebaseAuth.getInstance(app),
            FirebaseFirestore.getInstance(app),
            FirebaseFunctions.getInstance(app, "us-central1"),
        )
        val store = FileRemoteJobStore(applicationContext)
        val repository = FileProjectRepository(applicationContext.filesDir)

        return try {
            val remote = backend.status(id)
            when (remote?.state) {
                null -> {
                    store.save(
                        DurableRemoteJob(
                            id,
                            RemoteJobState.CANCELLED,
                            System.currentTimeMillis(),
                            errorCode = RemoteMissingPolicy.ERROR_REMOTE_JOB_NOT_FOUND,
                        ),
                    )
                }
                RemoteJobState.CANCELLED -> store.save(
                    DurableRemoteJob(id, RemoteJobState.CANCELLED, System.currentTimeMillis(), errorCode = remote.errorCode),
                )
                RemoteJobState.FAILED -> store.save(
                    DurableRemoteJob(id, RemoteJobState.FAILED, System.currentTimeMillis(), errorCode = remote.errorCode),
                )
                RemoteJobState.EXPIRED -> store.save(
                    DurableRemoteJob(id, RemoteJobState.EXPIRED, System.currentTimeMillis(), errorCode = remote.errorCode),
                )
                RemoteJobState.IMPORTED -> store.save(
                    DurableRemoteJob(id, RemoteJobState.FAILED, System.currentTimeMillis(), errorCode = "REMOTE_ALREADY_IMPORTED"),
                )
                else -> {
                    backend.cancel(id)
                    store.save(DurableRemoteJob(id, RemoteJobState.CANCELLED, System.currentTimeMillis()))
                }
            }
            restoreSourceReadyAfterTerminal(id, store, repository)
            Result.success()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Throwable) {
            val code = sanitizeRemoteFailure(error)
            when (RemoteRecoveryPolicy.cancellationFailureAction(code, runAttemptCount, MAX_CANCEL_ATTEMPTS)) {
                CancellationFailureAction.RETRY -> Result.retry()
                CancellationFailureAction.TERMINAL_FAILURE -> {
                    val current = store.load(id.jobId)
                    if (current != null && current.state !in RemoteRecoveryPolicy.terminalStates) {
                        store.save(
                            current.copy(
                                state = RemoteJobState.FAILED,
                                updatedAtMs = System.currentTimeMillis(),
                                errorCode = "CANCEL_UNCONFIRMED_${code.take(40)}",
                            ),
                        )
                    }
                    restoreSourceReadyAfterTerminal(id, store, repository)
                    Result.failure()
                }
            }
        }
    }

    private companion object {
        const val MAX_CANCEL_ATTEMPTS = 8
    }
}

private fun remoteIdentityFrom(data: Data): RemoteJobIdentity? = runCatching {
    RemoteJobIdentity(
        requireNotNull(data.getString("jobId")),
        requireNotNull(data.getString("projectId")),
        requireNotNull(data.getString("sourceAssetId")),
        requireNotNull(data.getString("inputSha256")),
    )
}.getOrNull()

private fun remoteWorkData(id: RemoteJobIdentity) = workDataOf(
    "jobId" to id.jobId,
    "projectId" to id.projectId,
    "sourceAssetId" to id.sourceAssetId,
    "inputSha256" to id.inputSha256,
)

private fun networkConstraints() =
    Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()

private fun restoreSourceReadyAfterTerminal(
    id: RemoteJobIdentity,
    store: FileRemoteJobStore,
    repository: FileProjectRepository,
) {
    val project = repository.load(id.projectId) ?: return
    val preparation = project.preparation ?: return
    val hasOtherActiveJob = store.active().any { candidate ->
        candidate.identity.projectId == id.projectId &&
            candidate.identity.jobId != id.jobId &&
            candidate.state !in RemoteRecoveryPolicy.terminalStates
    }
    if (
        RemoteRecoveryPolicy.shouldRestoreAfterTerminalJob(
            preparation.status,
            preparation.sourceAssetId,
            id.sourceAssetId,
            hasOtherActiveJob,
        )
    ) {
        repository.save(
            project.copy(
                updatedAtEpochMs = System.currentTimeMillis(),
                preparation = preparation.copy(status = PreparationStatus.SOURCE_READY),
            ),
        )
    }
}

private fun terminalizeExhaustedWorker(
    id: RemoteJobIdentity,
    code: String,
    store: FileRemoteJobStore,
    repository: FileProjectRepository,
) {
    val current = store.load(id.jobId) ?: return
    if (current.state in RemoteRecoveryPolicy.terminalStates) {
        restoreSourceReadyAfterTerminal(id, store, repository)
        return
    }
    val terminalState = RemoteMissingPolicy.failureTerminalState(current.state)
    store.save(
        current.copy(
            state = terminalState,
            updatedAtMs = System.currentTimeMillis(),
            errorCode = "WORKER_RETRY_EXHAUSTED_${code.take(40)}",
        ),
    )
    restoreSourceReadyAfterTerminal(id, store, repository)
}

private fun sanitizeRemoteFailure(error: Throwable): String =
    error.message
        ?.substringBefore(':')
        ?.take(48)
        ?.uppercase()
        ?.replace(Regex("[^A-Z0-9_]"), "_")
        ?.trim('_')
        ?.ifBlank { null }
        ?: "BACKEND_UNAVAILABLE"
