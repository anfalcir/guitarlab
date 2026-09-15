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
Digital scope through H21 is PASS at CI #633. Physical closure remains pending only on the residual real-hardware/visual review of the exact #633 signed candidate.

## M8 — Release hardening

### Canonical signed baseline — CI #633
Run `35033323990`, exact product/source SHA `2204e0272f9e6e2f218bebd36889db424e006e03`:
- software/unit/audio/DSP/persistence/migration/performance/Lint/build/provenance: PASS;
- API36 connected regression: 23/23 PASS;
- isolated 1920×1200 geometry: 1/1 PASS;
- signed homologation: PASS;
- unsigned APK SHA-256: `58165dc53cacaf39357a1f28315b6312ada3ed2b6669b59e32ddbe8e4d53c25d`;
- signed APK SHA-256: `f40b24cb36b4cb1299efcb28a35b54e66b107af2ec3d578a870c70e2966ff52e`;
- signer SHA-256: `4B82890A9812BB89E1BBEF179A48752BDA2CA8AB284C833DAA27A907F4CE5E89`.

### H20 — physical output canonicalization v2 — DIGITAL PASS
- built-in speaker logical endpoints such as `SM-X230`, `SM-X230 • 0`, `• back`, `• bottom` collapse into one physical user-facing route;
- H19 USB canonicalization remains intact;
- earpiece/Bluetooth/HDMI remain distinct;
- duplicate candidates are resolved by silent probe + authoritative `routedDevice` confirmation;
- 8 route-policy unit regressions passed in #633.

### H21 — Studio visual system overhaul — DIGITAL PASS
- hardware-inspired geometry standardized across the app;
- controls/cards/panels normalized around 6/8/10dp radii;
- practice bar renders Comparação, Ajustes and Timeline as distinct semantic chassis;
- title/action boundaries, containment and narrow/wide behavior remain regression-guarded;
- API36 and isolated target-tablet geometry both pass in #633.

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

## Final residual physical acceptance
Run only on the exact #633 signed APK:
- one SM-X230 physical speaker route;
- one MK-300 route when connected;
- audible playback/reconnect behavior;
- app-wide visual hierarchy is clear on the real tablet;
- retained recording/routing/live-waveform/meters/synchronization/listening smoke passes.

If these pass and no product/source code changes follow, M7 physical closure and the RC3 release decision may be finalized without another digital CI run.

## Gate discipline
`.github/workflows/android-ci.yml` remains manual-only (`workflow_dispatch`). The assistant must not dispatch or rerun Actions.
