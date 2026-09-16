# GuitarLab Studio

Android-first guitar practice, recording, comparison and mixing workspace.

Updated: 2026-09-16

## Repository truth
The repository is canonical for scope, architecture, implementation state and homologation evidence.

Read first: `docs/CURRENT_STATE.md`, `docs/IMPLEMENTATION_ROADMAP.md`, `docs/H25_UI_SETTINGS_SAFETY.md`, `docs/TEST_AND_HOMOLOGATION_PLAN.md`, `docs/RC3_FINAL_PHYSICAL_HOMOLOGATION.md`, `docs/CANDIDATE_IDENTITY_POLICY.md` and `docs/DOCUMENTATION_MAP.md`.

## Active RC3 state
Candidate line: `0.5.0-rc3`, versionCode `23`, package `studio.guitarlab.app`.

Last signed authority is CI #641 / run `35105065689` / producer `b11769f340f7056c37dfb17d95b062909dad87bf`, DIGITAL PASS through H24a: 269/269 JVM/unit, Lint/build/provenance, API36 **25/25 standard + 1/1 isolated geometry**, signed homologation. Signed APK SHA-256: `d3698067ed7117d3c3c844b0d94bb117c3448cac3da2897329e4dbfcd71e0f39`.

## H25 — current source block
H25 is **IMPLEMENTED / SOURCE-VALIDATED / PRE-GATE**. It aligns rounded-square interaction feedback with button geometry, moves calibration details/actions into a dedicated modal, consolidates diagnostics and requires explicit confirmation before project deletion. It also synchronizes Help and adds focused Android tests.

Because H25 changes product/test source after #641, the #641 APK remains historical signed evidence through H24a but is not the H25 physical candidate. One fresh manual full signed workflow is required after H25 lands.

## Branch/CI policy
- `main` is canonical.
- `.github/workflows/android-ci.yml` is manual-only (`workflow_dispatch`).
- ordinary source/docs commits use `[skip ci]`;
- the assistant must not dispatch or rerun Actions.

## Source materialization
Large RC3 deltas are versioned in `.source-parts` and materialized serially by `scripts/materialize_ci_sources.sh`. The canonical tail now ends at **H25** and fails closed on unexplained source drift.

## Security
Never commit keystores, credentials, local SDK configuration or secret artifacts.
