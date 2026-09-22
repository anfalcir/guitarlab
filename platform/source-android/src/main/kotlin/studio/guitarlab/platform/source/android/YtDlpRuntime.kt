package studio.guitarlab.platform.source.android

import android.content.Context
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal object YtDlpRuntime {
    private val initialized = AtomicBoolean(false)
    private val updateLock = Any()

    suspend fun ensureInitialized(context: Context) = withContext(Dispatchers.IO) {
        if (initialized.get()) return@withContext
        synchronized(this@YtDlpRuntime) {
            if (!initialized.get()) {
                YoutubeDL.getInstance().init(context.applicationContext)
                initialized.set(true)
            }
        }
    }

    suspend fun executeJson(context: Context, target: String, options: List<Pair<String, String?>> = emptyList(), processId: String): String =
        withContext(Dispatchers.IO) {
            ensureInitialized(context)
            val request = request(target, options + listOf("--socket-timeout" to "10", "--retries" to "1", "--extractor-retries" to "1"))
            try {
                YoutubeDL.getInstance().execute(request, processId).out.orEmpty()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Throwable) {
                throw IllegalStateException("Falha no yt-dlp: ${error.message ?: error::class.java.simpleName}", error)
            }
        }

    suspend fun execute(context: Context, target: String, options: List<Pair<String, String?>>, processId: String, onProgress: (Float, Long, String) -> Unit): String =
        withContext(Dispatchers.IO) {
            ensureInitialized(context)
            try {
                YoutubeDL.getInstance().execute(request(target, options), processId) { progress, eta, line -> onProgress(progress, eta, line) }.out.orEmpty()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Throwable) {
                throw IllegalStateException("Falha no yt-dlp: ${error.message ?: error::class.java.simpleName}", error)
            }
        }

    suspend fun updateNightly(context: Context, force: Boolean): String? = withContext(Dispatchers.IO) {
        ensureInitialized(context)
        val prefs = context.applicationContext.getSharedPreferences("guitarlab_ytdlp_runtime", Context.MODE_PRIVATE)
        val now = System.currentTimeMillis()
        val last = prefs.getLong("lastNightlyUpdateCheck", 0L)
        if (!force && now - last < UPDATE_INTERVAL_MS) return@withContext YoutubeDL.getInstance().versionName(context.applicationContext)
        synchronized(updateLock) {
            val current = prefs.getLong("lastNightlyUpdateCheck", 0L)
            if (force || now - current >= UPDATE_INTERVAL_MS) {
                runCatching { YoutubeDL.getInstance().updateYoutubeDL(context.applicationContext, YoutubeDL.UpdateChannel.NIGHTLY) }
                    .onSuccess { prefs.edit().putLong("lastNightlyUpdateCheck", now).apply() }
            }
        }
        YoutubeDL.getInstance().versionName(context.applicationContext)
    }

    fun cancel(processId: String) { runCatching { YoutubeDL.getInstance().destroyProcessById(processId) } }

    private fun request(target: String, options: List<Pair<String, String?>>): YoutubeDLRequest = YoutubeDLRequest(target).apply {
        addOption("--ignore-config"); addOption("--no-warnings")
        options.forEach { (name, value) -> if (value == null) addOption(name) else addOption(name, value) }
    }

    private const val UPDATE_INTERVAL_MS = 12L * 60L * 60L * 1000L
}
