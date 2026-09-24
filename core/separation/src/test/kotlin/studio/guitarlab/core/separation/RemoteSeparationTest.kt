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

        RemoteSeparationCoordinator(
            store,
            backend,
            transport,
            { manifest },
            publisher,
            expectedResultUid = { "u" },
        ).reconcile(i)
        assertEquals(listOf("publish", "ack", "cleanup"), events)
        assertEquals(RemoteJobState.IMPORTED, store.job.state)
    }


    @Test fun preparedReferenceV2DownloadsOnlyTwoArtifactsAndAcknowledgesAfterPublication() = runBlocking {
        val i = id()
        val manifestBytes = "manifest-v2".toByteArray()
        val manifestHash = sha(manifestBytes)
        val bytes = wav()
        val refs = listOf(
            RemoteReference(
                "backing", "REFERENCE_BACKING",
                "remote/v1/users/u/jobs/${i.jobId}/output/prepared/backing.wav",
                bytes.size.toLong(), sha(bytes), 44100, 2, 1, "FLOAT32_LE",
            ),
            RemoteReference(
                "guitar", "REFERENCE_GUITAR",
                "remote/v1/users/u/jobs/${i.jobId}/output/prepared/guitar.wav",
                bytes.size.toLong(), sha(bytes), 44100, 2, 1, "FLOAT32_LE",
            ),
        )
        val manifest = RemoteResultManifest(
            jobId = i.jobId,
            projectId = i.projectId,
            inputSha256 = i.inputSha256,
            engine = "demucs.cpp",
            engineRevision = "rc13",
            model = "htdemucs_6s",
            modelSha256 = RemoteResultManifest.MODEL_SHA256,
            sampleRate = 44100,
            channels = 2,
            frames = 1,
            durationSeconds = 1.0,
            schemaVersion = 2,
            deliverables = refs,
            referenceRecipe = RemoteReferenceRecipe(
                "prepared-reference-v2",
                -1.0,
                -0.5,
                RemoteResultManifest.BACKING_STEMS,
                "guitar",
            ),
            uid = "u",
        )
        val events = mutableListOf<String>()
        val store = MemoryStore(DurableRemoteJob(i, RemoteJobState.RUNNING, 1))
        val backend = backend(DurableRemoteJob(i, RemoteJobState.COMPLETED, 2, manifestHash), events)
        val transport = object : RemoteResultTransport {
            override suspend fun uploadSource(identity: RemoteJobIdentity) = error("unused")
            override suspend fun downloadManifest(identity: RemoteJobIdentity) = manifestBytes
            override suspend fun downloadStem(identity: RemoteJobIdentity, stem: RemoteStem) = error("v2 must not download stems")
            override suspend fun downloadReference(identity: RemoteJobIdentity, reference: RemoteReference): RemoteStemPayload {
                events += "download:${reference.name}"
                return BytesPayload(reference.name, bytes)
            }
            override suspend fun cleanup(identity: RemoteJobIdentity) { events += "cleanup" }
        }
        val publisher = object : RemoteStemPublisher {
            override fun publish(
                identity: RemoteJobIdentity,
                manifest: RemoteResultManifest,
                manifestSha256: String,
                stems: Map<String, RemoteStemPayload>,
            ) = error("v2 must not publish stems")

            override fun publishReferences(
                identity: RemoteJobIdentity,
                manifest: RemoteResultManifest,
                manifestSha256: String,
                references: Map<String, RemoteStemPayload>,
            ): Boolean {
                events += "publishReferences"
                assertEquals(setOf("backing", "guitar"), references.keys)
                return false
            }
        }

        RemoteSeparationCoordinator(store, backend, transport, { manifest }, publisher).reconcile(i)

        assertEquals(
            listOf("download:backing", "download:guitar", "publishReferences", "ack", "cleanup"),
            events,
        )
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


    @Test fun completedSameGenerationIsAdoptedBeforeAnyNewUpload() = runBlocking {
        val requested = id()
        val existing = requested.copy(jobId = UUID.randomUUID().toString())
        val recovered = DurableRemoteJob(existing, RemoteJobState.COMPLETED, 9, "b".repeat(64))
        val events = mutableListOf<String>()
        val store = MemoryStore(DurableRemoteJob(requested, RemoteJobState.READY, 1))
        val result = RemoteSeparationCoordinator(
            store,
            backend(status = null, events = events, recoverable = recovered),
            unusedTransport(events),
            { error("unused") },
            unusedPublisher(),
        ).reconcile(requested)
        assertEquals(existing.jobId, result.identity.jobId)
        assertEquals(RemoteJobState.COMPLETED, result.state)
        assertTrue(result.errorCode!!.contains("EXISTING_SAME_GENERATION"))
        assertEquals(listOf("findRecoverable"), events)
        assertEquals(RemoteJobState.FAILED, store.load(requested.jobId)?.state)
    }

    @Test fun enqueueRaceAdoptsExistingJobAndNeverQueuesRequestedIdentity() = runBlocking {
        val requested = id()
        val existing = requested.copy(jobId = UUID.randomUUID().toString())
        val events = mutableListOf<String>()
        val store = MemoryStore(DurableRemoteJob(requested, RemoteJobState.READY, 1))
        val enqueueResult = RemoteEnqueueResult(
            requested,
            existing,
            RemoteEnqueueDisposition.EXISTING_SAME_GENERATION,
            RemoteJobState.RUNNING,
        )
        val result = RemoteSeparationCoordinator(
            store,
            backend(status = null, events = events, recoverable = null, enqueueResult = enqueueResult),
            unusedTransport(events),
            { error("unused") },
            unusedPublisher(),
        ).reconcile(requested)
        assertEquals(existing.jobId, result.identity.jobId)
        assertEquals(RemoteJobState.RUNNING, result.state)
        assertEquals(listOf("findRecoverable", "upload", "enqueue:input"), events)
        assertEquals(RemoteJobState.FAILED, store.load(requested.jobId)?.state)
    }

    @Test fun recoverableGenerationMismatchFailsClosed() {
        val requested = id()
        val foreign = RemoteJobIdentity(UUID.randomUUID().toString(), requested.projectId, "different-source", requested.inputSha256)
        val store = MemoryStore(DurableRemoteJob(requested, RemoteJobState.UPLOADING, 1))
        assertThrows(IllegalArgumentException::class.java) {
            runBlocking {
                RemoteSeparationCoordinator(
                    store,
                    backend(status = null, recoverable = DurableRemoteJob(foreign, RemoteJobState.COMPLETED, 2)),
                    unusedTransport(),
                    { error("unused") },
                    unusedPublisher(),
                ).reconcile(requested)
            }
        }
    }

    private fun backend(
        status: DurableRemoteJob?,
        events: MutableList<String> = mutableListOf(),
        recoverable: DurableRemoteJob? = null,
        enqueueResult: RemoteEnqueueResult? = null,
    ) = object : RemoteSeparationBackend {
        override suspend fun enqueue(identity: RemoteJobIdentity, inputPath: String): RemoteEnqueueResult {
            events += "enqueue:$inputPath"
            return enqueueResult ?: RemoteEnqueueResult(
                identity,
                identity,
                RemoteEnqueueDisposition.CREATED,
                RemoteJobState.QUEUED,
            )
        }
        override suspend fun findRecoverable(generation: RemoteSourceGeneration): DurableRemoteJob? {
            events += "findRecoverable"
            return recoverable
        }
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

    private class MemoryStore(initial: DurableRemoteJob) : RemoteJobStore {
        private val jobs = linkedMapOf(initial.identity.jobId to initial)
        val job: DurableRemoteJob get() = jobs.values.maxByOrNull { it.updatedAtMs } ?: error("empty store")
        override fun load(jobId: String) = jobs[jobId]
        override fun active() = jobs.values.toList()
        override fun save(job: DurableRemoteJob): DurableRemoteJob {
            val old = jobs[job.identity.jobId]
            if (old == null || RemoteStateMachine.accepts(old.state, job.state)) jobs[job.identity.jobId] = job
            return jobs[job.identity.jobId]!!
        }
        override fun adopt(job: DurableRemoteJob, attemptedJobId: String?): DurableRemoteJob {
            attemptedJobId?.takeIf { it != job.identity.jobId }?.let { staleId ->
                jobs[staleId]?.let { stale ->
                    jobs[staleId] = stale.copy(
                        state = RemoteJobState.FAILED,
                        updatedAtMs = job.updatedAtMs,
                        errorCode = "ADOPTED_REMOTE_JOB:${job.identity.jobId}",
                    )
                }
            }
            jobs[job.identity.jobId] = job
            return job
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
