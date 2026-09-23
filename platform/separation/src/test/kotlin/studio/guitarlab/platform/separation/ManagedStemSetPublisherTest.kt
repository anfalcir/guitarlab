package studio.guitarlab.platform.separation

import java.io.ByteArrayInputStream
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.MessageDigest
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import studio.guitarlab.core.model.AssetClassification
import studio.guitarlab.core.model.AssetRole
import studio.guitarlab.core.model.GuitarProject
import studio.guitarlab.core.model.ManagedAsset
import studio.guitarlab.core.model.PreparationState
import studio.guitarlab.core.model.PreparationStatus
import studio.guitarlab.core.model.ProjectTemplate
import studio.guitarlab.core.project.FileProjectRepository
import studio.guitarlab.core.project.ProjectManagedMediaStore
import studio.guitarlab.core.project.StemSetProjectPublisher
import studio.guitarlab.core.separation.RemoteJobIdentity
import studio.guitarlab.core.separation.RemoteResultManifest
import studio.guitarlab.core.separation.RemoteStem
import studio.guitarlab.core.separation.RemoteStemPayload

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
                RemoteStem(
                    name,
                    "remote/v1/output/$name.wav",
                    if (name == "drums") bad.declaredBytes else valid.size.toLong(),
                    if (name == "drums") bad.declaredSha else sha(valid),
                )
            }
            val manifest = RemoteResultManifest(
                identity.jobId, identity.projectId, identity.inputSha256,
                "demucs.cpp", "rc5", "htdemucs_6s", RemoteResultManifest.MODEL_SHA256,
                bad.manifestRate, bad.manifestChannels, bad.manifestFrames, 1.0, entries,
            )
            val stems = entries.associate { entry ->
                val bytes = if (entry.name == "drums") bad.bytes else valid
                entry.name to BytePayload(entry.name, bytes)
            }
            assertThrows(IllegalArgumentException::class.java) {
                publisherNoMutation().publish(identity, manifest, "c".repeat(64), stems)
            }
        }
    }

    @Test fun validationAndPublicationKeepOnlyOneStemStreamOpenAtATime() {
        val root = temporary.newFolder()
        val repo = FileProjectRepository(root)
        val media = ProjectManagedMediaStore(root)
        val sourceBytes = ByteArray(64) { 1 }
        val managedSource = media.ingest("project", "source.wav", ByteArrayInputStream(sourceBytes))
        val source = ManagedAsset(
            "source", AssetRole.SOURCE_ORIGINAL, managedSource.relativePath, sha(sourceBytes),
            sourceBytes.size.toLong(), "wav", 44100, 2, 1, 1, AssetClassification.AUTHORITATIVE,
        )
        repo.save(
            GuitarProject(
                id = "project",
                name = "Song",
                template = ProjectTemplate.GUITAR,
                createdAtEpochMs = 1,
                updatedAtEpochMs = 1,
                assets = listOf(source),
                preparation = PreparationState(PreparationStatus.SOURCE_READY, "source"),
            ),
        )
        val identity = RemoteJobIdentity(UUID.randomUUID().toString(), "project", "source", source.sha256)
        val bytes = wav(44100, 2, 8)
        val openNow = AtomicInteger(0)
        val maxOpen = AtomicInteger(0)
        val entries = RemoteResultManifest.STEMS.map { name ->
            RemoteStem(name, "remote/v1/output/$name.wav", bytes.size.toLong(), sha(bytes))
        }
        val payloads = entries.associate { entry ->
            entry.name to TrackingPayload(entry.name, bytes, openNow, maxOpen)
        }
        val manifest = RemoteResultManifest(
            identity.jobId, identity.projectId, identity.inputSha256,
            "demucs.cpp", "rc5", "htdemucs_6s", RemoteResultManifest.MODEL_SHA256,
            44100, 2, 8, 1.0, entries,
        )
        ManagedStemSetPublisher(StemSetProjectPublisher(repo, media)).publish(identity, manifest, "c".repeat(64), payloads)
        assertEquals(1, maxOpen.get())
    }

    private fun publisherNoMutation() = ManagedStemSetPublisher(
        StemSetProjectPublisher(
            object : studio.guitarlab.core.project.ProjectRepository {
                override fun list() = emptyList<GuitarProject>()
                override fun load(projectId: String): GuitarProject? = error("delegate must not be reached")
                override fun save(project: GuitarProject) = project
                override fun delete(projectId: String) = false
                override fun duplicate(projectId: String, newName: String, newProjectId: String, nowEpochMs: Long): GuitarProject = error("unused")
            },
            ProjectManagedMediaStore(temporary.root),
        ),
    )

    private fun wav(rate: Int, channels: Int, frames: Int): ByteArray {
        val data = frames * channels * 2
        return ByteBuffer.allocate(44 + data).order(ByteOrder.LITTLE_ENDIAN)
            .put("RIFF".toByteArray()).putInt(36 + data).put("WAVEfmt ".toByteArray()).putInt(16)
            .putShort(1).putShort(channels.toShort()).putInt(rate).putInt(rate * channels * 2)
            .putShort((channels * 2).toShort()).putShort(16).put("data".toByteArray()).putInt(data)
            .apply { repeat(data) { put(0) } }.array()
    }

    private fun sha(bytes: ByteArray) =
        MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }

    private data class Case(
        val bytes: ByteArray,
        val declaredBytes: Long,
        val declaredSha: String,
        val manifestRate: Int,
        val manifestChannels: Int,
        val manifestFrames: Long,
    )

    private class BytePayload(
        override val name: String,
        private val bytes: ByteArray,
    ) : RemoteStemPayload {
        override val byteCount: Long = bytes.size.toLong()
        override fun openStream(): InputStream = ByteArrayInputStream(bytes)
    }

    private class TrackingPayload(
        override val name: String,
        private val bytes: ByteArray,
        private val openNow: AtomicInteger,
        private val maxOpen: AtomicInteger,
    ) : RemoteStemPayload {
        override val byteCount: Long = bytes.size.toLong()
        override fun openStream(): InputStream {
            val now = openNow.incrementAndGet()
            maxOpen.accumulateAndGet(now, ::maxOf)
            val delegate = ByteArrayInputStream(bytes)
            return object : InputStream() {
                private var closed = false
                override fun read(): Int = delegate.read()
                override fun read(b: ByteArray, off: Int, len: Int): Int = delegate.read(b, off, len)
                override fun skip(n: Long): Long = delegate.skip(n)
                override fun close() {
                    if (!closed) {
                        closed = true
                        delegate.close()
                        openNow.decrementAndGet()
                    }
                }
            }
        }
    }
}
