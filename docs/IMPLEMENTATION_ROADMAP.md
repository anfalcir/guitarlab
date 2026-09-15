# Implementation Roadmap

Updated: 2026-09-15

## M1 — Project/model foundation — CLOSED
Project model, templates, persistence baseline and repository structure.

## M2 — Android hardware/audio baseline — PASS/CLOSED
Samsung SM-X230 USB-audio baseline established.

## M3 — Codec/import foundation — ABSORBED
Consolidated into later milestones.

## M4 — Studio playback/edit/mix foundation — ABSORBED
Timeline, playback, editing, Mixer/Master and project interaction foundations are integrated.

## M5 — Reliable recording + Studio consolidation + Media I/O — PASS/CLOSED
Closed after explicit physical approval.

## M6 — Measured latency and synchronization — PASS/CLOSED
Closed after explicit physical approval. Later timing work hardens per-session startup behavior without reopening M6.

## M7 — Production audio polish — DIGITAL PASS / PHYSICAL CLOSURE PENDING
Managed media, SRC/editing domain, fades/crossfades, transactional recording/recovery, fail-closed input, monitoring isolation, takes, practice controls, level analysis, live REC waveform, Mixer/selection/metering refinements and project/master export are implemented.

CI #624 / run ID `35005147318` / exact source `7858dca021a51e0e08835e3fa3f86e6d3b657215` is the authoritative full digital PASS through Physical Review IV H12–H15 + H14a v2. The only remaining M7 authority is final physical homologation on real hardware/perception.

## M8 — Release hardening — DIGITAL PASS THROUGH H15/H14a

### Canonical gate — CI #624
Manual `workflow_dispatch`, exact source `7858dca021a51e0e08835e3fa3f86e6d3b657215`:
- source materialization: PASS;
- complete JVM/unit/core/audio/DSP/persistence/migration regression: PASS;
- performance evidence: PASS;
- Android Lint: PASS;
- debug + release build and unsigned provenance: PASS;
- API36 full connected regression: **22/22 PASS**;
- isolated 1920×1200 tablet geometry: PASS;
- signed homologation: PASS;
- package/version/source/signer/checksum provenance: PASS.

Canonical candidate identity:
- version `0.5.0-rc3` / versionCode `23`;
- package `studio.guitarlab.app`;
- signed APK SHA-256 `82da7c41591c01e304e44e57031ebac6263a2175866ecd1b438d65da0539462b`;
- unsigned APK SHA-256 `6dd4fb64803e161466f465f385a728ab67bc539ebd9d13307a195461a5bfbb08`;
- certificate SHA-256 `4B82890A9812BB89E1BBEF179A48752BDA2CA8AB284C833DAA27A907F4CE5E89`.

### H0–H11 — DIGITAL PASS
Historical baseline #620 remains useful regression evidence, but #624 supersedes it as the active canonical candidate because #624 includes all later Physical Review IV work.

### H12 — all-track level workflow — DIGITAL PASS
- `Níveis` action integrated into the Comparação practice segment;
- dedicated modal for all project tracks;
- global and per-track analysis/reanalysis;
- global and per-track apply;
- global apply is one transactional project edit / one Undo step;
- stale analysis rejected through exact track + audible-clip snapshot validation;
- explicit per-track/global busy state;
- connected UI instrumentation PASS at #624.

### H13 — Trim time-ruler projection — DIGITAL PASS
- waveform-overlapping Trim time bubbles removed;
- T1/T2 projected on the fixed timeline time ruler with short yellow ticks;
- precise floating labels remain above ordinary timeline/playhead drawing priority;
- nearby T1/T2 labels separate vertically;
- existing independent Trim handles retained;
- retained physical-editing instrumentation PASS at #624.

### H14 — Mixer horizontal overflow — DIGITAL PASS
- track strips use `LazyRow` horizontal scrolling;
- MASTER remains outside the scroll container and fixed at the right edge;
- product regression uses a 10-track Mixer and strict invariant MASTER bounds.

### H14a — viewport-independent physical swipe regression — DIGITAL PASS
- remains a separate source-part after H14 for bisectability;
- uses a real `swipe(start, end)` through non-slider track-header chrome;
- checks whether the final strip intersects the actual scroller viewport;
- bounded 20-gesture safety ceiling avoids viewport-specific fixed swipe counts;
- strict MASTER left/right geometry assertions remain;
- no `scrollToItem`, timeout inflation or weakened assertions;
- `MixerDockInstrumentedTest.overflowingTracksSwipeHorizontallyWhileMasterRemainsAnchored`: PASS at #624.

### H15 — resident Studio navigation return — DIGITAL PASS
- same-project `StudioViewModel.load()` returns from resident state rather than publishing `loading=true` and reloading;
- Home/Options → same Studio preserves resident project/history and avoids the observed double-render flash path;
- lifecycle instrumentation asserts object identity and Undo retention after round-trip navigation;
- different-project loading remains on the normal path;
- PASS at #624.

## Physical Review IV source chain
Required order after H11b:
1. `H12LevelEngine.patch`
2. `H12LevelUi.patch`
3. `H13TrimRuler.patch`
4. `H14MixerHorizontalScroll.patch`
5. `H14aMixerScrollViewportRegression.patch`
6. `H15ResidentStudioReturn.patch`

Patch SHA-256:
- H12 engine `39c6a42bca192b1e6a829bd52c46ddb7e546d7cad09500d850e257dec57bc37c`
- H12 UI `db9a7e448a22f79e8be22b9b795d4a4008bd92b4bff9754ad640eb957895308d`
- H13 `4fd47e9d55de072be9ccfbc64361f3e87f9e38d68aa57a76a5084085bce399b0`
- H14 `af3bf0808a5724f8aab3f6f17dec15b041591871ae26e51283d85a03a28a3e43`
- H14a v2 `0ecdfc2922311a3f6b0c773ece8cb4a27eb049780e8affa793bc2f496b71cef7`
- H15 `79e4a98f996f86cb2c0916f1c152ef8a34529e369f7db1622ea4a5a7f427a1c9`

## Diagnostic history
- #621: software gate PASS, API36 21/22; original H14 regression assumed four swipes.
- #622: materializer truncation before build/test; infrastructure failure only.
- #623: repaired software gate PASS, API36 21/22; H14a v1 centerline gesture conflicted with child sliders.
- H14a v2 moved the physical gesture to non-slider track-header chrome while keeping viewport reachability and MASTER invariants strict.
- #624: complete canonical PASS, including API36 22/22, isolated tablet geometry and signed homologation.

## Remaining release path
No new digital gate is required unless source/product code changes.

Next step is the final physical checklist `RC3_FINAL_PHYSICAL_HOMOLOGATION.md` using the exact #624 signed APK. Physical approval must cover only what CI cannot establish: real tablet/MK300 routing, REC behavior, synchronization/latency perception, listening quality, interaction feel, visual readability and absence of perceptible return flicker.

## Gate discipline
`.github/workflows/android-ci.yml` remains manual-only (`workflow_dispatch`). The assistant must not dispatch or rerun Actions. Documentation/maintenance commits use `[skip ci]` and do not alter the digitally homologated application/source SHA recorded above.
