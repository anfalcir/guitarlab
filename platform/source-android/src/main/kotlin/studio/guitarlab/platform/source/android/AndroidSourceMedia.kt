package studio.guitarlab.platform.source.android

import android.content.Context
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import android.provider.OpenableColumns
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import studio.guitarlab.core.codec.AudioImportFormatPolicy
import studio.guitarlab.core.codec.FileSeekableByteSource
import studio.guitarlab.core.codec.WavMetadataReader
import studio.guitarlab.core.project.ValidatedSourceMedia
import studio.guitarlab.core.source.SourceAcquisitionPolicy
import studio.guitarlab.platform.codec.android.AndroidAudioImportTranscoder

internal object AndroidSourceMedia {
    suspend fun stageLocal(context: Context, uri: Uri, operationId: String): ValidatedSourceMedia = withContext(Dispatchers.IO) {
        val info = documentInfo(context, uri)
        val format = AudioImportFormatPolicy.detect(info.name, info.mimeType)
            ?: error("Formato não suportado. Use ${AudioImportFormatPolicy.supportedExtensionsDescription}.")
        val dir = File(context.cacheDir, "source-acquisition/$operationId").apply { deleteRecursively(); mkdirs() }
        val extension = info.name.substringAfterLast('.', "bin").lowercase().takeIf { it.matches(Regex("[a-z0-9]{1,8}")) } ?: "bin"
        val staged = File(dir, "local-source.$extension")
        try {
            context.contentResolver.openInputStream(uri)?.buffered()?.use { input ->
                FileOutputStream(staged).use { output ->
                    val buffer = ByteArray(256 * 1024)
                    while (true) {
                        currentCoroutineContext().ensureActive()
                        val read = input.read(buffer)
                        if (read < 0) break
                        if (read > 0) output.write(buffer, 0, read)
                    }
                    output.flush(); output.fd.sync()
                }
            } ?: error("O Android não conseguiu abrir o áudio selecionado.")
            require(staged.length() > 0L) { "O arquivo selecionado está vazio." }

            val metadata = if (format.name == "WAV_PCM") {
                FileSeekableByteSource(staged).use { WavMetadataReader().read(it) }
            } else {
                AndroidAudioImportTranscoder.prepare(context, uri, format).use { prepared ->
                    FileSeekableByteSource(prepared.wavFile).use { WavMetadataReader().read(it) }
                }
            }
            require(metadata.totalFrames > 0L && metadata.durationUs > 0L) { "O arquivo não contém áudio utilizável." }
            ValidatedSourceMedia(
                file = staged,
                suggestedName = info.name.ifBlank { "source.$extension" },
                format = format.name.lowercase(),
                sampleRateHz = metadata.sampleRateHz,
                channelCount = metadata.channelCount,
                frameCount = metadata.totalFrames,
                sourceKind = "local-import",
            )
        } catch (cancelled: CancellationException) {
            dir.deleteRecursively(); throw cancelled
        } catch (error: Throwable) {
            dir.deleteRecursively(); throw error
        }
    }

    fun validateDownloaded(file: File, expectedSeconds: Double, sourceUrl: String, title: String): ValidatedSourceMedia {
        require(file.isFile && file.length() > 0L) { "O download ficou vazio." }
        val extractor = MediaExtractor()
        try {
            extractor.setDataSource(file.absolutePath)
            val audioTrack = (0 until extractor.trackCount).firstOrNull { index ->
                extractor.getTrackFormat(index).getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true
            } ?: error("O download não contém uma faixa de áudio válida.")
            val media = extractor.getTrackFormat(audioTrack)
            val rate = media.intOrNull(MediaFormat.KEY_SAMPLE_RATE)
            val channels = media.intOrNull(MediaFormat.KEY_CHANNEL_COUNT)
            val durationUs = media.longOrNull(MediaFormat.KEY_DURATION) ?: 0L
            val actualSeconds = durationUs / 1_000_000.0
            require(SourceAcquisitionPolicy.isPlausiblyComplete(expectedSeconds, actualSeconds)) {
                "O download parece parcial ou tem duração incompatível; o resultado foi descartado."
            }
            val frameCount = if (rate != null && durationUs > 0L) (durationUs * rate / 1_000_000L).coerceAtLeast(1L) else null
            return ValidatedSourceMedia(
                file = file,
                suggestedName = sanitizeName(title, file.name),
                format = file.extension.lowercase().ifBlank { "audio" },
                sampleRateHz = rate,
                channelCount = channels,
                frameCount = frameCount,
                sourceKind = "online-acquisition",
                sourceUrl = sourceUrl,
            )
        } finally { extractor.release() }
    }

    fun cleanup(context: Context, operationId: String) {
        File(context.cacheDir, "source-acquisition/$operationId").deleteRecursively()
        File(context.filesDir, "source-acquisition/$operationId").deleteRecursively()
    }

    private fun sanitizeName(title: String, fallback: String): String {
        val safe = title.trim().replace(Regex("[^A-Za-z0-9._ -]"), "_").take(88).ifBlank { fallback.substringBeforeLast('.', fallback) }
        val extension = fallback.substringAfterLast('.', "").lowercase().takeIf { it.matches(Regex("[a-z0-9]{1,8}")) }
        return if (extension != null && !safe.lowercase().endsWith(".$extension")) "$safe.$extension" else safe
    }

    private data class DocumentInfo(val name: String, val mimeType: String?)
    private fun documentInfo(context: Context, uri: Uri): DocumentInfo {
        var name = "source-audio"
        runCatching {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) name = cursor.getString(0).orEmpty().ifBlank { name }
            }
        }
        return DocumentInfo(name, context.contentResolver.getType(uri))
    }
    private fun MediaFormat.intOrNull(key: String): Int? = if (containsKey(key)) runCatching { getInteger(key) }.getOrNull() else null
    private fun MediaFormat.longOrNull(key: String): Long? = if (containsKey(key)) runCatching { getLong(key) }.getOrNull() else null
}
