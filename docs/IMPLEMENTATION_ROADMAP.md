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
Digital scope through H19/H18a is **PASS at CI #631**. Physical closure is pending only for facts that cannot be established by emulator/CI, especially real MK-300 USB routing and final listening/recording behavior.

## M8 — Release hardening

### Canonical signed baseline — CI #631
Run `35025012392`, exact source `33fb05a504be2d047259b1d967e6ab1a7e48a68c`:
- software/unit/audio/DSP/persistence/migration/performance/Lint/build/provenance: PASS;
- API36 connected regression: **23/23 PASS**;
- isolated 1920×1200 geometry: **1/1 PASS**;
- signed homologation: PASS;
- signed APK SHA-256: `61441b92e3065ba845d9f3e0ed6791d35d41975180a01bb21b612427b493c02d`;
- unsigned APK SHA-256: `895ed8ccc957bf0bb17addfdd98806fd3425cc695443f234e27bbae62607cfd8`;
- certificate SHA-256: `4B82890A9812BB89E1BBEF179A48752BDA2CA8AB284C833DAA27A907F4CE5E89`.

### H18 — Adaptive practice bar — DIGITAL PASS
- remove fixed percentage clipping from the docked practice bar;
- size Comparação/Ajustes to content on wide/tablet layouts;
- assign remaining horizontal space and overflow ownership to Timeline;
- enforce full containment of `Desativado`, `Referência`, `Minha`, `Ambas`.

### H18a — narrow fallback correction — DIGITAL PASS
- narrow docked widths stack Comparação, Ajustes and Timeline instead of scrolling the entire segmented strip;
- long groups own their local overflow;
- target-tablet wide branch remains content-first and single-row;
- regression separately covers narrow discoverability and target-tablet logical-width containment.

### H19 — USB output route canonicalization — DIGITAL PASS / FINAL HARDWARE ASSERTION PENDING
- collapse duplicate logical USB endpoints into one canonical physical output choice;
- retain distinct non-USB profiles;
- migrate old endpoint signatures;
- resolve duplicate candidates through silent `AudioTrack` probe + actual `routedDevice` confirmation;
- never depend on first/second Android enumeration order;
- reject stale endpoint IDs and clear disappeared selections;
- deterministic JVM policy tests passed at #631.

Digital acceptance is complete. Remaining H19 acceptance is intentionally physical: confirm one MK-300 choice, audible playback through it, and correct reconnect/reselection behavior on Samsung SM-X230.

## Diagnostic lineage
- CI #628: software/H19 PASS, API36 21/22 because the first H18 narrow fallback could hide Ajustes.
- H18a corrected the narrow fallback.
- CI #629/#630: pre-build `git diff --check` failures caused solely by four trailing spaces in the H18a patch file.
- `33fb05a...` cleaned only that whitespace.
- CI #631: complete green exact-source gate and signed candidate.

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

## Remaining acceptance
Use the exact #631 signed APK for the final residual physical checklist:
- all comparison controls visible and interactive at the real tablet width;
- Ajustes/Níveis balanced and non-overlapping;
- one MK-300 output entry only and audible playback through it;
- disconnect/reconnect/reselection;
- retained recording/routing/live-waveform/meters/synchronization/listening smoke.

If those checks pass and no product/source changes are introduced, M7 physical closure and the RC3 release decision can be finalized without another CI run.

## Gate discipline
`.github/workflows/android-ci.yml` remains manual-only (`workflow_dispatch`). The assistant must not dispatch or rerun Actions.
