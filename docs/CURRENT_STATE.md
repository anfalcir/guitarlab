# Current State — GuitarLab Studio

Updated: 2026-09-16

## Active line
- Repository/branch: `anfalcir/guitarlab` / `main`.
- Version line: `0.5.0-rc3`, versionCode `23`, package `studio.guitarlab.app`.
- Current signed DIGITAL PASS: **CI #650** / run `35154021384` / exact APK producer source `07c99155789774cb39f9b4382829f9e1d16649e3`.
- H26/H26e status: **DIGITAL PASS**.
- Unsigned APK SHA-256: `c536ba4af1b9fbe679caedd43d41e6e43a9f1c999301a8757f55a180acf4ce8d`.
- Signed APK SHA-256: `fdb870d0996b7ee4ec6f72028af9c7291d4767b4303cbd5fa62894288757d2fe`.
- Signed APK size: `13,737,498` bytes.
- Locked signer SHA-256: `4B82890A9812BB89E1BBEF179A48752BDA2CA8AB284C833DAA27A907F4CE5E89`.
- Signed artifact ID: `10470008288`, name `GuitarLabStudio-0.5.0-rc3-homologacao`.
- Unsigned artifact ID: `10470177531`.
- `.github/workflows/android-ci.yml` remains manual-only (`workflow_dispatch`).

## Producer identity vs repository HEAD
The signed H26 candidate is permanently bound to producer `07c99155789774cb39f9b4382829f9e1d16649e3`. Documentation-only commits after CI #650 may advance `main`; they do **not** change the producer SHA or bytes of the audited APK.

## CI #650 authoritative evidence
Workflow `GuitarLab Android CI`, manually dispatched (`workflow_dispatch`), completed SUCCESS in all three jobs:
1. `Unit tests + Lint + APK build` — SUCCESS;
2. `API 36 emulator regression` — SUCCESS;
3. `Signed homologation APK` — SUCCESS.

Audited results:
- source materialization through H26e with verified final hashes: PASS;
- JVM/unit: **285/285 PASS**, 0 failures, 0 errors, 0 skipped;
- performance evidence: PASS and evidence emitted for Small/Medium/Large workloads;
- Android Lint: PASS; generated reports contain **44 warnings + 3 hints, 0 errors**, so this is intentionally not described as “zero warnings”;
- `assembleDebug`: PASS;
- `assembleRelease`: PASS;
- unsigned candidate package/version/source provenance: PASS;
- standard API36: **31/31 PASS**;
- isolated 1920×1200 geometry: **1/1 PASS**;
- signed candidate provenance/package/version/zipalign/signature/certificate: PASS;
- signing material cleanup: PASS.

Canonical Android reporting for #650 is **31/31 standard + 1/1 isolated geometry**. The two counts must remain separate.

## H26/H26e — SAF Cloud Backup — DIGITAL PASS
Digitally proven contract includes:
- provider-neutral Android SAF tree selection with persistable read/write permission;
- real read/write/delete probe before adopting a target folder;
- safe target-folder replacement/disconnect without deleting previous remote content;
- full backup and single-project backup;
- restore of one selected version and restore-all latest valid version per project;
- restore always creates an independent local project and never silently overwrites an existing project;
- automatic incremental backup with coalescing plus periodic WorkManager safety work;
- configurable network/charging constraints plus battery/storage constraints;
- `dataSync` foreground execution for long automatic transfers;
- one process-wide operation lock across manual/automatic backup and restore;
- transactional remote version layout: metadata + package + `COMMITTED` written last;
- remote package re-read and exact byte-count + SHA-256 validation before commit;
- restore revalidation of byte count + SHA-256 before import;
- invalid/incomplete versions excluded from restore;
- retention ages 7/30/60/90/180/365 days or permanent and protected minimum 1/3/5/10 versions per project;
- protected minimum overriding age expiration;
- cleanup suppression on total upload failure, per-project protection on partial failure and cancellation propagation;
- dedicated Backup/Restauração screen, Settings summary, Home shortcut and synchronized Help.

H26a fixed only the SAF `copyPackage()` `Unit` contract. H26b removed the unavailable Compose `assertExists` API from the instrumentation test. H26e replaced the fragile H26c/H26d patch path with deterministic full-source materialization of `BackupScreenInstrumentedTest.kt`.

H26e integrity:
- decoded source SHA-256: `7f974aef7ad8b4052cd01b6a66ffe75cee78fb27c5e94585b9eda44e2bf322da`;
- final Git blob: `8ebac066a3bef1b93ee0316cdb5dcc726fe4b056`;
- terminal materializer message: `Source patch chain materialized through H26e with verified final hashes`.

## Signed candidate identity
`BUILD_IDENTITY.txt` from #650 binds the signed APK to the exact producer and records software+Android integration passed while physical validation remains pending. Full details live in `H26E_CI650_DIGITAL_PASS.md`.

## Remaining gate
RC3 is **not FINAL**. Remaining evidence is physical/provider-specific only: Google Drive/SAF picker and persisted permission behavior on SM-X230, real remote backup/restore/folder switching/revocation/cancel/large-transfer observations, plus retained MK-300 recording/timing/audio-route/edit/save/reopen/export smoke checks. Final closure also requires no repeatable P0/P1 and explicit user approval of the exact signed APK hash above.