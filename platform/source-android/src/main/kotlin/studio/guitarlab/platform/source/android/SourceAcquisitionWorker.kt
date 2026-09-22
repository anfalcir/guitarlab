package studio.guitarlab.platform.source.android

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CancellationException
import studio.guitarlab.core.project.FileProjectRepository
import studio.guitarlab.core.project.ProjectManagedMediaStore
import studio.guitarlab.core.project.SourceAssetPublisher
import studio.guitarlab.core.project.SourcePublicationRequest
import studio.guitarlab.core.source.SourceAcquisitionPolicy
import studio.guitarlab.core.source.SourceProvider

class SourceAcquisitionWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {
    private val operations = SourceOperationStore(applicationContext)

    override suspend fun doWork(): Result {
        val projectId = inputData.getString(KEY_PROJECT_ID).orEmpty()
        val operationId = inputData.getString(KEY_OPERATION_ID).orEmpty()
        val url = inputData.getString(KEY_URL).orEmpty()
        val provider = inputData.getString(KEY_PROVIDER)?.let { runCatching { SourceProvider.valueOf(it) }.getOrNull() }
        if (projectId.isBlank() || operationId.isBlank() || url.isBlank() || provider == null) return Result.failure()
        if (!operations.isCurrent(projectId, operationId)) return Result.success()
        val expectedSource = inputData.getString(KEY_EXPECTED_SOURCE)?.takeIf { it.isNotBlank() }
        val request = OnlineSourceRequest(
            url = url,
            provider = provider,
            formatId = inputData.getString(KEY_FORMAT_ID).orEmpty(),
            expectedDurationSeconds = inputData.getDouble(KEY_DURATION, 0.0),
            title = inputData.getString(KEY_TITLE).orEmpty(),
        )
        return try {
            operations.update(projectId, operationId, if (runAttemptCount > 0) SourceOperationState.RETRYING else SourceOperationState.RUNNING, 1, if (runAttemptCount > 0) "Retomando aquisição…" else "Iniciando aquisição…")
            val file = YtDlpSourceDownloader.download(applicationContext, request, operationId) { progress, message ->
                operations.update(projectId, operationId, SourceOperationState.RUNNING, progress, message)
                setProgressAsync(workDataOf("progress" to progress, "message" to message))
            }
            if (!operations.isCurrent(projectId, operationId)) {
                AndroidSourceMedia.cleanup(applicationContext, operationId)
                return Result.success()
            }
            val media = AndroidSourceMedia.validateDownloaded(file, request.expectedDurationSeconds, request.url, request.title)
            operations.update(projectId, operationId, SourceOperationState.RUNNING, 92, "Publicando fonte validada…")
            SourceAssetPublisher(
                repository = FileProjectRepository(applicationContext.filesDir),
                mediaStore = ProjectManagedMediaStore(applicationContext.filesDir),
            ).publish(SourcePublicationRequest(projectId, operationId, expectedSource, media))
            operations.update(projectId, operationId, SourceOperationState.SUCCESS, 100, "Fonte pronta para separação.")
            AndroidSourceMedia.cleanup(applicationContext, operationId)
            Result.success()
        } catch (cancelled: CancellationException) {
            AndroidSourceMedia.cleanup(applicationContext, operationId)
            if (operations.isCurrent(projectId, operationId)) operations.update(projectId, operationId, SourceOperationState.CANCELLED, 0, "Aquisição cancelada.")
            throw cancelled
        } catch (error: Throwable) {
            AndroidSourceMedia.cleanup(applicationContext, operationId)
            if (!operations.isCurrent(projectId, operationId)) return Result.success()
            val retryable = runAttemptCount < MAX_RETRIES && SourceAcquisitionPolicy.retryableMessage(error.message)
            if (retryable) {
                operations.update(projectId, operationId, SourceOperationState.RETRYING, 0, "Falha temporária; nova tentativa agendada.")
                Result.retry()
            } else {
                operations.update(projectId, operationId, SourceOperationState.ERROR, 0, error.message ?: "Não foi possível adquirir a fonte.")
                Result.failure()
            }
        }
    }

    companion object {
        private const val KEY_PROJECT_ID = "project_id"
        private const val KEY_OPERATION_ID = "operation_id"
        private const val KEY_EXPECTED_SOURCE = "expected_source"
        private const val KEY_URL = "url"
        private const val KEY_PROVIDER = "provider"
        private const val KEY_FORMAT_ID = "format_id"
        private const val KEY_DURATION = "duration"
        private const val KEY_TITLE = "title"
        private const val MAX_RETRIES = 2
        private const val TAG_ALL = "guitarlab-source-acquisition"

        fun enqueue(context: Context, projectId: String, operationId: String, expectedSourceAssetId: String?, request: OnlineSourceRequest) {
            val constraints = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
            val work = OneTimeWorkRequestBuilder<SourceAcquisitionWorker>()
                .setInputData(workDataOf(
                    KEY_PROJECT_ID to projectId,
                    KEY_OPERATION_ID to operationId,
                    KEY_EXPECTED_SOURCE to expectedSourceAssetId.orEmpty(),
                    KEY_URL to request.url,
                    KEY_PROVIDER to request.provider.name,
                    KEY_FORMAT_ID to request.formatId,
                    KEY_DURATION to request.expectedDurationSeconds,
                    KEY_TITLE to request.title,
                ))
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 20, TimeUnit.SECONDS)
                .addTag(TAG_ALL)
                .addTag(tagProject(projectId))
                .build()
            WorkManager.getInstance(context.applicationContext)
                .beginUniqueWork(uniqueName(projectId), ExistingWorkPolicy.REPLACE, work)
                .enqueue()
        }

        fun cancel(context: Context, projectId: String, operationId: String) {
            YtDlpSourceDownloader.cancel(operationId)
            WorkManager.getInstance(context.applicationContext).cancelUniqueWork(uniqueName(projectId))
        }

        private fun uniqueName(projectId: String) = "guitarlab-source:$projectId"
        private fun tagProject(projectId: String) = "guitarlab-source-project:$projectId"
    }
}
