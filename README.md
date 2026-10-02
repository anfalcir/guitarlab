# GuitarLab

Private, single-owner Android guitar preparation, study, recording, mixing, export and backup appliance.

Updated: 2026-10-02
Release state: **RC20 ACCEPTED / RC26 SIGNED DIGITAL / RC27 MIXER UI — QUALIFICATION PENDING**

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

GuitarLab `0.5.0-rc20` / versionCode `40` remains the accepted personal-use baseline. RC26 `0.5.0-rc26` / `46` remains the latest signed digital authority. The active source candidate is RC27 `0.5.0-rc27` / `47`, an owner-approved Mixer visual-hierarchy refinement with centered channel headers and soft functional segmentation; it does not change audio behavior. RC27 qualification and any later physical acceptance remain pending.

- package: `studio.guitarlab.app`;
- accepted baseline identity and signer: `docs/RELEASE_BASELINE.md`;
- active candidate details and qualification state: `docs/CURRENT_STATE.md`;
- target line: Samsung SM-X230 / Android 16 / API 36, with M-VAVE MK-300 USB where applicable;
- remote separation: official PyTorch/Demucs `htdemucs_6s`, production worker frozen by immutable digest;
- backup: direct Google Drive API v3.

The inherited RC22 feature adds an optional secondary CUE output and a headphone button per track. MAIN/CUE routing is fail-closed and is a live monitoring choice, not a Master Export exclusion control.

## Product model

**Home → Prepare → Studio → Export → Backup / Activity / Settings**

The product includes provider-backed source acquisition, cloud Demucs separation, managed backing/guitar references, Studio recording/editing/mixer/timing, per-track MAIN/CUE monitoring, study/master export, Activity/background state, direct Drive v3 backup/restore and tablet-focused responsive/accessibility behavior.

Studio remains locally usable without login for local work. Network-dependent acquisition/separation/backup surfaces expose their dependency.

GBW is historical implementation provenance only. Legacy standalone GBW/H37/pre-unification migration is outside supported scope.

**No tuner, pitch detection or tuning-detection feature is part of GuitarLab.**

## Repository and materialization

- canonical branch: `main`;
- RC23 integrated review: PR #9; exact signed identity in `docs/CURRENT_STATE.md`;
- canonical materialization entrypoint: `scripts/materialize_ci_sources.sh`;
- active candidate tail: **RC27bCompactClipBadge** via `scripts/materialize_ci_sources_rc27b.py`;
- protected payloads: `.source-parts/`;
- materialization is deterministic, hash/blob locked, idempotent and fail-closed.

Do not rewrite old source payloads in place. A future runtime change begins a new corrective/materialization step.

## CI and signing

Ordinary documentation/source commits use `[skip ci]` unless qualification is intentionally requested.

- `[run ci]`: Android software + API36 qualification;
- `[run ci signed]`: qualifies and signs the exact tested unsigned candidate;
- `[run u4 cloud]`: controlled real-cloud transactional smoke;
- `[run u7 cloud]`: backend verification/deploy workflow with explicit shadow/production authorization.

The accepted RC20 baseline does not require recurring CI. RC26 remains retained signed digital evidence. RC27a software/API36 passed in CI #953 and regenerated valid complete/minimum tablet evidence. Signing remains blocked on one focused RC27b visual correction: replace the title-colliding textual CLIP face with a compact warning badge while retaining the same 48 dp action target and meter/reset behavior.

## Security and maintenance

Never commit keystores, private credentials, client secrets, refresh tokens or service-account keys.

The accepted RC20 APK and backend digest remain frozen until a successor is physically promoted. RC27 is not yet digitally qualified or signed. RC26 remains signed digital predecessor evidence; the separation backend and audio routing algorithms are unchanged.
