package studio.guitarlab.platform.source.android

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import studio.guitarlab.core.source.SourceCandidateDraft
import studio.guitarlab.core.source.SourceProvider
import studio.guitarlab.core.source.SourceSearchRequest

class SourceDiscoveryTest {
    @Test
    fun ytDlpSearchRetriesAfterForcedRuntimeRefresh() = runBlocking {
        val backend = RecoveringBackend()
        val results = YtDlpDiscoveryProvider(backend).search(
            SourceSearchRequest(artist = "Wolves At The Gate", song = "Deadbolt"),
        )

        assertEquals(1, backend.forcedUpdates)
        assertEquals(1, results.size)
        assertEquals("Deadbolt", results.single().title)
        assertEquals(SourceProvider.YOUTUBE, results.single().provider)
    }

    @Test
    fun ytDlpSearchSurfacesPersistentRuntimeFailure() = runBlocking {
        val backend = AlwaysFailingBackend()
        val failure = runCatching {
            YtDlpDiscoveryProvider(backend).search(
                SourceSearchRequest(artist = "Wolves At The Gate", song = "Deadbolt"),
            )
        }.exceptionOrNull()

        assertEquals(1, backend.forcedUpdates)
        assertTrue(failure is IllegalStateException)
        assertTrue(failure?.message.orEmpty().contains("indisponíveis"))
    }

    private class RecoveringBackend : YtDlpDiscoveryBackend {
        var forcedUpdates = 0
        private var recovered = false

        override suspend fun updateRuntime(force: Boolean) {
            if (force) {
                forcedUpdates += 1
                recovered = true
            }
        }

        override suspend fun flatSearch(target: String, provider: SourceProvider): List<FlatCandidate> {
            if (!recovered) error("runtime unavailable")
            return if (provider == SourceProvider.YOUTUBE) {
                listOf(
                    FlatCandidate(
                        provider = SourceProvider.YOUTUBE,
                        title = "Deadbolt",
                        uploader = "Wolves At The Gate",
                        url = "https://www.youtube.com/watch?v=deadbolt",
                    ),
                )
            } else {
                emptyList()
            }
        }

        override suspend fun inspectUrl(url: String, provider: SourceProvider): SourceCandidateDraft =
            SourceCandidateDraft(
                provider = provider,
                title = "Deadbolt",
                uploader = "Wolves At The Gate",
                url = url,
                quality = "stream M4A",
                formatId = "140",
                durationSeconds = 179.0,
                automaticDownloadSupported = true,
            )
    }

    private class AlwaysFailingBackend : YtDlpDiscoveryBackend {
        var forcedUpdates = 0

        override suspend fun updateRuntime(force: Boolean) {
            if (force) forcedUpdates += 1
        }

        override suspend fun flatSearch(target: String, provider: SourceProvider): List<FlatCandidate> =
            error("runtime unavailable")

        override suspend fun inspectUrl(url: String, provider: SourceProvider): SourceCandidateDraft =
            error("runtime unavailable")
    }

    @Test
    fun coordinatorDistinguishesHealthyEmptyFromProviderFailureAndSuggestion() = runBlocking {
        val healthyNearMatch = object : studio.guitarlab.core.source.SourceSearchProviderClient {
            override suspend fun search(request: SourceSearchRequest) = listOf(
                SourceCandidateDraft(
                    provider = SourceProvider.YOUTUBE,
                    title = "Misery",
                    uploader = "Memphis May Fire - Topic",
                    url = "https://x/near",
                    automaticDownloadSupported = true,
                ),
            )
        }
        val failing = object : studio.guitarlab.core.source.SourceSearchProviderClient {
            override suspend fun search(request: SourceSearchRequest): List<SourceCandidateDraft> =
                error("provider offline")
        }

        val mixed = SourceSearchCoordinator(listOf(healthyNearMatch, failing)).search(
            SourceSearchRequest(artist = "Memphys May Fire", song = "Misery"),
        )
        assertEquals(1, mixed.providersSucceeded)
        assertEquals(1, mixed.providersFailed)
        assertEquals("Memphis May Fire", mixed.suggestedArtist)

        val allFailed = SourceSearchCoordinator(listOf(failing)).search(
            SourceSearchRequest(artist = "Memphys May Fire", song = "Misery"),
        )
        assertEquals(0, allFailed.providersSucceeded)
        assertEquals(1, allFailed.providersFailed)
        assertEquals(null, allFailed.suggestedArtist)
        assertTrue(allFailed.warnings.isNotEmpty())
    }

}
