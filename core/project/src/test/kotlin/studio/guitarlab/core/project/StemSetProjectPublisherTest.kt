package studio.guitarlab.core.project

import java.io.ByteArrayInputStream
import java.security.MessageDigest
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import studio.guitarlab.core.model.*

class StemSetProjectPublisherTest {
 @get:Rule val temp=TemporaryFolder()
 @Test fun sixStemsPublishAtomicallyAndRetryIsIdempotent(){val root=temp.newFolder();val repo=FileProjectRepository(root);val media=ProjectManagedMediaStore(root){"media-id"+System.nanoTime()};val sourceBytes=ByteArray(64){1};val managed=media.ingest("p","source.wav",ByteArrayInputStream(sourceBytes));val source=ManagedAsset("source",AssetRole.SOURCE_ORIGINAL,managed.relativePath,sha(sourceBytes),sourceBytes.size.toLong(),"wav",44100,2,1,1,AssetClassification.AUTHORITATIVE);repo.save(ProjectFactory(idGenerator={"p"}).create("P",ProjectTemplate.BLANK).copy(assets=listOf(source),preparation=PreparationState(PreparationStatus.SOURCE_READY,"source")));val stems=StemSetProjectPublisher.ROLES.keys.map{ValidatedStem(it,ByteArray(48){2},sha(ByteArray(48){2}),44100,2,1)};val request=StemSetPublicationRequest("p","job","source",source.sha256,"a".repeat(64),"demucs.cpp","htdemucs_6s","b".repeat(64),stems);val publisher=StemSetProjectPublisher(repo,media,{2});assertFalse(publisher.publish(request));assertTrue(publisher.publish(request));val saved=repo.load("p")!!;assertEquals(6,saved.preparation!!.activeStemAssetIds.size);assertEquals(7,saved.assets.size);assertEquals(PreparationStatus.READY,saved.preparation!!.status)}
 @Test fun wrongHashLeavesProjectAndManagedSetUntouched(){val root=temp.newFolder();val repo=FileProjectRepository(root);val media=ProjectManagedMediaStore(root);val sourceBytes=ByteArray(64){1};val managed=media.ingest("p","source.wav",ByteArrayInputStream(sourceBytes));val source=ManagedAsset("source",AssetRole.SOURCE_ORIGINAL,managed.relativePath,sha(sourceBytes),64,"wav",44100,2,1,1,AssetClassification.AUTHORITATIVE);repo.save(ProjectFactory(idGenerator={"p"}).create("P",ProjectTemplate.BLANK).copy(assets=listOf(source),preparation=PreparationState(PreparationStatus.SOURCE_READY,"source")));val stems=StemSetProjectPublisher.ROLES.keys.map{ValidatedStem(it,ByteArray(48),"0".repeat(64),44100,2,1)};assertThrows(IllegalArgumentException::class.java){StemSetProjectPublisher(repo,media).publish(StemSetPublicationRequest("p","job","source",source.sha256,"a".repeat(64),"demucs.cpp","htdemucs_6s","b".repeat(64),stems))};assertEquals(1,repo.load("p")!!.assets.size)}
 private fun sha(b:ByteArray)=MessageDigest.getInstance("SHA-256").digest(b).joinToString(""){"%02x".format(it)}
}
