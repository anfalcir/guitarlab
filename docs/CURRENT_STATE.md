# Current State — GuitarLab Studio

Updated: 2026-09-15

## Active candidate
- Repository/branch: `anfalcir/guitarlab` / `main`.
- Version: `0.5.0-rc3`, versionCode `23`, package `studio.guitarlab.app`.
- Canonical digitally homologated source SHA: `f187ab2ba7596c4aa04d223f007409b2fb39f490`.
- Canonical PASS: CI #626 / run `35017084625`, manual `workflow_dispatch`.
- Signed APK SHA-256: `93a7ed1ebfdedf7421cf21183db84a529d2d095c9564caafc87952506cb5426b`.
- Unsigned APK SHA-256: `28578bab8b76a611aa1b0cd92f8aa0b428826526779e1757ab45b5b34ce254b9`.
- Locked signer SHA-256: `4B82890A9812BB89E1BBEF179A48752BDA2CA8AB284C833DAA27A907F4CE5E89`.
- Signed artifact ID: `10415074932`.
- Android integration artifact ID: `10416645621`.
- `.github/workflows/android-ci.yml` remains manual-only (`workflow_dispatch`).

## Evidence boundary
CI #626 is the authoritative signed **DIGITAL PASS** for the complete RC3 digital gate through H17. It supersedes #625 as the active digitally homologated baseline. Physical validation remains pending and is the only authority for real tablet/MK-300/perception claims.

## CI #626 — canonical full PASS
Exact source `f187ab2ba7596c4aa04d223f007409b2fb39f490`:
- source materialization through H17: **PASS**;
- unit/core/audio/DSP/persistence/migration regression: **PASS**;
- reproducible performance evidence: **PASS**;
- Android Lint: **PASS**;
- debug + release assembly and unsigned provenance: **PASS**;
- API 36 connected regression: **22/22 PASS**;
- isolated 1920×1200 tablet geometry: **1/1 PASS**;
- signed homologation: **PASS**;
- package/version/source/signer/checksum validation: **PASS**.

Canonical signed identity:
- package: `studio.guitarlab.app`;
- versionName: `0.5.0-rc3`;
- versionCode: `23`;
- source SHA: `f187ab2ba7596c4aa04d223f007409b2fb39f490`;
- unsigned APK SHA-256: `28578bab8b76a611aa1b0cd92f8aa0b428826526779e1757ab45b5b34ce254b9`;
- signed APK SHA-256: `93a7ed1ebfdedf7421cf21183db84a529d2d095c9564caafc87952506cb5426b`;
- certificate SHA-256: `4B82890A9812BB89E1BBEF179A48752BDA2CA8AB284C833DAA27A907F4CE5E89`;
- APK Signature Scheme v2: verified;
- signers: 1, RSA 4096.

## H17 — CUT ruler + Ajustes spacing — DIGITAL PASS

### CUT
- visible T1/T2 time boxes are removed;
- CUT markers are short yellow ticks confined to the **time ruler**;
- ticks do not extend into the sections/playhead rail;
- horizontal projection remains the canonical timeline geometry;
- waveform Trim handles remain the editing controls.

### Practice bar
- `Comparação | Ajustes | Timeline` remains the three-block structure;
- `Ajustes + Níveis` is centered inside the dedicated center segment;
- Níveis remains fully contained in Ajustes;
- left/right breathing room around the center cluster is approximately symmetric.

### Regression evidence at #626
- `AutoSectionsSlotInstrumentedTest.dockedPracticeControlsRenderAsBalancedComparisonAdjustmentsAndTimelineSegments`: **PASS**;
- `PhysicalEditingHardeningInstrumentedTest.trimHandlesAreIndependentlyDraggableAndClipDeleteRequiresConfirmation`: **PASS**;
- overall connected API36 suite: **22/22 PASS**;
- isolated target-tablet geometry: **1/1 PASS**.

## H17 source representation
Source part: `.source-parts/H17CutRulerPracticeSpacing.patch`

Patch SHA-256:
`38b3f494cf528fcc9fc818e6ef38ed0647389e106ec1bcc821e00ee2e65278dc`

Materialization order tail:
`H16FinalUiTrimOverlay.patch.gz → H17CutRulerPracticeSpacing.patch`.

## Milestone state
- M2–M6: PASS/CLOSED.
- M7: complete digital gate through H17 is **DIGITAL PASS** at #626; final physical closure remains pending.
- M8: RC3 hardening through H17 is **DIGITAL PASS** at #626.

## Residual physical gate
No further CI rerun is required unless product/source code changes. Install the exact #626 signed APK and execute `RC3_FINAL_PHYSICAL_HOMOLOGATION.md`.

Physical review now focuses only on facts automation cannot establish: CUT ruler readability on the real tablet, visual balance of Ajustes/Níveis, natural Mixer swipe with fixed MASTER, absence of visible Studio-return flicker, MK-300 routing/REC/meters/synchronization and listening/perception smoke.

The signed artifact correctly records `gate=software+android-integration-passed;physical-validation-pending` until explicit physical approval.
