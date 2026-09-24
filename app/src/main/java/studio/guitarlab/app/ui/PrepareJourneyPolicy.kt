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
        RemoteJobState.IMPORT_FAILED,
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
        val legacyStemsReady = preparation?.activeStemAssetIds?.size == 6
        val referencesReady = preparation?.activeBackingAssetId != null && preparation.activeGuitarAssetId != null
        val separationReady = legacyStemsReady || referencesReady
        val stemsReady = separationReady
        val sourceNeedsAttention = !sourceAccepted && operation?.state == SourceOperationState.ERROR
        val separationNeedsAttention = sourceAccepted && !separationReady && separationJob?.state in setOf(
            RemoteJobState.IMPORT_FAILED,
            RemoteJobState.FAILED,
            RemoteJobState.CANCELLED,
            RemoteJobState.EXPIRED,
        )
        val referenceRetryRequired = legacyStemsReady && !referencesReady && preparation?.status == PreparationStatus.ERROR

        val activeStage = when {
            sourceReplacementActive || !sourceAccepted -> PrepareStage.SOURCE
            !separationReady -> PrepareStage.SEPARATION
            !referencesReady -> PrepareStage.REFERENCES
            else -> PrepareStage.READY
        }

        val summaries = PrepareStage.entries.map { stage ->
            val complete = when (stage) {
                PrepareStage.SOURCE -> sourceAccepted
                PrepareStage.SEPARATION -> separationReady
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
                referenceRetryRequired -> "Os stems legados estão seguros, mas a preparação das referências precisa ser repetida."
                referenceBusy -> "Preparando automaticamente a base sem guitarra e a guitarra de referência…"
                else -> "Os resultados remotos estão prontos. O GuitarLab está preparando as referências de estudo automaticamente."
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
        if (job == null) return "A fonte está pronta. Inicie a separação para preparar base e guitarra de referência."
        val errorCode = job.errorCode
        if (errorCode?.contains("RECOVERY:EXISTING_SAME_GENERATION") == true) {
            return when (job.state) {
                RemoteJobState.QUEUED, RemoteJobState.RUNNING ->
                    "Essa separação já está em andamento na nuvem. O GuitarLab retomou o acompanhamento."
                RemoteJobState.COMPLETED, RemoteJobState.IMPORTING ->
                    "O processamento em nuvem já terminou. Retomando a importação das referências…"
                else -> "Retomando o processamento remoto já existente…"
            }
        }
        if (errorCode?.startsWith("RETRY:") == true) {
            return when {
                errorCode.contains(":AUTHENTICATING:") -> "Não foi possível autenticar na nuvem. Nova tentativa agendada…"
                errorCode.contains(":CHECKING_REMOTE:") -> "Não foi possível consultar o processamento. Nova tentativa agendada…"
                errorCode.contains(":UPLOADING:") -> "O envio da fonte foi interrompido. Nova tentativa agendada…"
                errorCode.contains(":ENQUEUEING:") -> "A solicitação de separação não foi confirmada. Nova tentativa agendada…"
                errorCode.contains(":DOWNLOADING_RESULTS:") -> "O download/validação das faixas foi interrompido. Nova tentativa agendada…"
                errorCode.contains(":ACKNOWLEDGING:") -> "A confirmação da importação falhou temporariamente. Nova tentativa agendada…"
                else -> "Falha temporária na nuvem. Nova tentativa agendada…"
            }
        }
        return when (job.state) {
            RemoteJobState.UPLOADING -> "Enviando a fonte para a nuvem…"
            RemoteJobState.READY -> "Fonte enviada. Preparando o processamento…"
            RemoteJobState.QUEUED -> "Aguardando início do processamento em nuvem…"
            RemoteJobState.RUNNING -> "Separando a música e preparando as referências na nuvem…"
            RemoteJobState.COMPLETED, RemoteJobState.IMPORTING -> "Baixando, validando e importando base e guitarra de referência…"
            RemoteJobState.IMPORT_FAILED -> importFailureMessage(errorCode)
            RemoteJobState.IMPORTED -> "Base e guitarra de referência validadas"
            RemoteJobState.CANCEL_REQUESTED -> "Cancelamento solicitado…"
            RemoteJobState.CANCELLED -> "Separação cancelada. Você pode iniciar novamente quando quiser."
            RemoteJobState.FAILED -> failureMessage(job.errorCode)
            RemoteJobState.EXPIRED -> "O resultado remoto não está mais disponível. Você pode iniciar uma nova separação."
        }
    }

    private fun failureMessage(errorCode: String?): String = when {
        errorCode?.contains("AUTH_REQUIRED") == true ->
            "Entre na conta da separação em nuvem em Opções → Conta e nuvem. A fonte foi preservada."
        errorCode?.contains("AUTH_PROVIDER_DISABLED") == true ->
            "A autenticação da conta de separação está desativada no serviço. A fonte foi preservada; tente novamente após a configuração do serviço."
        errorCode?.contains("AUTH_") == true ->
            "Não foi possível autenticar no serviço de separação. A fonte foi preservada."
        errorCode?.contains("FIRESTORE_PERMISSION_DENIED") == true ||
            errorCode?.contains("FUNCTIONS_PERMISSION_DENIED") == true ||
            errorCode?.contains("STORAGE_NOT_AUTHORIZED") == true ->
            "O serviço recusou a autorização da separação. A fonte foi preservada."
        errorCode?.contains("STORAGE_") == true ->
            "Não foi possível enviar a fonte para a nuvem. A fonte local foi preservada."
        errorCode?.contains("ACTIVE_JOB_CONFLICT") == true ->
            "Já existe outra separação em andamento nesta conta. Aguarde ou cancele a operação ativa antes de iniciar outra."
        errorCode?.contains("MONTHLY_QUOTA_REACHED") == true ->
            "O limite mensal de separações em nuvem foi atingido."
        errorCode?.contains("REMOTE_STATE_AMBIGUOUS") == true ->
            "O GuitarLab encontrou mais de um estado remoto incompatível e preservou os dados para diagnóstico."
        else -> "Não foi possível concluir a separação. As mídias válidas do projeto foram preservadas."
    }

    private fun importFailureMessage(errorCode: String?): String = when {
        errorCode?.contains("STORAGE_NOT_AUTHENTICATED") == true ||
            errorCode?.contains("AUTH_REQUIRED") == true ->
            "A sessão da nuvem expirou antes da importação. Entre novamente e retome; os resultados remotos foram preservados."
        errorCode?.contains("STORAGE_NOT_AUTHORIZED") == true ->
            "O serviço recusou o download das referências. Atualize o app ou a configuração do serviço e retome; os resultados remotos foram preservados."
        errorCode?.contains("RESULT_INVALID") == true ->
            "As referências recebidas não passaram na validação de integridade. Os resultados remotos foram preservados para diagnóstico."
        else ->
            "O processamento em nuvem terminou, mas a importação precisa ser retomada. Os resultados remotos foram preservados."
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
            PrepareStageState.COMPLETE -> "Separação concluída"
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
