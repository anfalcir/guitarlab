package studio.guitarlab.app.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import studio.guitarlab.core.codec.AudioImportFormatPolicy
import studio.guitarlab.core.model.AssetRole
import studio.guitarlab.core.model.GuitarProject
import studio.guitarlab.core.separation.DurableRemoteJob
import studio.guitarlab.core.separation.RemoteJobState
import studio.guitarlab.core.source.RankedSourceCandidate
import studio.guitarlab.core.source.SourceSearchRules
import studio.guitarlab.platform.codec.android.AndroidMasterAudioEncoder
import studio.guitarlab.platform.codec.android.MasterExportFormat
import studio.guitarlab.platform.separation.RemoteCloudAuthClient
import studio.guitarlab.platform.separation.RemoteCloudAuthSession
import studio.guitarlab.platform.source.android.SourceOperationSnapshot
import studio.guitarlab.platform.source.android.SourceOperationState

@Composable
fun UnifiedPrepareScreen(
    project: GuitarProject?,
    projectId: String,
    onBack: () -> Unit,
    onStudio: () -> Unit,
    onExport: () -> Unit,
    onSettings: () -> Unit = {},
    candidates: List<RankedSourceCandidate> = emptyList(),
    warnings: List<String> = emptyList(),
    searchOutcome: SourceSearchOutcome? = null,
    searchBusy: Boolean = false,
    operation: SourceOperationSnapshot? = null,
    separationJob: DurableRemoteJob? = null,
    referenceBusy: Boolean = false,
    sourceReplacementActive: Boolean = false,
    onBeginSourceReplacement: () -> Unit = {},
    onCancelSourceReplacement: () -> Unit = {},
    onSearch: (artist: String, song: String) -> Unit = { _, _ -> },
    onImport: (android.net.Uri) -> Unit = {},
    onAcquire: (RankedSourceCandidate) -> Unit = {},
    onCancel: () -> Unit = {},
    onStartSeparation: () -> Unit = {},
    onResumeSeparationImport: () -> Unit = {},
    onCancelSeparation: () -> Unit = {},
    onPrepareReferences: () -> Unit = {},
    initialCloudSession: RemoteCloudAuthSession? = null,
    requestNotificationPermission: Boolean = true,
) {
    val context = LocalContext.current
    val remoteCloudAuth = remember(context) { RemoteCloudAuthClient(context) }
    var remoteCloudSession by remember(projectId, initialCloudSession) {
        mutableStateOf(initialCloudSession)
    }
    var cloudLoginDialogVisible by rememberSaveable(projectId) { mutableStateOf(false) }
    var startAfterCloudLogin by rememberSaveable(projectId) { mutableStateOf(false) }
    var resumeAfterCloudLogin by rememberSaveable(projectId) { mutableStateOf(false) }
    var pendingNotificationAction by rememberSaveable(projectId) { mutableStateOf<String?>(null) }
    var cloudAuthMessage by rememberSaveable(projectId) { mutableStateOf<String?>(null) }
    var artist by rememberSaveable(projectId) { mutableStateOf("") }
    var song by rememberSaveable(projectId) { mutableStateOf("") }
    var detailsExpanded by rememberSaveable(projectId) { mutableStateOf(false) }

    fun runCloudAction(action: String) {
        if (action == "RESUME_IMPORT") onResumeSeparationImport() else onStartSeparation()
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (!granted) {
            cloudAuthMessage = "A operação continuará, mas o Android pode ocultar a notificação de acompanhamento. Você pode reativá-la nas permissões do aplicativo."
        }
        pendingNotificationAction?.let(::runCloudAction)
        pendingNotificationAction = null
    }

    fun runCloudActionWithNotificationPermission(action: String) {
        val needsPermission =
            requestNotificationPermission &&
                Build.VERSION.SDK_INT >= 33 &&
                context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        if (needsPermission) {
            pendingNotificationAction = action
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            runCloudAction(action)
        }
    }

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) onImport(uri)
    }
    val journey = PrepareJourneyPolicy.resolve(
        project = project,
        operation = operation,
        separationJob = separationJob,
        referenceBusy = referenceBusy,
        sourceReplacementActive = sourceReplacementActive,
    )
    val preparation = project?.preparation
    val sourceAsset = preparation?.sourceAssetId?.let { sourceId -> project.assets.firstOrNull { it.assetId == sourceId } }

    ProjectShellScaffold(
        project = project,
        currentWorkspace = ProjectWorkspace.PREPARE,
        onProjects = onBack,
        onPrepare = {},
        onStudio = onStudio,
        onExport = onExport,
        onSettings = onSettings,
    ) {
        WorkspaceScrollContent {
            Text(
                journey.headline,
                modifier = Modifier.testTag("prepare-safe-state-$projectId"),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            PrepareProgressSummary(journey)

            if (sourceAsset != null) {
                Surface(
                    modifier = Modifier.fillMaxWidth().testTag("prepare-source-ready"),
                    tonalElevation = 1.dp,
                    shape = MaterialTheme.shapes.medium,
                ) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            if (sourceReplacementActive) "Fonte atual protegida" else "Fonte aceita",
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            "${sourceAsset.format.uppercase()} • ${sourceAsset.byteSize / 1024} KB",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        if (sourceReplacementActive) {
                            Text(
                                "Ela continua válida até a nova fonte ser verificada. Suas gravações e edições permanecem preservadas.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.testTag("prepare-source-replacement-note"),
                            )
                            OutlinedButton(
                                onClick = onCancelSourceReplacement,
                                modifier = Modifier.testTag("prepare-cancel-source-replacement"),
                            ) { Text("Manter fonte atual") }
                        } else {
                            OutlinedButton(
                                onClick = onBeginSourceReplacement,
                                modifier = Modifier.testTag("prepare-replace-source"),
                            ) { Text("Trocar fonte") }
                        }
                    }
                }
            }

            when (journey.activeStage) {
                PrepareStage.SOURCE -> SourceSelectionStep(
                    projectAvailable = project != null,
                    artist = artist,
                    song = song,
                    onArtistChange = { artist = it },
                    onSongChange = { song = it },
                    candidates = candidates,
                    warnings = warnings,
                    searchOutcome = searchOutcome,
                    searchBusy = searchBusy,
                    operation = operation,
                    sourceReplacementActive = sourceReplacementActive,
                    onSearch = onSearch,
                    onImport = { importLauncher.launch(AudioImportFormatPolicy.pickerMimeTypes) },
                    onAcquire = onAcquire,
                    onCancel = onCancel,
                )

                PrepareStage.SEPARATION -> SeparationStep(
                    job = separationJob,
                    cloudSession = remoteCloudSession,
                    cloudAuthMessage = cloudAuthMessage,
                    onLogin = { resumeImport ->
                        startAfterCloudLogin = !resumeImport
                        resumeAfterCloudLogin = resumeImport
                        cloudLoginDialogVisible = true
                    },
                    onStart = { runCloudActionWithNotificationPermission("START") },
                    onResumeImport = { runCloudActionWithNotificationPermission("RESUME_IMPORT") },
                    onCancel = onCancelSeparation,
                )

                PrepareStage.REFERENCES -> ReferencesStep(
                    retryRequired = journey.referenceRetryRequired,
                    busy = referenceBusy,
                    onRetry = onPrepareReferences,
                )

                PrepareStage.READY -> ReadyStep(onStudio)
            }

            if (project != null && (project.assets.isNotEmpty() || operation != null || separationJob != null)) {
                OutlinedButton(
                    onClick = { detailsExpanded = !detailsExpanded },
                    modifier = Modifier.testTag("prepare-details-toggle"),
                ) { Text(if (detailsExpanded) "Ocultar detalhes" else "Ver detalhes") }
                if (detailsExpanded) {
                    PrepareDiagnostics(project, operation, separationJob)
                }
            }
        }
    }

    CloudSeparationLoginDialog(
        visible = cloudLoginDialogVisible,
        authClient = remoteCloudAuth,
        initialEmail = remoteCloudSession?.email.orEmpty(),
        onDismiss = {
            startAfterCloudLogin = false
            resumeAfterCloudLogin = false
            cloudLoginDialogVisible = false
        },
        onAuthenticated = { session ->
            remoteCloudSession = session
            val resumingImport = resumeAfterCloudLogin
            cloudAuthMessage = if (resumingImport) {
                "Conta autenticada e autorizada. Retomando a importação…"
            } else {
                "Conta autenticada e autorizada. Iniciando a separação…"
            }
            cloudLoginDialogVisible = false
            when {
                resumingImport -> {
                    resumeAfterCloudLogin = false
                    runCloudActionWithNotificationPermission("RESUME_IMPORT")
                }
                startAfterCloudLogin -> {
                    startAfterCloudLogin = false
                    runCloudActionWithNotificationPermission("START")
                }
            }
        },
        testTagPrefix = "prepare-cloud-auth",
    )
}

@Composable
private fun PrepareProgressSummary(journey: PrepareJourneySnapshot) {
    Column(
        modifier = Modifier.fillMaxWidth().testTag("prepare-progress-summary"),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        journey.summaries.forEach { item ->
            val stateLabel = when (item.state) {
                PrepareStageState.COMPLETE -> "Concluída"
                PrepareStageState.ACTIVE -> "Etapa atual"
                PrepareStageState.UPCOMING -> "Próxima"
                PrepareStageState.ATTENTION -> "Atenção"
            }
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("prepare-step-${item.stage.name.lowercase()}")
                    .semantics { stateDescription = stateLabel },
                tonalElevation = if (item.state in setOf(PrepareStageState.ACTIVE, PrepareStageState.ATTENTION)) 2.dp else 0.dp,
                shape = MaterialTheme.shapes.small,
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("${item.stage.order}", style = MaterialTheme.typography.labelLarge)
                    Column(Modifier.weight(1f)) {
                        Text(item.stage.title, style = MaterialTheme.typography.bodyMedium)
                        Text(item.summary, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text(stateLabel, style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

@Composable
private fun SourceSelectionStep(
    projectAvailable: Boolean,
    artist: String,
    song: String,
    onArtistChange: (String) -> Unit,
    onSongChange: (String) -> Unit,
    candidates: List<RankedSourceCandidate>,
    warnings: List<String>,
    searchOutcome: SourceSearchOutcome?,
    searchBusy: Boolean,
    operation: SourceOperationSnapshot?,
    sourceReplacementActive: Boolean,
    onSearch: (String, String) -> Unit,
    onImport: () -> Unit,
    onAcquire: (RankedSourceCandidate) -> Unit,
    onCancel: () -> Unit,
) {
    if (sourceReplacementActive) {
        Text("Escolha a nova fonte", style = MaterialTheme.typography.titleMedium, modifier = Modifier.testTag("prepare-source-replacement-picker"))
    }
    Text("Importar do dispositivo", style = MaterialTheme.typography.titleMedium)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
        Button(
            enabled = projectAvailable && operation?.state !in setOf(SourceOperationState.RUNNING, SourceOperationState.RETRYING),
            onClick = onImport,
            modifier = Modifier.testTag("prepare-import-source"),
        ) { Text("Importar áudio") }
        Text(AudioImportFormatPolicy.supportedExtensionsDescription, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }

    Text("Pesquisar música", style = MaterialTheme.typography.titleMedium)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedTextField(
            value = artist,
            onValueChange = onArtistChange,
            label = { Text("Artista") },
            singleLine = true,
            modifier = Modifier.weight(1f).testTag("prepare-search-artist"),
        )
        OutlinedTextField(
            value = song,
            onValueChange = onSongChange,
            label = { Text("Música") },
            singleLine = true,
            modifier = Modifier.weight(1f).testTag("prepare-search-song"),
        )
    }
    Button(
        enabled = projectAvailable && song.isNotBlank() && !searchBusy,
        onClick = { onSearch(artist.trim(), song.trim()) },
        modifier = Modifier.testTag("prepare-search-action"),
    ) { Text(if (searchBusy) "Pesquisando…" else "Pesquisar fontes") }
    if (searchBusy) {
        Surface(
            modifier = Modifier.fillMaxWidth().testTag("prepare-search-progress"),
            tonalElevation = 1.dp,
            shape = MaterialTheme.shapes.medium,
        ) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Pesquisando fontes compatíveis…", style = MaterialTheme.typography.bodyMedium)
                Text(
                    "A busca pode levar alguns segundos. Você pode acompanhar esta operação em Atividade.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                LinearProgressIndicator(Modifier.fillMaxWidth())
            }
        }
    } else if (searchOutcome != null) {
        val terminalTag = searchOutcome.terminalState.name.lowercase()
        Surface(
            modifier = Modifier.fillMaxWidth().testTag("prepare-search-terminal-$terminalTag"),
            tonalElevation = 1.dp,
            shape = MaterialTheme.shapes.medium,
        ) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(searchOutcome.message, style = MaterialTheme.typography.bodyMedium)
                when (searchOutcome.terminalState) {
                    SourceSearchTerminalState.RESULTS -> {
                        Text(
                            if (searchOutcome.candidateCount == 1) "Resultado pronto para escolher." else "Resultados prontos para escolher.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    SourceSearchTerminalState.DID_YOU_MEAN -> {
                        searchOutcome.suggestedArtist?.let { suggestion ->
                            Text(
                                "Você quis dizer “$suggestion”?",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.testTag("prepare-search-suggestion"),
                            )
                            Button(
                                onClick = {
                                    onArtistChange(suggestion)
                                    onSongChange(searchOutcome.song)
                                    onSearch(suggestion, searchOutcome.song)
                                },
                                modifier = Modifier.testTag("prepare-search-use-suggestion"),
                            ) { Text("Usar “$suggestion” e pesquisar novamente") }
                        }
                    }
                    SourceSearchTerminalState.NO_EXACT_MATCH,
                    SourceSearchTerminalState.PROVIDER_FAILURE,
                    SourceSearchTerminalState.TIMEOUT -> {
                        OutlinedButton(
                            onClick = {
                                onArtistChange(searchOutcome.artist)
                                onSongChange(searchOutcome.song)
                                onSearch(searchOutcome.artist, searchOutcome.song)
                            },
                            modifier = Modifier.testTag("prepare-search-retry"),
                        ) { Text("Pesquisar novamente") }
                    }
                }
            }
        }
    }

    warnings.forEach { warning ->
        Text(warning, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.tertiary)
    }
    candidates.forEachIndexed { index, candidate -> SourceCandidateCard(candidate, index, onAcquire) }

    operation?.let { current ->
        Surface(Modifier.fillMaxWidth().testTag("prepare-activity"), tonalElevation = 2.dp, shape = MaterialTheme.shapes.medium) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Text("Aquisição da fonte", style = MaterialTheme.typography.titleMedium)
                Text(PrepareJourneyPolicy.sourceOperationMessage(current), style = MaterialTheme.typography.bodyMedium)
                if (current.state in setOf(SourceOperationState.RUNNING, SourceOperationState.RETRYING)) {
                    LinearProgressIndicator(progress = { current.progress.coerceIn(0, 100) / 100f }, modifier = Modifier.fillMaxWidth())
                    OutlinedButton(onClick = onCancel, modifier = Modifier.testTag("prepare-cancel-source")) { Text("Cancelar") }
                }
            }
        }
    }
}

@Composable
private fun SeparationStep(
    job: DurableRemoteJob?,
    cloudSession: RemoteCloudAuthSession?,
    cloudAuthMessage: String?,
    onLogin: (resumeImport: Boolean) -> Unit,
    onStart: () -> Unit,
    onResumeImport: () -> Unit,
    onCancel: () -> Unit,
) {
    Surface(Modifier.fillMaxWidth().testTag("prepare-separation-activity"), tonalElevation = 2.dp, shape = MaterialTheme.shapes.medium) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Separação e referências", style = MaterialTheme.typography.titleMedium)
            Text(PrepareJourneyPolicy.separationMessage(job), style = MaterialTheme.typography.bodyMedium)
            if (PrepareJourneyPolicy.separationIsActive(job)) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                if (job?.state != RemoteJobState.CANCEL_REQUESTED) {
                    OutlinedButton(onClick = onCancel, modifier = Modifier.testTag("prepare-cancel-separation")) { Text("Cancelar separação") }
                }
            } else if (cloudSession == null) {
                Text(
                    "Entre na conta da separação em nuvem aqui mesmo para continuar. A mesma sessão também aparece em Opções → Conta e nuvem.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.testTag("prepare-cloud-auth-required"),
                )
                Button(
                    onClick = { onLogin(job?.state == RemoteJobState.IMPORT_FAILED) },
                    modifier = Modifier.testTag("prepare-cloud-auth-action"),
                ) {
                    Text(
                        when (job?.state) {
                            RemoteJobState.IMPORT_FAILED -> "Entrar e retomar importação"
                            null -> "Entrar e iniciar separação"
                            else -> "Entrar e tentar novamente"
                        },
                    )
                }
            } else {
                Text(
                    "Conta autorizada: ${cloudSession.email}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.testTag("prepare-cloud-auth-session"),
                )
                cloudAuthMessage?.let { message ->
                    Text(
                        message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.testTag("prepare-cloud-auth-message"),
                    )
                }
                if (job?.state == RemoteJobState.IMPORT_FAILED) {
                    Button(onClick = onResumeImport, modifier = Modifier.testTag("prepare-resume-import")) {
                        Text("Retomar importação")
                    }
                    Text(
                        "O processamento já terminou. Esta ação baixa novamente apenas a base e a guitarra preparadas; não inicia outra separação.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.testTag("prepare-resume-import-note"),
                    )
                } else {
                    Button(onClick = onStart, modifier = Modifier.testTag("prepare-start-separation")) {
                        Text(if (job == null) "Iniciar separação" else "Tentar separação novamente")
                    }
                }
            }
        }
    }
}

@Composable
private fun ReferencesStep(retryRequired: Boolean, busy: Boolean, onRetry: () -> Unit) {
    Surface(Modifier.fillMaxWidth().testTag("prepare-reference-progress"), tonalElevation = 2.dp, shape = MaterialTheme.shapes.medium) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Preparando referências de estudo", style = MaterialTheme.typography.titleMedium)
            if (retryRequired) {
                Text("Este projeto legado preservou as faixas separadas locais. Apenas a criação da base e da guitarra de referência precisa ser repetida.")
                Button(
                    onClick = onRetry,
                    enabled = !busy,
                    modifier = Modifier.testTag("prepare-retry-references"),
                ) { Text(if (busy) "Preparando…" else "Tentar novamente") }
            } else {
                Text("O GuitarLab cria automaticamente a base sem guitarra e a guitarra de referência. Nenhuma confirmação extra é necessária.")
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun ReadyStep(onStudio: () -> Unit) {
    Surface(Modifier.fillMaxWidth().testTag("prepare-references-ready"), tonalElevation = 2.dp, shape = MaterialTheme.shapes.medium) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Pronto para o Studio", style = MaterialTheme.typography.titleMedium)
            Text("A base sem guitarra e a guitarra de referência estão ligadas ao projeto e prontas para uso.")
            Button(onClick = onStudio, modifier = Modifier.testTag("prepare-open-studio")) { Text("Abrir Studio") }
        }
    }
}

@Composable
private fun PrepareDiagnostics(project: GuitarProject, operation: SourceOperationSnapshot?, job: DurableRemoteJob?) {
    Surface(Modifier.fillMaxWidth().testTag("prepare-diagnostics"), tonalElevation = 1.dp, shape = MaterialTheme.shapes.medium) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Detalhes técnicos", style = MaterialTheme.typography.titleMedium)
            operation?.let { Text("Operação de fonte: ${it.operationId}", style = MaterialTheme.typography.bodySmall) }
            job?.let { Text("Processamento: ${it.identity.jobId}", style = MaterialTheme.typography.bodySmall) }
            val roles = listOf(
                AssetRole.SOURCE_ORIGINAL,
                AssetRole.REFERENCE_BACKING,
                AssetRole.REFERENCE_GUITAR,
                AssetRole.STEM_DRUMS,
                AssetRole.STEM_BASS,
                AssetRole.STEM_GUITAR,
                AssetRole.STEM_VOCALS,
                AssetRole.STEM_PIANO,
                AssetRole.STEM_OTHER,
            )
            roles.forEach { role ->
                project.assets.filter { it.role == role }.forEach { asset ->
                    Text(
                        "${assetRoleLabel(role)} • ${asset.format.uppercase()} • ${asset.byteSize / 1024} KB • SHA-256 ${asset.sha256}",
                        modifier = Modifier.testTag("prepare-asset-${role.name.lowercase()}"),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

private fun assetRoleLabel(role: AssetRole): String = when (role) {
    AssetRole.SOURCE_ORIGINAL -> "Fonte original"
    AssetRole.REFERENCE_BACKING -> "Base sem guitarra"
    AssetRole.REFERENCE_GUITAR -> "Guitarra de referência"
    AssetRole.STEM_DRUMS -> "Faixa separada: bateria"
    AssetRole.STEM_BASS -> "Faixa separada: baixo"
    AssetRole.STEM_GUITAR -> "Faixa separada: guitarra"
    AssetRole.STEM_VOCALS -> "Faixa separada: voz"
    AssetRole.STEM_PIANO -> "Faixa separada: piano"
    AssetRole.STEM_OTHER -> "Faixa separada: outros"
    else -> role.name.replace('_', ' ').lowercase()
}

@Composable
private fun SourceCandidateCard(candidate: RankedSourceCandidate, index: Int, onAcquire: (RankedSourceCandidate) -> Unit) {
    val badge = PrepareJourneyPolicy.candidateScore(candidate.score)
    Surface(Modifier.fillMaxWidth().testTag("prepare-candidate-$index"), tonalElevation = 1.dp, shape = MaterialTheme.shapes.medium) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Text(candidate.title, style = MaterialTheme.typography.titleMedium)
            Text(
                "${candidate.provider.publicLabel} • ${candidate.uploader.ifBlank { "autor desconhecido" }}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Surface(
                modifier = Modifier
                    .testTag("prepare-candidate-score-$index")
                    .semantics { contentDescription = badge.accessibilityText },
                tonalElevation = 1.dp,
                shape = MaterialTheme.shapes.small,
            ) {
                Text(badge.visibleText, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), style = MaterialTheme.typography.labelLarge)
            }
            Text("${candidate.quality} • ${SourceSearchRules.durationLabel(candidate.durationSeconds)}", style = MaterialTheme.typography.bodySmall)
            Text(candidate.reason, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Button(
                onClick = { onAcquire(candidate) },
                enabled = candidate.automaticDownloadSupported && !candidate.previewOnly,
                modifier = Modifier.testTag("prepare-use-candidate-$index"),
            ) { Text("Usar esta fonte") }
        }
    }
}

@Composable
fun UnifiedExportScreen(
    project: GuitarProject?,
    projectId: String,
    viewModel: HomeViewModel,
    onBack: () -> Unit,
    onPrepare: () -> Unit,
    onStudio: () -> Unit,
    onSettings: () -> Unit = {},
) {
    val uiState by viewModel.state.collectAsState()
    val safeName = project?.name.orEmpty().replace(Regex("[^A-Za-z0-9._ -]"), "_").trim().ifBlank { "GuitarLab" }
    val preparation = project?.preparation
    val backing = preparation?.activeBackingAssetId?.let { id -> project.assets.singleOrNull { it.assetId == id && it.role == AssetRole.REFERENCE_BACKING } }
    val guitar = preparation?.activeGuitarAssetId?.let { id -> project.assets.singleOrNull { it.assetId == id && it.role == AssetRole.REFERENCE_GUITAR } }

    fun codecSupported(asset: studio.guitarlab.core.model.ManagedAsset?, format: MasterExportFormat): Boolean =
        asset?.sampleRateHz?.let { rate -> asset.channelCount?.let { channels -> AndroidMasterAudioEncoder.isSupported(format, rate, channels) } } ?: false

    val projectLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri -> if (uri != null) viewModel.saveProjectPackage(projectId, uri) }

    val backingWav = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("audio/wav")) { uri -> if (uri != null) viewModel.exportStudyReference(projectId, StudyExportKind.BACKING, uri, MasterExportFormat.WAV_FLOAT32) }
    val backingFlac = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("audio/flac")) { uri -> if (uri != null) viewModel.exportStudyReference(projectId, StudyExportKind.BACKING, uri, MasterExportFormat.FLAC) }
    val backingMp3 = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("audio/mpeg")) { uri -> if (uri != null) viewModel.exportStudyReference(projectId, StudyExportKind.BACKING, uri, MasterExportFormat.MP3) }
    val guitarWav = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("audio/wav")) { uri -> if (uri != null) viewModel.exportStudyReference(projectId, StudyExportKind.GUITAR, uri, MasterExportFormat.WAV_FLOAT32) }
    val guitarFlac = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("audio/flac")) { uri -> if (uri != null) viewModel.exportStudyReference(projectId, StudyExportKind.GUITAR, uri, MasterExportFormat.FLAC) }
    val guitarMp3 = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("audio/mpeg")) { uri -> if (uri != null) viewModel.exportStudyReference(projectId, StudyExportKind.GUITAR, uri, MasterExportFormat.MP3) }

    val masterWav = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("audio/wav")) { uri -> if (uri != null) viewModel.exportMaster(projectId, uri, MasterExportFormat.WAV_FLOAT32) }
    val masterFlac = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("audio/flac")) { uri -> if (uri != null) viewModel.exportMaster(projectId, uri, MasterExportFormat.FLAC) }
    val masterMp3 = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("audio/mpeg")) { uri -> if (uri != null) viewModel.exportMaster(projectId, uri, MasterExportFormat.MP3) }

    ProjectShellScaffold(
        project = project,
        currentWorkspace = ProjectWorkspace.EXPORT,
        onProjects = onBack,
        onPrepare = onPrepare,
        onStudio = onStudio,
        onExport = {},
        onSettings = onSettings,
    ) {
        WorkspaceScrollContent {
        Text("O Studio usa as referências gerenciadas diretamente. Esta tela só cria arquivos externos quando você pedir.", color = MaterialTheme.colorScheme.onSurfaceVariant)

        if (uiState.exportBusy) {
            Surface(Modifier.fillMaxWidth().testTag("export-progress"), tonalElevation = 2.dp, shape = MaterialTheme.shapes.medium) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(uiState.exportOperationLabel ?: "Exportando…", style = MaterialTheme.typography.titleMedium)
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    OutlinedButton(onClick = viewModel::cancelExport, modifier = Modifier.testTag("export-cancel")) { Text("Cancelar") }
                }
            }
        }

        Text("Projeto portátil", style = MaterialTheme.typography.titleMedium)
        Text("Pacote portátil completo do projeto. Não altera os arquivos internos gerenciados.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Button(enabled = !uiState.exportBusy, onClick = { projectLauncher.launch("$safeName.guitarlab") }, modifier = Modifier.testTag("export-project")) { Text("Salvar .guitarlab") }

        Text("Arquivos para estudo", style = MaterialTheme.typography.titleMedium)
        Text("As duas referências finais — base sem guitarra e guitarra — ficam em WAV sem perdas dentro do projeto; os seis stems intermediários não são armazenados no fluxo v2. WAV é publicado sem conversão; FLAC/MP3 só são gerados quando você pedir.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        StudyExportRow(
            label = "Base sem guitarra", ready = backing != null, busy = uiState.exportBusy,
            flacSupported = codecSupported(backing, MasterExportFormat.FLAC), mp3Supported = codecSupported(backing, MasterExportFormat.MP3),
            onWav = { backingWav.launch("$safeName-backing.wav") }, onFlac = { backingFlac.launch("$safeName-backing.flac") }, onMp3 = { backingMp3.launch("$safeName-backing.mp3") },
            tagPrefix = "export-backing",
        )
        StudyExportRow(
            label = "Guitarra de referência", ready = guitar != null, busy = uiState.exportBusy,
            flacSupported = codecSupported(guitar, MasterExportFormat.FLAC), mp3Supported = codecSupported(guitar, MasterExportFormat.MP3),
            onWav = { guitarWav.launch("$safeName-guitar.wav") }, onFlac = { guitarFlac.launch("$safeName-guitar.flac") }, onMp3 = { guitarMp3.launch("$safeName-guitar.mp3") },
            tagPrefix = "export-guitar",
        )

        Text("Mix final do Studio", style = MaterialTheme.typography.titleMedium)
        Text("Renderiza a timeline e o mixer atuais uma única vez. WAV publica o render; FLAC/MP3 fazem somente a codificação final escolhida.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        val masterRate = project?.sampleRate?.fixedHz ?: project?.clips?.firstNotNullOfOrNull { it.editingSampleRateHz ?: it.sourceSampleRateHz }
        val canMaster = project?.clips?.isNotEmpty() == true && masterRate != null
        val canMasterFlac = canMaster && AndroidMasterAudioEncoder.isSupported(MasterExportFormat.FLAC, masterRate!!, 2)
        val canMasterMp3 = canMaster && AndroidMasterAudioEncoder.isSupported(MasterExportFormat.MP3, masterRate, 2)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(enabled = canMaster && !uiState.exportBusy, onClick = { masterWav.launch("$safeName-master.wav") }, modifier = Modifier.testTag("export-master-wav")) { Text("WAV") }
            OutlinedButton(enabled = canMasterFlac && !uiState.exportBusy, onClick = { masterFlac.launch("$safeName-master.flac") }, modifier = Modifier.testTag("export-master-flac")) { Text("FLAC") }
            OutlinedButton(enabled = canMasterMp3 && !uiState.exportBusy, onClick = { masterMp3.launch("$safeName-master.mp3") }, modifier = Modifier.testTag("export-master-mp3")) { Text("MP3") }
        }
        if (!canMaster) {
            Text("O mix final fica disponível quando o projeto tiver áudio utilizável na timeline.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            if (!canMasterFlac) Text("FLAC indisponível neste aparelho para o mix atual; WAV continua disponível.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (!canMasterMp3) Text("MP3 indisponível neste aparelho para o mix atual; use WAV ou outro formato disponível.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        }
    }
}

@Composable
private fun StudyExportRow(
    label: String,
    ready: Boolean,
    busy: Boolean,
    flacSupported: Boolean,
    mp3Supported: Boolean,
    onWav: () -> Unit,
    onFlac: () -> Unit,
    onMp3: () -> Unit,
    tagPrefix: String,
) {
    Surface(Modifier.fillMaxWidth(), tonalElevation = 1.dp, shape = MaterialTheme.shapes.medium) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(label, style = MaterialTheme.typography.titleSmall)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(enabled = ready && !busy, onClick = onWav, modifier = Modifier.testTag("$tagPrefix-wav")) { Text("WAV") }
                OutlinedButton(enabled = ready && flacSupported && !busy, onClick = onFlac, modifier = Modifier.testTag("$tagPrefix-flac")) { Text("FLAC") }
                OutlinedButton(enabled = ready && mp3Supported && !busy, onClick = onMp3, modifier = Modifier.testTag("$tagPrefix-mp3")) { Text("MP3 320 kbps") }
            }
            if (ready && !flacSupported) {
                Text("FLAC indisponível neste aparelho para este áudio; WAV continua disponível sem conversão.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (ready && !mp3Supported) {
                Text("MP3 indisponível neste aparelho para este áudio; use WAV ou outro formato disponível.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun WorkspaceScrollContent(content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
        content = content,
    )
}
