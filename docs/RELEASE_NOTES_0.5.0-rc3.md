# GuitarLab Studio 0.5.0-rc3

Updated: 2026-09-14

RC3 consolidates the final practice-workflow refinements requested after physical use of RC2. The goal is to make loop playback, section detection and punch recording behave as one coherent workflow without adding permanent UI clutter.

## Loop playback
- When Loop is enabled and the user presses Play, playback starts only inside `[loopStart, loopEnd)`.
- A playhead before the loop, exactly at the loop end, or after the loop is normalized to the loop start.
- Playback position callbacks are kept inside the active loop interval.
- The rule is intentionally Play-only. Recording may still begin before the loop start when punch pre-roll requires it.

## Section workflow
- `Detectar seções` now exposes the detected regions as a non-persistent timeline preview before acceptance.
- The preview uses the same normalized boundaries that will be persisted by `Aplicar/Aceitar`, preventing preview/application drift.
- Cancelling/discarding the preview leaves the project unchanged.
- `Limpar seções` removes all persisted sections without deleting clips, markers or changing the loop selection.
- Section labels remain in the shared timeline header; loop markers retain visual priority without adding another permanent rail.

## Recording with Loop enabled
- The dedicated persistent Punch Recording control group was removed.
- Pressing REC with Loop disabled starts the normal current-playhead recording flow.
- Pressing REC with Loop enabled opens a choice for the current recording only:
  - `Somente o loop`: transient punch recording for the current loop, preserving GuitarLab pre-roll/post-roll and latency-aware retention;
  - `Desde o início`: disables loop behavior for that recording and starts from 00:00;
  - `Cancelar`: no recording state is changed.
- Punch intent is no longer silently armed by a persisted project field. Legacy `punchRegion` remains readable for file-format compatibility only.

## Regression coverage added
- loop playback start before/inside/at-end/after the loop;
- playback cursor confinement versus untouched recording position;
- non-loop restart behavior at project end;
- transient recording modes and punch validity;
- section preview/application boundary equivalence;
- clear-sections behavior.

## Validation status
Source-level implementation and regression coverage are consolidated on `main`. RC3 is not considered digitally validated until the manually dispatched canonical GitHub Actions run passes software gate, full API 36 integration/geometry gate and signed homologation assembly for the exact same `github.sha`.

The remaining target-device gate after that automated PASS is Samsung SM-X230 + M-VAVE MK-300 physical validation of real USB routing/capture, recording isolation, monitoring/latency/listening and final tablet ergonomics.
