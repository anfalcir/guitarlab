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
- `docs/RELEASE_NOTES_0.5.0-rc14.md`
- `docs/RELEASE_NOTES_0.5.0-rc17.md`
- `docs/RC14_FINAL_PHYSICAL_HOMOLOGATION.md`
- `docs/RELEASE_NOTES_0.5.0-rc9.md` — historical corrective evidence

## Active source/release state

**U11 is CLOSED / DIGITAL PASS. U12 is active with RC17 as the physical-corrective source candidate.**

Active source candidate:
- `0.5.0-rc17`
- versionCode `36`
- package `studio.guitarlab.app`
- parser/reentry implementation baseline `2601220351cb241af5b23235a123645b4120ba64`; the final documentation/UI-coherence commit must bind its own exact workflow SHA before it can become authority

Latest signed authority is RC14 at producer `13c6f36e5e3c2bcf82cf21f885e8d7aa6c41ed34`: Android CI run `35937621047` passed unit, Lint, build, API 36 and signing. RC14's cloud-identical predecessor `9b4ee9f` passed U4 `35935229248` and U7 `35935229232`.

Locked homologation certificate SHA-256:
`4B82890A9812BB89E1BBEF179A48752BDA2CA8AB284C833DAA27A907F4CE5E89`.

RC17 retains end-to-end signal validation/canonicalization and Studio reinsertion, and removes the partitioned Demucs path proven to emit non-finite stem samples. It remains pending exact-source Android/U7 gates, shadow acceptance, controlled production deployment and target-device listening acceptance.

## Product model

GuitarLab is one application and one project model:

**Home → Prepare → Studio → Export → Backup / Activity / Settings**

The product includes:
- source search/import and managed source publication;
- cloud separation whose six intermediate stems remain ephemeral in v2;
- import of only the two managed final references (backing and guitar) into the project;
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
