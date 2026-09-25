# GuitarLab Studio 0.5.0-rc14 — Release Notes

Updated: 2026-09-23  
Status: source candidate; Android corrective qualification pending
Package: `studio.guitarlab.app`  
Version: `0.5.0-rc14` / versionCode `34`

## Scope

RC14 incorporates the findings from physical RC13 homologation without changing the Prepared References v2 cloud protocol or weakening any authentication, integrity or ownership boundary.

### Android background processing and notifications

- Remote separation now enters WorkManager foreground execution with the declared `dataSync` service type before cloud reconciliation starts.
- Opening the Android notification shade, navigating away or sending the app to the background no longer makes the separation depend on Activity focus.
- Separation and automatic-backup notifications use a monochrome GuitarLab pick/wave small icon instead of Android's generic upload/error/warning glyphs.
- Terminal and retry copy remains governed by the existing notification policy.

### Unified take management

- The track pencil exposes `Gerenciar takes e sincronização…` whenever the track owns recorded takes.
- That entry opens the existing track/take management flow; no parallel take editor or duplicate persistence path was introduced.
- Activate, audition, favorite, rename, note, delete and persistent per-take synchronization continue through `TakeManagementPolicy` and `StudioViewModel`.
- Global recording calibration still affects only future recordings; per-take adjustment remains retroactive, non-destructive and shared by splits in the same take lineage.

### Backup catalog and deleted-project lifecycle

- `Versões disponíveis` is grouped by immutable project ID, showing one project row and its version count.
- A project dialog lists the individual dated versions and their restore actions.
- Explicitly deleted local projects carry an `EXCLUÍDO` badge and can be restored or permanently removed from Drive.
- Projects discovered only in Drive carry `SOMENTE NA NUVEM`; absence alone never starts destructive cleanup.
- Local deletion records an explicit timestamped tombstone. Restore removes that tombstone.
- Manual total backup and automatic backup remove an explicitly deleted project's remote graph after ten days.
- Remote deletion removes project heads/manifests and only content-addressed assets not referenced by another project's heads.

## Regression coverage

- separation notification ongoing/terminal policy;
- foreground-worker compilation and manifest `dataSync` declaration;
- ten-day tombstone boundary and fail-safe empty-catalog behavior;
- grouped backup catalog, deleted badge, version modal and confirmed cloud deletion action;
- existing take-management domain tests remain the authority for lineage adjustment, activation and deletion;
- complete JVM suite, Android Lint, debug/release assembly and API 36 instrumentation remain mandatory in canonical CI.

## Candidate rule

RC14 does not become the signed physical-homologation authority until Android CI, U4 and U7 pass on the exact sealed producer SHA and the signed artifact identity is recorded here and in `CURRENT_STATE.md`.

The first sealed qualification (`9b4ee9f`, Android run `35935229131`) passed unit/Lint/build while U4 `35935229248` and U7 `35935229232` passed. API 36 found one stale test that still addressed the former flat backup list directly. Production behavior and the new grouped-catalog test passed; the corrective updates that legacy assertion to open the project-version modal before requesting restoration.
