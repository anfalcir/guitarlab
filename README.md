# GuitarLab Studio

Android-first guitar practice, recording, comparison and mixing workspace.

Updated: 2026-09-16

## Repository truth
The repository is canonical for scope, architecture, implementation state and homologation evidence.

Read first: `docs/CURRENT_STATE.md`, `docs/H28_BACKUP_IDENTITY_CONSISTENCY.md`, `docs/IMPLEMENTATION_ROADMAP.md`, `docs/TEST_AND_HOMOLOGATION_PLAN.md`, `docs/RC3_FINAL_PHYSICAL_HOMOLOGATION.md`, `docs/CANDIDATE_IDENTITY_POLICY.md` and `docs/DOCUMENTATION_MAP.md`.

## Active RC3 state
Candidate line: `0.5.0-rc3`, versionCode `23`, package `studio.guitarlab.app`.

The last signed digital authority is **CI #651** / run `35166195527` / exact producer `0b6ae1e28214decbcfba622a38d90c0dcbe2acf9`, with H27 DIGITAL PASS. Its signed APK SHA-256 is `d9ce720194812afcb281ecebd263d482d4320b4285f50044a2771fc6293736fe`.

Physical validation of that exact build exposed an eventually-consistent provider confirmation/deduplication defect. **H28 is IMPLEMENTED / SOURCE-VALIDATED / PRE-GATE** and supersedes #651 for final backup homologation.

## H28 — stable project/revision identity + provider consistency
H28 hardens the backup contract so that:
- `GuitarProject.id` is the immutable backup identity; renaming never creates a new project history;
- each new revision has deterministic `revisionId = edit timestamp + canonical-state digest`;
- package SHA-256 remains an independent byte-integrity identity;
- version paths are deterministic per revision;
- commit success is verified through the exact remote URIs written, not an immediately refreshed directory listing;
- a short targeted revision lookup absorbs normal cloud-provider listing delay before any retry upload;
- legacy H26/H27 backups remain readable;
- deduplication cannot collapse two different H28 states merely because they share the same timestamp.

## Branch/CI policy
- `main` is canonical.
- `.github/workflows/android-ci.yml` is manual-only (`workflow_dispatch`).
- ordinary source/docs commits use `[skip ci]`.
- the assistant must not dispatch or rerun Actions without explicit user instruction.

## Source materialization
Large RC3 deltas are versioned in `.source-parts` and materialized serially by `scripts/materialize_ci_sources.sh`. Canonical tail: **`… → H25 → H26 → H26a → H26b → H26e → H27 → H28`**. Unexpected source/hash drift fails closed.

## Security
Never commit keystores, credentials, local SDK configuration or secret artifacts. Backup provider access remains scoped to the user-selected document tree.
