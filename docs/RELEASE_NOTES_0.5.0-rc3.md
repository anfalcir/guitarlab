# GuitarLab Studio 0.5.0-rc3

Updated: 2026-09-14

RC3 consolidates the final practice-workflow, transport, editing and recording refinements requested through physical use. The objective is one coherent guitar-practice/recording workflow without permanent UI clutter or hidden destructive behavior.

## Loop playback and completion
- When Loop is enabled and the user presses Play, playback starts only inside `[loopStart, loopEnd)`.
- A playhead before the loop, exactly at loop end or after the loop normalizes to loop start.
- Explicit user Play treats the active loop as one bounded pass: `L▶` is the logical end.
- Natural looped completion stops and returns the playhead to `L◀`.
- Natural non-loop completion stops and returns to 00:00.
- Recording/backing playback keeps repeating-loop semantics where required by punch capture.

## Live playhead seek
- The playhead may be dragged while ordinary Play is active; audio seeks inside the current playback session without a Stop/Play cycle.
- Repeated drag updates coalesce so the most recent seek wins.
- With Loop active, live seek is clamped inside `[L◀, L▶)`.
- Countdown, recording and finalization reject playhead movement; structural timeline edits remain stopped-only.

## Studio organization and visual feedback
- Comparison and Timeline controls sit below the workspace/timeline and before the Mixer.
- `Comparação` and `Timeline` fill the available width; narrow layouts stack rather than clipping actions.
- Comparison-state badges use vivid `ATIVA`/`OCULTA` semantics.
- Armed tracks receive explicit red visual state in track header and timeline lane.
- `Detectar seções` is renamed to `Auto seções`.
- `Criar seção do loop` remains visible but is enabled only while Loop is active.

## In-app help
- Home and Studio both expose `Ajuda`; both open the same `StudioUserGuideDialog` implementation.
- The guide has been synchronized with the new trim, clip deletion and drag-to-trash workflows.

## Section workflow
- `Auto seções` exposes detected regions as a non-persistent timeline preview before acceptance.
- Suggestions near project start/end are normalized to avoid meaningless micro-sections, and rendering is clipped to the true project-end width.
- `Auto seções` owns a fixed slot; while preview exists, that same slot becomes `Aplicar` + red `X`, so neighboring controls do not shift.
- Preview and persisted application use the same normalized boundaries.
- Red `X` discards preview without mutating persisted sections.
- `Limpar seções` removes sections without touching clips, markers or loop selection.

## Recording with Loop enabled
- REC countdown is `3 → 2 → 1` in a large centered translucent overlay and does not push workspace controls.
- REC with Loop disabled uses the ordinary current-playhead path.
- REC with Loop enabled offers `Somente o loop`, `Desde o início` or `Cancelar` for that recording only.
- Persisted `punchRegion` remains readable for compatibility but does not silently arm future recording.

## Physical editing hardening
### Trim handles
- Trim no longer relies on acquiring an opaque full-card `RangeSlider` gesture.
- Start/end boundaries use independent interaction targets with deterministic frame mapping.
- Time bubbles remain passive visuals and cannot steal handle gestures.
- Trim stays non-destructive and bounded by immutable source availability.

### Clip deletion and split lineage
- A split recording may keep multiple clip segments tied to one recording-take lineage.
- Moving or deleting one child no longer unconditionally deletes the shared `RecordingTake`.
- If the canonical clip leaves, a surviving sibling is promoted deterministically; the take is removed only when no segment remains.
- `Excluir clipe` is now an explicit confirmed clip-level action, distinct from `Limpar pista` and `Excluir pista`.
- Shared managed source/proxy media is preserved while still referenced.

### Drag-to-trash and transaction hardening
- Clip drop resolution is explicit: move to track, delete through trash, or no-op.
- Dragging a clip exposes a trash target; dropping there invokes the same confirmed domain deletion as `Excluir clipe`.
- Source clip/track state is revalidated before committing a drag mutation.
- Same-origin/invalid/stale drops do not intentionally create partial project mutations.

## Recording synchronization hardening
- The reported approximately 0.5 s late guitar placement is not addressed with a magic constant.
- Recording sessions distinguish capture/backing startup skew from accepted route latency and punch/pre-roll offsets.
- Android audio timestamps/monotonic timing evidence are used where trustworthy, with bounded fallback.
- Startup skew and route compensation are combined once, preventing double correction.
- Final take placement/trim applies frame-domain compensation while respecting zero/source bounds.

## Live REC waveform hardening
- Live waveform time is based on captured frame spans, not the number/rate of `AudioRecord` callbacks.
- Each envelope point owns explicit recording-frame coverage.
- Bounded compaction preserves total covered time and maximum transient.
- UI publication is conflated/rate-bounded rather than posting one main-thread update per audio read.
- `recordingFrames` remains authoritative for live clip width; finalized waveform is still derived from committed media.

## Regression coverage added
In addition to the existing transport/sections/countdown/guide tests, RC3 now carries deterministic coverage for:
- trim-handle geometry/policy and physical interaction semantics;
- split → move/delete recording-take lineage;
- save/reopen after clip lineage operations;
- drag `Move`/`Delete`/`NoOp` intent;
- recording timing compensation policy;
- long/variable-cadence live-waveform frame coverage, bounded compaction and transient preservation;
- clip-delete confirmation and instrumented physical editing flows.

## Validation status
- CI #613: earlier complete RC3 signed baseline PASS at `db5a4208848e4b6ca2163ce715d0c5bb464cfe37`.
- CI #614: failed only while compiling a new Compose test using unavailable `assertDoesNotExist`; signing was correctly blocked.
- CI #615: complete PASS at `74bf86efbec94d249c4968c3284bf1985cd66b44`, including unit/JVM, Lint, debug/release build, full API 36 regression, isolated 1920×1200 geometry and signed homologation. Signed APK SHA-256: `d443cb33a010d2d21dad43e9ff12554d7ada4f2bb783be0c80e3ac11bfe2d6c9`.

The current source advanced **after #615** with the H0–H6 editing/recording hardening above. Therefore #615 is the latest fully green baseline, but it does not validate the current HEAD. One new manually dispatched exact-source workflow with signed homologation is required before a replacement APK is promoted.

After that automated PASS, residual physical validation is intentionally short: trim-handle ergonomics, split/move/delete/trash flow, real MK-300 route/isolation, guitar-vs-backing synchronization, a multi-minute live waveform check, listening/monitoring feel and one representative export/stress smoke.
