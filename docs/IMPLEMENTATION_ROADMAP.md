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
The complete digital scope through H16 is PASS at CI #625 / run `35010012582` / source `476fa740408130adf6a4e9665d166e724a9184dd`.

The remaining M7 authority is final physical homologation on the real Samsung SM-X230 + M-VAVE MK-300 and human visual/auditory perception.

## M8 — Release hardening — DIGITAL PASS THROUGH H16

### Canonical signed gate — CI #625
Manual `workflow_dispatch`, exact source `476fa740408130adf6a4e9665d166e724a9184dd`:
- source materialization: PASS;
- software/unit/audio/DSP/persistence/migration regression: PASS;
- performance evidence: PASS;
- Android Lint: PASS;
- debug + release build and unsigned provenance: PASS;
- API36 full connected regression: **22/22 PASS**;
- isolated 1920×1200 geometry: PASS;
- signed homologation: PASS;
- package/version/source/signer/checksum provenance: PASS.

Canonical candidate identity:
- version `0.5.0-rc3` / versionCode `23`;
- package `studio.guitarlab.app`;
- unsigned APK SHA-256 `46750bb10e70c70d350011aa46411d2cafb7766746c20b6e83add100b6f8055a`;
- signed APK SHA-256 `107795f040ed18246bb130a9519044ba7e07f334a5835566b952cbc7fdb528e6`;
- certificate SHA-256 `4B82890A9812BB89E1BBEF179A48752BDA2CA8AB284C833DAA27A907F4CE5E89`.

### H12–H15 + H14a — DIGITAL PASS
H12 global levels, H13 Trim ruler, H14/H14a scrollable Mixer with fixed MASTER, and H15 resident same-project return remain digitally homologated and are retained by #625.

### H16 — final practice-bar + Trim-overlay polish — DIGITAL PASS
Practice bar:
- neutral comparison label is `Desativado`;
- top-bar Mixer panel toggle remains `Mixer`;
- `Níveis` belongs to dedicated `Ajustes`;
- docked order `Comparação | Ajustes | Timeline`;
- proportional weights `0.34 / 0.16 / 0.50`.

Trim geometry:
- T1/T2 use the same effective width/projection as playhead/loop;
- ruler-only 8 dp shrink removed;
- compact ruler 20 dp;
- zero spacing between marker rail and ruler;
- T1/T2 overlay the existing rail/ruler while Cut is active;
- no dedicated Trim lane and no waveform obstruction.

Regression evidence at #625:
- `AutoSectionsSlotInstrumentedTest.dockedPracticeControlsRenderAsBalancedComparisonAdjustmentsAndTimelineSegments`: PASS;
- retained `PhysicalEditingHardeningInstrumentedTest`: PASS;
- overall connected API36 suite: **22/22 PASS**;
- isolated target-tablet geometry: PASS.

Source part:
`.source-parts/H16FinalUiTrimOverlay.patch.gz`

Decoded patch SHA-256:
`40a4056644707c57291dfd876fde8487d8dbe9c436ba0409ccb7c6a60c31a0bb`

## Canonical materialization tail
After H11b:
1. H12 Level Engine
2. H12 Level UI
3. H13 Trim Ruler
4. H14 Mixer Horizontal Scroll
5. H14a Mixer Scroll Viewport Regression
6. H15 Resident Studio Return
7. H16 Final UI / Trim Overlay

## Remaining release path
No new digital gate is required unless source/product code changes.

Next step is the final physical checklist `RC3_FINAL_PHYSICAL_HOMOLOGATION.md` using the exact #625 signed APK. Physical approval must cover only what CI cannot establish: real tablet/MK-300 routing, REC behavior, synchronization/latency perception, listening quality, interaction feel, visual readability and absence of perceptible return flicker.

## Gate discipline
`.github/workflows/android-ci.yml` remains manual-only (`workflow_dispatch`). The assistant must not dispatch or rerun Actions. Documentation-only promotion commits use `[skip ci]` and do not replace the exact application/source SHA recorded above.
