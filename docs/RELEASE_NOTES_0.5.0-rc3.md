# GuitarLab Studio 0.5.0-rc3

Updated: 2026-09-15

## Canonical digital homologation — CI #625
Run `35010012582`, manual `workflow_dispatch`, exact source `476fa740408130adf6a4e9665d166e724a9184dd`:
- source materialization: **PASS**;
- unit/core/audio/DSP/persistence/migration regression: **PASS**;
- reproducible performance evidence: **PASS**;
- Android Lint: **PASS**;
- debug + release assembly and unsigned provenance: **PASS**;
- API36 full connected regression: **22/22 PASS**;
- isolated 1920×1200 tablet geometry: **PASS**;
- signed homologation: **PASS**;
- package/version/source/signer/checksum verification: **PASS**.

Canonical identity:
- package `studio.guitarlab.app`;
- version `0.5.0-rc3` / versionCode `23`;
- unsigned APK SHA-256 `46750bb10e70c70d350011aa46411d2cafb7766746c20b6e83add100b6f8055a`;
- signed APK SHA-256 `107795f040ed18246bb130a9519044ba7e07f334a5835566b952cbc7fdb528e6`;
- certificate SHA-256 `4B82890A9812BB89E1BBEF179A48752BDA2CA8AB284C833DAA27A907F4CE5E89`;
- APK Signature Scheme v2 verified with one RSA-4096 signer.

CI #625 supersedes #624 as the active signed digital baseline because it includes H16 while retaining the previously homologated H12–H15/H14a behavior.

## H16 — final UI / Trim overlay polish — DIGITAL PASS

### Practice bar clarity
- Comparison neutral state is labeled **Desativado** instead of Mixer.
- The independent top-bar Mixer open/close control keeps the name **Mixer**.
- `Níveis` is moved into a dedicated **Ajustes** block between Comparação and Timeline.
- The docked bar is proportioned `34% / 16% / 50%` for Comparação / Ajustes / Timeline.
- Connected instrumentation verifies the three segments, labels, action placement and proportional geometry.

### Trim presentation correction
- T1/T2 share the exact horizontal timeline geometry used by playhead/loop markers.
- The old ruler-only 8 dp right shrink is removed.
- The dedicated-looking Trim strip is removed: ruler height is 20 dp and directly touches the marker rail.
- During Cut, yellow T1/T2 lines and precise labels overlay the existing marker rail/ruler with higher visual priority than playhead/loop.
- Trim handles remain in the waveform and independently draggable.
- Physical-editing instrumentation retains both handle behavior and validates the compact/aligned timeline presentation.

### Regression status
At #625:
- `AutoSectionsSlotInstrumentedTest.dockedPracticeControlsRenderAsBalancedComparisonAdjustmentsAndTimelineSegments`: PASS;
- `PhysicalEditingHardeningInstrumentedTest`: PASS;
- `MixerDockInstrumentedTest`: PASS;
- `GuitarLabLifecycleInstrumentedTest`: PASS;
- overall API36 suite: **22/22 PASS**;
- isolated target-tablet geometry: PASS.

H16 source-part decoded SHA-256:
`40a4056644707c57291dfd876fde8487d8dbe9c436ba0409ccb7c6a60c31a0bb`

## Physical homologation boundary
The exact #625 signed APK is now the intended physical-homologation candidate. Its embedded build identity correctly records `gate=software+android-integration-passed;physical-validation-pending`.

No additional CI run is required unless product/source code changes. Final approval now requires only the residual real-device checklist in `RC3_FINAL_PHYSICAL_HOMOLOGATION.md`, focused on MK-300 routing/REC/meters/synchronization, interaction feel, visual readability, listening/perception and absence of perceptible Studio-return flicker.
