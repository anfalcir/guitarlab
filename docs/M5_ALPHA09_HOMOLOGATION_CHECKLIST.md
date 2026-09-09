# M5 alpha09 — checklist de homologação física

## Identidade

- [ ] Confirmar `0.2.0-alpha09`, versionCode 10, SHA-256 e certificado do APK.
- [ ] Instalar sobre a versão anterior e também em instalação limpa.

## Regressão funcional M5

- [ ] Permissão de microfone concedida/negada; Arm único; countdown cancelável de 5 s.
- [ ] Captura, Stop, take gerenciado, waveform, persistência e duplex com backing.
- [ ] Desconexão de rota preserva apenas take parcial válido e não corrompe o projeto.

## UX consolidada

- [ ] REC/Arm é símbolo vermelho: discreto desligado, vivo armado.
- [ ] Pan parte do centro e preenche corretamente para E/D; Volume/Pan e valores são legíveis sem linha desperdiçada.
- [ ] Mixer tem altura confortável e Gain/Pan/Master funcionam durante Play e REC.
- [ ] Undo/Redo ficam depois de Loop e suas mensagens aparecem como popup temporário sem mover a timeline.
- [ ] Pista vazia mostra `+`; pista populada mostra três pontos e as quatro ações corretas.
- [ ] Waveform não contém título nem botões e ocupa a altura útil da faixa.
- [ ] Engrenagem exibe dados do áudio fonte: nome, formato, sample rate, canais e bit depth disponíveis.
- [ ] Long press + drag na sidebar reordena pista e conteúdo; Undo restaura a ordem.
- [ ] Long press + drag na waveform move o clipe para outra pista, sem duplicar nem alterar a fonte; Undo restaura.
- [ ] Rotação paisagem/retrato mantém projeto, tela, seleção e estado fixado do mixer.
- [ ] Playhead cruza todas as pistas; Loop cruza todas as pistas somente ativo; handles não se sobrepõem.
- [ ] Primeira ativação do Loop começa em 0–10%.
- [ ] Trim nasce em 35–65%, aparece somente na waveform ativa, mantém Apply/Cancel e clamp seguro.

## Gate

- [ ] Zero P0.
- [ ] Zero P1 reproduzível.
- [ ] Evidências registradas com Samsung SM-X230 e Pocket Amp, além do microfone interno.
