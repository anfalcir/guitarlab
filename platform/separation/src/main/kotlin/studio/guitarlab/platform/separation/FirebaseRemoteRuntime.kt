package studio.guitarlab.platform.separation

import android.content.Context
import android.net.Uri
import android.util.Log
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

internal class FirebaseResultTransport(
    private val context: Context,
    private val auth: FirebaseAuth,
    private val storage: FirebaseStorage,
) : RemoteResultTransport {
    private val staging = RemoteStemStaging(File(context.cacheDir, "remote-separation"))

    private suspend fun uid(): String = staged(RemotePipelineStage.AUTHENTICATING) {
        requireStableRemoteUid(auth)
    }

    override suspend fun uploadSource(identity: RemoteJobIdentity): String = staged(RemotePipelineStage.UPLOADING) {
        val project = requireNotNull(FileProjectRepository(context.filesDir).load(identity.projectId))
        val source = project.assets.singleOrNull { it.assetId == identity.sourceAssetId } ?: error("INPUT_MISSING")
        require(source.sha256 == identity.inputSha256) { "INPUT_HASH_MISMATCH" }
        val file = ProjectManagedMediaStore(context.filesDir).resolve(identity.projectId, source.relativePath)
        require(file.isFile && file.length() > 0L) { "INPUT_MISSING" }
        val path = "remote/v1/users/${uid()}/jobs/${identity.jobId}/input/source.${source.format.lowercase()}"
        val metadata = StorageMetadata.Builder()
            .setContentType("audio/${source.format.lowercase()}")
            .setCustomMetadata("sha256", identity.inputSha256)
            .setCustomMetadata("projectId", identity.projectId)
            .setCustomMetadata("jobId", identity.jobId)
            .build()
        storage.reference.child(path).putFile(Uri.fromFile(file), metadata).await()
        path
    }

    override suspend fun downloadManifest(identity: RemoteJobIdentity): ByteArray =
        staged(RemotePipelineStage.DOWNLOADING_RESULTS) {
            staging.cachedManifest(identity)?.let { return@staged it }
            val bytes = storage.reference.child(prefix(identity) + "/result-manifest.json").getBytes(65536).await()
            staging.commitManifest(identity, bytes)
        }

    override suspend fun downloadStem(identity: RemoteJobIdentity, stem: RemoteStem): RemoteStemPayload =
        staged(RemotePipelineStage.DOWNLOADING_RESULTS) {
            require(stem.path.startsWith(prefix(identity) + "/") && !stem.path.contains(".."))
            staging.cached(identity, stem)?.let { return@staged it }
            val partial = staging.partial(identity, stem)
            storage.reference.child(stem.path).getFile(partial).await()
            try {
                staging.commit(identity, stem, partial)
            } catch (error: Throwable) {
                partial.delete()
                throw RemoteResultValidationException("Downloaded stem failed integrity validation.", error)
            }
        }

    override suspend fun downloadReference(identity: RemoteJobIdentity, reference: RemoteReference): RemoteStemPayload =
        staged(RemotePipelineStage.DOWNLOADING_RESULTS) {
            require(reference.path.startsWith(prefix(identity) + "/prepared/") && !reference.path.contains(".."))
            staging.cached(identity, reference)?.let { return@staged it }
            val partial = staging.partial(identity, reference)
            storage.reference.child(reference.path).getFile(partial).await()
            try {
                staging.commit(identity, reference, partial)
            } catch (error: Throwable) {
                partial.delete()
                throw RemoteResultValidationException("Downloaded prepared reference failed integrity validation.", error)
            }
        }

    override suspend fun persistAcceptedManifest(identity: RemoteJobIdentity, manifestBytes: ByteArray) {
        AcceptedRemoteManifestStore(context).persist(identity, manifestBytes)
    }

    override suspend fun cleanup(identity: RemoteJobIdentity) {
        // Remote object ownership belongs to acknowledgeRemoteImport/cancel/janitor.
        // Client cleanup is deliberately local-only so backend purge failures remain observable.
        staging.clear(identity)
    }

    private suspend fun prefix(identity: RemoteJobIdentity) =
        "remote/v1/users/${uid()}/jobs/${identity.jobId}/output"

    private suspend fun <T> staged(stage: RemotePipelineStage, block: suspend () -> T): T =
        try {
            block()
        } catch (error: RemotePipelineException) {
            throw error
        } catch (error: Throwable) {
            throw RemotePipelineException(stage, error)
        }
}

class RemoteSeparationWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val id = remoteIdentityFrom(inputData) ?: return Result.failure()
        val app = GuitarLabFirebase.app(applicationContext)
        val auth = FirebaseAuth.getInstance(app)
        val store = FileRemoteJobStore(applicationContext)
        val repository = FileProjectRepository(applicationContext.filesDir)
        val mediaStore = ProjectManagedMediaStore(applicationContext.filesDir)
        val notifier = RemoteSeparationNotifier(applicationContext)
        val projectName = repository.load(id.projectId)?.name ?: "Projeto"
        val resultTransport = FirebaseResultTransport(
            applicationContext,
            auth,
            FirebaseStorage.getInstance(app),
        )
        val coordinator = RemoteSeparationCoordinator(
            store,
            FirebaseRemoteBackend(auth, FirebaseFirestore.getInstance(app), FirebaseFunctions.getInstance(app, "us-central1")),
            resultTransport,
            RemoteManifestCodec::decode,
            ManagedStemSetPublisher(
                StemSetProjectPublisher(repository, mediaStore),
                PreparedReferenceProjectPublisher(
                    repository = repository,
                    mediaStore = mediaStore,
                    tempDirectory = applicationContext.cacheDir,
                ),
            ),
            expectedResultUid = { requireStableRemoteUid(auth) },
        )
        val previousRetryCode = store.load(id.jobId)?.errorCode
        return try {
            val current = store.load(id.jobId)
            setForeground(
                notifier.foregroundInfo(
                    id,
                    projectName,
                    current?.state ?: RemoteJobState.UPLOADING,
                    current?.errorCode,
                ),
            )
            current?.let { notifier.show(id, projectName, it.state, it.errorCode) }
            val next = if (current == null) coordinator.start(id) else coordinator.reconcile(id)
            if (next.identity.jobId != id.jobId) {
                notifier.show(next.identity, projectName, next.state, next.errorCode)
                val manager = WorkManager.getInstance(applicationContext)
                if (next.state == RemoteJobState.CANCEL_REQUESTED) {
                    manager.enqueueUniqueWork(
                        remoteCancelWorkName(next.identity.jobId),
                        ExistingWorkPolicy.REPLACE,
                        remoteCancelRequest(next.identity),
                    )
                } else {
                    manager.enqueueUniqueWork(
                        remoteWorkName(next.identity.jobId),
                        ExistingWorkPolicy.KEEP,
                        remoteSeparationRequest(next.identity),
                    )
                }
                return Result.success()
            }
            when (next.state) {
                RemoteJobState.IMPORTED -> {
                    resultTransport.cleanup(id)
                    val preparation = repository.load(id.projectId)?.preparation
                    val referencesAlreadyPrepared =
                        preparation?.status == PreparationStatus.READY &&
                            preparation.activeBackingAssetId != null &&
                            preparation.activeGuitarAssetId != null
                    val result = if (referencesAlreadyPrepared) {
                        Result.success()
                    } else {
                        prepareReferencesOrRetry(id, next, store, repository, mediaStore)
                    }
                    notifier.show(id, projectName, RemoteJobState.IMPORTED, store.load(id.jobId)?.errorCode)
                    result
                }
                RemoteJobState.IMPORT_FAILED -> {
                    notifier.show(id, projectName, RemoteJobState.IMPORT_FAILED, next.errorCode)
                    Result.failure()
                }
                RemoteJobState.CANCELLED -> {
                    restoreSourceReadyAfterTerminal(id, store, repository)
                    notifier.show(id, projectName, RemoteJobState.CANCELLED, next.errorCode)
                    Result.success()
                }
                RemoteJobState.FAILED, RemoteJobState.EXPIRED -> {
                    restoreSourceReadyAfterTerminal(id, store, repository)
                    notifier.show(id, projectName, next.state, next.errorCode)
                    Result.failure()
                }
                else -> {
                    notifier.show(id, projectName, next.state, next.errorCode)
                    Result.retry()
                }
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Throwable) {
            val failure = RemoteFirebaseFailureClassifier.classify(error)
            Log.w(
                REMOTE_LOG_TAG,
                "job=${id.jobId} project=${id.projectId} stage=${failure.stage} code=${failure.code} retryable=${failure.retryable} attempt=${runAttemptCount + 1}",
                error,
            )
            val current = store.load(id.jobId)
            val failureAttempt = RemoteWorkerRetryPolicy.nextAttempt(previousRetryCode, failure)
            val durableFailure = RemoteWorkerRetryPolicy.durableCode(failure, failureAttempt)
            if (RemoteWorkerRetryPolicy.shouldRetry(failure, failureAttempt)) {
                if (current != null && current.state !in RemoteRecoveryPolicy.terminalStates) {
                    store.save(
                        current.copy(
                            updatedAtMs = System.currentTimeMillis(),
                            errorCode = durableFailure,
                        ),
                    )
                }
                notifier.showRetry(id, projectName, durableFailure)
                Result.retry()
            } else {
                terminalizeExhaustedWorker(id, durableFailure, store, repository)
                store.load(id.jobId)?.let { terminal ->
                    notifier.show(id, projectName, terminal.state, terminal.errorCode)
                }
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
        const val REMOTE_LOG_TAG = "GuitarLabRemote"
    }
}

class RemoteSeparationClient(private val context: Context) {
    private val notifier = RemoteSeparationNotifier(context)
    private val staging = RemoteStemStaging(File(context.cacheDir, "remote-separation"))

    @Synchronized
    fun enqueue(projectId: String): String {
        val repo = FileProjectRepository(context.filesDir)
        val store = FileRemoteJobStore(context)
        val latest = store.latestForProject(projectId)
        require(latest?.state != RemoteJobState.IMPORT_FAILED) {
            "O processamento em nuvem já terminou. Retome a importação antes de iniciar uma nova separação."
        }
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
        notifier.show(id, project.name, RemoteJobState.UPLOADING)
        WorkManager.getInstance(context).enqueueUniqueWork(
            workName(id.jobId),
            ExistingWorkPolicy.KEEP,
            separationRequest(id),
        )
        return jobId
    }

    @Synchronized
    fun resumeImport(projectId: String): String {
        val store = FileRemoteJobStore(context)
        val job = requireNotNull(store.latestForProject(projectId)) { "Nenhum resultado remoto disponível para retomar." }
        require(job.state == RemoteJobState.IMPORT_FAILED) { "A importação não está aguardando retomada." }
        val resumed = store.save(
            job.copy(
                state = RemoteJobState.IMPORTING,
                updatedAtMs = System.currentTimeMillis(),
                errorCode = null,
            ),
        )
        val projectName = FileProjectRepository(context.filesDir).load(projectId)?.name ?: "Projeto"
        notifier.show(resumed.identity, projectName, RemoteJobState.IMPORTING)
        WorkManager.getInstance(context).enqueueUniqueWork(
            workName(resumed.identity.jobId),
            ExistingWorkPolicy.REPLACE,
            separationRequest(resumed.identity),
        )
        return resumed.identity.jobId
    }
    fun snapshot(jobId: String) = FileRemoteJobStore(context).load(jobId)
    fun snapshotProject(projectId: String) = FileRemoteJobStore(context).latestForProject(projectId)
    fun observeProject(projectId: String): Flow<DurableRemoteJob?> = FileRemoteJobStore(context).observeProject(projectId)

    fun cancel(projectId: String, jobId: String) {
        val store = FileRemoteJobStore(context)
        val job = store.load(jobId) ?: return
        if (job.state in RemoteRecoveryPolicy.terminalStates && job.state != RemoteJobState.IMPORT_FAILED) return
        val requesting = store.save(job.copy(state = RemoteJobState.CANCEL_REQUESTED, updatedAtMs = System.currentTimeMillis()))
        val projectName = FileProjectRepository(context.filesDir).load(projectId)?.name ?: "Projeto"
        notifier.show(requesting.identity, projectName, RemoteJobState.CANCEL_REQUESTED)
        scheduleCancel(requesting)
    }

    /**
     * Activity may outlive the local durable job row after process/recovery edge cases.
     * In that case we still own the authenticated remote job id, so enqueue a minimal
     * Firebase cancellation request instead of leaving a cloud orphan behind.
     */
    fun cancelFromActivity(projectId: String, jobId: String): Boolean {
        val store = FileRemoteJobStore(context)
        val job = store.load(jobId)
        if (job != null) {
            if (job.identity.projectId != projectId) return false
            cancel(projectId, jobId)
            return true
        }
        WorkManager.getInstance(context.applicationContext).enqueueUniqueWork(
            remoteActivityCancelWorkName(jobId),
            ExistingWorkPolicy.REPLACE,
            remoteActivityCancelRequest(projectId, jobId),
        )
        return true
    }

    fun closeProject(projectId: String) {
        val store = FileRemoteJobStore(context)
        val jobs = store.active().filter { it.identity.projectId == projectId }
        val remoteOwned = jobs.filter {
            it.state !in RemoteRecoveryPolicy.terminalStates || it.state == RemoteJobState.IMPORT_FAILED
        }
        remoteOwned.forEach { cancel(projectId, it.identity.jobId) }
        jobs.forEach { job ->
            WorkManager.getInstance(context).cancelUniqueWork(workName(job.identity.jobId))
            if (job !in remoteOwned) {
                WorkManager.getInstance(context).cancelUniqueWork(cancelWorkName(job.identity.jobId))
                staging.clear(job.identity)
            }
        }
        if (remoteOwned.isEmpty()) store.removeProject(projectId)
    }

    fun resumePending() {
        val store = FileRemoteJobStore(context)
        store.repairLegacyImportExpirations()
        val repository = FileProjectRepository(context.filesDir)
        scheduleGenerationRecoveryProbes(store, repository)
        val jobs = store.active()
        notifier.reconcileExisting(jobs) { projectId ->
            repository.load(projectId)?.name ?: "Projeto"
        }
        jobs
            .filter { it.state !in RemoteRecoveryPolicy.terminalStates }
            .forEach { job ->
                val projectName = repository.load(job.identity.projectId)?.name ?: "Projeto"
                notifier.show(job.identity, projectName, job.state, job.errorCode)
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

    private fun scheduleGenerationRecoveryProbes(
        store: FileRemoteJobStore,
        repository: FileProjectRepository,
    ) {
        repository.list().forEach { project ->
            val preparation = project.preparation ?: return@forEach
            val sourceId = preparation.sourceAssetId ?: return@forEach
            if (preparation.activeBackingAssetId != null && preparation.activeGuitarAssetId != null) return@forEach
            val source = project.assets.firstOrNull { it.assetId == sourceId } ?: return@forEach
            val generation = RemoteSourceGeneration(project.id, sourceId, source.sha256)
            val hasUnresolved = store.active().any {
                RemoteRecoveryPolicy.isUnresolved(it.state) && RemoteRecoveryPolicy.sameGeneration(it, generation)
            }
            if (!hasUnresolved) {
                WorkManager.getInstance(context).enqueueUniqueWork(
                    remoteRecoveryWorkName(project.id),
                    ExistingWorkPolicy.KEEP,
                    remoteRecoveryRequest(project.id),
                )
            }
        }
    }

    private fun separationRequest(id: RemoteJobIdentity) =
        OneTimeWorkRequestBuilder<RemoteSeparationWorker>()
            .setInputData(remoteWorkData(id))
            .setConstraints(networkConstraints())
            .setBackoffCriteria(BackoffPolicy.LINEAR, 30, TimeUnit.SECONDS)
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


class RemoteRecoveryProbeWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val projectId = inputData.getString("projectId") ?: return Result.failure()
        val repository = FileProjectRepository(applicationContext.filesDir)
        val project = repository.load(projectId) ?: return Result.success()
        val preparation = project.preparation ?: return Result.success()
        val sourceId = preparation.sourceAssetId ?: return Result.success()
        if (preparation.activeBackingAssetId != null && preparation.activeGuitarAssetId != null) return Result.success()
        val source = project.assets.firstOrNull { it.assetId == sourceId } ?: return Result.success()
        val generation = RemoteSourceGeneration(projectId, sourceId, source.sha256)
        val app = GuitarLabFirebase.app(applicationContext)
        val backend = FirebaseRemoteBackend(
            FirebaseAuth.getInstance(app),
            FirebaseFirestore.getInstance(app),
            FirebaseFunctions.getInstance(app, "us-central1"),
        )
        val store = FileRemoteJobStore(applicationContext)
        val notifier = RemoteSeparationNotifier(applicationContext)
        return try {
            val remote = backend.findRecoverable(generation)
            if (remote == null) {
                val hasUnresolved = store.active().any {
                    RemoteRecoveryPolicy.isUnresolved(it.state) && RemoteRecoveryPolicy.sameGeneration(it, generation)
                }
                if (RemoteRecoveryPolicy.shouldRestoreSourceReady(preparation.status, hasUnresolved)) {
                    repository.save(
                        project.copy(
                            updatedAtEpochMs = System.currentTimeMillis(),
                            preparation = preparation.copy(status = PreparationStatus.SOURCE_READY),
                        ),
                    )
                }
                return Result.success()
            }
            val adopted = store.adopt(
                remote.copy(
                    updatedAtMs = System.currentTimeMillis(),
                    errorCode = remote.errorCode ?: RemoteSeparationCoordinator.RECOVERY_MARKER,
                ),
            )
            repository.save(
                project.copy(
                    updatedAtEpochMs = System.currentTimeMillis(),
                    preparation = preparation.copy(status = PreparationStatus.SEPARATING),
                ),
            )
            notifier.show(adopted.identity, project.name, adopted.state, adopted.errorCode)
            val manager = WorkManager.getInstance(applicationContext)
            if (adopted.state == RemoteJobState.CANCEL_REQUESTED) {
                manager.enqueueUniqueWork(
                    remoteCancelWorkName(adopted.identity.jobId),
                    ExistingWorkPolicy.REPLACE,
                    remoteCancelRequest(adopted.identity),
                )
            } else {
                manager.enqueueUniqueWork(
                    remoteWorkName(adopted.identity.jobId),
                    ExistingWorkPolicy.KEEP,
                    remoteSeparationRequest(adopted.identity),
                )
            }
            Result.success()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Throwable) {
            val failure = RemoteFirebaseFailureClassifier.classify(error)
            Log.w(
                "GuitarLabRemote",
                "recovery project=${projectId} code=${failure.code} retryable=${failure.retryable} attempt=${runAttemptCount + 1}",
                error,
            )
            if (failure.retryable && runAttemptCount < 5) Result.retry() else Result.success()
        }
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
        val notifier = RemoteSeparationNotifier(applicationContext)
        val staging = RemoteStemStaging(File(applicationContext.cacheDir, "remote-separation"))
        val projectName = repository.load(id.projectId)?.name ?: "Projeto"
        notifier.show(id, projectName, RemoteJobState.CANCEL_REQUESTED, store.load(id.jobId)?.errorCode)

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
            staging.clear(id)
            store.load(id.jobId)?.let { finished ->
                notifier.show(id, projectName, finished.state, finished.errorCode)
            }
            if (repository.load(id.projectId) == null) store.removeProject(id.projectId)
            Result.success()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Throwable) {
            val failure = RemoteFirebaseFailureClassifier.classify(error)
            Log.w(
                "GuitarLabRemote",
                "cancel job=${id.jobId} stage=${failure.stage} code=${failure.code} retryable=${failure.retryable} attempt=${runAttemptCount + 1}",
                error,
            )
            if (failure.retryable && runAttemptCount < MAX_CANCEL_ATTEMPTS) {
                store.load(id.jobId)?.takeIf { it.state !in RemoteRecoveryPolicy.terminalStates }?.let { current ->
                    store.save(
                        current.copy(
                            updatedAtMs = System.currentTimeMillis(),
                            errorCode = failure.durableCode(runAttemptCount),
                        ),
                    )
                }
                notifier.show(id, projectName, RemoteJobState.CANCEL_REQUESTED, failure.durableCode(runAttemptCount))
                Result.retry()
            } else {
                val current = store.load(id.jobId)
                if (current != null && current.state !in RemoteRecoveryPolicy.terminalStates) {
                    store.save(
                        current.copy(
                            state = RemoteJobState.FAILED,
                            updatedAtMs = System.currentTimeMillis(),
                            errorCode = "CANCEL_UNCONFIRMED:${failure.stage?.name ?: "UNKNOWN"}:${failure.code}",
                        ),
                    )
                }
                restoreSourceReadyAfterTerminal(id, store, repository)
                notifier.show(id, projectName, RemoteJobState.FAILED, store.load(id.jobId)?.errorCode)
                Result.failure()
            }
        }
    }

    private companion object {
        const val MAX_CANCEL_ATTEMPTS = 5
    }
}



class RemoteActivityOrphanCancelWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val jobId = inputData.getString("jobId").orEmpty()
        if (jobId.isBlank()) return Result.failure()
        return try {
            val app = GuitarLabFirebase.app(applicationContext)
            val auth = FirebaseAuth.getInstance(app)
            requireStableRemoteUid(auth)
            FirebaseFunctions.getInstance(app, "us-central1")
                .getHttpsCallable("cancelRemoteSeparation")
                .call(mapOf("jobId" to jobId))
                .await()
            Result.success()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Throwable) {
            if (runAttemptCount < 4) Result.retry() else Result.failure()
        }
    }
}

private fun remoteWorkName(jobId: String) = "guitarlab-separation:$jobId"
private fun remoteCancelWorkName(jobId: String) = "guitarlab-separation-cancel:$jobId"
private fun remoteActivityCancelWorkName(jobId: String) = "guitarlab-separation-activity-cancel:$jobId"
private fun remoteRecoveryWorkName(projectId: String) = "guitarlab-separation-recovery:$projectId"

private fun remoteSeparationRequest(id: RemoteJobIdentity) =
    OneTimeWorkRequestBuilder<RemoteSeparationWorker>()
        .setInputData(remoteWorkData(id))
        .setConstraints(networkConstraints())
        .setBackoffCriteria(BackoffPolicy.LINEAR, 30, TimeUnit.SECONDS)
        .addTag("guitarlab-separation")
        .addTag("guitarlab-separation:${id.projectId}")
        .build()

private fun remoteCancelRequest(id: RemoteJobIdentity) =
    OneTimeWorkRequestBuilder<RemoteCancelWorker>()
        .setInputData(remoteWorkData(id))
        .setConstraints(networkConstraints())
        .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
        .addTag("guitarlab-separation-cancel")
        .addTag("guitarlab-separation-cancel:${id.projectId}")
        .build()

private fun remoteActivityCancelRequest(projectId: String, jobId: String) =
    OneTimeWorkRequestBuilder<RemoteActivityOrphanCancelWorker>()
        .setInputData(workDataOf("projectId" to projectId, "jobId" to jobId))
        .setConstraints(networkConstraints())
        .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
        .addTag("guitarlab-separation-activity-cancel")
        .addTag("guitarlab-separation-activity-cancel:$projectId")
        .build()

private fun remoteRecoveryRequest(projectId: String) =
    OneTimeWorkRequestBuilder<RemoteRecoveryProbeWorker>()
        .setInputData(workDataOf("projectId" to projectId))
        .setConstraints(networkConstraints())
        .setBackoffCriteria(BackoffPolicy.LINEAR, 30, TimeUnit.SECONDS)
        .addTag("guitarlab-separation-recovery")
        .addTag("guitarlab-separation-recovery:$projectId")
        .build()

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
            RemoteRecoveryPolicy.isUnresolved(candidate.state)
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
        if (current.state != RemoteJobState.IMPORT_FAILED) {
            restoreSourceReadyAfterTerminal(id, store, repository)
        }
        return
    }
    val terminalState = RemoteMissingPolicy.failureTerminalState(current.state)
    store.save(
        current.copy(
            state = terminalState,
            updatedAtMs = System.currentTimeMillis(),
            errorCode = "WORKER_RETRY_EXHAUSTED_${code.take(80)}",
        ),
    )
    if (terminalState != RemoteJobState.IMPORT_FAILED) {
        restoreSourceReadyAfterTerminal(id, store, repository)
    }
}
