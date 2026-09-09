package studio.guitarlab.core.project

import java.io.ByteArrayOutputStream
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import studio.guitarlab.core.model.AudioClip
import studio.guitarlab.core.model.AudioTrack
import studio.guitarlab.core.model.GuitarProject
import studio.guitarlab.core.model.ProjectTemplate

class ProjectBundleReaderTest {
    @Test fun writerAndReaderRestoreIndependentProjectWithMedia() {
        val root = createTempDirectory("guitarlab-restore-").toFile()
        val sourceRoot = createTempDirectory("guitarlab-source-").toFile()
        try {
            File(sourceRoot, "media/source").mkdirs()
            File(sourceRoot, "media/proxy").mkdirs()
            File(sourceRoot, "media/source/original.mp3").writeBytes(byteArrayOf(1, 2, 3, 4))
            File(sourceRoot, "media/proxy/edit.wav").writeBytes(ByteArray(48) { it.toByte() })
            val project = GuitarProject(
                id = "original-id",
                name = "Projeto portátil",
                template = ProjectTemplate.BLANK,
                createdAtEpochMs = 1,
                updatedAtEpochMs = 2,
                tracks = listOf(AudioTrack(id = "t1", name = "Pista", order = 0)),
                clips = listOf(
                    AudioClip(
                        id = "c1",
                        trackId = "t1",
                        name = "original.mp3",
                        sourceUri = "managed://media/source/original.mp3",
                        startFrame = 0,
                        lengthFrames = 10,
                        managedSourcePath = "media/source/original.mp3",
                        managedEditProxyPath = "media/proxy/edit.wav",
                    )
                ),
            )
            val packageBytes = ByteArrayOutputStream().also {
                ProjectBundleWriter().write(project, sourceRoot, it)
            }.toByteArray()

            val restored = ProjectBundleReader(root).read(packageBytes.inputStream(), nowEpochMs = 99)

            assertNotEquals(project.id, restored.id)
            assertEquals(project.name, restored.name)
            assertEquals(99, restored.createdAtEpochMs)
            assertEquals(99, restored.updatedAtEpochMs)
            val restoredDirectory = File(File(root, "projects"), restored.id)
            assertTrue(File(restoredDirectory, "media/source/original.mp3").isFile)
            assertTrue(File(restoredDirectory, "media/proxy/edit.wav").isFile)
            assertEquals(restored, FileProjectRepository(root).load(restored.id))
        } finally {
            root.deleteRecursively()
            sourceRoot.deleteRecursively()
        }
    }

    @Test fun pathTraversalPackageIsRejectedWithoutPublishingProject() {
        val root = createTempDirectory("guitarlab-traversal-").toFile()
        try {
            val bytes = ByteArrayOutputStream().also { raw ->
                ZipOutputStream(raw).use { zip ->
                    zip.putNextEntry(ZipEntry("../escape.txt"))
                    zip.write(byteArrayOf(1))
                    zip.closeEntry()
                }
            }.toByteArray()

            assertFailsWith<IllegalArgumentException> {
                ProjectBundleReader(root).read(bytes.inputStream())
            }
            assertTrue(File(root, "projects").listFiles().orEmpty().none { !it.name.startsWith(".import-") })
            assertTrue(!File(root, "escape.txt").exists())
        } finally {
            root.deleteRecursively()
        }
    }

    @Test fun wrongManifestVersionIsRejected() {
        val root = createTempDirectory("guitarlab-manifest-").toFile()
        try {
            val bytes = ByteArrayOutputStream().also { raw ->
                ZipOutputStream(raw).use { zip ->
                    zip.putNextEntry(ZipEntry("manifest.properties"))
                    zip.write("format=guitarlab-project\nbundleVersion=999\nprojectId=p1\n".toByteArray())
                    zip.closeEntry()
                    zip.putNextEntry(ZipEntry("project.json"))
                    zip.write("{}".toByteArray())
                    zip.closeEntry()
                }
            }.toByteArray()
            assertFailsWith<IllegalArgumentException> { ProjectBundleReader(root).read(bytes.inputStream()) }
        } finally {
            root.deleteRecursively()
        }
    }
}
