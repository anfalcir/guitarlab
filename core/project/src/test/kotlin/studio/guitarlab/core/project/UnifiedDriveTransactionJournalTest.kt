package studio.guitarlab.core.project

import java.io.File
import java.nio.file.Files
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Test

class UnifiedDriveTransactionJournalTest {
    @Test fun fileJournalRoundTripsConfirmedStateAtomically() {
        val journal = FileDriveTransactionJournal(Files.createTempDirectory("u8d-journal").toFile())
        val record = DriveTransactionRecord("project", "r2", "r1", "a".repeat(64), DriveCommitStage.HEAD_VERIFIED, "r2")
        journal.save(record)
        assertEquals(record, journal.load("project"))
    }

    @Test fun malformedJournalFailsClosedAsAbsent() {
        val directory = Files.createTempDirectory("u8d-corrupt").toFile()
        File(directory, "project.properties").apply { parentFile.mkdirs(); writeText("not valid") }
        assertNull(FileDriveTransactionJournal(directory).load("project"))
    }

    @Test fun processDeathAfterRemotePublishConvergesWithoutDuplicateHead() = runBlocking {
        val local = local("payload")
        val manifest = manifest("r1", listOf(local.identity))
        val remote = InterruptOnceRemote()
        val journal = MemoryJournal()
        val durable = DurableUnifiedDriveBackupCoordinator(UnifiedDriveBackupCoordinator(remote), journal)
        assertFailsWith<CancellationException> { durable.commit(manifest, listOf(local), null) }
        assertEquals(1, remote.heads.size)
        val recovered = durable.commit(manifest, listOf(local), null)
        assertEquals("r1", recovered.descriptor.revisionId)
        assertEquals(1, remote.heads.size)
        assertEquals(DriveCommitStage.HEAD_VERIFIED, journal.record?.stage)
    }

    @Test fun differentRevisionCannotReplacePendingTransaction() = runBlocking {
        val journal = MemoryJournal().apply {
            record = DriveTransactionRecord("project", "pending", null, "a".repeat(64), DriveCommitStage.ASSETS_VERIFIED, null)
        }
        val local = local("payload")
        assertFailsWith<IllegalArgumentException> {
            DurableUnifiedDriveBackupCoordinator(UnifiedDriveBackupCoordinator(InterruptOnceRemote(false)), journal)
                .commit(manifest("different", listOf(local.identity)), listOf(local), null)
        }
        Unit
    }

    private fun local(text: String): DriveLocalAsset {
        val file = Files.createTempFile("u8d", ".bin").toFile().apply { writeText(text) }
        return DriveLocalAsset(DriveAssetObject(BackupHashing.sha256(file), file.length()), file)
    }
    private fun manifest(revision: String, assets: List<DriveAssetObject>) = DriveProjectRevisionManifest(
        "project", revision, null, 1, "c".repeat(64), assets,
    )

    private class MemoryJournal : DriveTransactionJournal {
        var record: DriveTransactionRecord? = null
        override fun load(projectId: String) = record
        override fun save(record: DriveTransactionRecord) { this.record = record }
    }

    private class InterruptOnceRemote(private var interrupt: Boolean = true) : UnifiedDriveRemoteStore {
        val assets = mutableMapOf<String, DriveRemoteObjectReceipt>()
        val manifests = mutableMapOf<String, DriveRemoteObjectReceipt>()
        val heads = mutableListOf<DrivePublishedHead>()
        override suspend fun findAsset(sha256: String) = assets[sha256]
        override suspend fun uploadAsset(asset: DriveLocalAsset) = DriveRemoteObjectReceipt(asset.identity.sha256, asset.identity.sizeBytes).also { assets[asset.identity.sha256] = it }
        override suspend fun findManifest(manifestSha256: String) = manifests[manifestSha256]
        override suspend fun uploadManifest(manifest: DriveProjectRevisionManifest) = DriveRemoteObjectReceipt(manifest.manifestSha256, manifest.canonicalBytes().size.toLong()).also { manifests[manifest.manifestSha256] = it }
        override suspend fun listHeads(projectId: String) = heads.toList()
        override suspend fun publishHead(head: DrivePublishedHead) {
            if (head !in heads) heads += head
            if (interrupt) { interrupt = false; throw CancellationException("process died after publish") }
        }
    }
}
