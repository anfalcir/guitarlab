# Implementation Roadmap

Updated: 2026-09-15

## M1 — Foundation — CLOSED
Project model, templates, persistence and repository structure.

## M2 — Android hardware/audio baseline — PASS/CLOSED
Android USB-audio baseline established; historical hardware evidence remains scoped to the device pair actually tested.

## M3–M4 — Codec/import + Studio foundation — ABSORBED
Consolidated into later milestones.

## M5 — Reliable recording + Studio consolidation + Media I/O — PASS/CLOSED
Closed after physical approval.

## M6 — Measured latency and synchronization — PASS/CLOSED
Closed after physical approval.

## M7 — Production audio polish
Digital scope through H17 is **PASS** at CI #626. Physical review after #626 produced H18/H19; therefore M7 physical closure remains pending until their exact-source gate and final MK-300 check pass.

## M8 — Release hardening

### Retained canonical signed baseline — CI #626
Run `35017084625`, source `f187ab2ba7596c4aa04d223f007409b2fb39f490`:
- software/performance/Lint/build/provenance: PASS;
- API36 connected regression: **22/22 PASS**;
- isolated 1920×1200 geometry: **1/1 PASS**;
- signed homologation: PASS;
- signed APK SHA-256: `93a7ed1ebfdedf7421cf21183db84a529d2d095c9564caafc87952506cb5426b`.

### H18 — Adaptive practice bar — IMPLEMENTED / SOURCE-VALIDATED / PRE-GATE
- remove fixed percentage clipping from the docked practice bar;
- measure Comparação/Ajustes by content;
- assign remaining space to Timeline with local overflow;
- stacked semantic-group fallback for narrow docked widths;
- enforce full containment of all four comparison controls.

Source part: `.source-parts/H18AdaptivePracticeBar.patch.gz`

Patch SHA-256: `fba48ae2b0eedd2c87c269738197542f84a2772bac1aa55e0646ee722ef3627e`.

### H19 — USB output route canonicalization — IMPLEMENTED / SOURCE-VALIDATED / PRE-GATE
- collapse duplicate logical USB endpoints into one physical output choice;
- retain distinct non-USB profiles;
- migrate old endpoint signatures to the canonical physical route;
- resolve duplicate candidates using a silent stereo probe and actual `routedDevice` confirmation;
- never select by first/second enumeration order;
- clear stale persistent selection when the route disappears;
- add deterministic JVM policy tests.

Source part: `.source-parts/H19UsbOutputRouteCanonicalization.patch.gz`

Patch SHA-256: `21373fa0d85d55ee180fed29308e5677e7bde90b1b555e366c61a872a388eb06`.

### CI #628 diagnostic — PARTIAL PASS / NOT SIGNED
Run `35021968990`, source `ec05eec58397dc09237d163d6537eb49cfbd3650`:
- complete software/Lint/build/provenance gate: PASS;
- H19 route-policy unit tests: PASS;
- API36: 21/22 PASS;
- only failure: H18 narrow fallback could scroll the Ajustes segment completely off-screen on the phone-shaped default emulator;
- signing skipped by design.

### H18a — narrow fallback correction — IMPLEMENTED / SOURCE-VALIDATED / PRE-GATE
- narrow docked widths stack Comparação, Ajustes and Timeline instead of scrolling the complete strip;
- each long group owns local overflow;
- wide target-tablet branch remains content-first and single-row;
- regression is split into narrow discoverability and ~1280dp target-tablet containment.

Source part: `.source-parts/H18aAdaptivePracticeBarNarrowFallback.patch`

Patch SHA-256: `cbb483c11fdc9166f8e23fb104378a81a4df98a0067164c8d5cd79d3ea84358b`.

## Canonical materialization tail
After H11b:
1. H12 Level Engine
2. H12 Level UI
3. H13 Trim Ruler
4. H14 Mixer Horizontal Scroll
5. H14a Mixer Scroll Viewport Regression
6. H15 Resident Studio Return
7. H16 Final UI / Trim Overlay
8. H17 CUT Ruler / Practice Spacing
9. **H18 Adaptive Practice Bar**
10. **H19 USB Output Route Canonicalization**
11. **H18a Narrow Practice-Bar Fallback Correction**

## Next acceptance gate
Because H18a changes product/UI source after the failed #628 diagnostic, one new manual exact-source gate is required. They become DIGITAL PASS only if software/unit/Lint/build/provenance, full API36 connected regression, isolated tablet geometry and signed homologation all pass on the same source SHA.

After that, physical closure should be narrowly focused on the exact new APK: all comparison buttons visible, one MK-300 output choice only, playback through that choice, reconnect/reselection behavior, and the retained recording/routing/listening smoke.

## Gate discipline
`.github/workflows/android-ci.yml` remains manual-only (`workflow_dispatch`). The assistant must not dispatch or rerun Actions.
