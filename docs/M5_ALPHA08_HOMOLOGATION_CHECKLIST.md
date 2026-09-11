# M5 alpha08 — checklist de homologação física

Status: **HISTÓRICO / SUPERADO**.

Este arquivo preserva o gate físico originalmente definido para `0.2.0-alpha08`. Ele não é mais um checklist ativo e não deve ser usado para inferir o estado atual da M5.

## Escopo histórico

O alpha08 cobria a primeira homologação integrada de gravação M5 no Samsung SM-X230 / Android 16/API 36 com Pocket Amp USB, incluindo:
- permissão e exatamente uma pista armada;
- countdown visível/cancelável de cinco segundos;
- captura, finalização, waveform e persistência do take;
- backing + captura/monitoring;
- falhas de rota e preservação segura de take parcial válido;
- Trim não destrutivo, Split/Duplicate/Delete e Undo/Redo;
- continuidade do projeto em mudança de orientação.

## Classificação posterior

A evolução alpha09 e alpha10 substituiu este gate. O alpha10 teve Trim e a distinção `Limpar pista` / `Excluir pista` aprovados, mas os dois fluxos de drag e o layout de `Configurar pista` permaneceram reprovados.

O único gate físico ativo é `M5_ALPHA11_FINAL_HOMOLOGATION_CHECKLIST.md`. A M5 continua OPEN até aprovação física explícita do alpha11.