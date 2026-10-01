package studio.guitarlab.app.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import studio.guitarlab.app.backup.BackupSettingsStore
import studio.guitarlab.core.audio.LatencyFineAdjustmentPolicy
import studio.guitarlab.core.audio.MonitoringMode
import studio.guitarlab.core.project.FileProjectRepository
import studio.guitarlab.core.project.RecordingSampleRatePolicy
import studio.guitarlab.core.project.ExternalControlAction
import studio.guitarlab.platform.separation.RemoteCloudAuthClient

@Composable
fun SettingsScreen(
    projectId: String? = null,
    onBack: () -> Unit,
    onAudioDiagnostics: () -> Unit,
    onCodecDiagnostics: () -> Unit,
    onBackupSettings: () -> Unit = {},
    onDiagnostics: () -> Unit = {},
) {
    val context = LocalContext.current
    ExternalControlHub.initialize(context)
    val externalControlState by ExternalControlHub.state.collectAsState()
    val routingStore = remember(context) { StudioAudioRoutingStore(context) }
    val backupSettings = remember(context) { BackupSettingsStore(context).snapshot() }
    var inputChoices by remember { mutableStateOf(routingStore.inputChoices()) }
    var outputChoices by remember { mutableStateOf(routingStore.outputChoices()) }
    var cueOutputChoices by remember { mutableStateOf(routingStore.cueOutputChoices()) }
    var selectedInput by remember { mutableStateOf(routingStore.selectedInputSignature()) }
    var selectedOutput by remember { mutableStateOf(routingStore.selectedOutputSignature()) }
    var selectedCueOutput by remember { mutableStateOf(routingStore.selectedCueOutputSignature()) }
    var monitoringMode by remember { mutableStateOf(routingStore.monitoringMode()) }
    val latencyStore = remember(context) { StudioLatencyCalibrationStore(context) }
    val latencyEngine = remember(context) { AndroidLatencyCalibrationEngine(context) }
    val projectRepository = remember(context) { FileProjectRepository(context.filesDir) }
    val scope = rememberCoroutineScope()
    val remoteCloudAuth = remember(context) { RemoteCloudAuthClient(context) }
    var remoteCloudSession by remember { mutableStateOf(remoteCloudAuth.currentSession()) }
    var cloudLoginDialogVisible by remember { mutableStateOf(false) }
    var cloudAuthMessage by remember { mutableStateOf<String?>(null) }
    var calibrating by remember { mutableStateOf(false) }
    var digitalVerifying by remember { mutableStateOf(false) }
    val latencyBusy = calibrating || digitalVerifying
    var calibrationStatus by remember { mutableStateOf<String?>(null) }
    var calibrationProgress by remember { mutableStateOf(0 to 0) }
    var pendingCalibration by remember { mutableStateOf(false) }
    var pendingDigitalVerification by remember { mutableStateOf(false) }
    var calibrationSampleRateHz by remember(projectId) { mutableStateOf<Int?>(null) }
    var calibrationRateStatus by remember(projectId) { mutableStateOf<String?>(null) }
    var fineAdjustmentFrames by remember { mutableStateOf(0L) }
    var calibrationDialogVisible by remember { mutableStateOf(false) }

    fun runLatencyCalibration() {
        if (latencyBusy) return
        if (selectedInput.isNullOrBlank() || selectedOutput.isNullOrBlank()) {
            calibrationStatus = "Selecione explicitamente a entrada e a saída antes de calibrar."
            return
        }
        val sampleRateHz = calibrationSampleRateHz
        if (sampleRateHz == null) {
            calibrationStatus = calibrationRateStatus ?: "Não foi possível determinar a taxa de amostragem deste projeto."
            return
        }
        val inputDevice = routingStore.resolveSelectedInputDevice()
        val outputDevice = routingStore.resolveSelectedOutputDevice()
        if (inputDevice == null || outputDevice == null) {
            calibrationStatus = "A rota selecionada não está mais disponível. Atualize os dispositivos e selecione novamente."
            return
        }
        calibrating = true
        calibrationProgress = 0 to 0
        calibrationStatus = "Confirmando silenciosamente as rotas antes do sinal de calibração…"
        scope.launch {
            runCatching {
                latencyEngine.calibrate(
                    sampleRateHz = sampleRateHz,
                    inputDevice = inputDevice,
                    outputDevice = outputDevice,
                    onProgress = { current, total -> calibrationProgress = current to total },
                )
            }.onSuccess { result ->
                latencyStore.save(selectedInput, selectedOutput, result)
                calibrationStatus = if (result.accepted) "Calibração física válida · ${result.describe()}" else "Medição física instável · ${result.describe()} · repita antes de usar compensação"
            }.onFailure { error ->
                calibrationStatus = error.message ?: "Não foi possível medir a latência física."
            }
            calibrating = false
        }
    }

    fun runSilentDigitalVerification() {
        if (latencyBusy) return
        if (selectedInput.isNullOrBlank() || selectedOutput.isNullOrBlank()) {
            calibrationStatus = "Selecione explicitamente a entrada e a saída antes da verificação digital."
            return
        }
        val sampleRateHz = calibrationSampleRateHz
        if (sampleRateHz == null) {
            calibrationStatus = calibrationRateStatus ?: "Não foi possível determinar a taxa de amostragem deste projeto."
            return
        }
        val inputDevice = routingStore.resolveSelectedInputDevice()
        val outputDevice = routingStore.resolveSelectedOutputDevice()
        if (inputDevice == null || outputDevice == null) {
            calibrationStatus = "A rota selecionada não está mais disponível. Atualize os dispositivos e selecione novamente."
            return
        }
        digitalVerifying = true
        calibrationStatus = "Executando verificação digital silenciosa…"
        scope.launch {
            runCatching {
                latencyEngine.verifyDigitalTiming(sampleRateHz, inputDevice, outputDevice)
            }.onSuccess { result ->
                calibrationStatus = "Verificação digital aprovada · ${result.describe()} · não mede a latência física round-trip."
            }.onFailure { error ->
                calibrationStatus = error.message ?: "Não foi possível verificar a sincronização digital."
            }
            digitalVerifying = false
        }
    }

    val latencyPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            when {
                pendingCalibration -> runLatencyCalibration()
                pendingDigitalVerification -> runSilentDigitalVerification()
            }
        } else {
            calibrationStatus = "Permissão de microfone necessária para analisar a rota de áudio."
        }
        pendingCalibration = false
        pendingDigitalVerification = false
    }

    LaunchedEffect(Unit) {
        inputChoices = routingStore.inputChoices()
        outputChoices = routingStore.outputChoices()
        cueOutputChoices = routingStore.cueOutputChoices()
    }

    LaunchedEffect(projectId) {
        if (projectId == null) {
            calibrationSampleRateHz = null
            calibrationRateStatus = "Abra as opções a partir de um projeto para calibrar a latência na taxa real da sessão."
        } else {
            runCatching { withContext(Dispatchers.IO) { projectRepository.load(projectId) } }
                .onSuccess { project ->
                    if (project == null) {
                        calibrationSampleRateHz = null
                        calibrationRateStatus = "O projeto não está disponível para calibração."
                    } else {
                        runCatching {
                            RecordingSampleRatePolicy.resolve(project, RecordingSampleRatePolicy.EMPTY_PROJECT_DEFAULT_HZ)
                                ?: RecordingSampleRatePolicy.EMPTY_PROJECT_DEFAULT_HZ
                        }.onSuccess { rate ->
                            calibrationSampleRateHz = rate
                            calibrationRateStatus = null
                        }.onFailure { error ->
                            calibrationSampleRateHz = null
                            calibrationRateStatus = error.message ?: "O projeto não possui uma taxa de edição única."
                        }
                    }
                }
                .onFailure { error ->
                    calibrationSampleRateHz = null
                    calibrationRateStatus = error.message ?: "Não foi possível ler a taxa do projeto."
                }
        }
    }

    LaunchedEffect(selectedInput, selectedOutput, calibrationSampleRateHz) {
        fineAdjustmentFrames = calibrationSampleRateHz?.let { rate ->
            latencyStore.fineAdjustmentFrames(selectedInput, selectedOutput, rate)
        } ?: 0L
        if (!latencyBusy) calibrationStatus = null
    }

    fun adjustFineLatency(deltaMilliseconds: Double) {
        val rate = calibrationSampleRateHz ?: return
        if (selectedInput.isNullOrBlank() || selectedOutput.isNullOrBlank()) {
            calibrationStatus = "Selecione explicitamente entrada e saída antes de aplicar ajuste fino."
            return
        }
        val deltaFrames = LatencyFineAdjustmentPolicy.millisecondsToFrames(deltaMilliseconds, rate)
        fineAdjustmentFrames = LatencyFineAdjustmentPolicy.clampFrames(fineAdjustmentFrames + deltaFrames, rate)
        latencyStore.saveFineAdjustmentFrames(selectedInput, selectedOutput, rate, fineAdjustmentFrames)
    }

    fun refreshAudioDevices() {
        inputChoices = routingStore.inputChoices()
        outputChoices = routingStore.outputChoices()
        cueOutputChoices = routingStore.cueOutputChoices()
        if (selectedInput != null && inputChoices.none { it.signature == selectedInput }) {
            selectedInput = null
            routingStore.selectInput(null)
        }
        if (selectedOutput != null && outputChoices.none { it.signature == selectedOutput }) {
            selectedOutput = null
            routingStore.selectOutput(null)
        }
        if (selectedCueOutput != null && cueOutputChoices.none { it.signature == selectedCueOutput }) {
            selectedCueOutput = null
            routingStore.selectCueOutput(null)
        }
        if (selectedCueOutput != null && selectedCueOutput == selectedOutput) {
            selectedCueOutput = null
            routingStore.selectCueOutput(null)
        }
    }

    val routeHealth = routingStore.routeHealth()

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            Modifier
                .widthIn(max = 920.dp)
                .fillMaxWidth()
                .fillMaxHeight()
                .padding(horizontal = 22.dp, vertical = 16.dp)
                .testTag("settings-content"),
        ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("Opções", style = MaterialTheme.typography.headlineMedium)
                Text(
                    if (projectId == null) "Preferências do aplicativo" else "Áudio, projeto e ferramentas do Studio",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            AppIconButton(icon = Icons.Default.ArrowBack, contentDescription = "Voltar", onClick = onBack)
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                OptionSection(
                    title = "Áudio",
                    subtitle = "Entrada, saída principal, saída secundária/CUE e monitoramento usados pelo Studio.",
                ) {
                    AudioDeviceSelector(
                        title = "Entrada de gravação",
                        selectedSignature = selectedInput,
                        choices = inputChoices,
                        onSelect = { signature ->
                            selectedInput = signature
                            routingStore.selectInput(signature)
                        },
                    )
                    AudioDeviceSelector(
                        title = "Saída principal",
                        selectedSignature = selectedOutput,
                        choices = outputChoices,
                        onSelect = { signature ->
                            selectedOutput = signature
                            routingStore.selectOutput(signature)
                            if (signature == null || signature == selectedCueOutput) {
                                selectedCueOutput = null
                                routingStore.selectCueOutput(null)
                            }
                        },
                    )
                    AudioDeviceSelector(
                        title = "Saída secundária / CUE",
                        selectedSignature = selectedCueOutput,
                        choices = if (selectedOutput == null) emptyList() else cueOutputChoices.filterNot { it.signature == selectedOutput },
                        nullLabel = "Desativada",
                        nullDetail = if (selectedOutput == null) {
                            "Selecione primeiro uma saída principal explícita."
                        } else {
                            "Selecione uma saída secundária de baixa latência. Bluetooth não é oferecido para CUE sincronizado."
                        },
                        onSelect = { signature ->
                            if (selectedOutput == null || signature == selectedOutput) {
                                selectedCueOutput = null
                                routingStore.selectCueOutput(null)
                            } else {
                                selectedCueOutput = signature
                                routingStore.selectCueOutput(signature)
                            }
                        },
                    )
                    MonitoringSelector(
                        mode = monitoringMode,
                        onSelect = { mode ->
                            monitoringMode = mode
                            routingStore.selectMonitoringMode(mode)
                        },
                    )
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.24f),
                    ) {
                        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text("Seleção e disponibilidade", style = MaterialTheme.typography.titleSmall)
                            Text("Entrada: ${routeHealth.effectiveInput?.let(::audioCapabilities) ?: if (selectedInput == null) "Automática" else "Selecionada, mas indisponível"}", style = MaterialTheme.typography.bodySmall)
                            Text("Saída principal: ${routeHealth.effectiveOutput?.let(::audioCapabilities) ?: if (selectedOutput == null) "Automática" else "Selecionada, mas indisponível"}", style = MaterialTheme.typography.bodySmall)
                            Text("Saída CUE: ${routeHealth.effectiveCueOutput?.let(::audioCapabilities) ?: if (selectedCueOutput == null) "Desativada" else "Selecionada, mas indisponível"}", style = MaterialTheme.typography.bodySmall)
                            if (routeHealth.usbDeviceDetected) Text("Dispositivo USB detectado", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                            if (!routeHealth.selectedInputAvailable) Text("A gravação será bloqueada: não haverá fallback silencioso para o microfone.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                            if (!routeHealth.selectedCueOutputAvailable || !routeHealth.cueOutputDistinctFromMain) Text("CUE bloqueado: a saída secundária precisa estar disponível e ser diferente da saída principal.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                        }
                    }
                    val storedCalibration = calibrationSampleRateHz?.let { rate ->
                        latencyStore.find(selectedInput, selectedOutput, rate)
                    }
                    val calibrationSummary = when {
                        storedCalibration == null -> "Não calibrada"
                        storedCalibration.accepted -> "Calibrada"
                        else -> "Medição instável · não aplicada"
                    }
                    SettingsActionRow(
                        title = "Calibração de latência",
                        detail = "$calibrationSummary · ${calibrationSampleRateHz?.let { "$it Hz" } ?: "taxa indisponível"} · detalhes e medição em painel dedicado",
                        actionLabel = "Abrir",
                        onClick = { calibrationDialogVisible = true },
                        testTag = "settings-open-calibration",
                    )
                    SettingsActionRow(
                        title = "Dispositivos de áudio",
                        detail = "Atualize a lista quando conectar ou remover uma interface USB.",
                        actionLabel = "Atualizar",
                        onClick = ::refreshAudioDevices,
                        testTag = "settings-refresh-audio-devices",
                    )
                }
            }

            item {
                OptionSection(
                    title = "Conta e nuvem",
                    subtitle = "Recursos em nuvem são opcionais. O Studio local continua disponível sem login.",
                ) {
                    OptionRow(
                        "Processamento em nuvem",
                        if (remoteCloudSession != null) "Autenticado" else "Login necessário",
                        remoteCloudSession?.let { "Conta autorizada: ${it.email}. A sessão é mantida pelo Firebase; a senha não é salva pelo GuitarLab." }
                            ?: "Entre com a conta pessoal do Firebase usada pelo backend de separação.",
                    )
                    SettingsActionRow(
                        title = "Conta da separação em nuvem",
                        detail = remoteCloudSession?.let { "Sessão ativa como ${it.email}. O UID estável desta conta é validado pelo backend." }
                            ?: "Use o mesmo e-mail/senha da conta pessoal criada para o processamento remoto.",
                        actionLabel = if (remoteCloudSession == null) "Entrar" else "Sair",
                        onClick = {
                            if (remoteCloudSession == null) {
                                cloudAuthMessage = null
                                cloudLoginDialogVisible = true
                            } else {
                                runCatching { remoteCloudAuth.signOut() }
                                    .onSuccess {
                                        remoteCloudSession = null
                                        cloudAuthMessage = "Sessão da separação em nuvem encerrada."
                                    }
                                    .onFailure { error ->
                                        cloudAuthMessage = error.message ?: "Não foi possível encerrar a sessão."
                                    }
                            }
                        },
                        testTag = "settings-cloud-auth",
                    )
                    cloudAuthMessage?.let {
                        Text(
                            it,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (remoteCloudSession != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.testTag("settings-cloud-auth-message"),
                        )
                    }
                    OptionRow(
                        "Backup no Google Drive",
                        if (backupSettings.driveConnected) "Conectado" else "Não conectado",
                        if (backupSettings.driveConnected)
                            "Backup incremental, histórico de versões e restauração sem sobrescrever projetos locais."
                        else
                            "Conecte somente quando quiser proteger ou recuperar projetos.",
                    )
                    SettingsActionRow(
                        title = "Gerenciar backup no Google Drive",
                        detail = "Conexão, backup automático, versões, restauração e retenção.",
                        actionLabel = "Abrir",
                        onClick = onBackupSettings,
                    )
                }
            }

            item {
                OptionSection(
                    title = "Controle externo",
                    subtitle = "MIDI/footswitch opcional para operar o Studio com as mesmas regras dos botões na tela.",
                ) {
                    SettingsActionRow(
                        title = "Controle externo",
                        detail = if (externalControlState.enabled)
                            "Ativo · MIDI/HID usa os mesmos comandos e proteções do Studio."
                        else
                            "Desativado por padrão e independente da rota de áudio.",
                        actionLabel = if (externalControlState.enabled) "Desativar" else "Ativar",
                        onClick = { ExternalControlHub.setEnabled(context, !externalControlState.enabled) },
                        testTag = "settings-external-control-toggle",
                    )
                    if (externalControlState.enabled) {
                        OptionRow(
                            "MIDI padrão",
                            if (externalControlState.midiDevices.isEmpty()) "Nenhum controlador detectado" else "${externalControlState.midiDevices.size} porta(s) disponível(is)",
                            externalControlState.midiDevices.joinToString { it.label }.ifBlank { "USB/Bluetooth MIDI aparece aqui quando o Android expõe uma porta MIDI padrão." },
                        )
                        SettingsActionRow(
                            title = "Pedal HID / teclado",
                            detail = if (externalControlState.hidEnabled)
                                "Permitido apenas para teclas explicitamente aprendidas em primeiro plano."
                            else
                                "Desativado · ative somente se o pedal aparecer como teclado/HID.",
                            actionLabel = if (externalControlState.hidEnabled) "Desativar" else "Permitir",
                            onClick = { ExternalControlHub.setHidEnabled(context, !externalControlState.hidEnabled) },
                            testTag = "settings-external-hid-toggle",
                        )

                        externalControlState.learningAction?.let { learning ->
                            Text(
                                "Aprendendo ${externalControlActionLabel(learning)}: pressione o controle desejado…",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            TextButton(onClick = ExternalControlHub::cancelLearn) { Text("Cancelar aprendizado") }
                        } ?: externalControlState.lastLearnedLabel?.let { label ->
                            Text("Último controle aprendido: $label", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }

                        ExternalControlAction.entries.forEach { action ->
                            val mapping = externalControlState.mappings.firstOrNull { it.action == action }
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.42f),
                            ) {
                                BoxWithConstraints(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp)) {
                                    val wide = maxWidth >= 500.dp
                                    if (wide) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        ) {
                                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                                Text(externalControlActionLabel(action), style = MaterialTheme.typography.labelLarge)
                                                Text(mapping?.label ?: "Não mapeado", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                                                OutlinedButton(
                                                    enabled = externalControlState.learningAction == null,
                                                    onClick = { ExternalControlHub.beginLearn(context, action) },
                                                    modifier = Modifier.testTag("settings-external-learn-${action.name}"),
                                                ) { Text("Aprender") }
                                                if (mapping != null) TextButton(onClick = { ExternalControlHub.clearMapping(context, action) }) { Text("Limpar") }
                                            }
                                        }
                                    } else {
                                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Text(externalControlActionLabel(action), style = MaterialTheme.typography.labelLarge)
                                            Text(mapping?.label ?: "Não mapeado", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                OutlinedButton(
                                                    enabled = externalControlState.learningAction == null,
                                                    onClick = { ExternalControlHub.beginLearn(context, action) },
                                                    modifier = Modifier.testTag("settings-external-learn-${action.name}"),
                                                ) { Text("Aprender") }
                                                if (mapping != null) TextButton(onClick = { ExternalControlHub.clearMapping(context, action) }) { Text("Limpar") }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item {
                OptionSection(
                    title = "Diagnóstico",
                    subtitle = "Ferramentas técnicas reunidas em um único lugar para suporte e solução de problemas.",
                ) {
                    SettingsActionRow(
                        title = "Visão geral e pacote",
                        detail = "Projeto, operações, rota, backup, journal sanitizado e exportação ZIP sem áudio.",
                        actionLabel = "Abrir",
                        onClick = onDiagnostics,
                        testTag = "settings-diagnostics",
                        primary = true,
                    )
                    SettingsActionRow(
                        title = "Áudio e dispositivos",
                        detail = "Rotas, capacidades e estado efetivo da entrada/saída.",
                        actionLabel = "Abrir",
                        onClick = onAudioDiagnostics,
                    )
                    SettingsActionRow(
                        title = "Codecs e arquivos",
                        detail = "Suporte de mídia, codecs e leitura dos arquivos do projeto.",
                        actionLabel = "Abrir",
                        onClick = onCodecDiagnostics,
                    )
                }
            }
        }
        }
    }

    CloudSeparationLoginDialog(
        visible = cloudLoginDialogVisible,
        authClient = remoteCloudAuth,
        initialEmail = remoteCloudSession?.email.orEmpty(),
        onDismiss = {
            cloudLoginDialogVisible = false
        },
        onAuthenticated = { session ->
            remoteCloudSession = session
            cloudAuthMessage = "Conta autenticada e autorizada para separação em nuvem."
            cloudLoginDialogVisible = false
        },
        testTagPrefix = "settings-cloud-auth",
    )

    if (calibrationDialogVisible) {
        val selectedInputLabel = inputChoices.firstOrNull { it.signature == selectedInput }?.label ?: "Não selecionada"
        val selectedOutputLabel = outputChoices.firstOrNull { it.signature == selectedOutput }?.label ?: "Não selecionada"
        val storedCalibration = calibrationSampleRateHz?.let { rate -> latencyStore.find(selectedInput, selectedOutput, rate) }
        val acceptedCalibration = storedCalibration?.takeIf { it.accepted }
        val fineMs = calibrationSampleRateHz?.let { rate -> LatencyFineAdjustmentPolicy.framesToMilliseconds(fineAdjustmentFrames, rate) } ?: 0.0

        AlertDialog(
            modifier = Modifier.widthIn(max = 640.dp).testTag("settings-calibration-dialog"),
            onDismissRequest = { if (!latencyBusy) calibrationDialogVisible = false },
            title = { Text("Calibração de latência") },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 520.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(9.dp),
                ) {
                    Text(
                        "A calibração é um refinamento opcional por entrada + saída + taxa. O REC continua disponível sem ela usando a sincronização base segura.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    OptionRow("Entrada da calibração", selectedInputLabel, "Selecione uma rota física explícita na página principal antes de medir.")
                    OptionRow("Saída da calibração", selectedOutputLabel, "A combinação entrada + saída + taxa forma uma chave independente.")
                    OptionRow(
                        "Taxa da sessão",
                        calibrationSampleRateHz?.let { "$it Hz" } ?: "Indisponível",
                        calibrationRateStatus ?: "Usa exatamente a taxa do domínio de edição/gravação deste projeto.",
                    )
                    OptionRow(
                        "Status",
                        when {
                            storedCalibration == null -> "Não calibrada"
                            storedCalibration.accepted -> "Válida"
                            else -> "Instável · não aplicada"
                        },
                        "Somente medições estáveis participam da compensação automática.",
                    )
                    storedCalibration?.let { calibration ->
                        val latencyMs = calibration.latencyFrames * 1000.0 / calibration.sampleRateHz
                        val jitterMs = calibration.jitterFrames * 1000.0 / calibration.sampleRateHz
                        OptionRow(
                            "Latência medida (mediana)",
                            String.format(java.util.Locale.US, "%.2f ms · %d frames", latencyMs, calibration.latencyFrames),
                            "${calibration.attempts} tentativas na taxa de ${calibration.sampleRateHz} Hz.",
                        )
                        OptionRow(
                            "Estabilidade",
                            String.format(java.util.Locale.US, "jitter %.2f ms · drift %+.0f ppm", jitterMs, calibration.driftPpm),
                            String.format(java.util.Locale.US, "Confiança %.0f%%", calibration.confidence * 100f),
                        )
                    }
                    OptionRow(
                        "Compensação automática",
                        acceptedCalibration?.describe() ?: "Sem compensação de rota aplicada",
                        "O alinhamento base permanece ativo; a medição round-trip só acrescenta compensação quando aceita.",
                    )
                    OptionRow(
                        "Ajuste global para novas takes",
                        String.format(java.util.Locale.US, "%+.1f ms", fineMs),
                        "Positivo antecipa; negativo atrasa. É isolado por entrada, saída e taxa e nunca reposiciona takes já gravadas.",
                    )
                    Surface(
                        modifier = Modifier.fillMaxWidth().testTag("settings-calibration-fine-adjustment"),
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.22f),
                    ) {
                        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                            Text(
                                "Antecipar",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                OutlinedButton(enabled = !latencyBusy && calibrationSampleRateHz != null, onClick = { adjustFineLatency(-25.0) }, modifier = Modifier.weight(1f).heightIn(min = 48.dp)) { Text("−25 ms") }
                                OutlinedButton(enabled = !latencyBusy && calibrationSampleRateHz != null, onClick = { adjustFineLatency(-5.0) }, modifier = Modifier.weight(1f).heightIn(min = 48.dp)) { Text("−5 ms") }
                                OutlinedButton(enabled = !latencyBusy && calibrationSampleRateHz != null, onClick = { adjustFineLatency(-1.0) }, modifier = Modifier.weight(1f).heightIn(min = 48.dp)) { Text("−1 ms") }
                            }
                            Text(
                                "Atrasar",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                OutlinedButton(enabled = !latencyBusy && calibrationSampleRateHz != null, onClick = { adjustFineLatency(1.0) }, modifier = Modifier.weight(1f).heightIn(min = 48.dp)) { Text("+1 ms") }
                                OutlinedButton(enabled = !latencyBusy && calibrationSampleRateHz != null, onClick = { adjustFineLatency(5.0) }, modifier = Modifier.weight(1f).heightIn(min = 48.dp)) { Text("+5 ms") }
                                OutlinedButton(enabled = !latencyBusy && calibrationSampleRateHz != null, onClick = { adjustFineLatency(25.0) }, modifier = Modifier.weight(1f).heightIn(min = 48.dp)) { Text("+25 ms") }
                            }
                            TextButton(
                                enabled = !latencyBusy && calibrationSampleRateHz != null && fineAdjustmentFrames != 0L,
                                onClick = {
                                    val rate = calibrationSampleRateHz ?: return@TextButton
                                    fineAdjustmentFrames = 0L
                                    latencyStore.saveFineAdjustmentFrames(selectedInput, selectedOutput, rate, 0L)
                                },
                                modifier = Modifier.align(Alignment.End),
                            ) { Text("Zerar ajuste") }
                        }
                    }
                    SettingsActionRow(
                        title = "Verificação digital silenciosa",
                        detail = "Usa somente PCM zero para confirmar rotas e clocks; não mede o atraso físico round-trip.",
                        actionLabel = if (digitalVerifying) "Verificando…" else "Verificar",
                        enabled = !latencyBusy && calibrationSampleRateHz != null,
                        onClick = {
                            if (context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                                runSilentDigitalVerification()
                            } else {
                                pendingDigitalVerification = true
                                latencyPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            }
                        },
                        testTag = "settings-run-digital-timing-verification",
                    )
                    SettingsActionRow(
                        title = "Calibração física round-trip",
                        detail = "Confirma as rotas em silêncio e só então usa um chirp curto com nível adaptativo baixo (3%–12%).",
                        actionLabel = if (calibrating) "Medindo…" else "Medir",
                        enabled = !latencyBusy && calibrationSampleRateHz != null,
                        primary = true,
                        onClick = {
                            if (context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                                runLatencyCalibration()
                            } else {
                                pendingCalibration = true
                                latencyPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            }
                        },
                        testTag = "settings-run-calibration",
                    )
                    calibrationStatus?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                    if (calibrating) {
                        Text("Medição física ${calibrationProgress.first}/${calibrationProgress.second.coerceAtLeast(1)}…", style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = !latencyBusy,
                    onClick = { calibrationDialogVisible = false },
                ) { Text("Fechar") }
            },
        )
    }
}

private fun externalControlActionLabel(action: ExternalControlAction): String = when (action) {
    ExternalControlAction.PLAY_STOP -> "Play / Stop"
    ExternalControlAction.RECORD_TOGGLE -> "REC"
    ExternalControlAction.RETURN_TO_START -> "Retornar ao início"
    ExternalControlAction.LOOP_TOGGLE -> "Loop"
    ExternalControlAction.UNDO -> "Desfazer"
    ExternalControlAction.REDO -> "Refazer"
}

private fun audioCapabilities(choice: StudioAudioDeviceChoice): String = buildString {
    append(choice.label)
    if (choice.channelCounts.isNotEmpty()) append(" · ${choice.channelCounts.joinToString("/")} canais")
    if (choice.sampleRates.isNotEmpty()) append(" · ${choice.sampleRates.joinToString("/")} Hz")
}

@Composable
private fun MonitoringSelector(mode: MonitoringMode, onSelect: (MonitoringMode) -> Unit) {
    var menuOpen by remember { mutableStateOf(false) }
    val label = when (mode) {
        MonitoringMode.OFF -> "Desligado"
        MonitoringMode.AUTO -> "Automático"
        MonitoringMode.ON -> "Ligado"
    }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("Monitoramento de entrada", style = MaterialTheme.typography.bodyMedium)
                Text(
                    when (mode) {
                        MonitoringMode.OFF -> "Não envia a entrada de volta para a saída pelo app; nunca controla o conteúdo gravado."
                        MonitoringMode.AUTO -> "Evita retorno duplicado em interfaces USB; nunca mistura backing no arquivo gravado."
                        MonitoringMode.ON -> "Força o retorno da entrada pela saída, sem misturar backing no arquivo gravado."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Box(Modifier.padding(start = 12.dp)) {
                OutlinedButton(onClick = { menuOpen = true }) { Text(label) }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    MonitoringMode.entries.forEach { candidate ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    when (candidate) {
                                        MonitoringMode.OFF -> "Desligado"
                                        MonitoringMode.AUTO -> "Automático"
                                        MonitoringMode.ON -> "Ligado"
                                    }
                                )
                            },
                            onClick = {
                                menuOpen = false
                                onSelect(candidate)
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AudioDeviceSelector(
    title: String,
    selectedSignature: String?,
    choices: List<StudioAudioDeviceChoice>,
    nullLabel: String = "Automático",
    nullDetail: String = "O Android escolhe a rota ativa",
    onSelect: (String?) -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    val selectedLabel = choices.firstOrNull { it.signature == selectedSignature }?.label ?: nullLabel
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, style = MaterialTheme.typography.bodyMedium)
                Text(
                    if (selectedSignature == null) nullDetail else "Dispositivo preferido",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Box(Modifier.padding(start = 12.dp)) {
                OutlinedButton(onClick = { menuOpen = true }) { Text(selectedLabel, maxLines = 1) }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(text = { Text(nullLabel) }, onClick = { menuOpen = false; onSelect(null) })
                    choices.forEach { choice ->
                        DropdownMenuItem(text = { Text(choice.label) }, onClick = { menuOpen = false; onSelect(choice.signature) })
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsActionRow(
    title: String,
    detail: String,
    actionLabel: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    primary: Boolean = false,
    testTag: String? = null,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.42f),
    ) {
        BoxWithConstraints(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp)) {
            val wide = maxWidth >= 500.dp
            val buttonModifier = (if (testTag != null) Modifier.testTag(testTag) else Modifier).heightIn(min = 48.dp)
            if (wide) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(title, style = MaterialTheme.typography.bodyMedium)
                        Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (primary) {
                        Button(onClick = onClick, enabled = enabled, modifier = buttonModifier) { Text(actionLabel) }
                    } else {
                        OutlinedButton(onClick = onClick, enabled = enabled, modifier = buttonModifier) { Text(actionLabel) }
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(title, style = MaterialTheme.typography.bodyMedium)
                    Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    val narrowModifier = buttonModifier.fillMaxWidth()
                    if (primary) {
                        Button(onClick = onClick, enabled = enabled, modifier = narrowModifier) { Text(actionLabel) }
                    } else {
                        OutlinedButton(onClick = onClick, enabled = enabled, modifier = narrowModifier) { Text(actionLabel) }
                    }
                }
            }
        }
    }
}

@Composable
private fun OptionSection(
    title: String,
    subtitle: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    ProductSectionCard(
        title = title,
        subtitle = subtitle,
        content = content,
    )
}

@Composable
private fun OptionRow(title: String, value: String, detail: String) {
    BoxWithConstraints(Modifier.fillMaxWidth().padding(horizontal = 2.dp, vertical = 4.dp)) {
        if (maxWidth >= 500.dp) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(title, style = MaterialTheme.typography.bodyMedium)
                    Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(
                    value,
                    modifier = Modifier.padding(start = 12.dp),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.secondary,
                )
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(title, style = MaterialTheme.typography.bodyMedium)
                Text(value, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.secondary)
                Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
