# Implementation Roadmap

Updated: 2026-09-15

## M1 — Foundation — CLOSED
Project model, templates, persistence and repository structure.

## M2 — Android hardware/audio baseline — PASS/CLOSED
Samsung SM-X230 USB-audio baseline established.

## M3–M4 — Codec/import + Studio foundation — ABSORBED
Consolidated into later milestones.

## M5 — Reliable recording + Studio consolidation + Media I/O — PASS/CLOSED
Closed after physical approval.

## M6 — Measured latency and synchronization — PASS/CLOSED
Closed after physical approval.

## M7 — Production audio polish
Digital scope through H16 is **PASS** at CI #625. H17 is the final physical-review correction and is PRE-GATE.

## M8 — Release hardening

### Canonical signed baseline — CI #625
Run `35010012582`, source `476fa740408130adf6a4e9665d166e724a9184dd`:
- complete software/performance/Lint/build/provenance: PASS;
- API36 connected regression: **22/22 PASS**;
- isolated 1920×1200 geometry: PASS;
- signed homologation: PASS;
- signed APK SHA-256: `107795f040ed18246bb130a9519044ba7e07f334a5835566b952cbc7fdb528e6`;
- signer SHA-256: `4B82890A9812BB89E1BBEF179A48752BDA2CA8AB284C833DAA27A907F4CE5E89`.

### H12–H16 — DIGITAL PASS
H12 global levels, H13 Trim ruler, H14/H14a Mixer overflow with fixed MASTER, H15 resident Studio return and H16 practice-bar/Trim-overlay polish are digitally homologated by #625.

### H17 — final CUT ruler + practice spacing correction — IMPLEMENTED / SOURCE-VALIDATED / PRE-GATE
- remove visible T1/T2 time-label boxes;
- render short CUT ticks only inside the time ruler;
- preserve exact timeline X projection;
- center Ajustes+Níveis inside the dedicated center segment;
- assert no overlap and approximately equal left/right inset;
- assert CUT ticks cannot enter the sections/playhead rail.

Implementation commit: `41534dd2fb1ba7b0fc18459dbe5c622e78f039cb`.

Source part: `.source-parts/H17CutRulerPracticeSpacing.patch`

Patch SHA-256: `38b3f494cf528fcc9fc818e6ef38ed0647389e106ec1bcc821e00ee2e65278dc`.

Source-level application/diff/parser checks: PASS. Android compilation/runtime authority remains the next manual CI.

## Canonical materialization tail
After H11b:
1. H12 Level Engine
2. H12 Level UI
3. H13 Trim Ruler
4. H14 Mixer Horizontal Scroll
5. H14a Mixer Scroll Viewport Regression
6. H15 Resident Studio Return
7. H16 Final UI / Trim Overlay
8. **H17 CUT Ruler / Practice Spacing**

## Next acceptance gate
Because H17 changes product source, one new manual exact-source gate is required. H17 becomes DIGITAL PASS only if software/Lint/build/provenance, full API36 connected regression, isolated tablet geometry and signed homologation all pass on the same source SHA.

## Gate discipline
`.github/workflows/android-ci.yml` remains manual-only (`workflow_dispatch`). The assistant must not dispatch or rerun Actions.
