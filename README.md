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

The current fully signed digital authority is **CI #641**, run `35105065689`, exact producer source `b11769f340f7056c37dfb17d95b062909dad87bf`.

It passed:
- **269/269** JVM/unit tests;
- Android Lint/build/unsigned provenance;
- API36 standard **25/25**;
- isolated 1920×1200 geometry **1/1**;
- signed homologation with the locked certificate.

Signed APK SHA-256: `d3698067ed7117d3c3c844b0d94bb117c3448cac3da2897329e4dbfcd71e0f39`.

Canonical Android reporting is **25/25 standard + 1/1 isolated geometry**.

## H24/H24a — Home Project Library — DIGITAL PASS
Home now provides efficient project search, combinable filters and deterministic ordering without changing project schema or `.guitarlab` format.

Search/filter/sort operates over an immutable normalized in-memory index. Typing does not reread project files. H24a corrects only the Android-test compile import found by historical CI #640.

CI #641 provides the full digital proof through H24a. Remaining H24 work is only a short target-tablet UX/touch smoke.

## H23b physical residual
Focused SM-X230 + MK-300 recording-timing validation remains pending. Start with fine adjustment 0.0 ms, loopback OFF for normal recording, include repeated 44.1 kHz takes and use calibration only if a repeatable route-specific residual exists.

## Current branch policy
- `main` is canonical.
- Ordinary development/documentation commits use `[skip ci]`.
- `.github/workflows/android-ci.yml` remains manual-only (`workflow_dispatch`).
- The assistant must not dispatch or rerun Actions.
- Historical alpha/review/audit files remain evidence of their original checkpoints; they do not override active-state documents.

## Build and source materialization
`scripts/build_local.sh` is the local software gate when the required Gradle/Android SDK environment is available. `.github/workflows/android-ci.yml` is the canonical full software/API36/geometry/signing executor.

Large RC3 deltas are versioned under `.source-parts` and applied serially by `scripts/materialize_ci_sources.sh`. The current canonical tail ends at **H24a** and fails closed on unexpected source drift.

## Physical validation policy
Automatable mathematics, persistence invariants, timing policy, malformed-input handling, lifecycle behavior, accessibility semantics, generic geometry and Home-library selection logic are automated responsibilities. Physical review is reserved for real MK-300 routing/timing, touch ergonomics and listening.

The exact physical candidate is the CI #641 signed APK produced by `b11769f340f7056c37dfb17d95b062909dad87bf`. Later documentation-only commits do not change that identity.

## Security
Never commit keystores, credentials, local SDK configuration or secret artifacts.
