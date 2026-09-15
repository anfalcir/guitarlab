# Current State — GuitarLab Studio

Updated: 2026-09-15

## Active candidate
- Canonical repository/branch: `anfalcir/guitarlab` / `main`.
- Active version: `0.5.0-rc3`, versionCode `23`, package `studio.guitarlab.app`.
- Canonical digitally homologated application/source SHA: `7858dca021a51e0e08835e3fa3f86e6d3b657215`.
- Canonical PASS: CI #624 / run ID `35005147318`, manual `workflow_dispatch`.
- #624 signed APK SHA-256: `82da7c41591c01e304e44e57031ebac6263a2175866ecd1b438d65da0539462b`.
- #624 unsigned release SHA-256: `6dd4fb64803e161466f465f385a728ab67bc539ebd9d13307a195461a5bfbb08`.
- Locked signer SHA-256: `4B82890A9812BB89E1BBEF179A48752BDA2CA8AB284C833DAA27A907F4CE5E89`.
- Signed artifact: `GuitarLabStudio-0.5.0-rc3-homologacao`, artifact ID `10411786642`.
- `.github/workflows/android-ci.yml` remains manual-only (`workflow_dispatch`). Maintenance/documentation commits use `[skip ci]`.

## Evidence boundary
CI #624 is now the authoritative signed **DIGITAL PASS** for the complete RC3 digital gate through Physical Review IV H12–H15 + H14a v2. It supersedes #620 as the active digital homologation baseline. Physical validation remains pending and is the only authority for hardware/perception claims.

### CI #624 — canonical full PASS
CI #624 / run ID `35005147318` / exact source `7858dca021a51e0e08835e3fa3f86e6d3b657215`:
- source materialization: **PASS**;
- unit/core/audio/DSP/persistence/migration regression: **PASS**;
- reproducible performance evidence: **PASS**;
- Android Lint: **PASS**;
- debug + release assembly and unsigned provenance: **PASS**;
- API 36 connected regression: **22/22 PASS**;
- isolated 1920×1200 tablet geometry: **PASS**;
- H12 all-track levels: **PASS**;
- H13 Trim-ruler contracts: **PASS**;
- H14/H14a Mixer overflow physical-swipe regression with fixed MASTER: **PASS**;
- H15 resident same-project return/lifecycle regression: **PASS**;
- signed homologation: **PASS**;
- signed APK package/version/source/signer validation: **PASS**.

Canonical signed identity:
- package: `studio.guitarlab.app`;
- versionName: `0.5.0-rc3`;
- versionCode: `23`;
- source SHA: `7858dca021a51e0e08835e3fa3f86e6d3b657215`;
- unsigned APK SHA-256: `6dd4fb64803e161466f465f385a728ab67bc539ebd9d13307a195461a5bfbb08`;
- signed APK SHA-256: `82da7c41591c01e304e44e57031ebac6263a2175866ecd1b438d65da0539462b`;
- certificate SHA-256: `4B82890A9812BB89E1BBEF179A48752BDA2CA8AB284C833DAA27A907F4CE5E89`;
- signature verification: APK Signature Scheme v2, one signer, verified.

## Physical Review IV — H12–H15 + H14a — DIGITAL PASS

### H12 — global assisted level analysis
- `Níveis` is available from the Comparação practice segment.
- `Níveis das pistas` presents every project track, current gain, audible-clip count and per-track analysis state.
- `Analisar todas` analyzes tracks with audible material while preserving effective-gain semantics.
- Each track can be analyzed/reanalyzed and applied independently.
- `Aplicar sugestões (N)` persists all actionable selected gain deltas in one project mutation / one Undo step.
- Source/track/clip snapshot validation rejects stale analysis after project changes.
- Busy state is explicit and application is blocked while analysis is running.

### H13 — Trim timing on the fixed timeline time ruler
- Large `Início`/`Fim` bubbles were removed from over the waveform/handles.
- T1/T2 are projected onto the fixed time ruler immediately above the tracks and below the playhead/section area.
- Each marker uses a short yellow tick at the proportional absolute-time position with precise time label.
- Nearby T1/T2 labels separate vertically and retain readable z-order.
- Existing draggable Trim handles remain independently accessible.

### H14 / H14a — Mixer overflow with fixed MASTER
- Track strips are inside a horizontally scrollable `LazyRow`.
- MASTER is a separate sibling anchored at the right edge and does not move with track scrolling.
- H14a v2 uses a real physical swipe through non-slider track-header chrome, is viewport-independent, detects actual final-strip/viewport intersection and preserves exact MASTER bound assertions.
- #624 exercised and passed `MixerDockInstrumentedTest.overflowingTracksSwipeHorizontallyWhileMasterRemainsAnchored`.

### H15 — resident Studio return without double-load flash
- `StudioViewModel.load(projectId)` is idempotent when the same project is already resident in the Activity-scoped ViewModel.
- Same-project return avoids unnecessary `loading=true`, repository reload and waveform/history rebuild.
- Transport/recording safety is normalized before the early return when needed.
- Undo history and resident project identity survive same-project return.
- Different-project navigation still follows the normal load path.

## Physical Review IV source representation
Materialization order after H11b:
1. `.source-parts/H12LevelEngine.patch`
2. `.source-parts/H12LevelUi.patch`
3. `.source-parts/H13TrimRuler.patch`
4. `.source-parts/H14MixerHorizontalScroll.patch`
5. `.source-parts/H14aMixerScrollViewportRegression.patch`
6. `.source-parts/H15ResidentStudioReturn.patch`

Patch SHA-256 evidence:
- H12 engine: `39c6a42bca192b1e6a829bd52c46ddb7e546d7cad09500d850e257dec57bc37c`
- H12 UI: `db9a7e448a22f79e8be22b9b795d4a4008bd92b4bff9754ad640eb957895308d`
- H13: `4fd47e9d55de072be9ccfbc64361f3e87f9e38d68aa57a76a5084085bce399b0`
- H14: `af3bf0808a5724f8aab3f6f17dec15b041591871ae26e51283d85a03a28a3e43`
- H14a v2: `0ecdfc2922311a3f6b0c773ece8cb4a27eb049780e8affa793bc2f496b71cef7`
- H15: `79e4a98f996f86cb2c0916f1c152ef8a34529e369f7db1622ea4a5a7f427a1c9`

Materializer/source validation retained:
- repaired materializer Git blob `de488110153b8a800b69b520a29860230bcb5838`;
- repaired materializer SHA-256 `612f171be1345b59e0f81e7f0e8cfbd78de0dcbc981fe4edcf22130b1b61779f`;
- `bash -n scripts/materialize_ci_sources.sh`: PASS;
- ordered H12→H15 materialization and `git diff --check`: PASS;
- final `MixerDockInstrumentedTest.kt` blob `bf81aa9414a376679634f8ddf3f0b9bbf58fde5d`.

## Diagnostic history retained
- #621: software gate PASS; API36 21/22; original fixed-four-swipes H14 regression failed.
- #622: infrastructure-only materializer truncation before compilation/testing.
- #623: repaired materializer and software gate PASS; API36 21/22; H14a centerline gesture crossed child sliders.
- H14a v2 moved the real swipe to non-slider header chrome without weakening reachability or MASTER invariants.
- #624: full canonical PASS and signed homologation.

## Milestone state
- M2–M6: PASS/CLOSED.
- M7: complete digital gate through H15/H14a is **DIGITAL PASS** at #624; final real-device physical closure remains pending.
- M8: RC3 digital hardening through H15/H14a is **DIGITAL PASS** at #624; final release decision still requires the intended physical homologation.

## Residual physical gate
No further CI rerun is required unless source/product code changes. Install the exact #624 signed APK and execute `RC3_FINAL_PHYSICAL_HOMOLOGATION.md`.

Physical checks should focus only on facts automation cannot establish: modal ergonomics, Trim marker readability over a real timeline/playhead, natural Mixer swipe feel with MASTER fixed, absence of visible Studio-return flash, MK300 routing/REC/meters/synchronization and listening/perception smoke.

The signed artifact itself records `gate=software+android-integration-passed;physical-validation-pending`, which remains the correct boundary until explicit physical approval.
