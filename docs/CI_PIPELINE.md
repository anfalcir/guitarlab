# Android CI / release pipeline

Updated: 2026-09-15

## Contract
`.github/workflows/android-ci.yml` is manual-only (`workflow_dispatch`). Commits do not auto-consume hosted CI, and the assistant must not dispatch or rerun the workflow.

The pipeline has three authority layers:
1. software gate — materialization, JVM/unit/audio/DSP/persistence/migration, performance, Lint, debug/release and unsigned provenance;
2. API36 gate — complete connected instrumentation plus isolated 1920×1200 geometry;
3. signed homologation — signs the exact tested unsigned artifact only after both mandatory gates pass.

## Source materialization
`.source-parts/` plus `scripts/materialize_ci_sources.sh` are part of the build contract. Unexpected source drift fails closed.

Canonical hardening order:
`H1 → H2 → H3 → H4 → H5 → H6 → H7 → H8 → H9 → H10 → H11 → H11a → H11b → H12 engine → H12 UI → H13 → H14 → H14a → H15 → H16 → H17`.

Final source tail:
- `.source-parts/H12LevelEngine.patch`
- `.source-parts/H12LevelUi.patch`
- `.source-parts/H13TrimRuler.patch`
- `.source-parts/H14MixerHorizontalScroll.patch`
- `.source-parts/H14aMixerScrollViewportRegression.patch`
- `.source-parts/H15ResidentStudioReturn.patch`
- `.source-parts/H16FinalUiTrimOverlay.patch.gz`
- `.source-parts/H17CutRulerPracticeSpacing.patch`

## Canonical evidence — CI #626
CI #626 / run `35017084625` / source `f187ab2ba7596c4aa04d223f007409b2fb39f490` is the authoritative signed DIGITAL PASS through H17.

Evidence:
- materialization through H17: PASS;
- complete software/performance/Lint/build/provenance: PASS;
- API36 connected regression: **22/22 PASS**;
- isolated 1920×1200 target-tablet geometry: **1/1 PASS**;
- signed homologation: PASS;
- unsigned APK SHA-256: `28578bab8b76a611aa1b0cd92f8aa0b428826526779e1757ab45b5b34ce254b9`;
- signed APK SHA-256: `93a7ed1ebfdedf7421cf21183db84a529d2d095c9564caafc87952506cb5426b`;
- certificate SHA-256: `4B82890A9812BB89E1BBEF179A48752BDA2CA8AB284C833DAA27A907F4CE5E89`;
- signed artifact ID: `10415074932`;
- Android integration artifact ID: `10416645621`.

H17-specific connected evidence includes:
- centered Ajustes/Níveis practice-bar regression: PASS;
- Níveis fully contained inside Ajustes: PASS;
- visible T1/T2 time labels absent: PASS;
- CUT ticks constrained to the time-ruler bounds with exact X projection: PASS.

CI #625 remains useful historical evidence for H16 but is superseded by #626 as the active digitally homologated candidate.

## Artifact identity discipline
The signed job checks out the exact workflow SHA, downloads the exact tested unsigned candidate, verifies its source/package/version/checksum, then signs without rebuilding.

For #626, `BUILD_IDENTITY.txt` records:
- `commit=f187ab2ba7596c4aa04d223f007409b2fb39f490`
- `package=studio.guitarlab.app`
- `versionName=0.5.0-rc3`
- `versionCode=23`
- `unsignedApkSha256=28578bab8b76a611aa1b0cd92f8aa0b428826526779e1757ab45b5b34ce254b9`
- `signedApkSha256=93a7ed1ebfdedf7421cf21183db84a529d2d095c9564caafc87952506cb5426b`
- `gate=software+android-integration-passed;physical-validation-pending`
- `certificateSha256=4B82890A9812BB89E1BBEF179A48752BDA2CA8AB284C833DAA27A907F4CE5E89`

## Next execution rule
No further CI run is needed unless product/source code changes.

Documentation-only promotion commits use `[skip ci]` and must never be confused with the exact source SHA that produced the signed candidate.
