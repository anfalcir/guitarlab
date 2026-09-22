# H37 — Native Google Drive API v3 Backup Transport

Status: **CLOSED / ABSORBED INTO UNIFIED U8 DRIVE v3 PRODUCTION PATH**
Updated: 2026-09-22
Historical transport candidate: `0.5.0-rc4` / versionCode `24`
Signed rc4 authority: **CI #669 / run `35591631207`**. Provider-real unified acceptance: **U8m PASS · r_1790095960 · cleanup 8/8/14**. U11 rc5/25 is the active release freeze.

## Purpose
H37 replaces SAF as the primary backup transport with direct client-side Google Drive API v3 while preserving the H28 backup domain contract. Firebase, Cloud Functions, Cloud Run, service accounts and backend token custody are intentionally not part of the backup data path.

Data path:

`GuitarLab Android -> Google Identity Services OAuth -> Drive API v3 -> user's Google Drive`

The legacy SAF implementation is historical compatibility code only. Under the 2026-09-21 clean-cutover decision, migrating H26-H28/standalone histories is not a unified-release requirement; current unified projects use the Drive v3 path.

## Security and authorization contract
- OAuth scope: `https://www.googleapis.com/auth/drive.file` only.
- No broad `drive` scope.
- No client secret, refresh token, service-account key or Firebase credential is embedded in the APK/repository.
- Access tokens are short-lived and kept in memory only.
- Resumable upload session URLs are stored only in app-private SharedPreferences for crash recovery and are excluded from Android cloud backup/device transfer.
- Drive connection/account binding is also device-local and excluded from Android backup/device transfer.
- Disconnect revokes the granted Drive scope, clears local recovery state and does **not** delete remote backups.

## Shared Google Cloud/Firebase project boundary
GBW and GuitarLab may intentionally use the same Google Cloud/Firebase project as preparation for a future product unification. This does **not** merge Android OAuth identity: GuitarLab remains package `studio.guitarlab.app` and requires its own Android app/OAuth client registration for the locked signing certificate.

The H37 backup path remains direct Google Identity Services + Drive API v3. No Firebase SDK, `google-services.json`, Cloud Functions or Cloud Run hop is required for backup bytes. A Firebase configuration belonging only to another package must never be embedded merely because both apps share the cloud project.

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

## Legacy SAF migration — historical only

The H37 migration design is retained for traceability but is **not a U11/U12 release gate**. The clean-cutover decision retired U9 because no retained production corpus requires automatic migration. Current unified-line backup/restore validation targets the direct Drive v3 representation and current unified project schema.

## Automatic backup
The existing WorkManager scheduling policy is retained, but jobs are scheduled only when Drive is connected. Existing cadence, unmetered-network, charging, battery/storage guards, edit coalescing and retention semantics remain intact.

Background authorization failure is fail-closed: WorkManager does not attempt to bypass user consent. The UI must reconnect the account.

## Source materialization evidence
H37 is stored using the repository's deterministic source-part mechanism. The compressed Base64 archive is split into five repository parts (`.part00` ... `.part04`) and concatenated byte-for-byte before decode; the materializer validates the reconstructed gzip, the decoded patch and every terminal Git blob.

Final source patch SHA-256:
`d4d7da1d1a097c9451644d78b92cc10dae6faf35b523b0339cce906ab01007a2`

Compressed archive SHA-256:
`253953752a421a5b2299a040024c7f897940cfaa7de17aec914bcc2793ce9000`

Source-part SHA-256:
- `.part00`: `d1f6d661fef4e2175f120ec7451a90d186618dd86c1c3024a2dcd5fa45198197`
- `.part01`: `7e1dde4ea9aeb096b831ec830b102bbb6b81b3624d208cbdab238f9107d5709e`
- `.part02`: `e38bfb214b994d053300b85ca7d6d100f6cc21db02a3a66b4df59a18a006ea02`
- `.part03`: `cae909f86f773b7f5962d6c5f1128d0e0c8acd0934f71122524fcd5f786ef85b`
- `.part04`: `fdae3c09f45f9bfa7bbeb6142aa8854e3e731c9b8e76601750f8c37c99099842`

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

CI #663 remains the signed authority. H37 producer `abecc73e4eab181a7776d98cc731758b17c64b06` was exercised by manual CI #664 / run `35544867278`, which failed deterministically at Kotlin compilation before Lint/build/signing. H37a corrects the compile failure; H37b additionally clears rejected OAuth access tokens from the Google Identity Services local cache and is the current source candidate requiring a fresh manual canonical run.

Required first physical OAuth/Drive acceptance after a successful H37 CI:
- connect the intended Google account;
- confirm the OAuth consent uses the Drive file scope only;
- create first backup;
- repeat unchanged backup and confirm no duplicate revision;
- interrupt/resume a representative larger upload;
- restore and verify project/package integrity;
- test disconnect/reconnect without deleting remote history;
- if legacy SAF history exists, execute one-time migration and verify the old destination is preserved until full success.


## H37a — compile corrective after CI #664

CI #664 proved that H37 materialization itself was valid but exposed two Kotlin compilation issues before any runtime/instrumentation gate:

- `DriveAuthorization.kt` used `CancellableContinuation.tryResume/completeResume` and `tryResumeWithException/completeResume`, which coroutines 1.11.0 treats as internal API;
- `DriveV3Protocol.kt` expressed the authorized request loop as `withContext<DriveHttpResponse> { while (true) ... }`, and Kotlin inferred the lambda terminal type as `Unit`.

H37a keeps the Drive behavior unchanged and applies the smallest source correction:

- Task success uses stable `kotlin.coroutines.resume`;
- Task failure uses stable `kotlin.coroutines.resumeWithException`;
- Google Task cancellation explicitly cancels the coroutine continuation;
- the authorized HTTP loop accumulates an explicit `DriveHttpResponse?` and returns `checkNotNull(completed)`, while preserving one token invalidation/retry on HTTP 401.

H37a source part:

`.source-parts/H37aDriveCompileCorrective.patch.gz.b64`

Encoded file SHA-256:

`16e20f2d7fd971327c3df4d1059518dfb9b1abe671529985f477ab7860b55548`

Compressed archive SHA-256:

`77f5d9ad6f5ebced2e9763dc13ed37a7ac7b4da3412f40c5f6498e813e5b4565`

Decoded patch SHA-256:

`5b623309e98c2b8f79434db437068ec80f197f855ec7efc8b5f8eb45b3163cae`

Expected terminal Git blobs:

- `DriveAuthorization.kt`: `be34ca749b70e33eac826eb904a71679796e4c5d`
- `DriveV3Protocol.kt`: `0a8b5dc97f8c3760cb3956f22fd80a7b413fe5eb`

Pre-publication H37a evidence:

- targeted Kotlin/coroutines compile probe PASS;
- clean H37 → H37a patch application PASS;
- exact terminal Git blob verification PASS;
- actual H37a materializer first execution PASS;
- second materializer execution idempotent PASS;
- deliberately corrupted H37a archive rejected before source mutation PASS.

No Drive domain, OAuth scope, upload protocol, retention, migration or restore semantics are changed by H37a.


## H37b — rejected-token cache hardening

After H37a, the final pre-CI audit identified a 401 recovery edge: clearing only GuitarLab's in-memory token reference is insufficient because Google Identity Services can retain the same access token in its own local cache.

Google's authorization API exposes `AuthorizationClient.clearToken(ClearTokenRequest)` specifically to clear an access token from that local cache. H37b therefore:
- captures the exact token that received 401;
- clears GuitarLab's in-memory copy first;
- clears that rejected token from Google Identity Services;
- reacquires authorization through the existing `drive.file` request;
- preserves the one-refresh-on-401 bound and resumable-upload server-offset reconciliation.

H37b source part:
`.source-parts/H37bDriveTokenCacheHardening.patch.gz.b64`

Encoded Base64 file SHA-256:
`22a4a8b962836d337407e3151d508a857526e7bd43159ed25697119bfcf3fbf8`

Compressed archive SHA-256:
`e9e79f2c46e47f0e04ccf0f9aaead83908afa04cb3f99c81bbd3438d250cc824`

Decoded patch SHA-256:
`9ebfb1e69a6ad4e888e9782c07d54f9566630814ba40b8f6163e82727bc85c5d`

Terminal Git blobs:
- `DriveAuthorization.kt`: `4e727ae8d2a7e24a8e2c960380ade30ad1be1bf3`
- `DriveV3Protocol.kt`: `83a46bcd0d98007c376b3476e99a97d3d6a96a9b`

Pre-publication validation:
- official API signature verification PASS;
- targeted Kotlin/coroutines compile probe PASS;
- clean H37a → H37b materialization PASS;
- second materialization idempotent PASS;
- corrupted H37b archive rejected fail-closed before source mutation PASS.

No H28 identity, Drive file model, resumable protocol, integrity gate, retention, restore or SAF-migration contract changes in H37b.
