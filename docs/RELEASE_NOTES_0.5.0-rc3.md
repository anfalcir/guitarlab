# GuitarLab Studio 0.5.0-rc3

Updated: 2026-09-15

RC3 consolidates the final practice, transport, editing, recording and release-hardening work. CI #624 is the canonical digital homologation for the complete candidate through Physical Review IV.

## Canonical digital homologation — CI #624
Run ID `35005147318`, manual `workflow_dispatch`, exact source `7858dca021a51e0e08835e3fa3f86e6d3b657215`:
- source materialization: **PASS**;
- unit/core/audio/DSP/persistence/migration regression: **PASS**;
- reproducible performance evidence: **PASS**;
- Android Lint: **PASS**;
- debug + release assembly and unsigned provenance: **PASS**;
- API 36 full connected regression: **22/22 PASS**;
- isolated 1920×1200 tablet geometry: **PASS**;
- signed homologation: **PASS**;
- package/version/source/signer/checksum verification: **PASS**.

Canonical identity:
- package `studio.guitarlab.app`;
- version `0.5.0-rc3` / versionCode `23`;
- unsigned APK SHA-256 `6dd4fb64803e161466f465f385a728ab67bc539ebd9d13307a195461a5bfbb08`;
- signed APK SHA-256 `82da7c41591c01e304e44e57031ebac6263a2175866ecd1b438d65da0539462b`;
- certificate SHA-256 `4B82890A9812BB89E1BBEF179A48752BDA2CA8AB284C833DAA27A907F4CE5E89`;
- APK Signature Scheme v2 verified with one signer.

## Physical Review IV — H12–H15 + H14a — DIGITAL PASS

### H12 — all-track level analysis
- adds `Níveis` to the Comparação practice segment;
- dedicated modal shows every track, current gain, audible content and analysis state;
- supports `Analisar todas` plus per-track analyze/reanalyze;
- supports per-track apply and `Aplicar sugestões (N)`;
- global apply is transactional: all actionable changes are one project mutation / one Undo step;
- stale results are rejected if the track or audible clips change after analysis;
- connected regression PASS at #624.

### H13 — clearer Trim timing
- removes time bubbles that obscured waveform/Trim handles;
- T1/T2 appear on the fixed timeline time ruler as short yellow ticks with precise floating times;
- labels retain elevated visual priority and separate vertically when close;
- existing independent drag handles remain the edit controls;
- retained physical-editing regression PASS at #624.

### H14 / H14a — Mixer overflow with fixed MASTER
- track strips use a horizontally scrollable `LazyRow`;
- hidden strips are reachable by real horizontal swipe;
- MASTER remains outside the scrolling area and fixed at the right edge;
- H14a v2 uses explicit `swipe(start, end)` through non-slider track-header chrome, checks actual final-strip/viewport intersection and retains strict MASTER left/right bounds;
- no `scrollToItem`, timeout inflation or assertion weakening is used;
- `MixerDockInstrumentedTest.overflowingTracksSwipeHorizontallyWhileMasterRemainsAnchored` PASS at #624.

### H15 — resident Studio return
- returning Home/Options → the same resident Studio project avoids a repository reload and transient `loading=true` state;
- preserves resident project state and Undo history while retaining recording/transport safety normalization;
- switching to a different project still uses the normal load path;
- lifecycle regression PASS at #624.

## Diagnostic history
- CI #621: software gate PASS, API36 21/22; original H14 regression incorrectly assumed four swipes were sufficient on every viewport.
- CI #622: materializer truncation before compilation/tests; infrastructure failure only.
- CI #623: repaired materializer/software gate PASS, API36 21/22; H14a v1 used a centerline swipe crossing horizontal child sliders.
- H14a v2 moved the same physical gesture to non-slider track-header chrome without weakening reachability or MASTER invariants.
- CI #624: complete canonical PASS, including API36 22/22, isolated tablet geometry and signed homologation.

## Physical homologation boundary
The exact #624 signed APK is the physical-homologation candidate. Its embedded build identity intentionally records `gate=software+android-integration-passed;physical-validation-pending`.

No additional CI run is required unless source/product code changes. Final approval still requires the real-device checklist in `RC3_FINAL_PHYSICAL_HOMOLOGATION.md`, focused on MK300 routing/REC/meters/synchronization, interaction feel, visual readability, listening/perception and absence of perceptible Studio-return flicker.
