# GuitarLab

Private, single-owner Android guitar preparation, study, recording, mixing, export and backup appliance.

Updated: 2026-10-01  
Release state: **RC20 ACCEPTED / RC22 DUAL-OUTPUT FEATURE CANDIDATE — REQUALIFICATION ACTIVE**

## Start here

For the current product, read:

1. `docs/PROJECT_IDENTITY.md`
2. `docs/CURRENT_STATE.md`
3. `docs/RELEASE_BASELINE.md`
4. `docs/PRODUCT_REQUIREMENTS.md`
5. `docs/ARCHITECTURE.md`
6. `docs/DECISIONS.md`
7. `docs/TEST_AND_HOMOLOGATION_POLICY.md`
8. `docs/CANDIDATE_IDENTITY_POLICY.md`
9. `docs/CI_PIPELINE.md`
10. `docs/DOCUMENTATION_MAP.md`

Completed roadmaps, milestone plans, candidate checklists, release campaigns and audits are under `docs/history/` and are not current authority.

## Accepted baseline and active candidate

GuitarLab `0.5.0-rc20` / versionCode `40` remains the accepted personal-use baseline. The active successor is `0.5.0-rc22` / versionCode `42`, an owner-requested per-track MAIN/CUE monitoring feature built on the RC21-integrated Android source.

- package: `studio.guitarlab.app`;
- accepted baseline identity and signer: `docs/RELEASE_BASELINE.md`;
- active candidate details and qualification state: `docs/CURRENT_STATE.md`;
- target line: Samsung SM-X230 / Android 16 / API 36, with M-VAVE MK-300 USB where applicable;
- remote separation: official PyTorch/Demucs `htdemucs_6s`, production worker frozen by immutable digest;
- backup: direct Google Drive API v3.

The active RC22 feature adds an optional secondary CUE output and a headphone button per track. MAIN/CUE routing is fail-closed and is a live monitoring choice, not a Master Export exclusion control.

## Product model

**Home → Prepare → Studio → Export → Backup / Activity / Settings**

The product includes provider-backed source acquisition, cloud Demucs separation, managed backing/guitar references, Studio recording/editing/mixer/timing, per-track MAIN/CUE monitoring, study/master export, Activity/background state, direct Drive v3 backup/restore and tablet-focused responsive/accessibility behavior.

Studio remains locally usable without login for local work. Network-dependent acquisition/separation/backup surfaces expose their dependency.

GBW is historical implementation provenance only. Legacy standalone GBW/H37/pre-unification migration is outside supported scope.

**No tuner, pitch detection or tuning-detection feature is part of GuitarLab.**

## Repository and materialization

- canonical branch: `main`;
- active RC22 development branch: `feature/dual-output-cue-routing`;
- canonical materialization entrypoint: `scripts/materialize_ci_sources.sh`;
- active candidate tail: **RC22DualOutputCueRouting** via `scripts/materialize_ci_sources_rc22.py`;
- protected payloads: `.source-parts/`;
- materialization is deterministic, hash/blob locked, idempotent and fail-closed.

Do not rewrite old source payloads in place. A future runtime change begins a new corrective/materialization step.

## CI and signing

Ordinary documentation/source commits use `[skip ci]` unless qualification is intentionally requested.

- `[run ci]`: Android software + API36 qualification;
- `[run ci signed]`: qualifies and signs the exact tested unsigned candidate;
- `[run u4 cloud]`: controlled real-cloud transactional smoke;
- `[run u7 cloud]`: backend verification/deploy workflow with explicit shadow/production authorization.

The accepted RC20 baseline does not require recurring CI. RC22 qualification is active because the owner explicitly requested this new audio-routing feature.

## Security and maintenance

Never commit keystores, private credentials, client secrets, refresh tokens or service-account keys.

The accepted RC20 APK and backend digest remain frozen until a successor is promoted. RC22 requalifies only the Android paths that its MAIN/CUE routing can materially affect plus adjacent integration coverage. The separation backend is unchanged.
