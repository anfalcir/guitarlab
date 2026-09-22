package studio.guitarlab.core.project

import java.io.ByteArrayInputStream
import java.security.MessageDigest
import java.util.UUID
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import studio.guitarlab.core.model.AssetClassification
import studio.guitarlab.core.model.AssetRole
import studio.guitarlab.core.model.ManagedAsset
import studio.guitarlab.core.model.PreparationState
import studio.guitarlab.core.model.PreparationStatus
import studio.guitarlab.core.model.ProjectFactory
import studio.guitarlab.core.model.ProjectTemplate

class StemSetRecoveryTest {
    @get:Rule val temporary = TemporaryFolder()

    @Test fun abandonedFilesFromProcessDeathAreRemovedBeforeAtomicRetry() {
        val root = temporary.newFolder()
        val repository = FileProjectRepository(root)
        val media = ProjectManagedMediaStore(root)
        val sourceBytes = ByteArray(64) { 1 }
        val sourceFile = media.ingest("project", "source.wav", ByteArrayInputStream(sourceBytes))
        val source = ManagedAsset("source", AssetRole.SOURCE_ORIGINAL, sourceFile.relativePath, sha(sourceBytes), sourceBytes.size.toLong(), "wav", 44100, 2, 1, 1, AssetClassification.AUTHORITATIVE)
        repository.save(ProjectFactory(idGenerator = { "project" }).create("Project", ProjectTemplate.BLANK).copy(assets = listOf(source), preparation = PreparationState(PreparationStatus.SOURCE_READY, source.assetId)))
        val jobId = UUID.randomUUID().toString()
        val abandoned = media.ingestStem("project", "$jobId-drums.wav", ByteArrayInputStream(ByteArray(48) { 9 }))
        val stems = StemSetProjectPublisher.ROLES.keys.map { name -> ByteArray(48) { 2 }.let { ValidatedStem(name, it, sha(it), 44100, 2, 1) } }
        val request = StemSetPublicationRequest("project", jobId, source.assetId, source.sha256, "a".repeat(64), "demucs.cpp", "htdemucs_6s", "b".repeat(64), stems)

        assertFalse(StemSetProjectPublisher(repository, media).publish(request))
        assertFalse(abandoned.file.exists())
        assertTrue(repository.load("project")!!.preparation!!.activeStemAssetIds.size == 6)
    }

    private fun sha(bytes: ByteArray) = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
}
