package studio.guitarlab.core.project

import java.io.File
import java.security.MessageDigest
import java.util.UUID
import studio.guitarlab.core.model.AssetClassification
import studio.guitarlab.core.model.AssetLifecycle
import studio.guitarlab.core.model.AssetProvenance
import studio.guitarlab.core.model.AssetRole
import studio.guitarlab.core.model.GuitarProject
import studio.guitarlab.core.model.ManagedAsset

data class ValidatedSourceMedia(
    val file: File,
    val suggestedName: String,
    val format: String,
    val sampleRateHz: Int?,
    val channelCount: Int?,
    val frameCount: Long?,
    val sourceKind: String,
    val sourceUrl: String? = null,
)

data class SourcePublicationRequest(
    val projectId: String,
    val operationId: String,
    val expectedSourceAssetId: String?,
    val media: ValidatedSourceMedia,
)

data class SourcePublicationResult(
    val project: GuitarProject,
    val asset: ManagedAsset,
    val alreadyPublished: Boolean,
)

/**
 * The only U3 path allowed to mutate project state after source acquisition.
 *
 * The network/SAF layer stages and validates first. Publication then re-checks project ownership,
 * rejects a stale competing source operation, ingests immutable managed media, and atomically saves
 * the updated project. Repeating the same operationId is idempotent at project-state level.
 */
class SourceAssetPublisher(
    private val repository: ProjectRepository,
    private val mediaStore: ProjectManagedMediaStore,
    private val nowEpochMs: () -> Long = System::currentTimeMillis,
    private val assetIdFactory: () -> String = { UUID.randomUUID().toString() },
) {
    fun publish(request: SourcePublicationRequest): SourcePublicationResult {
        require(request.projectId.isNotBlank()) { "projectId não pode ficar vazio." }
        require(request.operationId.isNotBlank()) { "operationId não pode ficar vazio." }
        require(request.media.file.isFile && request.media.file.length() > 0L) { "Fonte validada ausente ou vazia." }
        require(request.media.format.isNotBlank()) { "Formato da fonte não informado." }

        val initial = requireNotNull(repository.load(request.projectId)) { "Projeto não encontrado." }
        initial.findOperationAsset(request.operationId)?.let { existing ->
            return SourcePublicationResult(initial, existing, alreadyPublished = true)
        }
        require(initial.preparation?.sourceAssetId == request.expectedSourceAssetId) {
            "A fonte do projeto mudou enquanto esta operação estava em andamento. O resultado antigo foi descartado."
        }

        var managedPath: String? = null
        try {
            val managed = request.media.file.inputStream().buffered().use { input ->
                mediaStore.ingest(request.projectId, request.media.suggestedName, input)
            }
            managedPath = managed.relativePath
            val hash = sha256(managed.file)
            val asset = ManagedAsset(
                assetId = assetIdFactory(),
                role = AssetRole.SOURCE_ORIGINAL,
                relativePath = managed.relativePath,
                sha256 = hash,
                byteSize = managed.byteCount,
                format = request.media.format,
                sampleRateHz = request.media.sampleRateHz,
                channelCount = request.media.channelCount,
                frameCount = request.media.frameCount,
                createdAtEpochMs = nowEpochMs(),
                classification = AssetClassification.AUTHORITATIVE,
                lifecycle = AssetLifecycle.MANAGED,
                provenance = AssetProvenance(
                    kind = request.media.sourceKind,
                    parameters = buildMap {
                        put("operationId", request.operationId)
                        request.media.sourceUrl?.takeIf { it.isNotBlank() }?.let { put("sourceUrl", it) }
                    },
                    contractVersion = 1,
                ),
            )

            // Re-read immediately before commit. Rename and unrelated Studio edits are preserved;
            // only a competing source change invalidates this result.
            val current = requireNotNull(repository.load(request.projectId)) { "Projeto removido durante a aquisição." }
            current.findOperationAsset(request.operationId)?.let { existing ->
                mediaStore.discardUncommitted(request.projectId, managed.relativePath)
                return SourcePublicationResult(current, existing, alreadyPublished = true)
            }
            require(current.preparation?.sourceAssetId == request.expectedSourceAssetId) {
                "A fonte do projeto mudou enquanto esta operação estava em andamento. O resultado antigo foi descartado."
            }
            // Source replacement is atomic: the old source remains authoritative until this point.
            // Publishing the validated replacement starts a new Prepare generation, so old stems and
            // references stop being active without being deleted. Existing Studio bindings/recordings
            // remain untouched and may continue to reference the protected older assets.
            val committedAt = nowEpochMs()
            val updated = ProjectLifecyclePolicy.withPublishedSource(
                project = current.copy(assets = current.assets + asset),
                sourceAssetId = asset.assetId,
                nowEpochMs = committedAt,
            )
            repository.save(updated)
            return SourcePublicationResult(updated, asset, alreadyPublished = false)
        } catch (error: Throwable) {
            managedPath?.let { runCatching { mediaStore.discardUncommitted(request.projectId, it) }.onFailure(error::addSuppressed) }
            throw error
        }
    }

    private fun GuitarProject.findOperationAsset(operationId: String): ManagedAsset? = assets.firstOrNull {
        it.role == AssetRole.SOURCE_ORIGINAL && it.provenance?.parameters?.get("operationId") == operationId
    }

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().buffered().use { input ->
            val buffer = ByteArray(64 * 1024)
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                if (read > 0) digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}
