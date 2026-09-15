# Android CI / release pipeline

Updated: 2026-09-15

## Contract
`.github/workflows/android-ci.yml` is manual-only (`workflow_dispatch`). Commits never auto-consume hosted CI. The assistant must not dispatch or rerun the workflow.

The pipeline has three authority layers:
1. software gate — materialization, JVM/unit/audio/DSP/persistence/migration, performance, Lint, debug/release and unsigned provenance;
2. API36 gate — complete connected instrumentation plus isolated 1920×1200 geometry;
3. signed homologation — signs the exact tested unsigned artifact only after both mandatory gates pass.

## Source materialization
`.source-parts/` plus `scripts/materialize_ci_sources.sh` are part of the build contract. Unexpected source drift fails closed.

Canonical hardening order:
`H1 → H2 → H3 → H4 → H5 → H6 → H7 → H8 → H9 → H10 → H11 → H11a → H11b → H12 engine → H12 UI → H13 → H14 → H14a → H15 → H16`.

Final source tail:
- `.source-parts/H12LevelEngine.patch`
- `.source-parts/H12LevelUi.patch`
- `.source-parts/H13TrimRuler.patch`
- `.source-parts/H14MixerHorizontalScroll.patch`
- `.source-parts/H14aMixerScrollViewportRegression.patch`
- `.source-parts/H15ResidentStudioReturn.patch`
- `.source-parts/H16FinalUiTrimOverlay.patch.gz`

`apply_patch_once` accepts either a clean forward application or an exact reverse dry-run indicating the patch is already present. Overlapping historical stages retain explicit final-blob guards.

## Canonical evidence — CI #625
CI #625 / run `35010012582` / source `476fa740408130adf6a4e9665d166e724a9184dd` is the authoritative signed DIGITAL PASS through H16.

Evidence:
- materialization including H16: PASS;
- complete software/performance/Lint/build/provenance: PASS;
- API36 connected regression: **22/22 PASS**;
- isolated 1920×1200 target-tablet geometry: PASS;
- signed homologation: PASS;
- unsigned APK SHA-256: `46750bb10e70c70d350011aa46411d2cafb7766746c20b6e83add100b6f8055a`;
- signed APK SHA-256: `107795f040ed18246bb130a9519044ba7e07f334a5835566b952cbc7fdb528e6`;
- certificate SHA-256: `4B82890A9812BB89E1BBEF179A48752BDA2CA8AB284C833DAA27A907F4CE5E89`;
- signed artifact ID: `10412879614`;
- Android integration artifact ID: `10413311710`.

H16-specific connected evidence includes:
- balanced `Comparação | Ajustes | Timeline` practice-bar regression: PASS;
- neutral comparison label `Desativado`: PASS;
- Níveis placement in Ajustes: PASS;
- compact/aligned Trim rail/ruler contract retained by physical-editing regression: PASS.

CI #624 remains useful historical regression evidence for the pre-H16 state but is superseded by #625 as the active digitally homologated candidate.

## Artifact identity discipline
The signed job checks out the exact workflow SHA, downloads the exact tested unsigned candidate, verifies its source/package/version/checksum, then signs without rebuilding.

For #625, `BUILD_IDENTITY.txt` records:
- `commit=476fa740408130adf6a4e9665d166e724a9184dd`
- `package=studio.guitarlab.app`
- `versionName=0.5.0-rc3`
- `versionCode=23`
- `unsignedApkSha256=46750bb10e70c70d350011aa46411d2cafb7766746c20b6e83add100b6f8055a`
- `signedApkSha256=107795f040ed18246bb130a9519044ba7e07f334a5835566b952cbc7fdb528e6`
- `gate=software+android-integration-passed;physical-validation-pending`
- `certificateSha256=4B82890A9812BB89E1BBEF179A48752BDA2CA8AB284C833DAA27A907F4CE5E89`

## Next execution rule
No further CI run is needed unless product/source code changes.

Documentation-only promotion commits use `[skip ci]` and must never be confused with the exact source SHA that produced the signed candidate.
