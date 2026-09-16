# H25 — UI interaction shape, calibration modal and project-delete safety

Updated: 2026-09-16

Status: **IMPLEMENTED / SOURCE-VALIDATED / PRE-GATE**.

Last signed authority remains CI #641 / run `35105065689` / producer `b11769f340f7056c37dfb17d95b062909dad87bf`, authoritative through H24a only.

## Scope
H25 is a focused UX/safety block with no audio-DSP, project-schema or `.guitarlab` format change.

### Rounded-square interaction feedback
`AppIconButton` now makes the visible rounded-square `Surface` the actual clickable control. Hover, press, focus and ripple indication therefore use the same 6 dp rounded-square silhouette as the button chassis instead of the circular `IconButton` interaction shape.

### Calibration modal
The main Options page keeps only a compact calibration summary and a `Calibração` action. Detailed calibration state and controls move into a dedicated modal:
- selected input/output and session sample rate;
- calibration status, median latency, jitter, drift and confidence when available;
- accepted automatic route compensation;
- route/rate-scoped residual fine adjustment (`-5`, `-1`, `Zerar`, `+1`, `+5 ms`);
- loopback guidance, progress and calibration action.

Calibration remains optional. REC is not blocked merely because the route is uncalibrated; unstable calibration is not applied.

### Diagnostics organization
Duplicate diagnostic entry points were removed from Audio/Import sections. A single `Diagnóstico` section now groups:
- Audio and devices;
- Codecs and files.

`Atualizar dispositivos de áudio` remains in the Audio section because it is an operational route refresh, not a diagnostic workflow.

### Project deletion confirmation
The Home project menu no longer invokes deletion directly. Choosing `Excluir` opens a destructive confirmation dialog that:
- identifies the project by name;
- states that the project and managed files will be removed;
- warns that the action cannot be undone;
- offers `Cancelar` before the destructive action;
- invokes repository deletion only after explicit confirmation.

## Android tests added
- `ProjectDeleteConfirmationInstrumentedTest`
  - Cancel does not invoke the destructive callback;
  - explicit confirmation invokes it exactly once.
- `SettingsCalibrationModalInstrumentedTest`
  - calibration details are absent from the main Options page;
  - `Calibração` opens the dedicated modal;
  - the modal exposes the optional-calibration contract and closes cleanly.

Do not predeclare the next standard API36 total before the official workflow completes.

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

## Source validation completed
- patch generation from the exact #641 materialized source snapshot: PASS;
- patch whitespace/diff checks: PASS;
- Kotlin parser scan: no syntax/parser diagnostics (Android/Compose classpath unavailable locally);
- deterministic gzip+base64 integrity: PASS;
- first materialization from exact H24a state: PASS;
- second materialization idempotency: PASS;
- reverse patch restores exact H24a files and removes new tests: PASS;
- deterministic reapply restores exact H25 blobs: PASS;
- corrupted source part fails closed: PASS;
- materializer `bash -n`: PASS.

Local Android Gradle/Lint/API36/signing is not claimed because this runtime does not provide the Android SDK/Gradle execution environment. H25 requires one fresh user-dispatched full signed workflow before DIGITAL PASS.
