# GuitarLab Studio

Android-first guitar practice, recording, comparison and mixing workspace.

Updated: 2026-09-20

## Repository truth
The repository is canonical for scope, architecture, implementation state and homologation evidence.

Read first: `docs/CURRENT_STATE.md`, `docs/H35_TAKE_SYNC_QUIET_CALIBRATION.md`, `docs/POST_H28_HARDENING_AND_EXTERNAL_CONTROL_PLAN.md`, `docs/H28_BACKUP_IDENTITY_CONSISTENCY.md`, `docs/IMPLEMENTATION_ROADMAP.md`, `docs/TEST_AND_HOMOLOGATION_PLAN.md`, `docs/RC3_FINAL_PHYSICAL_HOMOLOGATION.md`, `docs/CANDIDATE_IDENTITY_POLICY.md` and `docs/DOCUMENTATION_MAP.md`.

## Active RC3 state
Candidate line: `0.5.0-rc3`, versionCode `23`, package `studio.guitarlab.app`.

The current signed digital authority is **CI #657** / run `35290128876` / exact producer `e371bb2a5c8040c668b926b2077c03d1c7c8c7d6`, with H29-H33/H33a/H33b DIGITAL PASS. Signed APK SHA-256: `05d6eaf71fb69ce55b28b8e3214e619a15f3b862dada973740927c2b9ecf2cd6`.

H34/H35 are now **SOURCE PRE-GATE READY** on top of signed #657. H34 expands the global route/rate-scoped residual adjustment to ±500 ms. H35 then separates that global value from a new persistent take-specific synchronization edit and hardens calibration UX: global adjustment affects future recordings only; an existing take can be adjusted from its edit UI without touching audio bytes; silent digital verification uses PCM zero; physical round-trip calibration confirms the exact live routes before emitting a short low-level adaptive chirp. #657 remains the signed authority until this source receives its own exact-source digital gate.

Target-device validation after #653 had already accepted the H28 backup corrective. The remaining stable-release physical boundary continues to be H29 timing/alignment, H30 real USB hot-unplug/reconnect/capture preservation and H32 continuous 10-minute quality. H33 real-controller acceptance remains a separate 1.1 boundary.

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
Large deltas are versioned in `.source-parts` and materialized serially by `scripts/materialize_ci_sources.sh`. Canonical tail: **`… → H25 → H26 → H26a → H26b → H26e → H27 → H28 → H29 → H30 → H31 → H32 → H33 → H33a → H33b → H34 → H35`**. The H28 materializer is frozen byte-for-byte as `scripts/materialize_ci_sources_through_h28.sh`; the post-H28 tail through H35 is isolated in `scripts/materialize_ci_sources_h29_h33.sh`. Unexpected source/archive/patch/final-blob drift fails closed.

## Security
Never commit keystores, credentials, local SDK configuration or secret artifacts. Backup provider access remains scoped to the user-selected document tree.
