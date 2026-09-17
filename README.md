# GuitarLab Studio

Android-first guitar practice, recording, comparison and mixing workspace.

Updated: 2026-09-16

## Repository truth
The repository is canonical for scope, architecture, implementation state and homologation evidence.

Read first: `docs/CURRENT_STATE.md`, `docs/H27_BACKUP_HISTORY_RELEASE_UX.md`, `docs/IMPLEMENTATION_ROADMAP.md`, `docs/TEST_AND_HOMOLOGATION_PLAN.md`, `docs/RC3_FINAL_PHYSICAL_HOMOLOGATION.md`, `docs/CANDIDATE_IDENTITY_POLICY.md` and `docs/DOCUMENTATION_MAP.md`.

## Active RC3 state
Candidate line: `0.5.0-rc3`, versionCode `23`, package `studio.guitarlab.app`.

The last signed digital authority remains **CI #650** / run `35154021384` / exact APK producer `07c99155789774cb39f9b4382829f9e1d16649e3`, with H26/H26e DIGITAL PASS. Its signed APK SHA-256 is `fdb870d0996b7ee4ec6f72028af9c7291d4767b4303cbd5fa62894288757d2fe`.

Physical validation of that build exposed a backup-history/product-copy defect. **H27 is IMPLEMENTED / SOURCE-VALIDATED / PRE-GATE** and supersedes #650 for final backup homologation.

## H27 — backup history + release UX
H27 changes the product contract so that:
- manual backup is idempotent for an unchanged persisted project revision;
- automatic and manual runs cannot intentionally create duplicate copies of the same revision;
- the configured version count is a **maximum history size per project**, not a protected minimum;
- duplicate versions of the same revision are cleaned automatically on a safe run;
- the newest valid version is always preserved;
- backup status reports the concrete project failure instead of only a generic count;
- user-facing copy describes product behavior rather than implementation details, APIs, development context or personal hardware.

A fresh manually dispatched signed CI is required before H27 can become DIGITAL PASS or a new physical candidate can be approved.

## Branch/CI policy
- `main` is canonical.
- `.github/workflows/android-ci.yml` is manual-only (`workflow_dispatch`).
- ordinary source/docs commits use `[skip ci]`.
- the assistant must not dispatch or rerun Actions without explicit user instruction.

## Source materialization
Large RC3 deltas are versioned in `.source-parts` and materialized serially by `scripts/materialize_ci_sources.sh`. The canonical tail is **`… → H25 → H26 → H26a → H26b → H26e → H27`** and fails closed on unexplained source/hash drift.

## Security
Never commit keystores, credentials, local SDK configuration or secret artifacts. Backup provider access remains scoped to the user-selected document tree.
