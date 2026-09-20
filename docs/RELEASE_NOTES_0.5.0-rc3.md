# GuitarLab Studio 0.5.0-rc3

Updated: 2026-09-20

## Current signed digital homologation — CI #659
Run `35512894518`, producer `a6a53e8ba9e75b32565e451870758c7c65ad687f`, is the signed DIGITAL PASS through H35a.

Signed APK SHA-256: `e7ddce638a83d92af0ef2a01e46a10152f8c0eb4c535f0ddbc8671d9b94a1295`.

Digital gate summary:
- 330/330 JVM/unit PASS;
- Android Lint PASS with 0 errors / 50 warnings / 4 hints;
- debug/release build and unsigned provenance PASS;
- 33/33 standard API36 PASS + 1/1 isolated target geometry PASS;
- exact tested-artifact signing/certificate/package verification PASS.

## H34/H35/H35a included in #659
H34 expands global manual residual latency adjustment to ±500 ms. H35 separates the global future-recording setting from a persistent take-specific synchronization edit, adds PCM-zero silent route/clock verification, and replaces the harsh calibration burst with an exact-route-validated low-level adaptive chirp. H35a adds the explicit local RECORD_AUDIO permission guard required by Lint. All are included in the signed #659 candidate.

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

## Current source after #659 — H36/H36a/H36b PRE-GATE
H36 refines Settings UX without changing audio behavior:
- main Settings is centered/capped on tablets instead of stretching across the display;
- section/card hierarchy is lighter and secondary actions become compact responsive trailing buttons;
- Audio refresh, Backup, External Control/HID and Diagnostics no longer present wide primary-looking actions on tablet layouts;
- external-control mapping rows are denser/responsive;
- calibration fine adjustment replaces horizontal scrolling with visible −25/−5/−1 and +1/+5/+25 ms rows plus reset;
- silent/physical calibration actions share the same compact responsive component;
- new/expanded Compose instrumentation protects Settings hierarchy and calibration-control visibility.

CI #660 passed H36's software gate but failed Android-test compilation; H36a corrected that import. CI #661 then compiled and ran instrumentation, with 31/34 passing and three viewport-only Settings assertions failing. H36b corrects those tests without changing runtime UI. CI #659 remains the signed authority until a complete exact-source canonical gate passes.

## Forward development
The approved post-H28 hardening and external-control roadmap is documented in `POST_H28_HARDENING_AND_EXTERNAL_CONTROL_PLAN.md`.
