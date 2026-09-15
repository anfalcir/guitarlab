# Current State — GuitarLab Studio

Updated: 2026-09-15

## Active candidate
- Repository/branch: `anfalcir/guitarlab` / `main`.
- Version: `0.5.0-rc3`, versionCode `23`, package `studio.guitarlab.app`.
- Canonical digitally homologated source SHA: `476fa740408130adf6a4e9665d166e724a9184dd`.
- Canonical PASS: CI #625 / run `35010012582`, manual `workflow_dispatch`.
- Signed APK SHA-256: `107795f040ed18246bb130a9519044ba7e07f334a5835566b952cbc7fdb528e6`.
- Unsigned APK SHA-256: `46750bb10e70c70d350011aa46411d2cafb7766746c20b6e83add100b6f8055a`.
- Locked signer SHA-256: `4B82890A9812BB89E1BBEF179A48752BDA2CA8AB284C833DAA27A907F4CE5E89`.
- Signed artifact: `GuitarLabStudio-0.5.0-rc3-homologacao`, artifact ID `10412879614`.
- Android integration artifact: `guitarlab-476fa740408130adf6a4e9665d166e724a9184dd-android-integration`, artifact ID `10413311710`.
- `.github/workflows/android-ci.yml` remains manual-only (`workflow_dispatch`).

## Evidence boundary
CI #625 is now the authoritative signed **DIGITAL PASS** for the complete RC3 digital gate through H16. It supersedes #624 as the active digital homologation baseline. Physical validation remains pending and is the only authority for real tablet/MK-300/perception claims.

## CI #625 — canonical full PASS
Exact source `476fa740408130adf6a4e9665d166e724a9184dd`:
- source materialization including H16: **PASS**;
- unit/core/audio/DSP/persistence/migration regression: **PASS**;
- reproducible performance evidence: **PASS**;
- Android Lint: **PASS**;
- debug + release assembly and unsigned provenance: **PASS**;
- API 36 connected regression: **22/22 PASS**;
- isolated 1920×1200 tablet geometry: **PASS**;
- signed homologation: **PASS**;
- package/version/source/signer/checksum validation: **PASS**.

Canonical signed identity:
- package: `studio.guitarlab.app`;
- versionName: `0.5.0-rc3`;
- versionCode: `23`;
- source SHA: `476fa740408130adf6a4e9665d166e724a9184dd`;
- unsigned APK SHA-256: `46750bb10e70c70d350011aa46411d2cafb7766746c20b6e83add100b6f8055a`;
- signed APK SHA-256: `107795f040ed18246bb130a9519044ba7e07f334a5835566b952cbc7fdb528e6`;
- certificate SHA-256: `4B82890A9812BB89E1BBEF179A48752BDA2CA8AB284C833DAA27A907F4CE5E89`;
- APK Signature Scheme v2: verified;
- signers: 1, RSA 4096.

## H16 — final UI / Trim overlay polish — DIGITAL PASS

### Practice bar
- neutral comparison state is labeled **`Desativado`**;
- the independent top-bar Mixer panel toggle remains **`Mixer`**;
- `Níveis` is in a dedicated center block **`Ajustes`**;
- docked order is **Comparação | Ajustes | Timeline**;
- proportional layout remains `0.34 / 0.16 / 0.50` so Ajustes is compact and Timeline retains space;
- `AutoSectionsSlotInstrumentedTest.dockedPracticeControlsRenderAsBalancedComparisonAdjustmentsAndTimelineSegments` passed at #625.

### Trim overlay
- T1/T2 use the same horizontal timeline geometry as playhead/loop;
- the old ruler-only 8 dp shrink is removed;
- the ruler is compact at 20 dp;
- marker rail and ruler are directly adjacent, with no dedicated Trim lane;
- while Cut is active, T1/T2 line/label overlay the existing rail/ruler with priority over competing playhead/loop presentation;
- waveform Trim handles remain independently draggable;
- retained `PhysicalEditingHardeningInstrumentedTest` passed at #625.

## H16 source representation
Source part: `.source-parts/H16FinalUiTrimOverlay.patch.gz`

Decoded patch SHA-256:
`40a4056644707c57291dfd876fde8487d8dbe9c436ba0409ccb7c6a60c31a0bb`

Materialization order after H15:
`H16FinalUiTrimOverlay.patch.gz`.

## Milestone state
- M2–M6: PASS/CLOSED.
- M7: complete digital gate through H16 is **DIGITAL PASS** at #625; final physical closure remains pending.
- M8: RC3 hardening through H16 is **DIGITAL PASS** at #625.

## Residual physical gate
No further CI rerun is required unless product/source code changes. Install the exact #625 signed APK and execute `RC3_FINAL_PHYSICAL_HOMOLOGATION.md`.

Physical review now focuses only on facts automation cannot establish: visual harmony/readability on the real tablet, T1/T2 overlay feel against playhead/loop, natural Mixer swipe with fixed MASTER, absence of visible Studio-return flicker, MK-300 routing/REC/meters/synchronization and listening/perception smoke.

The signed artifact correctly records `gate=software+android-integration-passed;physical-validation-pending` until explicit physical approval.
