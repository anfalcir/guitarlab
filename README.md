# GuitarLab Studio

Android-first guitar practice, recording, comparison and mixing workspace.

Updated: 2026-09-20

## Repository truth
The repository is canonical for scope, architecture, implementation state and homologation evidence.

Read first: `docs/CURRENT_STATE.md`, `docs/H37_DRIVE_V3_BACKUP.md`, `docs/H35_TAKE_SYNC_QUIET_CALIBRATION.md`, `docs/POST_H28_HARDENING_AND_EXTERNAL_CONTROL_PLAN.md`, `docs/H28_BACKUP_IDENTITY_CONSISTENCY.md`, `docs/IMPLEMENTATION_ROADMAP.md`, `docs/TEST_AND_HOMOLOGATION_PLAN.md`, `docs/RC3_FINAL_PHYSICAL_HOMOLOGATION.md`, `docs/CANDIDATE_IDENTITY_POLICY.md` and `docs/DOCUMENTATION_MAP.md`.

## Active source/release state
Source candidate: **`0.5.0-rc4` / versionCode `24` / H37a — SOURCE PRE-GATE**.
Current signed candidate remains **`0.5.0-rc3` / versionCode `23`**, package `studio.guitarlab.app`, until the manual H37a canonical gate passes.

The current signed digital authority is **CI #663** / run `35523442620` / exact producer `51d4098fa7b1b44a9fa315e939541020f594654d`, with the complete H29-H36c line DIGITAL PASS. Signed APK SHA-256: `8e6e0f555bc5e834c4bccbdb011134c42d124806ce0316dd28787a6c98fe7bf3`.

CI #663 closed the exact H36c source: 330/330 JVM/unit tests PASS, Android Lint 0 errors (50 warnings + 4 hints), **34/34 standard API36 instrumented tests PASS**, **1/1 isolated 1920×1200 geometry PASS**, debug/release build + unsigned provenance PASS, and exact tested-artifact signing/package/certificate verification PASS.

H36 Settings UX Polish is therefore digitally closed. The main Settings hierarchy and calibration modal presentation changes are now part of the signed candidate; H36a/H36b/H36c remain traceable test correctives that do not change runtime UI.

Target-device validation after #653 had already accepted the H28 backup corrective. The remaining stable-release physical boundary continues to be H29 timing/alignment, H30 real USB hot-unplug/reconnect/capture preservation and H32 continuous 10-minute quality. H33 real-controller acceptance remains a separate 1.1 boundary.

## H37 — Native Google Drive API v3 backup
H37 replaces SAF as the primary backup transport with direct Google Drive API v3 using the narrow `drive.file` OAuth scope. The H28 `projectId` / `revisionId` / package-SHA identity model, retention, deduplication and restore-as-copy semantics are preserved.

The transport adds resumable uploads, server-confirmed resume offsets, Drive size/SHA-256 verification, private `appProperties` catalog identity, crash reconciliation and bounded backoff. Existing H26-H28 SAF history remains supported as a one-time migration source and is not released locally until migration completes with zero failures.

H37 producer `abecc73e4eab181a7776d98cc731758b17c64b06` reached manual CI #664, which failed deterministically at Kotlin compilation before Lint/build/signing. H37a is the minimal compile corrective and remains **SOURCE PRE-GATE**. CI #663 / RC3 is still the signed authority until a fresh manual H37a workflow passes. See `docs/H37_DRIVE_V3_BACKUP.md`.

## H28 — stable project/revision identity + provider consistency
H28 hardens the backup contract so that:
- `GuitarProject.id` is the immutable backup identity; renaming never creates a new project history;
- each new revision has deterministic identity from edit timestamp + canonical-state digest;
- package SHA-256 remains independent byte-integrity evidence;
- version paths are deterministic per revision;
- commit success is verified through the exact remote URIs written rather than an immediately refreshed directory listing;
- targeted revision lookup absorbs normal provider listing delay before retry upload;
- legacy H26/H27 backups remain readable;
- deduplication cannot collapse two different H28 states merely because they share a timestamp.

## Approved forward scope
Before stable `1.0.0`, the project will harden existing recording/USB/recovery/takes/diagnostics/backup/UX behavior and establish a realistic quality boundary for song projects through 10 minutes. The only approved new feature after that stable line is external MIDI/footswitch control. See `docs/POST_H28_HARDENING_AND_EXTERNAL_CONTROL_PLAN.md`.

## Branch/CI policy
- `main` is canonical.
- `.github/workflows/android-ci.yml` is manual-only (`workflow_dispatch`).
- ordinary source/docs commits use `[skip ci]`.
- the assistant must not dispatch or rerun Actions without explicit user instruction.

## Source materialization
Large deltas are versioned in `.source-parts` and materialized serially by `scripts/materialize_ci_sources.sh`. Canonical source tail: **`… → H25 → H26 → H26a → H26b → H26e → H27 → H28 → H29 → H30 → H31 → H32 → H33 → H33a → H33b → H34 → H35 → H35a → H36 → H36a → H36b → H36c → H37 → H37a`**. The accepted H28 materializer remains frozen in `scripts/materialize_ci_sources_through_h28.sh`; `scripts/materialize_ci_sources_h37a.sh` composes H37 and then applies the hash-verified H37a compile corrective. Unexpected source/archive/patch/final-blob drift fails closed.

## Security
Never commit keystores, credentials, local SDK configuration or secret artifacts. H37 Drive access is limited to OAuth `drive.file`; no client secret, refresh token, service-account key or Firebase/backend credential belongs in the APK or repository. SAF persists only as a legacy migration source for existing H26-H28 histories.
