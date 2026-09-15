# Studio Options, Share and Mixer contract

Updated: 2026-09-14

## Options
Options owns low-frequency setup and diagnostics: input route, main output, monitoring, Studio preferences, import capability information and diagnostic tools. These controls stay out of the timeline.

Project-save and final-audio export actions are **not** Options actions. Earlier wording assigning export to Options is superseded by the Share contract below.

## Studio global actions
The right side of the Studio top bar is ordered:
**Mixer → Ajuda → Opções → Compartilhar → Home**.

`Ajuda` opens the same shared `StudioUserGuideDialog` implementation used by the Home entry point; there is no second guide implementation/copy.

## Share / Salvar e exportar
`Compartilhar` opens one modal titled `Salvar e exportar` with a clear semantic split:
- **Projeto editável** — `.guitarlab`, for portable round-trip persistence;
- **Master final** — WAV 32-bit float, FLAC lossless path and MP3 320 kbps.

The modal uses descriptive cards rather than unexplained format buttons. Project persistence is visually separated from final masters. Sample rate follows established project/render policy rather than exposing unnecessary codec knobs in the RC3 UI.

## Mixer
Mixer is a bottom dock with horizontally scrollable track strips and fixed Master. Gain/pan may preview live and are persisted on completed gestures. Pan is bipolar. Mute/Solo/REC Arm remain distinct. REC Arm is the persisted recording-target state used by the recording coordinator.

## Timeline/track UI
- track names: canonical 1–24 chars;
- `Limpar pista`: track-content action; removes clips but preserves track identity/function/color/order/mix;
- `Excluir clipe`: clip-segment action; removes only the selected clip after confirmation and preserves valid sibling take/media references;
- drag-to-trash is a direct-manipulation shortcut into the same confirmed clip-deletion domain command;
- `Excluir pista`: structural action inside `Configurar pista`; safe/disabled as required for populated tracks;
- source metadata lives in `Configurar pista`, responsive to available width;
- a populated track uses a clear edit affordance for its content/configuration path;
- track ordering remains drag-only through the workspace coordinator;
- clip migration/deletion resolves through transaction-safe drag intent (`Move`, `Delete`, `NoOp`).

## Active gate
Mixer/Options/Share behavior is an established regression surface. The active RC3 source additionally carries H0–H6 editing/recording hardening and requires one new exact-source manual CI PASS before final physical homologation. After that automated PASS, `RC3_FINAL_PHYSICAL_HOMOLOGATION.md` is the only active manual checklist; older alpha/RC checklists remain historical evidence.
