# Current State — GuitarLab Studio

Updated: 2026-09-16

## Active line
- Repository/branch: `anfalcir/guitarlab` / `main`.
- Version line: `0.5.0-rc3`, versionCode `23`, package `studio.guitarlab.app`.
- Current signed DIGITAL PASS: **CI #642** / run `35121955150` / exact producer source `7bfd876b6a5b0701ab0cf5203de36c31cd117632`, authoritative through H25 only.
- Unsigned APK SHA-256: `f6b4f21d0f514bad06b80eabdad141dac5cd236a707842c21258ec2072868c0e`.
- Signed APK SHA-256: `916758f694735febb8cccfe58f46f290562aaba1b5483ccc727e2be0cefba5aa`.
- Signed APK size: `13,269,914` bytes.
- Locked signer SHA-256: `4B82890A9812BB89E1BBEF179A48752BDA2CA8AB284C833DAA27A907F4CE5E89`.
- Signed artifact ID: `10458236596`, name `GuitarLabStudio-0.5.0-rc3-homologacao`.
- `.github/workflows/android-ci.yml` remains manual-only (`workflow_dispatch`).

## Evidence boundary
CI #642 remains the latest signed authority and covers through **H25**. H26/H26a changes source and is currently **IMPLEMENTED / SOURCE-VALIDATED / PRE-GATE**. Therefore the #642 APK must not be described as containing or validating H26.

## CI #642 retained evidence
- source materialization through H25: PASS;
- JVM/unit: **269/269 PASS**, 0 failures/errors/skips;
- performance evidence: PASS;
- Android Lint: PASS with non-blocking warnings/deprecations;
- debug + release assembly and unsigned provenance: PASS;
- standard API36: **28/28 PASS**;
- isolated 1920×1200 geometry: **1/1 PASS**;
- signed homologation/package/version/signer/cleanup: PASS.

Canonical Android reporting for #642 remains **28/28 standard + 1/1 isolated geometry**.

## H25 — DIGITAL PASS
Rounded-square interaction feedback, calibration modal, diagnostic consolidation and explicit project-delete confirmation are digitally proven at #642. Calibration remains optional; uncalibrated status alone does not block REC.

## H26/H26a — SAF Cloud Backup — PRE-GATE
Implemented source contract:
- provider-neutral Android SAF tree selection with persistable read/write permission;
- real read/write/delete probe before adopting a target folder;
- target-folder change is atomic from the app perspective: the old permission/data are retained until the new target passes probe; changing/disconnecting never deletes cloud content;
- full backup and single-project backup;
- full restore (latest valid version per project) and single-version restore;
- restore always imports as a new independent local project; it never overwrites the current local project;
- automatic incremental backup with coalesced save-triggered work plus periodic safety work;
- WorkManager constraints for network, unmetered network option, charging option, battery-not-low and storage-not-low;
- long-running automatic transfer promoted to a `dataSync` foreground operation;
- one process-wide operation lock serializes manual/automatic backup and restore;
- transactional remote version layout: metadata + package + `COMMITTED` marker;
- remote package is re-read after upload and must match local byte count + SHA-256 before commit;
- restore rechecks byte count + SHA-256 before project import;
- incomplete/invalid-marker versions are invisible to restore;
- retention supports 7/30/60/90/180/365 days or forever plus protected minimum 1/3/5/10 versions per project;
- protected minimum overrides age expiration;
- retention occurs only after a safe run; if every attempted upload fails, deletion is suspended; a project whose upload failed is excluded from that run's cleanup;
- cancellation propagates and cannot be converted into a normal per-project failure followed by cleanup;
- in-app dedicated Backup/Restauração screen plus per-project Home shortcut and clean Settings summary;
- in-app Help synchronized.

Source validation completed before publication:
- `git diff --check`: clean;
- H25→H26 patch applies from the exact H25 materialized baseline;
- second materializer execution is idempotent;
- all 24 H26 final source hashes match and materialized files are byte-identical to the audited workspace;
- H26 source archive is validated on every materializer invocation by base64 decode + gzip CRC + fixed SHA-256; deliberate corruption fails closed;
- core backup policy/coordinator syntax checks and focused runtime policy/route-codec harnesses passed in the available local environment;
- CI #643 failed before build only on `git diff --check` because of three Markdown trailing spaces; corrected without source behavior changes;
- CI #644 passed diff sanity and H26 materialization, then exposed one deterministic Android compile contract mismatch: `SafBackupRemoteStore.copyPackage()` inferred `Long` while `ProjectBackupRemoteStore` requires `Unit`; both software and API36 jobs stopped on that same compiler error before tests/instrumentation could execute;
- H26a corrects only that contract mismatch and adds a serial fail-closed materializer layer with fixed source-patch SHA-256 and final Git blob verification. H25→H26→H26a, idempotent rerun and deliberate H26a-patch corruption were locally proven.

Local limitation: the downloaded CI source artifact does not contain a complete Gradle wrapper and no system Gradle executable is available. Therefore full Kotlin/Android compilation, Lint, JVM suite and API36 instrumentation remain mandatory at the next manual CI; they are not claimed as locally passed.

## Next gate
Run one fresh **manual** `GuitarLab Android CI` with signed homologation enabled after H26a publication. Audit actual test counts rather than predicting them. Only a passing exact-source run can promote H26/H26a to DIGITAL PASS and produce the H26 physical candidate.

After that, target-device residuals are SAF provider/persisted-permission/large-transfer/restore UX plus retained H25/H24/H23b checks. Use `RC3_FINAL_PHYSICAL_HOMOLOGATION.md` only after it is bound to the new signed H26 candidate.
