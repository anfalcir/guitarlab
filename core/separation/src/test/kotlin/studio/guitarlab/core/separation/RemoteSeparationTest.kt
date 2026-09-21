package studio.guitarlab.core.separation
import org.junit.Assert.*
import org.junit.Test
import java.util.UUID
class RemoteSeparationTest {
 private fun id()=RemoteJobIdentity(UUID.randomUUID().toString(),"project-1","source-1","a".repeat(64))
 @Test fun terminalStatesNeverRegress(){assertFalse(RemoteStateMachine.accepts(RemoteJobState.IMPORTED,RemoteJobState.RUNNING));assertFalse(RemoteStateMachine.accepts(RemoteJobState.FAILED,RemoteJobState.QUEUED));assertTrue(RemoteStateMachine.accepts(RemoteJobState.RUNNING,RemoteJobState.COMPLETED))}
 @Test fun sixStemContractIsPinned(){val i=id();val s=RemoteResultManifest.STEMS.map{RemoteStem(it,"results/"+it+".wav",45,"b".repeat(64))};RemoteResultManifest(i.jobId,i.projectId,i.inputSha256,"demucs.cpp","rc5","htdemucs_6s",RemoteResultManifest.MODEL_SHA256,44100,2,44100,1.0,s).validateFor(i)}
 @Test fun ownershipFailsClosed(){val i=id();val s=RemoteResultManifest.STEMS.map{RemoteStem(it,it+".wav",45,"b".repeat(64))};val m=RemoteResultManifest(i.jobId,"other",i.inputSha256,"demucs.cpp","rc5","htdemucs_6s",RemoteResultManifest.MODEL_SHA256,44100,2,1,1.0,s);assertThrows(IllegalArgumentException::class.java){m.validateFor(i)}}
 @Test fun retryPolicyProtectsIntegrity(){assertFalse(RemoteFailurePolicy.shouldRetry("INPUT_HASH_MISMATCH",0));assertFalse(RemoteFailurePolicy.shouldRetry("QUOTA_EXCEEDED",0));assertTrue(RemoteFailurePolicy.shouldRetry("BACKEND_UNAVAILABLE",0))}
}
