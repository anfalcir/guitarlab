# GuitarLab

Private, single-owner Android guitar preparation, study, recording, mixing, export and backup appliance.

Updated: 2026-09-25  
Release state: **RC20 ACCEPTED / RC21 MAINTENANCE CANDIDATE — SOFTWARE/API36 PASS**

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

GuitarLab `0.5.0-rc20` / versionCode `40` remains the accepted personal-use baseline. A proportional maintenance candidate, `0.5.0-rc21` / versionCode `41`, has completed its unsigned software/API36 qualification and is progressing through canonical-main signing and residual owner acceptance.

- package: `studio.guitarlab.app`;
- final Android CI: #904 PASS;
- exact signed APK and certificate identity: `docs/RELEASE_BASELINE.md`;
- owner physical homologation: PASS on 2026-09-25;
- target line: Samsung SM-X230 / Android 16 / API 36, with M-VAVE MK-300 USB where applicable;
- remote separation: official PyTorch/Demucs `htdemucs_6s`, production worker frozen by immutable digest;
- backup: direct Google Drive API v3.

The exact hashes, producer SHAs, worker digest, model identity and qualification runs live in `docs/RELEASE_BASELINE.md`.

## Product model

**Home → Prepare → Studio → Export → Backup / Activity / Settings**

The product includes provider-backed source acquisition, cloud Demucs separation, managed backing/guitar references, Studio recording/editing/mixer/timing, study/master export, Activity/background state, direct Drive v3 backup/restore and tablet-focused responsive/accessibility behavior.

Studio remains locally usable without login for local work. Network-dependent acquisition/separation/backup surfaces expose their dependency.

GBW is historical implementation provenance only. Legacy standalone GBW/H37/pre-unification migration is outside supported scope.

**No tuner, pitch detection or tuning-detection feature is part of GuitarLab.**

## Repository and materialization

- canonical branch: `main`;
- canonical materialization entrypoint: `scripts/materialize_ci_sources.sh`;
- current source tail: **RC21IntegrityFeedbackBackup** via `scripts/materialize_ci_sources_rc21.py`;
- protected payloads: `.source-parts/`;
- materialization is deterministic, hash/blob locked, idempotent and fail-closed.

Do not rewrite old source payloads in place. A future runtime change begins a new corrective/materialization step.

## CI and signing

Ordinary documentation/source commits use `[skip ci]` unless qualification is intentionally requested.

- `[run ci]`: Android software + API36 qualification;
- `[run ci signed]`: qualifies and signs the exact tested unsigned candidate;
- `[run u4 cloud]`: controlled real-cloud transactional smoke;
- `[run u7 cloud]`: backend verification/deploy workflow with explicit shadow/production authorization.

The accepted RC20 baseline does not require recurring CI. RC21 qualification is intentionally active only because an observed regression reopened maintenance under the frozen-product policy.

## Security and maintenance

Never commit keystores, private credentials, client secrets, refresh tokens or service-account keys.

The accepted RC20 APK and backend digest remain frozen until a successor is promoted. RC21 exists because a real owner-observed regression triggered maintenance under `docs/PROJECT_IDENTITY.md` / D-090 / D-096; its qualification is proportional under `docs/TEST_AND_HOMOLOGATION_POLICY.md`. The separation backend is unchanged.
