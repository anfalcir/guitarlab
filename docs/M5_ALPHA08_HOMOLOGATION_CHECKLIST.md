# M5 alpha08 — checklist de homologação física

Status: executar somente com o software gate verde e APK assinado produzido pelo CI a partir do mesmo commit.

## Alvo

- Samsung SM-X230, Android 16/API 36;
- Pocket Amp por USB/OTG como entrada e saída principal;
- instalação sobre a versão anterior, sem limpar dados;
- projeto com backing WAV gerenciado e exatamente uma pista de guitarra armada.

## Pré-condições e integridade

- confirmar versão, versionCode, SHA-256 do APK e impressão digital do assinante;
- abrir projetos anteriores e confirmar tracks, clips, cores, mix e mídia;
- confirmar que nenhum arquivo `.recording.part.wav` órfão aparece como clipe;
- manter a fonte externa do backing indisponível e confirmar reprodução pela cópia gerenciada.

## Fluxo principal de gravação

1. Selecionar Pocket Amp em `Opções > Áudio` para entrada e saída.
2. Armar exatamente uma pista; confirmar recusa clara com zero ou mais de uma pista armada.
3. Tocar REC e observar `5 → 4 → 3 → 2 → 1`; cancelar uma vez e confirmar que nenhum arquivo/clipe é criado.
4. Repetir e confirmar que a captura começa somente após zero.
5. Confirmar backing + captura simultâneos, monitoring conforme OFF/ON/AUTO e medidores ativos.
6. Parar pelo REC/Stop; confirmar finalização, waveform automática, nome do take e posição correta na timeline.
7. Reabrir o projeto e reproduzir o take mono nos dois canais, sem mudança de velocidade ou pitch.

## Rotas, falhas e recuperação

- negar RECORD_AUDIO: sem crash, sem arquivo vazio e com mensagem útil;
- desconectar Pocket Amp durante countdown: captura não inicia;
- desconectar antes de frames válidos: nenhum clipe é criado;
- desconectar depois de frames válidos: take parcial é finalizado e identificado;
- reconectar em STOPPED, atualizar rotas e gravar novamente;
- executar ao menos 10 ciclos REC/Stop e 5 ciclos Play/REC/Stop sem vazamento, travamento ou corrupção;
- girar o tablet durante projeto aberto; rota e projeto sobrevivem. Não girar durante captura nesta homologação.

## Timeline, Trim e histórico

- trilho superior contém somente Playhead e Loop;
- Trim abre em 35%/65% do clipe ativo, dentro da waveform;
- os dois limites exibem tempo preciso e não cruzam;
- somente a região selecionada recebe mostarda translúcida;
- nenhuma guia de Trim atravessa outras pistas;
- Aplicar/Cancelar funcionam e o hash da fonte permanece idêntico;
- Split, Duplicate e Delete funcionam sem duplicar ou alterar bytes da fonte;
- Undo/Redo restaura metadata e nunca remove/regrava mídia imutável.

## Critério de aprovação

- software gate completo verde;
- zero P0;
- zero P1 repetível;
- nenhuma queda silenciosa de rota;
- nenhum arquivo vazio ou clipe fantasma;
- take, waveform, persistência, playback e duplex confirmados no hardware real.

