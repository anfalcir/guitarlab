# GuitarLab Studio 0.5.0-rc4 — H37a source candidate

Status: **SOURCE PRE-GATE**
Updated: 2026-09-20

`0.5.0-rc4` / versionCode `24` is the H37a Google Drive backup transport source candidate. It does not supersede the signed `0.5.0-rc3` / CI #663 candidate until the manual canonical CI passes.

Manual CI #664 / run `35544867278` exercised the merged H37 producer `abecc73e4eab181a7776d98cc731758b17c64b06` and failed at Kotlin compilation before Lint/build/signing. H37a corrects only those compile errors; Drive behavior and H28 semantics are unchanged.

Primary change: automatic/manual project backup and restore now target Google Drive API v3 directly with OAuth `drive.file`, resumable upload, server-confirmed resume offsets, Drive size/SHA-256 verification, application metadata identities, bounded retry/backoff and crash reconciliation. Firebase/Cloud Run/Functions are not in the backup data path.

H28 project/revision identity, deduplication, retention and restore-as-copy semantics are preserved. Existing SAF history can be migrated once to Drive; legacy content is not deleted and the SAF permission is retained until complete migration success.

See `docs/H37_DRIVE_V3_BACKUP.md` for the architecture, OAuth registration identity, security contract, pre-gate evidence and physical acceptance checklist.
