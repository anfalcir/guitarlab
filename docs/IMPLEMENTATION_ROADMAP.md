# Implementation Roadmap

Updated: 2026-09-16

## Milestones
- **M1 — Foundation:** CLOSED.
- **M2 — Android hardware/audio baseline:** PASS/CLOSED.
- **M3–M4:** absorbed into later milestones.
- **M5 — Reliable recording + Studio + Media I/O:** PASS/CLOSED.
- **M6 — Measured latency/synchronization:** PASS/CLOSED.
- **M7 — Production audio polish:** digital scope PASS; residual physical closure active.
- **M8 — Release hardening:** signed DIGITAL PASS through **H26/H26e**; final physical/provider closure active.

## Current signed authority
**CI #650** / run `35154021384` / producer `07c99155789774cb39f9b4382829f9e1d16649e3` is the current signed DIGITAL PASS.

Evidence:
- 285/285 JVM/unit PASS;
- performance evidence PASS;
- Lint PASS with 44 warnings + 3 hints, 0 errors in reports;
- debug/release build + unsigned provenance PASS;
- API36 **31/31 standard + 1/1 isolated geometry**;
- signed APK SHA-256 `fdb870d0996b7ee4ec6f72028af9c7291d4767b4303cbd5fa62894288757d2fe`;
- signer SHA-256 `4B82890A9812BB89E1BBEF179A48752BDA2CA8AB284C833DAA27A907F4CE5E89`;
- signed provenance and signing cleanup PASS.

## H26 — SAF Cloud Backup — DIGITAL PASS
Implemented and digitally proven:
1. persistable SAF folder selection with read/write/delete probe;
2. full and per-project manual backup;
3. automatic incremental/coalesced + periodic backup through WorkManager;
4. full/latest-per-project and single-version restore without overwrite;
5. transaction staging + remote re-read + SHA-256/size verification + strict `COMMITTED` marker;
6. configurable folder, cadence/network/charging restrictions;
7. age retention + protected minimum versions per project;
8. fail-safe cleanup rules for full/partial failure, cancellation and incomplete versions;
9. dedicated Backup/Restauração screen, Settings summary and Home per-project shortcut;
10. pure policy/coordinator plus Android UI coverage.

## Canonical materialization tail
`… → H25 → H26 → H26a → H26b → H26e`.

H26e deterministically installs the complete validated Backup-screen instrumentation source and was proven by #650.

## Remaining release path
1. Do **not** run another CI merely for the DIGITAL PASS documentation commit.
2. Install/upgrade to the exact #650 signed APK on Samsung SM-X230 and verify its SHA-256 before final approval.
3. Select a real Google Drive SAF folder and verify permission persistence after app restart and tablet reboot.
4. Smoke single/full backup, cloud visibility/status, target-folder change/disconnect, single/full restore and permission-revocation behavior.
5. Exercise representative large-transfer/cancel behavior and practical retention/incremental behavior where feasible.
6. Complete retained H25/H24/H23b physical checks, especially MK-300 loopback-off recording, timing/alignment, waveform, route, backing isolation and edit/save/reopen/export.
7. Final RC3 approval requires no repeatable P0/P1 and explicit user approval of the exact #650 signed APK SHA-256.

## Gate discipline
`.github/workflows/android-ci.yml` remains manual-only (`workflow_dispatch`). Ordinary commits use `[skip ci]`; the assistant must not dispatch/rerun without explicit user instruction. Documentation HEAD may advance after #650, but the signed APK producer remains exactly `07c99155789774cb39f9b4382829f9e1d16649e3`.