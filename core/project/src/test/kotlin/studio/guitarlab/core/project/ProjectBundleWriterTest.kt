package studio.guitarlab.core.project

import java.io.ByteArrayOutputStream
import java.io.File
import java.util.zip.ZipInputStream
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import studio.guitarlab.core.model.AudioClip
import studio.guitarlab.core.model.AudioTrack
import studio.guitarlab.core.model.GuitarProject
import studio.guitarlab.core.model.ProjectTemplate

class ProjectBundleWriterTest {
    @Test fun packageContainsProjectOriginalAndProxyExactlyOnce() {
        val root = createTempDirectory("guitarlab-bundle-").toFile()
        try {
            File(root, "media/source").mkdirs()
            File(root, "media/proxy").mkdirs()
            File(root, "media/source/original.mp3").writeBytes(byteArrayOf(1, 2, 3))
            File(root, "media/proxy/edit.wav").writeBytes(byteArrayOf(4, 5, 6))
            val project = GuitarProject(
                id = "p1", name = "Teste", template = ProjectTemplate.BLANK,
                createdAtEpochMs = 1, updatedAtEpochMs = 2,
                tracks = listOf(AudioTrack(id = "t1", name = "Pista", order = 0)),
                clips = listOf(AudioClip(
                    id = "c1", trackId = "t1", name = "original.mp3", sourceUri = "managed://media/source/original.mp3",
                    startFrame = 0, lengthFrames = 10,
                    managedSourcePath = "media/source/original.mp3", managedEditProxyPath = "media/proxy/edit.wav",
                )),
            )
            val bytes = ByteArrayOutputStream().also { ProjectBundleWriter().write(project, root, it) }.toByteArray()
            val names = mutableListOf<String>()
            ZipInputStream(bytes.inputStream()).use { zip ->
                while (true) {
                    val entry = zip.nextEntry ?: break
                    names += entry.name
                }
            }
            assertEquals(names.distinct(), names)
            assertTrue("project.json" in names)
            assertTrue("manifest.properties" in names)
            assertTrue("media/source/original.mp3" in names)
            assertTrue("media/proxy/edit.wav" in names)
        } finally { root.deleteRecursively() }
    }
}
