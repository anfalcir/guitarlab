package studio.guitarlab.platform.source.android

import android.content.Context
import java.net.HttpURLConnection
import java.net.URI
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import org.json.JSONObject
import studio.guitarlab.core.source.RankedSourceCandidate
import studio.guitarlab.core.source.SourceCandidateDraft
import studio.guitarlab.core.source.SourceProvider
import studio.guitarlab.core.source.SourceSearchDepth
import studio.guitarlab.core.source.SourceSearchProviderClient
import studio.guitarlab.core.source.SourceSearchRequest
import studio.guitarlab.core.source.SourceSearchRules

data class SourceDiscoveryResult(
    val candidates: List<RankedSourceCandidate>,
    val warnings: List<String> = emptyList(),
    val suggestedArtist: String? = null,
    val providersSucceeded: Int = 0,
    val providersFailed: Int = 0,
)

class SourceSearchCoordinator(private val providers: List<SourceSearchProviderClient>) {
    constructor(context: Context) : this(listOf(BandcampDiscoveryProvider(), YtDlpDiscoveryProvider(context.applicationContext)))

    suspend fun search(request: SourceSearchRequest): SourceDiscoveryResult {
        require(request.song.isNotBlank()) { "Informe o nome da música." }
        val outcomes = coroutineScope {
            providers.map { provider ->
                async {
                    try {
                        ProviderOutcome(provider.search(request).filter { it.automaticDownloadSupported && !it.previewOnly })
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (error: Exception) {
                        ProviderOutcome(warning = error.message ?: "Uma fonte de pesquisa ficou indisponível.")
                    }
                }
            }.awaitAll()
        }
        val drafts = outcomes.flatMap { it.drafts }
        val ranked = SourceSearchRules.rank(request, drafts)
        val succeeded = outcomes.count { it.warning == null }
        val failed = outcomes.size - succeeded
        return SourceDiscoveryResult(
            candidates = ranked,
            warnings = outcomes.mapNotNull { it.warning },
            suggestedArtist = if (ranked.isEmpty() && succeeded > 0) {
                SourceSearchRules.suggestArtist(request, drafts)?.artist
            } else {
                null
            },
            providersSucceeded = succeeded,
            providersFailed = failed,
        )
    }
}

private data class ProviderOutcome(val drafts: List<SourceCandidateDraft> = emptyList(), val warning: String? = null)

internal class BandcampDiscoveryProvider(private val fetcher: suspend (String) -> String = ::fetchBandcamp) : SourceSearchProviderClient {
    override suspend fun search(request: SourceSearchRequest): List<SourceCandidateDraft> {
        val query = listOf(request.artist.trim(), request.song.trim()).filter { it.isNotBlank() }.joinToString(" ")
        if (query.isBlank()) return emptyList()
        val html = fetcher("https://bandcamp.com/search?q=" + URLEncoder.encode(query, StandardCharsets.UTF_8.name()))
        val limit = if (request.depth == SourceSearchDepth.MAXIMUM) 15 else 8
        return BandcampParser.parse(html).take(limit)
    }
}

internal object BandcampParser {
    private val block = Regex("""(?is)<li[^>]*class=[\"'][^\"']*searchresult[^\"']*[\"'][^>]*>(.*?)</li>""")
    private val urlRegex = Regex("""https?://[a-z0-9._-]+\.bandcamp\.com/track/[a-z0-9._~%+\-/]+(?:\?[^\"'<>\s]*)?""", RegexOption.IGNORE_CASE)
    private val href = Regex("""(?is)href=[\"'](https?://[^\"']+\.bandcamp\.com/track/[^\"']+)[\"']""")
    private val heading = Regex("""(?is)class=[\"'][^\"']*heading[^\"']*[\"'][^>]*>.*?<a[^>]*>(.*?)</a>""")
    private val subhead = Regex("""(?is)class=[\"'][^\"']*subhead[^\"']*[\"'][^>]*>(.*?)</(?:div|span)>""")

    fun parse(html: String): List<SourceCandidateDraft> {
        if (html.isBlank()) return emptyList()
        val cards = block.findAll(html).mapNotNull { parseBlock(it.groupValues[1]) }.distinctBy { it.url }.toList()
        if (cards.isNotEmpty()) return cards
        return urlRegex.findAll(decode(html)).map { draft(clean(it.value)) }.distinctBy { it.url }.toList()
    }

    private fun parseBlock(value: String): SourceCandidateDraft? {
        val decoded = decode(value)
        val url = href.find(decoded)?.groupValues?.getOrNull(1) ?: urlRegex.find(decoded)?.value ?: return null
        val clean = clean(url)
        val title = heading.find(decoded)?.groupValues?.getOrNull(1)?.let(::text)?.takeIf { it.isNotBlank() } ?: titleFromUrl(clean)
        val uploader = subhead.find(decoded)?.groupValues?.getOrNull(1)?.let(::text)?.removePrefix("by ")?.trim()?.takeIf { it.isNotBlank() } ?: artistFromUrl(clean)
        return draft(clean, title, uploader)
    }

    private fun draft(url: String, title: String = titleFromUrl(url), uploader: String = artistFromUrl(url)) = SourceCandidateDraft(
        SourceProvider.BANDCAMP, title, uploader, url, "Fonte direta no Bandcamp", qualityBonus = 30, automaticDownloadSupported = true,
    )
    private fun titleFromUrl(url: String) = runCatching { URI(url).path.substringAfter("/track/").substringBefore('/').replace('-', ' ').trim() }.getOrDefault("").ifBlank { "Faixa no Bandcamp" }
    private fun artistFromUrl(url: String) = runCatching { URI(url).host.orEmpty().substringBefore(".bandcamp.com").replace('-', ' ').trim() }.getOrDefault("").ifBlank { "Bandcamp" }
    private fun clean(url: String) = url.substringBefore('#').trimEnd('.', ',', ')')
    private fun text(value: String) = decode(value.replace(Regex("(?is)<[^>]+>"), " ")).replace(Regex("\\s+"), " ").trim()
    private fun decode(value: String) = value.replace("&amp;", "&").replace("&quot;", "\"").replace("&#39;", "'").replace("&#x27;", "'").replace("&lt;", "<").replace("&gt;", ">").replace("&nbsp;", " ")
}

private suspend fun fetchBandcamp(url: String): String = withContext(Dispatchers.IO) {
    val connection = URI(url).toURL().openConnection() as HttpURLConnection
    try {
        connection.connectTimeout = 7_000; connection.readTimeout = 10_000; connection.instanceFollowRedirects = true
        connection.requestMethod = "GET"; connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Android) GuitarLab/0.5")
        val status = connection.responseCode
        require(status in 200..299) { "Bandcamp indisponível (HTTP $status)." }
        connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
    } finally { connection.disconnect() }
}

internal interface YtDlpDiscoveryBackend {
    suspend fun updateRuntime(force: Boolean)
    suspend fun flatSearch(target: String, provider: SourceProvider): List<FlatCandidate>
    suspend fun inspectUrl(url: String, provider: SourceProvider): SourceCandidateDraft
}

private class AndroidYtDlpDiscoveryBackend(private val context: Context) : YtDlpDiscoveryBackend {
    override suspend fun updateRuntime(force: Boolean) {
        YtDlpRuntime.updateNightly(context, force)
    }

    override suspend fun inspectUrl(url: String, provider: SourceProvider): SourceCandidateDraft {
        val json = YtDlpRuntime.executeJson(
            context,
            url,
            listOf("--skip-download" to null, "--no-playlist" to null, "--dump-single-json" to null),
            "inspect-${UUID.randomUUID()}",
        )
        return YtDlpInfoParser.toDraft(JSONObject(json), provider, url)
    }

    override suspend fun flatSearch(target: String, provider: SourceProvider): List<FlatCandidate> {
        val json = YtDlpRuntime.executeJson(
            context,
            target,
            listOf("--skip-download" to null, "--flat-playlist" to null, "--dump-single-json" to null),
            "search-${UUID.randomUUID()}",
        )
        val entries = JSONObject(json).optJSONArray("entries") ?: return emptyList()
        return buildList {
            for (i in 0 until entries.length()) {
                val e = entries.optJSONObject(i) ?: continue
                val title = e.optString("title").ifBlank { e.optString("track") }
                val uploader = sequenceOf(e.optString("artist"), e.optString("uploader"), e.optString("channel")).firstOrNull { it.isNotBlank() }.orEmpty()
                var url = sequenceOf(e.optString("webpage_url"), e.optString("original_url"), e.optString("url")).firstOrNull { it.startsWith("http") }.orEmpty()
                if (url.isBlank() && provider == SourceProvider.YOUTUBE) {
                    e.optString("id").takeIf { it.isNotBlank() }?.let { url = "https://www.youtube.com/watch?v=$it" }
                }
                if (url.isNotBlank()) add(FlatCandidate(provider, title, uploader, url))
            }
        }
    }
}

internal class YtDlpDiscoveryProvider internal constructor(
    private val backend: YtDlpDiscoveryBackend,
) : SourceSearchProviderClient {
    constructor(context: Context) : this(AndroidYtDlpDiscoveryBackend(context.applicationContext))

    override suspend fun search(request: SourceSearchRequest): List<SourceCandidateDraft> {
        refreshRuntime(force = false)
        return searchInternal(request, allowRecovery = true)
    }

    suspend fun inspectUrl(url: String, provider: SourceProvider): SourceCandidateDraft =
        backend.inspectUrl(url, provider)

    private suspend fun searchInternal(request: SourceSearchRequest, allowRecovery: Boolean): List<SourceCandidateDraft> {
        val query = listOf(request.artist.trim(), request.song.trim()).filter { it.isNotBlank() }.joinToString(" ")
        val limit = if (request.depth == SourceSearchDepth.MAXIMUM) 9 else 6
        val searches = listOf(
            SourceProvider.SOUNDCLOUD to "scsearch$limit:$query",
            SourceProvider.YOUTUBE to "ytsearch$limit:$query official audio",
            SourceProvider.YOUTUBE to "ytsearch$limit:$query Topic",
        )

        val searchAttempts = coroutineScope {
            searches.map { (provider, target) ->
                async { captureYtDlpAttempt { backend.flatSearch(target, provider) } }
            }.awaitAll()
        }

        if (searchAttempts.all { it.error != null }) {
            if (allowRecovery && refreshRuntime(force = true)) {
                return searchInternal(request, allowRecovery = false)
            }
            throw IllegalStateException(
                "YouTube/SoundCloud indisponíveis durante a pesquisa: " +
                    summarizeYtDlpFailures(searchAttempts.mapNotNull { it.error }),
            )
        }

        val flat = searchAttempts
            .mapNotNull { it.value }
            .flatten()
            .distinctBy { it.url }

        val planned = flat
            .filter { SourceSearchRules.titleMatchesSong(request.song, it.title) }
            .sortedByDescending { SourceSearchRules.textSimilarity(request.song, it.title) }
            .take(limit)

        if (planned.isEmpty()) return emptyList()

        val semaphore = Semaphore(3)
        val inspectionAttempts = coroutineScope {
            planned.map { item ->
                async {
                    semaphore.withPermit {
                        captureYtDlpAttempt { backend.inspectUrl(item.url, item.provider) }
                    }
                }
            }.awaitAll()
        }

        val inspected = inspectionAttempts.mapNotNull { it.value }
        if (inspected.isNotEmpty()) return inspected

        if (inspectionAttempts.any { it.error != null }) {
            if (allowRecovery && refreshRuntime(force = true)) {
                return searchInternal(request, allowRecovery = false)
            }
            throw IllegalStateException(
                "As fontes foram localizadas, mas não puderam ser validadas: " +
                    summarizeYtDlpFailures(inspectionAttempts.mapNotNull { it.error }),
            )
        }
        return emptyList()
    }

    private suspend fun refreshRuntime(force: Boolean): Boolean =
        try {
            backend.updateRuntime(force)
            true
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Throwable) {
            false
        }
}

internal data class FlatCandidate(
    val provider: SourceProvider,
    val title: String,
    val uploader: String,
    val url: String,
)

private data class YtDlpAttempt<T>(val value: T? = null, val error: Throwable? = null)

private suspend fun <T> captureYtDlpAttempt(block: suspend () -> T): YtDlpAttempt<T> =
    try {
        YtDlpAttempt(value = block())
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (error: Throwable) {
        YtDlpAttempt(error = error)
    }

private fun summarizeYtDlpFailures(errors: List<Throwable>): String =
    errors
        .mapNotNull { it.message?.trim()?.takeIf(String::isNotBlank) }
        .distinct()
        .take(2)
        .joinToString(" | ")
        .ifBlank { "falha no motor de pesquisa" }

internal object YtDlpInfoParser {
    private val losslessExts = setOf("flac", "wav", "alac", "ape", "aiff", "aif")
    fun toDraft(info: JSONObject, provider: SourceProvider, fallbackUrl: String): SourceCandidateDraft {
        val title = info.optString("track").ifBlank { info.optString("title") }
        val uploader = info.optString("artist").ifBlank { info.optString("uploader") }.ifBlank { info.optString("channel") }
        val duration = info.optDouble("duration", 0.0).takeIf { it.isFinite() } ?: 0.0
        val url = sequenceOf(info.optString("webpage_url"), info.optString("original_url"), fallbackUrl).firstOrNull { it.startsWith("http") }.orEmpty()
        val formats = info.optJSONArray("formats")
        val audio = buildList {
            if (formats != null) for (i in 0 until formats.length()) {
                val f = formats.optJSONObject(i) ?: continue
                if (f.optString("vcodec", "none") in setOf("", "none") && f.optString("acodec", "none") != "none") add(f)
            }
        }
        val nonPreview = audio.filterNot(::isPreview)
        val previewOnly = audio.isNotEmpty() && nonPreview.isEmpty()
        val usable = if (nonPreview.isNotEmpty()) nonPreview else audio
        val lossless = usable.filter { it.optString("ext").lowercase() in losslessExts || it.optString("acodec").lowercase().startsWith("pcm") }
        val selected = (lossless.ifEmpty { usable }).maxByOrNull { maxOf(it.optDouble("abr", 0.0), it.optDouble("tbr", 0.0)) }
        val formatId = selected?.optString("format_id").orEmpty().ifBlank { "bestaudio/best" }
        val ext = selected?.optString("ext").orEmpty()
        val qualityBonus = if (lossless.isNotEmpty()) 40 else 0
        val quality = if (previewOnly) "PREVIEW/TRUNCADO" else if (lossless.isNotEmpty()) "LOSSLESS ${ext.uppercase()}" else "stream ${ext.uppercase()}".trim()
        val normalizedTitle = SourceSearchRules.normalize(title); val normalizedUploader = SourceSearchRules.normalize(uploader)
        val official = provider == SourceProvider.YOUTUBE && ("topic" in normalizedUploader || "official audio" in normalizedTitle || "official music" in normalizedTitle)
        return SourceCandidateDraft(provider, title, uploader, url, quality, formatId, qualityBonus, duration, previewOnly, official, true)
    }
    private fun isPreview(f: JSONObject): Boolean = listOf("format_id", "format", "format_note", "url").joinToString(" ") { f.optString(it) }.lowercase().let { blob -> listOf("preview", "sample", "excerpt", "snippet").any { it in blob } }
}
