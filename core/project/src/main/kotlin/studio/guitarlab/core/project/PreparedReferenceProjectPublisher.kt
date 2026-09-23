package studio.guitarlab.core.project

import java.io.File
import java.io.InputStream
import java.security.MessageDigest
import java.util.UUID
import studio.guitarlab.core.codec.FileSeekableByteSource
import studio.guitarlab.core.codec.StereoWavChannelSplitter
import studio.guitarlab.core.codec.WavMetadataReader
import studio.guitarlab.core.model.AssetClassification
import studio.guitarlab.core.model.AssetLifecycle
import studio.guitarlab.core.model.AssetProvenance
import studio.guitarlab.core.model.AssetRole
import studio.guitarlab.core.model.ManagedAsset
import studio.guitarlab.core.model.PreparationStatus

data class ValidatedPreparedReference(
    val name: String,
    val role: AssetRole,
    val byteCount: Long,
    val sha256: String,
    val sampleRate: Int,
    val channels: Int,
    val frames: Long,
    val openStream: () -> InputStream,
)

data class PreparedReferencePublicationRequest(
    val projectId: String,
    val jobId: String,
    val sourceAssetId: String,
    val sourceSha256: String,
    val manifestSha256: String,
    val engine: String,
    val model: String,
    val modelSha256: String,
    val recipeVersion: String,
    val targetPeakDbfs: Double,
    val sharedGainDb: Double,
    val references: List<ValidatedPreparedReference>,
)

class PreparedReferenceProjectPublisher(
    private val repository: ProjectRepository,
    private val mediaStore: ProjectManagedMediaStore,
    private val tempDirectory: File,
    private val nowMs: () -> Long = System::currentTimeMillis,
    private val idFactory: () -> String = { UUID.randomUUID().toString() },
) {
    fun publish(request: PreparedReferencePublicationRequest): Boolean {
        require(request.jobId.isNotBlank() && request.manifestSha256.matches(SHA256))
        require(request.recipeVersion == "prepared-reference-v2")
        require(request.targetPeakDbfs == -1.0 && request.sharedGainDb <= 0.000001)
        require(request.references.size == 2)
        val byName = request.references.associateBy { it.name }
        require(byName.keys == setOf("backing", "guitar"))
        require(byName.getValue("backing").role == AssetRole.REFERENCE_BACKING)
        require(byName.getValue("guitar").role == AssetRole.REFERENCE_GUITAR)
        request.references.forEach {
            require(it.byteCount > 44 && it.sha256.matches(SHA256))
            require(it.sampleRate == 44_100 && it.channels == 2 && it.frames > 0)
        }
        require(byName.getValue("backing").frames == byName.getValue("guitar").frames)

        val before = requireNotNull(repository.load(request.projectId))
        val source = before.assets.singleOrNull { it.assetId == request.sourceAssetId }
            ?: error("source asset missing")
        require(
            source.sha256 == request.sourceSha256 &&
                before.preparation?.sourceAssetId == request.sourceAssetId,
        ) { "source ownership mismatch" }

        val prior = before.assets.filter { it.provenance?.parameters?.get("jobId") == request.jobId }
        if (prior.size == 4 && before.preparation?.activeBackingAssetId in prior.map { it.assetId } &&
            before.preparation?.activeGuitarAssetId in prior.map { it.assetId }
        ) return true
        require(prior.isEmpty()) { "partial prior prepared-reference publication" }

        mediaStore.discardAbandonedReferenceSet(request.projectId, request.jobId)
        val published = mutableListOf<ManagedMediaAsset>()
        val work = File(tempDirectory, "remote-ref-${request.jobId}-${UUID.randomUUID()}").also { it.mkdirs() }
        var committed = false
        try {
            val backing = byName.getValue("backing")
            val guitar = byName.getValue("guitar")
            val backingManaged = backing.openStream().use {
                mediaStore.ingestReference(request.projectId, "${request.jobId}-backing.wav", it)
            }.also { published += it }
            verifyManaged(backingManaged, backing)

            val guitarManaged = guitar.openStream().use {
                mediaStore.ingestReference(request.projectId, "${request.jobId}-guitar.wav", it)
            }.also { published += it }
            verifyManaged(guitarManaged, guitar)

            val leftTemp = File(work, "guitar-left.wav")
            val rightTemp = File(work, "guitar-right.wav")
            val split = StereoWavChannelSplitter.split(guitarManaged.file, leftTemp, rightTemp)
            require(split.sampleRateHz == guitar.sampleRate && split.totalFrames == guitar.frames)

            val leftManaged = leftTemp.inputStream().buffered().use {
                mediaStore.ingestReference(request.projectId, "${request.jobId}-guitar-left.wav", it)
            }.also { published += it }
            val rightManaged = rightTemp.inputStream().buffered().use {
                mediaStore.ingestReference(request.projectId, "${request.jobId}-guitar-right.wav", it)
            }.also { published += it }

            val latest = requireNotNull(repository.load(request.projectId))
            require(
                latest.preparation?.sourceAssetId == request.sourceAssetId &&
                    latest.assets.any { it.assetId == request.sourceAssetId && it.sha256 == request.sourceSha256 },
            ) { "source changed before prepared-reference commit" }

            val now = nowMs()
            val commonParameters = mapOf(
                "jobId" to request.jobId,
                "manifestSha256" to request.manifestSha256,
                "modelSha256" to request.modelSha256,
                "recipe" to request.recipeVersion,
                "targetPeakDbfs" to request.targetPeakDbfs.toString(),
                "sharedGainDb" to "%.9f".format(java.util.Locale.US, request.sharedGainDb),
            )
            val backingAsset = managedAsset(
                backingManaged,
                AssetRole.REFERENCE_BACKING,
                backing,
                now,
                AssetProvenance(
                    kind = "REMOTE_PREPARED_BACKING",
                    inputAssetIds = listOf(request.sourceAssetId),
                    inputSha256 = listOf(request.sourceSha256),
                    engine = request.engine,
                    model = request.model,
                    parameters = commonParameters + ("includedRoles" to "STEM_DRUMS,STEM_BASS,STEM_OTHER,STEM_VOCALS,STEM_PIANO"),
                    contractVersion = 2,
                    executionMode = "REMOTE",
                ),
            )
            val guitarAsset = managedAsset(
                guitarManaged,
                AssetRole.REFERENCE_GUITAR,
                guitar,
                now,
                AssetProvenance(
                    kind = "REMOTE_PREPARED_GUITAR",
                    inputAssetIds = listOf(request.sourceAssetId),
                    inputSha256 = listOf(request.sourceSha256),
                    engine = request.engine,
                    model = request.model,
                    parameters = commonParameters + ("sourceRole" to "STEM_GUITAR"),
                    contractVersion = 2,
                    executionMode = "REMOTE",
                ),
            )
            val leftAsset = splitAsset(leftManaged, guitarAsset, "LEFT", guitar, now, commonParameters)
            val rightAsset = splitAsset(rightManaged, guitarAsset, "RIGHT", guitar, now, commonParameters)
            val references = listOf(backingAsset, guitarAsset, leftAsset, rightAsset)

            val latestPreparation = requireNotNull(latest.preparation)
            val prepared = latest.copy(
                updatedAtEpochMs = now,
                assets = latest.assets + references,
                preparation = latestPreparation.copy(
                    status = PreparationStatus.READY,
                    activeStemAssetIds = emptyMap(),
                    activeBackingAssetId = backingAsset.assetId,
                    activeGuitarAssetId = guitarAsset.assetId,
                    availableReferenceAssetIds = (
                        latestPreparation.availableReferenceAssetIds + references.map { it.assetId }
                    ).distinct(),
                ),
            )
            val bound = PreparedReferenceBindingPolicy.applyInitialBindings(prepared, now, idFactory)
            repository.save(bound)
            committed = true
            return false
        } finally {
            work.deleteRecursively()
            if (!committed) {
                published.forEach { media ->
                    runCatching { mediaStore.discardUncommitted(request.projectId, media.relativePath) }
                }
            }
        }
    }

    private fun verifyManaged(media: ManagedMediaAsset, reference: ValidatedPreparedReference) {
        require(media.byteCount == reference.byteCount && sha256(media.file) == reference.sha256) {
            "managed prepared-reference integrity mismatch: ${reference.name}"
        }
        FileSeekableByteSource(media.file).use { source ->
            val metadata = WavMetadataReader().read(source)
            require(
                metadata.sampleRateHz == reference.sampleRate &&
                    metadata.channelCount == reference.channels &&
                    metadata.totalFrames == reference.frames,
            ) { "managed prepared-reference audio contract mismatch: ${reference.name}" }
        }
    }

    private fun managedAsset(
        media: ManagedMediaAsset,
        role: AssetRole,
        reference: ValidatedPreparedReference,
        now: Long,
        provenance: AssetProvenance,
    ) = ManagedAsset(
        assetId = idFactory(),
        role = role,
        relativePath = media.relativePath,
        sha256 = reference.sha256,
        byteSize = media.byteCount,
        format = "wav",
        sampleRateHz = reference.sampleRate,
        channelCount = reference.channels,
        frameCount = reference.frames,
        createdAtEpochMs = now,
        classification = AssetClassification.DERIVED,
        lifecycle = AssetLifecycle.MANAGED,
        provenance = provenance,
    )

    private fun splitAsset(
        media: ManagedMediaAsset,
        guitar: ManagedAsset,
        channel: String,
        reference: ValidatedPreparedReference,
        now: Long,
        commonParameters: Map<String, String>,
    ) = ManagedAsset(
        assetId = idFactory(),
        role = AssetRole.REFERENCE_GUITAR,
        relativePath = media.relativePath,
        sha256 = sha256(media.file),
        byteSize = media.byteCount,
        format = "wav",
        sampleRateHz = reference.sampleRate,
        channelCount = 1,
        frameCount = reference.frames,
        createdAtEpochMs = now,
        classification = AssetClassification.DERIVED,
        lifecycle = AssetLifecycle.MANAGED,
        provenance = AssetProvenance(
            kind = "GUITAR_CHANNEL_SPLIT",
            inputAssetIds = listOf(guitar.assetId),
            inputSha256 = listOf(guitar.sha256),
            parameters = commonParameters + ("channel" to channel),
            contractVersion = 2,
            executionMode = "LOCAL",
        ),
    )

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

    private companion object {
        val SHA256 = Regex("[a-f0-9]{64}")
    }
}
