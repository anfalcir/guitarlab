# Test and Homologation Plan

Updated: 2026-09-16

## Evidence boundary
Current signed DIGITAL PASS is still **CI #642** / run `35121955150` / producer `7bfd876b6a5b0701ab0cf5203de36c31cd117632`, through H25. Its signed SHA-256 is `916758f694735febb8cccfe58f46f290562aaba1b5483ccc727e2be0cefba5aa`.

H26 changes source and is **IMPLEMENTED / SOURCE-VALIDATED / PRE-GATE**. Do not use #642 as H26 evidence.

## Retained #642 evidence
- JVM/unit: 269/269 PASS;
- Lint/build/unsigned provenance PASS;
- API36: **28/28 standard + 1/1 isolated geometry**;
- signed homologation PASS.

## H26 local/source validation completed
- patch generated from exact H25 materialized source;
- `git diff --check` clean;
- clean H25→H26 application PASS;
- materializer second execution idempotent PASS;
- exact 24 final source hashes PASS;
- materialized source byte-identical to audited workspace PASS;
- concatenated H26 source archive is validated on every invocation by base64 decode + gzip CRC + fixed SHA-256; deliberate corruption fails closed PASS;
- focused core syntax/runtime policy checks PASS in the available local environment.

Full Gradle/JVM/Lint/Android execution is not claimed locally because the downloaded source artifact does not contain a complete Gradle wrapper and no system Gradle is available.

## H26 automated requirements for next manual CI
Mandatory:
- materializer reaches H26 final hashes;
- all existing JVM/unit suites + H26 policy/coordinator tests PASS;
- Android Lint PASS;
- debug/release assembly + unsigned provenance PASS;
- standard API36 suite, including H26 Backup screen tests, PASS;
- isolated 1920×1200 geometry PASS;
- signed homologation signs the exact tested unsigned artifact;
- package/version/certificate/zipalign/signature checks PASS;
- signing cleanup PASS.

Do not predict the new test counts. Record the actual XML-derived counts after the run and keep standard API36 separate from isolated geometry.

## H26 critical test matrix
### Integrity
- successful transaction becomes catalog-visible only after valid `COMMITTED`;
- truncated/corrupt package rejected;
- missing/partial/mismatched marker rejected;
- remote upload must re-read to matching SHA-256 + bytes before commit;
- restore must re-verify SHA-256 + bytes before import.

### Incremental/efficiency
- unchanged automatic project skipped;
- repeated save triggers coalesce;
- manual project/total backup forces explicit version;
- periodic work remains configured with selected constraints.

### Retention
- minimum protected versions override age;
- retention isolated per project;
- forever deletes nothing by age;
- all writes fail => cleanup suspended;
- partial failure protects failed project;
- manual single-project cleanup cannot touch other project groups;
- delete failure is reported without invalidating new committed backup;
- cancellation aborts and does not continue cleanup.

### Restore
- single version imports independent copy;
- restore-all chooses latest valid committed version per project;
- one corrupt project does not block others;
- no local project overwrite.

## Physical H26 residual after digital PASS
On SM-X230 with Google Drive exposed in the Android picker:
- select target folder and verify probe succeeds;
- restart app/device and confirm persisted access;
- single-project and total backup produce visible committed versions;
- edit/save and verify automatic incremental behavior under configured constraints;
- change target folder: old folder contents remain, new writes go to new target;
- disconnect: cloud content remains untouched;
- restore one version and restore-all create independent local projects;
- revoke folder/provider access and confirm safe error/no local damage;
- representative large `.guitarlab` transfer shows foreground transfer behavior and completes/cancels safely.

Retained H25/H24/H23b physical checks remain required before final RC closure.
