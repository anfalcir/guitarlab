# Android CI / release pipeline

Updated: 2026-09-16

## Contract
`.github/workflows/android-ci.yml` is manual-only (`workflow_dispatch`). Commits do not automatically consume hosted CI, and the assistant must not dispatch or rerun the workflow.

The pipeline has three authority layers:
1. software gate — materialization, JVM/unit/audio/DSP/persistence/migration, performance, Lint, debug/release build and unsigned provenance;
2. API36 gate — standard connected instrumentation plus isolated 1920×1200 / 240dpi geometry;
3. signed homologation — signs the exact tested unsigned artifact only after both mandatory gates pass.

## Source materialization
`.source-parts/` plus `scripts/materialize_ci_sources.sh` are part of the build contract. Unexpected source drift fails closed.

Canonical tail after H11b:
`H12 engine → H12 UI → H13 → H14 → H14a → H15 → H16 → H17 → H18 → H19 → H18a → H20 → H21 → H22 → H22a → H23`.

H23 is stored as deterministic gzip+base64 source-parts and materializes only after the H22a hashes are established. Final H23 hashes cover every modified production/test source file and a second materializer execution must report that H23 is already materialized.

## Last signed authority — CI #636
CI #636 / run `35040569179` / exact source `b0a39a765f7cfbb0e9320ee847809300bc1e3d01` is the authoritative signed DIGITAL PASS through H22/H22a:
- software/unit/audio/DSP/persistence/migration/performance/Lint/build/provenance: PASS;
- standard API36: **23/23 PASS**;
- isolated 1920×1200 geometry: **1/1 PASS**;
- signed homologation: PASS;
- unsigned APK SHA-256 `de996298a451cd559320cf71498121a652f9e3ca8054dba8bf2d95a281d08c47`;
- signed APK SHA-256 `b195d8fc4d90fa0f8fa8c826525d08328f65090859386a90eaa37e4bcdf4087e`;
- certificate SHA-256 `4B82890A9812BB89E1BBEF179A48752BDA2CA8AB284C833DAA27A907F4CE5E89`.

Do not flatten the API36 report to 24/24; use **23/23 standard + 1/1 isolated geometry**.

## H23 evidence boundary
H23 is PRE-GATE until one manually dispatched run on its exact source passes all three authority layers. Local source validation is intentionally not labeled Android build/runtime PASS.

Required H23-specific regression evidence includes:
- `AudioClockAnchorPolicy` stable/stale/inconsistent timestamp behavior;
- signed startup offset at 44.1/48/96 kHz and mixed-clock-basis fail-closed behavior;
- route latency and fine adjustment applied exactly once;
- recording sample-rate derivation from the editing domain;
- punch crop after final compensation;
- transient feedback contract and technical-route-token filtering;
- all existing route, recording, playback and UI regressions retained.

## Artifact identity discipline
A later documentation-only commit never replaces the exact source SHA that produced a signed APK. Until H23 receives a successful signed gate, the authoritative signed product/source remains `b0a39a765f7cfbb0e9320ee847809300bc1e3d01` from CI #636.
