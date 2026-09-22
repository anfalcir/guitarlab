package studio.guitarlab.platform.source.android

import android.content.Context
import android.net.Uri
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import studio.guitarlab.core.project.FileProjectRepository
import studio.guitarlab.core.project.ProjectManagedMediaStore
import studio.guitarlab.core.project.SourceAssetPublisher
import studio.guitarlab.core.project.SourcePublicationRequest
import studio.guitarlab.core.project.SourcePublicationResult
import studio.guitarlab.core.source.RankedSourceCandidate
import studio.guitarlab.core.source.SourceSearchRequest

class SourceAcquisitionClient(context: Context) {
    private val appContext = context.applicationContext
    private val operations = SourceOperationStore(appContext)
    private val repository = FileProjectRepository(appContext.filesDir)

    suspend fun search(request: SourceSearchRequest): SourceDiscoveryResult = SourceSearchCoordinator(appContext).search(request)

    suspend fun importLocal(projectId: String, uri: Uri): SourcePublicationResult {
        val operationId = UUID.randomUUID().toString()
        operations.begin(projectId, operationId, "Copiando e validando fonte local…")
        return try {
            val expectedSource = requireNotNull(repository.load(projectId)) { "Projeto não encontrado." }.preparation?.sourceAssetId
            val media = AndroidSourceMedia.stageLocal(appContext, uri, operationId)
            require(operations.isCurrent(projectId, operationId)) { "A operação foi substituída por outra aquisição." }
            operations.update(projectId, operationId, SourceOperationState.RUNNING, 90, "Publicando fonte validada…")
            val result = SourceAssetPublisher(repository, ProjectManagedMediaStore(appContext.filesDir)).publish(
                SourcePublicationRequest(projectId, operationId, expectedSource, media),
            )
            operations.update(projectId, operationId, SourceOperationState.SUCCESS, 100, "Fonte local pronta para separação.")
            AndroidSourceMedia.cleanup(appContext, operationId)
            result
        } catch (cancelled: CancellationException) {
            AndroidSourceMedia.cleanup(appContext, operationId)
            operations.update(projectId, operationId, SourceOperationState.CANCELLED, 0, "Importação cancelada.")
            throw cancelled
        } catch (error: Throwable) {
            AndroidSourceMedia.cleanup(appContext, operationId)
            if (operations.isCurrent(projectId, operationId)) operations.update(projectId, operationId, SourceOperationState.ERROR, 0, error.message ?: "Não foi possível importar a fonte.")
            throw error
        }
    }

    fun enqueueOnline(projectId: String, candidate: RankedSourceCandidate): String {
        require(candidate.automaticDownloadSupported && !candidate.previewOnly) { "Esta fonte não pode ser baixada automaticamente." }
        val operationId = UUID.randomUUID().toString()
        operations.begin(projectId, operationId, "Aquisição agendada. Você pode continuar usando o aplicativo.")
        val expectedSource = runCatching { requireNotNull(repository.load(projectId)) { "Projeto não encontrado." }.preparation?.sourceAssetId }
            .getOrElse { error ->
                operations.update(projectId, operationId, SourceOperationState.ERROR, 0, error.message ?: "Projeto não encontrado.")
                throw error
            }
        SourceAcquisitionWorker.enqueue(
            appContext,
            projectId,
            operationId,
            expectedSource,
            OnlineSourceRequest(candidate.url, candidate.provider, candidate.formatId, candidate.durationSeconds, candidate.title),
        )
        return operationId
    }

    fun cancel(projectId: String) {
        val snapshot = operations.snapshot(projectId) ?: return
        if (snapshot.state !in setOf(SourceOperationState.RUNNING, SourceOperationState.RETRYING)) return
        operations.markCancelled(projectId)
        SourceAcquisitionWorker.cancel(appContext, projectId, snapshot.operationId)
        AndroidSourceMedia.cleanup(appContext, snapshot.operationId)
    }

    /** Close all local ownership before a project is physically deleted. */
    fun closeProject(projectId: String) {
        val snapshot = operations.snapshot(projectId)
        if (snapshot != null) {
            SourceAcquisitionWorker.cancel(appContext, projectId, snapshot.operationId)
            AndroidSourceMedia.cleanup(appContext, snapshot.operationId)
        }
        operations.clear(projectId)
    }

    fun snapshot(projectId: String): SourceOperationSnapshot? = operations.snapshot(projectId)

    fun observe(projectId: String): Flow<SourceOperationSnapshot?> = operations.observe(projectId)
}
