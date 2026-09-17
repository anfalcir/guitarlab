# H26 — SAF Cloud Backup / Restore

Status: **DIGITAL PASS at CI #650 / H26e; product semantics superseded by H27 corrective**
Updated: 2026-09-16

## H26 evidence boundary
CI #650 / producer `07c99155789774cb39f9b4382829f9e1d16649e3` proved the H26/H26e implementation end to end digitally: 285/285 JVM/unit, Lint/build/provenance, API36 31/31 standard + 1/1 isolated geometry and signed homologation all passed.

That evidence remains valid for the source tested by #650. Physical use then exposed release-level history/copy semantics that require H27 before final approval.

## Stable H26 architecture retained by H27
- user-selected provider-neutral document-tree destination with persisted scoped read/write access;
- real destination probe before adoption;
- safe target-folder change/disconnect without deleting existing remote content;
- full and single-project backup;
- automatic coalesced/incremental + periodic WorkManager backup;
- one-version and restore-all flows as independent local copies, never silent overwrite;
- transactional remote package publication with integrity verification and a final commit marker;
- incomplete/invalid versions excluded from restore;
- process-wide operation lock across backup/restore;
- fail-safe cancellation and cleanup boundaries;
- long-running automatic transfer foreground handling.

These implementation details remain internal architecture; ordinary product UI should describe the user outcome rather than expose Android API/protocol vocabulary.

## H26 behavior corrected by H27
The #650/H26 contract used:
- manual `force = true`, which could create another version for an unchanged persisted project revision;
- a protected **minimum** version count that overrode age cleanup.

H27 supersedes those release semantics with:
- manual and automatic backup both incremental/idempotent for the same persisted revision;
- a **maximum** history size per project;
- automatic collapse of duplicate copies of the same revision;
- newest valid version always preserved;
- maximum-count cleanup active even when the age limit is disabled;
- clearer last-run and per-project failure reporting;
- end-user release copy with internal implementation terms removed from normal screens.

See `H27_BACKUP_HISTORY_RELEASE_UX.md` for the authoritative current behavior.

## Materialization history
H26/H26e canonical tail was `… → H25 → H26 → H26a → H26b → H26e` and was proven by #650.

Current source tail is now `… → H25 → H26 → H26a → H26b → H26e → H27`.

H26c/H26d remain historical recovery experiments and are not part of the current materialization path.

## Promotion boundary
CI #650 must not be described as containing or validating H27. A fresh manually dispatched signed workflow is required before the corrected backup behavior can become DIGITAL PASS or be rebound to final physical homologation.
