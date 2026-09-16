# GuitarLab Studio

Android-first guitar practice, recording, comparison and mixing workspace.

Updated: 2026-09-16

## Repository truth
The repository is the canonical source for scope, architecture, implementation state and homologation evidence. Chat history is supplementary only.

Read first:
- `docs/CURRENT_STATE.md` — authoritative live candidate/gate state;
- `docs/IMPLEMENTATION_ROADMAP.md` — milestone sequence and remaining release work;
- `docs/H24_HOME_PROJECT_LIBRARY.md` — Home project search/filter/sort contract;
- `docs/ARCHITECTURE.md` — current module, media, recording, Home-library and CI architecture;
- `docs/TEST_AND_HOMOLOGATION_PLAN.md` — automated + residual physical gate policy;
- `docs/RC3_FINAL_PHYSICAL_HOMOLOGATION.md` — active residual physical checklist;
- `docs/CANDIDATE_IDENTITY_POLICY.md` — source/version/signer/checksum identity contract;
- `docs/DOCUMENTATION_MAP.md` — active vs historical document map.

## Active RC3 state
Candidate: `0.5.0-rc3`, versionCode `23`, package `studio.guitarlab.app`.

The last fully signed digital authority is **CI #639**, run `35096711936`, exact source `eb9c4a4ca2a6269fbe2f2211a0807b8c703e115c`. It passed 260/260 JVM/unit tests, Android Lint/build/provenance, API36 standard 23/23, isolated 1920×1200 geometry 1/1 and signed homologation. Signed APK SHA-256: `ffac9da48c17fe2bd28172d357c2f45e906c15b20a216443c1b8b78a9a893696`.

H23b is therefore DIGITAL PASS. Focused SM-X230 + MK-300 physical recording-timing validation remains pending.

## H24 — Home Project Library
H24 is implemented and source-validated after CI #639. It adds efficient project search, combinable filters and deterministic ordering to Home without changing project schema or `.guitarlab` format.

The implementation is materialized through `.source-parts/H24HomeProjectLibrary.patch.gz.part00` and verified by `scripts/materialize_ci_sources.sh`. It remains **PRE-GATE** until a new user-dispatched full signed workflow succeeds on the then-current `main` SHA.

## Current branch policy
- `main` is canonical.
- Ordinary development/documentation commits use `[skip ci]`.
- `.github/workflows/android-ci.yml` remains manual-only (`workflow_dispatch`).
- The assistant must not dispatch or rerun Actions.
- Historical alpha/review/audit files remain evidence of their original checkpoints; they do not override active-state documents.

## Build and source materialization
`scripts/build_local.sh` is the local software gate when the required Gradle/Android SDK environment is available. `.github/workflows/android-ci.yml` is the canonical full software/API36/geometry/signing executor.

Large RC3 deltas are versioned under `.source-parts` and applied serially by `scripts/materialize_ci_sources.sh`. The current canonical tail ends at **H24** and must fail closed on source drift.

## Physical validation policy
Automatable mathematics, persistence invariants, timing policy, malformed-input handling, lifecycle behavior, accessibility semantics, generic geometry and Home-library selection logic are automated responsibilities. Physical review is reserved for target-device behavior that cannot be established digitally, especially real MK-300 routing, recording alignment, touch ergonomics and listening.

## Security
Never commit keystores, credentials, local SDK configuration or secret artifacts.
