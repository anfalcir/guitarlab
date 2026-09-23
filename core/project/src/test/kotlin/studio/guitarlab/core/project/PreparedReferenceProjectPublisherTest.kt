package studio.guitarlab.core.project

import java.io.ByteArrayInputStream
import java.io.File
import java.security.MessageDigest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import studio.guitarlab.core.codec.FloatWavFileWriter
import studio.guitarlab.core.model.AssetClassification
import studio.guitarlab.core.model.AssetRole
import studio.guitarlab.core.model.ManagedAsset
import studio.guitarlab.core.model.PreparationState
import studio.guitarlab.core.model.PreparationStatus
import studio.guitarlab.core.model.ProjectFactory
import studio.guitarlab.core.model.ProjectTemplate

class PreparedReferenceProjectPublisherTest {
    @get:Rule val temp = TemporaryFolder()

    @Test fun v2PublishesBackingGuitarAndLocalSplitAtomicallyWithoutStemAssets() {
        val root = temp.newFolder()
        val repo = FileProjectRepository(root)
        val media = ProjectManagedMediaStore(root) { "media-${System.nanoTime()}" }
        val sourceBytes = ByteArray(64) { 7 }
        val sourceManaged = media.ingest("p", "source.bin", ByteArrayInputStream(sourceBytes))
        val source = ManagedAsset(
            assetId = "source",
            role = AssetRole.SOURCE_ORIGINAL,
            relativePath = sourceManaged.relativePath,
            sha256 = sha(sourceBytes),
            byteSize = sourceBytes.size.toLong(),
            format = "bin",
            sampleRateHz = null,
            channelCount = null,
            frameCount = null,
            createdAtEpochMs = 1,
            classification = AssetClassification.AUTHORITATIVE,
        )
        var templateId = 0
        repo.save(
            ProjectFactory(
                idGenerator = {
                    templateId += 1
                    if (templateId == 1) "p" else "template-$templateId"
                },
            ).create("P", ProjectTemplate.GUITAR).copy(
                assets = listOf(source),
                preparation = PreparationState(
                    status = PreparationStatus.SOURCE_READY,
                    sourceAssetId = source.assetId,
                ),
            ),
        )

        val backingFile = File(root, "backing.wav").also { writeStereo(it, 0.2f, -0.2f) }
        val guitarFile = File(root, "guitar.wav").also { writeStereo(it, 0.4f, -0.4f) }
        val backing = reference("backing", AssetRole.REFERENCE_BACKING, backingFile)
        val guitar = reference("guitar", AssetRole.REFERENCE_GUITAR, guitarFile)

        val publisher = PreparedReferenceProjectPublisher(
            repository = repo,
            mediaStore = media,
            tempDirectory = temp.newFolder(),
            nowMs = { 2 },
            idFactory = sequenceId(),
        )
        val request = PreparedReferencePublicationRequest(
            projectId = "p",
            jobId = "job-v2",
            sourceAssetId = source.assetId,
            sourceSha256 = source.sha256,
            manifestSha256 = "a".repeat(64),
            engine = "demucs.cpp",
            model = "htdemucs_6s",
            modelSha256 = "b".repeat(64),
            recipeVersion = "prepared-reference-v2",
            targetPeakDbfs = -1.0,
            sharedGainDb = -0.5,
            references = listOf(backing, guitar),
        )

        assertFalse(publisher.publish(request))
        val saved = repo.load("p")!!
        val prep = saved.preparation!!
        assertEquals(PreparationStatus.READY, prep.status)
        assertTrue(prep.activeStemAssetIds.isEmpty())
        assertTrue(prep.activeBackingAssetId != null)
        assertTrue(prep.activeGuitarAssetId != null)
        val jobAssets = saved.assets.filter { it.provenance?.parameters?.get("jobId") == "job-v2" }
        assertEquals(4, jobAssets.size)
        assertEquals(1, jobAssets.count { it.role == AssetRole.REFERENCE_BACKING })
        assertEquals(3, jobAssets.count { it.role == AssetRole.REFERENCE_GUITAR })
        assertEquals(2, jobAssets.count { it.provenance?.kind == "GUITAR_CHANNEL_SPLIT" })
        assertTrue(publisher.publish(request))
    }

    private fun reference(name: String, role: AssetRole, file: File) =
        ValidatedPreparedReference(
            name = name,
            role = role,
            byteCount = file.length(),
            sha256 = sha(file.readBytes()),
            sampleRate = 44_100,
            channels = 2,
            frames = 128,
            openStream = file::inputStream,
        )

    private fun writeStereo(file: File, leftValue: Float, rightValue: Float) {
        FloatWavFileWriter(file, 44_100, 2).use { writer ->
            val frames = FloatArray(128 * 2)
            repeat(128) { frame ->
                frames[frame * 2] = leftValue
                frames[frame * 2 + 1] = rightValue
            }
            writer.writeInterleaved(frames, 128)
        }
    }

    private fun sequenceId(): () -> String {
        var value = 0
        return { "asset-${++value}" }
    }

    private fun sha(bytes: ByteArray) =
        MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
}
