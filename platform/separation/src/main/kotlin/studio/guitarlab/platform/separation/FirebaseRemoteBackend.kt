package studio.guitarlab.platform.separation

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.tasks.await
import studio.guitarlab.core.separation.DurableRemoteJob
import studio.guitarlab.core.separation.RemoteJobIdentity
import studio.guitarlab.core.separation.RemoteJobState
import studio.guitarlab.core.separation.RemoteSeparationBackend

class FirebaseRemoteBackend(
    private val auth: FirebaseAuth,
    private val db: FirebaseFirestore,
    private val functions: FirebaseFunctions,
) : RemoteSeparationBackend {
    override suspend fun enqueue(i: RemoteJobIdentity, inputPath: String) = staged(RemotePipelineStage.ENQUEUEING) {
        user()
        functions.getHttpsCallable("enqueueRemoteSeparation")
            .call(
                mapOf(
                    "jobId" to i.jobId,
                    "projectId" to i.projectId,
                    "inputSha256" to i.inputSha256,
                    "inputPath" to inputPath,
                    "sourceAssetId" to i.sourceAssetId,
                ),
            )
            .await()
        Unit
    }

    override suspend fun cancel(i: RemoteJobIdentity) = staged(RemotePipelineStage.CANCELLING) {
        user()
        functions.getHttpsCallable("cancelRemoteSeparation")
            .call(mapOf("jobId" to i.jobId))
            .await()
        Unit
    }

    override suspend fun acknowledge(i: RemoteJobIdentity, resultManifestSha256: String) =
        staged(RemotePipelineStage.ACKNOWLEDGING) {
            user()
            require(resultManifestSha256.matches(RemoteJobIdentity.SHA))
            functions.getHttpsCallable("acknowledgeRemoteImport")
                .call(
                    mapOf(
                        "jobId" to i.jobId,
                        "projectId" to i.projectId,
                        "resultManifestSha256" to resultManifestSha256,
                    ),
                )
                .await()
            Unit
        }

    override suspend fun status(i: RemoteJobIdentity): DurableRemoteJob? = staged(RemotePipelineStage.CHECKING_REMOTE) {
        val uid = user()
        val d = db.document("users/$uid/jobs/${i.jobId}").get().await()
        if (!d.exists()) return@staged null
        require(
            d.id == i.jobId &&
                d.getString("projectId") == i.projectId &&
                d.getString("sourceAssetId") == i.sourceAssetId &&
                d.getString("inputSha256") == i.inputSha256,
        ) { "remote job ownership mismatch" }
        DurableRemoteJob(
            i,
            RemoteJobState.valueOf(requireNotNull(d.getString("state"))),
            d.getTimestamp("updatedAt")?.toDate()?.time ?: System.currentTimeMillis(),
            d.getString("resultManifestSha256"),
            d.getString("errorCode"),
        )
    }

    private suspend fun user(): String = staged(RemotePipelineStage.AUTHENTICATING) {
        auth.currentUser?.uid ?: requireNotNull(auth.signInAnonymously().await().user?.uid) { "AUTH_REQUIRED" }
    }

    private suspend fun <T> staged(stage: RemotePipelineStage, block: suspend () -> T): T =
        try {
            block()
        } catch (error: RemotePipelineException) {
            throw error
        } catch (error: Throwable) {
            throw RemotePipelineException(stage, error)
        }
}
