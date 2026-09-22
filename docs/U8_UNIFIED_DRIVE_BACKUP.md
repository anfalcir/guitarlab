# U8 unified Drive backup

Updated: 2026-09-22
Status: U8a CLOSED / DIGITAL PASS — Android CI run `35678941252`, exact source
`a5d851ce8becd40ef08e95fd302424ac7ea4c082`

U8b status: CLOSED / DIGITAL PASS — Android CI #729 / run `35679878744`,
exact source `b3ecd5f5269728e9e481156a187922c16b93c05b`

U8c status: CLOSED / DIGITAL PASS — Android CI #730 / run `35707441794`,
exact source `58aad5cd230c9bb3700529dbf4b97498e08b429a`

U8d status: SOURCE COMPLETE / PRE-GATE — durable transaction journal and
process-death convergence implemented; exact CI evidence pending.

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

1. snapshot and authoritative-asset enumeration;
2. Drive v3 object/manifest/head transport with resumable verified uploads;
3. durable desired/confirmed revision state, retry and process-death recovery;
4. staged transactional restore and conflict actions;
5. reachability-based retention/GC;
6. C6 Activity, Home sync state and unified “Conta e nuvem” presentation;
7. network-fault, instrumentation and real Drive integration gates.

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
