package studio.guitarlab.core.project

import studio.guitarlab.core.model.ProjectFactory
import studio.guitarlab.core.model.ProjectTemplate
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class FileProjectRepositoryTest {
    @Test
    fun saveLoadListAndDeleteRoundTrip() {
        val root = Files.createTempDirectory("guitarlab-test").toFile()
        try {
            val repository = FileProjectRepository(root)
            var nextId = 0
            val project = ProjectFactory(idGenerator = { "id-${nextId++}" }, clock = { 100L })
                .create("Tone Test", ProjectTemplate.GUITAR)

            repository.save(project)
            assertEquals(1, repository.list().size)
            assertNotNull(repository.load(project.id))
            assertTrue(repository.delete(project.id))
            assertTrue(repository.list().isEmpty())
        } finally {
            root.deleteRecursively()
        }
    }
}
