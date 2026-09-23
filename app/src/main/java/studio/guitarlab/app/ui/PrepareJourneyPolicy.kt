package studio.guitarlab.app.ui

import studio.guitarlab.core.model.GuitarProject
import studio.guitarlab.core.model.PreparationStatus
import studio.guitarlab.core.separation.DurableRemoteJob
import studio.guitarlab.core.separation.RemoteJobState
import studio.guitarlab.platform.source.android.SourceOperationSnapshot
import studio.guitarlab.platform.source.android.SourceOperationState

enum class PrepareStage(val order: Int, val title: String) {
    SOURCE(1, "Fonte"),
    SEPARATION(2, "Separação"),
    REFERENCES(3, "Referências de estudo"),
    READY(4, "Pronto para o Studio"),
}

enum class PrepareStageState { COMPLETE, ACTIVE, UPCOMING, ATTENTION }

data class PrepareStageSummary(
    val stage: PrepareStage,
    val state: PrepareStageState,
    val summary: String,
)

data class PrepareJourneySnapshot(
    val activeStage: PrepareStage,
    val summaries: List<PrepareStageSummary>,
    val headline: String,
    val sourceAccepted: Boolean,
    val stemsReady: Boolean,
    val referencesReady: Boolean,
    val referenceRetryRequired: Boolean,
)

data class CandidateScoreBadge(
    val label: String,
    val score: Int,
) {
    val visibleText: String get() = "$label · $score/100"
    val accessibilityText: String get() = "Compatibilidade $label, $score de 100"
}

object PrepareJourneyPolicy {
    private val terminalSeparationStates = setOf(
        RemoteJobState.IMPORTED,
        RemoteJobState.CANCELLED,
        RemoteJobState.FAILED,
        RemoteJobState.EXPIRED,
    )

    fun resolve(
        project: GuitarProject?,
        operation: SourceOperationSnapshot?,
        separationJob: DurableRemoteJob?,
        referenceBusy: Boolean,
        sourceReplacementActive: Boolean,
    ): PrepareJourneySnapshot {
        val preparation = project?.preparation
        val sourceAccepted = preparation?.sourceAssetId != null && !sourceReplacementActive
        val stemsReady = preparation?.activeStemAssetIds?.size == 6
        val referencesReady = preparation?.activeBackingAssetId != null && preparation.activeGuitarAssetId != null
        val sourceNeedsAttention = !sourceAccepted && operation?.state == SourceOperationState.ERROR
        val separationNeedsAttention = sourceAccepted && !stemsReady && separationJob?.state in setOf(
            RemoteJobState.FAILED,
            RemoteJobState.CANCELLED,
            RemoteJobState.EXPIRED,
        )
        val referenceRetryRequired = stemsReady && !referencesReady && preparation?.status == PreparationStatus.ERROR

        val activeStage = when {
            sourceReplacementActive || !sourceAccepted -> PrepareStage.SOURCE
            !stemsReady -> PrepareStage.SEPARATION
            !referencesReady -> PrepareStage.REFERENCES
            else -> PrepareStage.READY
        }

        val summaries = PrepareStage.entries.map { stage ->
            val complete = when (stage) {
                PrepareStage.SOURCE -> sourceAccepted
                PrepareStage.SEPARATION -> stemsReady
                PrepareStage.REFERENCES -> referencesReady
                PrepareStage.READY -> referencesReady
            }
            val attention = when (stage) {
                PrepareStage.SOURCE -> sourceNeedsAttention
                PrepareStage.SEPARATION -> separationNeedsAttention
                PrepareStage.REFERENCES -> referenceRetryRequired
                PrepareStage.READY -> false
            }
            val state = when {
                complete && stage != PrepareStage.READY -> PrepareStageState.COMPLETE
                stage == activeStage && attention -> PrepareStageState.ATTENTION
                stage == activeStage -> PrepareStageState.ACTIVE
                complete -> PrepareStageState.COMPLETE
                stage.order > activeStage.order -> PrepareStageState.UPCOMING
                else -> PrepareStageState.COMPLETE
            }
            PrepareStageSummary(stage, state, summaryFor(stage, state, operation, separationJob, referenceBusy))
        }

        val headline = when (activeStage) {
            PrepareStage.SOURCE -> when {
                sourceReplacementActive -> "Escolha a nova fonte. A atual permanece protegida até a substituição ser validada."
                operation?.state in setOf(SourceOperationState.RUNNING, SourceOperationState.RETRYING) -> sourceOperationMessage(operation)
                sourceNeedsAttention -> "A fonte não pôde ser obtida. Você pode tentar novamente ou escolher outra opção."
                else -> "Escolha uma fonte por pesquisa ou importe um arquivo de áudio."
            }
            PrepareStage.SEPARATION -> separationMessage(separationJob)
            PrepareStage.REFERENCES -> when {
                referenceRetryRequired -> "As seis faixas estão seguras, mas a preparação das referências precisa ser repetida."
                referenceBusy -> "Preparando automaticamente a base sem guitarra e a guitarra de referência…"
                else -> "As seis faixas estão prontas. O GuitarLab está preparando as referências de estudo automaticamente."
            }
            PrepareStage.READY -> "Preparação concluída. A base sem guitarra e a guitarra de referência estão prontas para o Studio."
        }

        return PrepareJourneySnapshot(
            activeStage = activeStage,
            summaries = summaries,
            headline = headline,
            sourceAccepted = sourceAccepted,
            stemsReady = stemsReady,
            referencesReady = referencesReady,
            referenceRetryRequired = referenceRetryRequired,
        )
    }

    fun candidateScore(score: Int): CandidateScoreBadge {
        val normalized = score.coerceIn(0, 100)
        val label = when {
            normalized >= 85 -> "Excelente"
            normalized >= 70 -> "Boa"
            normalized >= 50 -> "Compatível"
            else -> "Baixa"
        }
        return CandidateScoreBadge(label, normalized)
    }

    fun sourceOperationMessage(operation: SourceOperationSnapshot?): String = when (operation?.state) {
        SourceOperationState.RUNNING -> when {
            operation.progress >= 90 -> "Validando e salvando a fonte…"
            operation.progress > 0 -> "Obtendo a fonte… ${operation.progress}%"
            else -> "Obtendo a fonte…"
        }
        SourceOperationState.RETRYING -> "A conexão foi interrompida. Uma nova tentativa está agendada."
        SourceOperationState.SUCCESS -> "Fonte validada e pronta para a próxima etapa"
        SourceOperationState.ERROR -> "Não foi possível obter a fonte"
        SourceOperationState.CANCELLED -> "Aquisição cancelada"
        SourceOperationState.IDLE, null -> "Aguardando fonte"
    }

    fun separationMessage(job: DurableRemoteJob?): String {
        if (job == null) return "A fonte está pronta. Inicie a separação em seis faixas quando quiser continuar."
        val errorCode = job.errorCode
        if (errorCode?.startsWith("RETRY:") == true) {
            return when {
                errorCode.contains(":AUTHENTICATING:") -> "Não foi possível autenticar na nuvem. Nova tentativa agendada…"
                errorCode.contains(":CHECKING_REMOTE:") -> "Não foi possível consultar o processamento. Nova tentativa agendada…"
                errorCode.contains(":UPLOADING:") -> "O envio da fonte foi interrompido. Nova tentativa agendada…"
                errorCode.contains(":ENQUEUEING:") -> "A solicitação de separação não foi confirmada. Nova tentativa agendada…"
                else -> "Falha temporária na nuvem. Nova tentativa agendada…"
            }
        }
        return when (job.state) {
            RemoteJobState.UPLOADING -> "Enviando a fonte para a nuvem…"
            RemoteJobState.READY -> "Fonte enviada. Preparando o processamento…"
            RemoteJobState.QUEUED -> "Aguardando início do processamento em nuvem…"
            RemoteJobState.RUNNING -> "Separando a música em seis faixas…"
            RemoteJobState.COMPLETED, RemoteJobState.IMPORTING -> "Validando e trazendo as faixas separadas…"
            RemoteJobState.IMPORTED -> "Seis faixas separadas validadas"
            RemoteJobState.CANCEL_REQUESTED -> "Cancelamento solicitado…"
            RemoteJobState.CANCELLED -> "Separação cancelada. Você pode iniciar novamente quando quiser."
            RemoteJobState.FAILED -> failureMessage(job.errorCode)
            RemoteJobState.EXPIRED -> "O processamento expirou. Você pode iniciar uma nova separação."
        }
    }

    private fun failureMessage(errorCode: String?): String = when {
        errorCode?.contains("AUTH_REQUIRED") == true ->
            "Entre na conta da separação em nuvem em Opções → Conta e nuvem. A fonte foi preservada."
        errorCode?.contains("AUTH_PROVIDER_DISABLED") == true ->
            "A autenticação anônima do serviço está desativada. A fonte foi preservada; tente novamente após a configuração do serviço."
        errorCode?.contains("AUTH_") == true ->
            "Não foi possível autenticar no serviço de separação. A fonte foi preservada."
        errorCode?.contains("FIRESTORE_PERMISSION_DENIED") == true ||
            errorCode?.contains("FUNCTIONS_PERMISSION_DENIED") == true ||
            errorCode?.contains("STORAGE_NOT_AUTHORIZED") == true ->
            "O serviço recusou a autorização da separação. A fonte foi preservada."
        errorCode?.contains("STORAGE_") == true ->
            "Não foi possível enviar a fonte para a nuvem. A fonte local foi preservada."
        else -> "Não foi possível concluir a separação. As mídias válidas do projeto foram preservadas."
    }

    fun separationIsActive(job: DurableRemoteJob?): Boolean = job != null && job.state !in terminalSeparationStates

    private fun summaryFor(
        stage: PrepareStage,
        state: PrepareStageState,
        operation: SourceOperationSnapshot?,
        separationJob: DurableRemoteJob?,
        referenceBusy: Boolean,
    ): String = when (stage) {
        PrepareStage.SOURCE -> when (state) {
            PrepareStageState.COMPLETE -> "Fonte aceita"
            PrepareStageState.ATTENTION -> "Escolha outra fonte ou tente novamente"
            PrepareStageState.ACTIVE -> sourceOperationMessage(operation)
            PrepareStageState.UPCOMING -> "Primeiro escolha uma fonte"
        }
        PrepareStage.SEPARATION -> when (state) {
            PrepareStageState.COMPLETE -> "6 faixas prontas"
            PrepareStageState.ATTENTION -> "A separação precisa de atenção"
            PrepareStageState.ACTIVE -> separationMessage(separationJob)
            PrepareStageState.UPCOMING -> "Depois da fonte"
        }
        PrepareStage.REFERENCES -> when (state) {
            PrepareStageState.COMPLETE -> "Base e guitarra de referência prontas"
            PrepareStageState.ATTENTION -> "Tente preparar as referências novamente"
            PrepareStageState.ACTIVE -> if (referenceBusy) "Preparando automaticamente…" else "Preparação automática"
            PrepareStageState.UPCOMING -> "Depois da separação"
        }
        PrepareStage.READY -> when (state) {
            PrepareStageState.COMPLETE, PrepareStageState.ACTIVE -> "Pronto para abrir no Studio"
            PrepareStageState.ATTENTION -> "Atenção necessária"
            PrepareStageState.UPCOMING -> "Conclusão da preparação"
        }
    }
}
