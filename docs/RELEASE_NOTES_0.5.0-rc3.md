# GuitarLab Studio 0.5.0-rc3

Updated: 2026-09-16

## Current signed digital homologation — CI #650
Run `35154021384`, producer `07c99155789774cb39f9b4382829f9e1d16649e3`, is the current signed DIGITAL PASS through H26/H26e.

Identity:
- package `studio.guitarlab.app`;
- version `0.5.0-rc3` / versionCode `23`;
- unsigned APK SHA-256 `c536ba4af1b9fbe679caedd43d41e6e43a9f1c999301a8757f55a180acf4ce8d`;
- signed APK SHA-256 `fdb870d0996b7ee4ec6f72028af9c7291d4767b4303cbd5fa62894288757d2fe`;
- signed APK size `13,737,498` bytes;
- certificate SHA-256 `4B82890A9812BB89E1BBEF179A48752BDA2CA8AB284C833DAA27A907F4CE5E89`;
- one RSA-4096 signer;
- APK Signature Scheme v2 verified; v1/v3/v3.1/v4 not used.

CI #650 evidence:
- 285/285 JVM/unit PASS, 0 failures/errors/skips;
- performance evidence PASS;
- Android Lint PASS with 44 warnings + 3 hints, 0 errors in generated reports;
- debug/release build and unsigned provenance PASS;
- API36 **31/31 standard + 1/1 isolated 1920×1200 geometry**;
- signed provenance/package/version/zipalign/signature/certificate PASS;
- signing material cleanup PASS.

## H25 retained behavior
H25 contains rounded-square interaction feedback, dedicated calibration modal, consolidated diagnostics and explicit project-delete confirmation.

## H26/H26e — DIGITAL PASS
RC3 now contains and digitally validates:
- user-selected SAF target folder with persistable scoped permission and real read/write/delete probe;
- manual full backup and per-project backup;
- automatic incremental/coalesced + periodic WorkManager backup;
- single-version and latest-per-project full restore as independent local copies;
- transactional remote versions with remote re-read, SHA-256/byte validation and strict `COMMITTED` marker;
- configurable cadence/network/charging and target-folder switching/disconnect without deleting old cloud data;
- retention by age plus a minimum protected version count per project;
- cleanup suppression/protection on full or partial upload failure and proper cancellation propagation;
- dedicated Backup/Restauração UI, Settings summary, Home per-project shortcut and synchronized Help.

H26a/H26b are narrow compile/test-compatibility correctives. H26e is the deterministic full-source materialization of the corrected Backup-screen instrumentation test, with decoded SHA-256 `7f974aef7ad8b4052cd01b6a66ffe75cee78fb27c5e94585b9eda44e2bf322da` and final Git blob `8ebac066a3bef1b93ee0316cdb5dcc726fe4b056`.

## Release status
This is **not** an RC3 FINAL declaration. The exact #650 signed APK is ready for residual physical homologation on SM-X230/Google Drive SAF and retained MK-300/audio/editing smoke tests. Final approval requires no repeatable P0/P1 and explicit user approval of this exact signed APK SHA-256.