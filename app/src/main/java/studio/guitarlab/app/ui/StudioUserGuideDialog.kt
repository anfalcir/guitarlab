package studio.guitarlab.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

private enum class GuideArea {
    HOME,
    PREPARE,
    STUDIO,
    EXPORT,
    CLOUD,
    SETTINGS,
}

private data class GuideSectionContent(
    val area: GuideArea,
    val title: String,
    val body: String,
)

/**
 * One canonical help-content model for the unified GuitarLab product.
 * Home consumes the complete guide; dense Studio surfaces consume only the
 * contextual Studio subset so the two entry points cannot drift into
 * independent help systems.
 */
private object GuitarLabGuideContent {
    val all: List<GuideSectionContent> = listOf(
        GuideSectionContent(
            GuideArea.HOME,
            "Biblioteca e novo projeto",
            "A Home é a biblioteca do GuitarLab. Pesquise, filtre e ordene projetos sem alterar seus arquivos. Em Novo projeto, escolha Buscar música, Importar áudio ou Começar no Studio. Renomear é apenas uma mudança de nome; Duplicar cria outro projeto; Excluir sempre pede confirmação. Quando houver trabalho em segundo plano, a interface informa a atividade e evita ações destrutivas inseguras.",
        ),
        GuideSectionContent(
            GuideArea.PREPARE,
            "Preparar",
            "O fluxo é Fonte → Separação → Referências de estudo → Pronto para o Studio. Trocar a fonte é uma ação explícita. No fluxo atual, o processamento usa stems intermediários somente na nuvem e entrega ao projeto as duas referências finais: base sem guitarra e guitarra. Falhas de importação preservam o resultado remoto para retomada; mídias locais já validadas também permanecem seguras. Você pode sair da tela enquanto uma operação persistente continua e acompanhar o progresso em Atividade.",
        ),
        GuideSectionContent(
            GuideArea.STUDIO,
            "Studio · transporte e edição",
            "Preparar e Studio usam o mesmo projeto e as mesmas mídias gerenciadas: não há exportação e reimportação entre eles. Use Play/Stop, posição, Loop, marcadores, seções, Trim, Split, fades e ações de clipe sem alterar o áudio de origem. Excluir um clipe remove apenas aquele trecho; Limpar pista remove o conteúdo da pista; Excluir pista remove a pista.",
        ),
        GuideSectionContent(
            GuideArea.STUDIO,
            "Studio · gravação e Mixer",
            "Arme a pista correta antes de REC. A contagem aparece sem deslocar o layout; durante a gravação, waveform, Peak e RMS acompanham a take. A entrada selecionada precisa ser confirmada pelo Android: o GuitarLab não troca silenciosamente para o microfone do aparelho. Stop e REC finalizam pela mesma rotina segura. No Mixer, volume, pan, Mute, Solo, fone/CUE e Arm mudam a monitoração sem apagar o áudio original. Com o transporte parado, o botão de fone envia qualquer pista para a saída secundária. Durante Play/REC essa rota fica travada para manter a sessão consistente. Se MAIN e CUE não puderem ser confirmadas como rotas físicas distintas e sincronizadas, CUE fica silencioso em vez de vazar para MAIN ou atrasar a saída principal.",
        ),
        GuideSectionContent(
            GuideArea.STUDIO,
            "Studio · prática e referências",
            "Comparação e Timeline ficam na barra de transporte e abrem painéis sem ocupar uma barra abaixo das pistas. Na Comparação, alterne entre Desativado, Referência, Minha e Ambas; o indicador R, M ou 2 no botão mostra o modo ativo. Níveis fica no Master do Mixer; Timeline reúne marcadores e seções em uma lista vertical. Loop, Comparação, marcadores e seções ajudam a estudar trechos. Quando uma nova preparação produz outra base ou guitarra de referência, o Studio não troca a referência usada por uma sessão existente sem sua escolha: você pode manter a atual ou atualizar explicitamente, preservando gravações, takes e edições.",
        ),
        GuideSectionContent(
            GuideArea.STUDIO,
            "Studio · espaço e Mixer",
            "Mostrar/Ocultar Mixer preserva sua preferência e sempre abre o dock fixado no rodapé. Na barra de transporte, alterne Mixer mínimo/completo; o modo escolhido é lembrado. Os dois modos mostram Mute, Solo, fone/CUE, REC Arm e volume em canais estreitos. O completo também mostra PK, RMS, clipping e pan; no mínimo, toque no nome da pista para abrir os controles completos, ajustar pan ou limpar clipping. As pistas rolam horizontalmente; o Master tem sua própria coluna fixa e o botão Níveis.",
        ),
        GuideSectionContent(
            GuideArea.EXPORT,
            "Exportar",
            "A área Exportar é o único lugar para escolher o resultado externo. Projeto portátil salva o projeto para transporte; Arquivos para estudo publica base e referência quando disponíveis; Mix final do Studio renderiza a sessão atual. O formato só é convertido quando necessário para o resultado escolhido.",
        ),
        GuideSectionContent(
            GuideArea.CLOUD,
            "Atividade, nuvem e backup",
            "Atividade reúne operações longas como aquisição, separação, preparação, exportação, backup e restauração. O Studio continua utilizável sem login. O Google Drive é autorizado somente quando uma função de backup precisa dele. O backup usa revisões transacionais e envia apenas o conteúdo necessário; uma revisão só é confirmada depois da validação remota. Restaurar cria uma cópia local segura e conflitos oferecem ações explícitas, sem sobrescrever silenciosamente o projeto.",
        ),
        GuideSectionContent(
            GuideArea.SETTINGS,
            "Opções e diagnósticos",
            "Em Opções, configure áudio e gravação, saída principal, saída secundária/CUE, monitoramento, sample rate, sincronização/calibração, controle externo quando habilitado, Conta e nuvem e preferências do aplicativo. CUE exige uma MAIN explícita e uma segunda rota física distinta. Diagnósticos mostram detalhes técnicos apenas quando eles ajudam a investigar um problema. Calibração digital não substitui a verificação física da latência da rota USB.",
        ),
    )

    val studio: List<GuideSectionContent> = all.filter { it.area == GuideArea.STUDIO }
}

@Composable
fun GuitarLabUserGuideDialog(onDismiss: () -> Unit) {
    UserGuideDialog(
        title = "Guia do GuitarLab",
        sections = GuitarLabGuideContent.all,
        testTag = "guitarlab-user-guide",
        onDismiss = onDismiss,
    )
}

@Composable
fun StudioUserGuideDialog(onDismiss: () -> Unit) {
    UserGuideDialog(
        title = "Ajuda do Studio",
        sections = GuitarLabGuideContent.studio,
        testTag = "studio-user-guide",
        onDismiss = onDismiss,
    )
}

@Composable
private fun UserGuideDialog(
    title: String,
    sections: List<GuideSectionContent>,
    testTag: String,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        modifier = Modifier.testTag(testTag),
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 560.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                sections.forEach { section -> GuideSection(section) }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Fechar") } },
    )
}

@Composable
private fun GuideSection(section: GuideSectionContent) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.28f),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Text(section.title, style = MaterialTheme.typography.titleSmall)
            Text(
                section.body,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
