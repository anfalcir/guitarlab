# M5 alpha10 — checkpoint corretivo pós-homologação

Alpha09 foi aprovado fisicamente, com quatro ajustes de UX aceitos para o candidato corretivo alpha10.

## Ajustes

- Os tempos de Trim agora usam bubbles flutuantes opacos, com borda mostarda, rótulos Início/Fim e posicionamento junto ao respectivo marcador. Quando os limites se aproximam, os bubbles ocupam alturas diferentes para não colidir.
- Configurar pista não contém mais controles de ordem. A ação textual `Excluir pista` é distinta de `Limpar pista` e só fica habilitada quando a pista está vazia.
- `Limpar pista`, no menu de três pontos, remove o conteúdo sem remover a pista.
- Drag de pista usa uma ghost layer acima da lista, encaixada na posição de destino, com indicação `Solte na posição N` e autoscroll nas bordas superior/inferior quando houver conteúdo rolável.
- Drag de clipe usa ghost layer acima das pistas, com waveform e indicação da pista-alvo; o conteúdo não fica mais encoberto pelas lanes paralelas.

## Gate

O alpha10 exige testes unitários, Android Lint, APK debug, APK release assinado, validação do certificado e conferência SHA-256. A aprovação física deve repetir somente Trim, limpar/excluir pista e os dois fluxos de drag, além de regressão curta de Undo/Redo.
