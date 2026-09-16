# GuitarLab Studio

Android-first guitar practice, recording, comparison and mixing workspace.

Updated: 2026-09-16

## Repository truth
The repository is canonical for scope, architecture, implementation state and homologation evidence.

Read first: `docs/CURRENT_STATE.md`, `docs/IMPLEMENTATION_ROADMAP.md`, `docs/H25_UI_SETTINGS_SAFETY.md`, `docs/TEST_AND_HOMOLOGATION_PLAN.md`, `docs/RC3_FINAL_PHYSICAL_HOMOLOGATION.md`, `docs/CANDIDATE_IDENTITY_POLICY.md` and `docs/DOCUMENTATION_MAP.md`.

## Active RC3 state
Candidate: `0.5.0-rc3`, versionCode `23`, package `studio.guitarlab.app`.

Current signed authority is **CI #642** / run `35121955150` / exact producer `7bfd876b6a5b0701ab0cf5203de36c31cd117632`, DIGITAL PASS through H25.

Evidence: **269/269 JVM/unit PASS**, Android Lint/build/unsigned provenance PASS, API36 **28/28 standard + 1/1 isolated geometry**, signed homologation PASS. Signed APK SHA-256: `916758f694735febb8cccfe58f46f290562aaba1b5483ccc727e2be0cefba5aa`.

H25 therefore includes the rounded-square interaction feedback correction, dedicated calibration modal, consolidated diagnostics and explicit project-delete confirmation in the signed physical candidate.

## Branch/CI policy
- `main` is canonical.
- `.github/workflows/android-ci.yml` remains manual-only (`workflow_dispatch`).
- ordinary source/docs commits use `[skip ci]`.
- the assistant must not dispatch or rerun Actions without explicit user instruction.

## Source materialization
Large RC3 deltas are versioned in `.source-parts` and materialized serially by `scripts/materialize_ci_sources.sh`. The canonical tail ends at **H25** and fails closed on unexplained source drift.

## Physical closure
No new deterministic CI is required merely to reconfirm #642. Remaining work is residual target-device homologation on the exact #642 APK, primarily H25 visual/interaction smoke plus retained H24 Home and H23b MK-300 recording-timing checks.

## Security
Never commit keystores, credentials, local SDK configuration or secret artifacts.
