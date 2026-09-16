# Android CI / release pipeline

Updated: 2026-09-16

## Contract
`.github/workflows/android-ci.yml` is manual-only (`workflow_dispatch`). Commits do not automatically consume hosted CI. The assistant must not dispatch or rerun workflows/jobs without explicit user instruction.

Authority layers:
1. **software gate** — deterministic source materialization, JVM/unit/audio/DSP/persistence/migration tests, performance evidence, Lint, debug/release assembly and unsigned provenance;
2. **API36 gate** — standard connected instrumentation plus isolated target-tablet geometry;
3. **signed homologation** — signs the exact tested unsigned release artifact only after upstream gates pass.

## Current source materialization
`.source-parts/` + `scripts/materialize_ci_sources.sh` are source-of-truth build inputs. Unexpected drift fails closed by exact SHA-256/Git blob checks.

Canonical H26 tail: `… → H25 → H26 → H26a → H26b → H26e`.

Relevant inputs:
- H26 archive: `.source-parts/H26SafCloudBackup.patch.gz.part00` through `.part04`;
- H26a: `.source-parts/H26aSafCopyPackageContract.patch.b64`;
- H26b: `.source-parts/H26bBackupScreenTestCompat.patch.b64`;
- H26e: `.source-parts/H26eBackupScreenInstrumentedTest.kt.b64`.

H26e decoded-source SHA-256: `7f974aef7ad8b4052cd01b6a66ffe75cee78fb27c5e94585b9eda44e2bf322da`.
Expected final `BackupScreenInstrumentedTest.kt` Git blob: `8ebac066a3bef1b93ee0316cdb5dcc726fe4b056`.
Expected terminal message: `Source patch chain materialized through H26e with verified final hashes`.

H26c/H26d were recovery experiments around incremental test-source patch materialization and are historical only. The current materializer does not depend on that fragile path; H26e installs the complete validated test source deterministically.

## Current signed authority — CI #650
CI #650 / run `35154021384` / producer `07c99155789774cb39f9b4382829f9e1d16649e3` is the current signed DIGITAL PASS through H26/H26e.

Software gate:
- 285/285 JVM/unit PASS; 0 failures/errors/skips;
- performance evidence PASS;
- Android Lint PASS with 44 warnings + 3 hints, 0 errors in generated reports;
- `assembleDebug` PASS;
- `assembleRelease` PASS;
- unsigned package/version/source provenance PASS.

Android integration gate:
- **31/31 standard API36 PASS**;
- **1/1 isolated 1920×1200 geometry PASS**.

Do not add these counts together in canonical reporting.

Signed homologation:
- unsigned APK SHA-256 `c536ba4af1b9fbe679caedd43d41e6e43a9f1c999301a8757f55a180acf4ce8d`;
- signed APK SHA-256 `fdb870d0996b7ee4ec6f72028af9c7291d4767b4303cbd5fa62894288757d2fe`;
- signed APK size `13,737,498` bytes;
- package `studio.guitarlab.app`;
- version `0.5.0-rc3` / code `23`;
- signer certificate SHA-256 `4B82890A9812BB89E1BBEF179A48752BDA2CA8AB284C833DAA27A907F4CE5E89`;
- 1 signer, RSA-4096;
- v2 signature verified; v1/v3/v3.1/v4 false; SourceStamp false;
- signing-material cleanup PASS.

## Artifact identity
GitHub artifact ZIP digests are transport/container digests and are distinct from the APK SHA-256 values above.

#650 artifacts:
- software gate ID `10469808002`, ZIP digest `sha256:aa8e0ac54063092eeb42f4d565e789c52ca47de8b5c5c749046590d60234d159`;
- Android integration ID `10469853528`, ZIP digest `sha256:c5db642096c5c6bdd2cbfd9c7b7b78b439342875f120c208d2c1d25c9317916e`;
- exact source snapshot ID `10469972661`, ZIP digest `sha256:40dae16263fd4fbbcb602cd8ec90d66622e9ca74637547750d875ade83f275fc`;
- signed homologation ID `10470008288`, ZIP digest `sha256:d8e426e7da20c0509d4c18fe9fb4ef5e491c438efaf7596100ade5c1a75e5a6d`;
- unsigned release ID `10470177531`, ZIP digest `sha256:abf2361077e9ddec669f674e2908bfc32a0080af17b1f8bd2a37a097b3d97069`.

## Known non-blocking CI observations
#650 logs contain non-fatal Android SDK XML/tooling warnings, one transient emulator offline/startup recovery, emulator graphics `ColorBuffer` messages, emulator shutdown `stop: Not implemented`, Kotlin/Compose/MediaCodec deprecation warnings and a Node `Buffer()` deprecation inside artifact download tooling. None caused a test/build/signing failure in #650. These observations do not replace target-device physical validation.

## Artifact identity discipline
The signed authority SHA is always the exact workflow producer SHA. Later documentation-only commits never change an existing APK producer identity. Detailed #650 audit: `H26E_CI650_DIGITAL_PASS.md`.