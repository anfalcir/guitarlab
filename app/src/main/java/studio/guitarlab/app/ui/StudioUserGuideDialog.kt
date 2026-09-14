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
                    "Pistas e funções",
                    "Cada pista pode ter uma função, como Base, Referência E/D ou Minha Guitarra E/D. Isso ajuda o GuitarLab a organizar comparação e gravação. Ao criar uma pista, o app pode sugerir funções importantes ainda livres. Você pode trocar ou remover a função em Configurar pista.",
                )
                GuideSection(
                    "Play, Stop e posição",
                    "Use Play/Stop normalmente. Durante o Play você pode arrastar o marcador azul para outro ponto e a música continua dali. Ao chegar ao fim, o Play para e volta ao início. O Stop manual mantém a posição atual.",
                )
                GuideSection(
                    "Loop",
                    "Ative Loop para trabalhar entre os dois marcadores verdes. O Play fica dentro desse trecho, termina no segundo marcador e volta parado ao primeiro. Criar seção do loop só fica disponível quando o Loop está ativo.",
                )
                GuideSection(
                    "Comparação",
                    "Mixer toca conforme sua mixagem. Referência destaca as guitarras de referência; Minha destaca suas guitarras; Ambas permite ouvir as duas. As etiquetas ATIVA e OCULTA mostram claramente o que participa da comparação.",
                )
                GuideSection(
                    "Marcadores e seções",
                    "Marcadores servem como pontos de referência. Crie uma seção a partir do Loop ou use Auto Seções para receber uma prévia automática. Você decide se aplica ou cancela a prévia. Limpar seções não apaga seus áudios.",
                )
                GuideSection(
                    "Gravação",
                    "Arme a pista que receberá a gravação; ela fica destacada em vermelho na lista e na timeline. Com Loop desligado, REC grava a partir da posição atual. Com Loop ligado, você escolhe gravar somente o trecho marcado ou desde o início.",
                )
                GuideSection(
                    "Mixer",
                    "No Mixer você controla volume, posição esquerda/direita, Mute, Solo e qual pista está armada para gravar. Esses controles alteram o que você ouve sem apagar o áudio original.",
                )
                GuideSection(
                    "Opções e saída",
                    "Use Opções para entrada/saída de áudio, monitoramento e ajustes do Studio. Use Salvar e exportar para guardar um projeto editável ou gerar o áudio final em um formato disponível no aparelho.",
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
        shape = androidx.compose.foundation.shape.RoundedCornerShape(9.dp),
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
