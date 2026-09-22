package studio.guitarlab.app.ui

import android.content.Context
import android.net.Uri
import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import studio.guitarlab.core.codec.FileSeekableByteSource
import studio.guitarlab.core.codec.WavMetadataReader
import studio.guitarlab.core.model.AssetLifecycle
import studio.guitarlab.core.model.AssetRole
import studio.guitarlab.core.model.ManagedAsset
import studio.guitarlab.core.project.FileProjectRepository
import studio.guitarlab.core.project.ProjectBundleWriter
import studio.guitarlab.core.project.ProjectManagedMediaStore
import studio.guitarlab.core.project.StagedFilePublisher
import studio.guitarlab.platform.audio.android.StudioMasterRenderRequestFactory
import studio.guitarlab.platform.audio.android.StudioMasterRenderer
import studio.guitarlab.platform.codec.android.AndroidMasterAudioEncoder
import studio.guitarlab.platform.codec.android.MasterExportFormat

enum class StudyExportKind(val label: String, val fileToken: String, val expectedRole: AssetRole) {
    BACKING("Base sem guitarra", "backing", AssetRole.REFERENCE_BACKING),
    GUITAR("Guitarra de referência", "guitar", AssetRole.REFERENCE_GUITAR),
}

enum class StudyExportStrategy { DIRECT_CANONICAL_WAV, SINGLE_ENCODE }

data class StudyExportResult(
    val projectId: String,
    val assetId: String,
    val kind: StudyExportKind,
    val format: MasterExportFormat,
    val strategy: StudyExportStrategy,
    val sourceSha256: String,
    val sampleRateHz: Int,
    val channelCount: Int,
    val frameCount: Long,
)

object ExportStoragePolicy {
    private const val RESERVE_BYTES = 8L * 1024L * 1024L

    fun requiredTemporaryBytes(sourceBytes: Long, requiresEncodedSibling: Boolean): Long {
        require(sourceBytes > 0)
        val multiplier = if (requiresEncodedSibling) 2L else 1L
        return sourceBytes.coerceAtMost(Long.MAX_VALUE / multiplier) * multiplier + RESERVE_BYTES
    }

    fun requireAvailable(availableBytes: Long, requiredBytes: Long) {
        require(requiredBytes > 0)
        require(availableBytes >= requiredBytes) {
            "Espaço temporário insuficiente para concluir a exportação com segurança."
        }
    }
}

class ProjectExportService(private val context: Context) {
    private val repository = FileProjectRepository(context.filesDir)
    private val mediaStore = ProjectManagedMediaStore(context.filesDir)

    suspend fun saveProject(projectId: String, uri: Uri) = withContext(Dispatchers.IO) {
        val project = repository.load(projectId) ?: error("Projeto não encontrado.")
        val staged = File.createTempFile("guitarlab-project-", ".guitarlab", context.cacheDir)
        try {
            staged.outputStream().buffered().use {
                ProjectBundleWriter().write(project, mediaStore.projectDirectoryForExport(project.id), it)
            }
            require(staged.length() > 0L) { "O pacote GuitarLab gerado está vazio." }
            publishStaged(uri, staged, "O Android não conseguiu criar o arquivo do projeto.")
        } finally {
            staged.delete()
        }
    }

    suspend fun exportStudyReference(
        projectId: String,
        kind: StudyExportKind,
        uri: Uri,
        format: MasterExportFormat,
    ): StudyExportResult = withContext(Dispatchers.IO) {
        val project = repository.load(projectId) ?: error("Projeto não encontrado.")
        val preparation = project.preparation ?: error("O projeto não possui referências preparadas.")
        val assetId = when (kind) {
            StudyExportKind.BACKING -> preparation.activeBackingAssetId
            StudyExportKind.GUITAR -> preparation.activeGuitarAssetId
        } ?: error("${kind.label} ainda não está preparado para exportação.")
        val asset = project.assets.singleOrNull { it.assetId == assetId && it.role == kind.expectedRole }
            ?: error("A referência ativa de ${kind.label.lowercase()} é inválida.")
        val source = validateCanonicalStudyAsset(projectId, asset)
        val contextSnapshot = currentCoroutineContext()
        contextSnapshot.ensureActive()

        val strategy = if (format == MasterExportFormat.WAV_FLOAT32) {
            publishStaged(uri, source, "O Android não conseguiu criar o WAV exportado.")
            StudyExportStrategy.DIRECT_CANONICAL_WAV
        } else {
            val sampleRate = requireNotNull(asset.sampleRateHz)
            val channels = requireNotNull(asset.channelCount)
            require(AndroidMasterAudioEncoder.isSupported(format, sampleRate, channels)) {
                "Este dispositivo não oferece encoder ${format.name} compatível para ${sampleRate} Hz / ${channels} canais."
            }
            val required = ExportStoragePolicy.requiredTemporaryBytes(source.length(), requiresEncodedSibling = false)
            ExportStoragePolicy.requireAvailable(context.cacheDir.usableSpace, required)
            val encoded = File.createTempFile("guitarlab-study-${kind.fileToken}-", ".${format.extension}", context.cacheDir)
            try {
                AndroidMasterAudioEncoder.encode(source, encoded, format) { !contextSnapshot.isActive }
                contextSnapshot.ensureActive()
                publishStaged(uri, encoded, "O Android não conseguiu criar o arquivo ${format.name} exportado.")
            } finally {
                encoded.delete()
            }
            StudyExportStrategy.SINGLE_ENCODE
        }

        StudyExportResult(
            projectId = project.id, assetId = asset.assetId, kind = kind, format = format, strategy = strategy,
            sourceSha256 = asset.sha256, sampleRateHz = requireNotNull(asset.sampleRateHz),
            channelCount = requireNotNull(asset.channelCount), frameCount = requireNotNull(asset.frameCount),
        )
    }

    suspend fun exportMaster(projectId: String, uri: Uri, format: MasterExportFormat) = withContext(Dispatchers.IO) {
        val project = repository.load(projectId) ?: error("Projeto não encontrado.")
        val rate = project.sampleRate.fixedHz ?: project.clips.firstNotNullOfOrNull { it.editingSampleRateHz ?: it.sourceSampleRateHz }
            ?: error("Projeto sem taxa de amostragem exportável.")
        require(AndroidMasterAudioEncoder.isSupported(format, rate, 2)) {
            "Este dispositivo não oferece encoder ${format.name} compatível para ${rate} Hz / 2 canais."
        }
        val request = StudioMasterRenderRequestFactory.create(project, rate) {
            mediaStore.resolveEditable(project.id, it)
        }
        val estimatedFloatWavBytes = 44L + request.projectEndFrame * 2L * 4L
        val required = ExportStoragePolicy.requiredTemporaryBytes(
            estimatedFloatWavBytes,
            requiresEncodedSibling = format != MasterExportFormat.WAV_FLOAT32,
        )
        ExportStoragePolicy.requireAvailable(context.cacheDir.usableSpace, required)
        val contextSnapshot = currentCoroutineContext()
        val floatWav = File.createTempFile("guitarlab-home-master-", ".wav", context.cacheDir)
        val encoded = if (format == MasterExportFormat.WAV_FLOAT32) floatWav else File.createTempFile("guitarlab-home-master-", ".${format.extension}", context.cacheDir)
        try {
            StudioMasterRenderer.renderFloatWav(request, floatWav) { !contextSnapshot.isActive }
            contextSnapshot.ensureActive()
            if (format != MasterExportFormat.WAV_FLOAT32) {
                AndroidMasterAudioEncoder.encode(floatWav, encoded, format) { !contextSnapshot.isActive }
            }
            contextSnapshot.ensureActive()
            val source = if (format == MasterExportFormat.WAV_FLOAT32) floatWav else encoded
            publishStaged(uri, source, "O Android não conseguiu criar o arquivo exportado.")
        } finally {
            floatWav.delete()
            if (encoded != floatWav) encoded.delete()
        }
    }

    private fun validateCanonicalStudyAsset(projectId: String, asset: ManagedAsset): File {
        require(asset.lifecycle == AssetLifecycle.MANAGED) { "A referência ainda não está publicada como asset gerenciado." }
        require(asset.format.equals("wav", ignoreCase = true)) { "A referência canônica precisa permanecer em WAV lossless." }
        val source = mediaStore.resolveAsset(projectId, asset.relativePath)
        require(source.length() == asset.byteSize) { "O tamanho da referência gerenciada não corresponde ao asset publicado." }
        require(sha256(source) == asset.sha256) { "Falha de integridade na referência gerenciada." }
        FileSeekableByteSource(source).use { input ->
            val metadata = WavMetadataReader().read(input)
            require(metadata.sampleRateHz == asset.sampleRateHz && metadata.channelCount == asset.channelCount && metadata.totalFrames == asset.frameCount) {
                "Os metadados do WAV gerenciado não correspondem ao asset publicado."
            }
        }
        return source
    }

    private fun sha256(file: File): String = FileInputStream(file).use { input ->
        val digest = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        while (true) {
            val read = input.read(buffer)
            if (read < 0) break
            if (read > 0) digest.update(buffer, 0, read)
        }
        digest.digest().joinToString("") { "%02x".format(it) }
    }

    private suspend fun publishStaged(uri: Uri, source: File, openError: String) {
        val resolver = context.contentResolver
        StagedFilePublisher.publish(
            source = source,
            openDestination = {
                resolver.openOutputStream(uri, "wt") ?: error(openError)
            },
            resetDestination = {
                resolver.openOutputStream(uri, "wt")?.use { } ?: error("Não foi possível limpar o arquivo parcial.")
            },
        )
    }
}
