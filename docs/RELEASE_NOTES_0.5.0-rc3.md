# GuitarLab Studio 0.5.0-rc3

Updated: 2026-09-14

RC3 consolidates the final practice-workflow and transport refinements requested after physical use of RC2. The goal is to make loop playback, section detection, punch recording and navigation behave as one coherent workflow without adding permanent UI clutter.

## Loop playback and completion
- When Loop is enabled and the user presses Play, playback starts only inside `[loopStart, loopEnd)`.
- A playhead before the loop, exactly at the loop end, or after the loop is normalized to the loop start.
- Explicit user Play treats the active loop as one bounded pass: `L▶` is the logical end of that Play pass.
- When a looped Play reaches `L▶` naturally, playback stops and the playhead returns to `L◀`.
- When ordinary non-loop Play reaches project end naturally, playback stops and the playhead returns to 00:00.
- These completion rules are intentionally user-Play-only. Recording/backing playback keeps repeating-loop semantics where required by punch capture.

## Live playhead seek
- The playhead may be dragged while ordinary Play is running; audio seeks inside the active playback session and continues from the selected position without requiring Stop → reposition → Play.
- Repeated drag updates are coalesced by the audio thread so the most recent seek wins without recreating the playback session for every pointer movement.
- With Loop active, live seek is clamped inside `[L◀, L▶)`.
- Countdown, active recording and recording finalization reject playhead movement. Loop markers and structural timeline edits remain stopped-only.

## Section workflow
- `Detectar seções` exposes detected regions as a non-persistent timeline preview before acceptance.
- The preview uses the same normalized boundaries that will be persisted by `Aplicar prévia`, preventing preview/application drift.
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
- natural playback end/reset for ordinary and looped Play;
- live-play seek inside and outside loop bounds;
- playback cursor confinement versus untouched recording position;
- transient recording modes and punch validity;
- section preview/application boundary equivalence;
- clear-sections behavior;
- Compose regression for the transient REC choice with Loop active.

## Validation status
Manual CI #596, source `1bc6652dcdbc580badb3e7aea0c416ba4d06ecce`, established that unit tests, Android Lint, debug APK assembly and Android instrumentation compilation pass. The API 36 suite executed 9 tests: 8 passed and the only failure was a timeout in the newly added REC-choice UI test while it waited for a loop toolbar node in a zero-length blank-project scenario. The test has since been rewritten to make the loop precondition deterministic through the Studio ViewModel.

The transport changes in this document and the deterministic test repair are newer than #596, so RC3 is **not** digitally validated yet. The authoritative PASS must come from one manually dispatched canonical workflow on the final `main` HEAD, with software gate, full API 36/geometry gate and signed homologation assembly all green for the same `github.sha`.

The remaining target-device gate after that automated PASS is Samsung SM-X230 + M-VAVE MK-300 physical validation of real USB routing/capture, recording isolation, monitoring/latency/listening and final tablet ergonomics.