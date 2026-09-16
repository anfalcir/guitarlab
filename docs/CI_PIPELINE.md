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
`H12 engine → H12 UI → H13 → H14 → H14a → H15 → H16 → H17 → H18 → H19 → H18a → H20 → H21 → H22 → H22a → H23 → H23a`.

H23 is stored as deterministic gzip+base64 source-parts. H23a is a test-only annotation correction applied after H23. The materializer verifies final hashes and reports `Source patch chain materialized through H23a with verified final hashes`.

## Last signed authority — CI #638
CI #638 / run `35084703365` / exact source `c310be6779e6591c57399257f380588c27bdf20a` is the authoritative signed DIGITAL PASS through H23/H23a:
- software/unit/audio/DSP/persistence/migration/performance/Lint/build/provenance: PASS;
- standard API36: **23/23 PASS**;
- isolated 1920×1200 geometry: **1/1 PASS**;
- signed homologation: PASS;
- unsigned APK SHA-256 `621e02355d265bdb6c24cb5e324b445b63e739f8e7d6adc233eebea3b31fe0d5`;
- signed APK SHA-256 `a650afa5edfd2b8c4f8393e65b314ae9fbb59487a87c2d3ea85ef978d7d895dc`;
- certificate SHA-256 `4B82890A9812BB89E1BBEF179A48752BDA2CA8AB284C833DAA27A907F4CE5E89`.

Do not flatten the API36 report to 24/24; use **23/23 standard + 1/1 isolated geometry**.

## CI #637 historical note
CI #637 / source `086fa3b080f0994b9031f52cb8ac3f01754d5378` materialized H23 and passed API36, but the software gate stopped while compiling `AppTransientFeedbackPolicyTest` because the test used `kotlin.test.Test` rather than the app-standard JUnit 4 annotation. H23a corrected only the test import; production code is unchanged between the H23 product source and the H23a test correction.

## H23-specific regression evidence
The successful #638 gate covers the complete existing suite plus H23-specific contracts:
- `AudioClockAnchorPolicy` stable/stale/inconsistent timestamp behavior;
- signed startup offset behavior and mixed-clock-basis fail-closed logic;
- route latency and fine adjustment applied exactly once;
- recording sample-rate derivation from the editing domain;
- punch crop after final compensation;
- transient feedback contract and technical-route-token filtering;
- all existing route, recording, playback and UI regressions retained.

## Artifact identity discipline
A later documentation-only commit never replaces the exact source SHA that produced a signed APK. The authoritative H23 product/source remains `c310be6779e6591c57399257f380588c27bdf20a` from CI #638 until a later product commit itself receives a successful signed gate.

## Remaining acceptance
Digital H23/H23a is PASS. Remaining work is focused physical validation only: real SM-X230 + MK-300 REC alignment at the actual project sample rate, with fine adjustment initially 0 ms; transient-feedback UX smoke; and retained route/recording/editing/export smoke.
