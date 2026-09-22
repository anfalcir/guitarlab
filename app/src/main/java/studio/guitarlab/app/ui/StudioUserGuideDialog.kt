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
import androidx.compose.ui.unit.dp

/**
 * Novice-facing in-app guide. Keep this synchronized with user-visible Studio behavior.
 * See docs/USER_GUIDE_POLICY.md.
 */
@Composable
fun StudioUserGuideDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Guia rápido do GuitarLab") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 560.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                GuideSection(
                    "Projetos na Home",
                    "A Home mostra sua biblioteca de projetos. Use Pesquisar projetos para localizar nomes sem precisar respeitar maiúsculas, minúsculas ou acentos. O botão de filtro permite combinar template, conteúdo e sample rate; o botão de ordenação alterna entre modificação, criação e nome. Filtros ativos aparecem logo abaixo da busca e podem ser limpos sem apagar nem alterar nenhum projeto. Excluir um projeto sempre exige confirmação explícita antes de remover o projeto e seus arquivos gerenciados.",
                )
                GuideSection(
                    "Preparar a música",
                    "Preparar segue uma sequência única: Fonte, Separação, Referências de estudo e Pronto para o Studio. Depois que uma fonte é aceita, a pesquisa e a importação ficam recolhidas em Trocar fonte. A separação gera seis faixas e, quando elas são validadas, o GuitarLab cria automaticamente a base sem guitarra e a guitarra de referência; não existe uma confirmação extra no caminho normal. Se essa preparação automática falhar, as seis faixas permanecem preservadas e aparece apenas a ação Tentar novamente. Você pode sair e voltar ao projeto enquanto o processamento continua. Use Ver detalhes somente quando precisar conferir informações técnicas dos arquivos ou da operação.",
                )
                GuideSection(
                    "Pistas e funções",
                    "Cada pista pode ter uma função, como Base, Guitarra de referência E/D ou Minha guitarra E/D. Isso ajuda o GuitarLab a organizar comparação e gravação. Ao criar uma pista, o app pode sugerir funções importantes ainda livres. Você pode trocar ou remover a função em Configurar pista.",
                )
                GuideSection(
                    "Play, Stop e posição",
                    "Use Play/Stop normalmente. Durante o Play você pode arrastar o marcador azul para outro ponto e a música continua dali. Ao chegar ao fim, o Play para e volta ao início. O Stop manual mantém a posição atual. Durante uma gravação, tanto Stop quanto tocar em REC novamente finalizam o take com segurança.",
                )
                GuideSection(
                    "Loop",
                    "Ative Loop para trabalhar entre os dois marcadores verdes. O Play fica dentro desse trecho, termina no segundo marcador e volta parado ao primeiro. Criar seção do loop só fica disponível quando o Loop está ativo.",
                )
                GuideSection(
                    "Comparação",
                    "Desativado reproduz a mixagem normal, sem realce de comparação. Referência destaca as guitarras de referência; Minha destaca suas guitarras; Ambas permite ouvir as duas. As etiquetas ATIVA e OCULTA mostram claramente o que participa da comparação. Quando o Mixer está aberto, Comparação, Ajustes e Timeline formam uma única barra segmentada no topo do painel. Em Ajustes, Níveis abre a análise assistida de todas as pistas, com opção de analisar/reanalisar cada uma e aplicar sugestões individualmente ou em lote. O botão Mixer da barra superior continua alternando abrir/fechar o painel e essa escolha fica persistida.",
                )
                GuideSection(
                    "Marcadores e seções",
                    "Marcadores servem como pontos de referência. Crie uma seção a partir do Loop ou use Auto seções para receber uma prévia automática. Durante a prévia, o mesmo espaço vira Aplicar + X: aplique para salvar ou toque no X vermelho para descartar. Limpar seções não apaga seus áudios.",
                )
                GuideSection(
                    "Edição de clipes",
                    "Abra Ações do áudio para cortar, dividir, duplicar, aplicar fades ou excluir um trecho. Tocar na própria área de waveform também seleciona a pista, da mesma forma que tocar no cabeçalho lateral ou no Mixer. Um toque em Cortar abre a edição assim que o menu fecha; se a edição estiver temporariamente bloqueada, o GuitarLab informa o motivo em vez de ignorar o comando. Em Cortar, arraste separadamente os marcadores de início e fim; T1/T2 usam a mesma geometria da timeline e aparecem apenas como pequenos traços amarelos na régua de tempo, sem rótulos numéricos nem ocupar a régua de seções/playhead; depois você aplica o corte. Excluir clipe remove somente aquele trecho; Limpar toda a pista fica em Configurar pista e remove todos os clipes/takes da pista sem excluir a própria pista. Arrastar um clipe até a lixeira usa a mesma exclusão confirmada e preserva arquivos de origem ainda compartilhados.",
                )
                GuideSection(
                    "Gravação",
                    "Arme a pista que receberá a gravação; ela fica destacada em vermelho na lista e na timeline. Ao iniciar REC, uma contagem de 3 segundos aparece sobreposta no centro sem mover o layout. Com Loop desligado, REC grava a partir da posição atual. Com Loop ligado, você escolhe gravar somente o trecho marcado ou desde o início. Durante o REC, a forma de onda usa uma escala temporal uniforme para que o que já foi gravado não se comprima nem se acumule para trás. A pista em gravação mostra Peak e RMS em tempo real na timeline e alimenta os mesmos medidores no Mixer. O app mede separadamente o sincronismo de início e a compensação da rota de áudio; Stop e REC finalizam o mesmo take pela mesma rotina segura.",
                )
                GuideSection(
                    "Mixer",
                    "No mixer você controla volume, posição esquerda/direita, Mute, Solo e qual pista está armada para gravar. Quando houver mais pistas do que cabem na tela, deslize horizontalmente a área das pistas; o MASTER permanece fixo à direita. Esses controles alteram o que você ouve sem apagar o áudio original. Em Configurar pista, Nível assistido mede uma pista; o botão Níveis do bloco Ajustes abre a visão de todas as pistas. Depois de aplicar uma sugestão, uma nova análise parte do novo ganho e não repete automaticamente a mesma correção.",
                )
                GuideSection(
                    "Backup e restauração",
                    "Em Opções, abra Backup e restauração para escolher uma pasta de destino. Você pode proteger todos os projetos ou apenas um, ativar o backup automático, definir quando ele pode ser executado e limitar o histórico por idade e quantidade de versões. O GuitarLab evita cópias repetidas da mesma versão, verifica os arquivos antes de disponibilizá-los para restauração e sempre restaura como uma nova cópia local, sem sobrescrever projetos existentes.",
                )
                GuideSection(
                    "Controle externo",
                    "Em Opções > Controle externo você pode habilitar, mapear e reaprender pedais ou controladores MIDI/HID compatíveis. O recurso vem desligado por padrão. Use Aprender, escolha a ação e pressione o controle desejado. Play/Stop, REC, retornar ao início, Loop, Desfazer e Refazer usam exatamente as mesmas regras dos botões na tela; um controlador nunca muda a rota de áudio, não ignora as proteções de gravação e não comanda o Studio quando o GuitarLab está em segundo plano.",
                )
                GuideSection(
                    "Opções e saída",
                    "Use Opções para entrada/saída de áudio, monitoramento e ajustes do Studio. O botão Calibração abre um painel dedicado: o ajuste global vale somente para novas takes; uma take já gravada pode receber seu próprio ajuste persistente em Configurar pista > Editar take. A Verificação digital silenciosa usa PCM zero para confirmar rota e clocks, mas não mede a latência física. A calibração física round-trip usa um chirp curto de baixo nível somente depois que a rota efetiva é confirmada. Gravar continua permitido sem calibração quando a sincronização base está disponível. Os diagnósticos de áudio/dispositivos e de codecs/arquivos ficam reunidos na seção Diagnóstico. Interfaces USB que o Android publica como múltiplos endpoints equivalentes aparecem como uma única saída física; quando necessário, o GuitarLab confirma silenciosamente qual endpoint realmente roteia áudio antes de usá-lo. Use Exportar para criar um Projeto portátil, Arquivos para estudo ou o Mix final do Studio em um formato disponível no aparelho.",
                )
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Fechar") } },
    )
}

@Composable
private fun GuideSection(title: String, body: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.28f),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(body, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
