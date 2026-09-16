# Implementation Roadmap

Updated: 2026-09-16

## Milestones
- **M1 — Foundation:** CLOSED.
- **M2 — Android hardware/audio baseline:** PASS/CLOSED.
- **M3–M4 — Codec/import + Studio foundation:** ABSORBED into later milestones.
- **M5 — Reliable recording + Studio consolidation + Media I/O:** PASS/CLOSED.
- **M6 — Measured latency and synchronization:** PASS/CLOSED. RC3 residual timing is release hardening, not a reopening of M6.
- **M7 — Production audio polish:** active residual physical closure.
- **M8 — Release hardening:** active.

## Current authority
CI #639 / run `35096711936` / source `eb9c4a4ca2a6269fbe2f2211a0807b8c703e115c` is the last signed DIGITAL PASS through H23b.

Evidence:
- 260/260 JVM/unit tests PASS;
- Lint/build/provenance PASS;
- API36 standard **23/23 PASS**;
- isolated 1920×1200 geometry **1/1 PASS**;
- signed homologation PASS;
- signed APK SHA-256 `ffac9da48c17fe2bd28172d357c2f45e906c15b20a216443c1b8b78a9a893696`;
- signer SHA-256 `4B82890A9812BB89E1BBEF179A48752BDA2CA8AB284C833DAA27A907F4CE5E89`.

## H22–H23b retained state
- H22/H22a semantic route UX: DIGITAL PASS plus focused SM-X230 PHYSICAL PASS.
- H23/H23a/H23b recording timing, route/rate calibration and transient-feedback hardening: DIGITAL PASS at #639.
- Focused H23b physical recording-timing validation remains pending.

## H24/H24a — Home Project Library
Status: **IMPLEMENTED / SOURCE-VALIDATED / PRE-GATE**.

Acceptance goals:
1. Search project names in real time, ignoring case and diacritics.
2. Combine template, content and sample-rate filters without repository rereads.
3. Support deterministic updated/created/name ordering in both directions.
4. Keep default behavior equivalent to the former recent-project list: updated descending.
5. Distinguish empty library from zero results caused by query/filter state.
6. Preserve query state while the project repository snapshot refreshes.
7. Use one immutable normalized index per repository snapshot; no disk I/O per keystroke.
8. Preserve project schema and `.guitarlab` compatibility.
9. Expose accessible/testable filter/sort/search controls and synchronize Help.
10. Fail closed through the existing source materialization/hash contract.

Implementation anchor: `96ffe7bd394e2eda68707cf2ad8c8596432cd26a`.

CI #640 (`35103316449`) produced a complete software-gate PASS but stopped at Android-test compilation because the H24 instrumented test used one invalid Compose test import. H24a is a test-source-only corrective alignment; production Home behavior is unchanged.

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
19. H24 Home Project Library
20. **H24a Android-test compile alignment**

## Remaining release path
1. H24a corrective source/materializer/docs land with `[skip ci]`.
2. User manually dispatches one fresh full signed CI on final `main`; do not rerun #640 because its source SHA is superseded.
3. Audit software/API36/geometry/signing and artifact provenance on the exact new workflow SHA.
4. If green, promote H24/H24a to DIGITAL PASS in documentation.
5. Install only that exact signed candidate on SM-X230.
6. Run short H24 Home-library physical smoke.
7. Complete H23b zero-adjustment recording-timing validation on MK-300, including repeated 44.1 kHz takes and non-zero playhead/punch.
8. Use route/rate calibration only if a repeatable residual remains.
9. Final RC3 approval requires no repeatable P0/P1, retained route/backing-isolation behavior and explicit approval of the exact signed APK.

## Gate discipline
`.github/workflows/android-ci.yml` remains manual-only (`workflow_dispatch`). The assistant must not dispatch or rerun Actions.
