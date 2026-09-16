# H26e / CI #650 — Digital PASS evidence

Updated: 2026-09-16
Status: **AUTHORITATIVE DIGITAL PASS EVIDENCE FOR H26/H26e**

## Workflow identity
- workflow: `GuitarLab Android CI`;
- run number: `650`;
- run ID: `35154021384`;
- event: `workflow_dispatch`;
- conclusion: `SUCCESS`;
- exact producer/head SHA: `07c99155789774cb39f9b4382829f9e1d16649e3`;
- package: `studio.guitarlab.app`;
- versionName: `0.5.0-rc3`;
- versionCode: `23`.

All three jobs completed SUCCESS:
1. `Unit tests + Lint + APK build`;
2. `API 36 emulator regression`;
3. `Signed homologation APK`.

## H26e materialization proof
Current canonical tail: `… → H25 → H26 → H26a → H26b → H26e`.

H26e replaces fragile incremental test-patch recovery with deterministic full-source installation of `BackupScreenInstrumentedTest.kt`.
- source part: `.source-parts/H26eBackupScreenInstrumentedTest.kt.b64`;
- decoded source SHA-256: `7f974aef7ad8b4052cd01b6a66ffe75cee78fb27c5e94585b9eda44e2bf322da`;
- expected/final Git blob: `8ebac066a3bef1b93ee0316cdb5dcc726fe4b056`;
- #650 terminal message: `Source patch chain materialized through H26e with verified final hashes`.

## JVM/unit evidence
XML-derived aggregate across Gradle test result suites:
- total: **285**;
- passed: **285**;
- failed: **0**;
- errors: **0**;
- skipped: **0**.

Module totals:
- `app`: 24;
- `core/audio`: 55;
- `core/codec`: 30;
- `core/model`: 17;
- `core/project`: 150;
- `platform/audio-android`: 9.

## Performance evidence
The passing performance suite emitted:
- `Small`: tracks=5, clips=10, save/load=0.970107 ms, bundle=0.368247 ms, reopen=3.146647 ms, json=7829 B, bundle=1052 B, heap delta=524312 B;
- `Medium`: tracks=12, clips=50, save/load=6.29094 ms, bundle=0.781742 ms, reopen=3.616526 ms, json=33463 B, bundle=2059 B, heap delta=524264 B;
- `Large`: tracks=24, clips=120, save/load=3.042199 ms, bundle=1.407318 ms, reopen=3.293228 ms, json=78427 B, bundle=3749 B, heap delta=1589240 B.

These are CI evidence values, not claims about physical-device audio latency.

## Android Lint and build
- Android Lint: **PASS**.
- Generated Lint XML reports contain 44 `Warning` + 3 `Hint`, 0 `Error`; therefore #650 is not described as “zero warnings”.
- `assembleDebug`: **PASS**.
- `assembleRelease`: **PASS**.

## Android integration
Diagnostic copies preserve the two runs independently:
- standard API36 connected suite: **31/31 PASS**, 0 failures/errors/skips;
- isolated 1920×1200 target geometry: **1/1 PASS**, 0 failures/errors/skips.

Canonical reporting is **31/31 standard + 1/1 isolated geometry**. Do not sum these counts.

The standard suite includes three H26 Backup-screen tests:
- project and total backup remain distinct actions;
- single-version restore requires explicit confirmation;
- restore-all requires explicit confirmation.

## Unsigned candidate and provenance
Unsigned release APK:
- file: `app-release-unsigned.apk`;
- real APK SHA-256: `c536ba4af1b9fbe679caedd43d41e6e43a9f1c999301a8757f55a180acf4ce8d`;
- size: `13,729,406` bytes;
- package/version/source identity: PASS;
- confirmed unsigned before signing.

`RELEASE_BUILD_IDENTITY.txt`:
```text
sourceSha=07c99155789774cb39f9b4382829f9e1d16649e3
packageName=studio.guitarlab.app
versionName=0.5.0-rc3
versionCode=23
```

The signed job downloaded the tested unsigned artifact with digest validation and rechecked source SHA, package, versionName and versionCode against the workflow producer/current source before signing.

## Signed APK identity
Signed file: `GuitarLabStudio-0.5.0-rc3-homologacao.apk`.
- real APK SHA-256: `fdb870d0996b7ee4ec6f72028af9c7291d4767b4303cbd5fa62894288757d2fe`;
- real size: `13,737,498` bytes;
- package: `studio.guitarlab.app`;
- versionName: `0.5.0-rc3`;
- versionCode: `23`;
- zipalign: PASS;
- number of signers: **1**;
- signer key algorithm/size: **RSA / 4096 bits**;
- signer certificate SHA-256: `4B82890A9812BB89E1BBEF179A48752BDA2CA8AB284C833DAA27A907F4CE5E89`.

`apksigner` verification:
- v1 (JAR signing): false;
- v2: **true**;
- v3: false;
- v3.1: false;
- v4: false;
- SourceStamp: false.

The pipeline deliberately signs this homologation candidate with v2 only; do not claim v3.

## BUILD_IDENTITY.txt
```text
commit=07c99155789774cb39f9b4382829f9e1d16649e3
package=studio.guitarlab.app
versionName=0.5.0-rc3
versionCode=23
unsignedApkSha256=c536ba4af1b9fbe679caedd43d41e6e43a9f1c999301a8757f55a180acf4ce8d
signedApkSha256=fdb870d0996b7ee4ec6f72028af9c7291d4767b4303cbd5fa62894288757d2fe
milestone=M7-physical-gate+M8-digital-hardening
gate=software+android-integration-passed;physical-validation-pending
hardwareBaseline=M6-alpha1-physical-pass
certificateSha256=4B82890A9812BB89E1BBEF179A48752BDA2CA8AB284C833DAA27A907F4CE5E89
```

## Artifact ZIP identities
These digests identify GitHub artifact ZIP containers and are **not** APK hashes:
- software gate ID `10469808002`: `sha256:aa8e0ac54063092eeb42f4d565e789c52ca47de8b5c5c749046590d60234d159`;
- Android integration ID `10469853528`: `sha256:c5db642096c5c6bdd2cbfd9c7b7b78b439342875f120c208d2c1d25c9317916e`;
- exact source snapshot ID `10469972661`: `sha256:40dae16263fd4fbbcb602cd8ec90d66622e9ca74637547750d875ade83f275fc`;
- signed homologation ID `10470008288`: `sha256:d8e426e7da20c0509d4c18fe9fb4ef5e491c438efaf7596100ade5c1a75e5a6d`;
- unsigned release ID `10470177531`: `sha256:abf2361077e9ddec669f674e2908bfc32a0080af17b1f8bd2a37a097b3d97069`.

## Signing cleanup
Signing material was restored only under the runner temporary signing directory. The explicit cleanup step removed that directory and completed successfully. No signing secret/keystore is part of the uploaded homologation artifact.

## Non-blocking warnings / limitations
Observed but non-fatal in #650:
- Android SDK XML/tooling compatibility warnings;
- a transient emulator ADB/offline startup condition that recovered before tests;
- emulator graphics `ColorBuffer` messages;
- emulator shutdown `stop: Not implemented` message;
- Kotlin/Compose/MediaCodec deprecation warnings;
- Node `Buffer()` deprecation inside artifact-download tooling.

These did not fail the run but should not be misrepresented as a warning-free pipeline.

Digital evidence also cannot substitute for real Google Drive DocumentsProvider behavior, persisted permission after real device restart, real provider revocation, long-transfer/cancel ergonomics or MK-300 audio/timing observations.

## Promotion decision
CI #650 is sufficient to promote **H26/H26e to DIGITAL PASS**. It is the current signed digital authority for RC3.

This does **not** declare RC3 FINAL. Final closure still requires the residual physical/provider/audio checklist, no repeatable P0/P1 and explicit user approval of the exact signed APK SHA-256 `fdb870d0996b7ee4ec6f72028af9c7291d4767b4303cbd5fa62894288757d2fe`.

Any later documentation-only repository HEAD must remain distinguished from the exact APK producer `07c99155789774cb39f9b4382829f9e1d16649e3`.