# M5.C — checkpoint de integração para homologação

Este checkpoint conecta a fundação M5.A/M5.B ao Studio real.

Implementado:

- permissão RECORD_AUDIO no fluxo criativo;
- countdown obrigatório, visível e cancelável de cinco segundos;
- revalidação no zero de projeto, pista armada, permissão e entrada selecionada;
- política de uma entrada global para exatamente uma pista armada por take;
- captura Android em WAV float 32-bit, mono/estéreo, com Peak/RMS e CLIP;
- transação temporária, validação, promoção atômica a `media/source/` e rollback seguro;
- preservação de take parcial somente quando há frames válidos;
- inserção automática de AudioClip e geração de waveform;
- backing reproduzido em paralelo quando compatível com a taxa do take;
- Stop seguro encerra playback e captura;
- Trim 35/65 dentro da waveform ativa, com tempos precisos e sem linhas globais;
- Undo/Redo e ações Split/Duplicate/Delete operam apenas em metadata.

Limite consciente: compensação fina de latência de round-trip permanece M6. Isso não impede a homologação funcional M5.

O suporte físico somente poderá ser declarado após o checklist `M5_ALPHA08_HOMOLOGATION_CHECKLIST.md` passar no Samsung SM-X230 + Pocket Amp com o APK assinado do mesmo HEAD.

