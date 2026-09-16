# Implementation Roadmap

Updated: 2026-09-16

## Milestones
- **M1 — Foundation:** CLOSED.
- **M2 — Android hardware/audio baseline:** PASS/CLOSED.
- **M3–M4:** absorbed into later milestones.
- **M5 — Reliable recording + Studio + Media I/O:** PASS/CLOSED.
- **M6 — Measured latency/synchronization:** PASS/CLOSED.
- **M7 — Production audio polish:** digital scope PASS; residual physical closure active.
- **M8 — Release hardening:** signed DIGITAL PASS through H25; H26 SAF backup is PRE-GATE.

## Current signed authority
**CI #642** / run `35121955150` / producer `7bfd876b6a5b0701ab0cf5203de36c31cd117632`: DIGITAL PASS through H25 only.
Evidence remains 269/269 JVM/unit, Lint/build/provenance PASS, API36 **28/28 standard + 1/1 isolated geometry**, signed SHA-256 `916758f694735febb8cccfe58f46f290562aaba1b5483ccc727e2be0cefba5aa`.

## H26 — SAF Cloud Backup — PRE-GATE
Implemented:
1. persistable SAF folder selection with read/write/delete probe;
2. full and per-project manual backup;
3. automatic incremental/coalesced + periodic backup through WorkManager;
4. full/latest-per-project and single-version restore without overwrite;
5. transaction staging + remote re-read + SHA-256/size verification + strict `COMMITTED` marker;
6. configurable folder, cadence/network/charging restrictions;
7. age retention + protected minimum versions per project;
8. fail-safe cleanup rules for full/partial failure, cancellation and incomplete versions;
9. dedicated Backup/Restauração screen, Settings summary and Home per-project shortcut;
10. pure policy/coordinator and Android UI test coverage.

## Canonical materialization tail
`… → H23b → H24 → H24a → H25 → H26`.

## Next release path
1. Manually run a fresh signed CI for the H26 source SHA; do not dispatch automatically.
2. Audit actual JVM/API36 counts, Lint/build/provenance, materializer final message and signed identity.
3. If green, promote H26 to DIGITAL PASS and bind the physical checklist to that exact APK.
4. On SM-X230, select a real Google Drive SAF folder and verify permission persistence/restart behavior.
5. Smoke single/full backup, single/full restore, target-folder change/disconnect, revoked permission and large-transfer foreground behavior.
6. Confirm old target data is never deleted by folder switching/disconnect and restore creates independent local projects.
7. Complete retained H25/H24/H23b physical checks, especially MK-300 recording-timing residuals.
8. Final RC approval requires no repeatable P0/P1 and explicit approval of the exact new signed candidate hash.

## Gate discipline
`.github/workflows/android-ci.yml` remains manual-only (`workflow_dispatch`). Ordinary commits use `[skip ci]`; the assistant must not dispatch/rerun without explicit user instruction.
