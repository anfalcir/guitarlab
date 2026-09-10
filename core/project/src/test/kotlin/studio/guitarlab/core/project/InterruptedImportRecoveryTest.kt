package studio.guitarlab.core.project

import java.io.File
import java.nio.file.Files
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import studio.guitarlab.core.model.ProjectFactory
import studio.guitarlab.core.model.ProjectTemplate

class InterruptedImportRecoveryTest {
    @Test
    fun repositoryDoesNotExposeInterruptedImportAsGhostProject() {
        val root = Files.createTempDirectory("guitarlab-staging-list").toFile()
        try {
            val repository = FileProjectRepository(root)
            val live = ProjectFactory(idGenerator = { "live" }, clock = { 1L }).create("Live", ProjectTemplate.BLANK)
            repository.save(live)
            val staged = ProjectFactory(idGenerator = { "ghost" }, clock = { 2L }).create("Ghost", ProjectTemplate.BLANK)
            val stagingDirectory = File(root, "projects/.import-dead-process").apply { mkdirs() }
            File(stagingDirectory, "project.json").writeText(ProjectCodec().encode(staged))

            assertEquals(listOf("live"), repository.list().map { it.id })
            assertTrue(stagingDirectory.isDirectory)
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun cleanupRemovesOnlyBundleImportStagingDirectories() {
        val root = createTempDirectory("guitarlab-interrupted-import-").toFile()
        try {
            val projects = File(root, "projects").apply { mkdirs() }
            val interrupted = File(projects, ".import-dead-process").apply { mkdirs() }
            File(interrupted, "project.json").writeText("partial")
            val realProject = File(projects, "real-project").apply { mkdirs() }
            File(realProject, "keep.txt").writeText("keep")
            val unrelatedHidden = File(projects, ".keep-private").apply { mkdirs() }

            assertEquals(1, ProjectBundleReader(root).cleanupInterruptedImports())
            assertTrue(!interrupted.exists())
            assertTrue(realProject.isDirectory)
            assertTrue(unrelatedHidden.isDirectory)
        } finally {
            root.deleteRecursively()
        }
    }
}
