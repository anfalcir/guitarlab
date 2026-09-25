package studio.guitarlab.core.source

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SourceSearchRulesTest {
    @Test fun normalizationIsAccentAndPunctuationStable() {
        assertEquals("coracao valente", SourceSearchRules.normalize("Coração—Valente!"))
    }

    @Test fun rankingRejectsWrongSongWrongArtistAndDuplicateUrls() {
        val request = SourceSearchRequest("Wolves At The Gate", "Man Of Sorrows")
        val good = SourceCandidateDraft(SourceProvider.YOUTUBE, "Man Of Sorrows (Official Audio)", "Wolves At The Gate - Topic", "https://x/1", durationSeconds = 255.0, officialSignal = true, automaticDownloadSupported = true)
        val duplicate = good.copy(title = "Man Of Sorrows", qualityBonus = 40)
        val wrongSong = good.copy(title = "Dead Man", url = "https://x/2")
        val wrongArtist = good.copy(uploader = "Other Band", url = "https://x/3")
        val ranked = SourceSearchRules.rank(request, listOf(good, duplicate, wrongSong, wrongArtist))
        assertEquals(1, ranked.size)
        assertEquals("https://x/1", ranked.single().url)
    }

    @Test fun previewIsForcedBelowFullTrack() {
        val request = SourceSearchRequest("Skillet", "Hero")
        val preview = SourceCandidateDraft(SourceProvider.OTHER, "Hero", "Skillet", "https://x/p", qualityBonus = 80, durationSeconds = 30.0, previewOnly = true, automaticDownloadSupported = true)
        val full = SourceCandidateDraft(SourceProvider.YOUTUBE, "Hero", "Skillet - Topic", "https://x/f", durationSeconds = 190.0, officialSignal = true, automaticDownloadSupported = true)
        val ranked = SourceSearchRules.rank(request, listOf(preview, full))
        assertEquals("https://x/f", ranked.first().url)
        assertTrue(ranked.last().score <= 5)
    }

    @Test fun durationConsensusPenalizesTruncatedCandidate() {
        val request = SourceSearchRequest("Flyleaf", "All Around Me")
        fun draft(url: String, seconds: Double) = SourceCandidateDraft(SourceProvider.YOUTUBE, "All Around Me", "Flyleaf", url, durationSeconds = seconds, automaticDownloadSupported = true)
        val ranked = SourceSearchRules.rank(request, listOf(draft("https://x/a", 205.0), draft("https://x/b", 203.0), draft("https://x/c", 85.0)))
        val short = ranked.first { it.url.endsWith("c") }
        assertTrue(short.durationWarning)
        assertTrue(short.score < ranked.first { it.url.endsWith("a") }.score)
    }

    @Test fun acquisitionDurationPolicyRejectsPartialAndExtremeMismatch() {
        assertTrue(SourceAcquisitionPolicy.isPlausiblyComplete(200.0, 198.0))
        assertFalse(SourceAcquisitionPolicy.isPlausiblyComplete(200.0, 80.0))
        assertFalse(SourceAcquisitionPolicy.isPlausiblyComplete(200.0, 500.0))
        assertFalse(SourceAcquisitionPolicy.isPlausiblyComplete(0.0, 0.0))
    }

    @Test fun retryClassificationIsBounded() {
        assertTrue(SourceAcquisitionPolicy.retryableMessage("network timeout"))
        assertTrue(SourceAcquisitionPolicy.retryableMessage("HTTP 503"))
        assertFalse(SourceAcquisitionPolicy.retryableMessage("arquivo não contém áudio"))
    }

    @Test fun typoArtistProducesHighConfidenceSuggestion() {
        val request = SourceSearchRequest("Memphys May Fire", "Misery")
        val drafts = listOf(
            SourceCandidateDraft(
                SourceProvider.YOUTUBE,
                "Misery (Official Audio)",
                "Memphis May Fire - Topic",
                "https://x/1",
                automaticDownloadSupported = true,
            ),
            SourceCandidateDraft(
                SourceProvider.BANDCAMP,
                "Misery",
                "Memphis May Fire",
                "https://x/2",
                automaticDownloadSupported = true,
            ),
        )

        val suggestion = SourceSearchRules.suggestArtist(request, drafts)
        assertEquals("Memphis May Fire", suggestion?.artist)
        assertTrue((suggestion?.confidence ?: 0.0) >= 0.82)
        assertEquals(2, suggestion?.supportCount)
    }

    @Test fun exactOrUnrelatedArtistDoesNotProduceSuggestion() {
        val exact = SourceSearchRules.suggestArtist(
            SourceSearchRequest("Memphis May Fire", "Misery"),
            listOf(SourceCandidateDraft(SourceProvider.YOUTUBE, "Misery", "Memphis May Fire - Topic", "https://x/1")),
        )
        val unrelated = SourceSearchRules.suggestArtist(
            SourceSearchRequest("Memphys May Fire", "Misery"),
            listOf(SourceCandidateDraft(SourceProvider.YOUTUBE, "Misery", "Skillet - Topic", "https://x/2")),
        )
        assertEquals(null, exact)
        assertEquals(null, unrelated)
    }
}
