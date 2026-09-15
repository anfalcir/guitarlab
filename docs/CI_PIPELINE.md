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

New tail files:
- `.source-parts/H20PhysicalOutputCanonicalization.patch.gz`
- `.source-parts/H21StudioVisualSystem.patch.gz`

H20/H21 were validated against the exact materialized source emitted by CI #631. Forward application, reverse round-trip and `git diff --check` pass. The updated materializer passes `bash -n`.

## Last signed authority — CI #631
CI #631 / run `35025012392` / exact source `33fb05a504be2d047259b1d967e6ab1a7e48a68c` remains the authoritative signed DIGITAL PASS through H18/H18a/H19:
- standard API36 **23/23 PASS**;
- isolated 1920×1200 geometry **1/1 PASS**;
- signed APK SHA-256 `61441b92e3065ba845d9f3e0ed6791d35d41975180a01bb21b612427b493c02d`;
- certificate SHA-256 `4B82890A9812BB89E1BBEF179A48752BDA2CA8AB284C833DAA27A907F4CE5E89`.

Do not flatten the API36 report to 24/24; use **23/23 standard + 1/1 isolated geometry**.

## H20 next-gate requirements
The next manual run must prove:
- exact SM-X230-like built-in speaker duplicate policy tests pass;
- USB H19 canonicalization tests remain green;
- earpiece/Bluetooth/HDMI profile separation remains intact;
- Android compilation/integration accepts built-in speaker family mapping and routed-device probing;
- no routing regression appears in the existing recording/playback suite.

## H21 next-gate requirements
The next manual run must prove:
- all existing UI/instrumented regressions remain green;
- target-tablet comparison controls stay fully contained;
- Comparação/Ajustes/Timeline chassis do not overlap;
- each group title remains before and distinct from its first actionable control;
- narrow discoverability remains green;
- global theme/geometry changes do not regress Home/Studio/Options navigation or touch targets;
- isolated 1920×1200 geometry still passes.

## Evidence boundary
Until a new exact-source workflow passes, H20/H21 are PRE-GATE. CI #631 remains the last signed DIGITAL PASS but is not the final candidate for the new source changes.

## Artifact identity discipline
Documentation-only commits never replace the exact source SHA that produced a signed APK. The next H20/H21 candidate must be identified by the exact product/source SHA dispatched by the user.
