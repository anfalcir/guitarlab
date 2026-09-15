# GuitarLab Studio 0.5.0-rc3

Updated: 2026-09-15

RC3 consolidates the final practice, transport, editing, recording and release-hardening work.

## Digitally established through H11
CI #620 / run ID `34924500870` digitally homologated H0–H11 against exact source `faaeb0ee4f9e52fbdcf369d097fa773e96d104a7`.

Canonical #620 identity:
- signed APK SHA-256 `acbe61b006aa4abe8b3063faf35b4a9569ed55aaf7f1a2ca3e1726c927855b3c`;
- package `studio.guitarlab.app`;
- version `0.5.0-rc3` / code `23`;
- certificate SHA-256 `4B82890A9812BB89E1BBEF179A48752BDA2CA8AB284C833DAA27A907F4CE5E89`.

Established behavior through H11 includes loop-aware transport, Auto seções preview, REC countdown, independent Trim handles, safe split/take lineage, deletion/drag hardening, recording synchronization, level-analysis convergence, Stop-during-REC, history/race hardening, stable live waveform bucketing, the segmented Mixer practice bar, waveform track selection and live REC Peak/RMS.

## Physical Review IV — H12–H15 — PRE-GATE
Real-device review after #620 identified four final workflow/ergonomic gaps. They are implemented and source-validated but require one new exact-source CI gate before promotion.

### H12 — all-track level analysis
- adds `Níveis` to the Comparação practice segment;
- dedicated modal shows every track, current gain, audible content and analysis state;
- supports `Analisar todas` plus per-track analyze/reanalyze;
- supports per-track apply and `Aplicar sugestões (N)`;
- global apply is transactional: all actionable changes are one project mutation / one Undo step;
- stale results are rejected if the track or audible clips change after analysis.

### H13 — clearer Trim timing
- removes the time bubbles that obscured waveform/Trim handles;
- T1/T2 now appear on the fixed timeline time ruler as short yellow ticks with precise floating times;
- labels have elevated visual priority over playhead overlap and separate vertically when close;
- the existing independent drag handles remain the edit controls.

### H14 — Mixer overflow
- track strips now use a horizontally scrollable `LazyRow`;
- hidden strips become reachable by swipe;
- MASTER remains outside the scrolling area and fixed at the right edge;
- regression verifies final-track access and invariant MASTER geometry.

### H15 — no redundant Studio reload on return
- returning Home/Options → the same resident Studio project avoids a repository reload and transient `loading=true` state;
- removes the observed double-render/flicker path;
- preserves resident project state and Undo history while keeping recording/transport safety normalization.

## Source-validation status
H12–H15 were serially reapplied against the exact materialized #620 source artifact. Forward patch dry-runs/application passed, `git diff --check` passed, and all changed/new source/test files matched the independently developed final tree byte-for-byte.

This is not Android gate evidence. H12–H15 remain **IMPLEMENTED / SOURCE-VALIDATED / PRE-GATE** until a new manually dispatched workflow on the final `main` SHA passes software/Lint/build, API36 full regression, tablet geometry and signed homologation.
