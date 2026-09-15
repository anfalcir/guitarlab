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

## M7 — Production audio polish — DIGITAL PASS / PHYSICAL CLOSURE PENDING
The complete digital scope through H17 is PASS at CI #626 / run `35017084625` / source `f187ab2ba7596c4aa04d223f007409b2fb39f490`.

The remaining M7 authority is final physical homologation on the real Samsung SM-X230 + M-VAVE MK-300 and human visual/auditory perception.

## M8 — Release hardening — DIGITAL PASS THROUGH H17

### Canonical signed gate — CI #626
Manual `workflow_dispatch`, exact source `f187ab2ba7596c4aa04d223f007409b2fb39f490`:
- source materialization through H17: PASS;
- software/unit/audio/DSP/persistence/migration regression: PASS;
- performance evidence: PASS;
- Android Lint: PASS;
- debug + release build and unsigned provenance: PASS;
- API36 full connected regression: **22/22 PASS**;
- isolated 1920×1200 geometry: **1/1 PASS**;
- signed homologation: PASS;
- package/version/source/signer/checksum provenance: PASS.

Canonical candidate identity:
- version `0.5.0-rc3` / versionCode `23`;
- package `studio.guitarlab.app`;
- unsigned APK SHA-256 `28578bab8b76a611aa1b0cd92f8aa0b428826526779e1757ab45b5b34ce254b9`;
- signed APK SHA-256 `93a7ed1ebfdedf7421cf21183db84a529d2d095c9564caafc87952506cb5426b`;
- certificate SHA-256 `4B82890A9812BB89E1BBEF179A48752BDA2CA8AB284C833DAA27A907F4CE5E89`.

### H12–H16 — retained DIGITAL PASS
H12 global levels, H13 Trim ruler, H14/H14a Mixer overflow with fixed MASTER, H15 resident Studio return and H16 practice-bar/Trim-overlay polish remain digitally homologated and are retained by #626.

### H17 — final CUT ruler + practice spacing correction — DIGITAL PASS
- visible T1/T2 time-label boxes removed;
- short CUT ticks confined to the time ruler;
- exact timeline X projection retained;
- `Ajustes + Níveis` centered inside the dedicated center segment;
- no overlap with Comparação or Timeline;
- approximately equal left/right inset inside Ajustes.

Regression evidence at #626:
- `AutoSectionsSlotInstrumentedTest.dockedPracticeControlsRenderAsBalancedComparisonAdjustmentsAndTimelineSegments`: PASS;
- `PhysicalEditingHardeningInstrumentedTest.trimHandlesAreIndependentlyDraggableAndClipDeleteRequiresConfirmation`: PASS;
- overall API36 suite: **22/22 PASS**;
- isolated target-tablet geometry: **1/1 PASS**.

Source part:
`.source-parts/H17CutRulerPracticeSpacing.patch`

Patch SHA-256: `38b3f494cf528fcc9fc818e6ef38ed0647389e106ec1bcc821e00ee2e65278dc`.

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

## Remaining release path
No new digital gate is required unless source/product code changes.

Next step is the final physical checklist `RC3_FINAL_PHYSICAL_HOMOLOGATION.md` using the exact #626 signed APK. Physical approval must cover only what CI cannot establish: real tablet/MK-300 routing, REC behavior, synchronization/latency perception, listening quality, interaction feel, visual readability and absence of perceptible return flicker.

## Gate discipline
`.github/workflows/android-ci.yml` remains manual-only (`workflow_dispatch`). The assistant must not dispatch or rerun Actions. Documentation-only promotion commits use `[skip ci]` and do not replace the exact application/source SHA recorded above.
