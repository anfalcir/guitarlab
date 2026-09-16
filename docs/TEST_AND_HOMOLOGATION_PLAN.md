# Test and Homologation Plan

Updated: 2026-09-16

## Evidence boundary
Current signed DIGITAL PASS is **CI #650** / run `35154021384` / producer `07c99155789774cb39f9b4382829f9e1d16649e3`, through H26/H26e.

Signed APK SHA-256: `fdb870d0996b7ee4ec6f72028af9c7291d4767b4303cbd5fa62894288757d2fe`.

Documentation-only commits after #650 may advance repository HEAD; they do not change this producer identity.

## #650 automated evidence
- source materializer terminal state through H26e: PASS;
- JVM/unit: **285/285 PASS**, 0 failures, 0 errors, 0 skipped;
- performance evidence: PASS with Small/Medium/Large measurements emitted;
- Android Lint: PASS; reports contain **44 warnings + 3 hints, 0 errors**;
- `assembleDebug`: PASS;
- `assembleRelease`: PASS;
- unsigned package/version/source provenance: PASS;
- API36: **31/31 standard PASS**;
- isolated target-tablet geometry: **1/1 PASS**;
- signed candidate provenance/package/version/zipalign/certificate/signature: PASS;
- signing material cleanup: PASS.

Canonical Android result is **31/31 standard + 1/1 isolated geometry**, reported separately.

## Performance evidence captured
- Small: 5 tracks / 10 clips — save/load `0.970107 ms`, bundle `0.368247 ms`, reopen `3.146647 ms`;
- Medium: 12 tracks / 50 clips — save/load `6.29094 ms`, bundle `0.781742 ms`, reopen `3.616526 ms`;
- Large: 24 tracks / 120 clips — save/load `3.042199 ms`, bundle `1.407318 ms`, reopen `3.293228 ms`.

These lines are retained as evidence from the passing performance test; they are not generalized into device-level latency claims.

## H26 critical matrix — digitally covered
### Integrity
- successful transaction becomes catalog-visible only after valid `COMMITTED`;
- truncated/corrupt package rejected;
- missing/partial/mismatched marker rejected;
- remote upload re-read must match SHA-256 + bytes before commit;
- restore re-verifies SHA-256 + bytes before import.

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
- delete failure reporting does not invalidate a newly committed backup;
- cancellation aborts and does not continue cleanup.

### Restore
- single version imports independent copy;
- restore-all chooses latest valid committed version per project;
- one corrupt project does not block others;
- no local project overwrite.

### Backup UI
#650 standard API36 includes three dedicated Backup-screen tests covering distinct project/total actions and explicit confirmation for single-version and restore-all flows. H26e fixes only robust test interaction/materialization; it does not weaken production semantics.

## Physical residual — do not duplicate digital tests
On Samsung SM-X230 with Google Drive exposed in the Android picker, validate only provider/device behavior that cannot be proven by the emulator/JVM suite:
- select Drive target and verify the real probe succeeds;
- close/reopen app, then reboot tablet, and confirm persisted permission/access;
- single-project backup and total backup create visible remote versions and coherent last-backup/status UI;
- change target folder and verify prior folder content is untouched while new writes use the new target;
- disconnect and verify remote content remains untouched;
- restore one version and restore-all, confirming independent local projects/no overwrite;
- revoke SAF/provider access and verify safe clear error/no local damage;
- representative long transfer/cancel behavior where practical;
- retention under a practical controlled version set;
- automatic incremental/coalescing behavior: a changed project uploads; an unchanged revision does not create unnecessary duplicate versions.

Retained H25/H24/H23b physical smoke remains required before final RC closure, especially real MK-300 recording/timing/routing/backing-isolation and representative edit/save/reopen/export.

## Final rule
CI #650 establishes DIGITAL PASS, not RC3 FINAL. Final closure requires the residual physical checklist, no repeatable P0/P1 and explicit user approval of the exact signed APK SHA-256.