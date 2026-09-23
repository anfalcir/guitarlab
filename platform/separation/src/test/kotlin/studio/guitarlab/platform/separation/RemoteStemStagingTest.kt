package studio.guitarlab.platform.separation

import java.util.UUID
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import studio.guitarlab.core.separation.RemoteJobIdentity
import studio.guitarlab.core.separation.RemoteStem

class RemoteStemStagingTest {
    @get:Rule val temp = TemporaryFolder()

    @Test fun validStagedStemIsReusableAcrossWorkerRetriesWithoutHeapMaterialization() {
        val staging = RemoteStemStaging(temp.root)
        val bytes = ByteArray(4096) { (it % 251).toByte() }
        val stem = RemoteStem("drums", "remote/v1/output/drums.wav", bytes.size.toLong(), sha(bytes))
        val id = identity()
        val partial = staging.partial(id, stem)
        partial.writeBytes(bytes)

        val committed = staging.commit(id, stem, partial)
        assertEquals(bytes.size.toLong(), committed.byteCount)
        assertNotNull(staging.cached(id, stem))
        assertEquals(bytes.size.toLong(), staging.bytes(id))
    }

    @Test fun corruptedCachedStemIsRejectedAndDeleted() {
        val staging = RemoteStemStaging(temp.root)
        val good = ByteArray(128) { 7 }
        val stem = RemoteStem("bass", "remote/v1/output/bass.wav", good.size.toLong(), sha(good))
        val id = identity()
        val partial = staging.partial(id, stem)
        partial.writeBytes(good)
        staging.commit(id, stem, partial)
        val corrupt = staging.partial(id, stem)
        corrupt.writeBytes(ByteArray(good.size) { 3 })
        // Commit must fail closed and the prior good cache remains reusable.
        runCatching { staging.commit(id, stem.copy(sha256 = "b".repeat(64)), corrupt) }
        assertNotNull(staging.cached(id, stem))
    }

    @Test fun clearRemovesWholeJobStagingDirectory() {
        val staging = RemoteStemStaging(temp.root)
        val bytes = ByteArray(64) { 1 }
        val stem = RemoteStem("piano", "remote/v1/output/piano.wav", bytes.size.toLong(), sha(bytes))
        val id = identity()
        val partial = staging.partial(id, stem)
        partial.writeBytes(bytes)
        staging.commit(id, stem, partial)
        staging.clear(id)
        assertNull(staging.cached(id, stem))
        assertEquals(0L, staging.bytes(id))
    }

    private fun identity() = RemoteJobIdentity(UUID.randomUUID().toString(), "project", "source", "a".repeat(64))
    private fun sha(bytes: ByteArray) =
        java.security.MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
}
