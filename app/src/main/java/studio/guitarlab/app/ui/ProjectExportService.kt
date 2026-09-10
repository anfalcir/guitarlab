package studio.guitarlab.app.ui

import android.content.Context
import android.net.Uri
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import studio.guitarlab.core.project.FileProjectRepository
import studio.guitarlab.core.project.ProjectBundleWriter
import studio.guitarlab.core.project.ProjectManagedMediaStore
import studio.guitarlab.platform.audio.android.StudioMasterRenderRequestFactory
import studio.guitarlab.platform.audio.android.StudioMasterRenderer
import studio.guitarlab.platform.codec.android.AndroidMasterAudioEncoder
import studio.guitarlab.platform.codec.android.MasterExportFormat

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
            val output = context.contentResolver.openOutputStream(uri, "w")
                ?: error("O Android não conseguiu criar o arquivo do projeto.")
            output.use { target -> staged.inputStream().buffered().use { it.copyTo(target) } }
        } finally {
            staged.delete()
        }
    }

    suspend fun exportMaster(projectId: String, uri: Uri, format: MasterExportFormat) = withContext(Dispatchers.IO) {
        val project = repository.load(projectId) ?: error("Projeto não encontrado.")
        val rate = project.sampleRate.fixedHz ?: project.clips.firstNotNullOfOrNull { it.editingSampleRateHz ?: it.sourceSampleRateHz }
            ?: error("Projeto sem taxa de amostragem exportável.")
        val request = StudioMasterRenderRequestFactory.create(project, rate) {
            mediaStore.resolveEditable(project.id, it)
        }
        val floatWav = File.createTempFile("guitarlab-home-master-", ".wav", context.cacheDir)
        val encoded = if (format == MasterExportFormat.WAV_FLOAT32) floatWav else File.createTempFile("guitarlab-home-master-", ".${format.extension}", context.cacheDir)
        try {
            StudioMasterRenderer.renderFloatWav(request, floatWav)
            if (format != MasterExportFormat.WAV_FLOAT32) AndroidMasterAudioEncoder.encode(floatWav, encoded, format)
            val source = if (format == MasterExportFormat.WAV_FLOAT32) floatWav else encoded
            val output = context.contentResolver.openOutputStream(uri, "w") ?: error("O Android não conseguiu criar o arquivo exportado.")
            output.use { target -> source.inputStream().buffered().use { it.copyTo(target) } }
        } finally {
            floatWav.delete()
            if (encoded != floatWav) encoded.delete()
        }
    }

}
