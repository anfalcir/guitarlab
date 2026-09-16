# Implementation Roadmap

Updated: 2026-09-16

## M1 — Foundation — CLOSED
Project model, templates, persistence and repository structure.

## M2 — Android hardware/audio baseline — PASS/CLOSED
Android USB-audio baseline established for the tested hardware.

## M3–M4 — Codec/import + Studio foundation — ABSORBED
Consolidated into later milestones.

## M5 — Reliable recording + Studio consolidation + Media I/O — PASS/CLOSED
Closed after physical approval.

## M6 — Measured latency and synchronization — PASS/CLOSED
The original calibration foundation is closed. RC3 residual timing is handled as release hardening, not as a reopening of the milestone architecture.

## M7 — Production audio polish
- H22/H22a: DIGITAL PASS at CI #636; focused physical route UX PASS on SM-X230.
- H23/H23a: DIGITAL PASS at CI #638 / exact source `c310be6779e6591c57399257f380588c27bdf20a`.
- H23b: corrective timing/calibration/feedback hardening, **SOURCE-VALIDATED / PRE-GATE** pending one user-dispatched signed CI.

## M8 — Release hardening

### Last signed authority
CI #638 / run `35084703365` / source `c310be6779e6591c57399257f380588c27bdf20a`:
- software/unit/Lint/build/provenance: PASS;
- API36 standard: **23/23 PASS**;
- isolated 1920×1200 geometry: **1/1 PASS**;
- signed homologation: PASS;
- signed APK SHA-256 `a650afa5edfd2b8c4f8393e65b314ae9fbb59487a87c2d3ea85ef978d7d895dc`;
- signer SHA-256 `4B82890A9812BB89E1BBEF179A48752BDA2CA8AB284C833DAA27A907F4CE5E89`.

### H23b acceptance goals
1. **Frame-exact signed clock mapping** in both startup directions with stale timestamp rejection.
2. **Complete rate matrix** at 44.1 / 48 / 88.2 / 96 kHz.
3. **No double compensation:** session clock, accepted route latency and residual fine adjustment remain independent.
4. **Exact calibration scope:** input + output + sample rate, no lossy key used for auto-application.
5. **Professional analyzer surface:** attempts, median, jitter, drift, confidence and valid/unstable/not-calibrated state.
6. **Global feedback discipline:** routine actions stay silent; technical Android route identity never leaks through normal Snackbar/error display.
7. **Regression retention:** H22 route semantics, punch/loop, recording state cleanup, import/export and prior editing behavior remain intact.

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
18. H23b Recording Timing / Calibration / Feedback Corrective Hardening

## Remaining release path
1. Publish H23b source-parts/materializer/docs atomically with `[skip ci]`.
2. User manually dispatches the full signed CI on the exact H23b `main` SHA.
3. Audit software/API36/signing results and artifact provenance on that same SHA.
4. Only then install the signed H23b candidate on SM-X230 + MK-300.
5. Start physical timing A/B with fine adjustment **0 ms**, including a 44.1 kHz project and at least three repeated takes.
6. If a stable residual remains, use accepted route/rate calibration before any manual fine adjustment.
7. Retain feedback, route reconnect, waveform/meters, punch/loop, export and trim/undo/redo smoke.
8. Final RC3 approval requires no repeatable P0/P1 timing defect and explicit approval of the exact signed APK.

## Gate discipline
`.github/workflows/android-ci.yml` remains manual-only (`workflow_dispatch`). The assistant must not dispatch or rerun Actions.
