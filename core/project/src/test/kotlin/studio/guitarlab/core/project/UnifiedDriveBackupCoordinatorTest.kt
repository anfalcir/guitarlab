package studio.guitarlab.core.project

import java.nio.file.Files
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking
import org.junit.Test

class UnifiedDriveBackupCoordinatorTest {
    @Test fun metadataOnlyCommitUploadsManifestAndHeadButNoMedia() = runBlocking {
        val local = local("source")
        val remote = FakeRemote(assets = mutableMapOf(local.identity.sha256 to receipt(local.identity)))
        val result = UnifiedDriveBackupCoordinator(remote).commit(manifest("r1", null, listOf(local.identity)), listOf(local), null)
        assertEquals(0, result.uploadedAssets)
        assertEquals(0, result.uploadedBytes)
        assertEquals(1, remote.manifests.size)
        assertEquals("r1", remote.heads.single().descriptor.revisionId)
    }

    @Test fun oneNewTakeUploadsOnlyItsBytes() = runBlocking {
        val source = local("source")
        val take = local("new-take")
        val base = head("r1", null)
        val remote = FakeRemote(
            assets = mutableMapOf(source.identity.sha256 to receipt(source.identity)),
            heads = mutableListOf(base),
        )
        val result = UnifiedDriveBackupCoordinator(remote).commit(
            manifest("r2", "r1", listOf(source.identity, take.identity)), listOf(source, take), "r1",
        )
        assertEquals(1, result.uploadedAssets)
        assertEquals(take.identity.sizeBytes, result.uploadedBytes)
    }

    @Test fun remoteHeadConflictFailsBeforeUploadingAnything() = runBlocking {
        val local = local("payload")
        val remote = FakeRemote(heads = mutableListOf(head("remote-r2", "r1")))
        assertFailsWith<DriveConflictException> {
            UnifiedDriveBackupCoordinator(remote).commit(manifest("local-r2", "r1", listOf(local.identity)), listOf(local), "r1")
        }
        assertTrue(remote.assets.isEmpty())
        assertTrue(remote.manifests.isEmpty())
    }

    @Test fun lostFinalResponseConvergesAfterReadingPublishedHead() = runBlocking {
        val local = local("payload")
        val remote = FakeRemote(throwAfterPublish = true)
        val stages = mutableListOf<DriveCommitStage>()
        val result = UnifiedDriveBackupCoordinator(remote).commit(
            manifest("r1", null, listOf(local.identity)), listOf(local), null, stages::add,
        )
        assertTrue(result.recoveredLostPublishResponse)
        assertEquals(DriveCommitStage.entries.toList(), stages)
    }

    @Test fun wrongServerReceiptFailsBeforeHeadPublication() = runBlocking {
        val local = local("payload")
        val remote = FakeRemote(corruptAssetReceipt = true)
        assertFailsWith<IllegalArgumentException> {
            UnifiedDriveBackupCoordinator(remote).commit(manifest("r1", null, listOf(local.identity)), listOf(local), null)
        }
        assertTrue(remote.heads.isEmpty())
    }

    private fun local(text: String): DriveLocalAsset {
        val file = Files.createTempFile("u8b", ".asset").toFile().apply { writeText(text) }
        return DriveLocalAsset(DriveAssetObject(BackupHashing.sha256(file), file.length()), file)
    }

    private fun manifest(revision: String, base: String?, assets: List<DriveAssetObject>) = DriveProjectRevisionManifest(
        projectId = "project", revisionId = revision, baseRevisionId = base, createdAtEpochMs = 1,
        canonicalProjectStateSha256 = "c".repeat(64), assets = assets,
    )

    private fun head(revision: String, base: String?) = DrivePublishedHead(
        DriveCurrentDescriptor("project", revision, "d".repeat(64)), base,
    )

    private fun receipt(asset: DriveAssetObject) = DriveRemoteObjectReceipt(asset.sha256, asset.sizeBytes)

    private class FakeRemote(
        val assets: MutableMap<String, DriveRemoteObjectReceipt> = mutableMapOf(),
        val manifests: MutableMap<String, DriveRemoteObjectReceipt> = mutableMapOf(),
        val heads: MutableList<DrivePublishedHead> = mutableListOf(),
        private val throwAfterPublish: Boolean = false,
        private val corruptAssetReceipt: Boolean = false,
    ) : UnifiedDriveRemoteStore {
        override suspend fun findAsset(sha256: String) = assets[sha256]
        override suspend fun uploadAsset(asset: DriveLocalAsset): DriveRemoteObjectReceipt {
            val value = if (corruptAssetReceipt) {
                DriveRemoteObjectReceipt("0".repeat(64), asset.identity.sizeBytes)
            } else {
                DriveRemoteObjectReceipt(asset.identity.sha256, asset.identity.sizeBytes)
            }
            assets[asset.identity.sha256] = value
            return value
        }
        override suspend fun findManifest(manifestSha256: String) = manifests[manifestSha256]
        override suspend fun uploadManifest(manifest: DriveProjectRevisionManifest): DriveRemoteObjectReceipt {
            return DriveRemoteObjectReceipt(manifest.manifestSha256, manifest.canonicalBytes().size.toLong())
                .also { manifests[manifest.manifestSha256] = it }
        }
        override suspend fun listHeads(projectId: String) = heads.toList()
        override suspend fun publishHead(head: DrivePublishedHead) {
            heads += head
            if (throwAfterPublish) error("response lost")
        }
    }
}
