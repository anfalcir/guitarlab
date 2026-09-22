# U8 unified Drive backup

Updated: 2026-09-22
U8l status: CLOSED / DIGITAL PASS — Android CI #756 / run `35746523924`, exact
source `2cacbfe4c95bfbee694e717468075b8eedc7f9c6`.

U8k status: CLOSED / DIGITAL PASS — Android CI #750 / run `35742420939`, exact
source `b5bf8d39008aa267d7411244ea0ae526142863f7`.

U8j status: CLOSED / DIGITAL PASS — Android CI #746 / run `35732391636`, exact
source `ca44bd8efada617bbc30c8c3f3cf9f3d6b18a3c1`.

U8i status: CLOSED / DIGITAL PASS — Android CI #744 / run `35729709715`, exact
source `89a7cca5acd2d5a1c9aabbb5f72e028347dd00db`. C6 is CLOSED.

U8h status: CLOSED / DIGITAL PASS — Android CI #739 / run `35721268453`, exact
source `336f0a137e6d16cf5936344ec93b884a9db27a17`.
Status: U8a CLOSED / DIGITAL PASS — Android CI run `35678941252`, exact source
`a5d851ce8becd40ef08e95fd302424ac7ea4c082`

U8b status: CLOSED / DIGITAL PASS — Android CI #729 / run `35679878744`,
exact source `b3ecd5f5269728e9e481156a187922c16b93c05b`

U8c status: CLOSED / DIGITAL PASS — Android CI #730 / run `35707441794`,
exact source `58aad5cd230c9bb3700529dbf4b97498e08b429a`

U8d status: CLOSED / DIGITAL PASS — Android CI #731 / run `35708902719`,
exact source `51b2fb69a49e8bf1b96991432c7685db9db51784`

## Scope boundary

U8 replaces the historical monolithic H37 backup protocol for projects created
by the unified product line. It does not discover, migrate or delete legacy
H37, pre-unification GuitarLab or standalone GBW backups. Portable
`.guitarlab` export remains a separate self-contained workflow.

OAuth remains direct client-side Google Drive API with the narrow
`drive.file` scope. Firebase and temporary separation Storage are not backup
transports.

## U8a domain foundation

The first checkpoint defines provider-neutral invariants before changing the
remote store or UI:

- immutable asset identity is canonical SHA-256 plus validated positive size;
- manifests use explicit schema v3 and deterministic canonical bytes;
- asset enumeration order cannot change the manifest digest;
- upload planning is content-based, so metadata-only edits upload no media and
  one new take uploads only its new object;
- reconciliation distinguishes no-op, local/remote-only, upload, download and
  conflict without timestamp-based winner selection;
- local state becomes server-confirmed only after the published head is read
  back and matches the desired revision;
- garbage collection protects every retained-manifest asset, pending asset and
  object inside the safety grace period.

U8a does not yet change production Drive bytes or claim end-to-end backup. Its
provider-neutral domain, complete `core:project` suite, Android unit suite,
Lint/build and complete API 36 regression passed in run `35678941252`. U8b is
the next checkpoint.

## Remaining U8 sequence

U8a–U8l have closed the provider-neutral domain, Drive v3 adapter, durable
transaction journal, transactional restore, reachability-safe GC, C6
presentation/Activity contracts, path-aware project snapshot/manifest format,
the production cutover to the vNext stack and the complete network/HTTP fault
matrix.

The **only remaining U8 gate** is controlled real-provider end-to-end proof:

1. execute the real Google Drive campaign against the production vNext path;
2. prove initial backup, metadata-only incremental backup and one-new-take
   incremental upload;
3. verify objects/manifests/append-only heads and confirmed-revision behavior;
4. restore and validate project state plus managed media;
5. exercise a real conflict and the explicit resolution actions;
6. validate safe GC/cleanup without deleting reachable or pending assets;
7. close U8 only after this real-provider gate is green.

Adopt `U8m` for this block unless newer normative documentation defines
another identifier.

## U8k production cutover — CLOSED / DIGITAL PASS

U8k makes the vNext architecture the single production backup/restore path.

Closed contracts:
- manual all-project and single-project backup route through `UnifiedDriveProductionService`, `UnifiedDriveProjectSnapshotBuilder`, durable journal/snapshot staging and `UnifiedDriveV3RemoteStore`;
- `AutomaticBackupWorker` uses the same production service, WorkManager retry/cancellation semantics and shared Activity/notification model;
- frozen snapshot bytes survive process death independently of the transaction journal;
- corrupt journal/snapshot state fails closed instead of being mistaken for “no transaction”;
- resumable sessions persist across process restart, recover server range with HTTP 308 and restart only after bounded session expiry;
- confirmed revision is written only from the exact descriptor after `HEAD_VERIFIED`;
- every “already synchronized” decision reconciles the live append-only remote head before skipping upload;
- restore downloads canonical manifest/objects, validates project state plus referenced media and publishes locally only after complete validation;
- replace-local restore is rollback-protected; import-as-copy allocates a fresh local identity while retaining media bytes unchanged;
- ambiguous concurrent remote heads fail closed and cannot silently replace local state;
- conflict actions remain GuitarLab-first: keep local, use cloud when the head is unique, or import cloud as copy;
- reachability-safe GC runs only after confirmed backup and protects retained manifests, pending assets and grace-period objects;
- the historical H37 coordinator/store and SAF migration remain retained code/evidence only; no normal ViewModel/Worker/UI path invokes them.

Canonical evidence:
- Android CI #750 / run `35742420939`;
- exact source `b5bf8d39008aa267d7411244ea0ae526142863f7`;
- deterministic U8k source materialization PASS;
- JVM/unit tests PASS;
- Android Lint PASS;
- debug/release candidate assembly PASS;
- complete API 36 emulator regression PASS;
- signed homologation APK SKIPPED intentionally because candidate freeze has not started.

Corrective history retained as evidence:
- #747 failed before compilation because the first U8k payload digest was sealed incorrectly;
- #748 exposed nullable resumable-session Kotlin wiring after the materializer was fixed;
- #749 exposed only test-constructor trailing-lambda ambiguity after production compilation succeeded;
- #750 closed all three issues on the exact source above.

## U8l network/HTTP fault injection

U8l status: **CLOSED / DIGITAL PASS** — Android CI #756 / run `35746523924`,
exact source `2cacbfe4c95bfbee694e717468075b8eedc7f9c6`.

U8l hardens and verifies the production vNext Drive transport rather than a
test-only adapter:

- ordinary requests retry bounded offline/timeout failures plus HTTP 429,
  retryable quota-class 403 and 500/502/503/504 responses;
- permanent permission-class 403 responses are not converted into transient
  retry;
- one rejected 401 refreshes the token once, while repeated 401 transitions to
  the explicit Drive authorization-required boundary;
- cancellation is never swallowed by network backoff;
- download retries delete stale/partial destination bytes before retry and
  leave no partial file after terminal failure;
- resumable session creation retries pre-upload network loss without sending a
  chunk first;
- chunk-response loss queries authoritative upload status before retransmitting
  bytes;
- lost final chunk response accepts a committed 200/201 status without blind
  replay;
- 308 Range recovery resumes from the server-confirmed offset;
- expired upload sessions restart within a bounded budget;
- malformed Range data fails closed;
- resumable auth/cancellation failures bypass generic network retry;
- paginated Drive listing accepts only non-empty, non-repeating page tokens and
  fails closed on malformed/cyclic pagination;
- automatic backup exposes transient network failure as RETRYING and never
  turns incomplete work into a confirmed/synchronized revision.

The canonical U8l source materializer is
`scripts/materialize_ci_sources_u8l.sh`, invoked by
`scripts/materialize_ci_sources.sh`. It chains from U8k and protects all eight
terminal production/test blobs. The canonical payload is
`.source-parts/U8lNetworkFaultGate.patch.b64` with payload SHA-256
`5ceb06ee612f437cd8fe2b2bf870e25745fb56e6d4cd3f30f43ccce1d2d67bae`
and decoded patch SHA-256
`6b21db17a22ccbc29d108b0136d26751769be7f1ae346107b44d735d79460c27`.
Ready-state acceptance additionally requires every protected Git blob to match
and a successful reverse-apply check.

Canonical gate evidence:
- deterministic U8l source materialization PASS;
- JVM/unit tests PASS;
- Android Lint PASS;
- debug/release assembly PASS;
- API 36 group 1 “Projeto e navegação” PASS — 9 tests observed;
- API 36 group 2 “Studio e prática” PASS — 1 test observed;
- API 36 group 3 “Importação, controles e ajustes” PASS — 1 test observed;
- API 36 group 4 “Exportação, backup e master” PASS — 2 tests observed;
- isolated tablet geometry 1920×1200 PASS — 1 test observed;
- signed homologation APK SKIPPED intentionally because candidate freeze has
  not started.

Corrective history remains diagnostic only:
- #751 exposed a JUnit assertion-signature issue before a complete gate;
- #752 exposed an incompletely merged remote-store fake harness;
- #753/#754 were superseded while the full eight-file source seal was being
  corrected;
- #755 reached compiled Unit Tests and exposed non-`Unit` JUnit test methods;
- #756 closed the corrected and fully sealed U8l source.

**Next block:** controlled real Google Drive end-to-end acceptance. Adopt
`U8m` for that source block unless a newer normative document defines a
different identifier. U8 itself remains OPEN until that real-Drive gate passes.

## U8b transactional coordinator

U8b adds the provider-neutral commit coordinator above the U8a domain:

- validates the frozen asset set, local sizes and SHA-256 before upload;
- uploads only absent content objects and verifies every server receipt;
- publishes and verifies the immutable manifest before any head record;
- checks the known base both before object transfer and immediately before
  publication;
- uses append-only head records and detects divergent descendants rather than
  assuming an undocumented Drive compare-and-swap primitive;
- recovers a lost final publish response by reading back the exact head;
- marks completion only after the head is uniquely observed;
- reports uploaded object count/bytes for deterministic efficiency evidence.

The complete unit/Lint/build and API 36 gate passed on the exact materialized
source. The existing H37 transport is not switched by this checkpoint. A Drive
v3 adapter for this interface and durable transaction state remain subsequent
U8 work.

## U8c Drive v3 adapter

U8c implements the concrete Drive API adapter without switching the user path:

- app-owned v3 objects are isolated by `drive.file`, root folder and explicit
  schema/kind properties;
- content objects are discovered by SHA-256 rather than filename;
- absent objects use resumable upload with 8 MiB chunks and server size/hash
  verification;
- manifests are immutable content objects with project/revision metadata;
- heads are append-only metadata records and are idempotent by revision;
- listing is paginated and malformed/incomplete records fail closed;
- the adapter reuses the hardened OAuth/HTTP client while the H37 production
  coordinator remains unchanged until the later cutover gate.

The complete unit/Lint/build and API 36 regression passed on the exact U8c
source. U8d durable desired/confirmed state and process-death recovery are
next.

## U8d durable transaction recovery

U8d makes the desired-versus-confirmed boundary durable before production
cutover:

- a per-project journal records the frozen revision, base revision, manifest
  digest and every verified commit stage;
- journal updates publish through a same-directory atomic rename, with a
  portable replacement fallback when atomic moves are unavailable;
- malformed journal data fails closed and cannot be mistaken for a confirmed
  revision;
- an incomplete transaction can resume only with the exact same revision,
  manifest and base; a different revision cannot overwrite pending work;
- cancellation immediately after remote head publication leaves recoverable
  local intent, and retry converges idempotently without a duplicate head;
- the journal becomes confirmed only after the coordinator reads back and
  uniquely verifies the desired head.

Transactional restore and explicit conflict actions are the next U8
checkpoint after the U8d CI gate.

## U8e transactional restore and conflict actions

U8e status: CLOSED / DIGITAL PASS — Android CI #732 / run `35710130595`,
exact source `4aa852db9e13717b85b438bcd9f4054383cdbc0b`

The provider-neutral restore boundary now:

- verifies the immutable descriptor against the loaded manifest before any
  asset is eligible for publication;
- requires the resolved target set to match the manifest asset set exactly;
- rejects absolute, traversal and platform-ambiguous staging paths;
- downloads every object into an isolated staging tree and validates exact
  size plus SHA-256 before running full-project validation;
- delegates only a completely validated tree to the atomic local publisher;
- removes staging on success, corruption, cancellation or validation failure,
  so an existing local project is never touched by a partial restore;
- exposes keep-local, use-Drive and import-as-copy as explicit conflict
  actions, with action availability derived from reconciliation state rather
  than timestamps.

Reachability-based retention/GC is the next U8 checkpoint after the U8e CI
gate.

## U8f reachability-safe garbage collection

U8f status: CLOSED / DIGITAL PASS — Android CI #733 / run `35711364276`,
exact source `b76433c516102279d40f9f57a66e9dd425655482`

U8f turns the U8a deletion predicate into an executable global collection
boundary:

- retention is evaluated independently per immutable project identity;
- each project's newest committed manifest is retained even when it exceeds
  the normal age window;
- content is deleted only when unreachable from every retained manifest
  across every project;
- assets referenced by a pending transaction and assets inside the safety
  grace period remain protected;
- object deletion is best-effort and reports individual failures without
  invalidating an already successful backup commit.

The next U8 checkpoint integrates scheduling, Activity/Home sync state and the
unified Conta e nuvem presentation.

## U8g shared Activity and sync-state domain

U8g status: CLOSED / DIGITAL PASS — Android CI #734 / run `35712643271`,
exact source `be05ae66a0d1d1f54ca493a18e38b3eae447d306`

The integration starts from one provider-neutral presentation contract:

- source acquisition, separation, reference preparation, export, backup and
  restore share the same operation kind/state/progress vocabulary;
- Activity ordering deduplicates durable operation IDs, prefers their newest
  snapshot and keeps active work ahead of history;
- Home's compact progress derives from the same ordered records instead of a
  second job model;
- project sync state distinguishes disconnected, local-only, pending,
  syncing, synchronized, failed and conflict states;
- worker completion alone cannot produce a false synchronized state: the
  server-confirmed revision must equal the current local revision.

Android persistence and the Activity/Home/Conta e nuvem surfaces are the next
U8g integration cut after this domain gate.

## U8h Android Activity and cloud presentation

U8h closes the Android integration cut for the shared Activity model. Durable
Activity snapshots are persisted and observed by one Android store; source,
separation, reference preparation, export, backup and restore are represented
in one Activity surface; Home shows compact active-operation progress and
project sync labels; and Settings presents one `Conta e nuvem` hierarchy with
separate processing and Google Drive capabilities.

Exact U8h materialization, JVM/unit, Lint, debug APK assembly and all API 36
regression groups passed in Android CI #739 / run `35721268453`.

U8h did not close the full U8 program.

## U8i confirmed sync boundary and Activity deep-links

U8i closes C6. The Android layer now persists the exact project revision
descriptor confirmed by the remote coordinator; a successful worker status by
itself cannot mark a project synchronized. Home derives its compact sync label
from the current deterministic local revision versus that confirmed revision,
while active backup operations may only present transient pending/syncing
states. Disconnect/delete clear stale local confirmation ownership.

Backup foreground notifications now carry a typed route to the owning Activity
operation. Cold start and `onNewIntent` both consume the same route contract,
and Activity focuses the referenced durable operation. The regression avoids
test-only lifecycle shortcuts by launching the exact deep-link intent in its
own managed ActivityScenario.

Android CI #744 / run `35729709715` passed deterministic materialization,
JVM/unit, performance evidence, Lint, debug/release assembly and all API 36
groups at exact source `89a7cca5acd2d5a1c9aabbb5f72e028347dd00db`.

U8 remains open. Static production-usage audit confirms the vNext
`UnifiedDriveV3RemoteStore`, durable commit coordinator, transactional restore
and reachability GC are still not the active product path. The next cut must
complete that production backup/restore cutover before network-fault and real
Drive integration can close U8.


## U8j path-aware project snapshots — CLOSED / DIGITAL PASS

U8j supplies the missing restorable project-layout contract required before the production vNext
cutover:

- `UnifiedDriveProjectSnapshotBuilder` freezes the persisted `GuitarProject` into immutable staging without audio conversion;
- `project.json` is stored as a dedicated content-addressed object;
- every referenced managed-media path is normalized, confined to the project root, copied byte-for-byte and verified against tracked size/SHA-256 when metadata exists;
- identical bytes at different project paths deduplicate to one remote object while the manifest retains every path mapping;
- complete schema-v3 manifests carry `projectStateAsset` plus canonical path-aware `fileEntries`, reject duplicate/unsafe layouts and reparse only if canonical bytes reproduce exactly;
- `UnifiedDriveProjectRestoreLayout` deterministically reconstructs `project.json` plus managed media under their original relative paths;
- `UnifiedDriveV3RemoteStore` now implements both backup transport and restore source, downloading manifests/assets by exact identity with size and SHA-256 validation;
- state-only project revisions are covered, so a metadata-only project remains restorable without requiring media objects.

The fail-closed source stage is `scripts/materialize_ci_sources_u8j.sh`, chained from U8i through
`scripts/materialize_ci_sources.sh`. The locked payload is
`.source-parts/U8jPathAwareProjectSnapshots.patch.b64` with payload SHA-256
`ef6abfca3cd26c381352d8c6fbca9d528757096c100bd706b5c62bad11dac210` and decoded patch SHA-256
`5823349c252dccf7379b4fe7a05b811dcde5f413ca573610dbf22553f6de95ea`.

Android CI #746 / run `35732391636` passed the exact source
`ca44bd8efada617bbc30c8c3f3cf9f3d6b18a3c1`: deterministic materialization, JVM/unit tests,
performance evidence, Android Lint, debug/release assembly and complete API 36 regression all
succeeded. Signed homologation was intentionally skipped because U8 is not yet at candidate freeze.

**U8j is fully closed.** The next source block must perform the production cutover: current
`BackupViewModel` and `AutomaticBackupWorker` still instantiate the historical
`ProjectBackupCoordinator` + `DriveV3BackupRemoteStore`, while
`UnifiedDriveProjectSnapshotBuilder`, `DurableUnifiedDriveBackupCoordinator`,
`UnifiedDriveV3RemoteStore`, `UnifiedDriveRestoreCoordinator` and reachability GC are not yet
the active user path.
