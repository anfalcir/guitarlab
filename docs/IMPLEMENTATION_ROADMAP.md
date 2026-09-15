# Implementation Roadmap

Updated: 2026-09-15

## M1 — Foundation — CLOSED
Project model, templates, persistence and repository structure.

## M2 — Android hardware/audio baseline — PASS/CLOSED
Android USB-audio baseline established for the hardware actually tested.

## M3–M4 — Codec/import + Studio foundation — ABSORBED
Consolidated into later milestones.

## M5 — Reliable recording + Studio consolidation + Media I/O — PASS/CLOSED
Closed after physical approval.

## M6 — Measured latency and synchronization — PASS/CLOSED
Closed after physical approval.

## M7 — Production audio polish
Digital scope through H19/H18a is PASS at CI #631. Physical review of that APK produced H20/H21, so M7 physical closure remains pending until the new exact-source digital gate and final hardware/visual review pass.

## M8 — Release hardening

### Last canonical signed baseline — CI #631
Run `35025012392`, source `33fb05a504be2d047259b1d967e6ab1a7e48a68c`:
- software/unit/audio/DSP/persistence/migration/performance/Lint/build/provenance: PASS;
- API36 connected regression: 23/23 PASS;
- isolated 1920×1200 geometry: 1/1 PASS;
- signed homologation: PASS;
- signed APK SHA-256: `61441b92e3065ba845d9f3e0ed6791d35d41975180a01bb21b612427b493c02d`.

### H20 — physical output canonicalization v2 — IMPLEMENTED / SOURCE-VALIDATED / PRE-GATE
- generalize physical-output canonicalization from USB-only to built-in speaker families;
- collapse `SM-X230`, `SM-X230 • 0`, `SM-X230 • back`, `SM-X230 • bottom` into one user-facing physical speaker choice;
- preserve earpiece/Bluetooth/HDMI and other materially distinct profiles;
- keep routedDevice-confirmed candidate resolution and legacy migration;
- add policy regressions for exact tablet topology and route separation.

### H21 — Studio visual system overhaul — IMPLEMENTED / SOURCE-VALIDATED / PRE-GATE
- standardize geometry across the entire app around 6dp controls, 8dp internal elements and 10dp major panels;
- reserve circles for genuine circular semantics only;
- square off track-color selectors with subtle rounding;
- give icon actions a visible hardware-style chassis while retaining 48dp touch targets;
- strengthen global surface/outline contrast;
- redesign the practice bar as three independent semantic chassis with fixed non-action titles and functional accent families;
- preserve adaptive wide/narrow behavior and add title/action boundary assertions.

See `docs/UI_VISUAL_SYSTEM.md` for the screen-by-screen contract.

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

## Next acceptance gate
Because H20/H21 change product source after #631, a new manually dispatched exact-source full gate is mandatory. DIGITAL PASS requires software/unit/Lint/build/provenance, route-policy regressions, complete API36 connected regression, isolated tablet geometry and signed homologation all green on the same SHA.

## Final residual physical acceptance after that gate
- output selector shows one SM-X230 physical speaker choice rather than logical `0/back/bottom` duplicates;
- MK-300 is represented once when connected and routes audible playback;
- disconnect/reconnect/reselection remains correct;
- visual hierarchy is immediately understandable on Home, Novo Projeto, Studio, Mixer, Options and diagnostics;
- practice-bar groups/titles/buttons are visually unambiguous;
- retained recording/routing/live-waveform/meters/synchronization/listening smoke passes.

## Gate discipline
`.github/workflows/android-ci.yml` remains manual-only (`workflow_dispatch`). The assistant must not dispatch or rerun Actions.
