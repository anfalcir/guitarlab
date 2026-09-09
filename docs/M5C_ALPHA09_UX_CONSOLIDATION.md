# M5.C alpha09 — consolidação de UX do Studio

Status: implementação de software concluída; homologação física pendente.

Este checkpoint corrige a divergência observada no alpha08 entre a especificação de UX e a tela efetivamente entregue.

## Contrato implementado

- Arm usa o símbolo de gravação, vermelho discreto quando desligado e vivo quando armado.
- Pan é bipolar, parte do centro e preenche apenas na direção E/D.
- Volume, Pan e respectivos valores ficam integrados ao controle; o mixer possui 294 dp de altura e permanece ajustável durante Play/REC.
- Undo e Redo ficam imediatamente após Loop no transporte.
- Mensagens transitórias usam Snackbar sobreposto, com duração curta e sem alterar a geometria da timeline.
- A waveform não exibe nome nem botões; utiliza toda a altura útil do clipe.
- Sidebar vazia exibe `+`; sidebar populada exibe menu de contexto com Duplicar, Dividir, Cortar e Excluir.
- Configurações da pista exibem nome, cor, ordem e metadados do áudio fonte.
- Pressionar e arrastar na sidebar reordena a pista completa; pressionar e arrastar a waveform move o clipe para outra pista sem copiar a mídia.
- A rota Studio é preservada por `rememberSaveable`, inclusive em mudança de orientação.
- Playhead atravessa todas as pistas; Loop atravessa todas as pistas quando ativo; Trim permanece restrito à waveform do clipe ativo.
- Handles do Playhead e do Loop ocupam faixas verticais distintas; a primeira ativação do Loop inicia em 0–10% do projeto.

## Restrições preservadas

- Edição de clipe é somente metadata e não destrói a mídia gerenciada.
- Reordenação preserva IDs e relações clipe/pista.
- Drag é desabilitado durante importação, Trim e estados incompatíveis do transporte.
- A M5 só pode ser declarada homologada depois do gate CI assinado e da checklist física alpha09.
