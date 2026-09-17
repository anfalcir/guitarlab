# Test and Homologation Plan

Updated: 2026-09-16

## Evidence boundary
The last signed DIGITAL PASS is **CI #650** / run `35154021384` / producer `07c99155789774cb39f9b4382829f9e1d16649e3`, through H26/H26e. Signed APK SHA-256: `fdb870d0996b7ee4ec6f72028af9c7291d4767b4303cbd5fa62894288757d2fe`.

H27 changes production source and tests after physical feedback. H27 is **IMPLEMENTED / SOURCE-VALIDATED / PRE-GATE**. Do not reuse #650 counts as H27 evidence.

## Retained #650 evidence
- JVM/unit 285/285 PASS;
- performance evidence PASS;
- Lint PASS with 44 warnings + 3 hints, 0 errors;
- debug/release assembly + unsigned provenance PASS;
- API36 **31/31 standard PASS**;
- isolated target geometry **1/1 PASS**;
- signed homologation/provenance/cleanup PASS.

## H27 automated requirements for next manual CI
Mandatory:
- materializer reaches `Source patch chain materialized through H27 with verified final hashes`;
- all existing + H27 JVM/unit suites PASS;
- Android Lint PASS;
- debug/release assembly + unsigned provenance PASS;
- standard API36 suite including updated backup release-copy assertions PASS;
- isolated 1920×1200 geometry PASS;
- signed homologation signs the exact tested unsigned artifact;
- package/version/certificate/zipalign/signature/source checks PASS;
- signing cleanup PASS.

Record actual XML-derived counts after the run. Keep standard API36 and isolated geometry separate.

## H27 critical matrix
### Revision/idempotency
- first backup of a persisted revision creates one version;
- repeated total backup without project change creates no additional version;
- repeated single-project backup without project change creates no additional version;
- automatic backup followed by manual backup of the same revision creates no duplicate;
- after a real persisted project change, exactly one new version can be created.

### Bounded history
- configured count is the maximum unique revisions retained per project;
- max 1/3/5/10 applies even with no age limit;
- history cleanup is independent per project;
- newest valid version is never automatically removed;
- same-revision duplicate copies collapse to the newest copy;
- optional age limit can remove older unique revisions sooner.

### Failure/cancellation safety
- all attempted writes fail => no destructive history cleanup;
- a failed project is protected from cleanup in a partially successful run;
- single-project backup never cleans another project's history;
- delete failures are reported without invalidating a newly stored backup;
- cancellation aborts and does not continue cleanup.

### Status/copy
- last execution is recorded even for partial/error outcomes;
- partial failure identifies the project and humanizes storage/integrity errors;
- normal backup UI does not expose `Storage Access Framework`, `SAF`, `COMMITTED` or backup `SHA-256` implementation copy;
- history setting reads `Máximo de versões por projeto`;
- generic settings/status copy does not hard-code personal test hardware such as MK-300.

### Restore/integrity regression
- package integrity is still verified before restore;
- single restore creates an independent local copy;
- restore-all selects the latest valid version per project;
- one bad remote project does not block other restores;
- no silent overwrite of local projects.

## Physical residual after H27 DIGITAL PASS
On Samsung SM-X230 with a real document provider:
- select target folder and verify persisted access after app restart/reboot;
- tap `Backup total agora` twice without modifying either project: the second run creates **zero** duplicate versions;
- modify/save one project, back up again: only that changed revision gains one new version;
- with maximum 3, create more than three distinct saved revisions and verify only the three newest unique revisions remain after a safe run;
- if old #650 duplicate rows are still present, run a safe H27 backup and verify same-revision duplicates are cleaned;
- provoke/reproduce a provider failure if practical and verify the UI identifies the affected project while its older history remains safe;
- smoke folder change/disconnect, restore one/restore all, revoked access and a representative large transfer/cancel;
- complete retained audio/edit/save/reopen/export smoke on the final exact candidate.

## Final rule
H27 must first receive a fresh signed DIGITAL PASS. RC3 FINAL then requires the reduced physical checklist, no repeatable P0/P1 and explicit approval of the exact final signed APK SHA-256.
