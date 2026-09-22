package studio.guitarlab.platform.source.android

import android.content.Context
import java.io.File
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import studio.guitarlab.core.source.SourceAcquisitionPolicy
import studio.guitarlab.core.source.SourceProvider

data class OnlineSourceRequest(
    val url: String,
    val provider: SourceProvider,
    val formatId: String,
    val expectedDurationSeconds: Double,
    val title: String,
)

internal object YtDlpSourceDownloader {
    private val audioExtensions = setOf("wav", "flac", "m4a", "mp3", "ogg", "opus", "webm", "aac", "aiff", "aif", "mka", "mp4", "mov")

    suspend fun download(context: Context, request: OnlineSourceRequest, operationId: String, onProgress: (Int, String) -> Unit): File {
        require(request.url.startsWith("https://") || request.url.startsWith("http://")) { "A URL da fonte é inválida." }
        val root = File(context.filesDir, "source-acquisition/$operationId").apply { deleteRecursively(); mkdirs() }
        var success = false
        try {
            onProgress(2, "Revalidando a fonte selecionada…")
            val inspected = YtDlpDiscoveryProvider(context).inspectUrl(request.url, request.provider)
            require(!inspected.previewOnly) { "A fonte selecionada oferece apenas um trecho curto." }
            val expected = request.expectedDurationSeconds.takeIf { it > 0.0 } ?: inspected.durationSeconds
            val fresh = inspected.formatId.ifBlank { request.formatId.ifBlank { "bestaudio/best" } }
            val attempts = downloadAttempts(request.provider, fresh)
            var lastFailure: Throwable? = null
            attempts.forEachIndexed { index, attempt ->
                currentCoroutineContext().ensureActive()
                root.listFiles()?.forEach { it.deleteRecursively() }
                if (attempt.forceUpdate) {
                    onProgress(5, "Atualizando o motor de aquisição…")
                    runCatching { YtDlpRuntime.updateNightly(context, true) }
                }
                val template = File(root, "original.%(ext)s").absolutePath
                try {
                    YtDlpRuntime.execute(
                        context,
                        request.url,
                        buildList {
                            add("-f" to attempt.format)
                            add("--no-playlist" to null); add("--no-part" to null); add("--newline" to null)
                            add("--retries" to "2"); add("--fragment-retries" to "2"); add("--socket-timeout" to "20")
                            add("-o" to template)
                            attempt.extractorArgs?.let { add("--extractor-args" to it) }
                        },
                        processId(operationId, index),
                    ) { progress, eta, _ ->
                        val mapped = (8 + progress.coerceIn(0f, 100f) * 0.72f).toInt().coerceIn(8, 80)
                        onProgress(mapped, "Download ${progress.coerceIn(0f, 100f).toInt()}%" + if (eta > 0) " • ~${eta}s" else "")
                    }
                    lastFailure = null
                    val file = root.listFiles()?.filter { it.isFile && it.extension.lowercase() in audioExtensions && !it.name.endsWith(".part") }?.maxByOrNull { it.length() }
                        ?: error("Download terminou, mas o áudio não foi localizado.")
                    require(file.length() > 0L) { "O arquivo baixado ficou vazio." }
                    onProgress(84, "Validando integridade e duração…")
                    AndroidSourceMedia.validateDownloaded(file, expected, request.url, request.title)
                    success = true
                    return file
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (error: Throwable) {
                    lastFailure = error
                    if (!SourceAcquisitionPolicy.retryableMessage(error.message) || index == attempts.lastIndex) throw error
                    onProgress(5, "A rota falhou; tentando uma alternativa compatível…")
                }
            }
            throw lastFailure ?: error("Não foi possível baixar a fonte.")
        } finally {
            if (!success) root.deleteRecursively()
        }
    }

    fun cancel(operationId: String) { repeat(4) { YtDlpRuntime.cancel(processId(operationId, it)) } }

    private data class Attempt(val format: String, val extractorArgs: String? = null, val forceUpdate: Boolean = false)
    private fun downloadAttempts(provider: SourceProvider, fresh: String): List<Attempt> = if (provider != SourceProvider.YOUTUBE) {
        listOf(Attempt(fresh.ifBlank { "bestaudio/best" }))
    } else {
        listOf(
            Attempt(fresh.ifBlank { "bestaudio/best" }),
            Attempt("bestaudio/best", "youtube:player_client=android_vr", true),
            Attempt("bestaudio[protocol^=http]/bestaudio/best", "youtube:player_client=web_embedded"),
        )
    }
    private fun processId(operationId: String, attempt: Int) = "guitarlab-source-$operationId-a$attempt"
}
