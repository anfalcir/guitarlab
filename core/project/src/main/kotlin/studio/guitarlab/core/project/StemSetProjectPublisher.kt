package studio.guitarlab.core.project

import java.io.File
import java.io.InputStream
import java.security.MessageDigest
import java.util.UUID
import studio.guitarlab.core.model.AssetClassification
import studio.guitarlab.core.model.AssetLifecycle
import studio.guitarlab.core.model.AssetProvenance
import studio.guitarlab.core.model.AssetRole
import studio.guitarlab.core.model.ManagedAsset
import studio.guitarlab.core.model.PreparationStatus

data class ValidatedStem(
    val name: String,
    val byteCount: Long,
    val sha256: String,
    val sampleRate: Int,
    val channels: Int,
    val frames: Long,
    val openStream: () -> InputStream,
)

data class StemSetPublicationRequest(
    val projectId: String,
    val jobId: String,
    val sourceAssetId: String,
    val sourceSha256: String,
    val manifestSha256: String,
    val engine: String,
    val model: String,
    val modelSha256: String,
    val stems: List<ValidatedStem>,
)

class StemSetProjectPublisher(
    private val repository: ProjectRepository,
    private val mediaStore: ProjectManagedMediaStore,
    private val nowMs: () -> Long = System::currentTimeMillis,
    private val idFactory: () -> String = { UUID.randomUUID().toString() },
) {
    fun publish(request: StemSetPublicationRequest): Boolean {
        require(request.jobId.isNotBlank() && request.manifestSha256.matches(SHA256))
        require(request.stems.map { it.name }.toSet() == ROLES.keys && request.stems.size == ROLES.size)
        request.stems.forEach { stem ->
            require(
                stem.byteCount > 44 &&
                    stem.sha256.matches(SHA256) &&
                    stem.sampleRate == 44_100 &&
                    stem.channels == 2 &&
                    stem.frames > 0,
            )
        }

        val before = requireNotNull(repository.load(request.projectId))
        val source = before.assets.singleOrNull { it.assetId == request.sourceAssetId }
            ?: error("source asset missing")
        require(
            source.sha256 == request.sourceSha256 &&
                before.preparation?.sourceAssetId == request.sourceAssetId,
        ) { "source ownership mismatch" }

        val prior = before.assets.count { it.provenance?.parameters?.get("jobId") == request.jobId }
        if (prior == ROLES.size) return true
        require(prior == 0) { "partial prior publication" }

        mediaStore.discardAbandonedStemSet(request.projectId, request.jobId)
        val ingested = mutableListOf<Pair<ValidatedStem, ManagedMediaAsset>>()

        try {
            request.stems.sortedBy { it.name }.forEach { stem ->
                val managed = stem.openStream().use { input ->
                    mediaStore.ingestStem(
                        request.projectId,
                        "${request.jobId}-${stem.name}.wav",
                        input,
                    )
                }
                ingested += stem to managed
                require(
                    managed.byteCount == stem.byteCount &&
                        sha256(managed.file) == stem.sha256,
                ) { "managed stem integrity mismatch: ${stem.name}" }
            }

            val current = requireNotNull(repository.load(request.projectId))
            require(
                current.preparation?.sourceAssetId == request.sourceAssetId &&
                    current.assets.any {
                        it.assetId == request.sourceAssetId &&
                            it.sha256 == request.sourceSha256
                    },
            ) { "source changed before commit" }

            val assets = ingested.map { (stem, managed) ->
                ManagedAsset(
                    assetId = idFactory(),
                    role = requireNotNull(ROLES[stem.name]),
                    relativePath = managed.relativePath,
                    sha256 = stem.sha256,
                    byteSize = managed.byteCount,
                    format = "wav",
                    sampleRateHz = stem.sampleRate,
                    channelCount = stem.channels,
                    frameCount = stem.frames,
                    createdAtEpochMs = nowMs(),
                    classification = AssetClassification.AUTHORITATIVE,
                    lifecycle = AssetLifecycle.MANAGED,
                    provenance = AssetProvenance(
                        kind = "REMOTE_SEPARATION",
                        inputAssetIds = listOf(request.sourceAssetId),
                        inputSha256 = listOf(request.sourceSha256),
                        engine = request.engine,
                        model = request.model,
                        parameters = mapOf(
                            "jobId" to request.jobId,
                            "manifestSha256" to request.manifestSha256,
                            "modelSha256" to request.modelSha256,
                        ),
                        contractVersion = 1,
                        executionMode = "REMOTE",
                    ),
                )
            }

            repository.save(
                current.copy(
                    updatedAtEpochMs = nowMs(),
                    assets = current.assets + assets,
                    preparation = requireNotNull(current.preparation).copy(
                        status = PreparationStatus.READY,
                        activeStemAssetIds = assets.associate { it.role to it.assetId },
                    ),
                ),
            )
            return false
        } catch (error: Throwable) {
            ingested.forEach { (_, managed) ->
                runCatching {
                    mediaStore.discardUncommitted(request.projectId, managed.relativePath)
                }
            }
            throw error
        }
    }

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                if (read > 0) digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    companion object {
        val ROLES = mapOf(
            "drums" to AssetRole.STEM_DRUMS,
            "bass" to AssetRole.STEM_BASS,
            "other" to AssetRole.STEM_OTHER,
            "vocals" to AssetRole.STEM_VOCALS,
            "guitar" to AssetRole.STEM_GUITAR,
            "piano" to AssetRole.STEM_PIANO,
        )
        private val SHA256 = Regex("[a-f0-9]{64}")
    }
}
