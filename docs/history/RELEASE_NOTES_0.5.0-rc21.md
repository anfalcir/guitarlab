# GuitarLab 0.5.0-rc21 — Maintenance Candidate Release Notes

Updated: 2026-09-25  
Status: **candidate — unsigned software/API36 PASS; signed/physical promotion pending**

RC21 is a focused maintenance successor to the physically accepted RC20 baseline. It does not change the production Demucs worker or expand product scope.

## What changed

- repairs legacy recorded-guitar stereo separation so derived mono clips use the actual splitter frame/rate bounds;
- narrowly reconstructs missing take metadata for recognizable legacy managed recordings without reclassifying generic imports;
- preserves unambiguous modern take metadata across stereo separation and fails closed for ambiguous shared lineage;
- verifies persisted project-state integrity before compatibility recovery mutates the in-memory representation;
- centralizes transient Error/Warning/Async Completion presentation and prevents stale Snackbar replay across navigation;
- rebuilds Drive version-history loading around one remote head snapshot plus a validated metadata-only local cache;
- keeps cached versions visible during refresh and shows immediate loading when no cached catalog exists;
- makes automatic backup/catalog interaction explicitly UI-independent: off-screen worker completion does not issue an unnecessary catalog read, while the next Backup-screen entry forces Drive verification;
- hardens cancellation, cache corruption/account/retention/local-revision invalidation and API36 fail-closed test classification.

## Compatibility and scope

RC21 keeps:

- package `studio.guitarlab.app`;
- direct Google Drive API v3 backup transport;
- OAuth `drive.file` scope;
- the accepted PyTorch/Demucs `htdemucs_6s` production backend and exact frozen worker digest;
- existing recording route/timing architecture;
- the no-tuner/no-pitch-detection product scope.

Historical GBW/H37/pre-unification migration remains outside supported scope.

## Qualification

Unsigned candidate authority:

- source: `ad182678cb2704dc9bbfc622124b4f2ac121fea1`;
- Android CI #910 / run `36195905242`;
- Unit tests: PASS;
- Android Lint: PASS;
- APK build: PASS;
- API 36 emulator regression: PASS.

The signed candidate must be produced from canonical `main` after integration and temporary-branch removal.

## Promotion status

RC20 remains the accepted baseline until RC21 completes:

1. exact-source signed qualification;
2. install on Samsung SM-X230 / Android 16;
3. proportional owner acceptance for the affected Studio legacy-recovery path and representative Drive backup/catalog/restore behavior;
4. final baseline/documentation promotion.

See `RC21_MAINTENANCE_QUALIFICATION_2026-09-25.md` for detailed forensic and CI evidence.
