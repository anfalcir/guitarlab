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
- H23b: **DIGITAL PASS at CI #639** / exact source `eb9c4a4ca2a6269fbe2f2211a0807b8c703e115c`; focused physical recording-timing validation remains pending.

## M8 — Release hardening

### Current signed authority
CI #639 / run `35096711936` / exact product source `eb9c4a4ca2a6269fbe2f2211a0807b8c703e115c`:
- unit/JVM: **260/260 PASS**;
- Android Lint/build/provenance: PASS;
- API36 standard: **23/23 PASS**;
- isolated 1920×1200 geometry: **1/1 PASS**;
- signed homologation: PASS;
- unsigned APK SHA-256 `195aa82a581bbcc30890b278cab03bc029eb5d3376fa99130b67e24f6213e21a`;
- signed APK SHA-256 `ffac9da48c17fe2bd28172d357c2f45e906c15b20a216443c1b8b78a9a893696`;
- signer SHA-256 `4B82890A9812BB89E1BBEF179A48752BDA2CA8AB284C833DAA27A907F4CE5E89`.

### H23b acceptance status
1. **Frame-exact signed clock mapping** with stale/backwards timestamp rejection — DIGITAL PASS.
2. **Complete rate matrix** at 44.1 / 48 / 88.2 / 96 kHz — DIGITAL PASS.
3. **No double compensation** across session clock, route latency and fine adjustment — DIGITAL PASS.
4. **Exact calibration scope** by input + output + sample rate — DIGITAL PASS.
5. **Analyzer telemetry surface** for attempts, median, jitter, drift, confidence and status — DIGITAL PASS.
6. **Global feedback discipline and route-token sanitization** — DIGITAL PASS.
7. **Regression retention** in automated gates — DIGITAL PASS; focused physical smoke remains.

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
1. **DONE:** H23b published atomically and materialized deterministically.
2. **DONE:** user-dispatched full signed CI #639 on exact H23b source.
3. **DONE:** software/API36/signing/provenance audit of CI #639.
4. Install the exact CI #639 signed candidate on SM-X230 + MK-300.
5. Start physical timing A/B with fine adjustment **0 ms**, including a 44.1 kHz project and at least three repeated takes.
6. Exercise timeline-zero/non-zero starts and one punch/loop take; verify no state carries into subsequent takes.
7. If a stable residual remains, run accepted route/rate calibration before any manual fine adjustment.
8. Retain H22 route UX, transient feedback, backing-leakage, waveform/meters, route reconnect, export and trim/undo/redo smoke.
9. Final RC3 approval requires no repeatable P0/P1 timing defect and explicit approval of the exact signed APK with SHA-256 `ffac9da48c17fe2bd28172d357c2f45e906c15b20a216443c1b8b78a9a893696`.

## Gate discipline
`.github/workflows/android-ci.yml` remains manual-only (`workflow_dispatch`). The assistant must not dispatch or rerun Actions. A later docs-only commit does not replace `eb9c4a4ca2a6269fbe2f2211a0807b8c703e115c` as the product source of CI #639.
