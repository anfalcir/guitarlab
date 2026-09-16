# H26 — SAF Cloud Backup / Restore

Status: **IMPLEMENTED / SOURCE-VALIDATED / PRE-GATE**  
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

## Test contract
Pure/JVM coverage includes:
- retention minimum vs age and per-project isolation;
- forever retention;
- exact-revision incremental skip;
- deterministic SHA-256;
- strict `COMMITTED` metadata validation;
- manual force vs automatic skip;
- all-write-failure retention suspension;
- partial-failure protection;
- unchanged automatic run with safe retention;
- single-project cleanup scope;
- incompatible commit rejection;
- retention-delete error reporting without invalidating a newly committed backup;
- cancellation propagation/no cleanup;
- corrupt restore rejection/no publish;
- restore-all latest-per-project and failure isolation.

Android instrumentation adds dedicated Backup screen action/confirmation coverage. Full Android compile/instrumentation is pending the next manual CI.

## Materialization
Source parts: `.source-parts/H26SafCloudBackup.patch.gz.part00` through `.part04`  
Canonical tail: `… → H23b → H24 → H24a → H25 → H26`.

The H26 entrypoint validates the concatenated source archive on every invocation by base64 decode, gzip CRC and fixed SHA-256, delegates the frozen H18–H25 chain to `materialize_ci_sources_through_h25.sh`, then verifies exact Git blob hashes for all H26 files. Local proof covered clean H25→H26 application, idempotent rerun, byte-identical final files and fail-closed behavior for a corrupted encoded package even when H26 was already materialized.

## Promotion rule
H26 remains PRE-GATE until one exact-source manually dispatched signed workflow passes software gate, API36 standard + isolated geometry gate and signed homologation. Do not attribute H26 to CI #642.
