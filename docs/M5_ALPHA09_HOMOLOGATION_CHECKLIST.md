# M5 alpha09 — checklist de homologação física

Status: **HISTÓRICO — alpha09 aprovado fisicamente com achados menores**.

Identidade histórica: `0.2.0-alpha09`, versionCode 10. Este checklist registra a etapa que consolidou a UX M5 e não é mais um gate ativo.

## Resultado consolidado

O alpha09 foi amplamente aprovado em validação física. Permaneceram achados de UX que motivaram o alpha10, sem invalidar a base de gravação/captura já homologada nessa etapa.

Foram validados ou mantidos como baseline de regressão:
- permissão, Arm único e countdown de 5 s;
- captura, Stop, take gerenciado, waveform, persistência e duplex com backing;
- REC/Arm simbólico, Pan bipolar e Mixer;
- Undo/Redo sem deslocar estruturalmente a timeline;
- sidebar contextual e waveform limpa;
- orientação mantendo o projeto aberto;
- Playhead/Loop globais e Trim local não destrutivo.

O alpha10 corrigiu e aprovou a apresentação do Trim e a separação entre `Limpar pista` e `Excluir pista`, porém revelou falhas remanescentes nos drags de pista/waveform e no layout de `Configurar pista`.

O único gate ativo agora é `M5_ALPHA11_FINAL_HOMOLOGATION_CHECKLIST.md`. A M5 permanece OPEN até aprovação física explícita do alpha11.