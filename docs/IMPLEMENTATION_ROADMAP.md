# Implementation Roadmap

Updated: 2026-09-16

## M1 — Foundation — CLOSED
Project model, templates, persistence and repository structure.

## M2 — Android hardware/audio baseline — PASS/CLOSED
Android USB-audio baseline established for the hardware actually tested.

## M3–M4 — Codec/import + Studio foundation — ABSORBED
Consolidated into later milestones.

## M5 — Reliable recording + Studio consolidation + Media I/O — PASS/CLOSED
Closed after physical approval.

## M6 — Measured latency and synchronization — PASS/CLOSED
Original latency/calibration foundation closed after physical approval. Residual real-device timing discovered during RC3 hardening is handled as H23 regression hardening rather than reopening M6 architecture.

## M7 — Production audio polish
- H20/H21: superseded by later validation.
- H22/H22a: DIGITAL PASS at CI #636.
- H22 focused physical route UX: PASS on target Samsung tablet; duplicate low-level routes resolved and semantic labels approved.
- H23 recording-timing/transient-feedback hardening: IMPLEMENTED / SOURCE-VALIDATED / PRE-GATE.

## M8 — Release hardening

### Last signed baseline — CI #636
Run `35040569179`, exact product/source SHA `b0a39a765f7cfbb0e9320ee847809300bc1e3d01`:
- software/unit/audio/DSP/persistence/migration/performance/Lint/build/provenance: PASS;
- API36 connected regression: 23/23 PASS;
- isolated 1920×1200 geometry: 1/1 PASS;
- signed homologation: PASS;
- unsigned APK SHA-256: `de996298a451cd559320cf71498121a652f9e3ca8054dba8bf2d95a281d08c47`;
- signed APK SHA-256: `b195d8fc4d90fa0f8fa8c826525d08328f65090859386a90eaa37e4bcdf4087e`;
- signer SHA-256: `4B82890A9812BB89E1BBEF179A48752BDA2CA8AB284C833DAA27A907F4CE5E89`.

### H23 gate objectives
1. **Automatic timing first:** signed capture/playback clock mapping with stable monotonic anchors.
2. **No double compensation:** session offset, route calibration and fine residual adjustment remain separate.
3. **Actual-rate calibration:** 44.1/48/96 kHz follows the project editing domain.
4. **Plan-B tool:** route/rate-specific loopback analyzer plus bounded fine adjustment.
5. **Feedback discipline:** no routine-state Snackbar spam and no raw Android route names in normal UI.
6. **Regression:** punch, loop/REC, route fail-closed and previous H22 route UX must remain intact.

See `docs/RECORDING_LATENCY_CONTRACT.md` and `docs/TRANSIENT_FEEDBACK_CONTRACT.md`.

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
9. H18 Adaptive Practice Bar
10. H19 USB Output Route Canonicalization
11. H18a Narrow Practice-Bar Fallback Correction
12. H20 Physical Output Canonicalization v2
13. H21 Studio Visual System Overhaul
14. H22 Semantic Physical Audio Routes + Practice-Bar Action Emphasis
15. H22a USB Migration Regression Alignment
16. H23 Recording Timing + Transient Feedback Hardening

## Remaining release path
1. Manually run the full signed CI on the exact H23 commit.
2. If green, install only that exact signed APK.
3. Focused physical H23 REC A/B on SM-X230 + MK-300, especially 44.1 kHz evidence scenario.
4. Verify no repeatable late placement with fine adjustment still at zero.
5. If a stable route-specific residual remains, measure/calibrate first; use fine adjustment only as documented Plan B.
6. Retained smoke: route presentation/reconnect, live waveform/meters, punch/loop, export, trim/undo/redo.
7. Final RC3 decision only after no repeatable P0/P1 remains.

## Gate discipline
`.github/workflows/android-ci.yml` remains manual-only (`workflow_dispatch`). The assistant must not dispatch or rerun Actions.
