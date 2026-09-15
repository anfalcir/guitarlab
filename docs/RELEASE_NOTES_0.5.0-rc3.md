# GuitarLab Studio 0.5.0-rc3

Updated: 2026-09-15

## Canonical signed digital homologation — CI #631
Run `35025012392`, exact source `33fb05a504be2d047259b1d967e6ab1a7e48a68c`, is the signed DIGITAL PASS through H18/H18a/H19.

Identity:
- package `studio.guitarlab.app`;
- version `0.5.0-rc3` / versionCode `23`;
- unsigned APK SHA-256 `895ed8ccc957bf0bb17addfdd98806fd3425cc695443f234e27bbae62607cfd8`;
- signed APK SHA-256 `61441b92e3065ba845d9f3e0ed6791d35d41975180a01bb21b612427b493c02d`;
- certificate SHA-256 `4B82890A9812BB89E1BBEF179A48752BDA2CA8AB284C833DAA27A907F4CE5E89`;
- APK Signature Scheme v2, one RSA-4096 signer.

Digital gates:
- complete software/unit/audio/DSP/persistence/migration/performance/Lint/build/provenance: PASS;
- standard API36 connected regression: **23/23 PASS**;
- isolated target-tablet 1920×1200 / 240dpi geometry: **1/1 PASS**;
- signed homologation: PASS.

## Physical-review lineage
After #626, CUT presentation and Ajustes/Níveis placement were accepted. Two remaining defects were identified:
- the final `Ambas` comparison control could be clipped by fixed docked proportions;
- MK-300 output could appear twice because Android exposed multiple logical USB output endpoints for the same physical interface, with one observed endpoint producing no sound.

## H18 / H18a — adaptive practice bar — DIGITAL PASS
- fixed percentage allocation removed from the critical docked path;
- wide/tablet Comparação and Ajustes size to content;
- Timeline flexes into the remaining width and owns its own overflow;
- narrow docked widths stack semantic groups rather than scrolling the whole strip;
- instrumentation requires all four comparison controls to remain contained at target-tablet logical width;
- Ajustes/Níveis containment and segment non-overlap remain enforced.

CI #628 revealed the first narrow fallback could move Ajustes outside the default emulator viewport. H18a corrected this. CI #629/#630 then stopped only at preflight because four blank lines in the H18a patch had trailing whitespace. Commit `33fb05a...` cleaned those bytes without changing the materialized product behavior. CI #631 passed completely.

## H19 — canonical USB output routing — DIGITAL PASS / PHYSICAL CHECK PENDING
- one physical USB interface is represented once in the output selector even if Android publishes multiple logical endpoints;
- legacy endpoint signatures migrate to one canonical route signature;
- non-USB profiles remain distinct;
- duplicate candidates are resolved by a silent stereo probe plus actual `AudioTrack.routedDevice` confirmation rather than enumeration order;
- unconfirmed duplicate resolution fails closed to the existing automatic-route fallback;
- stale persistent selections are cleared on device refresh;
- deterministic JVM tests cover canonicalization, migration, route separation and ranking and passed at #631.

The emulator cannot prove the real MK-300 endpoint topology/routing on the Samsung tablet. Final hardware confirmation remains required: exactly one MK-300 choice, audible playback through it, and correct behavior after disconnect/reconnect.

## Release decision
The #631 APK is the current RC3 candidate for final physical homologation. If the residual checklist passes and no source/product code changes are introduced, M7 physical closure and the RC3 release decision can be finalized without another digital CI run.
