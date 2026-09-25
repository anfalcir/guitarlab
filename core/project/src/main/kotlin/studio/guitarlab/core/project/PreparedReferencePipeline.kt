package studio.guitarlab.core.project

import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest
import java.util.UUID
import kotlin.math.abs
import kotlin.math.log10
import kotlin.math.min
import kotlin.math.pow
import studio.guitarlab.core.codec.FileSeekableByteSource
import studio.guitarlab.core.codec.FloatWavFileWriter
import studio.guitarlab.core.codec.StereoWavChannelSplitter
import studio.guitarlab.core.codec.WavMetadataReader
import studio.guitarlab.core.codec.WavPcmDecoder
import studio.guitarlab.core.model.AssetClassification
import studio.guitarlab.core.model.AssetLifecycle
import studio.guitarlab.core.model.AssetProvenance
import studio.guitarlab.core.model.AssetRole
import studio.guitarlab.core.model.AudioClip
import studio.guitarlab.core.model.BuiltInRoles
import studio.guitarlab.core.model.GuitarProject
import studio.guitarlab.core.model.ManagedAsset
import studio.guitarlab.core.model.PreparationStatus
import studio.guitarlab.core.model.ReferenceBinding
import studio.guitarlab.core.model.ReferenceBindingKind

private const val RECIPE_VERSION = "backing-v1"
private const val CONTRACT_VERSION = 1
private const val TARGET_PEAK_DBFS = -1.0
private const val BUFFER_FRAMES = 4096

private val BACKING_ROLES = listOf(
    AssetRole.STEM_DRUMS,
    AssetRole.STEM_BASS,
    AssetRole.STEM_OTHER,
    AssetRole.STEM_VOCALS,
    AssetRole.STEM_PIANO,
)
private val REQUIRED_STEM_ROLES = (BACKING_ROLES + AssetRole.STEM_GUITAR).toSet()

data class PreparedReferenceResult(
    val project: GuitarProject,
    val reusedExisting: Boolean,
    val sharedGainDb: Double,
)

data class PreparedReferenceRenderResult(
    val sampleRateHz: Int,
    val channels: Int,
    val frames: Long,
    val sharedGainDb: Double,
)

/**
 * Deterministic, bounded-memory renderer for the U5 backing/guitar pair.
 *
 * The five non-guitar stems form the backing. One common gain is computed against the loudest
 * of backing, guitar and their recombination, then applied to BOTH outputs. This preserves the
 * source balance and also prevents either delivered reference from exceeding the -1 dBFS ceiling.
 */
object PreparedReferenceRenderer {
    fun render(
        stems: Map<AssetRole, File>,
        backingOutput: File,
        guitarOutput: File,
        targetPeakDbfs: Double = TARGET_PEAK_DBFS,
    ): PreparedReferenceRenderResult {
        require(stems.keys.containsAll(REQUIRED_STEM_ROLES)) { "A preparação exige os seis stems Demucs." }
        val opened = REQUIRED_STEM_ROLES.associateWith { role -> open(stems.getValue(role)) }
        try {
            val first = opened.values.first().decoder.metadata
            require(first.channelCount == 2 && first.totalFrames > 0L) { "Os stems precisam ser estéreo e não vazios." }
            require(opened.values.all {
                val m = it.decoder.metadata
                m.sampleRateHz == first.sampleRateHz && m.channelCount == first.channelCount && m.totalFrames == first.totalFrames
            }) { "Os seis stems precisam estar perfeitamente alinhados em taxa, canais e frames." }

            val peak = measurePeak(opened)
            val ceiling = 10.0.pow(targetPeakDbfs / 20.0).toFloat()
            val gain = if (peak > ceiling && peak > 0f) ceiling / peak else 1f
            opened.values.forEach { it.decoder.seekToFrame(0) }

            backingOutput.parentFile?.mkdirs()
            guitarOutput.parentFile?.mkdirs()
            FloatWavFileWriter(backingOutput, first.sampleRateHz, 2).use { backingWriter ->
                FloatWavFileWriter(guitarOutput, first.sampleRateHz, 2).use { guitarWriter ->
                    val buffers = opened.mapValues { FloatArray(BUFFER_FRAMES * 2) }
                    val backing = FloatArray(BUFFER_FRAMES * 2)
                    val guitar = FloatArray(BUFFER_FRAMES * 2)
                    while (true) {
                        val frameCounts = opened.mapValues { (role, audio) ->
                            audio.decoder.readInterleaved(buffers.getValue(role), 0, BUFFER_FRAMES)
                        }
                        val frames = frameCounts.values.first()
                        if (frames <= 0) break
                        require(frameCounts.values.all { it == frames }) { "Os stems perderam alinhamento durante a leitura." }
                        val samples = frames * 2
                        for (sample in 0 until samples) {
                            var sum = 0f
                            BACKING_ROLES.forEach { role -> sum += buffers.getValue(role)[sample] }
                            backing[sample] = sum * gain
                            guitar[sample] = buffers.getValue(AssetRole.STEM_GUITAR)[sample] * gain
                        }
                        backingWriter.writeInterleaved(backing, frames)
                        guitarWriter.writeInterleaved(guitar, frames)
                    }
                }
            }
            return PreparedReferenceRenderResult(
                sampleRateHz = first.sampleRateHz,
                channels = 2,
                frames = first.totalFrames,
                sharedGainDb = if (gain >= 0.999999f) 0.0 else 20.0 * log10(gain.toDouble()),
            )
        } finally {
            opened.values.forEach { it.close() }
        }
    }

    private fun measurePeak(opened: Map<AssetRole, OpenAudio>): Float {
        opened.values.forEach { it.decoder.seekToFrame(0) }
        val buffers = opened.mapValues { FloatArray(BUFFER_FRAMES * 2) }
        var maximum = 0f
        while (true) {
            val frameCounts = opened.mapValues { (role, audio) ->
                audio.decoder.readInterleaved(buffers.getValue(role), 0, BUFFER_FRAMES)
            }
            val frames = frameCounts.values.first()
            if (frames <= 0) break
            require(frameCounts.values.all { it == frames }) { "Os stems perderam alinhamento durante a análise." }
            val samples = frames * 2
            for (sample in 0 until samples) {
                var backing = 0f
                BACKING_ROLES.forEach { role -> backing += buffers.getValue(role)[sample] }
                val guitar = buffers.getValue(AssetRole.STEM_GUITAR)[sample]
                maximum = maxOf(maximum, abs(backing), abs(guitar), abs(backing + guitar))
            }
        }
        return maximum
    }

    private data class OpenAudio(val source: FileSeekableByteSource, val decoder: WavPcmDecoder) : AutoCloseable {
        override fun close() = source.close()
    }

    private fun open(file: File): OpenAudio {
        val source = FileSeekableByteSource(file)
        return try { OpenAudio(source, WavPcmDecoder(source)) } catch (error: Throwable) { source.close(); throw error }
    }
}

class PreparedReferenceService(
    private val repository: ProjectRepository,
    private val mediaStore: ProjectManagedMediaStore,
    private val tempDirectory: File,
    private val nowMs: () -> Long = System::currentTimeMillis,
    private val idFactory: () -> String = { UUID.randomUUID().toString() },
) {
    fun prepare(projectId: String): PreparedReferenceResult {
        val before = requireNotNull(repository.load(projectId)) { "Projeto não encontrado: $projectId" }
        val preparation = requireNotNull(before.preparation) { "O projeto ainda não possui preparação." }
        require(preparation.status == PreparationStatus.READY) { "A separação precisa estar concluída antes de criar as referências." }
        require(preparation.activeStemAssetIds.keys.containsAll(REQUIRED_STEM_ROLES)) { "O conjunto ativo de stems está incompleto." }

        val stemAssets = REQUIRED_STEM_ROLES.associateWith { role ->
            val assetId = requireNotNull(preparation.activeStemAssetIds[role]) { "Stem ausente: $role" }
            before.assets.singleOrNull { it.assetId == assetId && it.role == role }
                ?: error("Asset de stem ausente ou com função incorreta: $role")
        }
        stemAssets.values.forEach { validateManagedAsset(projectId, it) }
        val fingerprint = recipeFingerprint(stemAssets)
        val reusable = findPreparedSet(before, fingerprint)
        if (reusable != null) {
            val beforePreparation = requireNotNull(before.preparation)
            val updated = if (beforePreparation.activeBackingAssetId == reusable.backing.assetId && beforePreparation.activeGuitarAssetId == reusable.guitar.assetId) before else {
                repository.save(before.copy(
                    updatedAtEpochMs = nowMs(),
                    preparation = preparation.copy(
                        activeBackingAssetId = reusable.backing.assetId,
                        activeGuitarAssetId = reusable.guitar.assetId,
                        availableReferenceAssetIds = (preparation.availableReferenceAssetIds + reusable.all.map { it.assetId }).distinct(),
                    ),
                ))
            }
            return PreparedReferenceResult(updated, reusedExisting = true, sharedGainDb = reusable.backing.provenance?.parameters?.get("sharedGainDb")?.toDoubleOrNull() ?: 0.0)
        }

        val work = File(tempDirectory, "u5-${UUID.randomUUID()}").also { it.mkdirs() }
        val publishedPaths = mutableListOf<String>()
        try {
            val backingTemp = File(work, "backing.wav")
            val guitarTemp = File(work, "guitar.wav")
            val render = PreparedReferenceRenderer.render(stemAssets.mapValues { mediaStore.resolveAsset(projectId, it.value.relativePath) }, backingTemp, guitarTemp)
            val leftTemp = File(work, "guitar-left.wav")
            val rightTemp = File(work, "guitar-right.wav")
            val split = StereoWavChannelSplitter.split(guitarTemp, leftTemp, rightTemp)
            require(split.sampleRateHz == render.sampleRateHz && split.totalFrames == render.frames)

            val backingManaged = backingTemp.inputStream().buffered().use { mediaStore.ingestReference(projectId, "backing-$fingerprint.wav", it) }.also { publishedPaths += it.relativePath }
            val guitarManaged = guitarTemp.inputStream().buffered().use { mediaStore.ingestReference(projectId, "guitar-$fingerprint.wav", it) }.also { publishedPaths += it.relativePath }
            val leftManaged = leftTemp.inputStream().buffered().use { mediaStore.ingestReference(projectId, "guitar-left-$fingerprint.wav", it) }.also { publishedPaths += it.relativePath }
            val rightManaged = rightTemp.inputStream().buffered().use { mediaStore.ingestReference(projectId, "guitar-right-$fingerprint.wav", it) }.also { publishedPaths += it.relativePath }

            val latest = requireNotNull(repository.load(projectId))
            require(recipeFingerprintFromProject(latest) == fingerprint) { "Os stems ativos mudaram antes da publicação das referências." }
            val inputIds = REQUIRED_STEM_ROLES.sortedBy { it.name }.map { stemAssets.getValue(it).assetId }
            val inputHashes = REQUIRED_STEM_ROLES.sortedBy { it.name }.map { stemAssets.getValue(it).sha256 }
            val baseParameters = mapOf(
                "recipe" to RECIPE_VERSION,
                "recipeFingerprint" to fingerprint,
                "targetPeakDbfs" to TARGET_PEAK_DBFS.toString(),
                "sharedGainDb" to "%.6f".format(java.util.Locale.US, render.sharedGainDb),
            )
            val now = nowMs()
            val backing = managedAsset(backingManaged, AssetRole.REFERENCE_BACKING, render.sampleRateHz, 2, render.frames, now,
                AssetProvenance("PREPARED_BACKING", inputIds, inputHashes, parameters = baseParameters + ("includedRoles" to BACKING_ROLES.joinToString(",") { it.name }), contractVersion = CONTRACT_VERSION))
            val guitar = managedAsset(guitarManaged, AssetRole.REFERENCE_GUITAR, render.sampleRateHz, 2, render.frames, now,
                AssetProvenance("PREPARED_GUITAR", inputIds, inputHashes, parameters = baseParameters + ("sourceRole" to AssetRole.STEM_GUITAR.name), contractVersion = CONTRACT_VERSION))
            val left = managedAsset(leftManaged, AssetRole.REFERENCE_GUITAR, render.sampleRateHz, 1, render.frames, now,
                AssetProvenance("GUITAR_CHANNEL_SPLIT", listOf(guitar.assetId), listOf(guitar.sha256), parameters = mapOf("channel" to "LEFT", "recipeFingerprint" to fingerprint), contractVersion = CONTRACT_VERSION))
            val right = managedAsset(rightManaged, AssetRole.REFERENCE_GUITAR, render.sampleRateHz, 1, render.frames, now,
                AssetProvenance("GUITAR_CHANNEL_SPLIT", listOf(guitar.assetId), listOf(guitar.sha256), parameters = mapOf("channel" to "RIGHT", "recipeFingerprint" to fingerprint), contractVersion = CONTRACT_VERSION))
            val references = listOf(backing, guitar, left, right)
            val prepared = latest.copy(
                updatedAtEpochMs = now,
                assets = latest.assets + references,
                preparation = requireNotNull(latest.preparation).let { latestPreparation ->
                    latestPreparation.copy(
                        activeBackingAssetId = backing.assetId,
                        activeGuitarAssetId = guitar.assetId,
                        availableReferenceAssetIds = (latestPreparation.availableReferenceAssetIds + references.map { it.assetId }).distinct(),
                    )
                },
            )
            val bound = PreparedReferenceBindingPolicy.applyInitialBindings(prepared, now, idFactory)
            return PreparedReferenceResult(repository.save(bound), reusedExisting = false, sharedGainDb = render.sharedGainDb)
        } catch (error: Throwable) {
            publishedPaths.forEach { path -> runCatching { mediaStore.discardUncommitted(projectId, path) } }
            throw error
        } finally {
            work.deleteRecursively()
        }
    }

    private fun managedAsset(media: ManagedMediaAsset, role: AssetRole, sampleRate: Int, channels: Int, frames: Long, now: Long, provenance: AssetProvenance) = ManagedAsset(
        assetId = idFactory(), role = role, relativePath = media.relativePath, sha256 = sha256(media.file), byteSize = media.byteCount,
        format = "wav", sampleRateHz = sampleRate, channelCount = channels, frameCount = frames, createdAtEpochMs = now,
        classification = AssetClassification.DERIVED, lifecycle = AssetLifecycle.MANAGED, provenance = provenance,
    )

    private fun validateManagedAsset(projectId: String, asset: ManagedAsset) {
        require(asset.lifecycle == AssetLifecycle.MANAGED && asset.format.equals("wav", ignoreCase = true)) { "Stem não está publicado como WAV gerenciado." }
        val file = mediaStore.resolveAsset(projectId, asset.relativePath)
        require(file.length() == asset.byteSize && sha256(file) == asset.sha256) { "Falha de integridade no stem ${asset.role}." }
        FileSeekableByteSource(file).use { source ->
            val metadata = WavMetadataReader().read(source)
            require(metadata.sampleRateHz == asset.sampleRateHz && metadata.channelCount == asset.channelCount && metadata.totalFrames == asset.frameCount) { "Metadados do stem ${asset.role} não correspondem ao asset publicado." }
        }
    }

    private fun recipeFingerprint(stems: Map<AssetRole, ManagedAsset>): String {
        val canonical = REQUIRED_STEM_ROLES.sortedBy { it.name }.joinToString("\n", prefix = "$RECIPE_VERSION\n") { role -> "$role:${stems.getValue(role).assetId}:${stems.getValue(role).sha256}" }
        return sha256(canonical.toByteArray()).take(24)
    }

    private fun recipeFingerprintFromProject(project: GuitarProject): String {
        val preparation = requireNotNull(project.preparation)
        return recipeFingerprint(REQUIRED_STEM_ROLES.associateWith { role -> project.assets.single { it.assetId == preparation.activeStemAssetIds[role] && it.role == role } })
    }

    private data class PreparedSet(val backing: ManagedAsset, val guitar: ManagedAsset, val left: ManagedAsset, val right: ManagedAsset) { val all get() = listOf(backing, guitar, left, right) }
    private fun findPreparedSet(project: GuitarProject, fingerprint: String): PreparedSet? {
        val matching = project.assets.filter { it.provenance?.parameters?.get("recipeFingerprint") == fingerprint }
        val backing = matching.singleOrNull { it.role == AssetRole.REFERENCE_BACKING && it.provenance?.kind == "PREPARED_BACKING" } ?: return null
        val guitar = matching.singleOrNull { it.role == AssetRole.REFERENCE_GUITAR && it.provenance?.kind == "PREPARED_GUITAR" } ?: return null
        val left = matching.singleOrNull { asset ->
            asset.provenance?.let { provenance -> provenance.kind == "GUITAR_CHANNEL_SPLIT" && provenance.parameters["channel"] == "LEFT" } == true
        } ?: return null
        val right = matching.singleOrNull { asset ->
            asset.provenance?.let { provenance -> provenance.kind == "GUITAR_CHANNEL_SPLIT" && provenance.parameters["channel"] == "RIGHT" } == true
        } ?: return null
        listOf(backing, guitar, left, right).forEach { validateManagedAsset(project.id, it) }
        return PreparedSet(backing, guitar, left, right)
    }

    private fun sha256(file: File): String = FileInputStream(file).use { input ->
        val digest = MessageDigest.getInstance("SHA-256"); val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        while (true) { val read = input.read(buffer); if (read < 0) break; if (read > 0) digest.update(buffer, 0, read) }
        digest.digest().joinToString("") { "%02x".format(it) }
    }
    private fun sha256(bytes: ByteArray): String = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
}

enum class PreparedReferenceRestoreTarget { BACKING, GUITAR }

object PreparedReferenceBindingPolicy {
    fun updateAvailable(project: GuitarProject): Boolean {
        val desired = desired(project)
        if (desired.isEmpty()) return false
        return bindingDiffersFromDesired(project) && project.preparation?.acknowledgedReferenceRevisionId != desiredRevisionId(desired)
    }

    fun bindingDiffersFromDesired(project: GuitarProject): Boolean {
        val desired = desired(project)
        return desired.isNotEmpty() && desired.any { (kind, asset) ->
            val binding = project.referenceBindings.singleOrNull { it.kind == kind && it.assetId == asset.assetId }
            binding == null || project.clips.none {
                it.trackId == binding.trackId && it.sourceUri == "guitarlab://asset/${asset.assetId}"
            }
        }
    }

    fun desiredRevisionId(project: GuitarProject): String? = desired(project).takeIf { it.isNotEmpty() }?.let(::desiredRevisionId)

    fun acknowledgeCurrent(project: GuitarProject, now: Long): GuitarProject {
        val desired = desired(project)
        require(desired.isNotEmpty()) { "Não há referências preparadas ativas." }
        return project.copy(
            updatedAtEpochMs = now,
            preparation = requireNotNull(project.preparation).copy(
                acknowledgedReferenceRevisionId = desiredRevisionId(desired),
            ),
        )
    }

    fun applyInitialBindings(project: GuitarProject, now: Long, idFactory: () -> String): GuitarProject {
        val desired = desired(project)
        if (desired.isEmpty()) return project
        var next = project
        desired.forEach { (kind, asset) ->
            val track = targetTrack(next, kind) ?: return@forEach
            val existingClips = next.clips.filter { it.trackId == track.id }
            if (existingClips.isEmpty()) next = bind(next, kind, asset, track.id, now, idFactory, replaceExisting = false)
        }
        return next
    }

    fun applyUpdate(project: GuitarProject, now: Long, idFactory: () -> String = { UUID.randomUUID().toString() }): GuitarProject {
        val desired = desired(project)
        require(desired.isNotEmpty()) { "Não há referências preparadas ativas." }
        var next = project
        desired.forEach { (kind, asset) ->
            val track = targetTrack(next, kind) ?: return@forEach
            next = bind(next, kind, asset, track.id, now, idFactory, replaceExisting = true)
        }
        return next.copy(
            updatedAtEpochMs = now,
            preparation = requireNotNull(next.preparation).copy(
                acknowledgedReferenceRevisionId = desiredRevisionId(desired),
            ),
        )
    }

    fun availableRestoreTargets(project: GuitarProject): Set<PreparedReferenceRestoreTarget> {
        val desired = desired(project)
        if (desired.isEmpty()) return emptySet()
        return buildSet {
            if (ReferenceBindingKind.BACKING in desired) add(PreparedReferenceRestoreTarget.BACKING)
            if (desired.keys.any { it in GUITAR_BINDING_KINDS }) add(PreparedReferenceRestoreTarget.GUITAR)
        }
    }

    /**
     * Explicit user-owned restoration from the current canonical prepared assets.
     *
     * Unlike applyUpdate/repair, this intentionally resets clip-level edits for the selected
     * reference family even when bindings already look consistent. Track mixer state, unrelated
     * audio on other tracks, takes, markers, sections and every unselected reference family are preserved.
     */
    fun restoreSelected(
        project: GuitarProject,
        targets: Set<PreparedReferenceRestoreTarget>,
        now: Long,
        idFactory: () -> String = { UUID.randomUUID().toString() },
    ): GuitarProject {
        require(targets.isNotEmpty()) { "Selecione ao menos uma referência para recolocar." }
        val desired = desired(project)
        require(desired.isNotEmpty()) { "Não há referências preparadas ativas." }
        val available = availableRestoreTargets(project)
        require(targets.all { it in available }) { "A referência selecionada não está disponível neste projeto." }

        var next = project
        if (PreparedReferenceRestoreTarget.BACKING in targets) {
            next = restoreFamily(
                project = next,
                desired = desired.filterKeys { it == ReferenceBindingKind.BACKING },
                familyKinds = setOf(ReferenceBindingKind.BACKING),
                assetRole = AssetRole.REFERENCE_BACKING,
                now = now,
                idFactory = idFactory,
            )
        }
        if (PreparedReferenceRestoreTarget.GUITAR in targets) {
            next = restoreFamily(
                project = next,
                desired = desired.filterKeys { it in GUITAR_BINDING_KINDS },
                familyKinds = GUITAR_BINDING_KINDS,
                assetRole = AssetRole.REFERENCE_GUITAR,
                now = now,
                idFactory = idFactory,
            )
        }
        return next.copy(
            updatedAtEpochMs = now,
            preparation = requireNotNull(next.preparation).copy(
                acknowledgedReferenceRevisionId = desiredRevisionId(desired),
            ),
        )
    }

    private fun desired(project: GuitarProject): Map<ReferenceBindingKind, ManagedAsset> {
        val prep = project.preparation ?: return emptyMap()
        val backing = prep.activeBackingAssetId?.let { id -> project.assets.singleOrNull { it.assetId == id && it.role == AssetRole.REFERENCE_BACKING } } ?: return emptyMap()
        val guitar = prep.activeGuitarAssetId?.let { id -> project.assets.singleOrNull { it.assetId == id && it.role == AssetRole.REFERENCE_GUITAR } } ?: return emptyMap()
        val children = project.assets.filter { asset ->
            asset.role == AssetRole.REFERENCE_GUITAR && asset.provenance?.let { provenance ->
                provenance.kind == "GUITAR_CHANNEL_SPLIT" && provenance.inputAssetIds == listOf(guitar.assetId)
            } == true
        }
        val left = children.singleOrNull { it.provenance?.parameters?.get("channel") == "LEFT" }
        val right = children.singleOrNull { it.provenance?.parameters?.get("channel") == "RIGHT" }
        return buildMap {
            put(ReferenceBindingKind.BACKING, backing)
            if (left != null && right != null) { put(ReferenceBindingKind.GUITAR_LEFT, left); put(ReferenceBindingKind.GUITAR_RIGHT, right) }
            else put(ReferenceBindingKind.GUITAR_REFERENCE, guitar)
        }
    }

    private fun desiredRevisionId(desired: Map<ReferenceBindingKind, ManagedAsset>): String {
        val canonical = desired.entries.sortedBy { it.key.name }.joinToString("\n", prefix = "prepared-reference-v1\n") {
            (kind, asset) -> "${kind.name}:${asset.assetId}:${asset.sha256}"
        }
        return MessageDigest.getInstance("SHA-256")
            .digest(canonical.toByteArray())
            .joinToString("") { "%02x".format(it) }
    }

    private fun targetTrack(project: GuitarProject, kind: ReferenceBindingKind) = project.tracks.firstOrNull { track ->
        track.roleId == when (kind) {
            ReferenceBindingKind.BACKING -> BuiltInRoles.BACKING
            ReferenceBindingKind.GUITAR_REFERENCE -> BuiltInRoles.REFERENCE_GUITAR
            ReferenceBindingKind.GUITAR_LEFT -> BuiltInRoles.REFERENCE_GUITAR_L
            ReferenceBindingKind.GUITAR_RIGHT -> BuiltInRoles.REFERENCE_GUITAR_R
        }
    }

    private fun restoreFamily(
        project: GuitarProject,
        desired: Map<ReferenceBindingKind, ManagedAsset>,
        familyKinds: Set<ReferenceBindingKind>,
        assetRole: AssetRole,
        now: Long,
        idFactory: () -> String,
    ): GuitarProject {
        require(desired.isNotEmpty()) { "A família de referência selecionada não está disponível." }
        val targetTrackIds = familyKinds.mapNotNull { targetTrack(project, it)?.id }.toSet()
        val retainedClips = project.clips.filterNot { it.trackId in targetTrackIds }
        val previousBindings = project.referenceBindings.associateBy { it.kind }
        var next = project.copy(
            clips = retainedClips,
            referenceBindings = project.referenceBindings.filterNot { it.kind in familyKinds },
        )
        desired.forEach { (kind, asset) ->
            val track = targetTrack(next, kind) ?: error("A pista de referência necessária não existe.")
            val frameCount = requireNotNull(asset.frameCount) { "A referência preparada não possui duração válida." }
            require(frameCount > 0L) { "A referência preparada está vazia." }
            val clip = AudioClip(
                id = idFactory(),
                trackId = track.id,
                name = when (kind) {
                    ReferenceBindingKind.BACKING -> "Base preparada"
                    ReferenceBindingKind.GUITAR_LEFT -> "Guitarra Ref. E"
                    ReferenceBindingKind.GUITAR_RIGHT -> "Guitarra Ref. D"
                    ReferenceBindingKind.GUITAR_REFERENCE -> "Guitarra de referência"
                },
                sourceUri = "guitarlab://asset/${asset.assetId}",
                startFrame = 0L,
                sourceStartFrame = 0L,
                lengthFrames = frameCount,
                gainDb = 0f,
                muted = false,
                managedSourcePath = asset.relativePath,
                originUri = "prepared:${asset.assetId}",
                sourceFormat = "wav",
                sourceSampleRateHz = asset.sampleRateHz,
                sourceChannelCount = asset.channelCount,
                sourceBitsPerSample = 32,
                sourceEncoding = "FLOAT32_LE",
                sourceTotalFrames = frameCount,
                editingSampleRateHz = asset.sampleRateHz,
                editingTotalFrames = frameCount,
                fadeInFrames = 0L,
                fadeOutFrames = 0L,
            )
            val binding = ReferenceBinding(
                bindingId = previousBindings[kind]?.bindingId ?: idFactory(),
                trackId = track.id,
                assetId = asset.assetId,
                kind = kind,
                createdAtEpochMs = now,
            )
            next = next.copy(
                clips = next.clips + clip,
                referenceBindings = next.referenceBindings + binding,
            )
        }
        return next
    }

    private fun bind(project: GuitarProject, kind: ReferenceBindingKind, asset: ManagedAsset, trackId: String, now: Long, idFactory: () -> String, replaceExisting: Boolean): GuitarProject {
        val oldBinding = project.referenceBindings.firstOrNull { it.kind == kind }
        val existingBoundClip = oldBinding?.let { binding -> project.clips.firstOrNull { it.trackId == trackId && it.sourceUri == "guitarlab://asset/${binding.assetId}" } }
        if (oldBinding?.assetId == asset.assetId && existingBoundClip != null) return project
        val safeSourceStart = (existingBoundClip?.sourceStartFrame ?: 0L).coerceIn(0L, asset.frameCount!! - 1L)
        val safeLength = min(existingBoundClip?.lengthFrames ?: asset.frameCount!!, asset.frameCount!! - safeSourceStart).coerceAtLeast(1L)
        val clip = AudioClip(
            id = existingBoundClip?.id ?: idFactory(), trackId = trackId, name = when(kind) { ReferenceBindingKind.BACKING -> "Base preparada"; ReferenceBindingKind.GUITAR_LEFT -> "Guitarra Ref. E"; ReferenceBindingKind.GUITAR_RIGHT -> "Guitarra Ref. D"; ReferenceBindingKind.GUITAR_REFERENCE -> "Guitarra de referência" },
            sourceUri = "guitarlab://asset/${asset.assetId}", startFrame = existingBoundClip?.startFrame ?: 0L,
            sourceStartFrame = safeSourceStart,
            lengthFrames = safeLength,
            gainDb = existingBoundClip?.gainDb ?: 0f, muted = existingBoundClip?.muted ?: false,
            managedSourcePath = asset.relativePath, originUri = "prepared:${asset.assetId}", sourceFormat = "wav",
            sourceSampleRateHz = asset.sampleRateHz, sourceChannelCount = asset.channelCount, sourceBitsPerSample = 32, sourceEncoding = "FLOAT32_LE", sourceTotalFrames = asset.frameCount,
            editingSampleRateHz = asset.sampleRateHz, editingTotalFrames = asset.frameCount,
            fadeInFrames = (existingBoundClip?.fadeInFrames ?: 0L).coerceAtMost(safeLength), fadeOutFrames = (existingBoundClip?.fadeOutFrames ?: 0L).coerceAtMost(safeLength),
        )
        val bindings = project.referenceBindings.filterNot { it.kind == kind } + ReferenceBinding(oldBinding?.bindingId ?: idFactory(), trackId, asset.assetId, kind, now)
        val clips = when {
            existingBoundClip != null -> project.clips.map { if (it.id == existingBoundClip.id) clip else it }
            !replaceExisting && project.clips.any { it.trackId == trackId } -> project.clips
            else -> project.clips + clip
        }
        return project.copy(referenceBindings = bindings, clips = clips)
    }

    private val GUITAR_BINDING_KINDS = setOf(
        ReferenceBindingKind.GUITAR_REFERENCE,
        ReferenceBindingKind.GUITAR_LEFT,
        ReferenceBindingKind.GUITAR_RIGHT,
    )
}
