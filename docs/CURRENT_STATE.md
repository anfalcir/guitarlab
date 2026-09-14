# Current State — GuitarLab Studio

Updated: 2026-09-14

## Active candidate
- Canonical repository/branch: `anfalcir/guitarlab` / `main`.
- Active candidate: `0.5.0-rc3`, versionCode `23`.
- Signed APK name after a successful canonical workflow: `GuitarLabStudio-0.5.0-rc3-homologacao.apk`.
- Locked homologation signer SHA-256: `4B82890A9812BB89E1BBEF179A48752BDA2CA8AB284C833DAA27A907F4CE5E89`.
- Ordinary commits remain `[skip ci]`; `.github/workflows/android-ci.yml` is manual-only.
- The exact promoted source SHA/APK SHA-256 are recorded only after the manually dispatched workflow succeeds for the final `main` HEAD.

## Milestone state
- M2 through M6: PASS/CLOSED.
- M7/M8 digital baseline: established and repeatedly green; residual release closure still depends on the exact active RC plus final Samsung SM-X230 + M-VAVE MK-300 confirmation.
- `0.5.0-rc1`: recording/practice hardening.
- `0.5.0-rc2`: timeline/practice visual integration refinement.
- `0.5.0-rc3`: active release candidate. CI #615 is the latest fully green signed baseline; source then advanced through the physical editing/recording hardening program H0–H6 and therefore requires one new exact-source canonical run before a replacement APK is promoted.

## Latest proven digital baseline — CI #615
Manual CI #615 completed fully green against source `74bf86efbec94d249c4968c3284bf1985cd66b44`.

Evidence:
- unit/JVM regression PASS;
- performance evidence PASS;
- Android Lint PASS;
- debug/release assembly PASS;
- full API 36 instrumented regression PASS;
- isolated 1920×1200 target-geometry PASS;
- signed homologation PASS;
- package `studio.guitarlab.app`, version `0.5.0-rc3` / code `23` verified;
- APK Signature Scheme v2 verified;
- official certificate fingerprint PASS;
- signed APK SHA-256 `d443cb33a010d2d21dad43e9ff12554d7ada4f2bb783be0c80e3ac11bfe2d6c9`.

#615 is a baseline only. The current `main` contains newer H0–H6 hardening and must not inherit #615's PASS claim.

## RC3 retained scope
### Transport, sections and layout
- Loop Play begins only inside `[loopStart, loopEnd)` and ends naturally at `L▶`, returning to `L◀`.
- Ordinary natural completion returns to project start; manual Stop preserves the current position.
- Playhead can be sought while ordinary Play is active; active Loop clamps seek inside its bounds.
- `Auto seções` produces a non-persistent preview, normalizes edge-adjacent boundaries and never renders beyond the real song end.
- `Auto seções` owns a stable slot; preview replaces that slot with `Aplicar` + red `X` without moving neighboring controls.
- Comparação/Timeline use the available width responsively; `Limpar seções` remains in normal Timeline flow.
- REC countdown is a large centered translucent `3 → 2 → 1` overlay and never changes workspace geometry.
- Home and Studio use the same `StudioUserGuideDialog` implementation.

### Loop-aware recording
- REC with Loop disabled uses ordinary current-playhead recording.
- REC with Loop enabled offers `Somente o loop`, `Desde o início` or `Cancelar`.
- Punch intent is transient for that recording; legacy persisted `punchRegion` remains readable only for file compatibility.

## Physical editing/recording hardening — H0–H6
The physical findings after RC3 were treated as one end-to-end workflow review rather than isolated fixes. `PHYSICAL_EDITING_RECORDING_HARDENING_PLAN.md` is the implementation record.

### H0 — reproduction contracts and invariants — IMPLEMENTED / PRE-GATE
Regression coverage was expanded around trim interaction, split/take lineage, clip move/delete persistence, recording timing and long-running live waveform behavior.

### H1 — trim interaction — IMPLEMENTED / PRE-GATE
- opaque full-card `RangeSlider` interaction was replaced by independent start/end trim handles;
- each handle has a dedicated minimum touch target, semantics/test identity and deterministic frame mapping;
- trim bubbles are passive visuals and do not own the handle gesture;
- source/timeline bounds remain non-destructive and Undo/Redo-compatible.

### H2 — clip lifecycle and deletion — IMPLEMENTED / PRE-GATE
- split siblings may share one recording-take lineage safely;
- moving/deleting one segment no longer blindly destroys the shared `RecordingTake`;
- if the canonical take clip leaves, a surviving sibling is promoted deterministically;
- the take is removed only when no sibling remains;
- `Excluir clipe` is exposed as an explicit confirmed action;
- shared source/proxy media ownership is preserved.

### H3 — drag/drop transaction hardening — IMPLEMENTED / PRE-GATE
- drop resolution is explicit: `Move`, `Delete` or `NoOp`;
- clip identity/source state is revalidated before commit;
- dragging a clip exposes a trash target; dropping there enters the same confirmation path as `Excluir clipe`;
- invalid/stale drop paths are designed to fail without partially mutating project state.

### H4 — recording synchronization and latency — IMPLEMENTED / PRE-GATE
- the observed delay is not compensated by a hard-coded `-0.5 s` constant;
- session startup skew is measured separately from route calibration/punch offsets;
- recording/playback engines expose monotonic/timestamp evidence where available, with bounded fallback;
- startup skew and accepted route latency are combined once, preventing double compensation;
- final take placement uses the resulting frame compensation while respecting timeline/source bounds.

### H5 — live REC waveform — IMPLEMENTED / PRE-GATE
- live waveform timebase is recording frames rather than callback count;
- each envelope point owns an explicit frame span;
- compaction preserves covered time and maximum transient;
- UI publication is conflated/bounded instead of launching one main-thread update per audio read;
- recording frame count remains authoritative for live clip width.

### H6 — integrated regression/materialization/documentation — IMPLEMENTED / PRE-GATE
- persistence regression includes split → move/delete → save/reopen scenarios;
- instrumented coverage was added for physical editing flows;
- timing and live-waveform deterministic regressions were added;
- the in-app guide was synchronized with the new trim/delete/drag behavior;
- all H1–H6 deltas are versioned under `.source-parts` and applied serially by `scripts/materialize_ci_sources.sh`;
- a final H6 test-assertion defect found during review was corrected in a separate patch and is now part of the materialization chain;
- source materialization remains fail-fast and idempotence-oriented: patch drift must fail instead of silently producing a partial candidate.

The above H0–H6 status means **implemented and locally/source-reviewed**, not canonical CI PASS. Only the next exact-source workflow may promote it to digitally homologated.

## Next authoritative digital gate
Run `.github/workflows/android-ci.yml` manually with signed homologation enabled against the final current `main` HEAD. That exact run must pass:
1. JVM/unit regression, including the new editing/timing/waveform suites;
2. Android Lint;
3. debug/release assembly;
4. Android test compilation;
5. API 36 full instrumented regression;
6. isolated 1920×1200 target-geometry regression;
7. signed release packaging;
8. package/version/source-provenance validation;
9. locked signer verification and APK SHA-256 publication.

Do not reuse #615's APK to validate H0–H6 because that APK predates the hardening.

## Residual physical gate after the new automated PASS
Only facts that require the real Samsung SM-X230 + M-VAVE MK-300 remain. The focused residual checks are:
- grab/move both trim handles reliably on the tablet;
- split a recorded clip, move one child to an empty compatible track, delete a child through both `Excluir clipe` and drag-to-trash, then save/reopen/Undo/Redo;
- confirm the selected MK-300 input remains fail-closed and backing is not recorded into the guitar take;
- record guitar against backing and confirm synchronization no longer exhibits the previously observed ~0.5 s late placement;
- record a multi-minute take and confirm the live waveform grows monotonically without accelerating/piling backward;
- listen for pops/dropouts/monitoring feel and perform one representative export/stress smoke.

`RC3_FINAL_PHYSICAL_HOMOLOGATION.md` is the only active manual checklist after the automated PASS. Explicit approval of the exact signed artifact is required before final closure.
