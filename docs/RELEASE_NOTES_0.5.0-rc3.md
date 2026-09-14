# GuitarLab Studio 0.5.0-rc3

Updated: 2026-09-14

RC3 consolidates the final practice-workflow, transport and Studio-clarity refinements requested after physical use of RC2. The goal is to make loop playback, section detection, recording, comparison, track functions and navigation behave as one coherent workflow without adding permanent UI clutter.

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

## Studio organization and visual feedback
- Comparison and Timeline controls move below the workspace/timeline, immediately before the Mixer, keeping editing context above its commands.
- Comparison-state badges use high-visibility neon semantics: green for `ATIVA`, magenta for `OCULTA`.
- Armed tracks receive an explicit red visual state both in the track header and in the corresponding timeline lane, in addition to the Mixer REC indicator.
- `Detectar seções` is renamed to `Auto seções`.
- `Criar seção do loop` remains visible but disabled while Loop is off and becomes clickable only while Loop is active.

## Track functions
- New tracks may immediately offer important workflow functions that are still free, such as Base, Guitarra de referência E/D and Minha guitarra E/D.
- The new-track prompt is optional; choosing `Agora não` keeps the track generic.
- `Configurar pista` now allows adding, changing or removing a function in the existing function area.
- Function assignment is centralized in one policy so the creation prompt and Track Settings use identical conflict rules.
- Structural workflow functions cannot be duplicated or combined incompatibly; valid L/R pairs can coexist, while reusable generic instrument functions remain repeatable.
- Legacy projects remain readable/savable even if an older version allowed a combination that the current editor no longer permits creating.

## In-app help
- Studio and Home both expose `Ajuda`; both buttons open the same shared `StudioUserGuideDialog`, avoiding duplicate help implementations/copy.
- `docs/USER_GUIDE_POLICY.md` makes guide synchronization a release requirement whenever user-visible controls, names or workflows change.

## Section workflow
- `Auto seções` exposes detected regions as a non-persistent timeline preview before acceptance.
- Suggestions too close to project start/end are suppressed to prevent meaningless micro-sections, and preview/persisted section rendering is hard-clipped to the true project-end width.
- The `Auto seções` button owns a fixed slot; while preview exists, the same slot becomes `Aplicar` + red `X`, so the rest of Timeline never shifts.
- `Comparação` and `Timeline` fill the available width; narrow layouts stack intentionally instead of truncating the last action.
- The preview uses the same normalized boundaries that will be persisted by `Aplicar`, preventing preview/application drift.
- Pressing the red `X` discards the preview and leaves the project unchanged.
- `Limpar seções` removes all persisted sections without deleting clips, markers or changing the loop selection.
- Section labels remain in the shared timeline header; loop markers retain visual priority without adding another permanent rail.

## Recording with Loop enabled
- REC countdown is now `3 → 2 → 1` in a large centered translucent overlay; it does not push the practice bar or workspace.
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
- section preview/application boundary equivalence and edge normalization;
- fixed `Auto seções` → `Aplicar` + red `X` slot geometry;
- 3-second centered countdown overlay geometry;
- shared Home/Studio guide entry behavior;
- clear-sections behavior;
- centralized track-function availability/assignment/conflict policy.

## Validation status
Manual CI **#613** fully passed at source `db5a4208848e4b6ca2163ce715d0c5bb464cfe37`: unit/JVM, Lint, debug/release build, full API 36 regression, isolated 1920×1200 geometry, signed homologation and official certificate verification. The signed APK SHA-256 is `4b62d38c1caf3f449c94b4f9111263dacd87d8cf5e9116caa26245ecb716f341`; the optimized run completed in about 3m01s.

The physical review then requested the section-rail/control-layout/Home-help/countdown refinements documented above. Therefore #613 remains the latest fully green baseline, while the **current RC3 source requires one new manually dispatched exact-source workflow** before a replacement APK is promoted.

The remaining target-device gate after that automated PASS is Samsung SM-X230 + M-VAVE MK-300 physical validation of real USB routing/capture, recording isolation, monitoring/latency/listening and final tablet ergonomics.
