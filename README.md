# GuitarLab Studio

Android-first guitar practice, recording, comparison and mixing workspace.

Updated: 2026-09-17

## Repository truth
The repository is canonical for scope, architecture, implementation state and homologation evidence.

Read first: `docs/CURRENT_STATE.md`, `docs/POST_H28_HARDENING_AND_EXTERNAL_CONTROL_PLAN.md`, `docs/H28_BACKUP_IDENTITY_CONSISTENCY.md`, `docs/IMPLEMENTATION_ROADMAP.md`, `docs/TEST_AND_HOMOLOGATION_PLAN.md`, `docs/RC3_FINAL_PHYSICAL_HOMOLOGATION.md`, `docs/CANDIDATE_IDENTITY_POLICY.md` and `docs/DOCUMENTATION_MAP.md`.

## Active RC3 state
Candidate line: `0.5.0-rc3`, versionCode `23`, package `studio.guitarlab.app`.

The current signed digital authority is **CI #653** / run `35207902169` / exact producer `d09fc003e2ae2d699238eb39ba699f75a746fea4`, with H28 DIGITAL PASS. Signed APK SHA-256: `1a36efcbe24d5995dd3609889237ca112670e52554e187d1b93ed5a8649263c0`.

Target-device validation after #653 confirmed the corrected H28 backup workflow is functioning correctly. The remaining release-critical physical boundary is recording latency/synchronization on the intended USB route.

## H28 — stable project/revision identity + provider consistency
H28 hardens the backup contract so that:
- `GuitarProject.id` is the immutable backup identity; renaming never creates a new project history;
- each new revision has deterministic identity from edit timestamp + canonical-state digest;
- package SHA-256 remains independent byte-integrity evidence;
- version paths are deterministic per revision;
- commit success is verified through the exact remote URIs written rather than an immediately refreshed directory listing;
- targeted revision lookup absorbs normal provider listing delay before retry upload;
- legacy H26/H27 backups remain readable;
- deduplication cannot collapse two different H28 states merely because they share a timestamp.

## Approved forward scope
Before stable `1.0.0`, the project will harden existing recording/USB/recovery/takes/diagnostics/backup/UX behavior and establish a realistic quality boundary for song projects through 10 minutes. The only approved new feature after that stable line is external MIDI/footswitch control. See `docs/POST_H28_HARDENING_AND_EXTERNAL_CONTROL_PLAN.md`.

## Branch/CI policy
- `main` is canonical.
- `.github/workflows/android-ci.yml` is manual-only (`workflow_dispatch`).
- ordinary source/docs commits use `[skip ci]`.
- the assistant must not dispatch or rerun Actions without explicit user instruction.

## Source materialization
Large RC3 deltas are versioned in `.source-parts` and materialized serially by `scripts/materialize_ci_sources.sh`. Canonical tail: **`… → H25 → H26 → H26a → H26b → H26e → H27 → H28`**. Unexpected source/hash drift fails closed.

## Security
Never commit keystores, credentials, local SDK configuration or secret artifacts. Backup provider access remains scoped to the user-selected document tree.
