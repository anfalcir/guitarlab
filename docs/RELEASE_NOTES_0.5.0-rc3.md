# GuitarLab Studio 0.5.0-rc3

Updated: 2026-09-15

## Canonical digital homologation — CI #626
Run `35017084625`, manual `workflow_dispatch`, exact source `f187ab2ba7596c4aa04d223f007409b2fb39f490`:
- source materialization through H17: **PASS**;
- unit/core/audio/DSP/persistence/migration regression: **PASS**;
- reproducible performance evidence: **PASS**;
- Android Lint: **PASS**;
- debug + release assembly and unsigned provenance: **PASS**;
- API36 full connected regression: **22/22 PASS**;
- isolated 1920×1200 tablet geometry: **1/1 PASS**;
- signed homologation: **PASS**;
- package/version/source/signer/checksum verification: **PASS**.

Canonical identity:
- package `studio.guitarlab.app`;
- version `0.5.0-rc3` / versionCode `23`;
- unsigned APK SHA-256 `28578bab8b76a611aa1b0cd92f8aa0b428826526779e1757ab45b5b34ce254b9`;
- signed APK SHA-256 `93a7ed1ebfdedf7421cf21183db84a529d2d095c9564caafc87952506cb5426b`;
- certificate SHA-256 `4B82890A9812BB89E1BBEF179A48752BDA2CA8AB284C833DAA27A907F4CE5E89`;
- APK Signature Scheme v2 verified with one RSA-4096 signer.

CI #626 supersedes #625 as the active signed digital baseline because it includes H17 while retaining the previously homologated H12–H16 behavior.

## H17 — CUT ruler + practice spacing — DIGITAL PASS

### CUT finalization
- numeric T1/T2 label boxes removed;
- yellow CUT markers reduced to short ticks inside the time ruler only;
- ticks no longer enter the sections/playhead rail;
- exact canonical timeline X projection retained;
- Trim handles remain in the waveform and independently draggable.

### Practice-bar finalization
- three-block `Comparação | Ajustes | Timeline` structure retained;
- `Ajustes + Níveis` centered inside its dedicated segment;
- Níveis remains fully inside Ajustes;
- center content now has approximately symmetric breathing room to both neighbors.

### Regression status
At #626:
- `AutoSectionsSlotInstrumentedTest.dockedPracticeControlsRenderAsBalancedComparisonAdjustmentsAndTimelineSegments`: PASS;
- `PhysicalEditingHardeningInstrumentedTest.trimHandlesAreIndependentlyDraggableAndClipDeleteRequiresConfirmation`: PASS;
- `MixerDockInstrumentedTest`: PASS;
- `GuitarLabLifecycleInstrumentedTest`: PASS;
- overall API36 suite: **22/22 PASS**;
- isolated target-tablet geometry: **1/1 PASS**.

H17 source-part SHA-256:
`38b3f494cf528fcc9fc818e6ef38ed0647389e106ec1bcc821e00ee2e65278dc`.

## Physical homologation boundary
The exact #626 signed APK is now the intended physical-homologation candidate. Its embedded build identity correctly records `gate=software+android-integration-passed;physical-validation-pending`.

No additional CI run is required unless product/source code changes. Final approval now requires only the residual real-device checklist in `RC3_FINAL_PHYSICAL_HOMOLOGATION.md`, focused on visual confirmation of the H17 CUT/Ajustes refinements plus MK-300 routing/REC/meters/synchronization, interaction feel, listening/perception and absence of perceptible Studio-return flicker.
