package studio.guitarlab.platform.separation

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.MessageDigest
import java.util.UUID
import org.junit.Assert.assertThrows
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import studio.guitarlab.core.model.GuitarProject
import studio.guitarlab.core.project.ProjectManagedMediaStore
import studio.guitarlab.core.project.ProjectRepository
import studio.guitarlab.core.project.StemSetProjectPublisher
import studio.guitarlab.core.separation.RemoteJobIdentity
import studio.guitarlab.core.separation.RemoteResultManifest
import studio.guitarlab.core.separation.RemoteStem

class ManagedStemSetPublisherTest {
    @get:Rule val temporary = TemporaryFolder()

    @Test fun corruptStemVariantsFailBeforeProjectMutation() {
        val identity = RemoteJobIdentity(UUID.randomUUID().toString(), "project", "source", "a".repeat(64))
        val valid = wav(44100, 2, 8)
        val cases = listOf(
            Case(valid.copyOf(valid.size - 1), valid.size.toLong(), sha(valid), 44100, 2, 8),
            Case(valid, valid.size.toLong() + 1, sha(valid), 44100, 2, 8),
            Case(valid, valid.size.toLong(), "b".repeat(64), 44100, 2, 8),
            Case(ByteArray(48), 48, sha(ByteArray(48)), 44100, 2, 8),
            Case(wav(48000, 2, 8), wav(48000, 2, 8).size.toLong(), sha(wav(48000, 2, 8)), 44100, 2, 8),
            Case(wav(44100, 1, 8), wav(44100, 1, 8).size.toLong(), sha(wav(44100, 1, 8)), 44100, 2, 8),
            Case(valid, valid.size.toLong(), sha(valid), 44100, 2, 80),
        )
        cases.forEach { bad ->
            val entries = RemoteResultManifest.STEMS.map { name ->
                RemoteStem(name, "remote/v1/output/$name.wav", if (name == "drums") bad.declaredBytes else valid.size.toLong(), if (name == "drums") bad.declaredSha else sha(valid))
            }
            val manifest = RemoteResultManifest(identity.jobId, identity.projectId, identity.inputSha256, "demucs.cpp", "rc5", "htdemucs_6s", RemoteResultManifest.MODEL_SHA256, bad.manifestRate, bad.manifestChannels, bad.manifestFrames, 1.0, entries)
            val stems = entries.associate { it.name to if (it.name == "drums") bad.bytes else valid }
            assertThrows(IllegalArgumentException::class.java) { publisher().publish(identity, manifest, "c".repeat(64), stems) }
        }
    }

    private fun publisher(): ManagedStemSetPublisher = ManagedStemSetPublisher(StemSetProjectPublisher(object : ProjectRepository {
        override fun list() = emptyList<GuitarProject>()
        override fun load(projectId: String): GuitarProject? = error("delegate must not be reached")
        override fun save(project: GuitarProject) = project
        override fun delete(projectId: String) = false
        override fun duplicate(projectId: String, newName: String, newProjectId: String, nowEpochMs: Long): GuitarProject = error("unused")
    }, ProjectManagedMediaStore(temporary.root)))

    private fun wav(rate: Int, channels: Int, frames: Int): ByteArray {
        val data = frames * channels * 2
        return ByteBuffer.allocate(44 + data).order(ByteOrder.LITTLE_ENDIAN)
            .put("RIFF".toByteArray()).putInt(36 + data).put("WAVEfmt ".toByteArray()).putInt(16)
            .putShort(1).putShort(channels.toShort()).putInt(rate).putInt(rate * channels * 2)
            .putShort((channels * 2).toShort()).putShort(16).put("data".toByteArray()).putInt(data)
            .apply { repeat(data) { put(0) } }.array()
    }
    private fun sha(bytes: ByteArray) = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
    private data class Case(val bytes: ByteArray, val declaredBytes: Long, val declaredSha: String, val manifestRate: Int, val manifestChannels: Int, val manifestFrames: Long)
}
