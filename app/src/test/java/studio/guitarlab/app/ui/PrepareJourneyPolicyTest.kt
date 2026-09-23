package studio.guitarlab.app.ui

import java.util.UUID
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import studio.guitarlab.core.model.AssetClassification
import studio.guitarlab.core.model.AssetRole
import studio.guitarlab.core.model.GuitarProject
import studio.guitarlab.core.model.ManagedAsset
import studio.guitarlab.core.model.PreparationState
import studio.guitarlab.core.model.PreparationStatus
import studio.guitarlab.core.model.ProjectTemplate
import studio.guitarlab.core.separation.DurableRemoteJob
import studio.guitarlab.core.separation.RemoteJobIdentity
import studio.guitarlab.core.separation.RemoteJobState
import studio.guitarlab.platform.source.android.SourceOperationSnapshot
import studio.guitarlab.platform.source.android.SourceOperationState

class PrepareJourneyPolicyTest {
    @Test fun noSourceStartsAtSourceAndUsesUserSafeCopy() {
        val snapshot = PrepareJourneyPolicy.resolve(project(), null, null, false, false)
        assertEquals(PrepareStage.SOURCE, snapshot.activeStage)
        assertFalse(snapshot.sourceAccepted)
        assertTrue(snapshot.headline.contains("fonte", ignoreCase = true))
        assertFalse(snapshot.headline.contains("SOURCE_"))
    }

    @Test fun acceptedSourceAdvancesToSeparationAndCollapsesSourceStep() {
        val p = project(
            assets = listOf(asset("source", AssetRole.SOURCE_ORIGINAL)),
            preparation = PreparationState(status = PreparationStatus.SOURCE_READY, sourceAssetId = "source"),
        )
        val snapshot = PrepareJourneyPolicy.resolve(p, null, null, false, false)
        assertEquals(PrepareStage.SEPARATION, snapshot.activeStage)
        assertTrue(snapshot.sourceAccepted)
        assertEquals(PrepareStageState.COMPLETE, snapshot.summaries.single { it.stage == PrepareStage.SOURCE }.state)
    }

    @Test fun sixStemsAdvanceToAutomaticReferencesWithoutUserDecision() {
        val stemIds = stemRoles.associateWith { "stem-${it.name}" }
        val p = project(
            preparation = PreparationState(
                status = PreparationStatus.READY,
                sourceAssetId = "source",
                activeStemAssetIds = stemIds,
            ),
        )
        val snapshot = PrepareJourneyPolicy.resolve(p, null, importedJob(), false, false)
        assertEquals(PrepareStage.REFERENCES, snapshot.activeStage)
        assertTrue(snapshot.stemsReady)
        assertFalse(snapshot.referenceRetryRequired)
        assertTrue(snapshot.headline.contains("automaticamente"))
    }

    @Test fun referenceFailurePreservesStemsAndSurfacesRetryOnlyAtReferenceStep() {
        val stemIds = stemRoles.associateWith { "stem-${it.name}" }
        val p = project(
            preparation = PreparationState(
                status = PreparationStatus.ERROR,
                sourceAssetId = "source",
                activeStemAssetIds = stemIds,
            ),
        )
        val snapshot = PrepareJourneyPolicy.resolve(p, null, importedJob(), false, false)
        assertEquals(PrepareStage.REFERENCES, snapshot.activeStage)
        assertTrue(snapshot.referenceRetryRequired)
        assertEquals(PrepareStageState.ATTENTION, snapshot.summaries.single { it.stage == PrepareStage.REFERENCES }.state)
    }

    @Test fun preparedReferencesAdvanceToReady() {
        val stemIds = stemRoles.associateWith { "stem-${it.name}" }
        val p = project(
            preparation = PreparationState(
                status = PreparationStatus.READY,
                sourceAssetId = "source",
                activeStemAssetIds = stemIds,
                activeBackingAssetId = "backing",
                activeGuitarAssetId = "guitar",
                availableReferenceAssetIds = listOf("backing", "guitar"),
            ),
        )
        val snapshot = PrepareJourneyPolicy.resolve(p, null, importedJob(), false, false)
        assertEquals(PrepareStage.READY, snapshot.activeStage)
        assertTrue(snapshot.referencesReady)
    }

    @Test fun replacementReturnsToSourceWithoutTreatingProtectedSourceAsNewGeneration() {
        val p = project(
            assets = listOf(asset("source", AssetRole.SOURCE_ORIGINAL)),
            preparation = PreparationState(status = PreparationStatus.SOURCE_READY, sourceAssetId = "source"),
        )
        val snapshot = PrepareJourneyPolicy.resolve(p, null, null, false, true)
        assertEquals(PrepareStage.SOURCE, snapshot.activeStage)
        assertFalse(snapshot.sourceAccepted)
        assertTrue(snapshot.headline.contains("protegida"))
    }

    @Test fun scoreBadgeNeverReliesOnColorAndPreservesNumericValue() {
        val badge = PrepareJourneyPolicy.candidateScore(88)
        assertEquals("Excelente · 88/100", badge.visibleText)
        assertEquals("Compatibilidade Excelente, 88 de 100", badge.accessibilityText)
    }

    @Test fun rawOperationMessagesAreNotUsedAsPrimaryJourneyCopy() {
        val op = SourceOperationSnapshot("p", "op", SourceOperationState.RETRYING, 42, "HTTP_503_BACKEND_UNAVAILABLE", 1)
        assertEquals("A conexão foi interrompida. Uma nova tentativa está agendada.", PrepareJourneyPolicy.sourceOperationMessage(op))
        val failed = DurableRemoteJob(identity(), RemoteJobState.FAILED, 1, errorCode = "REMOTE_JOB_NOT_FOUND")
        val message = PrepareJourneyPolicy.separationMessage(failed)
        assertFalse(message.contains("REMOTE_JOB_NOT_FOUND"))
        assertFalse(message.contains("FAILED"))
    }

    @Test fun retryCopyExposesPipelineStageWithoutRawCode() {
        val retrying = DurableRemoteJob(
            identity(),
            RemoteJobState.UPLOADING,
            1,
            errorCode = "RETRY:UPLOADING:NETWORK_UNAVAILABLE:ATTEMPT_2",
        )
        val message = PrepareJourneyPolicy.separationMessage(retrying)
        assertTrue(message.contains("envio", ignoreCase = true))
        assertTrue(message.contains("nova tentativa", ignoreCase = true))
        assertFalse(message.contains("NETWORK_UNAVAILABLE"))
    }

    @Test fun completedCloudResultWithImportFailureIsRecoverableAndDoesNotClaimExpiration() {
        val failedImport = DurableRemoteJob(
            identity(),
            RemoteJobState.IMPORT_FAILED,
            1,
            resultManifestSha256 = "b".repeat(64),
            errorCode = "WORKER_RETRY_EXHAUSTED_RETRY:DOWNLOADING_RESULTS:NETWORK_IO:ATTEMPT_6",
        )
        val message = PrepareJourneyPolicy.separationMessage(failedImport)
        assertTrue(message.contains("importação", ignoreCase = true))
        assertTrue(message.contains("preservados", ignoreCase = true))
        assertFalse(message.contains("expir", ignoreCase = true))
        assertFalse(PrepareJourneyPolicy.separationIsActive(failedImport))
    }

    @Test fun missingCloudSessionPointsToSettingsWithoutLeakingTechnicalCode() {
        val failed = DurableRemoteJob(
            identity(),
            RemoteJobState.FAILED,
            1,
            errorCode = "TERMINAL:AUTHENTICATING:AUTH_REQUIRED",
        )
        val message = PrepareJourneyPolicy.separationMessage(failed)
        assertTrue(message.contains("Opções"))
        assertTrue(message.contains("Conta e nuvem"))
        assertFalse(message.contains("AUTH_REQUIRED"))
    }

    @Test fun disabledAnonymousAuthFailsActionablyWithoutLeakingTechnicalCode() {
        val failed = DurableRemoteJob(
            identity(),
            RemoteJobState.FAILED,
            1,
            errorCode = "TERMINAL:AUTHENTICATING:AUTH_PROVIDER_DISABLED",
        )
        val message = PrepareJourneyPolicy.separationMessage(failed)
        assertTrue(message.contains("autenticação", ignoreCase = true))
        assertTrue(message.contains("desativada", ignoreCase = true))
        assertFalse(message.contains("AUTH_PROVIDER_DISABLED"))
    }

    private fun project(
        assets: List<ManagedAsset> = emptyList(),
        preparation: PreparationState? = null,
    ) = GuitarProject(
        id = "p",
        name = "Song",
        template = ProjectTemplate.GUITAR,
        createdAtEpochMs = 1,
        updatedAtEpochMs = 2,
        assets = assets,
        preparation = preparation,
    )

    private fun asset(id: String, role: AssetRole) = ManagedAsset(
        assetId = id,
        role = role,
        relativePath = "media/$id.wav",
        sha256 = "a".repeat(64),
        byteSize = 1024,
        format = "wav",
        createdAtEpochMs = 1,
        classification = AssetClassification.AUTHORITATIVE,
    )

    private fun identity() = RemoteJobIdentity(UUID.randomUUID().toString(), "p", "source", "a".repeat(64))
    private fun importedJob() = DurableRemoteJob(identity(), RemoteJobState.IMPORTED, 1)

    private val stemRoles = listOf(
        AssetRole.STEM_DRUMS,
        AssetRole.STEM_BASS,
        AssetRole.STEM_OTHER,
        AssetRole.STEM_VOCALS,
        AssetRole.STEM_GUITAR,
        AssetRole.STEM_PIANO,
    )
}
