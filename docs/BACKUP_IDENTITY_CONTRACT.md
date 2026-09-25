# Backup Identity Contract

Updated: 2026-09-24

This contract protects current GuitarLab backup/restore identity independently from transport. Historical H28 implementation and acceptance evidence is retained in `history/H28_BACKUP_IDENTITY_CONSISTENCY.md`.

## Project identity

`GuitarProject.id` is immutable and authoritative.

- display name is never identity;
- rename preserves `projectId`;
- intentional duplicate receives a new `projectId`;
- restore publishes an independent local project with a new identity;
- backup history groups by immutable source `projectId`.

## Revision identity

Every backup revision has a deterministic logical `revisionId` derived from persisted edit time and canonical project state, including immutable project identity.

Two distinct states must not collapse merely because timestamps match. Package SHA-256 independently identifies the exact bytes. Deduplication therefore uses logical revision identity plus package integrity.

## Commit identity

A remote revision is committed only when metadata, package and commit marker agree on:

- project identity;
- revision identity;
- package size;
- package SHA-256;
- schema/format version;
- committed state.

Incomplete or mismatched uploads never appear as restorable committed revisions.

## Idempotency and provider consistency

- retrying the same unchanged revision targets the same logical remote identity;
- server-confirmed state is authoritative;
- targeted revision lookup handles catalog/listing lag before any re-upload;
- a lost final response reconciles before retransmission;
- duplicate cleanup collapses only equivalent revision/content;
- bounded retention must never delete assets/revisions still protected by current state or policy.

## Restore

Restore downloads, stages and validates the complete package before publication. It must reject malformed, incompatible, unsafe or integrity-mismatched data without mutating an existing project.

Successful restore publishes a new independent project identity. Repeating restore must not overwrite the prior local project silently.

## Support boundary

Current unified-line backups and owner-important data remain protected. Automatic import/migration of historical GBW/H37/pre-unification backup corpora is outside scope under D-082.

## Qualification

When backup identity, transport, retention or restore changes, focused regression must cover:

- rename versus duplicate identity;
- deterministic revision identity and same-timestamp distinct states;
- unchanged backup idempotency;
- provider lag/lost-response reconciliation;
- size/hash/commit validation;
- retention safety;
- transactional restore-as-copy;
- malformed/unsafe package rejection.

Physical/provider-real repetition is required only when the change can invalidate provider behavior or Drive is deliberately included in the frozen candidate's physical acceptance.
