package studio.guitarlab.core.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class TrackNamePolicyTest {
    @Test
    fun acceptsUpToTwentyFourUnicodeCodePoints() {
        val valid = "Guitarra Referência L 01"
        assertEquals(24, TrackNamePolicy.characterCount(valid))
        assertTrue(TrackNamePolicy.isValid(valid))
        assertEquals(valid, TrackNamePolicy.requireValid(valid))
    }

    @Test
    fun normalizesWhitespaceWithoutSilentlyTruncating() {
        assertEquals("Minha Guitarra Lead", TrackNamePolicy.normalize("  Minha\nGuitarra\tLead  "))
    }

    @Test
    fun typingBeyondLimitIsConstrainedAndPersistedValidationRejectsOversize() {
        val oversized = "1234567890123456789012345"
        assertEquals("123456789012345678901234", TrackNamePolicy.constrainDraft(oversized))
        assertFalse(TrackNamePolicy.isValid(oversized))
        assertFailsWith<IllegalArgumentException> { TrackNamePolicy.requireValid(oversized) }
    }

    @Test
    fun accentedCharactersCountAsCharactersNotUtf16StorageUnits() {
        val value = "Áudio Guitarra São João"
        assertEquals(value.codePointCount(0, value.length), TrackNamePolicy.characterCount(value))
    }
}
