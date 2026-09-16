# GuitarLab Studio

Android-first guitar practice, recording, comparison and mixing workspace.

Updated: 2026-09-16

## Repository truth
The repository is canonical for scope, architecture, implementation state and homologation evidence.

Read first: `docs/CURRENT_STATE.md`, `docs/IMPLEMENTATION_ROADMAP.md`, `docs/H26_SAF_CLOUD_BACKUP.md`, `docs/H25_UI_SETTINGS_SAFETY.md`, `docs/TEST_AND_HOMOLOGATION_PLAN.md`, `docs/RC3_FINAL_PHYSICAL_HOMOLOGATION.md`, `docs/CANDIDATE_IDENTITY_POLICY.md` and `docs/DOCUMENTATION_MAP.md`.

## Active RC3 state
Candidate line: `0.5.0-rc3`, versionCode `23`, package `studio.guitarlab.app`.

Current signed authority remains **CI #642** / run `35121955150` / exact producer `7bfd876b6a5b0701ab0cf5203de36c31cd117632`, DIGITAL PASS through H25.

Evidence: **269/269 JVM/unit PASS**, Android Lint/build/unsigned provenance PASS, API36 **28/28 standard + 1/1 isolated geometry**, signed homologation PASS. Signed APK SHA-256: `916758f694735febb8cccfe58f46f290562aaba1b5483ccc727e2be0cefba5aa`.

## H26 — SAF Cloud Backup
H26 is **IMPLEMENTED / SOURCE-VALIDATED / PRE-GATE**. It adds provider-neutral backup and restore through Android Storage Access Framework (SAF), including full/single-project backup, full/single-version restore, automatic incremental backup, configurable target folder, retention by age with protected minimum versions, remote SHA-256 verification and fail-closed transactional commit markers.

H26 is a source change and is **not** contained in the #642 APK. A new manually dispatched signed workflow is required before H26 can become DIGITAL PASS.

## Branch/CI policy
- `main` is canonical.
- `.github/workflows/android-ci.yml` remains manual-only (`workflow_dispatch`).
- ordinary source/docs commits use `[skip ci]`.
- the assistant must not dispatch or rerun Actions without explicit user instruction.

## Source materialization
Large RC3 deltas are versioned in `.source-parts` and materialized serially by `scripts/materialize_ci_sources.sh`. The canonical tail is now **`… → H23b → H24 → H24a → H25 → H26`** and fails closed on unexplained source drift.

## Security
Never commit keystores, credentials, local SDK configuration or secret artifacts. H26 uses user-selected SAF document-tree access and does not require broad Google Drive credentials or Drive API access.
