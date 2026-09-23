package studio.guitarlab.core.separation

import java.io.ByteArrayInputStream
import java.io.InputStream
import java.security.MessageDigest
import java.util.UUID
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class RemoteSeparationTest {
    private fun id() = RemoteJobIdentity(
        UUID.randomUUID().toString(),
        "project-1",
        "source-1",
        "a".repeat(64),
    )

    @Test fun terminalStatesNeverRegress() {
        assertFalse(RemoteStateMachine.accepts(RemoteJobState.IMPORTED, RemoteJobState.RUNNING))
        assertFalse(RemoteStateMachine.accepts(RemoteJobState.FAILED, RemoteJobState.QUEUED))
        assertTrue(RemoteStateMachine.accepts(RemoteJobState.RUNNING, RemoteJobState.COMPLETED))
        assertTrue(RemoteStateMachine.accepts(RemoteJobState.IMPORT_FAILED, RemoteJobState.IMPORTING))
    }

    @Test fun sixStemContractIsPinned() {
        val i = id()
        val stems = RemoteResultManifest.STEMS.map {
            RemoteStem(it, "remote/v1/output/$it.wav", 45, "b".repeat(64))
        }
        RemoteResultManifest(
            i.jobId, i.projectId, i.inputSha256,
            "demucs.cpp", "rc5", "htdemucs_6s", RemoteResultManifest.MODEL_SHA256,
            44100, 2, 44100, 1.0, stems,
        ).validateFor(i)
    }

    @Test fun ownershipFailsClosed() {
        val i = id()
        val stems = RemoteResultManifest.STEMS.map {
            RemoteStem(it, "remote/v1/output/$it.wav", 45, "b".repeat(64))
        }
        val manifest = RemoteResultManifest(
            i.jobId, "other", i.inputSha256,
            "demucs.cpp", "rc5", "htdemucs_6s", RemoteResultManifest.MODEL_SHA256,
            44100, 2, 1, 1.0, stems,
        )
        assertThrows(IllegalArgumentException::class.java) { manifest.validateFor(i) }
    }

    @Test fun retryPolicyProtectsIntegrity() {
        assertFalse(RemoteFailurePolicy.shouldRetry("INPUT_HASH_MISMATCH", 0))
        assertFalse(RemoteFailurePolicy.shouldRetry("QUOTA_EXCEEDED", 0))
        assertTrue(RemoteFailurePolicy.shouldRetry("BACKEND_UNAVAILABLE", 0))
    }

    @Test fun cancelRequestedCanOnlyReachARealTerminalState() {
        assertTrue(RemoteStateMachine.accepts(RemoteJobState.CANCEL_REQUESTED, RemoteJobState.CANCELLED))
        assertTrue(RemoteStateMachine.accepts(RemoteJobState.CANCEL_REQUESTED, RemoteJobState.COMPLETED))
        assertFalse(RemoteStateMachine.accepts(RemoteJobState.CANCEL_REQUESTED, RemoteJobState.RUNNING))
    }

    @Test fun cancelRequestedWithoutRemoteTerminalizesLocally() = runBlocking {
        val i = id()
        val events = mutableListOf<String>()
        val store = MemoryStore(DurableRemoteJob(i, RemoteJobState.CANCEL_REQUESTED, 1))
        val backend = backend(status = null, events = events)
        val result = RemoteSeparationCoordinator(store, backend, unusedTransport(events), { error("unused") }, unusedPublisher())
            .reconcile(i)
        assertTrue(events.isEmpty())
        assertEquals(RemoteJobState.CANCELLED, result.state)
        assertEquals(RemoteMissingPolicy.ERROR_REMOTE_JOB_NOT_FOUND, result.errorCode)
    }

    @Test fun runningWithoutRemoteExpiresInsteadOfRemainingActive() = runBlocking {
        val i = id()
        val store = MemoryStore(DurableRemoteJob(i, RemoteJobState.RUNNING, 1))
        val result = RemoteSeparationCoordinator(store, backend(null), unusedTransport(), { error("unused") }, unusedPublisher())
            .reconcile(i)
        assertEquals(RemoteJobState.EXPIRED, result.state)
        assertEquals(RemoteMissingPolicy.ERROR_REMOTE_JOB_NOT_FOUND, result.errorCode)
    }

    @Test fun completedAndImportingWithoutRemoteBecomeRecoverableImportFailure() {
        assertEquals(RemoteJobState.IMPORT_FAILED, RemoteMissingPolicy.terminalState(RemoteJobState.COMPLETED))
        assertEquals(RemoteJobState.IMPORT_FAILED, RemoteMissingPolicy.terminalState(RemoteJobState.IMPORTING))
        assertEquals(RemoteJobState.IMPORT_FAILED, RemoteMissingPolicy.failureTerminalState(RemoteJobState.IMPORTING))
    }

    @Test fun missingRemoteJobReplaysUploadAndEnqueueIdempotently() = runBlocking {
        val i = id()
        val events = mutableListOf<String>()
        val store = MemoryStore(DurableRemoteJob(i, RemoteJobState.READY, 1))
        val backend = backend(null, events)
        val transport = object : RemoteResultTransport {
            override suspend fun uploadSource(identity: RemoteJobIdentity): String {
                events += "upload"
                return "owned/input"
            }
            override suspend fun downloadManifest(identity: RemoteJobIdentity) = error("unused")
            override suspend fun downloadStem(identity: RemoteJobIdentity, stem: RemoteStem) = error("unused")
            override suspend fun cleanup(identity: RemoteJobIdentity) {}
        }
        val result = RemoteSeparationCoordinator(store, backend, transport, { error("unused") }, unusedPublisher())
            .reconcile(i)
        assertEquals(listOf("upload", "enqueue:owned/input"), events)
        assertEquals(RemoteJobState.QUEUED, result.state)
    }

    @Test fun ackHappensOnlyAfterDurablePublicationAndPayloadsAreStreamed() = runBlocking {
        val i = id()
        val manifestBytes = "manifest".toByteArray()
        val manifestHash = sha(manifestBytes)
        val stemBytes = wav()
        val stems = RemoteResultManifest.STEMS.map {
            RemoteStem(it, "remote/v1/output/$it.wav", stemBytes.size.toLong(), sha(stemBytes))
        }
        val manifest = RemoteResultManifest(
            i.jobId, i.projectId, i.inputSha256,
            "demucs.cpp", "rc5", "htdemucs_6s", RemoteResultManifest.MODEL_SHA256,
            44100, 2, 1, 1.0, stems,
        )
        val events = mutableListOf<String>()
        val store = MemoryStore(DurableRemoteJob(i, RemoteJobState.RUNNING, 1))
        val backend = backend(DurableRemoteJob(i, RemoteJobState.COMPLETED, 2, manifestHash), events)
        val transport = object : RemoteResultTransport {
            override suspend fun uploadSource(identity: RemoteJobIdentity) = "input"
            override suspend fun downloadManifest(identity: RemoteJobIdentity) = manifestBytes
            override suspend fun downloadStem(identity: RemoteJobIdentity, stem: RemoteStem): RemoteStemPayload =
                BytesPayload(stem.name, stemBytes)
            override suspend fun cleanup(identity: RemoteJobIdentity) { events += "cleanup" }
        }
        val publisher = object : RemoteStemPublisher {
            override fun publish(
                identity: RemoteJobIdentity,
                manifest: RemoteResultManifest,
                manifestSha256: String,
                stems: Map<String, RemoteStemPayload>,
            ): Boolean {
                events += "publish"
                assertEquals(6, stems.size)
                assertTrue(stems.values.all { it.byteCount == stemBytes.size.toLong() })
                return false
            }
        }

        RemoteSeparationCoordinator(store, backend, transport, { manifest }, publisher).reconcile(i)
        assertEquals(listOf("publish", "ack", "cleanup"), events)
        assertEquals(RemoteJobState.IMPORTED, store.job.state)
    }

    @Test fun completedResultCanStillImportWhenRemoteDocumentDisappearsAfterCompletion() = runBlocking {
        val i = id()
        val manifestBytes = "manifest".toByteArray()
        val manifestHash = sha(manifestBytes)
        val stemBytes = wav()
        val manifest = RemoteResultManifest(
            i.jobId, i.projectId, i.inputSha256,
            "demucs.cpp", "rc5", "htdemucs_6s", RemoteResultManifest.MODEL_SHA256,
            44100, 2, 1, 1.0,
            RemoteResultManifest.STEMS.map {
                RemoteStem(it, "remote/v1/output/$it.wav", stemBytes.size.toLong(), sha(stemBytes))
            },
        )
        val store = MemoryStore(DurableRemoteJob(i, RemoteJobState.IMPORT_FAILED, 1, manifestHash))
        val events = mutableListOf<String>()
        val transport = object : RemoteResultTransport {
            override suspend fun uploadSource(identity: RemoteJobIdentity) = error("unused")
            override suspend fun downloadManifest(identity: RemoteJobIdentity) = manifestBytes
            override suspend fun downloadStem(identity: RemoteJobIdentity, stem: RemoteStem) = BytesPayload(stem.name, stemBytes)
            override suspend fun cleanup(identity: RemoteJobIdentity) { events += "cleanup" }
        }
        val publisher = object : RemoteStemPublisher {
            override fun publish(
                identity: RemoteJobIdentity,
                manifest: RemoteResultManifest,
                manifestSha256: String,
                stems: Map<String, RemoteStemPayload>,
            ): Boolean {
                events += "publish"
                return false
            }
        }

        val result = RemoteSeparationCoordinator(store, backend(null, events), transport, { manifest }, publisher).reconcile(i)
        assertEquals(RemoteJobState.IMPORTED, result.state)
        assertEquals(listOf("publish", "cleanup"), events)
    }

    private fun backend(
        status: DurableRemoteJob?,
        events: MutableList<String> = mutableListOf(),
    ) = object : RemoteSeparationBackend {
        override suspend fun enqueue(identity: RemoteJobIdentity, inputPath: String) { events += "enqueue:$inputPath" }
        override suspend fun status(identity: RemoteJobIdentity) = status
        override suspend fun cancel(identity: RemoteJobIdentity) { events += "cancel" }
        override suspend fun acknowledge(identity: RemoteJobIdentity, resultManifestSha256: String) { events += "ack" }
    }

    private fun unusedTransport(events: MutableList<String> = mutableListOf()) = object : RemoteResultTransport {
        override suspend fun uploadSource(identity: RemoteJobIdentity): String { events += "upload"; return "input" }
        override suspend fun downloadManifest(identity: RemoteJobIdentity) = error("unused")
        override suspend fun downloadStem(identity: RemoteJobIdentity, stem: RemoteStem) = error("unused")
        override suspend fun cleanup(identity: RemoteJobIdentity) {}
    }

    private fun unusedPublisher() = object : RemoteStemPublisher {
        override fun publish(
            identity: RemoteJobIdentity,
            manifest: RemoteResultManifest,
            manifestSha256: String,
            stems: Map<String, RemoteStemPayload>,
        ) = true
    }

    private class MemoryStore(var job: DurableRemoteJob) : RemoteJobStore {
        override fun load(jobId: String) = job
        override fun save(job: DurableRemoteJob): DurableRemoteJob {
            if (RemoteStateMachine.accepts(this.job.state, job.state)) this.job = job
            return this.job
        }
    }

    private class BytesPayload(
        override val name: String,
        private val bytes: ByteArray,
    ) : RemoteStemPayload {
        override val byteCount: Long = bytes.size.toLong()
        override fun openStream(): InputStream = ByteArrayInputStream(bytes)
    }

    private fun wav(): ByteArray {
        val bytes = ByteArray(48)
        bytes[0] = 'R'.code.toByte()
        bytes[1] = 'I'.code.toByte()
        bytes[2] = 'F'.code.toByte()
        bytes[3] = 'F'.code.toByte()
        return bytes
    }

    private fun sha(bytes: ByteArray) =
        MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
}
