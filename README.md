# GuitarLab

Android-first guitar practice, recording, preparation, comparison, mixing, export and backup workspace.

Updated: 2026-09-22

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

## Active source/release state

**U11 is CLOSED / DIGITAL PASS. U12 final physical homologation is active.**

U10 technical authority:
- Android CI #781 / run `35791192802`;
- exact technical source: `2aa97aa8b2c3af91d35e7d48a0eeb89c2c330e63`;
- software/Lint/build: PASS;
- API 36: 23/23 classified instrumented classes, 76 observed tests, 5/5 groups PASS;
- visual cohesion: 24/24 retained screenshots, deterministic visual review PASS.

Frozen signed U11 candidate:
- `0.5.0-rc5`
- versionCode `25`
- package `studio.guitarlab.app`
- producer `4218e4343746932a4de61c5abaa29ba5769a30ed`
- Android CI #783 / run `35793456972`
- signed APK SHA-256 `795839766f2b7546af53b2c56a0638b11c25b79622fe73cce5860b5602f050e0`
- branch `main`

This is the current signed release authority and the only APK eligible for U12 physical homologation.

Locked homologation certificate SHA-256:
`4B82890A9812BB89E1BBEF179A48752BDA2CA8AB284C833DAA27A907F4CE5E89`.

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

Remote separation uses the production cloud integration governed by the unified roadmap. U11 repeated the controlled real-cloud six-stem smoke on the exact freeze SHA; U4 Cloud Integration Smoke #104 passed.

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
- U11 is frozen and closed at `4218e4343746932a4de61c5abaa29ba5769a30ed`; subsequent docs-only commits do not alter the APK producer identity.

## Source materialization

Large protected deltas are versioned under `.source-parts/` and materialized serially by `scripts/materialize_ci_sources.sh`.

The current canonical tail ends at **U10zb**. Each stage preserves fail-closed patch/blob verification, `git diff --check` and reverse-apply/idempotence guarantees. Unexpected drift blocks the build.

## Security

Never commit keystores, credentials, local SDK configuration, client secrets, refresh tokens or service-account keys.

Drive authorization is limited to `drive.file`. Remote-separation deployment uses keyless GitHub/OpenID federation where cloud mutation is explicitly authorized. The Android package and signing identity remain `studio.guitarlab.app` plus the locked homologation certificate.

## Physical boundary

U11 is complete. U12 now performs one consolidated physical campaign on:
- Samsung SM-X230 / Android 16 / API 36;
- M-VAVE MK-300 over USB;
- actual intended hub/power topology when part of normal use.

The final manual campaign is limited to claims digital systems cannot establish: real USB routing/isolation, monitoring, capture behavior, timing/listening, reconnect behavior, continuous 10-minute quality and target-device ergonomics.

No tuner functionality is part of GuitarLab.
