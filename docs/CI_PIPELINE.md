# Android CI / release pipeline

Updated: 2026-09-15

## Contract
`.github/workflows/android-ci.yml` is manual-only (`workflow_dispatch`). Commits do not automatically consume hosted CI, and the assistant must not dispatch or rerun the workflow.

The pipeline has three authority layers:
1. software gate — materialization, JVM/unit/audio/DSP/persistence/migration, performance, Lint, debug/release build and unsigned provenance;
2. API36 gate — standard connected instrumentation plus isolated 1920×1200 / 240dpi geometry;
3. signed homologation — signs the exact tested unsigned artifact only after both mandatory gates pass.

## Source materialization
`.source-parts/` plus `scripts/materialize_ci_sources.sh` are part of the build contract. Unexpected source drift fails closed.

Canonical tail after H11b:
`H12 engine → H12 UI → H13 → H14 → H14a → H15 → H16 → H17 → H18 → H19 → H18a → H20 → H21`.

CI #632 exposed a transport corruption in the H21 source-part archive before compilation. The source materialization was hardened in exact source `2204e0272f9e6e2f218bebd36889db424e006e03`; CI #633 then materialized H20/H21 successfully and passed the complete gate.

## Last signed authority — CI #633
CI #633 / run `35033323990` / exact source `2204e0272f9e6e2f218bebd36889db424e006e03` is the authoritative signed DIGITAL PASS through H21:
- software/unit/audio/DSP/persistence/migration/performance/Lint/build/provenance: PASS;
- standard API36: **23/23 PASS**;
- isolated 1920×1200 geometry: **1/1 PASS**;
- signed homologation: PASS;
- unsigned APK SHA-256 `58165dc53cacaf39357a1f28315b6312ada3ed2b6669b59e32ddbe8e4d53c25d`;
- signed APK SHA-256 `f40b24cb36b4cb1299efcb28a35b54e66b107af2ec3d578a870c70e2966ff52e`;
- certificate SHA-256 `4B82890A9812BB89E1BBEF179A48752BDA2CA8AB284C833DAA27A907F4CE5E89`.

Do not flatten the API36 report to 24/24; use **23/23 standard + 1/1 isolated geometry**.

## H20 evidence at #633
Eight `StudioAudioRoutePolicyTest` cases passed, including:
- built-in speaker logical endpoints collapse to one physical choice;
- legacy built-in route migrates to the canonical route;
- duplicate USB endpoints collapse correctly;
- ordering does not affect compatibility ranking;
- earpiece and built-in speaker remain distinct;
- distinct USB addresses remain distinct routes.

## H21 evidence at #633
- materialization succeeds after the #632 transport hardening;
- existing UI/instrumented regressions remain green;
- target-tablet comparison controls remain contained;
- narrow semantic groups remain visible;
- isolated target-tablet geometry passes.

## Evidence boundary
H20/H21 are DIGITAL PASS at #633. The remaining acceptance is physical only: real-route enumeration/audibility/reconnect on SM-X230 + MK-300 and human visual review of the new app-wide system.

## Artifact identity discipline
Documentation-only commits after #633 do not replace the exact source SHA that produced the signed APK. The authoritative product/source SHA remains `2204e0272f9e6e2f218bebd36889db424e006e03`.
