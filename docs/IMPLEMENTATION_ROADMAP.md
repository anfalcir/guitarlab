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
- H23/H23a recording-timing/transient-feedback hardening: DIGITAL PASS at CI #638.

## M8 — Release hardening

### Last signed baseline — CI #638
Run `35084703365`, exact product/source SHA `c310be6779e6591c57399257f380588c27bdf20a`:
- software/unit/audio/DSP/persistence/migration/performance/Lint/build/provenance: PASS;
- API36 connected regression: **23/23 PASS**;
- isolated 1920×1200 geometry: **1/1 PASS**;
- signed homologation: PASS;
- unsigned APK SHA-256: `621e02355d265bdb6c24cb5e324b445b63e739f8e7d6adc233eebea3b31fe0d5`;
- signed APK SHA-256: `a650afa5edfd2b8c4f8393e65b314ae9fbb59487a87c2d3ea85ef978d7d895dc`;
- signer SHA-256: `4B82890A9812BB89E1BBEF179A48752BDA2CA8AB284C833DAA27A907F4CE5E89`.

### H23 achieved objectives
1. **Automatic timing first:** signed capture/playback clock mapping with stable monotonic anchors.
2. **No double compensation:** session offset, route calibration and fine residual adjustment remain separate.
3. **Actual-rate calibration:** 44.1/48/96 kHz follows the project editing domain.
4. **Plan-B tool:** route/rate-specific loopback analyzer plus bounded fine adjustment.
5. **Feedback discipline:** no routine-state Snackbar spam and no raw Android route names in normal UI.
6. **Regression:** punch, loop/REC, route fail-closed and previous H22 route UX retained through the full #638 gate.

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
17. H23a Transient Feedback Test Annotation Alignment

## Remaining release path
1. Install only the exact signed CI #638 APK.
2. Focused physical H23 REC A/B on SM-X230 + MK-300, especially the 44.1 kHz scenario that exposed residual late placement.
3. Begin with fine adjustment at **0 ms** and verify whether repeatable systematic late placement remains.
4. If a stable route-specific residual remains, use the objective route/rate latency analyzer first; use fine adjustment only as Plan B.
5. Verify normal Play/CUT/REC does not produce Snackbar spam or raw technical route names.
6. Retained smoke: route presentation/reconnect, live waveform/meters, punch/loop, export, trim/undo/redo.
7. Final RC3 decision only after no repeatable P0/P1 remains.

## Gate discipline
`.github/workflows/android-ci.yml` remains manual-only (`workflow_dispatch`). The assistant must not dispatch or rerun Actions.
