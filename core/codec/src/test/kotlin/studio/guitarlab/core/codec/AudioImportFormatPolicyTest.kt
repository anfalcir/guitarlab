package studio.guitarlab.core.codec

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AudioImportFormatPolicyTest {
    @Test
    fun detectsEveryPlannedFormatByExtensionCaseInsensitively() {
        val cases = mapOf(
            "take.WAV" to AudioImportFormat.WAV_PCM,
            "mix.flac" to AudioImportFormat.FLAC,
            "stem.aiff" to AudioImportFormat.AIFF,
            "legacy.AIFC" to AudioImportFormat.AIFF,
            "song.mp3" to AudioImportFormat.MP3,
            "mobile.m4a" to AudioImportFormat.AAC_M4A,
            "raw.aac" to AudioImportFormat.AAC_M4A,
            "loop.ogg" to AudioImportFormat.OGG_VORBIS,
            "voice.opus" to AudioImportFormat.OPUS,
        )
        cases.forEach { (name, expected) ->
            assertEquals(expected, AudioImportFormatPolicy.detect(name, null), name)
        }
    }

    @Test
    fun fallsBackToMimeWhenProviderOmitsExtension() {
        assertEquals(AudioImportFormat.FLAC, AudioImportFormatPolicy.detect("audio", "audio/flac"))
        assertEquals(AudioImportFormat.MP3, AudioImportFormatPolicy.detect(null, "audio/mpeg"))
        assertEquals(AudioImportFormat.AAC_M4A, AudioImportFormatPolicy.detect("sem-extensao", "audio/mp4; codecs=mp4a.40.2"))
    }

    @Test
    fun rejectsUnknownFormatsAndExposesAllPickerFamilies() {
        assertNull(AudioImportFormatPolicy.detect("video.mkv", "video/x-matroska"))
        assertTrue("audio/mpeg" in AudioImportFormatPolicy.pickerMimeTypes)
        assertTrue("audio/ogg" in AudioImportFormatPolicy.pickerMimeTypes)
        assertTrue("audio/opus" in AudioImportFormatPolicy.pickerMimeTypes)
        assertTrue("audio/x-aiff" in AudioImportFormatPolicy.pickerMimeTypes)
    }

    @Test
    fun managedNameAlwaysUsesWavInternalRepresentation() {
        assertEquals("musica.wav", AudioImportFormatPolicy.managedWavName("musica.mp3"))
        assertEquals("audio.wav", AudioImportFormatPolicy.managedWavName("audio"))
    }
}
