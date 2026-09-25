# Project portability contract

Updated: 2026-09-25

## Core invariant

`.guitarlab` is a versioned round-trip project container. A package that reports success but cannot be safely reopened with its required project media is a failed persistence feature.

## Package contents and identity

A portable package carries project metadata plus the managed authoritative media required by the project state. Derived/regenerable caches may be omitted when safe to rebuild.

- project/display filenames are not identity;
- manifest/media references must agree;
- referenced managed media must be present and integrity-valid;
- path traversal, duplicate/ambiguous entries and unsafe resource bounds are rejected;
- package schema/version is validated before publication.

## Save transaction

Package creation is staged. Failure or cancellation must not replace a known-good destination with a partial package. Save never mutates the authoritative managed sources inside the open project.

## Import/restore transaction

Import extracts into staging, validates schema, paths, media facts, required assets and integrity, then publishes atomically. Invalid/incompatible packages must fail without corrupting an existing project.

Where current code applies narrow compatibility recovery after decode, package/state integrity is checked against the persisted canonical representation first. Recovery is deterministic/idempotent and occurs only after those stored bytes/state are accepted.

An imported/restored copy receives an independent local project identity rather than silently overwriting an existing project with the source identity.

## Support boundary

Current unified GuitarLab packages are supported. Retired standalone GBW/H37/pre-unification migration is not implied by this contract.

## Qualification

When package schema, save/import, managed-media reachability or integrity rules change, cover deterministic save/reopen, missing/corrupt media, malformed archive/path attacks, cancellation/failure cleanup and independent restored-project identity.
