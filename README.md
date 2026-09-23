# GuitarLab

Android-first guitar practice, recording, preparation, comparison, mixing, export and backup workspace.

Updated: 2026-09-23

## Repository truth

The repository is canonical for scope, architecture, implementation state and homologation evidence.

Read first:
- `docs/CURRENT_STATE.md`
- `docs/UNIFIED_GUITARLAB_GBW_IMPLEMENTATION_ROADMAP.md`
- `docs/U10_FINAL_DIGITAL_COHESION_GATE.md`
- `docs/UNIFIED_PRODUCT_COHESION_AUDIT.md`
- `docs/CANDIDATE_IDENTITY_POLICY.md`
- `docs/CI_PIPELINE.md`
- `docs/TEST_AND_HOMOLOGATION_PLAN.md`
- `docs/DOCUMENTATION_MAP.md`
- `docs/H37_DRIVE_V3_BACKUP.md`
- `docs/H35_TAKE_SYNC_QUIET_CALIBRATION.md`
- `docs/RELEASE_NOTES_0.5.0-rc11.md`
- `docs/RELEASE_NOTES_0.5.0-rc9.md` — historical corrective evidence

## Active source/release state

**U11 is CLOSED / DIGITAL PASS. U12 final physical homologation is active on rc11.**

Current signed U12 candidate:
- `0.5.0-rc11`
- versionCode `31`
- package `studio.guitarlab.app`
- exact producer `34cb60624b2fabf21cfe2c60003b04eac1597418`
- Android CI #806 / run `35885021871` — PASS, including full API 36 regression and signed homologation
- U4 Cloud Integration Smoke #123 / run `35885021902` — PASS, real six-stem Cloud Run contract
- U7 Cloud Backend #81 / run `35885021969` — PASS, stable Email/Password Firebase identity + allowlisted UID verification + backend source/container/security gate
- signed artifact `GuitarLabStudio-0.5.0-rc11-homologacao` / id `10762204176`
- signed APK SHA-256 `3cd9bfe06c9aa1af7f452f0dd8d9e9bead50774bde4a3b22c72f09f51e5ea769`
- signed APK size `79,965,840` bytes
- signed artifact ZIP digest `9f9ad8e16222c4eac1d8dcc27d7946e75e54be6a9a9bfcd243beb0284e14e7b2`

Locked homologation certificate SHA-256:
`4B82890A9812BB89E1BBEF179A48752BDA2CA8AB284C833DAA27A907F4CE5E89`.

Rc11 supersedes rc9/rc10 for U12. Target-device bugreports proved the remote worker was executing but retrying before Storage/Functions/Cloud Run. The migration had replaced GBW's stable Firebase Email/Password identity with anonymous auth while the backend still enforced `GBW_ALLOWED_UIDS`. Rc11 restores the stable account contract, adds backend authorization preflight and typed pipeline failures, and hardens Activity cleanup without weakening the UID allowlist.

## Product model

GuitarLab is one application and one project model:

**Home → Prepare → Studio → Export → Backup / Activity / Settings**

The product includes:
- source search/import and managed source publication;
- cloud six-stem separation;
- automatic managed backing/reference preparation;
- Studio recording/editing/mixer/timing workflows;
- canonical export workspace;
- unified Activity/background-operation model;
- direct Google Drive API v3 backup/restore;
- phone/tablet responsive UI and accessibility contracts.

GBW is historical implementation provenance only; it is not a second end-user product surface.

Legacy standalone GBW/H37/pre-unification migration is intentionally out of release scope. U9 is retired.

## Current cloud/backup authority

Remote separation uses the production Firebase/Cloud Run integration governed by the unified roadmap. The rc11 producer passed U4 Cloud Integration Smoke #123 on the exact source SHA; U7 Cloud Backend #81 also passed Email/Password Firebase identity enforcement, allowlisted-user verification and backend source/container/security verification. The six-stem Cloud Run contract remains unchanged.

Drive backup uses direct client-side Google Identity Services + Drive API v3 with OAuth `drive.file`. U8m closed provider-real acceptance with:

`U8m PASS · r_1790095960 · cleanup 8/8/14`

Sanitized report SHA-256:
`84efb70615be8ef5939da538eee5f714f7311aa6d44dd5bb2cdd0b5e9e66b702`.

No Firebase/Cloud Run/Functions hop, service account, client secret or refresh-token custody is part of the Drive backup data path.

## Branch and CI policy

- `main` is canonical.
- Ordinary commits use `[skip ci]`.
- `[run ci]` runs software + API 36 gates.
- `[run ci signed]` additionally signs the exact tested unsigned release candidate.
- `[run u4 cloud]` runs the controlled real-cloud six-stem smoke.
- `[run u7 cloud]` runs backend source/container/security verification; push-triggered execution does not deploy.
- The current rc11 APK producer is frozen at `34cb60624b2fabf21cfe2c60003b04eac1597418`; subsequent docs-only commits never alter that APK producer identity.

## Source materialization

Large protected deltas are versioned under `.source-parts/` and materialized serially by `scripts/materialize_ci_sources.sh`.

The current canonical tail ends at **U12w**. Each stage preserves fail-closed patch/blob verification, `git diff --check` and reverse-apply/idempotence guarantees. Unexpected drift blocks the build.

## Security

Never commit keystores, credentials, local SDK configuration, client secrets, refresh tokens or service-account keys.

Drive authorization is limited to `drive.file`. Remote-separation deployment uses keyless GitHub/OpenID federation where cloud mutation is explicitly authorized. The Android package and signing identity remain `studio.guitarlab.app` plus the locked homologation certificate.

## Physical boundary

U11 is complete. U12 now resumes on the exact rc11 signed candidate. The focused corrective check is to authenticate once in Opções → Conta e nuvem using the existing personal Firebase Email/Password account, verify the account is accepted by the backend, then repeat Prepare separation to confirm upload → callable → Cloud Run → import on the target tablet. The broader residual campaign remains on:
- Samsung SM-X230 / Android 16 / API 36;
- M-VAVE MK-300 over USB;
- actual intended hub/power topology when part of normal use.

The final manual campaign is limited to claims digital systems cannot establish: real USB routing/isolation, monitoring, capture behavior, timing/listening, reconnect behavior, continuous 10-minute quality and target-device ergonomics.

No tuner functionality is part of GuitarLab.
