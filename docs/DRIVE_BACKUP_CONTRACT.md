# Google Drive v3 Backup Contract

Updated: 2026-09-24

GuitarLab backs up directly from Android to the owner's Google Drive. Historical H37 implementation/corrective evidence is retained in `history/H37_DRIVE_V3_BACKUP.md`.

## Data path

`GuitarLab Android → Google Identity Services OAuth → Drive API v3 → owner's Drive`

Firebase, Cloud Functions, Cloud Run, service accounts and backend token custody are not part of backup transport.

## Authorization and credentials

- OAuth scope is only `https://www.googleapis.com/auth/drive.file`;
- package `studio.guitarlab.app` uses its own Android OAuth client bound to the locked signing identity;
- no client secret, refresh token, service-account key or unrelated Firebase configuration is embedded;
- access tokens are short-lived and held in memory;
- rejected tokens are cleared from both app memory and Google Identity Services before bounded reauthorization;
- Drive account binding and resumable-session state are device-local and excluded from Android cloud/device backup;
- disconnect revokes/clears local authorization state without deleting remote backup data.

## Remote representation

Committed backups are regular `.guitarlab` files organized in the owner's Drive. Logical catalog identity comes from private `appProperties`, not filename or folder listing.

Metadata identifies:

- record/format and upload state;
- project and revision identity;
- display name and timestamps;
- expected byte size and SHA-256.

Moving the organizational folder must not split logical project history.

## Upload transaction

A revision becomes restorable only after:

1. resumable upload completes;
2. Drive reports expected byte size;
3. Drive reports matching SHA-256;
4. metadata transitions from `uploading` to `committed`;
5. committed project/revision/hash identity still agrees.

Incomplete uploads never appear in the restore catalog.

## Resumption and recovery

- uploads use bounded chunks and persist the session URL with revision/hash/size and lifetime;
- after process death or transport failure, query the server-confirmed offset before sending bytes;
- unknown offset never falls back blindly to byte zero;
- 401, 429, transient 5xx, I/O interruption and lost final response use bounded reconciliation/backoff;
- a completed upload missing final commit metadata reconciles without re-uploading bytes;
- cancellation propagates and does not create an uncontrolled retry/duplicate path;
- deterministic package bytes keep a resumed upload bound to the same persisted revision.

## Backup-domain invariants

`BACKUP_IDENTITY_CONTRACT.md` remains authoritative for immutable project identity, deterministic revision identity, exact package hash, unchanged-revision idempotency, bounded retention and transactional restore-as-copy.

## Automatic backup

Automatic backup runs only while Drive is connected and under the configured WorkManager network/power/storage/coalescing policy. Background authorization failure is fail-closed and requires owner reconnection in the UI.

## Support boundary

The supported path covers current unified GuitarLab projects and backups created by the current product line. Legacy SAF/GBW/H37 corpus migration is outside release scope. Harmless compatibility code may remain but creates no maintenance or physical-test obligation.

## Qualification

Repeat focused provider-real evidence when transport, OAuth, upload, catalog, integrity, retention or restore behavior changes. Cover:

- correct narrow consent scope;
- first backup and unchanged idempotent retry;
- interruption/process-death resume from server offset;
- lost final response reconciliation;
- size/hash/commit validation;
- restore integrity and restore-as-copy;
- disconnect/reconnect without remote deletion;
- bounded failure behavior.

Do not repeat the entire historical provider campaign for unrelated RC20 changes. A representative target-device backup/restore smoke belongs in final physical acceptance only if Drive is deliberately included in the frozen baseline or the candidate can materially affect it.
