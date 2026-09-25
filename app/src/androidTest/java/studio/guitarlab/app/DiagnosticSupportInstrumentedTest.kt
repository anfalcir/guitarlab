package studio.guitarlab.app

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.security.MessageDigest
import java.util.zip.ZipInputStream
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import studio.guitarlab.app.diagnostics.DiagnosticBundleExporter
import studio.guitarlab.app.diagnostics.DiagnosticJournal

@RunWith(AndroidJUnit4::class)
class DiagnosticSupportInstrumentedTest {
    private val context get() = ApplicationProvider.getApplicationContext<android.content.Context>()

    @Test
    fun journalRedactsSecretsDropsExpiredAndToleratesMalformedLine() {
        val root = File(context.filesDir, "diagnostics").apply { mkdirs() }
        val events = File(root, "events.jsonl")
        events.delete()
        val journal = DiagnosticJournal(context)

        journal.append(
            eventType = "test.expired",
            summary = "old",
            timestampEpochMs = System.currentTimeMillis() - DiagnosticJournal.RETENTION_MS - 1_000,
        )
        journal.append(
            eventType = "test.current",
            summary = "Authorization=Bearer abcdef123 token=secret password=hunter2",
        )
        events.appendText("{malformed\n")

        val parsed = journal.readEvents()
        val raw = journal.rawSanitizedJsonl()
        assertTrue(parsed.any { it.eventType == "test.current" })
        assertFalse(parsed.any { it.eventType == "test.expired" })
        assertFalse(raw.contains("abcdef123"))
        assertFalse(raw.contains("hunter2"))
        assertFalse(raw.contains("{malformed"))
        assertTrue(raw.contains("[REDACTED]"))
    }

    @Test
    fun diagnosticZipIsDeterministicInShapeChecksummedSanitizedAndContainsNoAudio() {
        val manifestDir = File(context.filesDir, "diagnostics/manifests").apply { mkdirs() }
        val manifest = File(manifestDir, "test-diagnostic-manifest.json")
        manifest.writeText(
            """{"jobId":"test-diagnostic-manifest","model":"htdemucs_6s","authorization":"Bearer super-secret","result":"ok"}""",
        )

        val output = ByteArrayOutputStream()
        val result = DiagnosticBundleExporter(context).export(null, output)
        val entries = unzip(output.toByteArray())

        listOf(
            "README.txt",
            "app.json",
            "device.json",
            "events.jsonl",
            "audio-route.json",
            "activity.json",
            "separation/jobs.json",
            "backup/status.json",
            "integrity/SHA256SUMS.txt",
            "separation/manifests/test-diagnostic-manifest.json",
        ).forEach { required -> assertTrue("missing $required", required in entries) }

        assertFalse(entries.keys.any { it.endsWith(".wav") || it.endsWith(".mp3") || it.endsWith(".flac") })
        assertFalse(entries.values.any { it.toString(Charsets.UTF_8).contains("super-secret") })
        assertTrue(entries["separation/manifests/test-diagnostic-manifest.json"]!!.toString(Charsets.UTF_8).contains("htdemucs_6s"))

        val sums = entries["integrity/SHA256SUMS.txt"]!!.toString(Charsets.UTF_8)
        result.sha256ByEntry.forEach { (name, expected) ->
            val actual = sha(entries.getValue(name))
            assertTrue("$name checksum mismatch", sums.contains("$expected  $name"))
            assertTrue("$name exporter result mismatch", actual == expected)
        }
        manifest.delete()
    }

    private fun unzip(bytes: ByteArray): Map<String, ByteArray> {
        val entries = linkedMapOf<String, ByteArray>()
        ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                entries[entry.name] = zip.readBytes()
                zip.closeEntry()
            }
        }
        return entries
    }

    private fun sha(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
}
