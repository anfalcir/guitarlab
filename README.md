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
- `docs/RELEASE_NOTES_0.5.0-rc9.md`

## Active source/release state

**U11 is CLOSED / DIGITAL PASS. U12 final physical homologation is active on rc9 after the rc8 orphan-reconciliation finding.**

Current signed U12 candidate:
- `0.5.0-rc9`
- versionCode `29`
- package `studio.guitarlab.app`
- exact producer `635124acfbf133553a96c8b2013f2245f58a6877`
- Android CI #794 / run `35856278980` — PASS
- U4 Cloud Integration Smoke #111 / run `35856278976` — PASS, real six-stem Cloud Run contract
- U7 Cloud Backend #69 / run `35856279117` — PASS, backend source/container/security verification; deploy skipped
- signed artifact `GuitarLabStudio-0.5.0-rc9-homologacao` / id `10748475073`
- signed APK SHA-256 `4fcf529b935a584217b3b882ca8dfa05e3c360f99cfe9d70071080175439dd6c`
- signed APK size `79,945,360` bytes
- signed artifact ZIP digest `86077306c01afc80a07ba10b14080aec9fdb1a6a2e6416ef193bad190d274f53`

Locked homologation certificate SHA-256:
`4B82890A9812BB89E1BBEF179A48752BDA2CA8AB284C833DAA27A907F4CE5E89`.

Rc8 remains historical digital evidence but is withdrawn from final U12 approval because target-device testing exposed a durable local `CANCEL_REQUESTED` job with no corresponding remote job. Rc9 makes backend existence authoritative for orphan reconciliation, bounds cancellation retries, guarantees local terminalization, preserves the accepted source, and prevents a stale terminal generation from masking or resetting a newer active separation.

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

Remote separation uses the production Firebase/Cloud Run integration governed by the unified roadmap. The rc9 producer passed U4 Cloud Integration Smoke #111 on the exact source SHA; U7 Cloud Backend #69 also passed backend source/container/security verification. The rc9 client recovery hardening changes local durable reconciliation, not the six-stem cloud data contract.

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
- The current rc9 APK producer is frozen at `635124acfbf133553a96c8b2013f2245f58a6877`; subsequent docs-only commits never alter that APK producer identity.

## Source materialization

Large protected deltas are versioned under `.source-parts/` and materialized serially by `scripts/materialize_ci_sources.sh`.

The current canonical tail ends at **U12k**. Each stage preserves fail-closed patch/blob verification, `git diff --check` and reverse-apply/idempotence guarantees. Unexpected drift blocks the build.

## Security

Never commit keystores, credentials, local SDK configuration, client secrets, refresh tokens or service-account keys.

Drive authorization is limited to `drive.file`. Remote-separation deployment uses keyless GitHub/OpenID federation where cloud mutation is explicitly authorized. The Android package and signing identity remain `studio.guitarlab.app` plus the locked homologation certificate.

## Physical boundary

U11 is complete. U12 now resumes on the exact rc9 signed candidate. The first corrective check is upgrade/install over the rc8 device state that remained in `CANCEL_REQUESTED`: opening Prepare must converge to a terminal orphan state, preserve the accepted source, and expose a new separation attempt. The broader residual campaign remains on:
- Samsung SM-X230 / Android 16 / API 36;
- M-VAVE MK-300 over USB;
- actual intended hub/power topology when part of normal use.

The final manual campaign is limited to claims digital systems cannot establish: real USB routing/isolation, monitoring, capture behavior, timing/listening, reconnect behavior, continuous 10-minute quality and target-device ergonomics.

No tuner functionality is part of GuitarLab.
