# GuitarLab Studio 0.5.0-rc3

Updated: 2026-09-20

## Current signed digital homologation — CI #657
Run `35290128876`, producer `e371bb2a5c8040c668b926b2077c03d1c7c8c7d6`, is the signed DIGITAL PASS through H33b.

Signed APK SHA-256: `05d6eaf71fb69ce55b28b8e3214e619a15f3b862dada973740927c2b9ecf2cd6`.

Digital gate summary:
- 323/323 JVM/unit PASS;
- Android Lint PASS with 0 errors;
- debug/release build and unsigned provenance PASS;
- 33/33 standard API36 PASS + 1/1 isolated target geometry PASS;
- exact tested-artifact signing/certificate/package verification PASS.

## Current source after #657 — H34/H35 PRE-GATE
H34 expands global manual residual latency adjustment to ±500 ms. H35 separates that global future-recording setting from a persistent take-specific synchronization edit and replaces the harsh calibration burst with an exact-route-validated, short low-level adaptive chirp. H35 also adds a PCM-zero silent digital route/clock verification that never stores a fake physical latency.

The #657 APK does not contain H34/H35; a new exact-source CI run is required before these changes become a signed candidate.

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
