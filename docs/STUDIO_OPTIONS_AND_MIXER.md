# Studio Options, Share and Mixer contract

Updated: 2026-09-14

## Options
Options owns low-frequency setup and diagnostics: input route, main output, monitoring, Studio preferences, import capability information and diagnostic tools. These controls stay out of the timeline.

Project-save and final-audio export actions are **not** Options actions. Earlier wording assigning export to Options is superseded by the Share contract below.

## Share / Salvar e exportar
The right side of the Studio top bar is ordered:
**Mixer → Opções → Compartilhar → Home**.

`Compartilhar` opens one modal titled `Salvar e exportar` with a clear semantic split:
- **Projeto editável** — `.guitarlab`, for portable round-trip persistence;
- **Master final** — WAV 32-bit float, FLAC lossless path and MP3 320 kbps.

The modal uses descriptive cards rather than unexplained format buttons. Project persistence is visually separated from final masters. Sample rate follows established project/render policy rather than exposing unnecessary codec knobs in the alpha13 UI.

## Mixer
Mixer is a bottom dock with horizontally scrollable track strips and fixed Master. Gain/pan may preview live and are persisted on completed gestures. Pan is bipolar. Mute/Solo/REC Arm remain distinct. REC Arm is the persisted recording-target state used by the M5 recording coordinator.

## Timeline/track UI
- track names: canonical 1–24 chars;
- `Limpar pista`: content action; removes clips only and preserves track identity/function/color/order/mix;
- `Excluir pista`: structural action inside `Configurar pista`; safe/disabled as required for populated tracks;
- source metadata lives in `Configurar pista`, responsive to available width;
- a populated track uses a pencil/edit affordance for its content/configuration path;
- ordering is drag-only through the workspace drag coordinator.

## Active gate
Mixer/Options/Share behavior is an established regression surface. The active residual physical gate is `RC3_FINAL_PHYSICAL_HOMOLOGATION.md`; alpha13 checklists are historical evidence only.
