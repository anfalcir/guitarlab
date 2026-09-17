# GuitarLab Studio 0.5.0-rc3

Updated: 2026-09-17

## Current signed digital homologation — CI #653
Run `35207902169`, producer `d09fc003e2ae2d699238eb39ba699f75a746fea4`, is the signed DIGITAL PASS through H28.

Signed APK SHA-256: `1a36efcbe24d5995dd3609889237ca112670e52554e187d1b93ed5a8649263c0`.

Digital gate summary:
- H28 materialization PASS;
- 294/294 JVM/unit PASS;
- Android Lint/build/provenance PASS;
- 32/32 standard API36 PASS + 1/1 isolated target geometry PASS;
- exact tested-artifact signing/certificate/package verification PASS.

## H28 backup identity/provider-consistency corrective
H28 adds:
- immutable project identity using existing `GuitarProject.id`, so rename never creates a separate backup project;
- deterministic revision identity tied to edit timestamp plus canonical project-state digest;
- package SHA-256 as independent integrity evidence;
- deterministic revision paths for retry idempotency;
- direct verification of the exact remote documents written instead of immediate parent-list confirmation;
- targeted settling lookup before retry upload;
- backward-compatible H26/H27 metadata reading;
- deduplication that distinguishes different H28 states even under equal timestamp.

## Physical result
Target-device retest after #653 confirms the backup workflow is now functioning correctly and the false confirmation/selective-duplicate behavior that triggered H28 is no longer reproduced.

The remaining RC3 physical closure is recording latency/synchronization on the intended USB route.

## Forward development
The approved post-H28 hardening and external-control roadmap is documented in `POST_H28_HARDENING_AND_EXTERNAL_CONTROL_PLAN.md`.
