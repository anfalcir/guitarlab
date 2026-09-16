# H26 — SAF Cloud Backup / Restore

Status: **DIGITAL PASS — CI #650 / H26e**
Updated: 2026-09-16

## Goal
Provide robust cloud-capable backup without Google Drive API credentials by using Android Storage Access Framework (SAF). The user selects a document-tree provider/folder (Google Drive when exposed by the device, or another compatible provider) and GuitarLab receives persistable access only to that tree.

## User flows
### Target folder
- `Opções → Backup e restauração` opens the dedicated screen.
- `Escolher pasta` / `Alterar pasta` launches `OpenDocumentTree`.
- GuitarLab takes persistable read/write permission and performs a real create/write/read/delete probe before adopting the folder.
- a failed probe leaves the previous target untouched;
- changing or disconnecting a target never deletes remote backup content.

### Manual backup
- `Backup total agora`: force a new committed version for every current local project.
- Home project menu `Backup deste projeto`: force a new committed version only for that project.
- manual operations share the same transactional engine as automatic backup.

### Automatic backup
- opt-in, disabled by default;
- cadence: 6 h / 12 h / daily / weekly;
- optional unmetered-only and charging-only restrictions;
- battery-not-low and storage-not-low always required;
- project save/import/rename/duplicate/recording persistence points enqueue one coalesced delayed incremental job;
- periodic work provides a safety sweep;
- unchanged persisted project revisions are skipped rather than re-uploaded.

### Restore
- single remote version: explicit confirmation, verify package then import as a new local project;
- restore all: restore the latest committed valid version of each cloud project independently;
- one corrupt project does not prevent other projects from restoring;
- restore never overwrites an existing local project.

## Transaction / integrity contract
Each remote version contains:
1. `metadata.properties`;
2. `project.guitarlab`;
3. `COMMITTED` written last.

Commit sequence:
1. create uncommitted version directory;
2. write metadata;
3. copy local staged `.guitarlab`;
4. re-read the remote package through the provider;
5. require exact byte count and SHA-256 match;
6. write `COMMITTED` containing the same identity;
7. re-read/parse the commit marker and require exact project/timestamp/size/hash agreement.

Catalog and restore ignore directories without a valid marker. Restore again checks bytes + SHA-256 before delegating to the existing `ProjectBundleReader`.

## Retention contract
Parameters:
- maximum age: 7 / 30 / 60 / 90 / 180 / 365 days or forever;
- protected minimum: 1 / 3 / 5 / 10 latest committed versions **per project**.

Rules:
- protected minimum always wins over age;
- deletion only considers committed valid versions;
- incomplete transactions are handled separately after a grace interval;
- all attempted writes failed => no retention deletion for that run;
- a project whose backup failed is excluded from cleanup in a partially successful run;
- a manual single-project run never cleans versions belonging to other projects;
- cancellation aborts the routine and does not continue into retention.

## Concurrency / lifecycle
A process-wide `Mutex` serializes manual backup, automatic backup and restore. WorkManager owns persistent automatic execution and uses a `dataSync` foreground operation for long transfers. Local staging files are temporary and stale staging is cleaned conservatively.

## Privacy / provider boundary
H26 stores no Google credentials and makes no Drive REST API calls. SAF URI permission is the authority. Revoked/missing permission fails safely and prompts the user to reselect a folder.

## Automated evidence — CI #650
Exact producer: `07c99155789774cb39f9b4382829f9e1d16649e3`.

#650 completed all three jobs successfully and promoted this contract to DIGITAL PASS:
- JVM/unit **285/285 PASS**, including H26 backup domain/coordinator coverage;
- performance evidence PASS;
- Android Lint PASS with non-blocking warnings/hints and no reported errors;
- debug/release assembly + unsigned provenance PASS;
- API36 **31/31 standard PASS**, including three dedicated `BackupScreenInstrumentedTest` cases;
- isolated target geometry **1/1 PASS**;
- exact tested unsigned artifact signed and provenance revalidated;
- package/version/certificate/signature/zipalign PASS;
- signing material cleanup PASS.

Detailed evidence: `H26E_CI650_DIGITAL_PASS.md`.

## Corrective/materialization history
- H26a fixed only `SafBackupRemoteStore.copyPackage()` so the override explicitly satisfies the interface `Unit` return contract.
- H26b removed use of Compose-test `assertExists`, unavailable in the pinned dependency version.
- CI #646 then proved the production software gate and ran 31 standard tests; its three failures were confined to H26 Backup-screen test interactions below the `LazyColumn` fold.
- H26c/H26d explored incremental source-patch recovery for the test scroll correction but exposed packaging/name/hash fragility in the corrective source-part, not a production H26 defect.
- H26e is the definitive path: the complete known-good `BackupScreenInstrumentedTest.kt` is decoded and installed deterministically after validating both SHA-256 and final Git blob.

## Canonical materialization
Canonical tail: `… → H25 → H26 → H26a → H26b → H26e`.

H26e input: `.source-parts/H26eBackupScreenInstrumentedTest.kt.b64`.
- decoded source SHA-256: `7f974aef7ad8b4052cd01b6a66ffe75cee78fb27c5e94585b9eda44e2bf322da`;
- expected final Git blob: `8ebac066a3bef1b93ee0316cdb5dcc726fe4b056`;
- final message: `Source patch chain materialized through H26e with verified final hashes`.

CI #650 produced that final message and then passed software, Android integration and signed homologation end to end.

## Remaining physical boundary
DIGITAL PASS does not prove real provider/device behavior. Final RC3 physical closure must still exercise on Samsung SM-X230 with Google Drive via SAF where available:
- picker/provider access and persisted permission across app/device restart;
- real single/full backup and cloud visibility;
- safe target-folder replacement/disconnect with old data preserved;
- single/full restore as independent local projects;
- revoked/unavailable provider error behavior;
- representative long transfer and cancellation;
- practical retention behavior when enough versions exist;
- automatic incremental behavior without redundant unchanged upload.

RC3 remains non-final until those residuals plus retained audio/editing smoke checks pass with no repeatable P0/P1 and the user explicitly approves the exact signed APK.