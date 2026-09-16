# Implementation Roadmap

Updated: 2026-09-16

## Milestones
- **M1 — Foundation:** CLOSED.
- **M2 — Android hardware/audio baseline:** PASS/CLOSED.
- **M3–M4 — Codec/import + Studio foundation:** ABSORBED into later milestones.
- **M5 — Reliable recording + Studio consolidation + Media I/O:** PASS/CLOSED.
- **M6 — Measured latency and synchronization:** PASS/CLOSED. RC3 residual timing is release hardening, not a reopening of M6.
- **M7 — Production audio polish:** digital scope PASS; residual physical closure active.
- **M8 — Release hardening:** digital scope PASS through H24a; residual physical closure active.

## Current signed authority
**CI #641** / run `35105065689` / exact producer source `b11769f340f7056c37dfb17d95b062909dad87bf` is the current signed DIGITAL PASS through H24a.

Evidence:
- **269/269** JVM/unit tests PASS;
- Android Lint gate PASS;
- debug/release build + unsigned provenance PASS;
- API36 standard **25/25 PASS**;
- isolated 1920×1200 geometry **1/1 PASS**;
- signed homologation PASS;
- unsigned APK SHA-256 `824e8c070ebee6bbf920990dce3c948d2fc3474610524f3a087c38bd27ca5248`;
- signed APK SHA-256 `d3698067ed7117d3c3c844b0d94bb117c3448cac3da2897329e4dbfcd71e0f39`;
- signer SHA-256 `4B82890A9812BB89E1BBEF179A48752BDA2CA8AB284C833DAA27A907F4CE5E89`.

Report Android regression canonically as **25/25 standard + 1/1 isolated geometry**.

## H22–H23b retained state
- H22/H22a semantic route UX: DIGITAL PASS plus focused SM-X230 PHYSICAL PASS.
- H23/H23a/H23b recording timing, route/rate calibration and transient-feedback hardening: DIGITAL PASS.
- Focused H23b physical recording-timing validation remains pending.

## H24/H24a — Home Project Library — DIGITAL PASS
Acceptance goals are digitally proven at #641:
1. case/diacritic-insensitive real-time project-name search;
2. combinable template/content/sample-rate filters without repository rereads;
3. deterministic updated/created/name ordering in both directions;
4. former recent-project behavior retained as default updated-desc sort;
5. true-empty library distinct from query/filter zero-results;
6. query state preserved while repository snapshot refreshes;
7. immutable normalized index per repository snapshot; no disk I/O per keystroke;
8. project schema and `.guitarlab` compatibility preserved;
9. accessible/testable filter/sort/search controls and synchronized Help;
10. fail-closed source materialization/hash contract.

Implementation anchor: `96ffe7bd394e2eda68707cf2ad8c8596432cd26a`.
H24a is a test-source-only compile correction and does not change production behavior.

Historical CI #640 remains useful evidence: software gate PASS, Android test compile failure from invalid Compose import, signing skipped. CI #641 supersedes it with the full exact-source PASS.

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
20. H24a Android-test compile alignment

## Remaining RC3 release path
1. Install the exact CI #641 signed candidate on SM-X230.
2. Run short H24 Home-library physical smoke.
3. Confirm retained H22 semantic route UX after MK-300 reconnect.
4. Complete H23b zero-adjustment recording-timing validation on MK-300, including repeated 44.1 kHz takes and zero/non-zero playhead plus loop/punch.
5. Use route/rate calibration only if a repeatable residual remains.
6. Run retained backing-isolation/live-waveform/input-fail-closed/edit-save-reopen/export smoke.
7. Final RC3 approval requires no repeatable P0/P1 and explicit approval of signed APK SHA `d3698067ed7117d3c3c844b0d94bb117c3448cac3da2897329e4dbfcd71e0f39`.

No further deterministic CI rerun is required unless a physical finding causes a source change.

## Gate discipline
`.github/workflows/android-ci.yml` remains manual-only (`workflow_dispatch`). The assistant must not dispatch or rerun Actions.
