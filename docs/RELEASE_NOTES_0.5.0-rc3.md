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

## Physical Review IV — H12–H15 + H14a — PRE-GATE
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
- labels have elevated visual priority and separate vertically when close;
- the existing independent drag handles remain the edit controls.

### H14 / H14a — Mixer overflow with viewport-independent regression
- track strips use a horizontally scrollable `LazyRow`;
- hidden strips become reachable by swipe;
- MASTER remains outside the scrolling area and fixed at the right edge;
- H14a keeps the regression as a real physical-swipe test but removes the fixed-four-swipes viewport assumption;
- the test retries within a bounded 20-swipe ceiling until the final strip intersects the scroller viewport and still requires invariant MASTER bounds;
- no `scrollToItem`, timeout inflation or assertion weakening is used.

### H15 — no redundant Studio reload on return
- returning Home/Options → the same resident Studio project avoids a repository reload and transient `loading=true` state;
- removes the observed double-render/flicker path;
- preserves resident project state and Undo history while keeping recording/transport safety normalization;
- switching to a different project still uses the normal load path.

## CI evidence after implementation
### CI #621
Run `34998393778`, source `afecde0efd4d22e58115eaedadf60eea0eb3615c`:
- complete software/unit/performance/Lint/build/provenance gate: PASS;
- API36: 21/22 PASS;
- only failure: the original H14 test assumed four swipes were sufficient on every viewport;
- signing: correctly skipped.

### CI #622
Run `35000635666`, source `5832c6800a7523a0bed0b64b9400a00c1fa876c2`:
- failed before compilation/tests in `Materialize split source` in both mandatory upstream jobs;
- shell error: `line 167: unexpected EOF while looking for matching '"'`;
- root cause: `scripts/materialize_ci_sources.sh` was physically truncated inside the H7–H10 guard;
- signing: correctly skipped.

#622 is infrastructure evidence only and does not invalidate #620 or establish a functional failure in H12–H15/H14a.

### CI #623
Run `35003025673`, source `973ecae78ee3c159e975b4b1d65a2f733aedf59a`:
- repaired source materialization: PASS;
- software/unit/performance/Lint/debug+release/provenance: PASS;
- API36: 21/22 PASS;
- only failure: H14a Mixer overflow gesture regression;
- H12/H13/H15 connected regressions: PASS;
- signing: correctly skipped.

The remaining regression was traced to H14a v1 using Compose `swipeLeft()` on the scroller centerline, which crosses horizontal slider controls. H14a v2 keeps a physical swipe but moves it to the non-slider track-header lane while retaining bounded retries, viewport intersection and fixed-MASTER assertions.

## Source-validation status after #623 H14a v2
The materializer was restored to the complete canonical chain and H14/H14a remain distinct, ordered source parts:
`H12 engine → H12 UI → H13 → H14 → H14a → H15`.

Recovery evidence:
- repaired materializer Git blob `de488110153b8a800b69b520a29860230bcb5838`;
- repaired materializer SHA-256 `612f171be1345b59e0f81e7f0e8cfbd78de0dcbc981fe4edcf22130b1b61779f`;
- `bash -n scripts/materialize_ci_sources.sh`: PASS;
- ordered Physical Review IV patch dry-run/application from exact #620 materialized source: PASS;
- `git diff --check`: PASS;
- final source/test/help blobs match the audited expected tree, including revised `MixerDockInstrumentedTest.kt` `bf81aa9414a376679634f8ddf3f0b9bbf58fde5d`;
- H14a v2 patch SHA-256 `0ecdfc2922311a3f6b0c773ece8cb4a27eb049780e8affa793bc2f496b71cef7`.

#623 provides CI PASS evidence for the repaired materializer plus JVM/performance/Lint/debug/release/provenance. The newer H14a v2 gesture-lane change remains source-validated/pre-gate until the next exact-source API36/signing run.

H12–H15/H14a remain **IMPLEMENTED / SOURCE-VALIDATED / PRE-GATE** until a new manually dispatched workflow on the final `main` SHA passes software/Lint/build, API36 full regression, tablet geometry and signed homologation.
