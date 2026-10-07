# GuitarLab

Private, single-owner Android guitar preparation, study, recording, mixing, export and backup appliance.

Updated: 2026-10-07
Release state: **RC20 ACCEPTED BASELINE / RC33 RUNTIME ALIGNMENT SUCCESSOR — SIGNED CI PENDING / PHYSICAL REQUALIFICATION PENDING**

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

GuitarLab `0.5.0-rc20` / versionCode `40` remains the physically accepted personal-use baseline until an exact signed successor is owner-accepted. The current successor source, CI/signing identity and residual physical gate live only in `docs/CURRENT_STATE.md`; historical RC chronology lives under `docs/history/`.

The current CUE architecture supports a capability-proven Communication Split path (MAIN media + CUE communication) and a conventional multi-device compatibility fallback. MAIN/CUE physical isolation remains fail-closed, CUE never blocks or automatically ducks MAIN, and the mixer remains the authority for content level. A future USB multichannel MAIN 1/2 + CUE 3/4 backend remains documented as a separate professional roadmap.

- package: `studio.guitarlab.app`;
- accepted baseline identity and signer: `docs/RELEASE_BASELINE.md`;
- active successor identity/status: `docs/CURRENT_STATE.md`;
- target line: Samsung SM-X230 / Android 16 / API 36, with M-VAVE MK-300 USB where applicable;
- remote separation: official PyTorch/Demucs `htdemucs_6s`, production worker frozen by immutable digest;
- backup: direct Google Drive API v3.

## Product model

**Home → Prepare → Studio → Export → Backup / Activity / Settings**

The product includes provider-backed source acquisition, cloud Demucs separation, managed backing/guitar references, Studio recording/editing/mixer/timing, per-track MAIN/CUE monitoring, study/master export, Activity/background state, direct Drive v3 backup/restore and tablet-focused responsive/accessibility behavior.

Studio remains locally usable without login for local work. Network-dependent acquisition/separation/backup surfaces expose their dependency.

GBW is historical implementation provenance only. Legacy standalone GBW/H37/pre-unification migration is outside supported scope.

**No tuner, pitch detection or tuning-detection feature is part of GuitarLab.**

## Repository and materialization

- canonical branch: `main`;
- canonical current-source validation entrypoint: `scripts/materialize_ci_sources.sh`;
- the current checked-in source is guarded by semantic/source verification; older staged materializers and `.source-parts/` are historical/reproducibility inputs, not current candidate state;
- candidate/run chronology belongs in `docs/CURRENT_STATE.md`, not in this README.

## CI and signing

Ordinary documentation/source commits do not require runtime qualification unless intentionally requested.

- `[run ci]`: Android software + API36 qualification;
- `[run ci signed]`: qualifies and signs the exact tested unsigned candidate;
- `[run u4 cloud]`: controlled real-cloud transactional smoke;
- `[run u7 cloud]`: backend verification/deploy workflow with explicit shadow/production authorization.

Exact current run numbers, producer SHA, hashes and signing state are recorded in `docs/CURRENT_STATE.md`. The immutable accepted baseline remains in `docs/RELEASE_BASELINE.md`.

## Security and maintenance

Never commit keystores, private credentials, client secrets, refresh tokens or service-account keys.

The accepted RC20 APK and backend digest remain frozen until an exact signed successor is physically promoted. Successor audio-routing work does not reopen unrelated separation, Drive, project-persistence or export evidence unless those paths are actually changed.
