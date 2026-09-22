# U8 unified Drive backup

Updated: 2026-09-22
Status: U8a SOURCE / PRE-GATE

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
next gate is the complete `core:project` test suite followed by Android CI on
the exact materialized source.

## Remaining U8 sequence

1. snapshot and authoritative-asset enumeration;
2. Drive v3 object/manifest/head transport with resumable verified uploads;
3. durable desired/confirmed revision state, retry and process-death recovery;
4. staged transactional restore and conflict actions;
5. reachability-based retention/GC;
6. C6 Activity, Home sync state and unified “Conta e nuvem” presentation;
7. network-fault, instrumentation and real Drive integration gates.
