# H25 — UI interaction shape, calibration modal and project-delete safety

Updated: 2026-09-16

Status: **DIGITAL PASS — CI #642**.

Signed authority: CI #642 / run `35121955150` / exact producer `7bfd876b6a5b0701ab0cf5203de36c31cd117632`.
Signed APK SHA-256: `916758f694735febb8cccfe58f46f290562aaba1b5483ccc727e2be0cefba5aa`.

## Scope
H25 is a focused UX/safety block with no audio-DSP, project-schema or `.guitarlab` format change.

### Rounded-square interaction feedback
`AppIconButton` makes the visible rounded-square `Surface` the clickable control. Hover, press, focus and ripple indication therefore use the same 6 dp rounded-square silhouette as the button chassis instead of the circular `IconButton` interaction shape.

### Calibration modal
The main Options page keeps only a compact calibration summary and a `Calibração` action. Detailed calibration state and controls live in a dedicated modal:
- selected input/output and session sample rate;
- calibration status, median latency, jitter, drift and confidence when available;
- accepted automatic route compensation;
- route/rate-scoped residual fine adjustment (`-5`, `-1`, `Zerar`, `+1`, `+5 ms`);
- loopback guidance, progress and calibration action.

Calibration remains optional. REC is not blocked merely because the route is uncalibrated; unstable calibration is not applied.

### Diagnostics organization
Duplicate diagnostic entry points were removed from Audio/Import sections. A single `Diagnóstico` section groups Audio/devices and Codecs/files. `Atualizar dispositivos de áudio` remains in Audio because it is an operational route refresh rather than a diagnostic workflow.

### Project deletion confirmation
The Home project menu no longer invokes deletion directly. Choosing `Excluir` opens a destructive confirmation dialog that identifies the project, states that managed files are removed, warns that the action cannot be undone, offers `Cancelar`, and invokes repository deletion only after explicit confirmation.

## Focused Android coverage
- `ProjectDeleteConfirmationInstrumentedTest`
  - Cancel does not invoke the destructive callback;
  - explicit confirmation invokes it exactly once.
- `SettingsCalibrationModalInstrumentedTest`
  - calibration details are absent from the main Options page;
  - `Calibração` opens the dedicated modal;
  - the modal exposes the optional-calibration contract and closes cleanly.

The complete #642 standard API36 suite is **28/28 PASS**; isolated 1920×1200 geometry is **1/1 PASS**.

## Materialization
Source part: `.source-parts/H25UiSettingsSafety.patch.gz.part00`.

Expected final message:
`Source patch chain materialized through H25 with verified final hashes`.

Final materialized Git blobs:
- `AppIconButton.kt` — `379bb35ab850eccfaeae10a02356c445ada67881`
- `HomeScreen.kt` — `bfafbaf04d6b316c2a1dc13b73ba17e48df9ff38`
- `SettingsScreen.kt` — `9d1f3c3e7a71d3464cb57b4174117dd561c1f7a3`
- `StudioUserGuideDialog.kt` — `ac76597758c683159599d86a3a20c363447f58c8`
- `ProjectDeleteConfirmationInstrumentedTest.kt` — `c8c5e4346e3dd2d58d762db743af47383276a8ba`
- `SettingsCalibrationModalInstrumentedTest.kt` — `cd186a7fac8b1102663c6fb36620111b14613628`

## Evidence
Pre-CI source validation passed patch integrity, exact H24a→H25 materialization, idempotency, reverse/reapply round-trip, final hashes, corruption fail-closed, shell syntax and diff/parser checks.

CI #642 then supplied the authoritative Android/build evidence on the exact producer SHA:
- 269/269 JVM/unit PASS;
- Lint/build/unsigned provenance PASS;
- 28/28 standard API36 + 1/1 isolated geometry PASS;
- signed homologation and locked signer PASS.

H25 is therefore digitally closed. Remaining evidence is only target-device visual/interaction smoke within the final RC3 physical checklist.
