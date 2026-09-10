package studio.guitarlab.app.io

import java.io.File
import kotlin.io.path.createTempDirectory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppCacheTemporaryCleanerTest {
    @Test
    fun `clean removes only direct known temporary files`() {
        val root = createTempDirectory("guitarlab-cache-cleaner-").toFile()
        try {
            val expectedPrefixes = listOf(
                "guitarlab-import-", "guitarlab-aiff-", "guitarlab-resample-",
                "guitarlab-L-", "guitarlab-R-", "guitarlab-project-", "guitarlab-home-master-",
            )
            val eligible = expectedPrefixes.mapIndexed { index, prefix ->
                File(root, "$prefix$index.tmp").apply { writeText("derived") }
            }
            val arbitrary = File(root, "user-audio.wav").apply { writeText("authoritative") }
            val persistentNames = listOf("source.wav", "take.wav", "proxy.wav", "waveform.glwf").map {
                File(root, it).apply { writeText("persistent") }
            }
            val matchingDirectory = File(root, "guitarlab-resample-directory").apply {
                mkdirs()
                File(this, "nested.wav").writeText("keep")
            }

            assertEquals(eligible.size, AppCacheTemporaryCleaner.clean(root))

            eligible.forEach { assertFalse(it.exists()) }
            assertTrue(arbitrary.isFile)
            persistentNames.forEach { assertTrue(it.isFile) }
            assertTrue(matchingDirectory.isDirectory)
            assertTrue(File(matchingDirectory, "nested.wav").isFile)
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun `eligibility rejects files outside cache and matching directories`() {
        val root = createTempDirectory("guitarlab-cache-policy-").toFile()
        val outside = createTempDirectory("guitarlab-cache-outside-").toFile()
        try {
            val outsideMatch = File(outside, "guitarlab-import-audio.wav").apply { writeText("keep") }
            val directoryMatch = File(root, "guitarlab-project-package.guitarlab").apply { mkdirs() }

            assertFalse(AppCacheTemporaryCleaner.isEligible(root, outsideMatch))
            assertFalse(AppCacheTemporaryCleaner.isEligible(root, directoryMatch))
            assertEquals(0, AppCacheTemporaryCleaner.clean(root))
            assertTrue(outsideMatch.isFile)
            assertTrue(directoryMatch.isDirectory)
        } finally {
            root.deleteRecursively()
            outside.deleteRecursively()
        }
    }
}
