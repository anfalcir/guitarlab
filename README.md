# GuitarLab

Private, single-owner Android guitar preparation, study, recording, mixing, export and backup appliance.

Updated: 2026-09-24

## Project identity

GuitarLab is for personal/private use by one owner. It is not a public or commercial service and does not carry universal device/input/support obligations. Release rigor is risk-based: integrity, musical usefulness, the primary workflow, recovery, quota/cost, credentials and exact artifact identity remain strict; unrelated matrices, convenience diagnostics and maintenance-only freshness do not block by default.

Read the stable identity policy in `docs/PROJECT_IDENTITY.md` and durable decisions in `docs/DECISIONS.md`.

## Read first

For current work, use this small authority set:

1. `docs/PROJECT_IDENTITY.md`
2. `docs/CURRENT_STATE.md`
3. `docs/DECISIONS.md`
4. `docs/PRODUCT_REQUIREMENTS.md`
5. `docs/IMPLEMENTATION_ROADMAP.md`
6. `docs/U12_RC19_AUDIO_FORENSICS_AND_DIAGNOSTICS_PLAN.md`
7. `docs/TEST_AND_HOMOLOGATION_PLAN.md`
8. `docs/CANDIDATE_IDENTITY_POLICY.md`
9. `docs/DOCUMENTATION_MAP.md`

Older U/H/M milestone files, release notes and audits remain evidence but are not current authority unless a live document explicitly reactivates a specific contract.

## Current release line

RC19 (`0.5.0-rc19` / versionCode `39`) is the latest signed digitally qualified candidate, but it is physically rejected because a real Prepare run produced musically unusable separated audio. RC20 is the corrective successor line.

The exact current worker/source/run status must be read from `docs/CURRENT_STATE.md`; this README intentionally does not duplicate volatile run IDs as release authority.

Locked Android identity:

- package: `studio.guitarlab.app`;
- homologation certificate SHA-256: `4B82890A9812BB89E1BBEF179A48752BDA2CA8AB284C833DAA27A907F4CE5E89`.

## Product model

**Home → Prepare → Studio → Export → Backup / Activity / Settings**

The product includes provider-backed source acquisition, cloud Demucs separation, project-managed backing/guitar references, Studio recording/editing/mixer/timing, study/master export, Activity/background state, direct Google Drive v3 backup/restore and tablet-focused responsive/accessibility behavior.

GBW is historical implementation provenance only. Legacy standalone GBW/H37/pre-unification migration is intentionally outside release scope.

No tuner functionality is part of GuitarLab.

## Branch and CI policy

- `main` is canonical.
- Ordinary documentation/source commits use `[skip ci]` unless a real gate is requested.
- `[run ci]` runs the current Android software/API36 workflow.
- `[run ci signed]` signs the exact tested unsigned release candidate.
- `[run u4 cloud]` runs the controlled real-cloud transactional smoke.
- `[run u7 cloud]` runs backend verification; controlled shadow/production mutation requires explicit authorization.
- Broad existing coverage remains available. A proven unrelated flaky/cosmetic failure may be quarantined only with explicit evidence; protected integrity/audio/primary-flow/quota/credential/signing failures may not be quarantined.

## Source materialization

Large protected deltas are versioned under `.source-parts/` and materialized serially by `scripts/materialize_ci_sources.sh`.

The current canonical tail is **U12bi**. Materialization remains deterministic, hash/blob verified, idempotent and fail-closed. It is not being refactored merely for cleanup before RC20.

## Security and freeze

Never commit keystores, private credentials, client secrets, refresh tokens or service-account keys.

After final physical acceptance, the exact signed APK and digest-pinned worker become the frozen personal-use baseline. Rebuild only for an observed regression, real provider/platform deprecation, applicable high-risk vulnerability, credential exposure, unacceptable integrity/cost risk or an owner-requested feature.
