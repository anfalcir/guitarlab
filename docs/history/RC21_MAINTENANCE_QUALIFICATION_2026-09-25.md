# RC21 Maintenance Qualification — 2026-09-25

Status: **UNSIGNED SOFTWARE/API36 PASS — SIGNED/PHYSICAL PROMOTION PENDING**

This record preserves the maintenance evidence for GuitarLab `0.5.0-rc21` / versionCode `41`. It is historical/candidate evidence; `../RELEASE_BASELINE.md` remains authoritative for the currently accepted RC20 release until RC21 is signed and accepted by the owner.

## Trigger

RC20 maintenance reopened after an owner-observed project-integrity regression. A legacy recorded-guitar clip could fail project persistence after stereo channel separation with `clip.trim.bounds`. The same maintenance cycle was used to harden transient feedback and the Drive backup catalog/cache path that had become relevant during owner use.

## Forensic root cause

The affected legacy recording predated current take persistence and had no take entry. The stored stereo source itself was valid, including its original trim bounds. The RC20 stereo separation path generated valid mono media but then ignored the splitter's returned media facts and set each derived clip's editing bound from the source clip's trimmed `lengthFrames`. For a trimmed source that made the derived clip's source trim exceed the declared editing bound, so project validation correctly rejected persistence.

## RC21 fixes

### Legacy recording compatibility

- strict `LegacyRecordingTakeRecoveryPolicy`;
- only recorded-guitar roles are eligible;
- source must be a project-managed WAV under the expected `media/source` convention;
- managed URI must exactly match the managed relative path;
- filename prefix must match the clip ID and contain the historical take timestamp shape;
- clips already carrying take IDs are untouched;
- generic imports never become takes;
- recovery is deterministic/idempotent and normalized through the existing take policy.

### Stereo separation integrity

- `StereoSeparationProjectPolicy` owns the project transaction;
- actual splitter total frames/sample rate become derived mono editing metadata;
- modern source take metadata/favorite/note/fine adjustment is preserved into separate per-track takes;
- active-take state is normalized per target track;
- a source take referenced by multiple temporal clips fails closed because lineage is ambiguous;
- uncommitted derived/proxy media is discarded on transaction failure.

### Persisted-state cryptographic integrity

`ProjectCodec.decodePersistedState()` now exposes migrated/normalized persisted state separately from compatibility repair. Drive/package integrity checks verify the canonical persisted digest before calling normal decode/recovery. Compatibility repair therefore cannot silently change the state whose hash was authenticated.

### Transient feedback

- shared `AppTransientFeedbackHost` replaces competing Home/Studio/Backup/Diagnostics Snackbar ownership;
- Error, Warning and non-obvious Async Completion remain eligible;
- operational/persistent state stays inline;
- eligible messages are consumed at host handoff to prevent delayed replay after navigation;
- normal transient paths retain user-safe sanitization.

### Drive catalog/cache

- `UnifiedDriveProductionService.loadCatalog()` uses one `listAllHeads()` snapshot for versions, reconciliation and remote tips;
- immutable known descriptors/manifest timestamps are reused where identity matches;
- metadata-only `BackupCatalogCacheStore` is schema/integrity/account/retention/local-revision constrained;
- corrupted or mismatched cache is discarded;
- cached versions remain visible during refresh;
- empty refresh exposes immediate loading state;
- user refresh and every visible Backup-screen entry force real Drive head verification;
- automatic WorkManager backup is independent of Backup UI lifetime;
- an automatic completion triggers immediate catalog refresh only while Backup is visible; otherwise the next screen entry performs the forced verification.

## Qualification chronology

The corrective branch was `maintenance/rc21-integrity-feedback-backup`, forked from accepted documentation HEAD `930af2a35e449ecc3a5ae65898e6e71f8db7430f`.

Intermediate failing runs were used as causal gates and fixed serially:

- #905: one malformed unit fixture plus missing fail-closed API36 test-group classification;
- #906: JUnit annotation mismatch in a new app unit test;
- #907: JUnit assertion imports incompatible with the Android instrumented source set;
- #908: Backup empty-catalog loading indicator existed below the initial viewport; the product UX was hardened rather than weakening the assertion;
- #909: superseded/cancelled by the final hardening iteration.

Final unsigned authority:

- Android CI: **#910 / run 36195905242**;
- exact qualified source: `ad182678cb2704dc9bbfc622124b4f2ac121fea1`;
- version: `0.5.0-rc21` / `41`;
- Unit tests: PASS;
- Android Lint: PASS;
- APK build: PASS;
- API 36 grouped instrumented regression: PASS;
- signing job: intentionally skipped on this run.

## Drive provider evidence

RC21 does not replace OAuth scope, Drive resumable transport, content/object identity, commit publication or restore-store semantics. Existing provider-real U8m evidence therefore remains valid authority for those unchanged behaviors.

Because RC21 changes the catalog/cache/background orchestration, final residual owner acceptance must include a representative real-Drive backup → version-history refresh → restore smoke on the target device. Repeating the entire destructive U8m campaign is not required without a transport/store mutation.

## Promotion boundary

Before any signed RC21 run:

1. complete live documentation;
2. merge the maintenance work into canonical `main`;
3. remove the temporary maintenance branch/trigger;
4. run the exact-source signed Android gate from `main`.

RC20 remains the accepted baseline until the resulting signed RC21 APK passes proportional owner acceptance. Only then may `RELEASE_BASELINE.md` be promoted.
