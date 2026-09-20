# H37 — Native Google Drive API v3 Backup Transport

Status: **SOURCE PRE-GATE**
Updated: 2026-09-20
Target line: `0.5.0-rc4` / versionCode `24`
Signed authority remains: **CI #663 / `0.5.0-rc3`** until the manual canonical gate passes.

## Purpose
H37 replaces SAF as the primary backup transport with direct client-side Google Drive API v3 while preserving the H28 backup domain contract. Firebase, Cloud Functions, Cloud Run, service accounts and backend token custody are intentionally not part of the backup data path.

Data path:

`GuitarLab Android -> Google Identity Services OAuth -> Drive API v3 -> user's Google Drive`

The legacy SAF implementation remains only as a one-time migration source for users who already have H26-H28 history.

## Security and authorization contract
- OAuth scope: `https://www.googleapis.com/auth/drive.file` only.
- No broad `drive` scope.
- No client secret, refresh token, service-account key or Firebase credential is embedded in the APK/repository.
- Access tokens are short-lived and kept in memory only.
- Resumable upload session URLs are stored only in app-private SharedPreferences for crash recovery and are excluded from Android cloud backup/device transfer.
- Drive connection/account binding is also device-local and excluded from Android backup/device transfer.
- Disconnect revokes the granted Drive scope, clears local recovery state and does **not** delete remote backups.

## Google Cloud OAuth prerequisite
Enable Google Drive API and register an OAuth 2.0 Android client for the signed GuitarLab application.

Official homologation/release identity:
- package: `studio.guitarlab.app`
- signer SHA-1: `42:C7:0C:79:D3:B5:CB:2C:FE:BD:F7:FF:80:93:1B:BB:F4:D1:00:37`
- requested scope: `https://www.googleapis.com/auth/drive.file`

A separately signed debug build needs its own Android OAuth client registration if it must exercise Drive authorization.

## Preserved H28 invariants
H37 does not replace the H28 identity model:
- immutable `projectId` identifies one project across renames;
- deterministic `revisionId` identifies one persisted logical state;
- package SHA-256 identifies the exact bytes;
- unchanged revisions are idempotent;
- retention remains bounded per project;
- restore creates an independent local copy;
- process-wide backup/restore serialization remains in force.

## Drive representation
Committed backups are regular `.guitarlab` files visible in the user's Drive. A `GuitarLab Studio Backups` folder is created for organization, but folder location is not the logical catalog identity.

Private `appProperties` carry:
- GuitarLab record kind and format;
- `uploading` / `committed` state;
- `projectId`;
- display project name;
- `revisionId`;
- project update timestamp;
- backup timestamp;
- expected byte size;
- expected SHA-256.

Catalog lookup is based on those application properties, not on filenames or an eventually consistent parent listing. Moving the folder does not split project history.

## Transaction and integrity contract
A revision is restorable only after all of the following are true:
1. the resumable upload has completed;
2. Drive reports the expected byte size;
3. Drive reports a `sha256Checksum` matching the locally generated package;
4. the file metadata is atomically patched from `uploading` to `committed`;
5. the committed descriptor still matches `projectId`, `revisionId` and SHA-256.

Interrupted or incomplete `uploading` files are never returned by the restore catalog.

## Resumable upload and failure recovery
- chunk size: 8 MiB (aligned to Drive resumable-upload requirements);
- session URL is persisted with revision hash/size and a bounded lifetime;
- after process death, the server-confirmed upload offset is queried before any bytes are sent;
- an unknown offset never falls back to byte zero;
- after 401, transient 429/5xx or I/O interruption, the client queries session state before retransmitting;
- a completed upload whose final commit metadata was not written is reconciled without re-uploading the package;
- 429 and transient 5xx use bounded exponential backoff;
- cancellation is propagated rather than converted into a duplicate/retry path.

## Deterministic package bytes
`ProjectBundleWriter` now writes deterministic ZIP entry timestamps and already sorts referenced media paths. Regenerating the same persisted revision therefore produces the same package bytes, allowing a resumable session to remain safe across Android process death.

## Legacy SAF migration
H37 can copy the complete committed H26-H28 SAF history to Drive.

Migration rules:
- validates the persisted SAF read/write permission first;
- preserves `projectId`, `revisionId`, timestamps and package SHA-256;
- skips a Drive revision only when revision identity, size and SHA-256 match;
- validates every SAF package locally before upload;
- never deletes legacy remote content;
- releases the old SAF permission and removes the local legacy destination only after the migration finishes with zero failures;
- any partial failure leaves the SAF destination intact and reports the affected projects.

## Automatic backup
The existing WorkManager scheduling policy is retained, but jobs are scheduled only when Drive is connected. Existing cadence, unmetered-network, charging, battery/storage guards, edit coalescing and retention semantics remain intact.

Background authorization failure is fail-closed: WorkManager does not attempt to bypass user consent. The UI must reconnect the account.

## Source materialization evidence
H37 is stored using the repository's deterministic source-part mechanism. The compressed Base64 archive is split into five repository parts (`.part00` ... `.part04`) and concatenated byte-for-byte before decode; the materializer validates the reconstructed gzip, the decoded patch and every terminal Git blob.

Final source patch SHA-256:
`80c47320730cb665a6523f6f4ed404a9a1cc758e142e34de47c6dfdce3fdbb92`

Compressed archive SHA-256:
`b50c966cc6d95b4dce4268d1e9a4616c841d73d3d8be6758472e22555c895e19`

Source-part SHA-256:
- `.part00`: `48b073aa4ad6f3c18299431ea436d39c5394dfdd9e84e9f9d7bc19a6a7154ce0`
- `.part01`: `fd82b1fbd104912412f4914e95c57f50b042335005b177c6881d0ec056c78bc4`
- `.part02`: `fa4d4924a418c84ac12c0a7fd3dc81c8872c492c525e812d5ab6f27cab7e26f7`
- `.part03`: `95d2541cf8ae298490eff215a3697471d1103a847c6dffe4a8ee6dd2738ac45e`
- `.part04`: `93c40a58059892425324e05e6dd7601a955bb911c740ae3c9a0d9af8665fa6f8`

Local pre-publication evidence:
- clean H36c -> H37 materialization PASS;
- second materializer execution idempotent PASS;
- terminal Git blob verification PASS;
- corrupted H37 archive rejected before H37 source mutation PASS;
- `git diff --check` PASS;
- materializer shell syntax PASS;
- static security invariant scan PASS (`drive.file` only; no Firebase/backend secrets/service account).

## Evidence boundary / next gate
This is **not yet DIGITAL PASS**. The current execution environment does not contain the Gradle wrapper/Android SDK/dependency cache needed to run the canonical Android gate locally.

CI #663 remains the signed authority until the user manually runs `.github/workflows/android-ci.yml` for the H37 source and the complete software/API36/signing pipeline passes.

Required first physical OAuth/Drive acceptance after a successful H37 CI:
- connect the intended Google account;
- confirm the OAuth consent uses the Drive file scope only;
- create first backup;
- repeat unchanged backup and confirm no duplicate revision;
- interrupt/resume a representative larger upload;
- restore and verify project/package integrity;
- test disconnect/reconnect without deleting remote history;
- if legacy SAF history exists, execute one-time migration and verify the old destination is preserved until full success.
